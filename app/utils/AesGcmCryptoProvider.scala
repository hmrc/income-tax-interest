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

package utils

import config.AppConfig
import uk.gov.hmrc.crypto.{Decrypter, Encrypter, SymmetricCryptoFactory}

import javax.inject.{Inject, Provider, Singleton}

// This Crypto provider has been added as part of implementing the Journey Answers functionality
// It uses Encrypter with Decrypter types compared to the existing CryptoFactory which uses AdEncrypter with AdDecrypter
// This is used to create an implicit binding in config.Module
// The implicit binding is used to provide an encryption/ decryption implementation to the JourneyAnswersRepository
@Singleton
class AesGcmCryptoProvider @Inject()(appConfig: AppConfig) extends Provider[Encrypter with Decrypter] {
  override def get(): Encrypter with Decrypter = SymmetricCryptoFactory.aesGcmCrypto(appConfig.encryptionKey)
}