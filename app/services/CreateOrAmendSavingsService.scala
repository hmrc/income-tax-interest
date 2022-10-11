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

package services

import connectors.httpParsers.CreateOrAmendSavingsHttpParser.CreateOrAmendSavingsResponse
import connectors.CreateOrAmendSavingsConnector
import models.CreateOrAmendSavingsModel
import play.api.Logging
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}


@Singleton
class CreateOrAmendSavingsService @Inject()(createOrAmendSavingsConnector: CreateOrAmendSavingsConnector)(implicit ec: ExecutionContext) extends Logging {


  def createOrAmendSavings(nino: String, taxYear: Int, savingsModel: CreateOrAmendSavingsModel)
                          (implicit hc: HeaderCarrier): Future[CreateOrAmendSavingsResponse] =
    createOrAmendSavingsConnector.createOrAmendSavings(nino, taxYear, savingsModel)
}