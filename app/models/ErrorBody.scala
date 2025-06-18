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

package models

import play.api.libs.json.{JsValue, Json, OFormat, Reads, Writes}

sealed trait ErrorBody
object ErrorBody {
  implicit val reads: Reads[ErrorBody] =
    ErrorBodyModel.reads.widen[ErrorBody] orElse ErrorsBodyModel.formats.widen[ErrorBody]
}

/** No Error message propagated **/
object EmptyErrorBody  extends ErrorBody

/** Single Error **/
case class ErrorBodyModel(code: String, reason: String) extends ErrorBody
object ErrorBodyModel {

  implicit val reads: Reads[ErrorBodyModel] = Reads { json =>
    for {
      code <- (json \ "code").validate[String] orElse (json \ "type").validate[String]
      reason <- (json \ "reason").validate[String]
    } yield ErrorBodyModel(code, reason)
  }

  implicit val writes: Writes[ErrorBodyModel] = Json.writes[ErrorBodyModel]
  val parsingError: ErrorBodyModel = ErrorBodyModel("PARSING_ERROR", "Error parsing response from API")
}

/** Multiple Errors **/
case class ErrorsBodyModel(failures: Seq[ErrorBodyModel]) extends ErrorBody
object ErrorsBodyModel {
  implicit val formats: OFormat[ErrorsBodyModel] = Json.format[ErrorsBodyModel]
}

case class ErrorModel(status: Int, body: ErrorBody) {
  def toJson: JsValue =
    body match {
      case error @ ErrorBodyModel(_, _) => Json.toJson(error)
      case errors @ ErrorsBodyModel(_) => Json.toJson(errors)
      case EmptyErrorBody => Json.obj()
    }
}
