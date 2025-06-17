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

import com.github.tomakehurst.wiremock.client.WireMock._
import models._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.{EitherValues, OptionValues}
import play.api.Configuration
import play.api.http.Status._
import play.api.libs.json.Json
import uk.gov.hmrc.http.test.{HttpClientV2Support, WireMockSupport}
import uk.gov.hmrc.http.{HeaderCarrier, HeaderNames, RequestId}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import scala.concurrent.ExecutionContext.Implicits.global

class CreateIncomeSourcesConnectorISpec
  extends AnyFreeSpec
    with Matchers
    with OptionValues
    with ScalaFutures
    with WireMockSupport
    with HttpClientV2Support
    with EitherValues {

  private val hipAuthToken = "Bearer test-token"

  private val servicesConfig = new ServicesConfig(Configuration.from(Map(
    "microservice.services.hip.protocol" -> "http",
    "microservice.services.hip.host" -> wireMockHost,
    "microservice.services.hip.port" -> wireMockPort,
    "microservice.services.hip.authorisation-token" -> "test-token"
  )))

  val connector = new CreateIncomeSourceConnectorImpl(httpClientV2, servicesConfig)

  val nino = "nino"
  val url = s"/itsd/income-sources/$nino"

  val incomeSourceName = "testName"
  val model: InterestSubmissionModel = InterestSubmissionModel(incomeSourceName = incomeSourceName)

  "CreateIncomeSourcesConnector" - {
    "when the response is 200" - {
      "when the response body can be parsed" - {
        "return the incomeSourceId include internal headers" in {
          val requestId = RequestId("request-id-value")
          val hc = HeaderCarrier(requestId = Some(requestId))

          val expectedResponse = IncomeSourceIdModel("income-source-id-value")

          stubFor(
            post(urlEqualTo(url))
              .withRequestBody(equalToJson(Json.toJson(model).toString))
              .withHeader("correlationId", equalTo(requestId.value))
              .withHeader(HeaderNames.authorisation, equalTo(hipAuthToken))
              .willReturn(
                aResponse()
                  .withStatus(OK)
                  .withHeader("correlationId", requestId.value)
                  .withBody(Json.toJson(expectedResponse).toString)
              )
          )

          val result = connector.createIncomeSource(nino, model)(hc).futureValue

          result.value shouldEqual expectedResponse
        }
      }

      "when the response body cannot be parsed" - {
        "return an error" in {
          val requestId = RequestId("request-id-value")
          val hc = HeaderCarrier(requestId = Some(requestId))

          stubFor(
            post(urlEqualTo(url))
              .withRequestBody(equalToJson(Json.toJson(model).toString))
              .withHeader("correlationId", equalTo(requestId.value))
              .withHeader(HeaderNames.authorisation, equalTo(hipAuthToken))
              .willReturn(
                aResponse()
                  .withStatus(OK)
                  .withHeader("correlationId", requestId.value)
                  .withBody(Json.obj("key" -> "value").toString)
              )
          )

          val result = connector.createIncomeSource(nino, model)(hc).futureValue

          result.left.value.status shouldBe 500
        }
      }
    }

    "when the response is 400" - {
      "return an error" in {
        val requestId = RequestId("request-id-value")
        val hc = HeaderCarrier(requestId = Some(requestId))

        stubFor(
          post(urlEqualTo(url))
            .withRequestBody(equalToJson(Json.toJson(model).toString))
            .withHeader("correlationId", equalTo(requestId.value))
            .withHeader(HeaderNames.authorisation, equalTo(hipAuthToken))
            .willReturn(
              aResponse()
                .withStatus(BAD_REQUEST)
                .withHeader("correlationId", requestId.value)
                .withBody(Json.obj("key" -> "value").toString)
            )
        )

        val result = connector.createIncomeSource(nino, model)(hc).futureValue

        result.left.value.status shouldBe 500
      }
    }

    "when the response is 500" - {
      "return an error when the error message can be parsed" in {
        val requestId = RequestId("request-id-value")
        val hc = HeaderCarrier(requestId = Some(requestId))

        stubFor(
          post(urlEqualTo(url))
            .withRequestBody(equalToJson(Json.toJson(model).toString))
            .withHeader("correlationId", equalTo(requestId.value))
            .withHeader(HeaderNames.authorisation, equalTo(hipAuthToken))
            .willReturn(
              aResponse()
                .withStatus(INTERNAL_SERVER_ERROR)
                .withHeader("correlationId", requestId.value)
                .withBody(Json.toJson(ErrorBodyModel("100001", "")).toString)
            )
        )

        val result = connector.createIncomeSource(nino, model)(hc).futureValue

        result.left.value.status shouldBe 500
      }

      "return when the error message cannot be parsed" in {
        val requestId = RequestId("request-id-value")
        val hc = HeaderCarrier(requestId = Some(requestId))

        stubFor(
          post(urlEqualTo(url))
            .withRequestBody(equalToJson(Json.toJson(model).toString))
            .withHeader("correlationId", equalTo(requestId.value))
            .withHeader(HeaderNames.authorisation, equalTo(hipAuthToken))
            .willReturn(
              aResponse()
                .withStatus(INTERNAL_SERVER_ERROR)
                .withHeader("correlationId", requestId.value)
                .withBody(Json.obj("key" -> "value").toString)
            )
        )

        val result = connector.createIncomeSource(nino, model)(hc).futureValue

        result.left.value.status shouldBe 500
      }
    }

    "when the response status is not an expected value" - {
      "return an error" in {
        val requestId = RequestId("request-id-value")
        val hc = HeaderCarrier(requestId = Some(requestId))

        stubFor(
          post(urlEqualTo(url))
            .withRequestBody(equalToJson(Json.toJson(model).toString))
            .withHeader("correlationId", equalTo(requestId.value))
            .withHeader(HeaderNames.authorisation, equalTo(hipAuthToken))
            .willReturn(
              aResponse()
                .withStatus(IM_A_TEAPOT)
                .withHeader("correlationId", requestId.value)
                .withBody(Json.obj("key" -> "value").toString)
            )
        )

        val result = connector.createIncomeSource(nino, model)(hc).futureValue

        result.left.value.status shouldBe 500
      }
    }
  }
}
