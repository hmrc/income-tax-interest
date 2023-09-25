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

package connectors

import config.AppConfig
import connectors.httpParsers.CreateOrAmendAnnualIncomeSourcePeriodHttpParser.CreateOrAmendAnnualIncomeSourcePeriodResponse
import connectors.httpParsers.SavingsIncomeDataParser.{IncomeSourceHttpReads, SavingsIncomeDataResponse}
import models.InterestDetailsModel

import javax.inject.Inject
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient}

import scala.concurrent.{ExecutionContext, Future}

class GetSavingsIncomeDataConnector @Inject()(http: HttpClient, val appConfig: AppConfig)(implicit ec: ExecutionContext) extends IFConnector {

  val apiNumber = "1606"
  def getSavingsIncomeData(nino: String, taxYear: Int)(implicit hc: HeaderCarrier): Future[SavingsIncomeDataResponse] = {
    val taxYearParameter = s"${taxYear - 1}-${taxYear.toString takeRight 2}"
    val savingsIncomeDataUrl = appConfig.desBaseUrl +
      s"/income-tax/income/savings/$nino/$taxYearParameter"

    http.GET[SavingsIncomeDataResponse](savingsIncomeDataUrl)(IncomeSourceHttpReads, ifHeaderCarrier(savingsIncomeDataUrl, apiNumber), ec)

  }
}
