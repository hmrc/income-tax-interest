/*
 * Copyright 2022 HM Revenue & Customs
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

import connectors.GetSavingsIncomeDataConnector
import connectors.httpParsers.SavingsIncomeDataParser.SavingsIncomeDataResponse
import models.{ForeignInterestModel, SavingsIncomeDataModel, SecuritiesModel}
import testUtils.TestSuite
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

class GetSavingsIncomeDataServiceSpec extends TestSuite {

  val connector: GetSavingsIncomeDataConnector = mock[GetSavingsIncomeDataConnector]
  val service: GetSavingsIncomeDataService = new GetSavingsIncomeDataService(connector)

  ".getSavingsIncomeData" should {

    "return the connector response" in {

      val expectedResult: SavingsIncomeDataResponse = Right(SavingsIncomeDataModel(
        submittedOn = Some("2020-01-04T05:01:01Z"),
        securities = SecuritiesModel(Some(800.67), 7455.99, Some(6123.2)),
        foreignInterest = Seq(ForeignInterestModel("BES", Some(1232.56), Some(3422.22), Some(5622.67), Some(true), 2821.92))
      ))

      (connector.getSavingsIncomeData(_: String, _: Int)(_: HeaderCarrier))
        .expects("12345678", 1234, *)
        .returning(Future.successful(expectedResult))

      val result = await(service.getSavingsIncomeData("12345678", 1234))

      result mustBe expectedResult

    }
  }
}
