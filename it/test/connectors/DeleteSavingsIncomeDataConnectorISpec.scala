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
import models.{Done, ErrorBodyModel, ErrorModel}
import org.scalatestplus.play.PlaySpec
import play.api.Configuration
import play.api.http.Status._
import uk.gov.hmrc.http.{HeaderCarrier, HeaderNames, HttpClient, SessionId}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

class DeleteSavingsIncomeDataConnectorISpec extends PlaySpec with WiremockSpec{

  lazy val connector: DeleteSavingsIncomeDataConnector = app.injector.instanceOf[DeleteSavingsIncomeDataConnector]
  implicit val hc: HeaderCarrier = HeaderCarrier()
  val nino = "nino"
  val taxYear = 2021

  def mkTaxYear(taxYear: Int): String = {
    s"${taxYear - 1}-${taxYear.toString takeRight 2}"
  }
  val desUrl = s"/income-tax/income/savings/$nino/${mkTaxYear(taxYear)}"

  lazy val httpClient: HttpClient = app.injector.instanceOf[HttpClient]

  def appConfig(desHost: String): AppConfig = new BackendAppConfig(app.injector.instanceOf[Configuration], app.injector.instanceOf[ServicesConfig]) {
    override val desBaseUrl: String = s"http://$desHost:$wireMockPort"
  }

  "DeleteSavingsIncomeDataConnector " should {

    "include internal headers" when {
      val headersSentToDes = Seq(
        new HttpHeader(HeaderNames.authorisation, "Bearer secret"),
        new HttpHeader(HeaderNames.xSessionId, "sessionIdValue")
      )

      val internalHost = "localhost"
      val externalHost = "127.0.0.1"

      "the host for DES is 'Internal'" in {
        implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId("sessionIdValue")))
        val connector = new DeleteSavingsIncomeDataConnector(httpClient, appConfig(internalHost))

        stubDeleteWithoutResponseBody(desUrl, NO_CONTENT, headersSentToDes)

        val result = await(connector.deleteSavingsIncomeData(nino, taxYear)(hc))

        result mustBe Right(Done)
      }

      "the host for DES is 'External'" in {
        implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId("sessionIdValue")))
        val connector = new DeleteSavingsIncomeDataConnector(httpClient, appConfig(externalHost))

        stubDeleteWithoutResponseBody(desUrl, NO_CONTENT, headersSentToDes)

        val result = await(connector.deleteSavingsIncomeData(nino, taxYear)(hc))

        result mustBe Right(Done)
      }
    }

    "handle error" when {

      val errorBodyModel = ErrorBodyModel("DES_CODE", "DES_REASON")

      Seq(INTERNAL_SERVER_ERROR, SERVICE_UNAVAILABLE, NOT_FOUND, BAD_REQUEST).foreach { status =>

        s"Des returns $status" in {
          val desError = ErrorModel(status, errorBodyModel)
          implicit val hc: HeaderCarrier = HeaderCarrier()

          stubDeleteWithResponseBody(desUrl, status, desError.toJson.toString())

          val result = await(connector.deleteSavingsIncomeData(nino, taxYear)(hc))

          result mustBe Left(desError)
        }
      }

      "DES returns an unexpected error code - 502 BadGateway" in {
        val desError = ErrorModel(INTERNAL_SERVER_ERROR, errorBodyModel)
        implicit val hc: HeaderCarrier = HeaderCarrier()

        stubDeleteWithResponseBody(desUrl, BAD_GATEWAY, desError.toJson.toString())

        val result = await(connector.deleteSavingsIncomeData(nino, taxYear)(hc))

        result mustBe Left(desError)
      }

    }

  }
}
