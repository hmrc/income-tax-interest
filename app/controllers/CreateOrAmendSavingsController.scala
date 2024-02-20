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

import connectors.httpParsers.CreateOrAmendSavingsHttpParser.CreateOrAmendSavingsResponse
import controllers.predicates.AuthorisedAction

import javax.inject.Inject
import models.CreateOrAmendSavingsModel
import play.api.libs.json.{JsSuccess, Json}
import play.api.mvc.{Action, AnyContent, ControllerComponents, Result}
import services.CreateOrAmendSavingsService
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import scala.concurrent.{ExecutionContext, Future}

class CreateOrAmendSavingsController @Inject()(createOrAmendSavingsService: CreateOrAmendSavingsService,
                                               cc: ControllerComponents,
                                               authorisedAction: AuthorisedAction)
                                              (implicit ec: ExecutionContext) extends BackendController(cc) {

  def createOrAmendSavings(nino: String, taxYear: Int): Action[AnyContent] = authorisedAction.async { implicit user =>
    user.request.body.asJson.map(_.validate[CreateOrAmendSavingsModel]) match {
      case Some(JsSuccess(model, _)) =>
        responseHandler(createOrAmendSavingsService.createOrAmendSavings(nino, taxYear, model))
      case _ => Future.successful(BadRequest)
    }
  }

  private def responseHandler(serviceResponse: Future[CreateOrAmendSavingsResponse]): Future[Result] = {
    serviceResponse.map {
      case Right(responseModel) => NoContent
      case Left(errorModel) => Status(errorModel.status)(Json.toJson(errorModel.toJson))
    }
  }
}
