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
import connectors.httpParsers.DeleteSavingsIncomeDataParser.{DeleteSavingsIncomeDataHttpReads, DeleteSavingsIncomeDataResponse}
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, StringContextOps}
import utils.TaxYearUtils.convertSpecificTaxYear

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class DeleteSavingsIncomeDataTysConnector @Inject()(http: HttpClientV2, val appConfig: AppConfig)(implicit ec: ExecutionContext) extends IFConnector {

  private val apiNumber = "1905"
  def deleteSavingsIncomeData(nino: String, taxYear: Int)(implicit hc: HeaderCarrier): Future[DeleteSavingsIncomeDataResponse] = {

    val savingsIncomeDataUrl: String =
      appConfig.ifBaseUrl + s"/income-tax/income/savings/${convertSpecificTaxYear(taxYear)}/$nino"

    http.delete(url"$savingsIncomeDataUrl")(ifHeaderCarrier(savingsIncomeDataUrl, apiNumber)).execute[DeleteSavingsIncomeDataResponse](DeleteSavingsIncomeDataHttpReads, ec)
  }
}
