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

package connectors.httpParsers

import models._
import play.api.Logging
import play.api.http.Status._
import uk.gov.hmrc.http.{HttpReads, HttpResponse}

object CreateOrAmendSavingsHttpParser extends DESParser with Logging{
  type CreateOrAmendSavingsResponse = Either[DesErrorModel, Boolean]

  override val parserName: String = "CreateOrAmendSavingsParser"

  implicit object CreateOrAmendSavingsHttpReads extends HttpReads[CreateOrAmendSavingsResponse] {
    override def read(method: String, url: String, response: HttpResponse): CreateOrAmendSavingsResponse = {
      response.status match {
        case OK => Right(true)
        case INTERNAL_SERVER_ERROR =>
          logger.error(logMessage(response))
          handleDESError(response)
        case SERVICE_UNAVAILABLE =>
          logger.error(logMessage(response))
          handleDESError(response)
        case BAD_REQUEST | FORBIDDEN | NOT_FOUND | UNPROCESSABLE_ENTITY =>
          logger.error(logMessage(response))
          handleDESError(response)
        case _ =>
          logger.error(logMessage(response))
          handleDESError(response, Some(INTERNAL_SERVER_ERROR))
      }
    }
  }
}