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

import uk.gov.hmrc.http.HeaderNames
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import java.util.UUID

trait HipConnector {

  val config: ServicesConfig
  val hipBaseUrl: String = config.baseUrl("hip")

  private val hipAuthToken: String = config.getString("microservice.services.hip.authorisation-token")
  private val hipAuthType: String = config.getString("microservice.services.hip.authType")

  def hipHeaders: Seq[(String, String)] = Seq(
    "correlationId" -> UUID.randomUUID().toString,
    HeaderNames.authorisation -> s"$hipAuthType $hipAuthToken"
  )

}
