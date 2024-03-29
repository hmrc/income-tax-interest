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

import connectors.httpParsers.CreateOrAmendInterestHttpParser.CreateOrAmendInterestResponse
import connectors.{CreateIncomeSourceConnector, CreateOrAmendAnnualIncomeSourcePeriodConnector, CreateOrAmendInterestConnector}
import models._
import play.api.Logging
import play.api.http.Status.isServerError
import uk.gov.hmrc.http.HeaderCarrier
import utils.PagerDutyHelper.{getPagerKeyFromInt, pagerDutyLog}
import utils.TaxYearUtils.specificTaxYear

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}


@Singleton
class CreateOrAmendInterestService @Inject()(
                                              createOrAmendInterestConnector: CreateOrAmendInterestConnector,
                                              createOrAmendAnnualIncomeSourcePeriodConnector:CreateOrAmendAnnualIncomeSourcePeriodConnector,
                                              createIncomeSourceConnector: CreateIncomeSourceConnector)(implicit ec: ExecutionContext) extends Logging {


  def createOrAmendInterest(nino: String, taxYear: Int, submittedInterest: InterestDetailsModel, attempt: Int = 0
                           )(implicit hc: HeaderCarrier): Future[CreateOrAmendInterestResponse] = {
    if (taxYear >= specificTaxYear) {
      taxYearSpecificCreateInterest(nino, taxYear, submittedInterest, attempt)
    }
    else {
      createOrAmendInterestConnector.createOrAmendInterest(nino, taxYear, submittedInterest).flatMap {
        case Right(true) => Future.successful(Right(true))
        case Left(errorResponse) if isServerError(errorResponse.status) && attempt < 2 => createOrAmendInterest(nino, taxYear, submittedInterest, attempt + 1)
        case Left(errorResponse) => logAndReturn(errorResponse, "[CreateOrAmendInterestService][createOrAmendInterest]")
      }
    }
  }


  private def taxYearSpecificCreateInterest(nino: String, taxYear: Int, submittedInterest: InterestDetailsModel, attempt: Int
                                   )(implicit hc: HeaderCarrier): Future[CreateOrAmendInterestResponse] = {
    createOrAmendAnnualIncomeSourcePeriodConnector.createOrAmendAnnualIncomeSourcePeriod(nino, taxYear, submittedInterest).flatMap {
      case Right(true) => Future.successful(Right(true))
      case Left(errorResponse) if isServerError(errorResponse.status) && attempt < 2 => createOrAmendInterest(nino, taxYear, submittedInterest, attempt + 1)
      case Left(errorResponse) => logAndReturn(errorResponse, "[CreateOrAmendInterestService][createOrAmendInterest]")
    }
  }

  def getIncomeSourceId(nino: String, interestSubmittedModel: CreateOrAmendInterestModel, attempt: Int = 0)
                       (implicit hc: HeaderCarrier): Future[Either[ErrorModel, InterestDetailsModel]] = {
    if (interestSubmittedModel.id.isEmpty) {
      createIncomeSourceConnector.createIncomeSource(nino, InterestSubmissionModel(incomeSourceName = interestSubmittedModel.accountName)).flatMap {
        case Right(incomeSourceIdModel) =>
          Future.successful(Right(
            InterestDetailsModel(incomeSourceIdModel.incomeSourceId, interestSubmittedModel.taxedUkInterest, interestSubmittedModel.untaxedUkInterest)
          ))
        case Left(errorResponse) if isServerError(errorResponse.status) && attempt < 2 => getIncomeSourceId(nino, interestSubmittedModel, attempt + 1)
        case Left(errorResponse) => logAndReturn(errorResponse, "[CreateOrAmendInterestService][getIncomeSourceId]")
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

  private def logAndReturn(errorResponse: ErrorModel, logKey: String) = {
    pagerDutyLog(
      getPagerKeyFromInt(errorResponse.status),
      s"$logKey Received ${errorResponse.status} from DES. Body:${errorResponse.body}"
    )
    Future.successful(Left(errorResponse))
  }

}
