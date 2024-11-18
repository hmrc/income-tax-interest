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
import models._
import models.mongo.JourneyAnswers
import models.taskList.SectionTitle.InterestTitle
import models.taskList.TaskStatus.{Completed, InProgress}
import models.taskList._
import play.api.http.Status.{IM_A_TEAPOT, NOT_FOUND}
import play.api.libs.json.{JsArray, JsObject, JsString, Json}
import support.mocks.{MockGetInterestsService, MockGetSavingsIncomeDataService, MockJourneyAnswersRepository}
import testUtils.{MockAppConfig, TestSuite}

import java.time.Instant

class CommonTaskListServiceSpec extends TestSuite
  with MockJourneyAnswersRepository
  with MockGetInterestsService
  with MockGetSavingsIncomeDataService {

  trait Test {
    val service: CommonTaskListService = new CommonTaskListService(
      appConfig = mockAppConfig,
      interestsService = mockGetInterestsService,
      savingsIncomeDataService = mockGetSavingsService,
      journeyAnswersRepository = mockJourneyAnswersRepo
    )

    val nino: String = "12345678"
    val taxYear: Int = 1234
    val mtdItId: String = "1234567890"

    val fullInterestResult: Right[ErrorModel, List[NamedInterestDetailsModel]] =
      Right(List[NamedInterestDetailsModel](
        NamedInterestDetailsModel("AccountID", "IncomeSourceID", Some(20.00), Some(20.00))
      ))

    val emptyInterestResult: Left[ErrorModel, List[NamedInterestDetailsModel]] =
      Left(ErrorModel(NOT_FOUND, ErrorBodyModel("SOME_CODE", "reason")))

    val fullGiltEdgeOrAccruedResult: SavingsIncomeDataResponse = Right(SavingsIncomeDataModel(
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

    val emptyGiltEdgeResult: SavingsIncomeDataResponse = Left(ErrorModel(NOT_FOUND, ErrorBodyModel("SOME_CODE", "reason")))

    val emptyTaskList: TaskListSection = TaskListSection(InterestTitle, None)

    val baseUrl = "http://localhost:9308"
    val banksAndBuildingsUrl = s"$baseUrl/update-and-submit-income-tax-return/personal-income/$taxYear/interest/check-interest"
    val trustFundBondUrl = s"$baseUrl/update-and-submit-income-tax-return/personal-income/$taxYear/interest/check-interest"
    val giltEdgedUrl = s"$baseUrl/update-and-submit-income-tax-return/personal-income/$taxYear/interest/check-interest-from-securities"

    val completedTaskSection: TaskListSection =
      TaskListSection(
        sectionTitle = SectionTitle.InterestTitle,
        taskItems = Some(List(
          TaskListSectionItem(TaskTitle.BanksAndBuilding, TaskStatus.Completed, Some(banksAndBuildingsUrl)),
          TaskListSectionItem(TaskTitle.TrustFundBond, TaskStatus.Completed, Some(trustFundBondUrl)),
          TaskListSectionItem(TaskTitle.GiltEdged, TaskStatus.Completed, Some(giltEdgedUrl)),
        ))
      )

    val inProgressTaskSection: TaskListSection =
      TaskListSection(
        sectionTitle = SectionTitle.InterestTitle,
        taskItems = Some(List(
          TaskListSectionItem(TaskTitle.BanksAndBuilding, TaskStatus.InProgress, Some(banksAndBuildingsUrl)),
          TaskListSectionItem(TaskTitle.TrustFundBond, TaskStatus.InProgress, Some(trustFundBondUrl)),
          TaskListSectionItem(TaskTitle.GiltEdged, TaskStatus.InProgress, Some(giltEdgedUrl)),
        ))
      )

    def journeyAnswers(journey: String, status: String): JourneyAnswers = JourneyAnswers(
      mtdItId = mtdItId,
      taxYear = taxYear,
      journey = journey,
      data = Json.obj("status" -> JsString(status)),
      lastUpdated = Instant.MIN
    )
  }

  "CommonTaskList" when {
    "an error occurs while attempting to retrieve data from DES" should {
      "handle appropriately when DES returns an API error" in new Test {
        mockGetInterestsList(nino, taxYear.toString, emptyInterestResult)
        mockGetSavingsIncomeData(nino, taxYear, emptyGiltEdgeResult)

        def result: TaskListSection = await(service.get(taxYear, nino, mtdItId))
        result mustBe emptyTaskList
      }

      "handle appropriately when an exception occurs during interests data retrieval" in new Test {
        mockGetInterestsListException(nino, taxYear.toString, new RuntimeException("Dummy exception"))

        def result: TaskListSection = await(service.get(taxYear, nino, mtdItId))
        assertThrows[RuntimeException](result)
      }

      "handle appropriately when an exception occurs during savings income data retrieval" in new Test {
        mockGetInterestsList(nino, taxYear.toString, Left(ErrorModel(IM_A_TEAPOT, ErrorBodyModel("foo", "bar"))))
        mockGetSavingsIncomeDataException(nino, taxYear, new RuntimeException("Dummy exception"))

        def result: TaskListSection = await(service.get(taxYear, nino, mtdItId))
        assertThrows[RuntimeException](result)
      }
    }

    "an error occurs while attempting to retrieve Journey Answers from the Journey Answers repository" should {
      "handle appropriately for Banks and Buildings Journey Answers" in new Test {
        mockGetInterestsList(nino, taxYear.toString, fullInterestResult)
        mockGetJourneyAnswersException(mtdItId, taxYear, "uk-interest", new RuntimeException("Dummy Error"))

        def result: TaskListSection = await(service.get(taxYear, nino, mtdItId))
        assertThrows[RuntimeException](result)
      }

      "handle appropriately for Trust Fund Bond Journey Answers" in new Test {
        mockGetInterestsList(nino, taxYear.toString, fullInterestResult)
        mockGetJourneyAnswers(mtdItId, taxYear, "uk-interest", None)
        mockGetJourneyAnswersException(mtdItId, taxYear, "uk-interest", new RuntimeException("Dummy Error"))

        def result: TaskListSection = await(service.get(taxYear, nino, mtdItId))
        assertThrows[RuntimeException](result)
      }

      "handle appropriately for Gilt Edged Journey Answers" in new Test {
        mockGetInterestsList(nino, taxYear.toString, emptyInterestResult)
        mockGetSavingsIncomeData(nino, taxYear, fullGiltEdgeOrAccruedResult)
        mockGetJourneyAnswersException(mtdItId, taxYear, "gilt-edged", new RuntimeException("Dummy Error"))

        def result: TaskListSection = await(service.get(taxYear, nino, mtdItId))
        assertThrows[RuntimeException](result)
      }
    }

    "no relevant savings or interests data is returned from DES" should {
      "return empty task list" in new Test {
        mockGetInterestsList(nino, taxYear.toString, Right(Nil))
        mockGetSavingsIncomeData(nino, taxYear, Right(SavingsIncomeDataModel(None, None, None)))
        mockGetJourneyAnswers(mtdItId, taxYear, "uk-interest", None)
        mockGetJourneyAnswers(mtdItId, taxYear, "uk-interest", None)
        mockGetJourneyAnswers(mtdItId, taxYear, "gilt-edged", None)

        def result: TaskListSection = await(service.get(taxYear, nino, mtdItId))
        result mustBe emptyTaskList
      }
    }

    "relevant data is returned from DES for interests and savings" should {
      "populate task list based on Journey Answers statuses when Journey Answers are defined" in new Test {
        mockGetInterestsList(nino, taxYear.toString, fullInterestResult)
        mockGetSavingsIncomeData(nino, taxYear, fullGiltEdgeOrAccruedResult)
        mockGetJourneyAnswers(mtdItId, taxYear, "uk-interest", Some(journeyAnswers("uk-interest", "completed")))
        mockGetJourneyAnswers(mtdItId, taxYear, "uk-interest", Some(journeyAnswers("uk-interest", "inProgress")))
        mockGetJourneyAnswers(mtdItId, taxYear, "gilt-edged", Some(journeyAnswers("gilt-edged", "inProgress")))

        def result: TaskListSection = await(service.get(taxYear, nino, mtdItId))
        result mustBe TaskListSection(InterestTitle, Some(Seq(
          TaskListSectionItem(TaskTitle.BanksAndBuilding, Completed, Some(banksAndBuildingsUrl)),
          TaskListSectionItem(TaskTitle.TrustFundBond, InProgress, Some(trustFundBondUrl)),
          TaskListSectionItem(TaskTitle.GiltEdged, InProgress, Some(giltEdgedUrl))
        )))
      }

      "return 'Completed' status when Journey Answers are not defined and hyfJourneyEnabled is false" in new Test {
        val hyfDisabledAppConfig: MockAppConfig = new MockAppConfig{
          override val hyfJourneyEnabled: Boolean = false
        }

        override val service: CommonTaskListService = new CommonTaskListService(
          appConfig = hyfDisabledAppConfig,
          interestsService = mockGetInterestsService,
          savingsIncomeDataService = mockGetSavingsService,
          journeyAnswersRepository = mockJourneyAnswersRepo
        )

        mockGetInterestsList(nino, taxYear.toString, fullInterestResult)
        mockGetSavingsIncomeData(nino, taxYear, fullGiltEdgeOrAccruedResult)
        mockGetJourneyAnswers(mtdItId, taxYear, "uk-interest", None)
        mockGetJourneyAnswers(mtdItId, taxYear, "uk-interest", None)
        mockGetJourneyAnswers(mtdItId, taxYear, "gilt-edged", None)

        def result: TaskListSection = await(service.get(taxYear, nino, mtdItId))
        result mustBe completedTaskSection
      }

      "return 'InProgress' status when Journey Answers are not defined and hyfJourneyEnabled is true" in new Test {
        val hyfDisabledAppConfig: MockAppConfig = new MockAppConfig{
          override val hyfJourneyEnabled: Boolean = true
        }

        override val service: CommonTaskListService = new CommonTaskListService(
          appConfig = hyfDisabledAppConfig,
          interestsService = mockGetInterestsService,
          savingsIncomeDataService = mockGetSavingsService,
          journeyAnswersRepository = mockJourneyAnswersRepo
        )

        mockGetInterestsList(nino, taxYear.toString, fullInterestResult)
        mockGetSavingsIncomeData(nino, taxYear, fullGiltEdgeOrAccruedResult)
        mockGetJourneyAnswers(mtdItId, taxYear, "uk-interest", None)
        mockGetJourneyAnswers(mtdItId, taxYear, "uk-interest", None)
        mockGetJourneyAnswers(mtdItId, taxYear, "gilt-edged", None)

        def result: TaskListSection = await(service.get(taxYear, nino, mtdItId))
        result mustBe inProgressTaskSection
      }


      "return 'Not Started' status when Journey Answers are defined but a status cannot be parsed" in new Test {
        mockGetInterestsList(nino, taxYear.toString, fullInterestResult)
        mockGetSavingsIncomeData(nino, taxYear, fullGiltEdgeOrAccruedResult)

        mockGetJourneyAnswers(
          mtdItId,
          taxYear,
          "uk-interest",
          Some(journeyAnswers("uk-interest", ""))
        )

        mockGetJourneyAnswers(
          mtdItId,
          taxYear,
          "uk-interest",
          Some(journeyAnswers("uk-interest", "somethingRandom"))
        )

        mockGetJourneyAnswers(
          mtdItId,
          taxYear,
          "gilt-edged",
          Some(journeyAnswers("gilt-edged", "").copy(data = Json.obj("status" -> JsArray())))
        )

        val expected: TaskListSection = TaskListSection(
          sectionTitle = SectionTitle.InterestTitle,
          taskItems = Some(List(
            TaskListSectionItem(TaskTitle.BanksAndBuilding, TaskStatus.NotStarted, Some(banksAndBuildingsUrl)),
            TaskListSectionItem(TaskTitle.TrustFundBond, TaskStatus.NotStarted, Some(trustFundBondUrl)),
            TaskListSectionItem(TaskTitle.GiltEdged, TaskStatus.NotStarted, Some(giltEdgedUrl)),
          ))
        )

        def result: TaskListSection = await(service.get(taxYear, nino, mtdItId))
        result mustBe expected
      }
    }

    "JourneyAnswers are retrieved but 'data' parameter has no 'status' field" should {
      "handle appropriately" in new Test {
        mockGetInterestsList(nino, taxYear.toString, fullInterestResult)

        mockGetJourneyAnswers(
          mtdItId,
          taxYear,
          "uk-interest",
          Some(journeyAnswers("uk-interest", "").copy(data = JsObject.empty))
        )

        mockGetJourneyAnswers(mtdItId, taxYear, "uk-interest", None)

        def result: TaskListSection = await(service.get(taxYear, nino, mtdItId))
        assertThrows[NoSuchElementException](result)
      }
    }
  }
}
