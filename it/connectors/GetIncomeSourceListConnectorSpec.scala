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

import connectors.httpParsers.IncomeSourceListParser._
import helpers.WiremockSpec
import models.IncomeSourceModel
import org.scalatestplus.play.PlaySpec
import play.api.http.Status._
import play.api.libs.json.Json
import uk.gov.hmrc.http.HeaderCarrier

class GetIncomeSourceListConnectorSpec extends PlaySpec with WiremockSpec{

  lazy val connector: GetIncomeSourceListConnector = app.injector.instanceOf[GetIncomeSourceListConnector]
  implicit val hc: HeaderCarrier = HeaderCarrier()
  val nino = "nino"
  val taxYear = "2020"
  val url = s"/income-tax/income-sources/nino/$nino\\?incomeSourceType=interest-from-uk-banks&taxYear=$taxYear"

  val model: List[IncomeSourceModel] = List(IncomeSourceModel(taxYear, "interest-from-uk-banks", "incomeSource1"))

  "GetIncomeSourceListConnector" should {

    "return a success result" when {

      "DES returns a 200" in {
        stubGetWithResponseBody(url, OK, Json.toJson(model).toString())

        val result = await(connector.getIncomeSourceList(nino, taxYear))
        result mustBe Right(model)
      }
    }

    "return a failure" when {

      "DES returns incorrect json" in {
        stubGetWithResponseBody(url, OK, Json.obj("nino"-> "nino").toString())
        val result = await(connector.getIncomeSourceList(nino, taxYear))
        result mustBe Left(IncomeSourcesInvalidJson)
      }

      "DES returns a BAD_REQUEST" in {
        stubGetWithoutResponseBody(url, BAD_REQUEST)

        val result = await(connector.getIncomeSourceList(nino, taxYear))
        result mustBe Left(InvalidSubmission)
      }

      "DES returns a NOT_FOUND" in {
        stubGetWithoutResponseBody(url, NOT_FOUND)

        val result = await(connector.getIncomeSourceList(nino, taxYear))
        result mustBe Left(NotFoundException)
      }

      "DES returns a INTERNAL_SERVER_ERROR" in {
        stubGetWithoutResponseBody(url, INTERNAL_SERVER_ERROR)

        val result = await(connector.getIncomeSourceList(nino, taxYear))
        result mustBe Left(InternalServerErrorUpstream)
      }

      "DES returns a SERVICE_UNAVAILABLE" in {
        stubGetWithoutResponseBody(url, SERVICE_UNAVAILABLE)

        val result = await(connector.getIncomeSourceList(nino, taxYear))
        result mustBe Left(UpstreamServiceUnavailable)
      }

      "DES returns an UNEXPECTED_STATUS" in {
        stubGetWithoutResponseBody(url, NO_CONTENT)

        val result = await(connector.getIncomeSourceList(nino, taxYear))
        result mustBe Left(UnexpectedStatus)
      }
    }
  }

}
