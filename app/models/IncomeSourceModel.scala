/*
 * Copyright 2020 HM Revenue & Customs
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

import play.api.libs.json.{Reads, _}

case class IncomeSourceModel (incomeSourceId: String, incomeSourceType: String, incomeSourceName: String)

object IncomeSourceModel {

  implicit val reads: Reads[IncomeSourceModel] = (json: JsValue) => {
    for {
      incomeSourceId <- (json \ "incomeSourceId").validate[String]
      incomeSourceType <- (json \ "incomeSourceType").validate[String]
      incomeSourceName <- (json \ "incomeSourceName").validate[String]
    } yield {
      IncomeSourceModel(incomeSourceId, incomeSourceType, incomeSourceName)
    }
  }

  implicit val writes: Writes[IncomeSourceModel] = Json.writes[IncomeSourceModel]

}
