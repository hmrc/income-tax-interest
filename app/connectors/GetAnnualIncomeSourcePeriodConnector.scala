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
import connectors.httpParsers.GetSubmittedInterestIfHttpParser.{GetAnnualIncomeSourcePeriod, GetAnnualIncomeSourcePeriodReads}
import uk.gov.hmrc.http.{HeaderCarrier, StringContextOps}
import uk.gov.hmrc.http.client.HttpClientV2
import utils.TaxYearUtils.convertSpecificTaxYear

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class GetAnnualIncomeSourcePeriodConnector @Inject()(val http: HttpClientV2,
                                                     val appConfig: AppConfig)(implicit ec:ExecutionContext) extends IFConnector {
  val GetAnnualIncomeSourcePeriod = "1785"
  def getAnnualIncomeSourcePeriod(nino: String,
                                  taxYear: String,
                                  incomeSourceId: String,
                                  deletedPeriod: Option[Boolean])(implicit hc: HeaderCarrier): Future[GetAnnualIncomeSourcePeriod] = {
    val taxYearParameter = convertSpecificTaxYear(taxYear)

    val incomeSourcesUri: String = appConfig.ifBaseUrl + deletedPeriod.map {
      period => s"/income-tax/$taxYearParameter/$nino/income-source/savings/annual?deleteReturnPeriod=$period&incomeSourceId=$incomeSourceId"
    }.getOrElse {
      s"/income-tax/$taxYearParameter/$nino/income-source/savings/annual?incomeSourceId=$incomeSourceId"
    }

    http.get(url"$incomeSourcesUri")(ifHeaderCarrier(incomeSourcesUri, GetAnnualIncomeSourcePeriod)).execute[GetAnnualIncomeSourcePeriod]
  }
}