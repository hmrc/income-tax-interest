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

package connectors.httpParsers

import models._
import play.api.Logging
import play.api.http.Status._
import uk.gov.hmrc.http.{HttpReads, HttpResponse}

object CreateOrAmendAnnualIncomeSourcePeriodHttpParser extends APIParser with Logging{
  type CreateOrAmendAnnualIncomeSourcePeriodResponse = Either[ErrorModel, Done]

  implicit object CreateIncomeSourceHttpReads extends HttpReads[CreateOrAmendAnnualIncomeSourcePeriodResponse] {
    override def read(method: String, url: String, response: HttpResponse): CreateOrAmendAnnualIncomeSourcePeriodResponse = {
      response.status match {
        case OK => Right(Done)
        case INTERNAL_SERVER_ERROR =>
          logger.error(logMessage(response))
          handleAPIError(response)
        case SERVICE_UNAVAILABLE =>
          logger.error(logMessage(response))
          handleAPIError(response)
        case BAD_REQUEST | NOT_FOUND | UNPROCESSABLE_ENTITY =>
          logger.error(logMessage(response))
          handleAPIError(response)
        case _ =>
          logger.error(logMessage(response))
          handleAPIError(response, Some(INTERNAL_SERVER_ERROR))
      }
    }
  }
}
