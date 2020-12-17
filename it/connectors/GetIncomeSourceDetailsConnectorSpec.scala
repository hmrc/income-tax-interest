package connectors

import connectors.httpParsers.IncomeSourcesDetailsParser._
import helpers.WiremockSpec
import models.InterestDetailsModel
import org.scalatestplus.play.PlaySpec
import play.api.http.Status._
import play.api.libs.json.Json
import uk.gov.hmrc.http.HeaderCarrier

class GetIncomeSourceDetailsConnectorSpec extends PlaySpec with WiremockSpec{

  lazy val connector = app.injector.instanceOf[GetIncomeSourceDetailsConnector]
  val nino = "nino"
  val taxYear = "2020"
  val incomeSourceId = "someId"
  val url = s"/income-tax/income-sources/nino/${nino}\\?incomeSourceType=savings&taxYear=${taxYear}&incomeSourceId=${incomeSourceId}"

  val model: InterestDetailsModel = InterestDetailsModel(incomeSourceId, Some(29.99), Some(37.65))

  ".getIncomeSourceDetails" should {

    "return a success result" when {

      "DES returns a 200" in {
        stubGetWithResponseBody(url, OK, Json.toJson(model).toString())
        implicit val hc = HeaderCarrier()
        val result = await(connector.getIncomeSourceDetails(nino, taxYear, incomeSourceId))

        result mustBe Right(model)

      }
    }
    "return a failure result" when {

      "DES returns wrong Json" in {
        stubGetWithResponseBody(url, OK, Json.obj("nino" -> nino).toString())
        implicit val hc = HeaderCarrier()
        val result = await(connector.getIncomeSourceDetails(nino, taxYear, incomeSourceId))

        result mustBe Left(InterestDetailsInvalidJson)
      }

      "DES returns BAD_REQUEST" in {
        stubGetWithoutResponseBody(url, BAD_REQUEST)
        implicit val hc = HeaderCarrier()
        val result = await(connector.getIncomeSourceDetails(nino, taxYear, incomeSourceId))

        result mustBe Left(InvalidSubmission)
      }

      "DES returns NOT_FOUND" in {
        stubGetWithoutResponseBody(url, NOT_FOUND)
        implicit val hc = HeaderCarrier()
        val result = await(connector.getIncomeSourceDetails(nino, taxYear, incomeSourceId))

        result mustBe Left(NotFoundException)
      }

      "DES returns INTERNAL_SERVER_ERROR" in {
        stubGetWithoutResponseBody(url, INTERNAL_SERVER_ERROR)
        implicit val hc = HeaderCarrier()
        val result = await(connector.getIncomeSourceDetails(nino, taxYear, incomeSourceId))

        result mustBe Left(InternalServerErrorUpstream)
      }

      "DES returns SERVICE_UNAVAILABLE" in {
        stubGetWithoutResponseBody(url, SERVICE_UNAVAILABLE)
        implicit val hc = HeaderCarrier()
        val result = await(connector.getIncomeSourceDetails(nino, taxYear, incomeSourceId))

        result mustBe Left(ServiceUnavailable)
      }

      "DES returns an UNEXPECTED_STATUS" in {

        stubGetWithoutResponseBody(url, NO_CONTENT)

        implicit val hc = HeaderCarrier()
        val result = await(connector.getIncomeSourceDetails(nino, taxYear, incomeSourceId))
        result mustBe Left(UnexpectedStatus)
      }
    }
  }

}
