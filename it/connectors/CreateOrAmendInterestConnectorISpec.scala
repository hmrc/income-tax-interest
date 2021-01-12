

package connectors

import helpers.WiremockSpec
import models._
import org.scalatestplus.play.PlaySpec
import play.api.http.Status.{INTERNAL_SERVER_ERROR, NOT_FOUND, OK, SERVICE_UNAVAILABLE}
import play.api.libs.json.Json
import uk.gov.hmrc.http.HeaderCarrier

class CreateOrAmendInterestConnectorISpec extends PlaySpec with WiremockSpec{

  lazy val connector: CreateOrAmendInterestConnector = app.injector.instanceOf[CreateOrAmendInterestConnector]
  implicit val hc: HeaderCarrier = HeaderCarrier()
  val nino = "nino"
  val taxYear = 2021
  val url = s"/income-tax/nino/$nino/income-source/savings/annual/$taxYear"

  val model: InterestDetailsModel = InterestDetailsModel("incomeSourceId", Some(100), Some(100))


  " CreateOrAmendInterestConnector" should {
    "return a success result" when {
      "DES Returns a 200" in {
        val expectedResult = true

        stubPostWithoutResponseBody(url, OK, Json.toJson(model).toString())

        implicit val hc: HeaderCarrier = HeaderCarrier()
        val result = await(connector.createOrAmendInterest(nino, taxYear, model)(hc))

        result mustBe Right(expectedResult)
      }
    }
    "return a failed result" when {
      "DES Returns a Not Found" in {
        val expectedResult = NotFoundError

        stubPostWithoutResponseBody(url, NOT_FOUND, Json.toJson(model).toString())

        implicit val hc: HeaderCarrier = HeaderCarrier()
        val result = await(connector.createOrAmendInterest(nino, taxYear, model)(hc))

        result mustBe Left(expectedResult)
      }
      "DES Returns a SERVICE_UNAVAILABLE" in {
        val expectedResult = ServiceUnavailableError

        stubPostWithoutResponseBody(url, SERVICE_UNAVAILABLE, Json.toJson(model).toString())

        implicit val hc: HeaderCarrier = HeaderCarrier()
        val result = await(connector.createOrAmendInterest(nino, taxYear, model)(hc))

        result mustBe Left(expectedResult)
      }
      "DES Returns a INTERNAL_SERVER_ERROR" in {
        val expectedResult = InternalServerError

        stubPostWithoutResponseBody(url, INTERNAL_SERVER_ERROR, Json.toJson(model).toString())

        implicit val hc: HeaderCarrier = HeaderCarrier()
        val result = await(connector.createOrAmendInterest(nino, taxYear, model)(hc))

        result mustBe Left(expectedResult)
      }
    }
  }

}
