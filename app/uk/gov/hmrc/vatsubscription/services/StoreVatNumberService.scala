/*
 * Copyright 2018 HM Revenue & Customs
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

package uk.gov.hmrc.vatsubscription.services

import javax.inject.Inject

import uk.gov.hmrc.vatsubscription.repositories.SubscriptionRequestRepository

import scala.concurrent.{ExecutionContext, Future}

class StoreVatNumberService @Inject()(subscriptionRequestRepository: SubscriptionRequestRepository
                                     )(implicit ec: ExecutionContext) {
  def storeVatNumber(internalId: String, vatNumber: String): Future[Either[StoreVatNumberFailure, StoreVatNumberSuccess.type]] =
    subscriptionRequestRepository.upsertVatNumber(internalId, vatNumber) map {
      _ => Right(StoreVatNumberSuccess)
    } recover {
      case _ => Left(DatabaseFailure)
    }
}

object StoreVatNumberSuccess

sealed trait StoreVatNumberFailure

object DatabaseFailure extends StoreVatNumberFailure