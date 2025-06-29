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

import com.google.inject.ImplementedBy
import connectors.httpParsers.CreateIncomeSourcesHttpParser._
import models.InterestSubmissionModel
import play.api.libs.json.Json
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, StringContextOps}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@ImplementedBy(classOf[CreateIncomeSourceConnectorImpl])
trait CreateIncomeSourceConnector {
  def createIncomeSource(nino: String,
                         interestSubmissionModel: InterestSubmissionModel
                        )(implicit hc: HeaderCarrier): Future[CreateIncomeSourcesResponse]
}

@Singleton
class CreateIncomeSourceConnectorImpl @Inject()(httpClient: HttpClientV2,
                                                val config: ServicesConfig
                                               )(implicit executionContext: ExecutionContext) extends CreateIncomeSourceConnector with HipConnector {

  def createIncomeSource(nino: String,
                         interestSubmissionModel: InterestSubmissionModel
                        )(implicit hc: HeaderCarrier): Future[CreateIncomeSourcesResponse] = {
    httpClient
      .post(url"$hipBaseUrl/itsd/income-sources/$nino")
      .setHeader(hipHeaders: _*)
      .withBody(Json.toJson(interestSubmissionModel))
      .execute[CreateIncomeSourcesResponse]
  }
}
