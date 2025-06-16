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

import models.{ErrorBody, ErrorBodyModel, ErrorModel}
import play.api.http.Status.INTERNAL_SERVER_ERROR
import uk.gov.hmrc.http.HttpResponse
import utils.PagerDutyHelper.PagerDutyKeys.{BAD_SUCCESS_JSON_FROM_API, UNEXPECTED_RESPONSE_FROM_API}
import utils.PagerDutyHelper.{getCorrelationId, pagerDutyLog}

import scala.util.control.NonFatal

trait APIParser {

  def logMessage(response:HttpResponse): String =
    s"[APIParser][read] Received ${response.status} status code. Body:${response.body}" + getCorrelationId(response)

  def badSuccessJsonFromAPI[Response]: Either[ErrorModel, Response] = {
    pagerDutyLog(BAD_SUCCESS_JSON_FROM_API, s"[APIParser][read] Invalid Json response.")
    Left(ErrorModel(INTERNAL_SERVER_ERROR, ErrorBodyModel.parsingError))
  }

  def handleAPIError[Response](response: HttpResponse, statusOverride: Option[Int] = None): Either[ErrorModel, Response] = {
    val status = statusOverride.getOrElse(response.status)

    try {
      response.json.asOpt[ErrorBody] match {
        case Some(apiError) => Left(ErrorModel(status, apiError))
        case None =>
          pagerDutyLog(UNEXPECTED_RESPONSE_FROM_API, s"[APIParser][read] Unexpected Json response.")
          Left(ErrorModel(status, ErrorBodyModel.parsingError))
      }
    } catch {
      case NonFatal(_) => Left(ErrorModel(status, ErrorBodyModel.parsingError))
    }
  }
}
