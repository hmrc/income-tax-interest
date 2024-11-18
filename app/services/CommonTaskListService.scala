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

import cats.data.EitherT
import config.AppConfig
import models.ErrorModel
import models.mongo.JourneyAnswers
import models.taskList.TaskStatus.{Completed, InProgress, NotStarted}
import models.taskList.TaskTitle
import models.taskList._
import play.api.Logging
import repositories.JourneyAnswersRepository
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CommonTaskListService @Inject()(appConfig: AppConfig,
                                      interestsService: GetInterestsService,
                                      savingsIncomeDataService: GetSavingsIncomeDataService,
                                      journeyAnswersRepository: JourneyAnswersRepository) extends Logging {

  // TODO: these will be links to the new individual CYA pages when they are made
  private lazy val baseUrl = s"${appConfig.personalFrontendBaseUrl}/update-and-submit-income-tax-return/personal-income"

  private def getTaskForItem(taskTitle: TaskTitle,
                             taskUrl: String,
                             journeyAnswers: Option[JourneyAnswers],
                             isDataDefined: Boolean): Option[TaskListSectionItem] =
    (journeyAnswers, isDataDefined) match {
      case (Some(ja), _) =>
        val status: TaskStatus = ja.data.value("status").validate[TaskStatus].asOpt match {
          case Some(TaskStatus.Completed) => Completed
          case Some(TaskStatus.InProgress) => InProgress
          case _ =>
            logger.info("[CommonTaskListService][getStatus] status stored in an invalid format, setting as 'Not yet started'.")
            NotStarted
        }

        Some(TaskListSectionItem(taskTitle, status, Some(taskUrl)))
      case (_, true) =>
        Some(TaskListSectionItem(taskTitle, if(appConfig.hyfJourneyEnabled) InProgress else Completed, Some(taskUrl)))
      case _ => None
    }

  private def getInterestTasks(taxYear: Int, nino: String, mtdItId: String)
                              (implicit ec: ExecutionContext, hc: HeaderCarrier): Future[Seq[TaskListSectionItem]] = {
    lazy val bankAndBuildingUrl: String = s"$baseUrl/$taxYear/interest/check-interest"
    lazy val trustFundUrl: String = s"$baseUrl/$taxYear/interest/check-interest"

    val banksAndBuildingsJourneyName: String = "uk-interest" //"banks-and-buildings"
    val trustFundBondJourneyName: String = "uk-interest" //"trust-fund-bond"

    val result: EitherT[Future, ErrorModel, Seq[TaskListSectionItem]] = {
      for {
        interestsList <- EitherT(interestsService.getInterestsList(nino, taxYear.toString))
        taxedJourneyAnswers <- EitherT.right(journeyAnswersRepository.get(mtdItId, taxYear, banksAndBuildingsJourneyName))
        untaxedJourneyAnswers <- EitherT.right(journeyAnswersRepository.get(mtdItId, taxYear, trustFundBondJourneyName))
        taxedUkInterestExists = interestsList.exists(_.taxedUkInterest.isDefined)
        untaxedUkInterestExists = interestsList.exists(_.untaxedUkInterest.isDefined)
      } yield Seq(
        getTaskForItem(TaskTitle.BanksAndBuilding, bankAndBuildingUrl, taxedJourneyAnswers, taxedUkInterestExists),
        getTaskForItem(TaskTitle.TrustFundBond, trustFundUrl, untaxedJourneyAnswers, untaxedUkInterestExists)
      ).flatten
    }

    result.leftMap(_ => Nil).merge
  }

  private def getSavingsTasks(taxYear: Int, nino: String, mtdItId: String)
                              (implicit ec: ExecutionContext, hc: HeaderCarrier): Future[Seq[TaskListSectionItem]] = {
    lazy val giltEdgeUrl: String = s"$baseUrl/$taxYear/interest/check-interest-from-securities"
    val giltEdgedJourneyName: String = "gilt-edged"

    val result: EitherT[Future, ErrorModel, Seq[TaskListSectionItem]] = {
      for {
        savings <- EitherT(savingsIncomeDataService.getSavingsIncomeData(nino, taxYear))
        journeyAnswers <- EitherT.right(journeyAnswersRepository.get(mtdItId, taxYear, giltEdgedJourneyName))
        securitiesIsDefined = savings.securities.isDefined
      } yield Seq(
        getTaskForItem(TaskTitle.GiltEdged, giltEdgeUrl, journeyAnswers, securitiesIsDefined)
      ).flatten
    }

    result.leftMap(_ => Nil).merge
  }

  def get(taxYear: Int, nino: String, mtdItId: String)
         (implicit ec: ExecutionContext, hc: HeaderCarrier): Future[TaskListSection] = {

    val result = for {
      interestTasks <- getInterestTasks(taxYear, nino, mtdItId)
      savingsTasks <- getSavingsTasks(taxYear, nino, mtdItId)
      allTasks = interestTasks ++ savingsTasks
    } yield {
      val tasks = if (allTasks.nonEmpty) Some(allTasks) else None
      TaskListSection(SectionTitle.InterestTitle, tasks)
    }

    result
  }
}
