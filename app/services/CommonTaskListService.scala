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

import config.AppConfig
import models.tasklist._
import models.{AllInterest, NamedInterestDetailsModel, SavingsIncomeDataModel}
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CommonTaskListService @Inject()(appConfig: AppConfig,
                                      interestsService: GetInterestsService,
                                      savingsIncomeDataService: GetSavingsIncomeDataService
                                     ) {

  def get(taxYear: Int, nino: String)(implicit ec: ExecutionContext, hc: HeaderCarrier): Future[TaskListSection] = {

    val interest = interestsService.getInterestsList(nino, taxYear.toString).map {
      case Left(_) => List[NamedInterestDetailsModel](NamedInterestDetailsModel("", "", None, None))
      case Right(value) => value
    }

    val savings: Future[SavingsIncomeDataModel] = savingsIncomeDataService.getSavingsIncomeData(nino, taxYear).map {
      case Left(_) => SavingsIncomeDataModel(None, None, None)
      case Right(value) => value
    }


    val allInterest: Future[AllInterest] = for {
      taxedUkInterest <- interest.map(_.find(_.taxedUkInterest.isDefined).flatMap(_.taxedUkInterest))
      untaxedUkInterest <- interest.map(_.find(_.untaxedUkInterest.isDefined).flatMap(_.untaxedUkInterest))
      giftedEdgeOrAccrued <- savings.map(_.securities)
    } yield {
      AllInterest(
        NamedInterestDetailsModel("", "", taxedUkInterest, untaxedUkInterest),
        SavingsIncomeDataModel(None, giftedEdgeOrAccrued, None)
      )
    }

    allInterest.map { i =>
      val tasks: Option[Seq[TaskListSectionItem]] = {
        val optionalTasks: Seq[TaskListSectionItem] = getTasks(i.interestDetails, i.savingsIncomeData, taxYear)
        if (optionalTasks.nonEmpty) {
          Some(optionalTasks)
        } else {
          None
        }
      }
      TaskListSection(SectionTitle.InterestTitle, tasks)
    }
  }

  private def getTasks(interest: NamedInterestDetailsModel, savings: SavingsIncomeDataModel, taxYear: Int): Seq[TaskListSectionItem] = {

    // TODO: these will be links to the new individual CYA pages when they are made
    val bankAndBuildingUrl: String =
      s"${appConfig.personalFrontendBaseUrl}/update-and-submit-income-tax-return/personal-income/$taxYear/interest/check-interest"
    val trustFundUrl: String =
      s"${appConfig.personalFrontendBaseUrl}/update-and-submit-income-tax-return/personal-income/$taxYear/interest/check-interest"
    val giltEdgeUrl: String =
      s"${appConfig.personalFrontendBaseUrl}/update-and-submit-income-tax-return/personal-income/$taxYear/interest/check-interest-from-securities"

    val bankAndBuildingSocieties: Option[TaskListSectionItem] = interest.untaxedUkInterest.map(_ =>
      TaskListSectionItem(TaskTitle.BankAndBuildingSocieties, TaskStatus.Completed, Some(bankAndBuildingUrl)))

    val trustFundBond: Option[TaskListSectionItem] = interest.taxedUkInterest.map(_ =>
      TaskListSectionItem(TaskTitle.TrustFundBond, TaskStatus.Completed, Some(trustFundUrl)))

    val giltEdgedOrAccrued: Option[TaskListSectionItem] = savings.securities.map(_ =>
      TaskListSectionItem(TaskTitle.GiltEdgedOrAccrued, TaskStatus.Completed, Some(giltEdgeUrl)))

    Seq[Option[TaskListSectionItem]](bankAndBuildingSocieties, trustFundBond, giltEdgedOrAccrued).flatten
  }
}
