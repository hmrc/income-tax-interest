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

package support.mocks

import connectors.httpParsers.SavingsIncomeDataParser.SavingsIncomeDataResponse
import org.scalamock.handlers.CallHandler3
import org.scalamock.scalatest.MockFactory
import org.scalatest.TestSuite
import services.GetSavingsIncomeDataService
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

trait MockGetSavingsIncomeDataService extends MockFactory { _: TestSuite =>
  protected val mockGetSavingsService: GetSavingsIncomeDataService = mock[GetSavingsIncomeDataService]

  private type MockType = CallHandler3[String, Int, HeaderCarrier, Future[SavingsIncomeDataResponse]]

  def mockGetSavingsIncomeData(nino: String,
                               taxYear: Int,
                               result: SavingsIncomeDataResponse): MockType = {
    (mockGetSavingsService.getSavingsIncomeData(_: String, _: Int)(_: HeaderCarrier))
      .expects(nino, taxYear, *)
      .returning(Future.successful(result))
  }

  def mockGetSavingsIncomeDataException(nino: String,
                                        taxYear: Int,
                                        result: Throwable): MockType = {
    (mockGetSavingsService.getSavingsIncomeData(_: String, _: Int)(_: HeaderCarrier))
      .expects(nino, taxYear, *)
      .returning(Future.failed(result))
  }
}
