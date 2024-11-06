/*
 * Copyright 2024 HM Revenue & Customs
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

import connectors.httpParsers.SavingsIncomeDataParser.SavingsIncomeDataResponse
import models.{NamedInterestDetailsModel, _}
import models.tasklist._
import play.api.http.Status.NOT_FOUND
import support.mocks.MockJourneyAnswersRepository
import testUtils.TestSuite
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

class CommonTaskListServiceSpec extends TestSuite with MockJourneyAnswersRepository {
  val interestsService: GetInterestsService = mock[GetInterestsService]
  val savingsIncomeDataService: GetSavingsIncomeDataService = mock[GetSavingsIncomeDataService]

  val service: CommonTaskListService = new CommonTaskListService(
    appConfig = mockAppConfig,
    interestsService = interestsService,
    savingsIncomeDataService = savingsIncomeDataService,
    journeyAnswersRepository = mockJourneyAnswersRepo
  )

  val nino: String = "12345678"
  val taxYear: Int = 1234
  val mtdItId: String = "1234567890"

  val fullInterestResult: Right[ErrorModel, List[NamedInterestDetailsModel]] =
    Right(List[NamedInterestDetailsModel]{NamedInterestDetailsModel("AccountID", "IncomeSourceID", Some(20.00), Some(20.00))})

  val emptyInterestResult: Left[ErrorModel, List[NamedInterestDetailsModel]] = Left(ErrorModel(NOT_FOUND, ErrorBodyModel("SOME_CODE", "reason")))

  val fullGiltedEdgeOrAccruedResult: SavingsIncomeDataResponse = Right(SavingsIncomeDataModel(
    submittedOn = Some("2020-06-17T10:53:38Z"),
    securities = Some(SecuritiesModel(Some(100.00),100.00,Some(100.00))),
    foreignInterest = Some(Seq(ForeignInterestModel(
      countryCode = "UK",
      amountBeforeTax = Some(20.00),
      taxTakenOff = Some(20.00),
      specialWithholdingTax = Some(20.00),
      foreignTaxCreditRelief = Some(false),
      taxableAmount = 20.00
    )))
  ))

  val emptyGiltedEdgeResult: SavingsIncomeDataResponse = Left(ErrorModel(NOT_FOUND, ErrorBodyModel("SOME_CODE", "reason")))

  val fullTaskSection: TaskListSection =
    TaskListSection(SectionTitle.InterestTitle,
      Some(List(
        TaskListSectionItem(TaskTitle.BanksAndBuilding, TaskStatus.Completed,
          Some("http://localhost:9308/update-and-submit-income-tax-return/personal-income/1234/interest/check-interest")),
        TaskListSectionItem(TaskTitle.TrustFundBond, TaskStatus.Completed,
          Some("http://localhost:9308/update-and-submit-income-tax-return/personal-income/1234/interest/check-interest")),
        TaskListSectionItem(TaskTitle.GiltEdged, TaskStatus.Completed,
          Some("http://localhost:9308/update-and-submit-income-tax-return/personal-income/1234/interest/check-interest-from-securities")),
      ))
    )

  "CommonTaskListService.get" should {

    "return a full task list section model" in {

      (interestsService.getInterestsList(_: String, _: String)(_: HeaderCarrier, _: ExecutionContext))
        .expects(nino, taxYear.toString, *, *)
        .returning(Future.successful(fullInterestResult))

      (savingsIncomeDataService.getSavingsIncomeData(_: String, _: Int)(_: HeaderCarrier))
        .expects(nino, taxYear, *)
        .returning(Future.successful(fullGiltedEdgeOrAccruedResult))

      val underTest = service.get(taxYear, nino, mtdItId)

      await(underTest) mustBe fullTaskSection
    }

    "return a minimal task list section model" in {

      (interestsService.getInterestsList(_: String, _: String)(_: HeaderCarrier, _: ExecutionContext))
        .expects(nino, taxYear.toString, *, *)
        .returning(Future.successful(Right(List[NamedInterestDetailsModel]{NamedInterestDetailsModel("", "",Some(20.00), None)})))

      (savingsIncomeDataService.getSavingsIncomeData(_: String, _: Int)(_: HeaderCarrier))
        .expects(nino, taxYear, *)
        .returning(Future.successful(emptyGiltedEdgeResult))

      val underTest = service.get(taxYear, nino, mtdItId)

      await(underTest) mustBe fullTaskSection.copy(
        taskItems = Some(List(
          TaskListSectionItem(
            TaskTitle.TrustFundBond, TaskStatus.Completed, Some("http://localhost:9308/update-and-submit-income-tax-return/personal-income/1234/interest/check-interest"))
        ))
      )
    }

    "return an empty task list section model" in {

      (interestsService.getInterestsList(_: String, _: String)(_: HeaderCarrier, _: ExecutionContext))
        .expects(nino, taxYear.toString, *, *)
        .returning(Future.successful(emptyInterestResult))

      (savingsIncomeDataService.getSavingsIncomeData(_: String, _: Int)(_: HeaderCarrier))
        .expects(nino, taxYear, *)
        .returning(Future.successful(emptyGiltedEdgeResult))

      val underTest = service.get(taxYear, nino, mtdItId)

      await(underTest) mustBe TaskListSection(SectionTitle.InterestTitle, None)
    }
  }
}
