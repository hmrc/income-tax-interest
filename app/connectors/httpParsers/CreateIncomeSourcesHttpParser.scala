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
import org.slf4j.MDC
import play.api.Logging
import play.api.http.Status._
import uk.gov.hmrc.http.{HttpReads, HttpResponse}
import utils.PagerDutyHelper.PagerDutyKeys.{FIVEXX_RESPONSE_FROM_API, FOURXX_RESPONSE_FROM_API, UNEXPECTED_RESPONSE_FROM_API}

object CreateIncomeSourcesHttpParser extends APIParser with Logging {
  type CreateIncomeSourcesResponse = Either[ErrorModel, IncomeSourceIdModel]

  implicit object CreateIncomeSourcesHttpReads extends HttpReads[CreateIncomeSourcesResponse] {
    override def read(method: String, url: String, response: HttpResponse): CreateIncomeSourcesResponse = {
      response.header("CorrelationId").foreach(MDC.put("CorrelationId", _))

      logger.debug(s"[CreateIncomeSourcesHttpReads] Response body: ${response.body}")

      response.status match {
        case OK =>
          response.json.validate[IncomeSourceIdModel].fold(
            _ => {
              logger.error(s"$UNEXPECTED_RESPONSE_FROM_API  Unexpected Json response.")
              Left(ErrorModel(INTERNAL_SERVER_ERROR, ErrorBodyModel.parsingError))
            },
            Right(_)
          )
        case BAD_REQUEST | UNAUTHORIZED | UNPROCESSABLE_ENTITY =>
          response.json.asOpt[ErrorBody] match {
            case Some(apiError) =>
              logger.error(s"$FOURXX_RESPONSE_FROM_API Received ${response.status} status code. Body:${response.body}")
              Left(ErrorModel(500, apiError))
            case None =>
              logger.error(s"$FOURXX_RESPONSE_FROM_API $UNEXPECTED_RESPONSE_FROM_API Unexpected Json response.")
              Left(ErrorModel(500, ErrorBodyModel.parsingError))
          }

        case INTERNAL_SERVER_ERROR | BAD_GATEWAY | SERVICE_UNAVAILABLE =>
          response.json.asOpt[ErrorBody] match {
            case Some(apiError) =>
              logger.error(s"$FIVEXX_RESPONSE_FROM_API Received ${response.status} status code. Body:${response.body}")
              Left(ErrorModel(500, apiError))
            case None =>
              logger.error(s"$FIVEXX_RESPONSE_FROM_API Unexpected Json response.")
              Left(ErrorModel(500, ErrorBodyModel.parsingError))
          }

        case _ =>
          logger.warn(s"$UNEXPECTED_RESPONSE_FROM_API Received ${response.status} status code. Body:${response.body}")
          Left(ErrorModel(500, EmptyErrorBody))
      }
    }
  }
}
