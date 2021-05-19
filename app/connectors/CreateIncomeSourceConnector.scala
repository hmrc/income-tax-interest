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

package connectors

import config.AppConfig
import connectors.httpParsers.CreateIncomeSourcesHttpParser._
import javax.inject.Inject
import models.InterestSubmissionModel
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient}

import scala.concurrent.{ExecutionContext, Future}

class CreateIncomeSourceConnector @Inject()(http: HttpClient, val appConfig: AppConfig)(implicit ec: ExecutionContext) extends DesConnector {

  def createIncomeSource(
                          nino: String, interestSubmissionModel: InterestSubmissionModel
                        )(implicit hc: HeaderCarrier): Future[CreateIncomeSourcesResponse] = {
    val createIncomeSourceUrl: String = appConfig.desBaseUrl + s"/income-tax/income-sources/nino/$nino"

    def desCall(implicit hc: HeaderCarrier): Future[CreateIncomeSourcesResponse] = {
      http.POST[InterestSubmissionModel, CreateIncomeSourcesResponse](createIncomeSourceUrl, interestSubmissionModel)
    }

    desCall(desHeaderCarrier(createIncomeSourceUrl))
  }
}
