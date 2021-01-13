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

import connectors.httpParsers.IncomeSourceListParser.IncomeSourceListException
import connectors.httpParsers.{IncomeSourceListParser, IncomeSourcesDetailsParser}
import connectors.httpParsers.IncomeSourcesDetailsParser.InterestDetailsException
import controllers.predicates.AuthorisedAction
import javax.inject.Inject
import models.NamedInterestDetailsModel
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, ControllerComponents, Result}
import services.GetInterestsService
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}

class GetInterestsController @Inject()(cc: ControllerComponents,
                                          authorisedAction: AuthorisedAction,
                                          getInterestsService: GetInterestsService)
                                         (implicit ec: ExecutionContext) extends BackendController(cc){

  def getIncomeSource(nino: String, taxYear: String, mtditid: String): Action[AnyContent] = authorisedAction.async(mtditid) { implicit user =>
    getInterestsService.getInterestsList(nino, taxYear).map{
        case Right(interestList) => Ok(interestList)
        case Left(exception) => handleListFetchExceptions(exception)
      }
  }
  //move logic into singular service.

  private def handleListFetchExceptions(exception: IncomeSourceListException): Result = exception match {
    case IncomeSourceListParser.IncomeSourcesInvalidJson => InternalServerError("")
    case IncomeSourceListParser.InvalidSubmission => BadRequest("")
    case IncomeSourceListParser.NotFoundException => NotFound("")
    case IncomeSourceListParser.InternalServerErrorUpstream => InternalServerError("")
    case IncomeSourceListParser.UpstreamServiceUnavailable => ServiceUnavailable("")
    case IncomeSourceListParser.UnexpectedStatus => InternalServerError("")
  }

}
