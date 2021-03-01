/*
 * Copyright 2021 HM Revenue & Customs
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

import models.{DesErrorBodyModel, DesErrorModel, NamedInterestDetailsModel}
import org.scalamock.handlers.CallHandler4
import play.api.http.Status._
import play.api.libs.json.Json
import services.GetInterestsService
import testUtils.TestSuite
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

class GetInterestsControllerSpec extends TestSuite{

  val nino: String = "AA23456A"
  val mtditid: String = "1234567890"
  val taxYear: String = "2020"
  val service: GetInterestsService = mock[GetInterestsService]
  val controller = new GetInterestsController(mockControllerComponents, authorisedAction, service)
  val notFoundModel: DesErrorBodyModel = DesErrorBodyModel("NOT_FOUND", "No data can be found")
  val serviceUnavailableModel: DesErrorBodyModel = DesErrorBodyModel("SERVICE_UNAVAILABLE", "The service is currently unavailable")
  val badRequestModel: DesErrorBodyModel = DesErrorBodyModel("BAD_REQUEST", "The supplied NINO is invalid")
  val internalServerErrorModel: DesErrorBodyModel = DesErrorBodyModel("INTERNAL_SERVER_ERROR", "There has been an unexpected error")

  def mockGetIncomeSourceNotFound(): CallHandler4[String, String, HeaderCarrier, ExecutionContext,Future[Either[DesErrorModel, List[NamedInterestDetailsModel]]]] = {
    val invalidIncomeSource: Either[DesErrorModel, List[NamedInterestDetailsModel]] = Left(DesErrorModel(NOT_FOUND, notFoundModel))
    (service.getInterestsList(_: String, _: String)(_: HeaderCarrier, _: ExecutionContext))
      .expects("nino", "2020", *, *)
      .returning(Future.successful(invalidIncomeSource))
  }

  def mockGetIncomeSourceServiceUnavailable(): CallHandler4[String, String, HeaderCarrier, ExecutionContext, Future[Either[DesErrorModel, List[NamedInterestDetailsModel]]]] ={
    val invalidIncomeSource: Either[DesErrorModel, List[NamedInterestDetailsModel]] = Left(DesErrorModel(SERVICE_UNAVAILABLE,serviceUnavailableModel))
    (service.getInterestsList(_: String, _: String)(_: HeaderCarrier, _: ExecutionContext))
      .expects("nino", "2020", *, *)
      .returning(Future.successful(invalidIncomeSource))
  }

  def mockGetIncomeSourceBadRequest(): CallHandler4[String, String, HeaderCarrier, ExecutionContext, Future[Either[DesErrorModel, List[NamedInterestDetailsModel]]]] ={
    val invalidIncomeSource: Either[DesErrorModel, List[NamedInterestDetailsModel]] = Left(DesErrorModel(BAD_REQUEST, badRequestModel))
    (service.getInterestsList(_: String, _: String)(_: HeaderCarrier, _: ExecutionContext))
      .expects("nino", "2020", *, *)
      .returning(Future.successful(invalidIncomeSource))
  }

  def mockGetIncomeSourceInternalServerError(): CallHandler4[String, String, HeaderCarrier, ExecutionContext, Future[Either[DesErrorModel, List[NamedInterestDetailsModel]]]] ={
    val invalidIncomeSource: Either[DesErrorModel, List[NamedInterestDetailsModel]] = Left(DesErrorModel(INTERNAL_SERVER_ERROR, internalServerErrorModel))
    (service.getInterestsList(_: String, _: String)(_: HeaderCarrier, _: ExecutionContext))
      .expects("nino", "2020", *, *)
      .returning(Future.successful(invalidIncomeSource))
  }


  ".getIncomeSource" should {

    "Return a list with one interest source" in {

      val serviceResult = Right(List(NamedInterestDetailsModel("IncomeSource1", "incomeSourceId", Some(29.54), Some(36.28))))
      val finalResult = Json.toJson(List(NamedInterestDetailsModel("IncomeSource1", "incomeSourceId", Some(29.54), Some(36.28)))).toString()

      def serviceCallMock() = (service.getInterestsList(_: String, _: String)(_: HeaderCarrier, _: ExecutionContext))
        .expects("nino", "2020", *, *)
        .returning(Future.successful(serviceResult))


      val result = {
        mockAuth()
        serviceCallMock()
        controller.getIncomeSource("nino", "2020", "someMtditid")(fakeRequestWithMtditid)
      }

      status(result) mustBe OK
      bodyOf(result) mustBe finalResult

    }

    "without existing interest sources" should {

      "return a NotFound when called as an individual" in {

        val result = {
          mockAuth()
          mockGetIncomeSourceNotFound()
          controller.getIncomeSource("nino", "2020", "someMtditid")(fakeRequestWithMtditid)
        }
        status(result) mustBe NOT_FOUND
      }

      "return a NotFound response when called as an agent" in {

        val result = {
          mockAuthAsAgent()
          mockGetIncomeSourceNotFound()
          controller.getIncomeSource("nino", "2020", "someMtditid")(fakeRequestWithMtditid)
        }
        status(result) mustBe NOT_FOUND
      }


    }

    "With an invalid NINO" should {

      "return a BadRequest as an individual" in {

        val result = {
          mockAuth()
          mockGetIncomeSourceBadRequest()
          controller.getIncomeSource("nino", "2020", "someMtditid")(fakeRequestWithMtditid)
        }
        status(result) mustBe BAD_REQUEST
      }

      "return a BadRequest as an agent" in {

        val result = {
          mockAuthAsAgent()
          mockGetIncomeSourceBadRequest()
          controller.getIncomeSource("nino", "2020", "someMtditid")(fakeRequestWithMtditid)
        }
        status(result) mustBe BAD_REQUEST
      }
    }

    "with an unavailable service" should {

      "return a ServiceUnavailable response when called as an individual" in {

        val result = {
          mockAuth()
          mockGetIncomeSourceServiceUnavailable()
          controller.getIncomeSource("nino", "2020", "someMtditid")(fakeRequestWithMtditid)
        }
        status(result) mustBe SERVICE_UNAVAILABLE

      }

      "return a ServiceUnavailable response when called as an agent" in {

        val result = {
          mockAuthAsAgent()
          mockGetIncomeSourceServiceUnavailable()
          controller.getIncomeSource("nino", "2020", "someMtditid")(fakeRequestWithMtditid)
        }
        status(result) mustBe SERVICE_UNAVAILABLE
      }
    }

    "with something that causes an internal server error in DES" should {

      "return an InternalServerError when called as an individual" in {

        val result = {
          mockAuth()
          mockGetIncomeSourceInternalServerError()
          controller.getIncomeSource("nino", "2020", "someMtditid")(fakeRequestWithMtditid)
        }
        status(result) mustBe INTERNAL_SERVER_ERROR
      }

      "return an InternalServerError when called as an agent" in {

        val result ={
          mockAuthAsAgent()
          mockGetIncomeSourceInternalServerError()
          controller.getIncomeSource("nino", "2020", "someMtditid")(fakeRequestWithMtditid)
        }
        status(result) mustBe INTERNAL_SERVER_ERROR
      }
    }
  }

}
