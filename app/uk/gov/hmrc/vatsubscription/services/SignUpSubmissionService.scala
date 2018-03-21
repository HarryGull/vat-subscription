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

import javax.inject.{Inject,Singleton}

import cats.data._
import cats.implicits._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.vatsubscription.connectors.{CustomerSignUpConnector, EmailVerificationConnector, RegistrationConnector, TaxEnrolmentsConnector}
import uk.gov.hmrc.vatsubscription.httpparsers.{EmailNotVerified, EmailVerified, RegisterWithMultipleIdsSuccess, SuccessfulTaxEnrolment}
import uk.gov.hmrc.vatsubscription.models.{CustomerSignUpResponseSuccess, SubscriptionRequest}
import uk.gov.hmrc.vatsubscription.repositories.SubscriptionRequestRepository
import SignUpSubmissionService._
import uk.gov.hmrc.auth.core.Enrolments
import uk.gov.hmrc.vatsubscription.config.Constants._

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SignUpSubmissionService @Inject()(subscriptionRequestRepository: SubscriptionRequestRepository,
                                        emailVerificationConnector: EmailVerificationConnector,
                                        customerSignUpConnector: CustomerSignUpConnector,
                                        registrationConnector: RegistrationConnector,
                                        taxEnrolmentsConnector: TaxEnrolmentsConnector
                                       )(implicit ec: ExecutionContext) {

  def submitSignUpRequest(vatNumber: String, enrolments: Enrolments)(implicit hc: HeaderCarrier): Future[SignUpRequestSubmissionResponse] = {
    val isDelegated = enrolments getEnrolment AgentEnrolmentKey nonEmpty

    subscriptionRequestRepository.findById(vatNumber) flatMap {
      case Some(SubscriptionRequest(_, Some(companyNumber), None, Some(emailAddress), identityVerified)) if isDelegated || identityVerified =>
        val result = for {
          emailAddressVerified <- isEmailAddressVerified(emailAddress)
          safeId <- registerCompany(vatNumber, companyNumber)
          _ <- signUp(safeId, vatNumber, emailAddress, emailAddressVerified, isDelegated)
          _ <- registerEnrolment(vatNumber, safeId)
          _ <- deleteRecord(vatNumber)
        } yield SignUpRequestSubmitted

        result.value
      case Some(SubscriptionRequest(_, None, Some(nino), Some(emailAddress), identityVerified)) if isDelegated || identityVerified =>
        val result = for {
          emailAddressVerified <- isEmailAddressVerified(emailAddress)
          safeId <- registerIndividual(vatNumber, nino)
          _ <- signUp(safeId, vatNumber, emailAddress, emailAddressVerified, isDelegated)
          _ <- registerEnrolment(vatNumber, safeId)
          _ <- deleteRecord(vatNumber)
        } yield SignUpRequestSubmitted

        result.value
      case _ =>
        Future.successful(Left(InsufficientData))
    }
  }

  private def isEmailAddressVerified(emailAddress: String
                                    )(implicit hc: HeaderCarrier): EitherT[Future, SignUpRequestSubmissionFailure, Boolean] =
    EitherT(emailVerificationConnector.getEmailVerificationState(emailAddress)) bimap( {
      _ => EmailVerificationFailure
    }, {
      case EmailVerified => true
      case EmailNotVerified => false
    })

  private def registerCompany(vatNumber: String,
                              companyNumber: String
                             )(implicit hc: HeaderCarrier): EitherT[Future, SignUpRequestSubmissionFailure, String] =
    EitherT(registrationConnector.registerCompany(vatNumber, companyNumber)) bimap( {
      _ => RegistrationFailure
    }, {
      case RegisterWithMultipleIdsSuccess(safeId) => safeId
    })

  private def registerIndividual(vatNumber: String,
                                 nino: String
                                )(implicit hc: HeaderCarrier): EitherT[Future, SignUpRequestSubmissionFailure, String] =
    EitherT(registrationConnector.registerIndividual(vatNumber, nino)) bimap( {
      _ => RegistrationFailure
    }, {
      case RegisterWithMultipleIdsSuccess(safeId) => safeId
    })

  private def signUp(safeId: String,
                     vatNumber: String,
                     emailAddress: String,
                     emailAddressVerified: Boolean,
                     isDelegated: Boolean
                    )(implicit hc: HeaderCarrier): EitherT[Future, SignUpRequestSubmissionFailure, CustomerSignUpResponseSuccess.type] =
    if(isDelegated || emailAddressVerified)
      EitherT(customerSignUpConnector.signUp(safeId, vatNumber, emailAddress, emailAddressVerified)) leftMap {
        _ => SignUpFailure
      }
    else EitherT.leftT(UnVerifiedPrincipalEmailFailure)


  private def registerEnrolment(vatNumber: String,
                                safeId: String
                               )(implicit hc: HeaderCarrier): EitherT[Future, SignUpRequestSubmissionFailure, SuccessfulTaxEnrolment.type] = {
    EitherT(taxEnrolmentsConnector.registerEnrolment(vatNumber, safeId)) leftMap {
      _ => EnrolmentFailure
    }
  }

  private def deleteRecord(vatNumber: String): EitherT[Future, SignUpRequestSubmissionFailure, SignUpRequestDeleted.type] = {
    val delete: Future[Either[SignUpRequestSubmissionFailure, SignUpRequestDeleted.type]] =
      subscriptionRequestRepository.deleteRecord(vatNumber).map(_ => Right(SignUpRequestDeleted))
    EitherT(delete) leftMap {
      _ => DeleteRecordFailure
    }
  }
}

object SignUpSubmissionService {

  type SignUpRequestSubmissionResponse = Either[SignUpRequestSubmissionFailure, SignUpRequestSubmitted.type]

  case object SignUpRequestDeleted

  case object SignUpRequestSubmitted

  sealed trait SignUpRequestSubmissionFailure

  case object InsufficientData extends SignUpRequestSubmissionFailure

  case object EmailVerificationFailure extends SignUpRequestSubmissionFailure

  case object UnVerifiedPrincipalEmailFailure extends SignUpRequestSubmissionFailure

  case object SignUpFailure extends SignUpRequestSubmissionFailure

  case object RegistrationFailure extends SignUpRequestSubmissionFailure

  case object EnrolmentFailure extends SignUpRequestSubmissionFailure

  case object DeleteRecordFailure extends SignUpRequestSubmissionFailure

}
