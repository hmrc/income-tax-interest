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

import connectors.httpParsers.CreateOrAmendSavingsHttpParser.CreateOrAmendSavingsResponse
import models.{CreateOrAmendSavingsModel, ErrorBodyModel, ErrorModel, ForeignInterestModel, SecuritiesModel}
import org.scalamock.handlers.CallHandler4
import play.api.http.Status._
import play.api.libs.json.Json
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import services.CreateOrAmendSavingsService
import testUtils.TestSuite
import uk.gov.hmrc.http.{HeaderCarrier, SessionKeys}

import scala.concurrent.Future

class CreateOrAmendSavingsControllerSpec extends TestSuite {

  val serviceMock: CreateOrAmendSavingsService = mock[CreateOrAmendSavingsService]
  val controller = new CreateOrAmendSavingsController(serviceMock, mockControllerComponents, authorisedAction)

  val notFoundModel: ErrorModel = ErrorModel(NOT_FOUND, ErrorBodyModel("NotFound", "Unable to find source"))
  val serviceUnavailableModel: ErrorModel = ErrorModel(SERVICE_UNAVAILABLE, ErrorBodyModel("SERVICE_UNAVAILABLE", "The service is currently unavailable"))
  val badRequestModel: ErrorModel = ErrorModel(BAD_REQUEST, ErrorBodyModel("BAD_REQUEST", "The supplied NINO is invalid"))
  val internalServerErrorModel: ErrorModel = ErrorModel(INTERNAL_SERVER_ERROR, ErrorBodyModel("INTERNAL_SERVER_ERROR", "There has been an unexpected error"))

  val model: CreateOrAmendSavingsModel = CreateOrAmendSavingsModel(
    securities = Some(SecuritiesModel(Some(800.67), 7455.99, Some(6123.2))),
    foreignInterest = Some(Seq(ForeignInterestModel("BES", Some(1232.56), Some(3422.22), Some(5622.67), Some(true), 2821.92)))
  )

  val nino = "nino"
  val taxYear = 2021
  val mtditid = "1234567890"


  override val fakeRequestWithMtditid: FakeRequest[AnyContentAsEmpty.type] = FakeRequest("PUT", "/").withHeaders("MTDITID" -> mtditid,
    SessionKeys.sessionId -> "someSessionId")


  ".createOrAmendSavings" should {

    "Return a 204 NO Content response with valid CreateOrAmendSavings" in {
      val serviceResult = Right(true)

      def serviceCallMock(): CallHandler4[String, Int, CreateOrAmendSavingsModel, HeaderCarrier, Future[CreateOrAmendSavingsResponse]] =
        (serviceMock.createOrAmendSavings(_: String, _: Int, _: CreateOrAmendSavingsModel)(_: HeaderCarrier))
          .expects(nino, taxYear, model, *)
          .returning(Future.successful(serviceResult))

      val result = {
        mockAuth()
        serviceCallMock()
        controller.createOrAmendSavings(nino, taxYear)(fakeRequestWithMtditid.withJsonBody(Json.toJson(model)))
      }
      status(result) mustBe NO_CONTENT
    }

    "return a Left response" when {
      def mockCreateOrAmendSavingsWithError(errorModel: ErrorModel):
        CallHandler4[String, Int, CreateOrAmendSavingsModel, HeaderCarrier, Future[CreateOrAmendSavingsResponse]] = {
        (serviceMock.createOrAmendSavings(_: String, _: Int, _: CreateOrAmendSavingsModel)(_: HeaderCarrier))
          .expects(nino, taxYear, *, *)
          .returning(Future.successful(Left(errorModel)))
      }

      "the service returns a NO_CONTENT" in {
        val result = {
          mockAuth()
          mockCreateOrAmendSavingsWithError(notFoundModel)
          controller.createOrAmendSavings(nino, taxYear)(fakeRequestWithMtditid.withJsonBody(Json.toJson(model)))
        }
        status(result) mustBe NOT_FOUND
      }

      "the service returns a SERVICE_UNAVAILABLE" in {
        val result = {
          mockAuth()
          mockCreateOrAmendSavingsWithError(serviceUnavailableModel)
          controller.createOrAmendSavings(nino, taxYear)(fakeRequestWithMtditid.withJsonBody(Json.toJson(model)))
        }
        status(result) mustBe SERVICE_UNAVAILABLE
      }
      "the service returns a BAD_REQUEST" in {
        val result = {
          mockAuth()
          mockCreateOrAmendSavingsWithError(badRequestModel)
          controller.createOrAmendSavings(nino, taxYear)(fakeRequestWithMtditid.withJsonBody(Json.toJson(model)))
        }
        status(result) mustBe BAD_REQUEST
      }
      "the service returns a INTERNAL_SERVER_ERROR" in {
        val result = {
          mockAuth()
          mockCreateOrAmendSavingsWithError(internalServerErrorModel)
          controller.createOrAmendSavings(nino, taxYear)(fakeRequestWithMtditid.withJsonBody(Json.toJson(model)))
        }
        status(result) mustBe INTERNAL_SERVER_ERROR
      }
    }
  }

}
