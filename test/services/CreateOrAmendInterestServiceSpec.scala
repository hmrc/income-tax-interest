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

package services

import connectors.httpParsers.CreateIncomeSourcesHttpParser.CreateIncomeSourcesResponse
import connectors.httpParsers.CreateOrAmendInterestHttpParser.CreateOrAmendInterestResponse
import connectors.{CreateIncomeSourceConnector, CreateOrAmendInterestConnector}
import models._
import org.scalamock.handlers.{CallHandler, CallHandler4}
import testUtils.TestSuite
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

class CreateOrAmendInterestServiceSpec extends TestSuite {


  val createIncomeSourceConnector: CreateIncomeSourceConnector = mock[CreateIncomeSourceConnector]
  val createOrAmendInterestConnector: CreateOrAmendInterestConnector = mock[CreateOrAmendInterestConnector]
  val service: CreateOrAmendInterestService = new CreateOrAmendInterestService(createOrAmendInterestConnector, createIncomeSourceConnector)

  val nino = "nino"
  val taxYear = 2021
  val incomeSourceName = "incomeSourceNameTest"
  val incomeSourceId = "incomeSourceIdTest"

  val interestDetailsModel: InterestDetailsModel = InterestDetailsModel(incomeSourceId, Some(100.00), Some(100.00))
  val submissionModel: InterestSubmissionModel = InterestSubmissionModel(incomeSourceName = incomeSourceName)
  val connectorResult: IncomeSourceIdModel = IncomeSourceIdModel(incomeSourceId)
  val submittedModelWithId: CreateOrAmendInterestModel = CreateOrAmendInterestModel(Some(incomeSourceId), incomeSourceName, Some(100.00), Some(100.00))
  val submittedModelWithoutId: CreateOrAmendInterestModel = CreateOrAmendInterestModel(None, incomeSourceName, Some(100.00), Some(100.00))

  def createOrAmendInterestMockSuccess: CallHandler4[String, Int, InterestDetailsModel, HeaderCarrier, Future[CreateOrAmendInterestResponse]] =
    (createOrAmendInterestConnector.createOrAmendInterest(_: String, _: Int, _: InterestDetailsModel)(_: HeaderCarrier))
    .expects(nino, taxYear, interestDetailsModel,  *)
    .returning(Future.successful(Right(true)))

  def createOrAmendInterestMockFailure: CallHandler[Future[CreateOrAmendInterestResponse]] =
    (createOrAmendInterestConnector.createOrAmendInterest(_: String, _: Int, _: InterestDetailsModel)(_: HeaderCarrier))
    .expects(nino, taxYear, interestDetailsModel,  *)
    .returning(Future.successful(Left(NotFoundError)))

  def createIncomeSourceConnectorMockSuccess: CallHandler[Future[CreateIncomeSourcesResponse]] =
    (createIncomeSourceConnector.createIncomeSource(_: String, _: InterestSubmissionModel)(_: HeaderCarrier))
      .expects(nino, submissionModel, *)
      .returning(Future.successful(Right(connectorResult)))

  def createIncomeSourceConnectorMockFailure: CallHandler[Future[CreateIncomeSourcesResponse]] =
    (createIncomeSourceConnector.createIncomeSource(_: String, _: InterestSubmissionModel)(_: HeaderCarrier))
    .expects(nino, submissionModel, *)
    .returning(Future.successful(Left(NotFoundError)))

  ".createOrAmendInterest" should {

    "return a Right(true) " in {

      val expectedResult = Right(true)

      createOrAmendInterestMockSuccess

      val result = await(service.createOrAmendInterest(nino, taxYear, interestDetailsModel))

      result mustBe expectedResult
    }
    "return a Left(errorResponse)" in {

      val expectedResult = Left(NotFoundError)

      createOrAmendInterestMockFailure.repeat(3)

      val result = await(service.createOrAmendInterest(nino, taxYear, interestDetailsModel))

      result mustBe expectedResult
    }
  }

  ".getIncomeSourceId" should {
    "return a IncomeSourceIdModel " when {
      "Not passed an incomeSourceID" in {

        val expectedResult = Right(InterestDetailsModel(connectorResult.incomeSourceId, Some(100.00), Some(100.00)))

        createIncomeSourceConnectorMockSuccess

        val result = await(service.getIncomeSourceId(nino, submittedModelWithoutId))

        result mustBe expectedResult
      }
      "passed an incomeSourceID" in {

        val expectedResult = Right(InterestDetailsModel(incomeSourceId, Some(100.00), Some(100.00)))

        val result = await(service.getIncomeSourceId(nino, submittedModelWithId))

        result mustBe expectedResult
      }
    }
    "return an error " in{

      val expectedResult = Left(NotFoundError)

      createIncomeSourceConnectorMockFailure.repeat(3)

      val result = await(service.getIncomeSourceId(nino, submittedModelWithoutId))

      result mustBe expectedResult
    }
  }

  ".createOrAmendAllInterest" should {

    "return a Right(true) " when {

      "both connectors return successful responses" in {

        val expectedResult = Seq(Right(true))

        createIncomeSourceConnectorMockSuccess

        createOrAmendInterestMockSuccess

        val result = await(service.createOrAmendAllInterest(nino, taxYear, Seq(submittedModelWithoutId)))

        result mustBe expectedResult
      }
      "the first connector fails twice before success" in {

        val expectedResult = Seq(Right(true))

        createIncomeSourceConnectorMockFailure.repeat(2)

        createIncomeSourceConnectorMockSuccess

        createOrAmendInterestMockSuccess

        val result = await(service.createOrAmendAllInterest(nino, taxYear, Seq(submittedModelWithoutId)))

        result mustBe expectedResult
      }
      "the second connector fails twice before success" in {

        val expectedResult = Seq(Right(true))

        createIncomeSourceConnectorMockSuccess

        createOrAmendInterestMockFailure.repeat(2)

        createOrAmendInterestMockSuccess

        val result = await(service.createOrAmendAllInterest(nino, taxYear, Seq(submittedModelWithoutId)))

        result mustBe expectedResult
      }
      "both connectors fail twice before success" in {

        val expectedResult = Seq(Right(true))

        createIncomeSourceConnectorMockFailure.repeat(2)

        createIncomeSourceConnectorMockSuccess

        createOrAmendInterestMockFailure.repeat(2)

        createOrAmendInterestMockSuccess

        val result = await(service.createOrAmendAllInterest(nino, taxYear, Seq(submittedModelWithoutId)))

        result mustBe expectedResult
      }
    }
    "return a Left(Error)" when {

      "CreateIncomeSourceConnector fails and CreateOrAmendConnector passes" in {
        val expectedResult = Seq(Left(NotFoundError))

        createIncomeSourceConnectorMockFailure.repeat(3)

        val result = await(service.createOrAmendAllInterest(nino, taxYear, Seq(submittedModelWithoutId)))

        result mustBe expectedResult
      }

      "CreateIncomeSourceConnector passes and CreateOrAmendConnector fails" in {
        val expectedResult = Seq(Left(NotFoundError))

        createIncomeSourceConnectorMockSuccess

        createOrAmendInterestMockFailure.repeat(3)

        val result = await(service.createOrAmendAllInterest(nino, taxYear, Seq(submittedModelWithoutId)))

        result mustBe expectedResult
      }
    }
  }
}
