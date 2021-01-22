/*
 * Copyright 2021 HM Revenue & Customs
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
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import services.GetInterestsService
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class GetInterestsController @Inject()(cc: ControllerComponents,
                                          authorisedAction: AuthorisedAction,
                                          getInterestsService: GetInterestsService)
                                         (implicit ec: ExecutionContext) extends BackendController(cc){

  def getIncomeSource(nino: String, taxYear: String, mtditid: String): Action[AnyContent] = authorisedAction.async(mtditid) { implicit user =>
    getInterestsService.getInterestsList(nino, taxYear).map{
        case Right(interestList) => Ok(Json.toJson(interestList))
        case Left(errorModel) => Status(errorModel.status)(Json.toJson(errorModel.body))
      }
  }

}
