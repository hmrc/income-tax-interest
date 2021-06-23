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

import connectors.httpParsers.IncomeSourceListParser.IncomeSourceListResponse
import connectors.{GetIncomeSourceDetailsConnector, GetIncomeSourceListConnector}
import models._
import play.api.http.Status._
import testUtils.TestSuite
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

class GetInterestsServiceSpec extends TestSuite {

  val listConnector: GetIncomeSourceListConnector = mock[GetIncomeSourceListConnector]
  val detailsConnector: GetIncomeSourceDetailsConnector = mock[GetIncomeSourceDetailsConnector]
  val service = new GetInterestsService(listConnector, detailsConnector)

  ".getInterests" should {

    "return the id and name if no details exist" in {
      val expectedList: IncomeSourceListResponse = Right(List(IncomeSourceModel("incomeSourceId", "incomeSourceType", "incomeSourceName")))
      val expectedDetails = Left(DesErrorModel(NOT_FOUND, DesErrorBodyModel("NOT FOUND", "Not found")))
      val expectedResult = Right(List(NamedInterestDetailsModel("incomeSourceName", "incomeSourceId", None, None)))

      (listConnector.getIncomeSourceList(_: String)(_: HeaderCarrier))
        .expects("nino", *)
        .returning(Future.successful(expectedList))

      (detailsConnector.getIncomeSourceDetails(_: String, _: String, _: String)(_: HeaderCarrier))
        .expects("nino", "2020", "incomeSourceId", *)
        .returning(Future.successful(expectedDetails))

      val result = await(service.getInterestsList("nino", "2020"))

      result mustBe expectedResult
    }

    "return the id and name if no details exist along with data with details" in {
      val expectedList: IncomeSourceListResponse = Right(List(IncomeSourceModel("incomeSourceId", "incomeSourceType", "incomeSourceName"),
        IncomeSourceModel("incomeSourceId2", "incomeSourceType2", "incomeSourceName2")))
      val expectedDetails = Left(DesErrorModel(NOT_FOUND, DesErrorBodyModel("NOT FOUND", "Not found")))
      val expectedDetails2 = Right(InterestDetailsModel("incomeSourceId2", Some(29.89), Some(67.77)))
      val expectedResult = Right(List(NamedInterestDetailsModel("incomeSourceName", "incomeSourceId", None, None),
        NamedInterestDetailsModel("incomeSourceName2", "incomeSourceId2", Some(29.89), Some(67.77))))

      (listConnector.getIncomeSourceList(_: String)(_: HeaderCarrier))
        .expects("nino", *)
        .returning(Future.successful(expectedList))

      (detailsConnector.getIncomeSourceDetails(_: String, _: String, _: String)(_: HeaderCarrier))
        .expects("nino", "2020", "incomeSourceId", *)
        .returning(Future.successful(expectedDetails))

      (detailsConnector.getIncomeSourceDetails(_: String, _: String, _: String)(_: HeaderCarrier))
        .expects("nino", "2020", "incomeSourceId2", *)
        .returning(Future.successful(expectedDetails2))

      val result = await(service.getInterestsList("nino", "2020"))

      result mustBe expectedResult
    }

    "return the correct response when all calls succeed" in {
      val expectedList: IncomeSourceListResponse = Right(List(IncomeSourceModel("incomeSourceId", "incomeSourceType", "incomeSourceName")))
      val expectedDetails = Right(InterestDetailsModel("incomeSourceId", Some(29.89), Some(67.77)))
      val expectedResult = Right(List(NamedInterestDetailsModel("incomeSourceName", "incomeSourceId", Some(29.89), Some(67.77))))

      (listConnector.getIncomeSourceList(_: String)(_: HeaderCarrier))
        .expects("nino", *)
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

      (listConnector.getIncomeSourceList(_: String)(_: HeaderCarrier))
        .expects("nino", *)
        .returning(Future.successful(expectedList))

      (detailsConnector.getIncomeSourceDetails(_: String, _: String, _: String)(_: HeaderCarrier))
        .expects("nino", "2020", "incomeSourceId", *)
        .returning(Future.successful(expectedDetails))

      val result = await(service.getInterestsList("nino", "2020"))

      result mustBe expectedResult

    }
  }
}
