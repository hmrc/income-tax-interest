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

import connectors.httpParsers.IncomeSourceListParser
import models.NamedInterestDetailsModel
import play.api.http.Status._
import play.api.libs.json.Json
import services.GetInterestsService
import testUtils.TestSuite
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

class GetInterestsControllerSpec extends TestSuite{

  val service = mock[GetInterestsService]
  val controller = new GetInterestsController(mockControllerComponents, authorisedAction, service)

  ".getIncomeSource" should {

    "Return a list with one interest source" in {

      val serviceResult = Right(Json.toJson(List(NamedInterestDetailsModel("IncomeSource1", "incomeSourceId", Some(29.54), Some(36.28)))))
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

    "Return an internalServerError when first call fails because invalid Json is returned" in {
      val serviceResult = Left(IncomeSourceListParser.IncomeSourcesInvalidJson)

      def serviceCallMock() = (service.getInterestsList(_: String, _: String)(_: HeaderCarrier, _: ExecutionContext))
        .expects("nino", "2020", *, *)
        .returning(Future.successful(serviceResult))

      val result = {
        mockAuth()
        serviceCallMock()
        controller.getIncomeSource("nino", "2020", "someMtditid")(fakeRequestWithMtditid)
      }

      status(result) mustBe INTERNAL_SERVER_ERROR

    }

    "Return an internalServerError when first call fails because the submission is invalid" in {
      val serviceResult = Left(IncomeSourceListParser.InvalidSubmission)

      def serviceCallMock() = (service.getInterestsList(_: String, _: String)(_: HeaderCarrier, _: ExecutionContext))
        .expects("nino", "2020", *, *)
        .returning(Future.successful(serviceResult))

      val result = {
        mockAuth()
        serviceCallMock()
        controller.getIncomeSource("nino", "2020", "someMtditid")(fakeRequestWithMtditid)
      }

      status(result) mustBe BAD_REQUEST

    }

    "Return an internalServerError when first call fails because the submission is Not found" in {
      val serviceResult = Left(IncomeSourceListParser.NotFoundException)

      def serviceCallMock() = (service.getInterestsList(_: String, _: String)(_: HeaderCarrier, _: ExecutionContext))
        .expects("nino", "2020", *, *)
        .returning(Future.successful(serviceResult))

      val result = {
        mockAuth()
        serviceCallMock()
        controller.getIncomeSource("nino", "2020", "someMtditid")(fakeRequestWithMtditid)
      }

      status(result) mustBe NOT_FOUND

    }

    "Return an internalServerError when first call fails because of an internal server error upstream" in {
      val serviceResult = Left(IncomeSourceListParser.InternalServerErrorUpstream)

      def serviceCallMock() = (service.getInterestsList(_: String, _: String)(_: HeaderCarrier, _: ExecutionContext))
        .expects("nino", "2020", *, *)
        .returning(Future.successful(serviceResult))

      val result = {
        mockAuth()
        serviceCallMock()
        controller.getIncomeSource("nino", "2020", "someMtditid")(fakeRequestWithMtditid)
      }

      status(result) mustBe INTERNAL_SERVER_ERROR

    }

    "Return an internalServerError when first call fails because of a service unavailable error" in {
      val serviceResult = Left(IncomeSourceListParser.UpstreamServiceUnavailable)

      def serviceCallMock() = (service.getInterestsList(_: String, _: String)(_: HeaderCarrier, _: ExecutionContext))
        .expects("nino", "2020", *, *)
        .returning(Future.successful(serviceResult))

      val result = {
        mockAuth()
        serviceCallMock()
        controller.getIncomeSource("nino", "2020", "someMtditid")(fakeRequestWithMtditid)
      }

      status(result) mustBe SERVICE_UNAVAILABLE

    }

    "Return an internalServerError when first call fails because of a an unexpected status" in {
      val serviceResult = Left(IncomeSourceListParser.UnexpectedStatus)

      def serviceCallMock() = (service.getInterestsList(_: String, _: String)(_: HeaderCarrier, _: ExecutionContext))
        .expects("nino", "2020", *, *)
        .returning(Future.successful(serviceResult))

      val result = {
        mockAuth()
        serviceCallMock()
        controller.getIncomeSource("nino", "2020", "someMtditid")(fakeRequestWithMtditid)
      }

      status(result) mustBe INTERNAL_SERVER_ERROR

    }

  }

}
