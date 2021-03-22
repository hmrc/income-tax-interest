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

package connectors.httpParsers

import models._
import org.slf4j
import play.api.Logger
import play.api.http.Status._
import uk.gov.hmrc.http.{HttpReads, HttpResponse}

object CreateIncomeSourcesHttpParser extends DESParser {
  type CreateIncomeSourcesResponse = Either[DesErrorModel, IncomeSourceIdModel]

  lazy val logger: slf4j.Logger = Logger.logger

  override val parserName: String = "CreateIncomeSourcesParser"

  implicit object CreateIncomeSourcesHttpReads extends HttpReads[CreateIncomeSourcesResponse] {
    override def read(method: String, url: String, response: HttpResponse): CreateIncomeSourcesResponse = {
      response.status match {
        case OK => response.json.validate[IncomeSourceIdModel].fold[CreateIncomeSourcesResponse](
          jsonErrors => badSuccessJsonFromDES,
          parsedModel => Right(parsedModel)
        )
        case INTERNAL_SERVER_ERROR =>
          logger.error(logMessage(response).get)
          handleDESError(response)
        case SERVICE_UNAVAILABLE =>
          logger.error(logMessage(response).get)
          handleDESError(response)
        case BAD_REQUEST | FORBIDDEN | CONFLICT =>
          logger.error(logMessage(response).get)
          handleDESError(response)
        case _ =>
          logger.error(logMessage(response).get)
          handleDESError(response, Some(INTERNAL_SERVER_ERROR))
      }
    }
  }
}
