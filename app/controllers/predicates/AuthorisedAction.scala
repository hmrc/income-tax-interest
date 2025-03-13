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

import common.{DelegatedAuthRules, EnrolmentIdentifiers, EnrolmentKeys}
import config.AppConfig
import models.User
import play.api.Logging
import play.api.mvc.Results.{InternalServerError, Unauthorized}
import play.api.mvc._
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals.{affinityGroup, allEnrolments, confidenceLevel}
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.http.{HeaderCarrier, SessionKeys}
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import utils.HMRCHeaderNames.CorrelationId

import java.util.UUID
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class AuthorisedAction @Inject()()(implicit val authConnector: AuthConnector,
                                   defaultActionBuilder: DefaultActionBuilder,
                                   val appConfig: AppConfig,
                                   val cc: ControllerComponents) extends AuthorisedFunctions with Logging {

  implicit val executionContext: ExecutionContext = cc.executionContext

  val unauthorized: Future[Result] = Future.successful(Unauthorized)


  def async(block: User[AnyContent] => Future[Result]): Action[AnyContent] = defaultActionBuilder.async { implicit request =>

    implicit lazy val headerCarrier: HeaderCarrier = HeaderCarrierConverter.fromRequest(request)
      .withExtraHeaders(CorrelationId -> correlationId(request.headers.get(CorrelationId)))

    request.headers.get("mtditid").fold {
      logger.warn("[AuthorisedAction][async] - No MTDITID in the header. Returning unauthorised.")
      unauthorized
    }(
      mtdItId =>
        authorised().retrieve(affinityGroup) {
          case Some(AffinityGroup.Agent) => agentAuthentication(block, mtdItId)(request, headerCarrier)
          case _ => individualAuthentication(block, mtdItId)(request, headerCarrier)
        } recover {
          case _: NoActiveSession =>
            logger.info(s"[AuthorisedAction][async] - No active session.")
            Unauthorized
          case _: AuthorisationException =>
            logger.warn(s"[AuthorisedAction][async] - User failed to authenticate")
            Unauthorized
          case e =>
            logger.error(s"[AuthorisedAction][async] - Unexpected exception of type '${e.getClass.getSimpleName}' was caught.")
            InternalServerError
        }
    )
  }

  private def correlationId(correlationIdHeader: Option[String]): String = {

    if (correlationIdHeader.isDefined) {
      logger.info("[AuthorisedAction]Valid CorrelationId header found.")
      correlationIdHeader.get
    } else {
      lazy val id = UUID.randomUUID().toString
      logger.warn(s"[AuthorisedAction]No valid CorrelationId found in headers. Defaulting Correlation Id. $id")
      id
    }
  }
  val minimumConfidenceLevel: Int = ConfidenceLevel.L250.level

  private[predicates] def individualAuthentication[A](block: User[A] => Future[Result], requestMtdItId: String)
                                                     (implicit request: Request[A], hc: HeaderCarrier): Future[Result] = {
    authorised().retrieve(allEnrolments and confidenceLevel) {
      case enrolments ~ userConfidence if userConfidence.level >= minimumConfidenceLevel =>
        val optionalMtdItId: Option[String] = enrolmentGetIdentifierValue(EnrolmentKeys.Individual, EnrolmentIdentifiers.individualId, enrolments)
        val optionalNino: Option[String] = enrolmentGetIdentifierValue(EnrolmentKeys.nino, EnrolmentIdentifiers.nino, enrolments)

        (optionalMtdItId, optionalNino) match {
          case (Some(authMTDITID), Some(_)) =>
            sessionIdFrom(request, hc) match {
              case Some(sessionId) =>
                enrolments.enrolments.collectFirst {
                  case Enrolment(EnrolmentKeys.Individual, enrolmentIdentifiers, _, _)
                    if enrolmentIdentifiers.exists(identifier => identifier.key == EnrolmentIdentifiers.individualId && identifier.value == requestMtdItId) =>
                    block(User(requestMtdItId, None))
                } getOrElse {
                  logger.warn(s"[AuthorisedAction][individualAuthentication] Non-agent with an invalid MTDITID. " +
                    s"MTDITID in auth matches MTDITID in request: ${authMTDITID == requestMtdItId}")
                  unauthorized
                }
              case None =>
                logger.warn(s"[AuthorisedAction] - No session id in request")
                unauthorized
            }

          case (_, None) =>
            logger.warn(s"[AuthorisedAction][individualAuthentication] - User has no nino.")
            unauthorized
          case (None, _) =>
            logger.warn(s"[AuthorisedAction][individualAuthentication] - User has no MTD IT enrolment.")
            unauthorized
        }
      case _ =>
        logger.warn("[AuthorisedAction][individualAuthentication] User has confidence level below 250.")
        unauthorized
    }
  }

  private[predicates] def agentAuthPredicate(mtdId: String) =
    Enrolment(EnrolmentKeys.Individual)
      .withIdentifier("MTDITID", mtdId)
      .withDelegatedAuthRule(DelegatedAuthRules.agentDelegatedAuthRule)

  private[predicates] def agentAuthentication[A](block: User[A] => Future[Result], mtdItId: String)
                                                (implicit request: Request[A], hc: HeaderCarrier): Future[Result] = {
    authorised(agentAuthPredicate(mtdItId))
      .retrieve(allEnrolments) { enrolments =>
        populateAgent(block, mtdItId, None, enrolments)
      }.recoverWith(agentRecovery(block, mtdItId))
  }

  private def agentRecovery[A](block: User[A] => Future[Result], mtdItId: String)
                              (implicit request: Request[A], hc: HeaderCarrier): PartialFunction[Throwable, Future[Result]] = {
    case _: NoActiveSession =>
      logger.info(s"[AuthorisedAction][agentAuthentication] - No active session.")
      unauthorized
    case _: AuthorisationException =>
      logger.warn(s"[AuthorisedAction][agentAuthentication] - Agent does not have delegated authority for Client.")
      unauthorized
    case e =>
      logger.error(s"[AuthorisedAction][agentAuthentication] - Unexpected exception of type '${e.getClass.getSimpleName}' was caught.")
      Future(InternalServerError)
  }

  private def populateAgent[A](block: User[A] => Future[Result],
                               mtdItId: String,
                               nino: Option[String],
                               enrolments: Enrolments)(implicit request: Request[A], hc: HeaderCarrier): Future[Result] = {
    enrolmentGetIdentifierValue(EnrolmentKeys.Agent, EnrolmentIdentifiers.agentReference, enrolments) match {
      case Some(arn) =>
        sessionIdFrom(request, hc).fold {
          logger.warn(s"[AuthorisedAction][agentAuthentication] - No session id in request.")
          unauthorized
        } { sessionId =>
          block(User(mtdItId, Some(arn)))
        }
      case None =>
        logger.warn("[AuthorisedAction][agentAuthentication] Agent with no HMRC-AS-AGENT enrolment.")
        unauthorized
    }
  }

  private[predicates] def enrolmentGetIdentifierValue(checkedKey: String,
                                                      checkedIdentifier: String,
                                                      enrolments: Enrolments): Option[String] = enrolments.enrolments.collectFirst {
    case Enrolment(`checkedKey`, enrolmentIdentifiers, _, _) => enrolmentIdentifiers.collectFirst {
      case EnrolmentIdentifier(`checkedIdentifier`, identifierValue) => identifierValue
    }
  }.flatten

  private def sessionIdFrom(request: Request[_], hc: HeaderCarrier): Option[String] = hc.sessionId match {
    case Some(sessionId) => Some(sessionId.value)
    case _ => request.headers.get(SessionKeys.sessionId)
  }

}
