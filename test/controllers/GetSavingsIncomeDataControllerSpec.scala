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

package controllers

import connectors.httpParsers.SavingsIncomeDataParser.SavingsIncomeDataResponse
import models._
import org.scalamock.handlers.CallHandler3
import play.api.http.Status._
import play.api.libs.json.Json
import services.GetSavingsIncomeDataService
import testUtils.TestSuite
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

class GetSavingsIncomeDataControllerSpec extends TestSuite {

  val serviceMock: GetSavingsIncomeDataService = mock[GetSavingsIncomeDataService]
  val controller = new GetSavingsIncomeDataController(serviceMock, mockControllerComponents, authorisedAction)

  val notFoundModel: ErrorModel = ErrorModel(NOT_FOUND, ErrorBodyModel("NotFound", "Unable to find source"))
  val serviceUnavailableModel: ErrorModel =
    ErrorModel(SERVICE_UNAVAILABLE, ErrorBodyModel("SERVICE_UNAVAILABLE", "The service is currently unavailable"))
  val badRequestModel: ErrorModel = ErrorModel(BAD_REQUEST, ErrorBodyModel("BAD_REQUEST", "The supplied NINO is invalid"))
  val internalServerErrorModel: ErrorModel =
    ErrorModel(INTERNAL_SERVER_ERROR, ErrorBodyModel("INTERNAL_SERVER_ERROR", "There has been an unexpected error"))


  val nino = "nino"
  val taxYear = 2021
  val mtditid = "someMtditid"

  val savingsModel: SavingsIncomeDataModel = SavingsIncomeDataModel(
    submittedOn = Some("2020-01-04T05:01:01Z"),
    securities = Some(SecuritiesModel(Some(800.67), 7455.99, Some(6123.2))),
    foreignInterest = Some(Seq(ForeignInterestModel("BES", Some(1232.56), Some(3422.22), Some(5622.67), Some(true), 2821.92)))
  )

  ".getIncomeSource" should {

    "Return a 200 OK response with valid IncomeSavingData" in {

      val serviceResult = Right(savingsModel)
      val finalResult = Json.toJson(savingsModel).toString()

      def serviceCallMock(): CallHandler3[String, Int, HeaderCarrier, Future[SavingsIncomeDataResponse]] =
        (serviceMock.getSavingsIncomeData(_: String, _: Int)(_: HeaderCarrier))
        .expects(nino, taxYear, *)
        .returning(Future.successful(serviceResult))


      val result = {
        mockAuth()
        serviceCallMock()
        controller.getSavingsIncomeData(nino, taxYear)(fakeRequest)
      }

      status(result) mustBe OK
      bodyOf(result) mustBe finalResult

    }

    "return a Left response" when {

      def mockGetSavingIncomeDataWithError(errorModel: ErrorModel): CallHandler3[String, Int, HeaderCarrier, Future[SavingsIncomeDataResponse]] = {
        (serviceMock.getSavingsIncomeData(_: String, _: Int)(_: HeaderCarrier))
          .expects(nino, taxYear, *)
          .returning(Future.successful(Left(errorModel)))
      }

      "the service returns a NO_CONTENT" in {
        val result = {
          mockAuth()
          mockGetSavingIncomeDataWithError(notFoundModel)
          controller.getSavingsIncomeData(nino, taxYear)(fakeRequest)
        }
        status(result) mustBe NOT_FOUND
      }

      "the service returns a SERVICE_UNAVAILABLE" in {
        val result = {
          mockAuth()
          mockGetSavingIncomeDataWithError(serviceUnavailableModel)
          controller.getSavingsIncomeData(nino, taxYear)(fakeRequest)
        }
        status(result) mustBe SERVICE_UNAVAILABLE
      }
      "the service returns a BAD_REQUEST" in {
        val result = {
          mockAuth()
          mockGetSavingIncomeDataWithError(badRequestModel)
          controller.getSavingsIncomeData(nino, taxYear)(fakeRequest)
        }
        status(result) mustBe BAD_REQUEST
      }
      "the service returns a INTERNAL_SERVER_ERROR" in {
        val result = {
          mockAuth()
          mockGetSavingIncomeDataWithError(internalServerErrorModel)
          controller.getSavingsIncomeData(nino, taxYear)(fakeRequest)
        }
        status(result) mustBe INTERNAL_SERVER_ERROR
      }
    }
  }

}
