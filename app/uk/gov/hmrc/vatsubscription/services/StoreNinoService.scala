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

import javax.inject.{Inject, Singleton}

import play.api.mvc.Request
import uk.gov.hmrc.auth.core.Enrolments
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.vatsubscription.connectors.AuthenticatorConnector
import uk.gov.hmrc.vatsubscription.models.UserDetailsModel
import uk.gov.hmrc.vatsubscription.config.Constants._
import uk.gov.hmrc.vatsubscription.models.monitoring.UserMatchingAuditing.UserMatchingAuditModel
import uk.gov.hmrc.vatsubscription.repositories.SubscriptionRequestRepository
import uk.gov.hmrc.vatsubscription.services.monitoring.AuditService

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class StoreNinoService @Inject()(subscriptionRequestRepository: SubscriptionRequestRepository,
                                 authenticatorConnector: AuthenticatorConnector,
                                 auditService: AuditService
                                )(implicit ec: ExecutionContext) {


  def storeNino(vatNumber: String, userDetailsModel: UserDetailsModel, enrolments: Enrolments)
               (implicit hc: HeaderCarrier, request: Request[_]): Future[Either[StoreNinoFailure, StoreNinoSuccess.type]] = {

    val optAgentReferenceNumber: Option[String] =
      enrolments getEnrolment AgentEnrolmentKey flatMap {
        agentEnrolment =>
          agentEnrolment getIdentifier AgentReferenceNumberKey map (_.value)
      }

    matchUser(userDetailsModel, optAgentReferenceNumber) flatMap {
      case Right(nino) => storeNinoToMongo(vatNumber, nino)
      case Left(failure) => Future.successful(Left(failure))
    }
  }

  private def matchUser(userDetailsModel: UserDetailsModel, agentReferenceNumber: Option[String])
                       (implicit hc: HeaderCarrier, request: Request[_]): Future[Either[UserMatchingFailure, String]] =
    authenticatorConnector.matchUser(userDetailsModel).map {
      case Right(Some(nino)) => {
        auditService.audit(UserMatchingAuditModel(userDetailsModel, agentReferenceNumber, true))
        Right(nino)
      }
      case Right(None) => {
        auditService.audit(UserMatchingAuditModel(userDetailsModel, agentReferenceNumber, false))
        Left(NoMatchFoundFailure)
      }
      case _ => Left(AuthenticatorFailure)
    }

  private def storeNinoToMongo(vatNumber: String, nino: String): Future[Either[MongoFailure, StoreNinoSuccess.type]] =
    subscriptionRequestRepository.upsertNino(vatNumber, nino) map {
      _ => Right(StoreNinoSuccess)
    } recover {
      case e: NoSuchElementException => Left(NinoDatabaseFailureNoVATNumber)
      case _ => Left(NinoDatabaseFailure)
    }

}

case object StoreNinoSuccess

sealed trait StoreNinoFailure

sealed trait UserMatchingFailure extends StoreNinoFailure

sealed trait MongoFailure extends StoreNinoFailure

case object AuthenticatorFailure extends UserMatchingFailure

case object NoMatchFoundFailure extends UserMatchingFailure

case object NinoDatabaseFailure extends MongoFailure

case object NinoDatabaseFailureNoVATNumber extends MongoFailure
