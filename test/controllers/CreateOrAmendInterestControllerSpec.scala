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

import connectors.httpParsers.CreateOrAmendInterestHttpParser.CreateOrAmendInterestResponse
import models.{CreateOrAmendInterestModel, Done, ErrorBodyModel, ErrorModel}
import org.scalamock.handlers.CallHandler4
import play.api.http.Status._
import play.api.libs.json.Json
import services.CreateOrAmendInterestService
import testUtils.TestSuite
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

class CreateOrAmendInterestControllerSpec extends TestSuite {

  val serviceMock: CreateOrAmendInterestService = mock[CreateOrAmendInterestService]
  val controller = new CreateOrAmendInterestController(serviceMock, mockControllerComponents, authorisedAction)

  val notFoundModel: ErrorModel = ErrorModel(NOT_FOUND, ErrorBodyModel("NotFound", "Unable to find source"))

  val nino = "nino"
  val taxYear = 2021
  val incomeSourceName = "incomeSourceNameTest"
  val incomeSourceId = "incomeSourceIdTest"
  val mtditid = "someMtditid"

  val interestSubmittedModel: Seq[CreateOrAmendInterestModel] = Seq(CreateOrAmendInterestModel(Some(incomeSourceId), incomeSourceName, None, Some(100.00)))
  val interestSubmittedModelInvalid: CreateOrAmendInterestModel = CreateOrAmendInterestModel(Some(incomeSourceId), incomeSourceName, None, Some(100.00))

  val interestSuccessResponse: Future[Seq[CreateOrAmendInterestResponse]] =
    Future.successful(Seq(Right(Done), Right(Done), Right(Done)))

  val interestFailResponse: Future[Seq[CreateOrAmendInterestResponse]] =
    Future.successful(Seq(Right(Done), Left(notFoundModel), Right(Done)))

  def mockServiceSuccessCall: CallHandler4[String, Int, Seq[CreateOrAmendInterestModel], HeaderCarrier, Future[Seq[CreateOrAmendInterestResponse]]] =
    (serviceMock.createOrAmendAllInterest(_: String, _: Int, _: Seq[CreateOrAmendInterestModel])(_: HeaderCarrier))
      .expects(nino, taxYear, interestSubmittedModel, *)
      .returning(interestSuccessResponse)

  def mockServiceFailCall: CallHandler4[String, Int, Seq[CreateOrAmendInterestModel], HeaderCarrier, Future[Seq[CreateOrAmendInterestResponse]]] =
    (serviceMock.createOrAmendAllInterest(_: String, _: Int, _: Seq[CreateOrAmendInterestModel])(_: HeaderCarrier))
      .expects(nino, taxYear, interestSubmittedModel, *)
      .returning(interestFailResponse)

  ".createOrAmendInterest" should {
    "return a no content" when {
      "passed a validModel and there are no failures from the connector" in {

        val expectedResult = NO_CONTENT

        mockAuth()
        mockServiceSuccessCall
        val result = controller.createOrAmendInterest(nino, taxYear)(fakeRequest.withJsonBody(Json.toJson(interestSubmittedModel)))

        status(result) mustBe expectedResult
      }
    }


    "return an error" when {
      "passed a valid model but at least one post fails" in {
        mockAuth()
        mockServiceFailCall
        val result = controller.createOrAmendInterest(nino, taxYear)(fakeRequest.withJsonBody(Json.toJson(interestSubmittedModel)))

        status(result) mustBe NOT_FOUND
      }

      "passed a invalid model" in {
        mockAuth()
        val result = controller.createOrAmendInterest(nino, taxYear)(fakeRequest.withJsonBody(Json.toJson(interestSubmittedModelInvalid)))

        status(result) mustBe BAD_REQUEST
      }

      "passed an empty has body" in {
        mockAuth()
        val result = controller.createOrAmendInterest(nino, taxYear)(fakeRequest)

        status(result) mustBe BAD_REQUEST
      }

    }
  }
}
