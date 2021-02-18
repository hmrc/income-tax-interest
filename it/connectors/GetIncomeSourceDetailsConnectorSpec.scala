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

package connectors

import helpers.WiremockSpec
import models.{DesErrorBodyModel, DesErrorModel, InterestDetailsModel}
import org.scalatestplus.play.PlaySpec
import play.api.http.Status._
import play.api.libs.json.{JsObject, Json}
import uk.gov.hmrc.http.HeaderCarrier

class GetIncomeSourceDetailsConnectorSpec extends PlaySpec with WiremockSpec{

  lazy val connector: GetIncomeSourceDetailsConnector = app.injector.instanceOf[GetIncomeSourceDetailsConnector]
  implicit val hc: HeaderCarrier = HeaderCarrier()
  val nino = "nino"
  val taxYear = "2020"
  val incomeSourceId = "someId"
  val url = s"/income-tax/income-sources/nino/$nino\\?incomeSourceType=savings&taxYear=$taxYear&incomeSourceId=$incomeSourceId"

  val model: InterestDetailsModel = InterestDetailsModel(incomeSourceId, Some(29.99), Some(37.65))
  val desReturned: JsObject = Json.obj(
    "savingsInterestAnnualIncome" -> Json.arr(model)
  )
  val desReturnedEmpty: JsObject = Json.obj(
    "savingsInterestAnnualIncome" -> Json.arr()
  )

  ".getIncomeSourceDetails" should {

    "return a success result" when {

      "DES returns a 200" in {
        stubGetWithResponseBody(url, OK, desReturned.toString())
        val result = await(connector.getIncomeSourceDetails(nino, taxYear, incomeSourceId))

        result mustBe Right(model)

      }
    }

    "return a InternalServerError parsing error when incorrectly parsed" in {

      val invalidJson = Json.obj(
        "savingsInterestAnnualIncome" -> "test"
      )

      val expectedResult = DesErrorModel(INTERNAL_SERVER_ERROR, DesErrorBodyModel.parsingError)
      stubGetWithResponseBody(url, INTERNAL_SERVER_ERROR, invalidJson.toString())
      implicit val hc: HeaderCarrier = HeaderCarrier()
      val result = await(connector.getIncomeSourceDetails(nino, taxYear, incomeSourceId))

      result mustBe Left(expectedResult)
    }

    "return a parsing error InternalServerError when it is a bad success response" in {

      val invalidJson = Json.obj(
        "savingsInterestAnnualIncome" -> Json.arr(
          ""
        )
      )

      val expectedResult = DesErrorModel(INTERNAL_SERVER_ERROR, DesErrorBodyModel.parsingError)
      stubGetWithResponseBody(url, OK, invalidJson.toString())

      implicit val hc: HeaderCarrier = HeaderCarrier()
      val result = await(connector.getIncomeSourceDetails(nino, taxYear, incomeSourceId)(hc))

      result mustBe Left(expectedResult)
    }

    "return a NoContent response" in {

      val expectedResult = DesErrorModel(INTERNAL_SERVER_ERROR, DesErrorBodyModel.parsingError)
      stubGetWithResponseBody(url, NO_CONTENT, "{}")

      implicit val hc: HeaderCarrier = HeaderCarrier()
      val result = await(connector.getIncomeSourceDetails(nino, taxYear, incomeSourceId)(hc))

      result mustBe Left(expectedResult)
    }

    "return a BadRequest response" in {

      val responseBody = Json.obj(
        "code" -> "INVALID_NINO",
        "reason" -> "NINO is invalid"
      )
      val expectedResult = DesErrorModel(BAD_REQUEST, DesErrorBodyModel("INVALID_NINO","NINO is invalid"))
      stubGetWithResponseBody(url, BAD_REQUEST, responseBody.toString())

      implicit val hc: HeaderCarrier = HeaderCarrier()
      val result = await(connector.getIncomeSourceDetails(nino, taxYear, incomeSourceId)(hc))

      result mustBe Left(expectedResult)
    }

    "return a NotFound response" in {

      val responseBody = Json.obj(
        "code" -> "NOT_FOUND_INCOME_SOURCE",
        "reason" -> "Can't find the income source"
      )
      val expectedResult = DesErrorModel(NOT_FOUND, DesErrorBodyModel("NOT_FOUND_INCOME_SOURCE", "Can't find the income source"))
      stubGetWithResponseBody(url, NOT_FOUND, responseBody.toString())

      implicit val hc: HeaderCarrier = HeaderCarrier()
      val result = await(connector.getIncomeSourceDetails(nino, taxYear, incomeSourceId)(hc))

      result mustBe Left(expectedResult)
    }

    "return an InternalServerError response" in {

      val responseBody = Json.obj(
        "code" -> "SERVER_ERROR",
        "reason" -> "Internal Server Error"
      )
      val expectedResult = DesErrorModel(INTERNAL_SERVER_ERROR, DesErrorBodyModel("SERVER_ERROR", "Internal Server Error"))
      stubGetWithResponseBody(url, INTERNAL_SERVER_ERROR, responseBody.toString())

      implicit val hc: HeaderCarrier = HeaderCarrier()
      val result = await(connector.getIncomeSourceDetails(nino, taxYear, incomeSourceId)(hc))

      result mustBe Left(expectedResult)
    }

    "return a ServiceUnavailable response" in {

      val responseBody = Json.obj(
        "code" -> "SERVICE_UNAVAILABLE",
        "reason" -> "The service is currently unavailable"
      )
      val expectedResult = DesErrorModel(SERVICE_UNAVAILABLE, DesErrorBodyModel("SERVICE_UNAVAILABLE", "The service is currently unavailable"))
      stubGetWithResponseBody(url, SERVICE_UNAVAILABLE, responseBody.toString())

      implicit val hc: HeaderCarrier = HeaderCarrier()
      val result = await(connector.getIncomeSourceDetails(nino, taxYear, incomeSourceId)(hc))

      result mustBe Left(expectedResult)
    }

}}
