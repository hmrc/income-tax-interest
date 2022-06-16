/*
 * Copyright 2022 HM Revenue & Customs
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

import com.github.tomakehurst.wiremock.http.HttpHeader
import config.{AppConfig, BackendAppConfig}
import helpers.WiremockSpec
import models.{DesErrorBodyModel, DesErrorModel, InterestDetailsModel}
import org.scalatestplus.play.PlaySpec
import play.api.Configuration
import play.api.http.Status._
import play.api.libs.json.{JsObject, Json}
import uk.gov.hmrc.http.{HeaderCarrier, HeaderNames, HttpClient, SessionId}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

class GetIncomeSourceDetailsConnectorSpec extends PlaySpec with WiremockSpec{

  lazy val connector: GetIncomeSourceDetailsConnector = app.injector.instanceOf[GetIncomeSourceDetailsConnector]
  implicit val hc: HeaderCarrier = HeaderCarrier()
  val nino = "nino"
  val taxYear = "2020"
  val incomeSourceId = "someId"

  val url = s"/income-tax/nino/$nino/income-source/savings/annual/$taxYear\\?incomeSourceId=$incomeSourceId"

  lazy val httpClient: HttpClient = app.injector.instanceOf[HttpClient]

  def appConfig(desHost: String): AppConfig = new BackendAppConfig(app.injector.instanceOf[Configuration], app.injector.instanceOf[ServicesConfig]) {
    override val desBaseUrl: String = s"http://$desHost:$wireMockPort"
  }

  val model: InterestDetailsModel = InterestDetailsModel(incomeSourceId, Some(29.99), Some(37.65))
  val desReturned: JsObject = Json.obj(
    "savingsInterestAnnualIncome" -> Json.arr(model)
  )
  val desReturnedEmpty: JsObject = Json.obj(
    "savingsInterestAnnualIncome" -> Json.arr()
  )

  ".getIncomeSourceDetails" should {

    "include internal headers" when {

      val headersSentToDes = Seq(
        new HttpHeader(HeaderNames.authorisation, "Bearer secret"),
        new HttpHeader(HeaderNames.xSessionId, "sessionIdValue")
      )

      val externalHost = "127.0.0.1"

      "the host for DES is 'Internal'" in {
        implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId("sessionIdValue")))

        stubGetWithResponseBody(url, OK, desReturned.toString())

        val result = await(connector.getIncomeSourceDetails(nino, taxYear, incomeSourceId)(hc))

        result mustBe Right(model)
      }

      "the host for DES is 'External'" in {
        implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId("sessionIdValue")))

        stubGetWithResponseBody(url, OK, desReturned.toString(),headersSentToDes)

        val connector = new GetIncomeSourceDetailsConnector(httpClient, appConfig(externalHost))


        val result = await(connector.getIncomeSourceDetails(nino, taxYear, incomeSourceId)(hc))

        result mustBe Right(model)
      }
    }

    "return a success result" when {

      "DES returns a 200" in {
        stubGetWithResponseBody(url, OK, desReturned.toString())
        val result = await(connector.getIncomeSourceDetails(nino, taxYear, incomeSourceId))

        result mustBe Right(model)

      }
    }

    "return an error" when {

      "DES returns an empty 200" in {
        stubGetWithResponseBody(url, OK, desReturnedEmpty.toString())
        val result = await(connector.getIncomeSourceDetails(nino, taxYear, incomeSourceId))
        val expectedResult = DesErrorModel(INTERNAL_SERVER_ERROR, DesErrorBodyModel.parsingError)

        result mustBe Left(expectedResult)

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
