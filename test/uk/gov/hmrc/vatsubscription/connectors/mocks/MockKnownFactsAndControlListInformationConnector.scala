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

package uk.gov.hmrc.vatsubscription.connectors.mocks

import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfterEach, Suite}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.vatsubscription.connectors.KnownFactsAndControlListInformationConnector
import uk.gov.hmrc.vatsubscription.httpparsers.KnownFactsAndControlListInformationHttpParser.KnownFactsAndControlListInformationHttpParserResponse

import scala.concurrent.Future

trait MockKnownFactsAndControlListInformationConnector extends MockitoSugar with BeforeAndAfterEach {
  this: Suite =>
  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockKnownFactsAndControlListInformationConnector)
  }

  val mockKnownFactsAndControlListInformationConnector: KnownFactsAndControlListInformationConnector = mock[KnownFactsAndControlListInformationConnector]

  def mockGetKnownFactsAndControlListInformation(vatNumber: String
                                                )(response: Future[KnownFactsAndControlListInformationHttpParserResponse]): Unit = {
    when(mockKnownFactsAndControlListInformationConnector.getKnownFactsAndControlListInformation(
      ArgumentMatchers.eq(vatNumber)
    )(ArgumentMatchers.any[HeaderCarrier])) thenReturn response
  }

}
