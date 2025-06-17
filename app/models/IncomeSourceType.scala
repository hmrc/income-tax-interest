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

import play.api.libs.json.{JsString, Reads, Writes, __}

sealed class IncomeSourceType(val code: String)
object UKBankAccount extends IncomeSourceType("09")

object IncomeSourceType {

  val values: Seq[IncomeSourceType] = Seq(UKBankAccount)

  def apply(code: String): IncomeSourceType = code match {
    case "09" => UKBankAccount
    case _    => throw new IllegalArgumentException(s"Invalid income source type code: $code")
  }

  implicit val writes: Writes[IncomeSourceType] = (o: IncomeSourceType) => JsString(o.code)
  implicit val reads: Reads[IncomeSourceType] = __.read[String] map apply
}
