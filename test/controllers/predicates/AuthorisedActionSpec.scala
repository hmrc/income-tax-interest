/*
 * Copyright 2023 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package controllers.predicates

import common.{EnrolmentIdentifiers, EnrolmentKeys}
import models.User
import org.scalamock.handlers.CallHandler4
import play.api.http.Status._
import play.api.mvc.Results._
import play.api.mvc.{AnyContent, AnyContentAsEmpty, Result}
import play.api.test.FakeRequest
import testUtils.{MockAppConfig, TestSuite}
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.Retrieval
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.auth.core.syntax.retrieved.authSyntaxForRetrieved
import uk.gov.hmrc.auth.core.{Enrolment, EnrolmentIdentifier, Enrolments, _}
import uk.gov.hmrc.http.{HeaderCarrier, SessionId}

import scala.concurrent.{ExecutionContext, Future}

class AuthorisedActionSpec extends TestSuite {

  val auth: AuthorisedAction = authorisedAction
  val mtdItId = "1234567890"
  val arn = "0987654321"

  def mockAuthorisePredicates[A](predicate: Predicate,
                                 returningResult: Future[A]): CallHandler4[Predicate, Retrieval[_], HeaderCarrier, ExecutionContext, Future[Any]] = {
    (mockAuthConnector.authorise(_: Predicate, _: Retrieval[_])(_: HeaderCarrier, _: ExecutionContext))
      .expects(predicate, *, *, *)
      .returning(returningResult)
  }

  ".enrolmentGetIdentifierValue" should {

    "return the value for a given identifier" in {
      val returnValue = "anIdentifierValue"
      val returnValueAgent = "anAgentIdentifierValue"

      val enrolments = Enrolments(Set(
        Enrolment(EnrolmentKeys.Individual, Seq(EnrolmentIdentifier(EnrolmentIdentifiers.individualId, returnValue)), "Activated"),
        Enrolment(EnrolmentKeys.Agent, Seq(EnrolmentIdentifier(EnrolmentIdentifiers.agentReference, returnValueAgent)), "Activated")
      ))

      auth.enrolmentGetIdentifierValue(EnrolmentKeys.Individual, EnrolmentIdentifiers.individualId, enrolments) mustBe Some(returnValue)
      auth.enrolmentGetIdentifierValue(EnrolmentKeys.Agent, EnrolmentIdentifiers.agentReference, enrolments) mustBe Some(returnValueAgent)
    }
    "return a None" when {
      val key = "someKey"
      val identifierKey = "anIdentifier"
      val returnValue = "anIdentifierValue"

      val enrolments = Enrolments(Set(Enrolment(key, Seq(EnrolmentIdentifier(identifierKey, returnValue)), "someState")))


      "the given identifier cannot be found" in {
        auth.enrolmentGetIdentifierValue(key, "someOtherIdentifier", enrolments) mustBe None
      }

      "the given key cannot be found" in {
        auth.enrolmentGetIdentifierValue("someOtherKey", identifierKey, enrolments) mustBe None
      }

    }

    ".individualAuthentication" should {

      "perform the block action" when {

        "the correct enrolment and nino exist" which {
          val block: User[AnyContent] => Future[Result] = user => Future.successful(Ok(user.mtditid))
          val mtditid = "AAAAAA"
          val enrolments = Enrolments(Set(Enrolment(
            EnrolmentKeys.Individual,
            Seq(EnrolmentIdentifier(EnrolmentIdentifiers.individualId, mtditid)), "Activated"),
            Enrolment(
              EnrolmentKeys.nino,
              Seq(EnrolmentIdentifier(EnrolmentIdentifiers.nino, mtditid)), "Activated")
          ))

          lazy val result: Future[Result] = {
            (mockAuthConnector.authorise(_: Predicate, _: Retrieval[_])(_: HeaderCarrier, _: ExecutionContext))
              .expects(*, Retrievals.allEnrolments and Retrievals.confidenceLevel, *, *)
              .returning(Future.successful(enrolments and ConfidenceLevel.L250))
            auth.individualAuthentication(block, mtditid)(fakeRequest, emptyHeaderCarrier)
          }

          "returns an OK status" in {
            status(result) mustBe OK
          }

          "returns a body of the mtditid" in {
            bodyOf(result) mustBe mtditid
          }
        }
        "the correct enrolment and nino exist but the request is for a different id" which {
          val block: User[AnyContent] => Future[Result] = user => Future.successful(Ok(user.mtditid))
          val mtditid = "AAAAAA"
          val enrolments = Enrolments(Set(Enrolment(
            EnrolmentKeys.Individual,
            Seq(EnrolmentIdentifier(EnrolmentIdentifiers.individualId, "123456")), "Activated"),
            Enrolment(
              EnrolmentKeys.nino,
              Seq(EnrolmentIdentifier(EnrolmentIdentifiers.nino, mtditid)), "Activated")
          ))

          lazy val result: Future[Result] = {
            (mockAuthConnector.authorise(_: Predicate, _: Retrieval[_])(_: HeaderCarrier, _: ExecutionContext))
              .expects(*, Retrievals.allEnrolments and Retrievals.confidenceLevel, *, *)
              .returning(Future.successful(enrolments and ConfidenceLevel.L250))
            auth.individualAuthentication(block, mtditid)(fakeRequest, emptyHeaderCarrier)
          }

          "returns an UNAUTHORIZED status" in {
            status(result) mustBe UNAUTHORIZED
          }
        }
        "the correct enrolment and nino exist but low CL" which {
          val block: User[AnyContent] => Future[Result] = user => Future.successful(Ok(user.mtditid))
          val mtditid = "AAAAAA"
          val enrolments = Enrolments(Set(Enrolment(
            EnrolmentKeys.Individual,
            Seq(EnrolmentIdentifier(EnrolmentIdentifiers.individualId, mtditid)), "Activated"),
            Enrolment(
              EnrolmentKeys.nino,
              Seq(EnrolmentIdentifier(EnrolmentIdentifiers.nino, mtditid)), "Activated")
          ))

          lazy val result: Future[Result] = {
            (mockAuthConnector.authorise(_: Predicate, _: Retrieval[_])(_: HeaderCarrier, _: ExecutionContext))
              .expects(*, Retrievals.allEnrolments and Retrievals.confidenceLevel, *, *)
              .returning(Future.successful(enrolments and ConfidenceLevel.L50))
            auth.individualAuthentication(block, mtditid)(fakeRequest, emptyHeaderCarrier)
          }

          "returns an UNAUTHORIZED status" in {
            status(result) mustBe UNAUTHORIZED
          }
        }

        "return unauthorised when no sessionId exist" which {
          val fakeRequestWithNoSessionId: FakeRequest[AnyContentAsEmpty.type] =
            FakeRequest().
              withHeaders("mtditid" -> "1234567890")

          val block: User[AnyContent] => Future[Result] = user => Future.successful(Ok(user.mtditid))

          val enrolments = Enrolments(Set(Enrolment(
            EnrolmentKeys.Individual,
            Seq(EnrolmentIdentifier(EnrolmentIdentifiers.individualId, mtdItId)), "Activated"),
            Enrolment(
              EnrolmentKeys.nino,
              Seq(EnrolmentIdentifier(EnrolmentIdentifiers.nino, mtdItId)), "Activated")
          ))

          lazy val result: Future[Result] = {
            (mockAuthConnector.authorise(_: Predicate, _: Retrieval[_])(_: HeaderCarrier, _: ExecutionContext))
              .expects(*, Retrievals.allEnrolments and Retrievals.confidenceLevel, *, *)
              .returning(Future.successful(enrolments and ConfidenceLevel.L250))
            auth.individualAuthentication(block, mtdItId)(fakeRequestWithNoSessionId, emptyHeaderCarrier)
          }

          "returns an 401 status" in {
            status(result) mustBe UNAUTHORIZED
          }
        }

        "the correct enrolment exist but no nino" which {
          val block: User[AnyContent] => Future[Result] = user => Future.successful(Ok(user.mtditid))
          val mtditid = "AAAAAA"
          val enrolments = Enrolments(Set(Enrolment(
            EnrolmentKeys.Individual,
            Seq(EnrolmentIdentifier(EnrolmentIdentifiers.individualId, mtditid)), "Activated")
          ))

          lazy val result: Future[Result] = {
            (mockAuthConnector.authorise(_: Predicate, _: Retrieval[_])(_: HeaderCarrier, _: ExecutionContext))
              .expects(*, Retrievals.allEnrolments and Retrievals.confidenceLevel, *, *)
              .returning(Future.successful(enrolments and ConfidenceLevel.L250))
            auth.individualAuthentication(block, mtditid)(fakeRequest, emptyHeaderCarrier)
          }

          "returns an 401 status" in {
            status(result) mustBe UNAUTHORIZED
          }
        }
        "the correct nino exist but no enrolment" which {
          val block: User[AnyContent] => Future[Result] = user => Future.successful(Ok(user.mtditid))
          val id = "AAAAAA"
          val enrolments = Enrolments(Set(Enrolment(
            EnrolmentKeys.nino,
            Seq(EnrolmentIdentifier(EnrolmentIdentifiers.nino, id)), "Activated")
          ))

          lazy val result: Future[Result] = {
            (mockAuthConnector.authorise(_: Predicate, _: Retrieval[_])(_: HeaderCarrier, _: ExecutionContext))
              .expects(*, Retrievals.allEnrolments and Retrievals.confidenceLevel, *, *)
              .returning(Future.successful(enrolments and ConfidenceLevel.L250))
            auth.individualAuthentication(block, id)(fakeRequest, emptyHeaderCarrier)
          }

          "returns an 401 status" in {
            status(result) mustBe UNAUTHORIZED
          }
        }

      }

      "return a unauthorised" when {

        "the correct enrolment is missing" which {
          val block: User[AnyContent] => Future[Result] = user => Future.successful(Ok(user.mtditid))
          val mtditid = "AAAAAA"
          val enrolments = Enrolments(Set(Enrolment("notAnIndividualOops", Seq(EnrolmentIdentifier(EnrolmentIdentifiers.individualId, mtditid)), "Activated")))

          lazy val result: Future[Result] = {
            (mockAuthConnector.authorise(_: Predicate, _: Retrieval[_])(_: HeaderCarrier, _: ExecutionContext))
              .expects(*, Retrievals.allEnrolments and Retrievals.confidenceLevel, *, *)
              .returning(Future.successful(enrolments and ConfidenceLevel.L250))
            auth.individualAuthentication(block, mtditid)(fakeRequest, emptyHeaderCarrier)
          }

          "returns a UNAUTHORIZED" in {
            status(result) mustBe UNAUTHORIZED
          }
        }
      }
    }

    ".agentAuthenticated" should {

      val block: User[AnyContent] => Future[Result] = user => Future.successful(Ok(s"${user.mtditid} ${user.arn.get}"))

      "perform the block action" when {

        "the agent is authorised for the given user" which {

          val enrolments = Enrolments(Set(
            Enrolment(EnrolmentKeys.Individual, Seq(EnrolmentIdentifier(EnrolmentIdentifiers.individualId, mtdItId)), "Activated"),
            Enrolment(EnrolmentKeys.Agent, Seq(EnrolmentIdentifier(EnrolmentIdentifiers.agentReference, arn)), "Activated")
          ))

          lazy val result = {
            (mockAuthConnector.authorise(_: Predicate, _: Retrieval[_])(_: HeaderCarrier, _: ExecutionContext))
              .expects(*, *, *, *)
              .returning(Future.successful(enrolments))

            auth.agentAuthentication(block,mtdItId)(fakeRequestWithMtditid, emptyHeaderCarrier)
          }

          "has a status of OK" in {
            status(result) mustBe OK
          }

          "has the correct body" in {
            bodyOf(result) mustBe mtdItId + " " + arn
          }
        }
      }

      "return an Unauthorised" when {

        "the authorisation service returns an AuthorisationException exception" in {
          object AuthException extends AuthorisationException("Some reason")

          lazy val result = {
            mockAuthReturnException(AuthException)
            auth.agentAuthentication(block,mtdItId)(fakeRequestWithMtditid, emptyHeaderCarrier)
          }
          status(result) mustBe UNAUTHORIZED
        }

      }

      "return an Unauthorised" when {

        "the authorisation service returns a NoActiveSession exception" in {
          object NoActiveSession extends NoActiveSession("Some reason")

          lazy val result = {
            mockAuthReturnException(NoActiveSession)
            auth.agentAuthentication(block,mtdItId)(fakeRequestWithMtditid, emptyHeaderCarrier)
          }

          status(result) mustBe UNAUTHORIZED
        }
      }
      "return a UNAUTHORIZED" when {

        "the user does not have an enrolment for the agent" in {
          val enrolments = Enrolments(Set(
            Enrolment(EnrolmentKeys.Individual, Seq(EnrolmentIdentifier(EnrolmentIdentifiers.individualId, mtdItId)), "Activated")
          ))

          lazy val result = {
            (mockAuthConnector.authorise(_: Predicate, _: Retrieval[_])(_: HeaderCarrier, _: ExecutionContext))
              .expects(*, *, *, *)
              .returning(Future.successful(enrolments))
            auth.agentAuthentication(block,mtdItId)(fakeRequestWithMtditid, emptyHeaderCarrier)
          }

          status(result) mustBe UNAUTHORIZED
        }
      }
    }

    ".agentAuthenticated as secondary agent" should {

      val block: User[AnyContent] => Future[Result] = user => Future.successful(Ok(s"${user.mtditid} ${user.arn.get}"))

      val secondaryAgentEnrolments = Enrolments(Set(
        Enrolment("HMRC-MTD-IT-SUPP", Seq(EnrolmentIdentifier("MTDITID", mtdItId)), "Activated"),
        Enrolment("HMRC-AS-AGENT", Seq(EnrolmentIdentifier("AgentReferenceNumber", arn)), "Activated")
      ))
      "perform the block action" when {

        "primary fails and secondary agent is authroised " which {

          lazy val result = {
            mockAuthorisePredicates(auth.agentAuthPredicate(mtdItId), Future.failed(InsufficientEnrolments("Primary failed")))

            mockAuthorisePredicates(auth.secondaryAgentPredicate(mtdItId), Future.successful(secondaryAgentEnrolments))

            auth.agentAuthentication(block, mtdItId)(fakeRequestWithMtditid, emptyHeaderCarrier)
          }

          "has a status of OK" in {
            status(result) mustBe OK
          }

          "has the correct body" in {
            bodyOf(result) mustBe mtdItId + " " + arn
          }


          "not fallback to secondary agent if primary fails when supporting agents are disabled" which {
            val mockAppConfig = new MockAppConfig{
              override val emaSupportingAgentsEnabled: Boolean = false
            }

            val authorisedAction = new AuthorisedAction()(mockAuthConnector, defaultActionBuilder, mockAppConfig, mockControllerComponents)

            lazy val result = {
              mockAuthorisePredicates(authorisedAction.agentAuthPredicate(mtdItId), Future.failed(InsufficientEnrolments("Primary failed")))
              authorisedAction.agentAuthentication(block, mtdItId)(fakeRequestWithMtditid, emptyHeaderCarrier)
            }

            "has a status of SEE_OTHER" in {
              status(result) mustBe UNAUTHORIZED
            }
          }

          "return error if sessionId is not present, when supporting agents are enabled" which {
            val fakeRequestWithNoSessionId: FakeRequest[AnyContentAsEmpty.type] =
              FakeRequest().
                withSession("MTDITID" -> "1234567890")

            lazy val result = {

              mockAuthorisePredicates(auth.agentAuthPredicate(mtdItId), Future.failed(InsufficientEnrolments("Primary failed")))
              mockAuthorisePredicates(auth.secondaryAgentPredicate(mtdItId), Future.successful(secondaryAgentEnrolments))

              auth.agentAuthentication(block, mtdItId)(fakeRequestWithNoSessionId, emptyHeaderCarrier)
            }

            "has a status of SEE_OTHER" in {
              status(result) mustBe UNAUTHORIZED
            }
          }

          "return succeed if sessionId is present in HeaderCarrier" which {
            val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId("sessionIdValue")))
            val fakeRequestWithNoSessionId: FakeRequest[AnyContentAsEmpty.type] =
              FakeRequest().
                withSession("MTDITID" -> "1234567890")

            lazy val result = {

              mockAuthorisePredicates(auth.agentAuthPredicate(mtdItId), Future.failed(InsufficientEnrolments("Primary failed")))
              mockAuthorisePredicates(auth.secondaryAgentPredicate(mtdItId), Future.successful(secondaryAgentEnrolments))

              auth.agentAuthentication(block, mtdItId)(fakeRequestWithNoSessionId, hc)
            }

            "has a status of SEE_OTHER" in {
              status(result) mustBe OK
            }
          }



          "return error if both primary and secondary fails when supporting agents are enabled" which {
            lazy val result = {

              mockAuthorisePredicates(auth.agentAuthPredicate(mtdItId), Future.failed(InsufficientEnrolments("Primary failed")))
              mockAuthorisePredicates(auth.secondaryAgentPredicate(mtdItId), Future.failed(InsufficientEnrolments("Secondary failed")))


              auth.agentAuthentication(block, mtdItId)(fakeRequestWithMtditid, emptyHeaderCarrier)
            }

            "has a status of SEE_OTHER" in {
              status(result) mustBe UNAUTHORIZED
            }
          }
        }
      }
    }

    ".async" should {
      lazy val block: User[AnyContent] => Future[Result] = user =>
        Future.successful(Ok(s"mtditid: ${user.mtditid}${user.arn.fold("")(arn => " arn: " + arn)}"))

      "perform the block action" when {

        "the user is successfully verified as an agent" which {

          lazy val result = {
            mockAuthAsAgent()
            auth.async(block)(fakeRequest)
          }

          "should return an OK(200) status" in {

            status(result) mustBe OK
            bodyOf(result) mustBe "mtditid: " + mtdItId + " arn: " + arn
          }
        }

        "the user is successfully verified as an individual" in {

          lazy val result = {
            mockAuth()
            auth.async(block)(fakeRequest)
          }

          status(result) mustBe OK
          bodyOf(result) mustBe "mtditid: " + mtdItId
        }
      }

      "return an Unauthorised" when {

        "the authorisation service returns an AuthorisationException exception" in {
          object AuthException extends AuthorisationException("Some reason")

          lazy val result = {
            mockAuthReturnException(AuthException)
            auth.async(block)
          }
          status(result(fakeRequest)) mustBe UNAUTHORIZED
        }

      }

      "return an Unauthorised" when {

        "the authorisation service returns a NoActiveSession exception" in {
          object NoActiveSession extends NoActiveSession("Some reason")

          lazy val result = {
            mockAuthReturnException(NoActiveSession)
            auth.async(block)
          }

          status(result(fakeRequest)) mustBe UNAUTHORIZED
        }


        "the mtditid is not in the header" in {

          lazy val result = auth.async(block)(FakeRequest())
          status(result) mustBe UNAUTHORIZED
        }

      }
    }
  }
}
