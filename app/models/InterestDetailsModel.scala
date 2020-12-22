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

import play.api.libs.json._

case class InterestDetailsModel (incomeSourceId: String, taxedUkInterest: Option[BigDecimal], untaxedUkInterest: Option[BigDecimal])

object InterestDetailsModel {

  implicit val reads: Reads[InterestDetailsModel] = {
    for {
      incomeSourceId <- (__ \ "incomeSourceId").read[String]
      taxedUkInterest <- (__ \ "taxedUkInterest").readNullable[BigDecimal]
      untaxedUkInterest <- (__ \ "untaxedUkInterest").readNullable[BigDecimal]
    } yield {
      InterestDetailsModel(incomeSourceId, taxedUkInterest, untaxedUkInterest)
    }
  }

  implicit val writes: Writes[InterestDetailsModel] = Json.writes[InterestDetailsModel]

}
