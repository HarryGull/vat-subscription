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

import uk.gov.hmrc.auth.core.Enrolments
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.vatsubscription.config.Constants._
import uk.gov.hmrc.vatsubscription.connectors.AgentClientRelationshipsConnector
import uk.gov.hmrc.vatsubscription.models.{HaveRelationshipResponse, NoRelationshipResponse}
import uk.gov.hmrc.vatsubscription.repositories.SubscriptionRequestRepository

import scala.concurrent.{ExecutionContext, Future}

class StoreVatNumberService @Inject()(subscriptionRequestRepository: SubscriptionRequestRepository,
                                      agentClientRelationshipsConnector: AgentClientRelationshipsConnector
                                     )(implicit ec: ExecutionContext) {

  def storeVatNumber(vatNumber: String,
                     enrolments: Enrolments
                    )(implicit hc: HeaderCarrier): Future[Either[StoreVatNumberFailure, StoreVatNumberSuccess.type]] = {

    val optAgentReferenceNumber: Option[String] =
      enrolments getEnrolment AgentEnrolmentKey flatMap {
        agentEnrolment =>
          agentEnrolment getIdentifier AgentReferenceNumberKey map (_.value)
      }

    val optVatEnrolment: Option[String] =
      enrolments getEnrolment VATEnrolmentKey flatMap {
        agentEnrolment =>
          agentEnrolment getIdentifier VATReferenceKey map (_.value)
      }

    (optVatEnrolment, optAgentReferenceNumber) match {
      case (Some(vatNumberFromEnrolment), _) if vatNumber == vatNumberFromEnrolment =>
        insertVatNumber(vatNumber)
      case (Some(vatNumberFromEnrolment), _) =>
        Future.successful(Left(DoesNotMatchEnrolment))
      case (_, Some(agentReferenceNumber)) =>
        storeDelegatedVatNumber(vatNumber, agentReferenceNumber)
      case _ => Future.successful(Left(InsufficientEnrolments))
    }
  }

  private def storeDelegatedVatNumber(vatNumber: String, agentReferenceNumber: String)(implicit hc: HeaderCarrier) =
    agentClientRelationshipsConnector.checkAgentClientRelationship(agentReferenceNumber, vatNumber) flatMap {
      case Right(HaveRelationshipResponse) =>
        insertVatNumber(vatNumber)
      case Right(NoRelationshipResponse) =>
        Future.successful(Left(RelationshipNotFound))
      case _ =>
        Future.successful(Left(AgentServicesConnectionFailure))
    } recover {
      case _ => Left(AgentServicesConnectionFailure)
    }

  private def insertVatNumber(vatNumber: String) =
    subscriptionRequestRepository.upsertVatNumber(vatNumber) map {
      _ => Right(StoreVatNumberSuccess)
    } recover {
      case _ => Left(VatNumberDatabaseFailure)
    }
}

object StoreVatNumberSuccess

sealed trait StoreVatNumberFailure

object DoesNotMatchEnrolment extends StoreVatNumberFailure

object InsufficientEnrolments extends StoreVatNumberFailure

object RelationshipNotFound extends StoreVatNumberFailure

object AgentServicesConnectionFailure extends StoreVatNumberFailure

object VatNumberDatabaseFailure extends StoreVatNumberFailure
