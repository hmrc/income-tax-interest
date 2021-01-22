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

import connectors.httpParsers.{IncomeSourceListParser, IncomeSourcesDetailsParser}
import connectors.{GetIncomeSourceDetailsConnector, GetIncomeSourceListConnector}
import connectors.httpParsers.IncomeSourceListParser.IncomeSourceListResponse
import models.{DesErrorBodyModel, DesErrorModel, IncomeSourceModel, InterestDetailsModel, NamedInterestDetailsModel}
import play.api.libs.json.Json
import testUtils.TestSuite
import uk.gov.hmrc.http.HeaderCarrier
import play.api.http.Status._

import scala.concurrent.Future

class GetInterestsServiceSpec extends TestSuite {

  val listConnector: GetIncomeSourceListConnector = mock[GetIncomeSourceListConnector]
  val detailsConnector: GetIncomeSourceDetailsConnector = mock[GetIncomeSourceDetailsConnector]
  val service = new GetInterestsService(listConnector, detailsConnector)

  ".getInterests" should {

    "return the correct response when all calls succeed" in {
      val expectedList: IncomeSourceListResponse = Right(List(IncomeSourceModel("incomeSourceId", "incomeSourceType", "incomeSourceName")))
      val expectedDetails = Right(InterestDetailsModel("incomeSourceId", Some(29.89), Some(67.77)))
      val expectedResult = Right(List(NamedInterestDetailsModel("incomeSourceName", "incomeSourceId", Some(29.89), Some(67.77))))

      (listConnector.getIncomeSourceList(_: String, _: String)(_: HeaderCarrier))
        .expects("nino", "2020", *)
        .returning(Future.successful(expectedList))

      (detailsConnector.getIncomeSourceDetails(_: String, _: String, _: String)(_: HeaderCarrier))
        .expects("nino", "2020", "incomeSourceId", *)
        .returning(Future.successful(expectedDetails))


      val result = await(service.getInterestsList("nino", "2020"))

      result mustBe expectedResult
    }

    "return an error response when the call fails" in {

      val expectedList: IncomeSourceListResponse = Right(List(IncomeSourceModel("incomeSourceId", "incomeSourceType", "incomeSourceName")))
      val expectedDetails = Left(DesErrorModel(INTERNAL_SERVER_ERROR, DesErrorBodyModel("InternalServerError", "Server Error")))
      val expectedResult: IncomeSourceListResponse = Left(DesErrorModel(INTERNAL_SERVER_ERROR, DesErrorBodyModel("InternalServerError", "Server Error")))

      (listConnector.getIncomeSourceList(_: String, _: String)(_: HeaderCarrier))
        .expects("nino", "2020", *)
        .returning(Future.successful(expectedList))

      (detailsConnector.getIncomeSourceDetails(_: String, _: String, _: String)(_: HeaderCarrier))
        .expects("nino", "2020", "incomeSourceId", *)
        .returning(Future.successful(expectedDetails))

      val result = await(service.getInterestsList("nino", "2020"))

      result mustBe expectedResult

    }

    "return an error response when there is an incomeSourceId but the call has failed downstream" in {

      val expectedList: IncomeSourceListResponse = Right(List(IncomeSourceModel("incomeSourceId", "incomeSourceType", "incomeSourceName")))
      val expectedDetails = Left(DesErrorModel(NOT_FOUND, DesErrorBodyModel("NotFound", "Unable to find source")))
      val expectedResult = Right(List())

      (listConnector.getIncomeSourceList(_: String, _: String)(_: HeaderCarrier))
        .expects("nino", "2020", *)
        .returning(Future.successful(expectedList))

      (detailsConnector.getIncomeSourceDetails(_: String, _: String, _: String)(_: HeaderCarrier))
        .expects("nino", "2020", "incomeSourceId", *)
        .returning(Future.successful(expectedDetails))

      val result = await(service.getInterestsList("nino", "2020"))

      result mustBe expectedResult

    }
  }
}
