

package helpers

import com.codahale.metrics.SharedMetricRegistries
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.ws.{WSClient, WSRequest}
import play.api.test.{DefaultAwaitTimeout, FutureAwaits}

import scala.concurrent.ExecutionContext

trait WiremockSpec extends BeforeAndAfterEach with BeforeAndAfterAll with GuiceOneServerPerSuite
  with FutureAwaits with DefaultAwaitTimeout with WiremockStubHelpers {
  self: PlaySpec =>

  val wireMockPort = 11111

  lazy val ws: WSClient = app.injector.instanceOf(classOf[WSClient])
  implicit val ec: ExecutionContext = ExecutionContext.global

  val wireMockServer: WireMockServer = new WireMockServer(wireMockConfig().port(wireMockPort))

  lazy val connectedServices: Seq[String] = Seq("des")

  def servicesToUrlConfig: Seq[(String, String)] = connectedServices
    .flatMap(service => Seq(s"microservice.services.$service.host" -> s"localhost", s"microservice.services.$service.port" -> wireMockPort.toString))

  override implicit lazy val app = GuiceApplicationBuilder()
    .configure(
      ("auditing.consumer.baseUri.port" -> wireMockPort) +:
        servicesToUrlConfig: _*
    )
    .build()

  override def beforeAll(): Unit = {
    super.beforeAll()
    wireMockServer.start()
    SharedMetricRegistries.clear()
    WireMock.configureFor("localhost", wireMockPort)
  }

  override def afterAll(): Unit = {
    super.afterAll()
    wireMockServer.stop()
  }

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset()
  }

  def buildClient(urlandUri: String, port: Int = port): WSRequest = ws
    .url(s"http://localhost:$port$urlandUri")
    .withFollowRedirects(false)

}