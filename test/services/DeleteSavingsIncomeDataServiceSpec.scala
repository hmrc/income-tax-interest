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

import connectors.{DeleteSavingsIncomeDataConnector, DeleteSavingsIncomeDataTysConnector}
import models.Done
import testUtils.TestSuite
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future
class DeleteSavingsIncomeDataServiceSpec extends TestSuite {

  val connector: DeleteSavingsIncomeDataConnector = mock[DeleteSavingsIncomeDataConnector]
  val tysConnector: DeleteSavingsIncomeDataTysConnector = mock[DeleteSavingsIncomeDataTysConnector]
  val service: DeleteSavingsIncomeDataService = new DeleteSavingsIncomeDataService(connector, tysConnector)

  "DeleteSavingsIncomeData" should {

    "return the connector response" in {
      val expectedResult = Right(Done)

      (connector.deleteSavingsIncomeData(_: String, _: Int)(_: HeaderCarrier))
        .expects("12345678", 2023, *)
        .returning(Future.successful(expectedResult))

      val result = await(service.deleteSavingsIncomeData("12345678", 2023))
      result mustBe expectedResult
    }

    "return the TYS connector response" in {
      val expectedResult = Right(Done)

      (tysConnector.deleteSavingsIncomeData(_: String, _: Int)(_: HeaderCarrier))
        .expects("12345678", 2024, *)
        .returning(Future.successful(expectedResult))

      val result = await(service.deleteSavingsIncomeData("12345678", 2024))
      result mustBe expectedResult
    }

  }

}
