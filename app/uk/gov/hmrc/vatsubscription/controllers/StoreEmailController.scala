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

package uk.gov.hmrc.vatsubscription.controllers

import javax.inject.{Inject, Singleton}

import play.api.libs.json.{JsPath, Json}
import play.api.mvc.Action
import uk.gov.hmrc.auth.core.retrieve.Retrievals
import uk.gov.hmrc.auth.core.{AuthConnector, AuthorisedFunctions}
import uk.gov.hmrc.play.bootstrap.controller.BaseController
import uk.gov.hmrc.vatsubscription.config.Constants.EmailVerification.EmailVerifiedKey
import uk.gov.hmrc.vatsubscription.models.SubscriptionRequest.emailKey
import uk.gov.hmrc.vatsubscription.services.StoreEmailService._
import uk.gov.hmrc.vatsubscription.services._

import scala.concurrent.ExecutionContext

@Singleton
class StoreEmailController @Inject()(val authConnector: AuthConnector,
                                     storeEmailService: StoreEmailService
                                    )(implicit ec: ExecutionContext)
  extends BaseController with AuthorisedFunctions {

  def storeEmail(vatNumber: String): Action[String] =
    Action.async(parse.json((JsPath \ emailKey).read[String])) {
      implicit req =>
        authorised().retrieve(Retrievals.allEnrolments) {
          enrolments =>
            val email = req.body
            storeEmailService.storeEmail(vatNumber, email, enrolments) map {
              case Right(StoreEmailSuccess(emailVerified)) =>
                Ok(Json.obj(
                  EmailVerifiedKey -> emailVerified
                ))
              case Left(EmailDatabaseFailureNoVATNumber) => NotFound
              case Left(EmailDatabaseFailure) => InternalServerError
              case Left(EmailVerificationFailure) => BadGateway
            }
        }
    }

}
