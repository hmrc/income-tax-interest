package support

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

import config.AppConfig
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.{DefaultAwaitTimeout, FutureAwaits}
import play.api.{Application, Environment, Mode}

import scala.concurrent.ExecutionContext

trait IntegrationTest extends AnyWordSpec
  with FutureAwaits with DefaultAwaitTimeout
  with Matchers
  with GuiceOneServerPerSuite
  with BeforeAndAfterAll {

  protected implicit val ec: ExecutionContext = ExecutionContext.Implicits.global
  protected implicit lazy val appConfig: AppConfig = app.injector.instanceOf[AppConfig]

  protected val config: Map[String, String] = Map(
    "useEncryption" -> "true"
  )

  override implicit lazy val app: Application = new GuiceApplicationBuilder()
    .in(Environment.simple(mode = Mode.Dev))
    .configure(config)
    .build()

  protected override def beforeAll(): Unit = {
    super.beforeAll()
  }

  protected override def afterAll(): Unit = {
    super.afterAll()
  }
}