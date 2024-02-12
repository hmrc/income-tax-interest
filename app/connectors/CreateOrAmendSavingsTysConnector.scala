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
import connectors.httpParsers.CreateOrAmendSavingsHttpParser._
import models.CreateOrAmendSavingsModel
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient}
import utils.TaxYearUtils.convertSpecificTaxYear

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CreateOrAmendSavingsTysConnector @Inject()(http: HttpClient, val appConfig: AppConfig)(implicit ec: ExecutionContext) extends IFConnector {

  private val apiNumber = "1903"
  def createOrAmendSavings(nino: String, taxYear: Int, savingsModel: CreateOrAmendSavingsModel
                          )(implicit hc: HeaderCarrier): Future[CreateOrAmendSavingsResponse] = {

    val createOrAmendSavingsUrl: String =
      appConfig.ifBaseUrl + s"/income-tax/income/savings/$nino/${convertSpecificTaxYear(taxYear)}"

    def iFCall(implicit hc: HeaderCarrier): Future[CreateOrAmendSavingsResponse] =
      http.PUT[CreateOrAmendSavingsModel, CreateOrAmendSavingsResponse](createOrAmendSavingsUrl, savingsModel)

    iFCall(ifHeaderCarrier(createOrAmendSavingsUrl, apiNumber))
  }
}

