/*
 * Copyright 2024 HM Revenue & Customs
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

package models.taskList

import models.taskList.SectionTitle.InterestTitle
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.libs.json.{JsPath, JsSuccess, Json}

class SectionTitleSpec extends AnyFreeSpec with Matchers {

  "InterestTitle" - {

    "must parse to and from json" in {
      val underTest = Json.toJson(InterestTitle)

      underTest.toString() mustBe s"\"$InterestTitle\""
      underTest.validate[SectionTitle] mustBe JsSuccess(InterestTitle, JsPath())
    }
  }
}
