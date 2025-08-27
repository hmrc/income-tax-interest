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
import connectors.httpParsers.CreateOrAmendAnnualIncomeSourcePeriodHttpParser._
import models.InterestDetailsModel
import play.api.libs.json.Json
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, StringContextOps}
import utils.TaxYearUtils.convertSpecificTaxYear

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CreateOrAmendAnnualIncomeSourcePeriodConnector @Inject()(http: HttpClientV2, val appConfig: AppConfig)(implicit ec: ExecutionContext) extends IFConnector {

  val createOrAmendAnnualIncomeSourcePeriod = "1784"

  def createOrAmendAnnualIncomeSourcePeriod(
                             nino: String, taxYear: Int, interestModel: InterestDetailsModel
                           )(implicit hc: HeaderCarrier): Future[CreateOrAmendAnnualIncomeSourcePeriodResponse] = {
    val taxYearParameter = convertSpecificTaxYear(taxYear)
    val createOrAmendAnnualIncomeSourcePeriodUrl: String = appConfig.ifBaseUrl + s"/income-tax/$taxYearParameter/$nino/income-source/savings/annual"

    def IFCall(implicit hc: HeaderCarrier): Future[CreateOrAmendAnnualIncomeSourcePeriodResponse] = {
      http.post(url"$createOrAmendAnnualIncomeSourcePeriodUrl").withBody(Json.toJson(interestModel)).execute[CreateOrAmendAnnualIncomeSourcePeriodResponse]
    }

    IFCall(ifHeaderCarrier(createOrAmendAnnualIncomeSourcePeriodUrl, createOrAmendAnnualIncomeSourcePeriod))
  }
}
