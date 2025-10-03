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
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HeaderNames, SessionId}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

class CreateOrAmendSavingsConnectorISpec extends PlaySpec with WiremockSpec {

  val model: CreateOrAmendSavingsModel = CreateOrAmendSavingsModel(
    securities = Some(SecuritiesModel(Some(800.67), 7455.99, Some(6123.2))),
    foreignInterest = Some(Seq(ForeignInterestModel("BES", Some(1232.56), Some(3422.22), Some(5622.67), Some(true), 2821.92)))
  )

  val modelNoForeign: CreateOrAmendSavingsModel = CreateOrAmendSavingsModel(
    securities = Some(SecuritiesModel(Some(800.67), 7455.99, Some(6123.2))),
    foreignInterest = None
  )

  lazy val connector: CreateOrAmendSavingsConnector = app.injector.instanceOf[CreateOrAmendSavingsConnector]
  implicit val hc: HeaderCarrier = HeaderCarrier()
  val nino = "nino"
  val taxYear = 2021

  def mkTaxYear(taxYear: Int): String = {
    s"${taxYear - 1}-${taxYear.toString takeRight 2}"
  }

  val url = s"/income-tax/income/savings/$nino/${mkTaxYear(taxYear)}"

  lazy val httpClient: HttpClientV2 = app.injector.instanceOf[HttpClientV2]

  def appConfig(desHost: String): AppConfig = new BackendAppConfig(app.injector.instanceOf[Configuration], app.injector.instanceOf[ServicesConfig]) {
    override val desBaseUrl: String = s"http://$desHost:$wireMockPort"
  }

  val desReturned: CreateOrAmendSavingsModel = CreateOrAmendSavingsModel(
    securities = Some(SecuritiesModel(Some(800.67), 7455.99, Some(6123.2))),
    foreignInterest = Some(Seq(ForeignInterestModel("BES", Some(1232.56), Some(3422.22), Some(5622.67), Some(true), 2821.92)))
  )

  val desReturnedNoForeign: CreateOrAmendSavingsModel = CreateOrAmendSavingsModel(
    securities = Some(SecuritiesModel(Some(800.67), 7455.99, Some(6123.2))),
    foreignInterest = None
  )

  val desReturnedEmpty: JsObject = Json.obj()

  " CreateOrAmendSavingsConnector" should {

    "include internal headers" when {

      val headersSentToDes = Seq(
        new HttpHeader(HeaderNames.authorisation, "Bearer secret"),
        new HttpHeader(HeaderNames.xSessionId, "sessionIdValue")
      )

      val externalHost = "127.0.0.1"

      "the host for DES is 'Internal'" in {
        implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId("sessionIdValue")))

        stubPutWithoutResponseBody(url, Json.toJson(desReturned).toString, NO_CONTENT)

        val result = await(connector.createOrAmendSavings(nino, taxYear, model)(hc))

        result mustBe Right(true)
      }

      "the host for DES is 'External'" in {
        implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId("sessionIdValue")))

        stubPutWithoutResponseBody(url, NO_CONTENT, Json.toJson(desReturned).toString, headersSentToDes)

        val connector = new CreateOrAmendSavingsConnector(httpClient, appConfig(externalHost))

        val result = await(connector.createOrAmendSavings(nino, taxYear, model)(hc))

        result mustBe Right(true)
      }
    }

    "return a success result" when {

      "DES returns a 200" in {
        stubPutWithoutResponseBody(url, NO_CONTENT, Json.toJson(desReturned).toString)
        val result = await(connector.createOrAmendSavings(nino, taxYear, model))

        result mustBe Right(true)

      }

      "DES returns a 200 with no foreign interest" in {
        stubPutWithoutResponseBody(url, NO_CONTENT, Json.toJson(desReturnedNoForeign).toString)
        val result = await(connector.createOrAmendSavings(nino, taxYear, modelNoForeign))

        result mustBe Right(true)

      }
    }

    "return a BadRequest response" in {

      val responseBody = Json.obj(
        "code" -> "INVALID_NINO",
        "reason" -> "NINO is invalid"
      )
      val expectedResult = ErrorModel(BAD_REQUEST, ErrorBodyModel("INVALID_NINO", "NINO is invalid"))
      stubPutWithResponseBody(url, BAD_REQUEST, Json.toJson(desReturned).toString(), responseBody.toString())

      implicit val hc: HeaderCarrier = HeaderCarrier()
      val result = await(connector.createOrAmendSavings(nino, taxYear, model)(hc))

      result mustBe Left(expectedResult)
    }

    "return an InternalServerError response" in {

      val responseBody = Json.obj(
        "code" -> "SERVER_ERROR",
        "reason" -> "Internal Server Error"
      )
      val expectedResult = ErrorModel(INTERNAL_SERVER_ERROR, ErrorBodyModel("SERVER_ERROR", "Internal Server Error"))
      stubPutWithResponseBody(url, INTERNAL_SERVER_ERROR, Json.toJson(desReturned).toString(), responseBody.toString())

      implicit val hc: HeaderCarrier = HeaderCarrier()
      val result = await(connector.createOrAmendSavings(nino, taxYear, model)(hc))

      result mustBe Left(expectedResult)
    }

    "return a ServiceUnavailable response" in {

      val responseBody = Json.obj(
        "code" -> "SERVICE_UNAVAILABLE",
        "reason" -> "The service is currently unavailable"
      )
      val expectedResult = ErrorModel(SERVICE_UNAVAILABLE, ErrorBodyModel("SERVICE_UNAVAILABLE", "The service is currently unavailable"))
      stubPutWithResponseBody(url, SERVICE_UNAVAILABLE, Json.toJson(desReturned).toString(), responseBody.toString())

      implicit val hc: HeaderCarrier = HeaderCarrier()
      val result = await(connector.createOrAmendSavings(nino, taxYear, model)(hc))

      result mustBe Left(expectedResult)
    }
  }

}
