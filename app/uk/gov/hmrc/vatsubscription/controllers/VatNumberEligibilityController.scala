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
import play.api.mvc.{Action, AnyContent}
import uk.gov.hmrc.auth.core.{AuthConnector, AuthorisedFunctions}
import uk.gov.hmrc.play.bootstrap.controller.BaseController
import uk.gov.hmrc.vatsubscription.services.VatNumberEligibilityService._
import uk.gov.hmrc.vatsubscription.services._

import scala.concurrent.ExecutionContext

@Singleton
class VatNumberEligibilityController @Inject()(val authConnector: AuthConnector,
                                               vatNumberEligibilityService: VatNumberEligibilityService)
                                              (implicit ec: ExecutionContext)
  extends BaseController with AuthorisedFunctions {

  def checkVatNumberEligibility(vatNumber: String): Action[AnyContent] = Action.async {
    implicit request =>
      authorised() {
        vatNumberEligibilityService.checkVatNumberEligibility(vatNumber) map {
          case Right(VatNumberEligible) => NoContent
          case Left(AlreadySubscribed) => Conflict
          case Left(VatNumberIneligible) => BadRequest
          case Left(VatNumberNotFound | InvalidVatNumber) => NotFound
          case _ => BadGateway
        }
      }
  }
}
