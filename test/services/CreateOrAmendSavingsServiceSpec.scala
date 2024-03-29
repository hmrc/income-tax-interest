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

import connectors.httpParsers.CreateOrAmendSavingsHttpParser.CreateOrAmendSavingsResponse
import connectors.{CreateOrAmendSavingsConnector, CreateOrAmendSavingsTysConnector}
import models._
import testUtils.TestSuite
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

class CreateOrAmendSavingsServiceSpec extends TestSuite {

  val connector: CreateOrAmendSavingsConnector = mock[CreateOrAmendSavingsConnector]
  val tysConnector: CreateOrAmendSavingsTysConnector = mock[CreateOrAmendSavingsTysConnector]
  val service: CreateOrAmendSavingsService = new CreateOrAmendSavingsService(connector, tysConnector)

  val model: CreateOrAmendSavingsModel = CreateOrAmendSavingsModel(
    securities = Some(SecuritiesModel(Some(800.67), 7455.99, Some(6123.2))),
    foreignInterest = Some(Seq(ForeignInterestModel("BES", Some(1232.56), Some(3422.22), Some(5622.67), Some(true), 2821.92)))
  )

  ".createOrAmendSavings" should {

    "return the connector response" in {
      val expectedResult: CreateOrAmendSavingsResponse = Right(true)

      (connector.createOrAmendSavings(_: String, _: Int, _: CreateOrAmendSavingsModel)(_: HeaderCarrier))
        .expects("12345678", 2023, model, *)
        .returning(Future.successful(expectedResult))

      val result = await(service.createOrAmendSavings("12345678", 2023, model))

      result mustBe expectedResult

    }

    "return the tysConnector response" in {
      val expectedResult: CreateOrAmendSavingsResponse = Right(true)

      (tysConnector.createOrAmendSavings(_: String, _: Int, _: CreateOrAmendSavingsModel)(_: HeaderCarrier))
        .expects("12345678", 2024, model, *)
        .returning(Future.successful(expectedResult))

      val result = await(service.createOrAmendSavings("12345678", 2024, model))

      result mustBe expectedResult

    }

  }
}
