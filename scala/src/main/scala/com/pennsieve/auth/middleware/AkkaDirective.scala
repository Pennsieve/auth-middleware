// Copyright (c) 2021 University of Pennsylvania. All Rights Reserved.

package com.pennsieve.auth.middleware

import akka.http.scaladsl.server.directives.{
  AuthenticationDirective,
  Credentials
}
import akka.http.scaladsl.server.Directives

object AkkaDirective {

  private def authenticator(
    credentials: Credentials
  )(implicit
    config: Jwt.Config
  ): Option[Jwt.Claim] =
    credentials match {
      case Credentials.Provided(jwt) =>
        val token: Jwt.Token = Jwt.Token(jwt)

        Jwt
          .parseClaim(token)
          .toOption
          .filter(_.isValid)
      case _ => None
    }

  def authenticateJwt(
    realm: String
  )(implicit
    config: Jwt.Config
  ): AuthenticationDirective[Jwt.Claim] =
    Directives.authenticateOAuth2(realm, authenticator)

}
