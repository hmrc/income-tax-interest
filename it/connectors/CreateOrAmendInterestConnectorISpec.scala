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
import models._
import org.scalatestplus.play.PlaySpec
import play.api.http.Status._
import play.api.libs.json.Json
import uk.gov.hmrc.http.HeaderCarrier

class CreateOrAmendInterestConnectorISpec extends PlaySpec with WiremockSpec{

  lazy val connector: CreateOrAmendInterestConnector = app.injector.instanceOf[CreateOrAmendInterestConnector]
  implicit val hc: HeaderCarrier = HeaderCarrier()
  val nino = "nino"
  val taxYear = 2021
  val url = s"/income-tax/nino/$nino/income-source/savings/annual/$taxYear"

  val model: InterestDetailsModel = InterestDetailsModel("incomeSourceId", Some(100.00), Some(100.00))


  " CreateOrAmendInterestConnector" should {
    "return a success result" when {
      "DES Returns a 200" in {
        val expectedResult = true

        stubPostWithoutResponseBody(url, OK, Json.toJson(model).toString())

        implicit val hc: HeaderCarrier = HeaderCarrier()
        val result = await(connector.createOrAmendInterest(nino, taxYear, model)(hc))

        result mustBe Right(expectedResult)
      }
    }
    "return a InternalServerError parsing error when incorrectly parsed" in {

      val invalidJson = Json.obj(
        "notErrormodel" -> "test"
      )

      val expectedResult = DesErrorModel(INTERNAL_SERVER_ERROR, DesErrorBodyModel.parsingError)

      stubPostWithResponseBody(url, INTERNAL_SERVER_ERROR, Json.toJson(model).toString(), invalidJson.toString)
      implicit val hc: HeaderCarrier = HeaderCarrier()
      val result = await(connector.createOrAmendInterest(nino, taxYear, model)(hc))

      result mustBe Left(expectedResult)
    }
    "return a failed result" when {
      "DES Returns a BAD_REQUEST" in {
        val expectedResult = DesErrorModel(BAD_REQUEST, DesErrorBodyModel("INVALID_IDTYPE","ID is invalid"))

        val responseBody = Json.obj(
          "code" -> "INVALID_IDTYPE",
          "description" -> "ID is invalid"
        )
        stubPostWithResponseBody(url, BAD_REQUEST, Json.toJson(model).toString(), responseBody.toString)

        implicit val hc: HeaderCarrier = HeaderCarrier()
        val result = await(connector.createOrAmendInterest(nino, taxYear, model)(hc))

        result mustBe Left(expectedResult)
      }
      "DES Returns a SERVICE_UNAVAILABLE" in {
        val expectedResult = DesErrorModel(SERVICE_UNAVAILABLE, DesErrorBodyModel("SERVICE_UNAVAILABLE", "The service is currently unavailable"))

        val responseBody = Json.obj(
          "code" -> "SERVICE_UNAVAILABLE",
          "description" -> "The service is currently unavailable"
        )
        stubPostWithResponseBody(url, SERVICE_UNAVAILABLE, Json.toJson(model).toString(), responseBody.toString)

        implicit val hc: HeaderCarrier = HeaderCarrier()
        val result = await(connector.createOrAmendInterest(nino, taxYear, model)(hc))

        result mustBe Left(expectedResult)
      }
      "DES Returns a INTERNAL_SERVER_ERROR" in {
        val expectedResult = DesErrorModel(INTERNAL_SERVER_ERROR, DesErrorBodyModel("SERVER_ERROR", "Internal Server Error"))

        val responseBody = Json.obj(
          "code" -> "SERVER_ERROR",
          "description" -> "Internal Server Error"
        )
        stubPostWithResponseBody(url, INTERNAL_SERVER_ERROR, Json.toJson(model).toString(), responseBody.toString())

        implicit val hc: HeaderCarrier = HeaderCarrier()
        val result = await(connector.createOrAmendInterest(nino, taxYear, model)(hc))

        result mustBe Left(expectedResult)
      }
      "DES Returns a unexpected response" in {
        val expectedResult = DesErrorModel(INTERNAL_SERVER_ERROR, DesErrorBodyModel.parsingError)

        stubPostWithoutResponseBody(url, GONE, Json.toJson(model).toString())

        implicit val hc: HeaderCarrier = HeaderCarrier()
        val result = await(connector.createOrAmendInterest(nino, taxYear, model)(hc))

        result mustBe Left(expectedResult)
      }
    }
  }

}
