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

package uk.gov.hmrc.vatsubscription.repositories.mocks

import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfterEach, Suite}
import reactivemongo.api.commands.{UpdateWriteResult, WriteResult}
import uk.gov.hmrc.vatsubscription.repositories.EmailRequestRepository

import scala.concurrent.Future

trait MockEmailRequestRepository extends MockitoSugar with BeforeAndAfterEach {
  this: Suite =>
  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockEmailRequestRepository)
  }

  val mockEmailRequestRepository: EmailRequestRepository = mock[EmailRequestRepository]

  def mockUpsertEmailAfterSubscription(vatNumber: String, email: String)(response: Future[UpdateWriteResult]): Unit =
    when(mockEmailRequestRepository.upsertEmail(ArgumentMatchers.eq(vatNumber), ArgumentMatchers.eq(email)))
      .thenReturn(response)


}
