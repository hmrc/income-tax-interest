/*
 * Copyright 2021 HM Revenue & Customs
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
import models.{DesErrorBodyModel, DesErrorModel, IncomeSourceModel}
import org.scalatestplus.play.PlaySpec
import play.api.http.Status._
import play.api.libs.json.Json
import uk.gov.hmrc.http.HeaderCarrier

class GetIncomeSourceListConnectorSpec extends PlaySpec with WiremockSpec{

  lazy val connector: GetIncomeSourceListConnector = app.injector.instanceOf[GetIncomeSourceListConnector]
  implicit val hc: HeaderCarrier = HeaderCarrier()
  val nino = "nino"
  val taxYear = "2020"
  val url = s"/income-tax/income-sources/nino/$nino\\?incomeSourceType=interest-from-uk-banks&taxYear=$taxYear"

  val model: List[IncomeSourceModel] = List(IncomeSourceModel(taxYear, "interest-from-uk-banks", "incomeSource1"))

  "GetIncomeSourceListConnector" should {

    "return a success result" when {

      "DES returns a 200" in {
        stubGetWithResponseBody(url, OK, Json.toJson(model).toString())

        val result = await(connector.getIncomeSourceList(nino, taxYear))
        result mustBe Right(model)
      }
    }

    "return a parsing error InternalServerError response when incorrectly parsed" in {

      val invalidJson = Json.obj(
        "incomeSourceId" -> "test"
      )

      val expectedResult = DesErrorModel(INTERNAL_SERVER_ERROR, DesErrorBodyModel.parsingError)

      stubGetWithResponseBody(url, INTERNAL_SERVER_ERROR, invalidJson.toString())
      implicit val hc: HeaderCarrier = HeaderCarrier()
      val result = await(connector.getIncomeSourceList(nino, taxYear)(hc))

      result mustBe Left(expectedResult)
    }

    "return a parsing error InternalServerError when it is a bad success" in {
      val invalidJson = Json.obj(
        "incomeSourceId" -> ""
      )

      val expectedResult = DesErrorModel(INTERNAL_SERVER_ERROR, DesErrorBodyModel.parsingError)

      stubGetWithResponseBody(url, OK, invalidJson.toString())
      implicit val hc: HeaderCarrier = HeaderCarrier()
      val result = await(connector.getIncomeSourceList(nino, taxYear)(hc))

      result mustBe Left(expectedResult)
    }

    "return a NoContent response" in {

      val expectedResult = DesErrorModel(INTERNAL_SERVER_ERROR, DesErrorBodyModel.parsingError)

      stubGetWithResponseBody(url, NO_CONTENT, "{}")
      implicit val hc: HeaderCarrier = HeaderCarrier()
      val result = await(connector.getIncomeSourceList(nino, taxYear)(hc))

      result mustBe Left(expectedResult)
    }

    "return a BadRequest response" in {

      val responseBody = Json.obj(
        "code" -> "Invalid_NINO",
        "reason" -> "NINO is Invalid"
      )

      val expectedResult = DesErrorModel(BAD_REQUEST, DesErrorBodyModel("Invalid_NINO", "NINO is Invalid"))

      stubGetWithResponseBody(url, BAD_REQUEST, responseBody.toString())
      implicit val hc: HeaderCarrier = HeaderCarrier()
      val result = await(connector.getIncomeSourceList(nino, taxYear)(hc))

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
      val result = await(connector.getIncomeSourceList(nino, taxYear)(hc))

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
      val result = await(connector.getIncomeSourceList(nino, taxYear)(hc))

      result mustBe Left(expectedResult)
    }

    "return a ServiceUnavailableResponse" in {

      val responseBody = Json.obj(
        "code" -> "SERVICE_UNAVAILABLE",
        "reason" -> "The service is currently unavailable"
      )
      val expectedResult = DesErrorModel(SERVICE_UNAVAILABLE, DesErrorBodyModel("SERVICE_UNAVAILABLE", "The service is currently unavailable"))

      stubGetWithResponseBody(url, SERVICE_UNAVAILABLE, responseBody.toString())
      implicit val hc: HeaderCarrier = HeaderCarrier()
      val result = await(connector.getIncomeSourceList(nino, taxYear)(hc))

      result mustBe Left(expectedResult)
    }

  }
}
