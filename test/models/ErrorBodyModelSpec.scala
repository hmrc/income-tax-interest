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

import play.api.http.Status.SERVICE_UNAVAILABLE
import play.api.libs.json.Json
import testUtils.TestSuite

class ErrorBodyModelSpec extends TestSuite {

  private val errorBodyModelAsJson = Json.obj(
    "code" -> "SERVICE_UNAVAILABLE",
    "reason" -> "The service is currently unavailable"
  )

  private val hipErrorBodyModelAsJson = Json.obj(
    "type" -> "SERVICE_UNAVAILABLE",
    "reason" -> "The service is currently unavailable"
  )

  "ErrorBodyModel" should {
    val model = new ErrorBodyModel("SERVICE_UNAVAILABLE", "The service is currently unavailable")

    "serialize to Json" in {
      Json.toJson(model) mustBe errorBodyModelAsJson
    }

    "for a DES/IF response" should {

      "deserialize from json without throwing a parse exception" in {
        errorBodyModelAsJson.as[ErrorBodyModel]
      }
    }

    "for a HIP response" should {

      "deserialize from json without throwing a parse exception" in {
        hipErrorBodyModelAsJson.as[ErrorBodyModel]
      }
    }
  }

  "ErrorModel" should {
    "serialize to Json" when {
      "there is a single error message" in {
        val model = ErrorModel(SERVICE_UNAVAILABLE, ErrorBodyModel("SERVICE_UNAVAILABLE","The service is currently unavailable"))

        model.toJson mustBe errorBodyModelAsJson
      }

      "there are multiple error messages" in {
        val errorsModel = ErrorModel(SERVICE_UNAVAILABLE, ErrorsBodyModel(Seq(
          ErrorBodyModel("SERVICE_UNAVAILABLE","The service is currently unavailable"),
          ErrorBodyModel("INTERNAL_SERVER_ERROR","The service is currently facing issues.")
        )))


        errorsModel.toJson mustBe Json.obj(
          "failures" -> Json.arr(
            Json.obj("code" -> "SERVICE_UNAVAILABLE",
              "reason" -> "The service is currently unavailable"),
            Json.obj("code" -> "INTERNAL_SERVER_ERROR",
              "reason" -> "The service is currently facing issues.")
          )
        )
      }

      "when there are no error messages" in {
        val errorModel = ErrorModel(SERVICE_UNAVAILABLE, EmptyErrorBody)

        errorModel.toJson mustBe Json.obj()
      }
    }
  }

}
