/*
 * Copyright 2020 HM Revenue & Customs
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

package services

import connectors.httpParsers.IncomeSourceListParser.IncomeSourceListException
import connectors.httpParsers.{IncomeSourceListParser, IncomeSourcesDetailsParser}
import connectors.httpParsers.IncomeSourcesDetailsParser.{IncomeSourcesDetailsResponse, InterestDetailsException}
import connectors.{GetIncomeSourceDetailsConnector, GetIncomeSourceListConnector}
import javax.inject.Inject
import models.NamedInterestDetailsModel
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration.Duration

class GetInterestsService @Inject()(getIncomeSourceListConnector: GetIncomeSourceListConnector,
                                    getIncomeSourceDetailsConnector: GetIncomeSourceDetailsConnector) {

  def getInterestsList(nino: String, taxYear: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Either[IncomeSourceListException, JsValue]] = {
    getIncomeSourceListConnector.getIncomeSourceList(nino, taxYear).flatMap {
      case Right(incomeSourcesModel) => Future.sequence(incomeSourcesModel.map(incomeSource => {
        getIncomeSourceDetails(nino, taxYear, incomeSource.incomeSourceId).map {
          case Right(interestDetailsModel) =>
            NamedInterestDetailsModel(incomeSource.incomeSourceName, interestDetailsModel.incomeSourceId,
              interestDetailsModel.taxedUkInterest, interestDetailsModel.untaxedUkInterest)
          case Left(exception) =>
            handleDetailsFetchException(exception)
        }
      })).map(interestList => Right(Json.toJson(interestList.filter(_.incomeSourceId != "NotFound"))))
      case Left(exception) => Future.successful(Left(exception))
    }.recover {
      case _ => Left(IncomeSourceListParser.IncomeSourcesInvalidJson)
    }
  }

  def getIncomeSourceDetails(nino: String, taxYear: String, incomeSourceId: String)(implicit hc: HeaderCarrier): Future[IncomeSourcesDetailsResponse] = {
    getIncomeSourceDetailsConnector.getIncomeSourceDetails(nino, taxYear, incomeSourceId)
  }

  private def handleDetailsFetchException(exception: InterestDetailsException) = exception match {
    case IncomeSourcesDetailsParser.InvalidSubmission => throw new Exception
    case IncomeSourcesDetailsParser.NotFoundException => NamedInterestDetailsModel("NotFound", "NotFound", None, None)
    case IncomeSourcesDetailsParser.InternalServerErrorUpstream => throw new Exception
    case IncomeSourcesDetailsParser.ServiceUnavailable => throw new Exception
    case IncomeSourcesDetailsParser.UnexpectedStatus => throw new Exception
  }

}
