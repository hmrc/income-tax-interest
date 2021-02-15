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

import connectors.httpParsers.CreateOrAmendInterestHttpParser.CreateOrAmendInterestResponse
import connectors.{CreateIncomeSourceConnector, CreateOrAmendInterestConnector}
import javax.inject.{Inject, Singleton}
import models._
import org.slf4j
import play.api.Logger
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}


@Singleton
class CreateOrAmendInterestService @Inject()(
                                              createOrAmendInterestConnector: CreateOrAmendInterestConnector,
                                              createIncomeSourceConnector: CreateIncomeSourceConnector)(implicit ec: ExecutionContext) {

  lazy val logger: slf4j.Logger = Logger.logger

  def createOrAmendInterest(nino: String, taxYear: Int, submittedInterest: InterestDetailsModel, attempt: Int = 0
                           )(implicit hc: HeaderCarrier): Future[CreateOrAmendInterestResponse] = {
    createOrAmendInterestConnector.createOrAmendInterest(nino, taxYear, submittedInterest).flatMap {
      case Right(true) => Future.successful(Right(true))
      case Left(errorResponse) =>
        logger.info(s"[CreateOrAmendInterestService][createOrAmendInterest] - Failed to update income Source - Attempt number $attempt")
        if (attempt< 2) {
          createOrAmendInterest(nino, taxYear, submittedInterest, attempt + 1)
        }else{
          Future.successful(Left(errorResponse))
        }
    }
  }

  def getIncomeSourceId(nino: String, interestSubmittedModel: CreateOrAmendInterestModel, attempt: Int = 0)
                       (implicit hc: HeaderCarrier): Future[Either[DesErrorModel, InterestDetailsModel]] = {
    if (interestSubmittedModel.id.isEmpty) {
      createIncomeSourceConnector.createIncomeSource(nino, InterestSubmissionModel(incomeSourceName = interestSubmittedModel.accountName)).flatMap {
        case Right(incomeSourceIdModel) =>
          Future.successful(Right(
            InterestDetailsModel(incomeSourceIdModel.incomeSourceId, interestSubmittedModel.taxedUkInterest, interestSubmittedModel.untaxedUkInterest)
          ))
        case Left(errorResponse) =>
          logger.info(s"[CreateOrAmendInterestService][getIncomeSourceId] - Failed to create new income Source - Attempt number $attempt")
          if (attempt < 2) {
            getIncomeSourceId(nino, interestSubmittedModel, attempt + 1)
          } else {
            Future.successful(Left(errorResponse))
          }
      }
    } else {
      Future.successful(Right(
        InterestDetailsModel(interestSubmittedModel.id.get, interestSubmittedModel.taxedUkInterest, interestSubmittedModel.untaxedUkInterest)
      ))
    }
  }

  def createOrAmendAllInterest(nino: String, taxYear: Int, submittedModel: Seq[CreateOrAmendInterestModel]
                              )(implicit hc: HeaderCarrier): Future[Seq[CreateOrAmendInterestResponse]] = {

    Future.sequence(submittedModel.map { model =>
      getIncomeSourceId(nino, model).flatMap {
        case Right(value) =>
          createOrAmendInterest(nino, taxYear, value)
        case Left(errorResponse) =>
          Future.successful(Left(errorResponse))
      }
    })
  }

}
