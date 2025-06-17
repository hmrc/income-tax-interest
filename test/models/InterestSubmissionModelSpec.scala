/*
 * Copyright 2025 HM Revenue & Customs
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

import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.{JsObject, Json}

class InterestSubmissionModelSpec extends AnyWordSpec with Matchers {

  val model: InterestSubmissionModel = InterestSubmissionModel(incomeSourceName = "HSBC Savings")
  val json: JsObject = Json.obj(
    "incomeSourceType" -> "09",
    "incomeSourceName" -> "HSBC Savings"
  )

  "serialise to JSON" in {
    Json.toJson(model) mustBe json
  }

  "deserialise from JSON" in {
    json.as[InterestSubmissionModel] mustBe model
  }
}
