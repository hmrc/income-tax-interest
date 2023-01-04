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

package services

import connectors.httpParsers.IncomeSourcesDetailsParser.IncomeSourcesDetailsResponse
import connectors.{GetIncomeSourceDetailsConnector, GetIncomeSourceListConnector, GetAnnualIncomeSourcePeriodConnector}
import models.{ErrorBodyModel, ErrorModel, IncomeSourceModel, NamedInterestDetailsModel}
import play.api.http.Status._
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class GetInterestsService @Inject()(getIncomeSourceListConnector: GetIncomeSourceListConnector,
                                    getIncomeSourceDetailsConnector: GetIncomeSourceDetailsConnector,
                                    getAnnualIncomeSourcePeriodConnector: GetAnnualIncomeSourcePeriodConnector)(implicit ec: ExecutionContext) {


  def getInterestsList(nino: String, taxYear: String)
                      (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Either[ErrorModel, List[NamedInterestDetailsModel]]] = {
    getIncomeSourceListConnector.getIncomeSourceList(nino).flatMap {
      case Right(incomeSourcesModel) =>

        val listOfResponses: Future[List[Either[ErrorModel, Option[NamedInterestDetailsModel]]]] = Future.sequence(
          incomeSourcesModel.map(incomeSource => {
            getIncomeSourceDetails(nino, taxYear, incomeSource.incomeSourceId).map {
              case Right(interestDetailsModel) =>
                Right(Some(NamedInterestDetailsModel(incomeSource.incomeSourceName, interestDetailsModel.incomeSourceId,
                  interestDetailsModel.taxedUkInterest, interestDetailsModel.untaxedUkInterest)))
              case Left(errorResponse) => handleDetailsError(incomeSource, errorResponse)
            }
          })
        )

        listOfResponses.map {
          listOfResponses =>

            if (listOfResponses.exists(_.isLeft)) {

              val error: Option[ErrorModel] = listOfResponses.filter(_.isLeft).map(_.left.get).headOption
              Left(error.getOrElse(ErrorModel(INTERNAL_SERVER_ERROR, ErrorBodyModel.parsingError)))
            } else {
              Right(listOfResponses.filter(_.isRight).flatMap(_.right.get))
            }
        }
      case Left(error) => Future.successful(Left(error))
    }
  }

  def getIncomeSourceDetails(nino: String, taxYear: String, incomeSourceId: String)(implicit hc: HeaderCarrier): Future[IncomeSourcesDetailsResponse] = {
    if (taxYear.equals("2024")) {
      getAnnualIncomeSourcePeriodConnector.getAnnualIncomeSourcePeriod(nino, taxYear, incomeSourceId, Some(false))
    }
    else {
      getIncomeSourceDetailsConnector.getIncomeSourceDetails(nino, taxYear, incomeSourceId)
    }
  }

  private def handleDetailsError(incomeSource: IncomeSourceModel,
                                 error: ErrorModel): Either[ErrorModel, Option[NamedInterestDetailsModel]] = error.status match {
    case NOT_FOUND => Right(Some(NamedInterestDetailsModel(incomeSource.incomeSourceName, incomeSource.incomeSourceId, None, None)))
    case _ => Left(error)
  }
}
