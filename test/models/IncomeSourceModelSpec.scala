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

import com.codahale.metrics.SharedMetricRegistries
import play.api.libs.json.{JsObject, Json}
import testUtils.TestSuite

class IncomeSourceModelSpec extends TestSuite{
  SharedMetricRegistries.clear()

  val model: IncomeSourceModel = new IncomeSourceModel("nino", "incomeSourceId",
    "interest-from-uk-banks", "incomeSourceName")

  val jsonModel: JsObject = Json.obj(
    "nino" -> "nino",
    "incomeSourceId" -> "incomeSourceId",
    "incomeSourceType" -> "interest-from-uk-banks",
    "incomeSourceName" -> "incomeSourceName"
  )

  "submitted interests" should {

    "parse to json" in {
      Json.toJson(model) mustBe jsonModel
    }

    "parse from json" in {
      jsonModel.as[IncomeSourceModel]
    }
  }

}
