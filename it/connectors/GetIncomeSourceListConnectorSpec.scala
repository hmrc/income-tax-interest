package connectors

import connectors.httpParsers.IncomeSourceListParser._
import helpers.WiremockSpec
import models.IncomeSourceModel
import org.scalatestplus.play.PlaySpec
import play.api.http.Status._
import play.api.libs.json.Json
import uk.gov.hmrc.http.HeaderCarrier

class GetIncomeSourceListConnectorSpec extends PlaySpec with WiremockSpec{

  lazy val connector = app.injector.instanceOf[GetIncomeSourceListConnector]
  val nino = "nino"
  val taxYear = "2020"
  val url = s"/income-tax/income-sources/nino/${nino}\\?incomeSourceType=interest-from-uk-banks&taxYear=$taxYear"

  val model: List[IncomeSourceModel] = List(IncomeSourceModel(nino, taxYear, "interest-from-uk-banks", "incomeSource1"))

  "GetIncomeSourceListConnector" should {

    "return a success result" when {

      "DES returns a 200" in {
        stubGetWithResponseBody(url, OK, Json.toJson(model).toString())

        implicit val hc = HeaderCarrier()
        val result = await(connector.getIncomeSourceList(nino, taxYear))
        result mustBe Right(model)
      }
    }

    "return a failure" when {

      "DES returns incorrect json" in {

        stubGetWithResponseBody(url, OK, Json.obj("nino"-> "nino").toString())
        implicit val hc = HeaderCarrier()
        val result = await(connector.getIncomeSourceList(nino, taxYear))
        result mustBe Left(IncomeSourcesInvalidJson)
      }

      "DES returns a BAD_REQUEST" in {

        stubGetWithoutResponseBody(url, BAD_REQUEST)

        implicit val hc = HeaderCarrier()
        val result = await(connector.getIncomeSourceList(nino, taxYear))
        result mustBe Left(InvalidSubmission)
      }

      "DES returns a NOT_FOUND" in {

        stubGetWithoutResponseBody(url, NOT_FOUND)

        implicit val hc = HeaderCarrier()
        val result = await(connector.getIncomeSourceList(nino, taxYear))
        result mustBe Left(NotFoundException)
      }

      "DES returns a INTERNAL_SERVER_ERROR" in {

        stubGetWithoutResponseBody(url, INTERNAL_SERVER_ERROR)

        implicit val hc = HeaderCarrier()
        val result = await(connector.getIncomeSourceList(nino, taxYear))
        result mustBe Left(InternalServerErrorUpstream)
      }

      "DES returns a SERVICE_UNAVAILABLE" in {

        stubGetWithoutResponseBody(url, SERVICE_UNAVAILABLE)

        implicit val hc = HeaderCarrier()
        val result = await(connector.getIncomeSourceList(nino, taxYear))
        result mustBe Left(UpstreamServiceUnavailable)
      }

      "DES returns an UNEXPECTED_STATUS" in {

        stubGetWithoutResponseBody(url, NO_CONTENT)

        implicit val hc = HeaderCarrier()
        val result = await(connector.getIncomeSourceList(nino, taxYear))
        result mustBe Left(UnexpectedStatus)
      }
    }
  }

}
