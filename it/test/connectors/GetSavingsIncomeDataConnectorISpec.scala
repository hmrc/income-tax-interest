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

package connectors

import com.github.tomakehurst.wiremock.http.HttpHeader
import config.{AppConfig, BackendAppConfig}
import helpers.WiremockSpec
import models._
import org.scalatestplus.play.PlaySpec
import play.api.Configuration
import play.api.http.Status._
import play.api.libs.json.{JsObject, Json}
import uk.gov.hmrc.http.{HeaderCarrier, HeaderNames, HttpClient, SessionId}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

class GetSavingsIncomeDataConnectorISpec extends PlaySpec with WiremockSpec{

  lazy val connector: GetSavingsIncomeDataConnector = app.injector.instanceOf[GetSavingsIncomeDataConnector]
  implicit val hc: HeaderCarrier = HeaderCarrier()
  val nino = "nino"
  val taxYear = 2021

  def mkTaxYear(taxYear: Int): String = {
    s"${taxYear - 1}-${taxYear.toString takeRight 2}"
  }
  val url = s"/income-tax/income/savings/$nino/${mkTaxYear(taxYear)}"

  lazy val httpClient: HttpClient = app.injector.instanceOf[HttpClient]

  def appConfig(desHost: String): AppConfig = new BackendAppConfig(app.injector.instanceOf[Configuration], app.injector.instanceOf[ServicesConfig]) {
    override val desBaseUrl: String = s"http://$desHost:$wireMockPort"
  }
  
  val desReturned: SavingsIncomeDataModel = SavingsIncomeDataModel(
    submittedOn = Some("2020-01-04T05:01:01Z"),
    securities = Some(SecuritiesModel(Some(800.67), 7455.99, Some(6123.2))),
    foreignInterest = Some(Seq(ForeignInterestModel("BES", Some(1232.56), Some(3422.22), Some(5622.67), Some(true), 2821.92)))
  )

  val desReturnedEmpty: JsObject = Json.obj()

  " GetSavingsIncomeDataConnector" should {

    "include internal headers" when {

      val headersSentToDes = Seq(
        new HttpHeader(HeaderNames.authorisation, "Bearer secret"),
        new HttpHeader(HeaderNames.xSessionId, "sessionIdValue")
      )

      val externalHost = "127.0.0.1"

      "the host for DES is 'Internal'" in {
        implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId("sessionIdValue")))

        stubGetWithResponseBody(url, OK, Json.toJson(desReturned).toString)

        val result = await(connector.getSavingsIncomeData(nino, taxYear)(hc))

        result mustBe Right(desReturned)
      }

      "the host for DES is 'External'" in {
        implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId("sessionIdValue")))

        stubGetWithResponseBody(url, OK, Json.toJson(desReturned).toString,headersSentToDes)

        val connector = new GetSavingsIncomeDataConnector(httpClient, appConfig(externalHost))

        val result = await(connector.getSavingsIncomeData(nino, taxYear)(hc))

        result mustBe Right(desReturned)
      }
    }

    "return a success result" when {

      "DES returns a 200" in {
        stubGetWithResponseBody(url, OK, Json.toJson(desReturned).toString)
        val result = await(connector.getSavingsIncomeData(nino, taxYear))

        result mustBe Right(desReturned)

      }
    }

    "return an error" when {

      "DES returns an empty 200" in {
        stubGetWithResponseBody(url, OK, desReturnedEmpty.toString())
        val result = await(connector.getSavingsIncomeData(nino, taxYear))
        val expectedResult = Right(SavingsIncomeDataModel(None, None, None))

        result mustBe expectedResult

      }
    }

    "return a NoContent response" in {

      val expectedResult = ErrorModel(INTERNAL_SERVER_ERROR, ErrorBodyModel.parsingError)
      stubGetWithResponseBody(url, NO_CONTENT, "{}")

      implicit val hc: HeaderCarrier = HeaderCarrier()
      val result = await(connector.getSavingsIncomeData(nino, taxYear)(hc))

      result mustBe Left(expectedResult)
    }

    "return a BadRequest response" in {

      val responseBody = Json.obj(
        "code" -> "INVALID_NINO",
        "reason" -> "NINO is invalid"
      )
      val expectedResult = ErrorModel(BAD_REQUEST, ErrorBodyModel("INVALID_NINO","NINO is invalid"))
      stubGetWithResponseBody(url, BAD_REQUEST, responseBody.toString())

      implicit val hc: HeaderCarrier = HeaderCarrier()
      val result = await(connector.getSavingsIncomeData(nino, taxYear)(hc))

      result mustBe Left(expectedResult)
    }

    "return a NotFound response" in {

      val responseBody = Json.obj(
        "code" -> "NOT_FOUND_INCOME_SOURCE",
        "reason" -> "Can't find the income source"
      )
      val expectedResult = ErrorModel(NOT_FOUND, ErrorBodyModel("NOT_FOUND_INCOME_SOURCE", "Can't find the income source"))
      stubGetWithResponseBody(url, NOT_FOUND, responseBody.toString())

      implicit val hc: HeaderCarrier = HeaderCarrier()
      val result = await(connector.getSavingsIncomeData(nino, taxYear)(hc))

      result mustBe Left(expectedResult)
    }

    "return an InternalServerError response" in {

      val responseBody = Json.obj(
        "code" -> "SERVER_ERROR",
        "reason" -> "Internal Server Error"
      )
      val expectedResult = ErrorModel(INTERNAL_SERVER_ERROR, ErrorBodyModel("SERVER_ERROR", "Internal Server Error"))
      stubGetWithResponseBody(url, INTERNAL_SERVER_ERROR, responseBody.toString())

      implicit val hc: HeaderCarrier = HeaderCarrier()
      val result = await(connector.getSavingsIncomeData(nino, taxYear)(hc))

      result mustBe Left(expectedResult)
    }

    "return a ServiceUnavailable response" in {

      val responseBody = Json.obj(
        "code" -> "SERVICE_UNAVAILABLE",
        "reason" -> "The service is currently unavailable"
      )
      val expectedResult = ErrorModel(SERVICE_UNAVAILABLE, ErrorBodyModel("SERVICE_UNAVAILABLE", "The service is currently unavailable"))
      stubGetWithResponseBody(url, SERVICE_UNAVAILABLE, responseBody.toString())

      implicit val hc: HeaderCarrier = HeaderCarrier()
      val result = await(connector.getSavingsIncomeData(nino, taxYear)(hc))

      result mustBe Left(expectedResult)
    }
  }

}
