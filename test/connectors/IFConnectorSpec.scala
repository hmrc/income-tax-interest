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

import config.AppConfig
import org.scalamock.scalatest.MockFactory
import support.providers.AppConfigStubProvider
import support.stubs.AppConfigStub
import testUtils.TestSuite
import uk.gov.hmrc.http.HeaderNames._
import uk.gov.hmrc.http.{Authorization, HeaderCarrier, SessionId}

class IFConnectorSpec extends TestSuite
  with MockFactory
  with AppConfigStubProvider {

  private val underTest = new IFConnector {
    override protected val appConfig: AppConfig = new AppConfigStub().config()
  }

  ".baseUrl" should {
    "return the app config + '/if' value when environment is test" in {
      val underTest = new IFConnector {
        override protected val appConfig: AppConfig = new AppConfigStub().config("test")
      }

      underTest.baseUrl mustBe appConfigStub.ifBaseUrl + "/if"
    }

    "return the app config value when environment is not test" in {
      val underTest = new IFConnector {
        override protected val appConfig: AppConfig = new AppConfigStub().config("not-test")
      }

      underTest.baseUrl mustBe appConfigStub.ifBaseUrl
    }
  }

  ".ifHeaderCarrier" should {
    "return correct HeaderCarrier when internal host" in {
      val internalHost = "http://localhost"

      val result = underTest.ifHeaderCarrier(internalHost, "some-api-version")(HeaderCarrier())

      result.authorization mustBe Some(Authorization(s"Bearer ${appConfigStub.authorisationTokenFor("some-api-version")}"))
      result.extraHeaders mustBe Seq("Environment" -> appConfigStub.ifEnvironment)
    }

    "return correct HeaderCarrier when external host" in {
      val externalHost = "http://127.0.0.1"
      val hc = HeaderCarrier(sessionId = Some(SessionId("sessionIdHeaderValue")))

      val result = underTest.ifHeaderCarrier(externalHost, "some-api-version")(hc)

      result.extraHeaders.size mustBe 4
      result.extraHeaders.contains(xSessionId -> "sessionIdHeaderValue") mustBe true
      result.extraHeaders.contains(authorisation -> s"Bearer ${appConfigStub.authorisationTokenFor("some-api-version")}") mustBe true
      result.extraHeaders.contains("Environment" -> appConfigStub.ifEnvironment) mustBe true
      result.extraHeaders.exists(x => x._1.equalsIgnoreCase(xRequestChain)) mustBe true
    }
  }
}