package helpers

import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.libs.json.JsValue

trait WiremockStubHelpers {

  def stubGetWithResponseBody(url: String, status: Int, response: String): StubMapping =
    stubFor(get(urlMatching(url))
      .willReturn(
        aResponse()
          .withStatus(status)
          .withBody(response)
          .withHeader("Content-Type", "application/json; charset=utf-8")))

  def stubGetWithoutResponseBody(url: String, status: Int): StubMapping =
    stubFor(get(urlMatching(url))
      .willReturn(
        aResponse()
          .withStatus(status)))

  def stubPostWithoutResponseBody(url: String, status: Int, requestBody: String): StubMapping =
    stubFor(post(urlEqualTo(url)).withRequestBody(equalToJson(requestBody))
      .willReturn(
        aResponse()
          .withStatus(status)
          .withHeader("Content-Type", "application/json; charset=utf-8")))

  def stubPutWithoutResponseBody(url: String, requestBody: String, status: Int): StubMapping =
    stubFor(put(urlEqualTo(url))
      .willReturn(
        aResponse()
          .withStatus(status)
          .withHeader("Content-Type", "application/json; charset=utf-8")))

  def stubPatchWithoutResponseBody(url: String, requestBody: String, status: Int): StubMapping =
    stubFor(patch(urlEqualTo(url))
      .willReturn(
        aResponse()
          .withStatus(status)
          .withHeader("Content-Type", "application/json; charset=utf-8")))

  def stubPostWithoutResponseAndRequestBody(url: String, status: Int): StubMapping =
    stubFor(post(urlEqualTo(url))
      .willReturn(
        aResponse()
          .withStatus(status)
          .withHeader("Content-Type", "application/json; charset=utf-8")))

  def verifyPostWithRequestBody(url: String, times: Int, body: JsValue): Unit =
    verify(times, postRequestedFor(urlEqualTo(url))
      .withRequestBody(equalToJson(body.toString(), true, true))
    )

  def auditStubs(): Unit = {
    val auditResponseCode = 204
    stubPostWithoutResponseAndRequestBody("/write/audit", auditResponseCode)
  }

}