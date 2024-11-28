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

package config

import com.google.inject.ImplementedBy

import javax.inject.Inject
import play.api.Configuration
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import scala.concurrent.duration.Duration


@ImplementedBy(classOf[BackendAppConfig])
trait AppConfig {
  val authBaseUrl: String

  val desBaseUrl: String

  val auditingEnabled: Boolean
  val graphiteHost: String

  val desEnvironment: String
  val authorisationToken: String

  val authorisationTokenKey: String
  val ifAuthorisationToken: String
  val ifBaseUrl: String
  val ifEnvironment: String

  val personalFrontendBaseUrl: String

  val sectionCompletedQuestionEnabled: Boolean

  //User data Mongo config
  val encryptionKey: String

  //Journey answers Mongo config
  val mongoJourneyAnswersTTL: Int
  val replaceJourneyAnswersIndexes: Boolean

  def authorisationTokenFor(apiVersion: String): String
  def desAuthorisationTokenFor(apiVersion: String): String
}


class BackendAppConfig @Inject()(config: Configuration, servicesConfig: ServicesConfig) extends AppConfig {

  val authBaseUrl: String = servicesConfig.baseUrl("auth")

  val desBaseUrl: String = servicesConfig.baseUrl("des")

  val auditingEnabled: Boolean = config.get[Boolean]("auditing.enabled")
  val graphiteHost: String = config.get[String]("microservice.metrics.graphite.host")

  val desEnvironment: String = config.get[String]("microservice.services.des.environment")
  val authorisationToken: String = config.get[String]("microservice.services.des.authorisation-token")

  lazy val authorisationTokenKey: String = "microservice.services.integration-framework.authorisation-token"
  lazy val ifAuthorisationToken: String = config.get[String]("microservice.services.integration-framework.authorisation-token")
  lazy val ifBaseUrl: String = servicesConfig.baseUrl(serviceName = "integration-framework")
  lazy val ifEnvironment: String = servicesConfig.getString(key = "microservice.services.integration-framework.environment")

  val personalFrontendBaseUrl: String = config.get[String]("microservice.services.personal-income-tax-submission-frontend.url")

  lazy val sectionCompletedQuestionEnabled: Boolean = servicesConfig.getBoolean("feature-switch.sectionCompletedQuestionEnabled")

  //User data Mongo config
  lazy val encryptionKey: String = servicesConfig.getString("mongodb.encryption.key")

  //Journey answers Mongo config
  lazy val mongoJourneyAnswersTTL: Int = Duration(servicesConfig.getString("mongodb.journeyAnswersTimeToLive")).toDays.toInt
  lazy val replaceJourneyAnswersIndexes: Boolean = servicesConfig.getBoolean("mongodb.replaceJourneyAnswersIndexes")

  def authorisationTokenFor(api: String): String = config.get[String](s"microservice.services.integration-framework.authorisation-token.$api")
  def desAuthorisationTokenFor(api: String): String = config.get[String](s"microservice.services.des.authorisation-token-des.$api")
}