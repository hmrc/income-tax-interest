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
import play.api.libs.json.{JsString, Json}

class IncomeSourceTypeSpec extends AnyWordSpec with Matchers {

  IncomeSourceType.values.foreach { incomeSourceType =>

    s"for IncomeSourceType of ${incomeSourceType.getClass.getSimpleName.stripSuffix("$")}" should {

      "serialise to JSON" in {
        Json.toJson(incomeSourceType) mustBe JsString(incomeSourceType.code)
      }

      "deserialise from JSON" in {
        JsString(incomeSourceType.code).as[IncomeSourceType] mustBe incomeSourceType
      }
    }

    "for an invalid code should throw IllegalArgumentException" in {
      val e = intercept[IllegalArgumentException](IncomeSourceType("a"))
      e.getMessage mustBe "Invalid income source type code: a"
    }
  }
}
