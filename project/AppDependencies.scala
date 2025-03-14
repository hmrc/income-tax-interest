/*
 * Copyright 2022 HM Revenue & Customs
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

import sbt._

object AppDependencies {

  private val bootstrapVersion = "8.6.0"
  private val hmrcMongoPlay30Version = "2.5.0"

  private val jacksonAndPlayExclusions: Seq[InclusionRule] = Seq(
    ExclusionRule(organization = "com.fasterxml.jackson.core"),
    ExclusionRule(organization = "com.fasterxml.jackson.datatype"),
    ExclusionRule(organization = "com.fasterxml.jackson.module"),
    ExclusionRule(organization = "com.fasterxml.jackson.core:jackson-annotations"),
    ExclusionRule(organization = "com.typesafe.play")
  )

  val compile: Seq[ModuleID] = Seq(
    "uk.gov.hmrc" %% "bootstrap-backend-play-30" % bootstrapVersion,
    "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.17.0",
    "uk.gov.hmrc.mongo"             %% "hmrc-mongo-play-30"         % hmrcMongoPlay30Version,
    "com.beachape" %% "enumeratum" % "1.7.3",
    "uk.gov.hmrc"  %% "crypto-json-play-30" % "7.6.0",
    "org.typelevel" %% "cats-core" % "2.12.0",
    "com.beachape" %% "enumeratum-play-json" % "1.7.3" excludeAll (jacksonAndPlayExclusions *)
  )

  val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"             %% "bootstrap-test-play-30"     % bootstrapVersion              % "test",
    "uk.gov.hmrc.mongo"       %% "hmrc-mongo-test-play-30"    % hmrcMongoPlay30Version        % "test",
    "org.scalatest"           %% "scalatest"                  % "3.2.15"                      % "test",
    "org.scalamock"           %% "scalamock"                  % "5.1.0"                       % "test",
    "org.mockito"             %% "mockito-scala"              % "1.17.37"                     % "test"
  )
}
