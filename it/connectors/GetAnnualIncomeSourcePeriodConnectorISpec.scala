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
import config.BackendAppConfig
import helpers.WiremockSpec
import models._
import play.api.Configuration
import play.api.http.Status._
import play.api.libs.json.{JsObject, Json}
import uk.gov.hmrc.http.{HeaderCarrier, HeaderNames, HttpClient, SessionId}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import org.scalatestplus.play.PlaySpec
import utils.TaxYearUtils
import utils.TaxYearUtils.convertSpecificTaxYear


class GetAnnualIncomeSourcePeriodConnectorISpec extends PlaySpec with WiremockSpec {
  
  lazy val connector: GetAnnualIncomeSourcePeriodConnector = app.injector.instanceOf[GetAnnualIncomeSourcePeriodConnector]
  implicit val hc: HeaderCarrier = HeaderCarrier()
  val nino = "123456789"
  val specificTaxYear: String = TaxYearUtils.specificTaxYear.toString
  val specificTaxYearPlusOne: String = (TaxYearUtils.specificTaxYear + 1).toString
  val incomeSourceId = "someId"
  val deletedPeriod = Some(false)
  val taxYearParameter: String = convertSpecificTaxYear(specificTaxYear)
  val taxYearParameterPlusOne: String = convertSpecificTaxYear(specificTaxYearPlusOne)

  val url: String = s"/income-tax/$taxYearParameter/$nino/income-source/savings/annual\\?deleteReturnPeriod=false&incomeSourceId=$incomeSourceId"
  val urlPlusOne: String = s"/income-tax/$taxYearParameterPlusOne/$nino/income-source/savings/annual\\?deleteReturnPeriod=false&incomeSourceId=$incomeSourceId"

  lazy val httpClient: HttpClient = app.injector.instanceOf[HttpClient]

  def appConfig(ifHost: String): BackendAppConfig = new BackendAppConfig(app.injector.instanceOf[Configuration], app.injector.instanceOf[ServicesConfig]) {
    override lazy val ifBaseUrl: String = s"http://$ifHost:$wireMockPort"
  }

  val model: InterestDetailsModel = InterestDetailsModel(incomeSourceId, Some(29.99), Some(37.65))
  val ifReturned: JsObject = Json.obj(
    "savingsInterestAnnualIncome" -> Json.arr(model)
  )
  val ifReturnedEmpty: JsObject = Json.obj(
    "savingsInterestAnnualIncome" -> Json.arr()
  )

  ".InterestDetailsIfModel" should {

    "include internal headers" when {

      val headersSentToIf = Seq(
        new HttpHeader(HeaderNames.authorisation, "Bearer secret"),
        new HttpHeader(HeaderNames.xSessionId, "sessionIdValue")
      )

      val internalHost = "localhost"
      val externalHost = "127.0.0.1"

      "the host for IF is 'Internal'" in {
        implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId("sessionIdValue")))

        stubGetWithResponseBody(url, OK, ifReturned.toString())
        val connector = new GetAnnualIncomeSourcePeriodConnector(httpClient, appConfig(internalHost))
        val result = await(connector.getAnnualIncomeSourcePeriod(nino, specificTaxYear, incomeSourceId, deletedPeriod)(hc))

        result mustBe Right(model)
      }


      "the host for IF is 'External'" in {
        implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId("sessionIdValue")))

        stubGetWithResponseBody(url, OK, ifReturned.toString(),headersSentToIf)
        val connector = new GetAnnualIncomeSourcePeriodConnector(httpClient, appConfig(externalHost))
        val result = await(connector.getAnnualIncomeSourcePeriod(nino, specificTaxYear, incomeSourceId, deletedPeriod)(hc))

        result mustBe Right(model)
      }
    }

    "return a success result" when {

      "IF returns a 200 for specific tax year" in {
        stubGetWithResponseBody(url, OK, ifReturned.toString())
        val result = await(connector.getAnnualIncomeSourcePeriod(nino, specificTaxYear, incomeSourceId, deletedPeriod))

        result mustBe Right(model)

      }

      "IF returns a 200 for specific tax year plus one" in {
        stubGetWithResponseBody(urlPlusOne, OK, ifReturned.toString())
        val result = await(connector.getAnnualIncomeSourcePeriod(nino, specificTaxYearPlusOne, incomeSourceId, deletedPeriod))

        result mustBe Right(model)

      }
    }

    "return an error" when {

      "IF returns an empty 200" in {
        stubGetWithResponseBody(url, OK, ifReturnedEmpty.toString())
        val result = await(connector.getAnnualIncomeSourcePeriod(nino, specificTaxYear, incomeSourceId, deletedPeriod))
        val expectedResult = ErrorModel(INTERNAL_SERVER_ERROR, ErrorBodyModel.parsingError)

        result mustBe Left(expectedResult)

      }
    }

    "return a InternalServerError parsing error when incorrectly parsed" in {

      val invalidJson = Json.obj(
        "savingsInterestAnnualIncome" -> "test"
      )

      val expectedResult = ErrorModel(INTERNAL_SERVER_ERROR, ErrorBodyModel.parsingError)
      stubGetWithResponseBody(url, INTERNAL_SERVER_ERROR, invalidJson.toString())
      implicit val hc: HeaderCarrier = HeaderCarrier()
      val result = await(connector.getAnnualIncomeSourcePeriod(nino, specificTaxYear, incomeSourceId, deletedPeriod))

      result mustBe Left(expectedResult)
    }

    "return a parsing error InternalServerError when it is a bad success response" in {

      val invalidJson = Json.obj(
        "savingsInterestAnnualIncome" -> Json.arr(
          ""
        )
      )

      val expectedResult = ErrorModel(INTERNAL_SERVER_ERROR, ErrorBodyModel.parsingError)
      stubGetWithResponseBody(url, OK, invalidJson.toString())

      implicit val hc: HeaderCarrier = HeaderCarrier()
      val result = await(connector.getAnnualIncomeSourcePeriod(nino, specificTaxYear, incomeSourceId, deletedPeriod)(hc))

      result mustBe Left(expectedResult)
    }

    "return a NoContent response" in {

      val expectedResult = ErrorModel(INTERNAL_SERVER_ERROR, ErrorBodyModel.parsingError)
      stubGetWithResponseBody(url, NO_CONTENT, "{}")

      implicit val hc: HeaderCarrier = HeaderCarrier()
      val result = await(connector.getAnnualIncomeSourcePeriod(nino, specificTaxYear, incomeSourceId, deletedPeriod)(hc))

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
      val result = await(connector.getAnnualIncomeSourcePeriod(nino, specificTaxYear, incomeSourceId, deletedPeriod)(hc))

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
      val result = await(connector.getAnnualIncomeSourcePeriod(nino, specificTaxYear, incomeSourceId, deletedPeriod)(hc))

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
      val result = await(connector.getAnnualIncomeSourcePeriod(nino, specificTaxYear, incomeSourceId, deletedPeriod)(hc))

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
      val result = await(connector.getAnnualIncomeSourcePeriod(nino, specificTaxYear, incomeSourceId, deletedPeriod)(hc))

      result mustBe Left(expectedResult)
    }

    "return an Unprocessable entity Error when IF throws an unexpected result that isn't parsable" in {
      val responseBody = Json.obj(
        "code" -> "UNPROCESSABLE_ENTITY"
      )
      val expectedResult = ErrorModel(UNPROCESSABLE_ENTITY, ErrorBodyModel.parsingError)

      stubGetWithResponseBody(url, UNPROCESSABLE_ENTITY, responseBody.toString())
      implicit val hc: HeaderCarrier = HeaderCarrier()
      val result = await(connector.getAnnualIncomeSourcePeriod(nino, specificTaxYear, incomeSourceId, deletedPeriod)(hc))

      result mustBe Left(expectedResult)
    }

  }
}
