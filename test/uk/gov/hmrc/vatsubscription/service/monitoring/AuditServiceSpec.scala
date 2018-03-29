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

package uk.gov.hmrc.vatsubscription.service.monitoring

import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mockito.MockitoSugar
import play.api.Configuration
import uk.gov.hmrc.play.audit.http.connector.AuditConnector

import scala.concurrent.ExecutionContext
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.vatsubscription.services.monitoring.{AuditModel, AuditService, CanAudit}

import scala.concurrent.ExecutionContext.Implicits.global

class AuditServiceSpec extends UnitSpec with MockitoSugar with BeforeAndAfterEach {
  val mockAuditConnector = mock[AuditConnector]
  val mockConfiguration = mock[Configuration]
  val testAppName = "app"
  val testUrl = "testUrl"

  val testAuditService = new AuditService(mockConfiguration, mockAuditConnector)

  private implicit val hc: HeaderCarrier = HeaderCarrier()

  val testAuditModel: String => AuditModel =
    dataSource => new AuditModel{
    override val auditType = s"${dataSource}AuditType"
    override val transactionName = s"${dataSource}TransactionName"
    override val detail = Map[String, String]()
    }

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockAuditConnector, mockConfiguration)

    when(mockConfiguration.getString("appName")) thenReturn Some(testAppName)
  }

  "audit" when {
    "given a auditable data type" should {
      "extract the data and pass it into the AuditConnector" in {

        implicit object TestAuditableString extends CanAudit[String] {
          override def apply(dataSource: String): AuditModel = testAuditModel(dataSource)
        }

        val expectedData = testAuditService.toDataEvent(testAppName, testAuditModel("testString"), "testUrl")

        testAuditService.audit("testString", "testUrl")

        verify(mockAuditConnector)
          .sendEvent(
            ArgumentMatchers.refEq(expectedData, "eventId", "generatedAt")
          )(
            ArgumentMatchers.any[HeaderCarrier],
            ArgumentMatchers.any[ExecutionContext]
          )
      }
    }
  }
}