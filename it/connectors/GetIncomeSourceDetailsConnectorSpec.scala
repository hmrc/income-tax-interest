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

package connectors

import connectors.httpParsers.IncomeSourcesDetailsParser._
import helpers.WiremockSpec
import models.InterestDetailsModel
import org.scalatestplus.play.PlaySpec
import play.api.http.Status._
import play.api.libs.json.{JsObject, Json}
import uk.gov.hmrc.http.HeaderCarrier

class GetIncomeSourceDetailsConnectorSpec extends PlaySpec with WiremockSpec{

  lazy val connector: GetIncomeSourceDetailsConnector = app.injector.instanceOf[GetIncomeSourceDetailsConnector]
  implicit val hc = HeaderCarrier()
  val nino = "nino"
  val taxYear = "2020"
  val incomeSourceId = "someId"
  val url = s"/income-tax/income-sources/nino/$nino\\?incomeSourceType=savings&taxYear=$taxYear&incomeSourceId=$incomeSourceId"

  val model: InterestDetailsModel = InterestDetailsModel(incomeSourceId, Some(29.99), Some(37.65))
  val desReturned: JsObject = Json.obj(
    "savingsInterestAnnualIncome" -> Json.arr(model)
  )
  val desReturnedEmpty: JsObject = Json.obj(
    "savingsInterestAnnualIncome" -> Json.arr()
  )

  ".getIncomeSourceDetails" should {

    "return a success result" when {

      "DES returns a 200" in {
        stubGetWithResponseBody(url, OK, desReturned.toString())
        val result = await(connector.getIncomeSourceDetails(nino, taxYear, incomeSourceId))

        result mustBe Right(model)

      }
    }
    "return a failure result" when {

      "DES returns Empty Json" in {
        stubGetWithResponseBody(url, OK, desReturnedEmpty.toString())
        val result = await(connector.getIncomeSourceDetails(nino, taxYear, incomeSourceId))

        result mustBe Left(InvalidSubmission)
      }

      "DES returns wrong Json" in {
        stubGetWithResponseBody(url, OK, Json.obj("nino" -> nino).toString())
        val result = await(connector.getIncomeSourceDetails(nino, taxYear, incomeSourceId))

        result mustBe Left(InterestDetailsInvalidJson)
      }

      "DES returns BAD_REQUEST" in {
        stubGetWithoutResponseBody(url, BAD_REQUEST)
        val result = await(connector.getIncomeSourceDetails(nino, taxYear, incomeSourceId))

        result mustBe Left(InvalidSubmission)
      }

      "DES returns NOT_FOUND" in {
        stubGetWithoutResponseBody(url, NOT_FOUND)
        val result = await(connector.getIncomeSourceDetails(nino, taxYear, incomeSourceId))

        result mustBe Left(NotFoundException)
      }

      "DES returns INTERNAL_SERVER_ERROR" in {
        stubGetWithoutResponseBody(url, INTERNAL_SERVER_ERROR)
        val result = await(connector.getIncomeSourceDetails(nino, taxYear, incomeSourceId))

        result mustBe Left(InternalServerErrorUpstream)
      }

      "DES returns SERVICE_UNAVAILABLE" in {
        stubGetWithoutResponseBody(url, SERVICE_UNAVAILABLE)
        val result = await(connector.getIncomeSourceDetails(nino, taxYear, incomeSourceId))

        result mustBe Left(ServiceUnavailable)
      }

      "DES returns an UNEXPECTED_STATUS" in {
        stubGetWithoutResponseBody(url, NO_CONTENT)

        val result = await(connector.getIncomeSourceDetails(nino, taxYear, incomeSourceId))
        result mustBe Left(UnexpectedStatus)
      }
    }
  }

}
