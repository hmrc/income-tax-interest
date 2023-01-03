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
import play.api.libs.json.{JsObject, Json}
import testUtils.TestSuite

class ErrorBodyModelSpec extends TestSuite {

  val model: ErrorBodyModel = new ErrorBodyModel(
    "SERVICE_UNAVAILABLE", "The service is currently unavailable")

  val jsModel: JsObject = Json.obj(
    "code" -> "SERVICE_UNAVAILABLE",
    "reason" -> "The service is currently unavailable"
  )

  val errorsJsModel: JsObject = Json.obj(
    "failures" -> Json.arr(
      Json.obj("code" -> "SERVICE_UNAVAILABLE",
      "reason" -> "The service is currently unavailable"),
      Json.obj("code" -> "INTERNAL_SERVER_ERROR",
      "reason" -> "The service is currently facing issues.")
    )
  )

  "The ErrorBodyModel" should {

    "parse to Json" in {
      Json.toJson(model) mustBe jsModel
    }

    "parse from json" in {
      jsModel.as[ErrorBodyModel]
    }
  }

  "The ErrorModel" should {

    val model = ErrorModel(SERVICE_UNAVAILABLE, ErrorBodyModel("SERVICE_UNAVAILABLE","The service is currently unavailable"))
    val errorsModel = ErrorModel(SERVICE_UNAVAILABLE, ErrorsBodyModel(Seq(
      ErrorBodyModel("SERVICE_UNAVAILABLE","The service is currently unavailable"),
      ErrorBodyModel("INTERNAL_SERVER_ERROR","The service is currently facing issues.")
    )))

    "parse to Json" in {
      model.toJson mustBe jsModel
    }
    "parse to Json for multiple errors" in {
      errorsModel.toJson mustBe errorsJsModel
    }
  }

}
