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

package controllers

import config.AppConfig
import models.Done
import models.TaxYearPathBindable.TaxYear
import models.mongo.JourneyAnswers
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchersSugar.eqTo
import org.mockito.MockitoSugar
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.{BeforeAndAfterEach, OptionValues}
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.JourneyAnswersRepository
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals.{affinityGroup, allEnrolments, confidenceLevel}
import uk.gov.hmrc.auth.core.retrieve.~

import java.time.temporal.ChronoUnit
import java.time.{Clock, Instant, ZoneId}
import scala.concurrent.Future

class JourneyAnswersControllerSpec
  extends AnyWordSpec
    with Matchers
    with MockitoSugar
    with OptionValues
    with ScalaFutures
    with BeforeAndAfterEach {

  private val mtdItId: String = "1234567890"
  private val activated: String = "Activated"

  private val enrolments: Enrolments = Enrolments(Set(
    Enrolment(
      "HMRC-MTD-IT",
      Seq(EnrolmentIdentifier("MTDITID", mtdItId)),
      activated
    ),
    Enrolment(
      "HMRC-NI",
      Seq(EnrolmentIdentifier("NINO", "nino")),
      activated
    )
  ))

  private val authResponse: Enrolments ~ ConfidenceLevel =
    new~(
      enrolments,
      ConfidenceLevel.L250
    )

  private val mockRepo = mock[JourneyAnswersRepository]
  private val mockAuthConnector = mock[AuthConnector]

  private val journey: String = "journey"
  private val validTaxYear: Int = 2023
  private val invalidTaxYearInt: Int = 1899
  private val instant = Instant.now.truncatedTo(ChronoUnit.MILLIS)
  private val stubClock = Clock.fixed(instant, ZoneId.systemDefault)
  private val userData = JourneyAnswers(mtdItId, validTaxYear, journey, Json.obj("bar" -> "baz"), Instant.now(stubClock))
  private val taxYear = TaxYear(userData.taxYear)
  private val invalidTaxYear = TaxYear(invalidTaxYearInt)


  override def beforeEach(): Unit = {
    super.beforeEach()
  }

  private val app = new GuiceApplicationBuilder().overrides(
    bind[AppConfig].toInstance(mock[AppConfig]),
    bind[JourneyAnswersRepository].toInstance(mockRepo),
    bind[AuthConnector].toInstance(mockAuthConnector)
  ).build()

  when(mockAuthConnector.authorise[Option[AffinityGroup]](any(), eqTo(affinityGroup))(any(), any()))
    .thenReturn(Future.successful(Some(AffinityGroup.Individual)))

  when(mockAuthConnector.authorise[Enrolments ~ ConfidenceLevel](any(), eqTo(allEnrolments and confidenceLevel))(any(), any()))
    .thenReturn(Future.successful(authResponse))

  ".get" should {

    "return OK and the data when user data can be found for this mtdItId and taxYear" in {

      when(mockRepo.get(eqTo(userData.mtdItId), eqTo(userData.taxYear), eqTo(userData.journey))).thenReturn(Future.successful(Some(userData)))

      val request =
        FakeRequest(GET, routes.JourneyAnswersController.get(journey, taxYear).url)
          .withHeaders("mtditid" -> userData.mtdItId)

      val result = route(app, request).value

      status(result) shouldBe OK
      contentAsJson(result) shouldBe Json.toJson(userData)
    }

    "return NOT_FOUND when user data cannot be found for this mtditid and taxYear" in {

      when(mockRepo.get(any(), any(), any())) thenReturn Future.successful(None)

      val request =
        FakeRequest(GET, routes.JourneyAnswersController.get(journey, taxYear).url)
          .withHeaders("mtditid" -> userData.mtdItId)

      val result = route(app, request).value

      status(result) shouldBe NOT_FOUND
    }

    "return UNAUTHORIZED when the request does not have a mtditid in their header" in {

      val request = FakeRequest(GET, routes.JourneyAnswersController.get(journey, taxYear).url)

      val result = route(app, request).value

      status(result) shouldBe UNAUTHORIZED
    }

    "return BAD_REQUEST when the taxYear is not valid" in {

      val request = FakeRequest(GET, routes.JourneyAnswersController.get(journey, invalidTaxYear).url)
        .withHeaders("mtditid" -> userData.mtdItId)

      val result = route(app, request).value

      status(result) shouldBe BAD_REQUEST
    }

    "return INTERNAL_SERVER_ERROR when throwing an error" in {

      when(mockRepo.get(eqTo(userData.mtdItId), eqTo(userData.taxYear), eqTo(userData.journey))).thenReturn(Future.failed(new Throwable()))

      val request =
        FakeRequest(GET, routes.JourneyAnswersController.get(journey, taxYear).url)
          .withHeaders("mtditid" -> userData.mtdItId)

      val result = route(app, request).value

      status(result) shouldBe INTERNAL_SERVER_ERROR
    }
  }

  ".set" should {

    "return No Content when the data is successfully saved" in {

      when(mockRepo.set(any())) thenReturn Future.successful(Done)

      val request =
        FakeRequest(POST, routes.JourneyAnswersController.set.url)
          .withHeaders(
            "mtditid" -> userData.mtdItId,
            "Content-Type" -> "application/json"
          )
          .withBody(Json.toJson(userData).toString)

      val result = route(app, request).value

      status(result) shouldBe NO_CONTENT
      verify(mockRepo, times(1)).set(eqTo(userData))
    }

    "return Bad Request when the taxYear is invalid" in {

      when(mockRepo.set(any())) thenReturn Future.successful(Done)

      val request =
        FakeRequest(POST, routes.JourneyAnswersController.set.url)
          .withHeaders(
            "mtditid" -> userData.mtdItId,
            "Content-Type" -> "application/json"
          )
          .withBody(Json.toJson(userData.copy(taxYear = invalidTaxYearInt)).toString)

      val result = route(app, request).value

      status(result) shouldBe BAD_REQUEST
    }

    "return UNAUTHORIZED when the request does not have a mtditid in request header" in {

      val request =
        FakeRequest(POST, routes.JourneyAnswersController.set.url)
          .withHeaders("Content-Type" -> "application/json")
          .withBody(Json.toJson(userData))

      val result = route(app, request).value

      status(result) shouldBe UNAUTHORIZED
    }

    "return Bad Request when the request cannot be parsed as UserData" in {

      val badPayload = Json.obj("foo" -> "bar")

      val request =
        FakeRequest(POST, routes.JourneyAnswersController.set.url)
          .withHeaders(
            "mtditid" -> userData.mtdItId,
            "Content-Type" -> "application/json"
          )
          .withBody(badPayload)

      val result = route(app, request).value

      status(result) shouldBe BAD_REQUEST
    }

    "return INTERNAL_SERVER_ERROR when throwing an error" in {

      when(mockRepo.set(any())) thenReturn Future.failed(new Throwable())

      val request =
        FakeRequest(POST, routes.JourneyAnswersController.set.url)
          .withHeaders(
            "mtditid" -> userData.mtdItId,
            "Content-Type" -> "application/json"
          )
          .withBody(Json.toJson(userData).toString)

      val result = route(app, request).value

      status(result) shouldBe INTERNAL_SERVER_ERROR
    }
  }

  ".clear" should {

    "return No Content when data is cleared" in {

      when(mockRepo.clear(eqTo(userData.mtdItId), eqTo(userData.taxYear), eqTo(userData.journey))) thenReturn Future.successful(Done)

      val request =
        FakeRequest(DELETE, routes.JourneyAnswersController.clear(journey, taxYear).url)
          .withHeaders("mtditid" -> userData.mtdItId)

      val result = route(app, request).value

      status(result) shouldBe NO_CONTENT
      verify(mockRepo, times(1)).clear(eqTo(userData.mtdItId), eqTo(userData.taxYear), eqTo(userData.journey))
    }

    "return UNAUTHORIZED when the request does not have a mtditid in request header" in {

      val request =
        FakeRequest(DELETE, routes.JourneyAnswersController.clear(journey, taxYear).url)
          .withBody(Json.toJson(userData))

      val result = route(app, request).value

      status(result) shouldBe UNAUTHORIZED
    }

    "return BAD_REQUEST when the taxYear is invalid" in {

      val request =
        FakeRequest(DELETE, routes.JourneyAnswersController.clear(journey, invalidTaxYear).url)
          .withBody(Json.toJson(userData))

      val result = route(app, request).value

      status(result) shouldBe BAD_REQUEST
    }
  }

  ".keepAlive" should {

    "return No Content when keepAlive updates last updated" in {

      when(mockRepo.keepAlive(eqTo(userData.mtdItId), eqTo(userData.taxYear), eqTo(userData.journey))) thenReturn Future.successful(Done)

      val request =
        FakeRequest(POST, routes.JourneyAnswersController.keepAlive(journey, taxYear).url)
          .withHeaders("mtditid" -> userData.mtdItId)

      val result = route(app, request).value

      status(result) shouldBe NO_CONTENT
      verify(mockRepo, times(1)).clear(eqTo(userData.mtdItId), eqTo(userData.taxYear), eqTo(userData.journey))
    }

    "return UNAUTHORIZED when the request does not have a mtditid in request header" in {

      val request =
        FakeRequest(POST, routes.JourneyAnswersController.keepAlive(journey, taxYear).url)
          .withBody(Json.toJson(userData))

      val result = route(app, request).value

      status(result) shouldBe UNAUTHORIZED
    }

    "return BAD_REQUEST when the taxYear is invalid" in {

      val request =
        FakeRequest(POST, routes.JourneyAnswersController.keepAlive(journey, invalidTaxYear).url)
          .withBody(Json.toJson(userData))

      val result = route(app, request).value

      status(result) shouldBe BAD_REQUEST
    }
  }
}