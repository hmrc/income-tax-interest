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

package services


import connectors.httpParsers.IncomeSourcesDetailsParser.IncomeSourcesDetailsResponse
import connectors.{GetIncomeSourceDetailsConnector, GetIncomeSourceListConnector}
import models.{DesErrorBodyModel, DesErrorModel, NamedInterestDetailsModel}
import play.api.http.Status._
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class GetInterestsService @Inject()(getIncomeSourceListConnector: GetIncomeSourceListConnector,
                                    getIncomeSourceDetailsConnector: GetIncomeSourceDetailsConnector) {


  def getInterestsList(nino: String, taxYear: String)
                      (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Either[DesErrorModel, List[NamedInterestDetailsModel]]] = {
    getIncomeSourceListConnector.getIncomeSourceList(nino, taxYear).flatMap {
      case Right(incomeSourcesModel) =>

        val listOfResponses: Future[List[Either[DesErrorModel, Option[NamedInterestDetailsModel]]]] = Future.sequence(
          incomeSourcesModel.map(incomeSource => {
            getIncomeSourceDetails(nino, taxYear, incomeSource.incomeSourceId).map {
              case Right(interestDetailsModel) =>
                Right(Some(NamedInterestDetailsModel(incomeSource.incomeSourceName, interestDetailsModel.incomeSourceId,
                  interestDetailsModel.taxedUkInterest, interestDetailsModel.untaxedUkInterest)))
              case Left(errorResponse) => handleDetailsError(errorResponse)
            }
          })
        )

        listOfResponses.map{
          listOfResponses =>

            if(listOfResponses.exists(_.isLeft)){

              val error: Option[DesErrorModel] = listOfResponses.filter(_.isLeft).map(_.left.get).headOption
              Left(error.getOrElse(DesErrorModel(INTERNAL_SERVER_ERROR, DesErrorBodyModel.parsingError)))
            } else {
              Right(listOfResponses.filter(_.isRight).flatMap(_.right.get))
            }
        }
      case Left(error) => Future.successful(Left(error))
    }
  }

  def getIncomeSourceDetails(nino: String, taxYear: String, incomeSourceId: String)(implicit hc: HeaderCarrier): Future[IncomeSourcesDetailsResponse] = {
    getIncomeSourceDetailsConnector.getIncomeSourceDetails(nino, taxYear, incomeSourceId)
  }

  private def handleDetailsError(error: DesErrorModel): Either[DesErrorModel, Option[NamedInterestDetailsModel]] = error.status match {
    case NOT_FOUND => Right(None)
    case _ => Left(error)
  }
}
