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


import controllers.predicates.AuthorisedAction
import models.CreateOrAmendInterestModel
import play.api.Logging
import play.api.libs.json.{JsError, JsSuccess}
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import services.CreateOrAmendInterestService
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CreateOrAmendInterestController @Inject()(createOrAmendInterestService: CreateOrAmendInterestService,
                                                cc: ControllerComponents,
                                                authorisedAction: AuthorisedAction)
                                               (implicit ec: ExecutionContext) extends BackendController(cc) with Logging {

  def createOrAmendInterest(nino: String, taxYear: Int): Action[AnyContent] = authorisedAction.async { implicit user =>
    user.request.body.asJson.map(_.validate[Seq[CreateOrAmendInterestModel]]) match {
      case Some(JsSuccess(model, _)) =>
        createOrAmendInterestService.createOrAmendAllInterest(nino, taxYear, model).map {
          _.collectFirst {
            case Left(error) => Status(error.status)(error.toJson)
          }.getOrElse(NoContent)
        }
      case Some(JsError(errors)) =>
        logger.info("Error in parsing request body. Errors: " + errors.mkString("\n"))
        Future.successful(BadRequest)
      case None =>
        logger.info("Empty request body")
        Future.successful(BadRequest)
    }

  }

}
