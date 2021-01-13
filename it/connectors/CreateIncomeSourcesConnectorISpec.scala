

package connectors

import helpers.WiremockSpec
import models._
import org.scalatestplus.play.PlaySpec
import play.api.http.Status.{INTERNAL_SERVER_ERROR, NOT_FOUND, OK, SERVICE_UNAVAILABLE}
import play.api.libs.json.Json
import uk.gov.hmrc.http.HeaderCarrier

class CreateIncomeSourcesConnectorISpec extends PlaySpec with WiremockSpec{

  lazy val connector: CreateIncomeSourceConnector = app.injector.instanceOf[CreateIncomeSourceConnector]
  implicit val hc = HeaderCarrier()
  val nino = "nino"
  val incomeSourceName = "testName"
  val url = s"/income-tax/income-sources/nino/$nino"

  val model: InterestSubmissionModel = InterestSubmissionModel(incomeSourceName = incomeSourceName)


  "CreateIncomeSourcesConnector" should {
    "return a success result" when {
      "DES Returns a 200 with valid json" in {
        val expectedResult = IncomeSourceIdModel("1234567890")

        stubPostWithResponseBody(url, OK, Json.toJson(model).toString(), Json.toJson(expectedResult).toString())

        implicit val hc: HeaderCarrier = HeaderCarrier()
        val result = await(connector.createIncomeSource(nino, model)(hc))

        result mustBe Right(expectedResult)
      }
    }
    "return a failed result" when {
      "DES Returns a 200 with invalid json" in {
        val expectedResult = InternalServerError

        stubPostWithResponseBody(url, OK, Json.toJson(model).toString(), Json.obj("invalidJson" -> "test").toString())

        implicit val hc: HeaderCarrier = HeaderCarrier()
        val result = await(connector.createIncomeSource(nino, model)(hc))

        result mustBe Left(expectedResult)
      }
      "DES Returns a Not Found" in {
        val expectedResult = NotFoundError

        stubPostWithoutResponseBody(url, NOT_FOUND, Json.toJson(model).toString())

        implicit val hc: HeaderCarrier = HeaderCarrier()
        val result = await(connector.createIncomeSource(nino, model)(hc))

        result mustBe Left(expectedResult)
      }
      "DES Returns a SERVICE_UNAVAILABLE" in {
        val expectedResult = ServiceUnavailableError

        stubPostWithoutResponseBody(url, SERVICE_UNAVAILABLE, Json.toJson(model).toString())

        implicit val hc: HeaderCarrier = HeaderCarrier()
        val result = await(connector.createIncomeSource(nino, model)(hc))

        result mustBe Left(expectedResult)
      }
      "DES Returns a INTERNAL_SERVER_ERROR" in {
        val expectedResult = InternalServerError

        stubPostWithoutResponseBody(url, INTERNAL_SERVER_ERROR, Json.toJson(model).toString())

        implicit val hc: HeaderCarrier = HeaderCarrier()
        val result = await(connector.createIncomeSource(nino, model)(hc))

        result mustBe Left(expectedResult)
      }
    }
  }

}
