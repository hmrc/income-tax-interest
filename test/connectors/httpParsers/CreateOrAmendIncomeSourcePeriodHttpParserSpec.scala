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

package connectors.httpParsers

import connectors.httpParsers.CreateOrAmendAnnualIncomeSourcePeriodHttpParser.CreateIncomeSourceHttpReads.read
import models.{Done, ErrorBodyModel, ErrorModel}
import play.api.http.Status._
import play.api.libs.json.Json
import testUtils.UnitTest
import uk.gov.hmrc.http.HttpResponse

class CreateOrAmendIncomeSourcePeriodHttpParserSpec extends UnitTest {

  def httpResponse(_status: Int, _body: String): HttpResponse = {
    class SomeResponse(override val status: Int,
                       override val body: String
                      ) extends HttpResponse {
      override def allHeaders: Map[String, Seq[String]] = Map()
    }

    new SomeResponse(_status, _body)
  }

  ".read" should {

    "returns true" when {

      "the response contains an OK and the correct body" in {
        val validBody = Json.prettyPrint(Json.obj("transactionReference" -> "some-transaction-id"))
        val result = read("POST", "/some-url", httpResponse(OK, validBody))

        result shouldBe Right(Done)
      }

      "the response contains an OK, but a malformed body" in {
        val invalidBody = Json.prettyPrint(Json.obj("malformed" -> "not-what-im-looking-for"))
        val result = read("POST", "/some-url", httpResponse(OK, invalidBody))

        result shouldBe Right(Done)
      }

    }

    "a IF error" when {

      "the response status is NOT_FOUND" in {
        val errorBody = Json.prettyPrint(Json.obj("code" -> "NOT_FOUND", "reason" -> "Submission Period not found"))
        val result = read("POST", "/some-url", httpResponse(NOT_FOUND, errorBody))

        result shouldBe Left(ErrorModel(NOT_FOUND, ErrorBodyModel("NOT_FOUND", "Submission Period not found")))
      }

      "the response status is UNPROCESSABLE_ENTITY" in {
        val errorBody = Json.prettyPrint(Json.obj("code" -> "UNPROCESSABLE_ENTITY", "reason" -> "The remote endpoint has indicated that for given income source type, message payload is incorrect."))
        val result = read("POST", "/some-url", httpResponse(UNPROCESSABLE_ENTITY, errorBody))

        result shouldBe Left(ErrorModel(UNPROCESSABLE_ENTITY, ErrorBodyModel("UNPROCESSABLE_ENTITY", "The remote endpoint has indicated that for given income source type, message payload is incorrect.")))
      }

      "the response status is INTERNAL_SERVER_ERROR" in {
        val errorBody = Json.prettyPrint(Json.obj("code" -> "AWW_MAN", "reason" -> "not again"))
        val result = read("POST", "/some-url", httpResponse(INTERNAL_SERVER_ERROR, errorBody))

        result shouldBe Left(ErrorModel(INTERNAL_SERVER_ERROR, ErrorBodyModel("AWW_MAN", "not again")))
      }

      "the response status is SERVICE_UNAVAILABLE" in {
        val errorBody = Json.prettyPrint(Json.obj("code" -> "AWW_MAN", "reason" -> "not again"))
        val result = read("POST", "/some-url", httpResponse(SERVICE_UNAVAILABLE, errorBody))

        result shouldBe Left(ErrorModel(SERVICE_UNAVAILABLE, ErrorBodyModel("AWW_MAN", "not again")))
      }

      "the response status is BAD_REQUEST" in {
        val errorBody = Json.prettyPrint(Json.obj("code" -> "AWW_MAN", "reason" -> "not again"))
        val result = read("POST", "/some-url", httpResponse(BAD_REQUEST, errorBody))

        result shouldBe Left(ErrorModel(BAD_REQUEST, ErrorBodyModel("AWW_MAN", "not again")))
      }

      "the response status is any other value" in {
        val errorBody = Json.prettyPrint(Json.obj("code" -> "IMMA_LITTLE_TEAPOT", "reason" -> "short and stout"))
        val result = read("POST", "/some-url", httpResponse(IM_A_TEAPOT, errorBody))

        result shouldBe Left(ErrorModel(INTERNAL_SERVER_ERROR, ErrorBodyModel("IMMA_LITTLE_TEAPOT", "short and stout")))
      }

    }
  }

}
