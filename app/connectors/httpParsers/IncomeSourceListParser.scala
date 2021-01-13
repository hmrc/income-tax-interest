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

import models.IncomeSourceModel
import play.api.http.Status._
import uk.gov.hmrc.http.{HttpReads, HttpResponse}

object IncomeSourceListParser {
  type IncomeSourceListResponse = Either[IncomeSourceListException, List[IncomeSourceModel]]

  implicit object IncomeSourceHttpReads extends HttpReads[IncomeSourceListResponse] {
    override def read(method: String, url: String, response: HttpResponse): IncomeSourceListResponse = response.status match {
      case OK => response.json.validate[List[IncomeSourceModel]].fold[IncomeSourceListResponse](
        jsonErrors => Left(IncomeSourcesInvalidJson),
        parsedModel => Right(parsedModel)
      )
      case BAD_REQUEST => Left(InvalidSubmission)
      case NOT_FOUND => Left(NotFoundException)
      case INTERNAL_SERVER_ERROR => Left(InternalServerErrorUpstream)
      case SERVICE_UNAVAILABLE => Left(UpstreamServiceUnavailable)
      case _ => Left(UnexpectedStatus)
    }

  }

  sealed trait IncomeSourceListException

  object IncomeSourcesInvalidJson extends IncomeSourceListException
  object InvalidSubmission extends IncomeSourceListException
  object NotFoundException extends IncomeSourceListException
  object InternalServerErrorUpstream extends IncomeSourceListException
  object UpstreamServiceUnavailable extends IncomeSourceListException
  object UnexpectedStatus extends IncomeSourceListException

}
