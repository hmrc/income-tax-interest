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
import connectors.httpParsers.CreateOrAmendInterestHttpParser._
import models.InterestDetailsModel
import play.api.libs.json.Format.GenericFormat
import play.api.libs.json.Json
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, StringContextOps}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CreateOrAmendInterestConnector @Inject()(http: HttpClientV2, val appConfig: AppConfig)(implicit ec: ExecutionContext) extends DesConnector {

  def createOrAmendInterest(
                             nino: String, taxYear: Int, interestModel: InterestDetailsModel
                           )(implicit hc: HeaderCarrier): Future[CreateOrAmendInterestResponse] = {
    val createOrAmendInterestUrl: String = appConfig.desBaseUrl + s"/income-tax/nino/$nino/income-source/savings/" +
      s"annual/$taxYear"

    def desCall(implicit hc: HeaderCarrier): Future[CreateOrAmendInterestResponse] = {
      http.post(url"$createOrAmendInterestUrl").withBody(Json.toJson(interestModel)).execute[CreateOrAmendInterestResponse]
    }

    desCall(desHeaderCarrier(createOrAmendInterestUrl))
  }
}
