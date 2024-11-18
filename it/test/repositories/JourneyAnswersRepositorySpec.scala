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

package repositories

import com.fasterxml.jackson.core.JsonParseException
import config.AppConfig
import models.Done
import models.mongo.JourneyAnswers
import org.mockito.MockitoSugar.mock
import org.mongodb.scala.bson.BsonDocument
import org.mongodb.scala.bson.conversions.Bson
import org.mongodb.scala.model.Filters
import org.scalatest.OptionValues
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.{Application, Configuration}
import support.IntegrationTest
import uk.gov.hmrc.crypto.{Decrypter, Encrypter, SymmetricCryptoFactory}
import uk.gov.hmrc.mongo.test.DefaultPlayMongoRepositorySupport

import java.security.SecureRandom
import java.time.temporal.ChronoUnit
import java.time.{Clock, Instant, ZoneId}
import java.util.Base64

class JourneyAnswersRepositorySpec
  extends IntegrationTest
    with OptionValues
    with DefaultPlayMongoRepositorySupport[JourneyAnswers] {

  private val instant = Instant.now.truncatedTo(ChronoUnit.MILLIS)
  private val stubClock: Clock = Clock.fixed(instant, ZoneId.systemDefault)

  private val journey: String = "journey"
  private val validTaxYear: Int = 2023
  private val invalidTaxYear = 1999

  private val userData = JourneyAnswers("mtdItId", validTaxYear, journey, Json.obj("foo" -> "bar"), Instant.ofEpochSecond(1))

  private val aesKey = {
    val aesKey = new Array[Byte](32)
    new SecureRandom().nextBytes(aesKey)
    Base64.getEncoder.encodeToString(aesKey)
  }

  private val configuration = Configuration("crypto.key" -> aesKey)

  private implicit val crypto: Encrypter with Decrypter =
    SymmetricCryptoFactory.aesGcmCryptoFromConfig("crypto", configuration.underlying)

  override implicit lazy val appConfig: AppConfig = mock[AppConfig]

  protected override val repository = new JourneyAnswersRepositoryImpl(
    mongoComponent = mongoComponent,
    appConfig = appConfig,
    clock = stubClock
  )

  override lazy val app: Application = new GuiceApplicationBuilder().overrides(
    bind[AppConfig].toInstance(mock[AppConfig]),
    bind[JourneyAnswersRepository].toInstance(repository)
  ).build()

  def filterByMtdItIdYear(mtdItId: String, taxYear: Int, journey: String): Bson =
    Filters.and(
      Filters.equal("mtdItId", mtdItId),
      Filters.equal("taxYear", taxYear),
      Filters.equal("journey", journey)
    )

  ".set" should {

    "set the last updated time on the supplied user answers to `now`, and save them" in {

      val expectedResult: JourneyAnswers = userData copy (lastUpdated = instant)

      val setResult: Done = repository.set(userData).futureValue
      val updatedRecord: JourneyAnswers = find(filterByMtdItIdYear(userData.mtdItId, userData.taxYear, userData.journey)).futureValue.headOption.value

      setResult shouldBe Done
      updatedRecord shouldBe expectedResult
    }

    "store the data section as encrypted bytes" in {

      repository.set(userData).futureValue

      val record: BsonDocument = repository.collection
        .find[BsonDocument](filterByMtdItIdYear(userData.mtdItId, userData.taxYear, userData.journey))
        .headOption()
        .futureValue
        .get

      val json = Json.parse(record.toJson)
      val data = (json \ "data").as[String]

      assertThrows[JsonParseException] {
        Json.parse(data)
      }
    }
  }

  ".get" should {

    "update the lastUpdated time and get the record" when {

      "there is a record for this mtdItId" in {

        insert(userData).futureValue

        val result = repository.get(userData.mtdItId, userData.taxYear, userData.journey).futureValue
        val expectedResult = userData copy (lastUpdated = instant)

        result.value shouldBe expectedResult
      }
    }

    "return None" when {

      "there is no record for this mtdItId" in {

        repository.get("mtdItId that does not exist", userData.taxYear, userData.journey).futureValue should not be defined
      }

      "there is no record for this taxYear" in {

        repository.get(userData.mtdItId, invalidTaxYear, userData.journey).futureValue should not be defined
      }
    }
  }

  ".clear" should {

    "remove a record" in {

      insert(userData).futureValue

      val result = repository.clear(userData.mtdItId, userData.taxYear, userData.journey).futureValue

      result shouldBe Done
      repository.get(userData.mtdItId, userData.taxYear, userData.journey).futureValue should not be defined
    }

    "should return Done when there is no record for the mtdItId to remove" in {
      val result = repository.clear("mtdItId that does not exist", userData.taxYear, userData.journey).futureValue

      result shouldBe Done
    }

    "should return Done when there is no record for the TaxYear to remove" in {
      val result = repository.clear(userData.mtdItId, invalidTaxYear, userData.journey).futureValue

      result shouldBe Done
    }
  }


  ".keepAlive" should {

    "update its lastUpdated to `now` and return Done" when {

      "there is a record for this mtdItId" in {

        insert(userData).futureValue

        val result = repository.keepAlive(userData.mtdItId, userData.taxYear, userData.journey).futureValue

        val expectedUpdatedAnswers = userData copy (lastUpdated = instant)

        result shouldBe Done
        val updatedAnswers = find(filterByMtdItIdYear(userData.mtdItId, userData.taxYear, userData.journey)).futureValue.headOption.value
        updatedAnswers shouldBe expectedUpdatedAnswers
      }
    }

    "when there is no record for this mtdItId" should {

      "return Done" in {

        repository.keepAlive("id that does not exist", 2023, userData.journey).futureValue shouldBe Done
      }
    }
    "when there is no record for this taxYear" should {

      "return Done" in {

        repository.keepAlive(userData.mtdItId, invalidTaxYear, userData.journey).futureValue shouldBe Done
      }
    }
  }

}