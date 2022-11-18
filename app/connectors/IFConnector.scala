/*
 * Copyright 2022 HM Revenue & Customs
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

import com.typesafe.config.ConfigFactory
import config.AppConfig
import uk.gov.hmrc.http.HeaderCarrier.Config
import uk.gov.hmrc.http.{Authorization, HeaderCarrier}
import utils.HeaderCarrierSyntax.HeaderCarrierSyntax

import java.net.URL

trait IFConnector {

  protected val appConfig: AppConfig
  protected[connectors] lazy val baseUrl: String = appConfig.ifBaseUrl
  val GetAnnualIncomeSourcePeriod = "1785"

  protected val headerCarrierConfig: Config = HeaderCarrier.Config.fromConfig(ConfigFactory.load())

  protected[connectors] def ifHeaderCarrier(url: String, apiVersion: String)(implicit hc: HeaderCarrier): HeaderCarrier = {
    val isInternalHost = headerCarrierConfig.internalHostPatterns.exists(_.pattern.matcher(new URL(url).getHost).matches())
    val hcWithAuth = hc.copy(authorization = Some(Authorization(s"Bearer ${appConfig.authorisationTokenFor(apiVersion)}")))

    if (isInternalHost) {
      hcWithAuth.withExtraHeaders(headers = "Environment" -> appConfig.ifEnvironment)
    } else {
      hcWithAuth.withExtraHeaders(("Environment" -> appConfig.ifEnvironment) +: hcWithAuth.addExplicitHeaders: _*)
    }
  }
}
