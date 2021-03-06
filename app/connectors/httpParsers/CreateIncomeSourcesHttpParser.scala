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
import play.api.http.Status._
import uk.gov.hmrc.http.{HttpReads, HttpResponse}
import utils.PagerDutyHelper.PagerDutyKeys._
import utils.PagerDutyHelper.pagerDutyLog
import utils.PagerDutyHelper.getCorrelationId

object CreateIncomeSourcesHttpParser {
  type CreateIncomeSourcesResponse = Either[DesErrorModel, IncomeSourceIdModel]

  implicit object CreateIncomeSourcesHttpReads extends HttpReads[CreateIncomeSourcesResponse] {
    override def read(method: String, url: String, response: HttpResponse): CreateIncomeSourcesResponse = {
      response.status match {
        case OK => response.json.validate[IncomeSourceIdModel].fold[CreateIncomeSourcesResponse](
          jsonErrors => handleDESError(response, Some(INTERNAL_SERVER_ERROR)),
          parsedModel => Right(parsedModel)
        )
        case INTERNAL_SERVER_ERROR =>
          pagerDutyLog(INTERNAL_SERVER_ERROR_FROM_DES, logMessage(response))
          handleDESError(response)
        case SERVICE_UNAVAILABLE =>
          pagerDutyLog(SERVICE_UNAVAILABLE_FROM_DES, logMessage(response))
          handleDESError(response)
        case BAD_REQUEST | FORBIDDEN | CONFLICT=>
          pagerDutyLog(FOURXX_RESPONSE_FROM_DES, logMessage(response))
          handleDESError(response)
        case _ =>
          pagerDutyLog(UNEXPECTED_RESPONSE_FROM_DES, logMessage(response))
          handleDESError(response, Some(INTERNAL_SERVER_ERROR))
      }
    }
  }

  private def handleDESError(response: HttpResponse, statusOverride: Option[Int] = None): CreateIncomeSourcesResponse = {

    val status = statusOverride.getOrElse(response.status)

    try {
      response.json.validate[DesErrorBodyModel].fold[CreateIncomeSourcesResponse](

        jsonErrors => {
          pagerDutyLog(UNEXPECTED_RESPONSE_FROM_DES, Some(s"[CreateIncomeSourcesParser][read] Unexpected Json from DES."))
          Left(DesErrorModel(status, DesErrorBodyModel.parsingError))
        },
        parsedModel => Left(DesErrorModel(status, parsedModel)))
    } catch {
      case _: Exception => Left(DesErrorModel(status, DesErrorBodyModel.parsingError))
    }
  }

  private def logMessage(response:HttpResponse): Option[String] ={
    Some(s"[CreateIncomeSourcesParser][read] Received ${response.status} from DES. Body:${response.body}" + getCorrelationId(response))
  }
}
