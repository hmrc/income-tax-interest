/*
 * Copyright 2020 HM Revenue & Customs
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
import models.{IncomeSourceModel, InterestDetailsModel, NamedInterestDetailsModel}
import play.api.libs.json.Json
import testUtils.TestSuite
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

class GetInterestsServiceSpec extends TestSuite {

  val listConnector = mock[GetIncomeSourceListConnector]
  val detailsConnector = mock[GetIncomeSourceDetailsConnector]
  val service = new GetInterestsService(listConnector, detailsConnector)

  ".getInterests" should {

    "return the correct response when all calls succeed" in {
      val expectedList: IncomeSourceListResponse = Right(List(IncomeSourceModel("nino", "incomeSourceId", "incomeSourceType", "incomeSourceName")))
      val expectedDetails = Right(InterestDetailsModel("incomeSourceId", Some(29.89), Some(67.77)))
      val expectedResult = Right(Json.toJson(List(NamedInterestDetailsModel("incomeSourceName", "incomeSourceId", Some(29.89), Some(67.77)))))

      (listConnector.getIncomeSourceList(_: String, _ :String)(_: HeaderCarrier))
        .expects("nino", "2020", *)
        .returning(Future.successful(expectedList))

      (detailsConnector.getIncomeSourceDetails(_: String, _: String, _:String)(_: HeaderCarrier))
        .expects("nino", "2020", "incomeSourceId", *)
        .returning(Future.successful(expectedDetails))


      val result = await(service.getInterestsList("nino", "2020"))

      result mustBe expectedResult
    }

    "return the correct response when the first call fails due to IncomeSourcesInvalidJson" in {
      val expectedList: IncomeSourceListResponse = Left(IncomeSourceListParser.IncomeSourcesInvalidJson)
      val expectedResult =  Left(IncomeSourceListParser.IncomeSourcesInvalidJson)

      (listConnector.getIncomeSourceList(_: String, _ :String)(_: HeaderCarrier))
        .expects("nino", "2020", *)
        .returning(Future.successful(expectedList))

      val result = await(service.getInterestsList("nino", "2020"))

      result mustBe expectedResult
    }

    "return the correct response when the first call fails due to InvalidSubmission" in {
      val expectedList: IncomeSourceListResponse = Left(IncomeSourceListParser.InvalidSubmission)
      val expectedResult =  Left(IncomeSourceListParser.InvalidSubmission)

      (listConnector.getIncomeSourceList(_: String, _ :String)(_: HeaderCarrier))
        .expects("nino", "2020", *)
        .returning(Future.successful(expectedList))

      val result = await(service.getInterestsList("nino", "2020"))

      result mustBe expectedResult
    }

    "return the correct response when the first call fails due to NotFoundException" in {
      val expectedList: IncomeSourceListResponse = Left(IncomeSourceListParser.NotFoundException)
      val expectedResult =  Left(IncomeSourceListParser.NotFoundException)

      (listConnector.getIncomeSourceList(_: String, _ :String)(_: HeaderCarrier))
        .expects("nino", "2020", *)
        .returning(Future.successful(expectedList))

      val result = await(service.getInterestsList("nino", "2020"))

      result mustBe expectedResult
    }

    "return the correct response when the first call fails due to InternalServerErrorUpstream" in {
      val expectedList: IncomeSourceListResponse = Left(IncomeSourceListParser.InternalServerErrorUpstream)
      val expectedResult =  Left(IncomeSourceListParser.InternalServerErrorUpstream)

      (listConnector.getIncomeSourceList(_: String, _ :String)(_: HeaderCarrier))
        .expects("nino", "2020", *)
        .returning(Future.successful(expectedList))

      val result = await(service.getInterestsList("nino", "2020"))

      result mustBe expectedResult
    }

    "return the correct response when the first call fails due to UpstreamServiceUnavailable" in {
      val expectedList: IncomeSourceListResponse = Left(IncomeSourceListParser.UpstreamServiceUnavailable)
      val expectedResult =  Left(IncomeSourceListParser.UpstreamServiceUnavailable)

      (listConnector.getIncomeSourceList(_: String, _ :String)(_: HeaderCarrier))
        .expects("nino", "2020", *)
        .returning(Future.successful(expectedList))

      val result = await(service.getInterestsList("nino", "2020"))

      result mustBe expectedResult
    }

    "return the correct response when the first call fails due to UnexpectedStatus" in {
      val expectedList: IncomeSourceListResponse = Left(IncomeSourceListParser.UnexpectedStatus)
      val expectedResult =  Left(IncomeSourceListParser.UnexpectedStatus)

      (listConnector.getIncomeSourceList(_: String, _ :String)(_: HeaderCarrier))
        .expects("nino", "2020", *)
        .returning(Future.successful(expectedList))

      val result = await(service.getInterestsList("nino", "2020"))

      result mustBe expectedResult
    }

    "return the correct response when the second call fails due to InterestDetailsInvalidJson" in {
      val expectedList: IncomeSourceListResponse = Right(List(IncomeSourceModel("nino", "incomeSourceId", "incomeSourceType", "incomeSourceName")))
      val expectedDetails = Left(IncomeSourcesDetailsParser.InterestDetailsInvalidJson)
      val expectedResult =  Left(IncomeSourceListParser.IncomeSourcesInvalidJson)

      (listConnector.getIncomeSourceList(_: String, _ :String)(_: HeaderCarrier))
        .expects("nino", "2020", *)
        .returning(Future.successful(expectedList))

      (detailsConnector.getIncomeSourceDetails(_: String, _: String, _:String)(_: HeaderCarrier))
        .expects("nino", "2020", "incomeSourceId", *)
        .returning(Future.successful(expectedDetails))

      val result = await(service.getInterestsList("nino", "2020"))

      result mustBe expectedResult
    }

    "return the correct response when the second call fails due to InvalidSubmission" in {
      val expectedList: IncomeSourceListResponse = Right(List(IncomeSourceModel("nino", "incomeSourceId", "incomeSourceType", "incomeSourceName")))
      val expectedDetails = Left(IncomeSourcesDetailsParser.InvalidSubmission)
      val expectedResult =  Left(IncomeSourceListParser.IncomeSourcesInvalidJson)

      (listConnector.getIncomeSourceList(_: String, _ :String)(_: HeaderCarrier))
        .expects("nino", "2020", *)
        .returning(Future.successful(expectedList))

      (detailsConnector.getIncomeSourceDetails(_: String, _: String, _:String)(_: HeaderCarrier))
        .expects("nino", "2020", "incomeSourceId", *)
        .returning(Future.successful(expectedDetails))

      val result = await(service.getInterestsList("nino", "2020"))

      result mustBe expectedResult
    }

    "return the correct response when the second call fails due to NotFoundException" in {
      val expectedList: IncomeSourceListResponse = Right(List(IncomeSourceModel("nino", "incomeSourceId", "incomeSourceType", "incomeSourceName")))
      val expectedDetails = Left(IncomeSourcesDetailsParser.NotFoundException)
      val expectedResult =  Right(Json.toJson(List.empty[NamedInterestDetailsModel]))

      (listConnector.getIncomeSourceList(_: String, _ :String)(_: HeaderCarrier))
        .expects("nino", "2020", *)
        .returning(Future.successful(expectedList))

      (detailsConnector.getIncomeSourceDetails(_: String, _: String, _:String)(_: HeaderCarrier))
        .expects("nino", "2020", "incomeSourceId", *)
        .returning(Future.successful(expectedDetails))

      val result = await(service.getInterestsList("nino", "2020"))

      result mustBe expectedResult
    }

    "return the correct response when the second call fails due to InternalServerErrorUpstream" in {
      val expectedList: IncomeSourceListResponse = Right(List(IncomeSourceModel("nino", "incomeSourceId", "incomeSourceType", "incomeSourceName")))
      val expectedDetails = Left(IncomeSourcesDetailsParser.InternalServerErrorUpstream)
      val expectedResult =  Left(IncomeSourceListParser.IncomeSourcesInvalidJson)

      (listConnector.getIncomeSourceList(_: String, _ :String)(_: HeaderCarrier))
        .expects("nino", "2020", *)
        .returning(Future.successful(expectedList))

      (detailsConnector.getIncomeSourceDetails(_: String, _: String, _:String)(_: HeaderCarrier))
        .expects("nino", "2020", "incomeSourceId", *)
        .returning(Future.successful(expectedDetails))

      val result = await(service.getInterestsList("nino", "2020"))

      result mustBe expectedResult
    }

    "return the correct response when the second call fails due to ServiceUnavailable" in {
      val expectedList: IncomeSourceListResponse = Right(List(IncomeSourceModel("nino", "incomeSourceId", "incomeSourceType", "incomeSourceName")))
      val expectedDetails = Left(IncomeSourcesDetailsParser.ServiceUnavailable)
      val expectedResult =  Left(IncomeSourceListParser.IncomeSourcesInvalidJson)

      (listConnector.getIncomeSourceList(_: String, _ :String)(_: HeaderCarrier))
        .expects("nino", "2020", *)
        .returning(Future.successful(expectedList))

      (detailsConnector.getIncomeSourceDetails(_: String, _: String, _:String)(_: HeaderCarrier))
        .expects("nino", "2020", "incomeSourceId", *)
        .returning(Future.successful(expectedDetails))

      val result = await(service.getInterestsList("nino", "2020"))

      result mustBe expectedResult
    }

    "return the correct response when the second call fails due to UnexpectedStatus" in {
      val expectedList: IncomeSourceListResponse = Right(List(IncomeSourceModel("nino", "incomeSourceId", "incomeSourceType", "incomeSourceName")))
      val expectedDetails = Left(IncomeSourcesDetailsParser.UnexpectedStatus)
      val expectedResult =  Left(IncomeSourceListParser.IncomeSourcesInvalidJson)

      (listConnector.getIncomeSourceList(_: String, _ :String)(_: HeaderCarrier))
        .expects("nino", "2020", *)
        .returning(Future.successful(expectedList))

      (detailsConnector.getIncomeSourceDetails(_: String, _: String, _:String)(_: HeaderCarrier))
        .expects("nino", "2020", "incomeSourceId", *)
        .returning(Future.successful(expectedDetails))

      val result = await(service.getInterestsList("nino", "2020"))

      result mustBe expectedResult
    }

  }
}
