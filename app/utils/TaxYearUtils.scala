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

package utils

import play.api.Logging

object TaxYearUtils extends Logging {

  val specificTaxYear: Int = 2024

  def convertStringTaxYear(taxYear: String): Int = {
    //2021-22 -> 2022
    taxYear.take(4).toInt + 1
  }

  def convertSpecificTaxYear(taxYear: String): String =
    s"${(taxYear.toInt - 1).toString takeRight 2}-${taxYear takeRight 2}"

  def convertSpecificTaxYear(taxYear: Int): String =
    s"${(taxYear - 1).toString takeRight 2}-${taxYear.toString takeRight 2}"
}