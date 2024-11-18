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

import models.{ErrorModel, NamedInterestDetailsModel}
import org.scalamock.handlers.CallHandler4
import org.scalamock.scalatest.MockFactory
import services.GetInterestsService
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

trait MockGetInterestsService extends MockFactory {
  protected val mockGetInterestsService: GetInterestsService = mock[GetInterestsService]

  type ReturnType = Either[ErrorModel, List[NamedInterestDetailsModel]]
  private type MockType = CallHandler4[String, String, HeaderCarrier, ExecutionContext, Future[ReturnType]]

  def mockGetInterestsList(nino: String,
                            taxYear: String,
                            result: ReturnType): MockType = {
    (mockGetInterestsService.getInterestsList(_: String, _: String)(_: HeaderCarrier, _: ExecutionContext))
      .expects(nino, taxYear, *, *)
      .returning(Future.successful(result))
  }

  def mockGetInterestsListException(nino: String,
                                     taxYear: String,
                                     result: Throwable): MockType= {
    (mockGetInterestsService.getInterestsList(_: String, _: String)(_: HeaderCarrier, _: ExecutionContext))
      .expects(nino, taxYear, *, *)
      .returning(Future.failed(result))
  }
}
