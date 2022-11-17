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

import config.{AppConfig, BackendAppConfig}
import com.github.tomakehurst.wiremock.http.HttpHeader
import helpers.WiremockSpec
import models._
import org.scalatestplus.play.PlaySpec
import play.api.Configuration
import play.api.http.Status._
import play.api.libs.json.Json
import uk.gov.hmrc.http.{HeaderCarrier, HeaderNames, HttpClient, SessionId}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

class CreateIncomeSourcesConnectorISpec extends PlaySpec with WiremockSpec {

  lazy val connector: CreateIncomeSourceConnector = app.injector.instanceOf[CreateIncomeSourceConnector]

  lazy val httpClient: HttpClient = app.injector.instanceOf[HttpClient]

  def appConfig(desHost: String): AppConfig = new BackendAppConfig(app.injector.instanceOf[Configuration], app.injector.instanceOf[ServicesConfig]) {
    override val desBaseUrl: String = s"http://$desHost:$wireMockPort"
  }

  val taxYear: Int = 1999
  val nino = "nino"
  val incomeSourceName = "testName"
  val url = s"/income-tax/income-sources/nino/$nino"

  val model: InterestSubmissionModel = InterestSubmissionModel(incomeSourceName = incomeSourceName)
  val createIncomeSource = IncomeSourceIdModel("1234567890")


  "CreateIncomeSourcesConnector" should {

    "include internal headers" when {
      val requestBody = Json.toJson(model).toString()
      val responseBody = Json.toJson(createIncomeSource).toString()

      val headersSentToDes = Seq(
        new HttpHeader(HeaderNames.authorisation, "Bearer secret"),
        new HttpHeader(HeaderNames.xSessionId, "sessionIdValue")
      )

      val externalHost = "127.0.0.1"

      "the host for DES is 'Internal'" in {
        implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId("sessionIdValue")))
        val expectedResult = IncomeSourceIdModel("1234567890")

        stubPostWithResponseBody(url, OK, requestBody, responseBody, headersSentToDes)

        val result = await(connector.createIncomeSource(nino, model)(hc))

        result mustBe Right(expectedResult)
      }

      "the host for DES is 'External'" in {
        implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId("sessionIdValue")))
        val connector = new CreateIncomeSourceConnector(httpClient, appConfig(externalHost))
        val expectedResult = createIncomeSource

        stubPostWithResponseBody(url, OK, requestBody, responseBody, headersSentToDes)

        val result = await(connector.createIncomeSource(nino, model)(hc))

        result mustBe Right(expectedResult)
      }
    }


    "return a success result" when {
      "DES Returns a 200 with valid json" in {
        val expectedResult = IncomeSourceIdModel("1234567890")

        stubPostWithResponseBody(url, OK, Json.toJson(model).toString(), Json.toJson(expectedResult).toString())

        implicit val hc: HeaderCarrier = HeaderCarrier()
        val result = await(connector.createIncomeSource(nino, model)(hc))

        result mustBe Right(expectedResult)
      }
    }
    "return a failed result" when {
      "DES Returns a 200 with invalid json" in {
        val expectedResult = ErrorModel(INTERNAL_SERVER_ERROR, ErrorBodyModel.parsingError)

        stubPostWithResponseBody(url, OK, Json.toJson(model).toString(), Json.obj("invalidJson" -> "test").toString())

        implicit val hc: HeaderCarrier = HeaderCarrier()
        val result = await(connector.createIncomeSource(nino, model)(hc))

        result mustBe Left(expectedResult)
      }
      "DES Returns a BadRequest" in {
        val expectedResult = ErrorModel(BAD_REQUEST, ErrorBodyModel("INVALID_IDTYPE", "ID is invalid"))

        val responseBody = Json.obj(
          "code" -> "INVALID_IDTYPE",
          "reason" -> "ID is invalid"
        )

        stubPostWithResponseBody(url, BAD_REQUEST, Json.toJson(model).toString(), responseBody.toString())

        implicit val hc: HeaderCarrier = HeaderCarrier()
        val result = await(connector.createIncomeSource(nino, model)(hc))

        result mustBe Left(expectedResult)
      }
      "DES Returns a Conflict" in {
        val expectedResult = ErrorModel(CONFLICT, ErrorBodyModel("MAX_ACCOUNTS_REACHED",
          "The remote endpoint has indicated that the maximum savings accounts reached."))

        val responseBody = Json.obj(
          "code" -> "MAX_ACCOUNTS_REACHED",
          "reason" -> "The remote endpoint has indicated that the maximum savings accounts reached."
        )

        stubPostWithResponseBody(url, CONFLICT, Json.toJson(model).toString(), responseBody.toString())

        implicit val hc: HeaderCarrier = HeaderCarrier()
        val result = await(connector.createIncomeSource(nino, model)(hc))

        result mustBe Left(expectedResult)
      }
      "DES Returns a SERVICE_UNAVAILABLE" in {
        val expectedResult = ErrorModel(SERVICE_UNAVAILABLE, ErrorBodyModel("SERVICE_UNAVAILABLE", "The service is currently unavailable"))

        val responseBody = Json.obj(
          "code" -> "SERVICE_UNAVAILABLE",
          "reason" -> "The service is currently unavailable"
        )

        stubPostWithResponseBody(url, SERVICE_UNAVAILABLE, Json.toJson(model).toString(), responseBody.toString())

        implicit val hc: HeaderCarrier = HeaderCarrier()
        val result = await(connector.createIncomeSource(nino, model)(hc))

        result mustBe Left(expectedResult)
      }
      "DES Returns a INTERNAL_SERVER_ERROR" in {
        val expectedResult = ErrorModel(INTERNAL_SERVER_ERROR, ErrorBodyModel("SERVER_ERROR", "Internal Server Error"))

        val responseBody = Json.obj(
          "code" -> "SERVER_ERROR",
          "reason" -> "Internal Server Error"
        )

        stubPostWithResponseBody(url, INTERNAL_SERVER_ERROR, Json.toJson(model).toString(), responseBody.toString())

        implicit val hc: HeaderCarrier = HeaderCarrier()
        val result = await(connector.createIncomeSource(nino, model)(hc))

        result mustBe Left(expectedResult)
      }
      "DES Returns a unexpected response" in {
        val expectedResult = ErrorModel(INTERNAL_SERVER_ERROR, ErrorBodyModel.parsingError)


        stubPostWithoutResponseBody(url, NOT_IMPLEMENTED, Json.toJson(model).toString())

        implicit val hc: HeaderCarrier = HeaderCarrier()
        val result = await(connector.createIncomeSource(nino, model)(hc))

        result mustBe Left(expectedResult)
      }
    }
  }
}
