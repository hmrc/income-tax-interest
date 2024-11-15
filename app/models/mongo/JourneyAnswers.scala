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

package models.mongo

import play.api.libs.json._
import uk.gov.hmrc.crypto.Sensitive._
import uk.gov.hmrc.crypto.json.JsonEncryption
import uk.gov.hmrc.crypto.{Decrypter, Encrypter}
import uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats

import java.time.Instant

case class JourneyAnswers(mtdItId: String,
                          taxYear: Int,
                          journey: String,
                          data: JsObject,
                          lastUpdated: Instant)

object JourneyAnswers {

  val reads: Reads[JourneyAnswers] = {
    import play.api.libs.functional.syntax._

    (
      (__ \ "mtdItId").read[String] and
        (__ \ "taxYear").read[Int].filter(_.toString.matches("^20\\d{2}$")) and
        (__ \ "journey").read[String] and
        (__ \ "data").read[JsObject] and
        (__ \ "lastUpdated").read(MongoJavatimeFormats.instantFormat)
      )(JourneyAnswers.apply _)
  }

  val writes: OWrites[JourneyAnswers] = {
    import play.api.libs.functional.syntax._

    (
      (__ \ "mtdItId").write[String] and
        (__ \ "taxYear").write[Int] and
        (__ \ "journey").write[String] and
        (__ \ "data").write[JsObject] and
        (__ \ "lastUpdated").write(MongoJavatimeFormats.instantFormat)
      )(unlift(JourneyAnswers.unapply))
  }

  implicit val format: OFormat[JourneyAnswers] = OFormat(reads, writes)

  def encryptedFormat(implicit crypto: Encrypter with Decrypter): OFormat[JourneyAnswers] = {

    import play.api.libs.functional.syntax._

    implicit val sensitiveFormat: Format[SensitiveString] = {
      JsonEncryption.sensitiveEncrypterDecrypter(SensitiveString.apply)
    }

    val encryptedReads: Reads[JourneyAnswers] =
      (
        (__ \ "mtdItId").read[String] and
          (__ \ "taxYear").read[Int] and
          (__ \ "journey").read[String] and
          (__ \ "data").read[SensitiveString] and
          (__ \ "lastUpdated").read(MongoJavatimeFormats.instantFormat)
        )((mtdItId, taxYear, journey, data, lastUpdated) => JourneyAnswers(mtdItId, taxYear, journey, Json.parse(data.decryptedValue).as[JsObject], lastUpdated))

    val encryptedWrites: OWrites[JourneyAnswers] =
      (
        (__ \ "mtdItId").write[String] and
          (__ \ "taxYear").write[Int] and
          (__ \ "journey").write[String] and
          (__ \ "data").write[SensitiveString] and
          (__ \ "lastUpdated").write(MongoJavatimeFormats.instantFormat)
        )(ua => (ua.mtdItId, ua.taxYear, ua.journey, SensitiveString(Json.stringify(ua.data)), ua.lastUpdated))

    OFormat(encryptedReads orElse reads, encryptedWrites)
  }
}