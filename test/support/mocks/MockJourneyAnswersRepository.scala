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

package support.mocks

import models.Done
import models.mongo.JourneyAnswers
import org.scalamock.handlers.{CallHandler1, CallHandler3}
import org.scalamock.scalatest.MockFactory
import org.scalatest.TestSuite
import repositories.JourneyAnswersRepository

import scala.concurrent.Future

trait MockJourneyAnswersRepository extends MockFactory { _: TestSuite =>
  protected val mockJourneyAnswersRepo: JourneyAnswersRepository = mock[JourneyAnswersRepository]

  def mockKeepAliveJourneyAnswers(mtdItId: String,
                                  taxYear: Int,
                                  journey: String,
                                  result: Done): CallHandler3[String, Int, String, Future[Done]] = {
    (mockJourneyAnswersRepo.keepAlive _)
      .expects(mtdItId, taxYear, journey)
      .returning(Future.successful(result))
  }

  def mockGetJourneyAnswers(mtdItId: String,
                            taxYear: Int,
                            journey: String,
                            result: Option[JourneyAnswers]): CallHandler3[String, Int, String, Future[Option[JourneyAnswers]]] = {
    (mockJourneyAnswersRepo.get(_: String, _: Int, _: String))
      .expects(mtdItId, taxYear, journey)
      .returning(Future.successful(result))
  }

  def mockGetJourneyAnswersException(mtdItId: String,
                                     taxYear: Int,
                                     journey: String,
                                     result: Throwable): CallHandler3[String, Int, String, Future[Option[JourneyAnswers]]] = {
    (mockJourneyAnswersRepo.get(_: String, _: Int, _: String))
      .expects(mtdItId, taxYear, journey)
      .returning(Future.failed(result))
  }

  def mockSetJourneyAnswers(userData: JourneyAnswers,
                            result: Done): CallHandler1[JourneyAnswers, Future[Done]] =
    (mockJourneyAnswersRepo.set(_: JourneyAnswers))
      .expects(userData)
      .returning(Future.successful(result))

  def mockClearJourneyAnswers(mtdItId: String,
                              taxYear: Int,
                              journey: String,
                              result: Done): CallHandler3[String, Int, String, Future[Done]] =
    (mockJourneyAnswersRepo.clear(_: String, _: Int, _: String))
      .expects(mtdItId, taxYear, journey)
      .returning(Future.successful(result))
}
