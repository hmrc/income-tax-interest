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

package services

import connectors.httpParsers.CreateIncomeSourcesHttpParser.CreateIncomeSourcesResponse
import connectors.httpParsers.CreateOrAmendAnnualIncomeSourcePeriodHttpParser.CreateOrAmendAnnualIncomeSourcePeriodResponse
import connectors.httpParsers.CreateOrAmendInterestHttpParser.CreateOrAmendInterestResponse
import connectors.{CreateIncomeSourceConnector, CreateOrAmendAnnualIncomeSourcePeriodConnector, CreateOrAmendInterestConnector}
import models._
import org.scalamock.handlers.{CallHandler, CallHandler4}
import play.api.http.Status._
import testUtils.TestSuite
import uk.gov.hmrc.http.HeaderCarrier
import utils.TaxYearUtils

import scala.concurrent.Future

class CreateOrAmendInterestServiceSpec extends TestSuite {

  implicit val hc: HeaderCarrier = HeaderCarrier()

  val createIncomeSourceConnector = mock[CreateIncomeSourceConnector]
  val createOrAmendAnnualIncomeSourcePeriodConnector = mock[CreateOrAmendAnnualIncomeSourcePeriodConnector]
  val createOrAmendInterestConnector = mock[CreateOrAmendInterestConnector]

  val service = new CreateOrAmendInterestService(createOrAmendInterestConnector,
                                     createOrAmendAnnualIncomeSourcePeriodConnector,
                                     createIncomeSourceConnector)

  val nino = "nino"
  val taxYear = 2021
  val specificTaxYear: Int = TaxYearUtils.specificTaxYear
  val specificTaxYearPlusOne: Int = specificTaxYear + 1
  val incomeSourceName = "incomeSourceNameTest"
  val incomeSourceId = "incomeSourceIdTest"

  val notFoundModel: ErrorModel = ErrorModel(NOT_FOUND, ErrorBodyModel("NotFound", "Unable to find source"))
  val internalServerErrorModel: ErrorModel = ErrorModel(INTERNAL_SERVER_ERROR, ErrorBodyModel("InternalServerError", "Internal Server Error"))

  val interestDetailsModel: InterestDetailsModel = InterestDetailsModel(incomeSourceId, Some(100.00), Some(100.00))
  val submissionModel: InterestSubmissionModel = InterestSubmissionModel(incomeSourceType = UKBankAccount, incomeSourceName = incomeSourceName)
  val connectorResult: IncomeSourceIdModel = IncomeSourceIdModel(incomeSourceId)
  val submittedModelWithId: CreateOrAmendInterestModel = CreateOrAmendInterestModel(Some(incomeSourceId), incomeSourceName, Some(100.00), Some(100.00))
  val submittedModelWithoutId: CreateOrAmendInterestModel = CreateOrAmendInterestModel(None, incomeSourceName, Some(100.00), Some(100.00))

  def createOrAmendInterestMockSuccess: CallHandler4[String, Int, InterestDetailsModel, HeaderCarrier, Future[CreateOrAmendInterestResponse]] =
    (createOrAmendInterestConnector.createOrAmendInterest(_: String, _: Int, _: InterestDetailsModel)(_: HeaderCarrier))
    .expects(nino, taxYear, interestDetailsModel,  *)
    .returning(Future.successful(Right(Done)))

  def createOrAmendInterestMockFailure(expectedErrorModel: ErrorModel): CallHandler[Future[CreateOrAmendInterestResponse]] =
    (createOrAmendInterestConnector.createOrAmendInterest(_: String, _: Int, _: InterestDetailsModel)(_: HeaderCarrier))
    .expects(nino, taxYear, interestDetailsModel,  *)
    .returning(Future.successful(Left(expectedErrorModel)))

  def createOrAmendAnnualIncomeSourcePeriodMockSuccess(taxYear:Int): CallHandler4[String,
    Int, InterestDetailsModel, HeaderCarrier, Future[CreateOrAmendAnnualIncomeSourcePeriodResponse]] =
    (createOrAmendAnnualIncomeSourcePeriodConnector.createOrAmendAnnualIncomeSourcePeriod(_: String, _: Int, _: InterestDetailsModel)(_: HeaderCarrier))
      .expects(nino, taxYear, interestDetailsModel, *)
      .returning(Future.successful(Right(Done)))

  def createOrAmendAnnualIncomeSourcePeriodMockFailure(expectedErrorModel: ErrorModel, taxYear:Int):
  CallHandler[Future[CreateOrAmendAnnualIncomeSourcePeriodResponse]] =
    (createOrAmendAnnualIncomeSourcePeriodConnector.createOrAmendAnnualIncomeSourcePeriod(_: String, _: Int, _: InterestDetailsModel)(_: HeaderCarrier))
      .expects(nino, taxYear, interestDetailsModel, *)
      .returning(Future.successful(Left(expectedErrorModel)))

  def createIncomeSourceConnectorMockSuccess: CallHandler[Future[CreateIncomeSourcesResponse]] =
    (createIncomeSourceConnector.createIncomeSource(_: String, _: InterestSubmissionModel)(_: HeaderCarrier))
      .expects(nino, submissionModel, *)
      .returning(Future.successful(Right(connectorResult)))

  def createIncomeSourceConnectorMockFailure(expectedErrorModel: ErrorModel): CallHandler[Future[CreateIncomeSourcesResponse]] =
    (createIncomeSourceConnector.createIncomeSource(_: String, _: InterestSubmissionModel)(_: HeaderCarrier))
    .expects(nino, submissionModel, *)
    .returning(Future.successful(Left(expectedErrorModel)))

  ".createOrAmendInterest" should {

    "return a Right(Done) " in {

      val expectedResult = Right(Done)

      createOrAmendInterestMockSuccess

      val result = await(service.createOrAmendInterest(nino, taxYear, interestDetailsModel))

      result mustBe expectedResult
    }

    "return a Left(notFoundError) and not retry call to createOrAmendInterest" in {

      val expectedResult = Left(notFoundModel)

      createOrAmendInterestMockFailure(notFoundModel)

      val result = await(service.createOrAmendInterest(nino, taxYear, interestDetailsModel))

      result mustBe expectedResult
    }

    "return a Left(internalServerError) and retry call to createOrAmendInterest 3 times" in {

      val expectedResult = Left(internalServerErrorModel)

      createOrAmendInterestMockFailure(internalServerErrorModel).repeat(3)

      val result = await(service.createOrAmendInterest(nino, taxYear, interestDetailsModel))

      result mustBe expectedResult
    }
  }

  ".createOrAmendInterest with specific tax year" should {

    "return a Right(Done) " in {

      val expectedResult = Right(Done)

      createOrAmendAnnualIncomeSourcePeriodMockSuccess(specificTaxYear)

      val result = await(service.createOrAmendInterest(nino, specificTaxYear, interestDetailsModel))

      result mustBe expectedResult
    }

    "return a Left(notFoundError) and not retry call to createOrAmendInterest" in {

      val expectedResult = Left(notFoundModel)

      createOrAmendAnnualIncomeSourcePeriodMockFailure(notFoundModel, specificTaxYear)

      val result = await(service.createOrAmendInterest(nino, specificTaxYear, interestDetailsModel))

      result mustBe expectedResult
    }

    "return a Left(internalServerError) and retry call to createOrAmendInterest 3 times" in {

      val expectedResult = Left(internalServerErrorModel)

      createOrAmendAnnualIncomeSourcePeriodMockFailure(internalServerErrorModel, specificTaxYear).repeat(3)

      val result = await(service.createOrAmendInterest(nino, specificTaxYear, interestDetailsModel))

      result mustBe expectedResult
    }
  }

  ".createOrAmendInterest with specific tax year plus one" should {

    "return a Right(Done) " in {

      val expectedResult = Right(Done)

      createOrAmendAnnualIncomeSourcePeriodMockSuccess(specificTaxYearPlusOne)

      val result = await(service.createOrAmendInterest(nino, specificTaxYearPlusOne, interestDetailsModel))

      result mustBe expectedResult
    }

    "return a Left(notFoundError) and not retry call to createOrAmendInterest" in {

      val expectedResult = Left(notFoundModel)

      createOrAmendAnnualIncomeSourcePeriodMockFailure(notFoundModel, specificTaxYearPlusOne)

      val result = await(service.createOrAmendInterest(nino, specificTaxYearPlusOne, interestDetailsModel))

      result mustBe expectedResult
    }

    "return a Left(internalServerError) and retry call to createOrAmendInterest 3 times" in {

      val expectedResult = Left(internalServerErrorModel)

      createOrAmendAnnualIncomeSourcePeriodMockFailure(internalServerErrorModel, specificTaxYearPlusOne).repeat(3)

      val result = await(service.createOrAmendInterest(nino, specificTaxYearPlusOne, interestDetailsModel))

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

    "return a Left(notFoundError) and not retry call to createIncomeSourceConnector" in {

      val expectedResult = Left(notFoundModel)

      createIncomeSourceConnectorMockFailure(notFoundModel)

      val result = await(service.getIncomeSourceId(nino, submittedModelWithoutId))

      result mustBe expectedResult
    }

    "return a Left(internalServerError) and retry call to createIncomeSourceConnector 3 times" in {

      val expectedResult = Left(internalServerErrorModel)

      createIncomeSourceConnectorMockFailure(internalServerErrorModel).repeat(3)

      val result = await(service.getIncomeSourceId(nino, submittedModelWithoutId))

      result mustBe expectedResult
    }
  }

  ".createOrAmendAllInterest" should {

    "return a Right(Done) " when {

      "both connectors return successful responses" in {

        val expectedResult = Seq(Right(Done))

        createIncomeSourceConnectorMockSuccess

        createOrAmendInterestMockSuccess

        val result = await(service.createOrAmendAllInterest(nino, taxYear, Seq(submittedModelWithoutId)))

        result mustBe expectedResult
      }
      "the first connector fails twice before success" in {

        val expectedResult = Seq(Right(Done))

        createIncomeSourceConnectorMockFailure(internalServerErrorModel).repeat(2)

        createIncomeSourceConnectorMockSuccess

        createOrAmendInterestMockSuccess

        val result = await(service.createOrAmendAllInterest(nino, taxYear, Seq(submittedModelWithoutId)))

        result mustBe expectedResult
      }
      "the second connector fails twice before success" in {

        val expectedResult = Seq(Right(Done))

        createIncomeSourceConnectorMockSuccess

        createOrAmendInterestMockFailure(internalServerErrorModel).repeat(2)

        createOrAmendInterestMockSuccess

        val result = await(service.createOrAmendAllInterest(nino, taxYear, Seq(submittedModelWithoutId)))

        result mustBe expectedResult
      }
      "both connectors fail twice before success" in {

        val expectedResult = Seq(Right(Done))

        createIncomeSourceConnectorMockFailure(internalServerErrorModel).repeat(2)

        createIncomeSourceConnectorMockSuccess

        createOrAmendInterestMockFailure(internalServerErrorModel).repeat(2)

        createOrAmendInterestMockSuccess

        val result = await(service.createOrAmendAllInterest(nino, taxYear, Seq(submittedModelWithoutId)))

        result mustBe expectedResult
      }
    }
    "return a Left(Error)" when {

      "CreateIncomeSourceConnector fails and CreateOrAmendConnector passes" in {
        val expectedResult = Seq(Left(internalServerErrorModel))

        createIncomeSourceConnectorMockFailure(internalServerErrorModel).repeat(3)

        val result = await(service.createOrAmendAllInterest(nino, taxYear, Seq(submittedModelWithoutId)))

        result mustBe expectedResult
      }

      "CreateIncomeSourceConnector passes and CreateOrAmendConnector fails" in {
        val expectedResult = Seq(Left(internalServerErrorModel))

        createIncomeSourceConnectorMockSuccess

        createOrAmendInterestMockFailure(internalServerErrorModel).repeat(3)

        val result = await(service.createOrAmendAllInterest(nino, taxYear, Seq(submittedModelWithoutId)))

        result mustBe expectedResult
      }
    }
  }
}
