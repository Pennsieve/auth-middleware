// Copyright (c) 2021 University of Pennsylvania. All Rights Reserved.

package com.pennsieve.auth.middleware

import com.pennsieve.models.Role
import com.pennsieve.auth.middleware.Jwt.{
  Claim,
  Config,
  DatasetRole,
  OrganizationRole,
  Token
}
import com.pennsieve.auth.middleware.Jwt.Role.RoleIdentifier
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.{ Authorization, OAuth2BearerToken }
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.ScalatestRouteTest
import io.circe.generic.auto._
import org.scalatest.wordspec.AnyWordSpec
import shapeless.syntax.inject._

import scala.concurrent.duration._
import java.time.Instant
import org.scalatest.matchers.should.Matchers

class AkkaDirectiveSpec
    extends AnyWordSpec
    with Matchers
    with ScalatestRouteTest {

  implicit val config = TestConfig

  val route = Route.seal {
    path("test") {
      get {
        AkkaDirective.authenticateJwt("test")(config) { _ =>
          complete("success!")
        }
      }
    }
  }

  "authenticateJwt" should {

    "not authenticate a missing token" in {
      Get(uri = "/test") ~> route ~> check {
        status should be(StatusCodes.Unauthorized)
      }
    }

    "not authenticate an expired token" in {
      val content: ClaimType = ServiceClaim(
        List(
          OrganizationRole(
            OrganizationId(1).inject[RoleIdentifier[OrganizationId]],
            Role.Owner
          )
        )
      )

      val claim = Jwt.Claim(content, Instant.MIN)
      val token = Jwt.generateToken(claim)

      val request = Get(uri = "/test")
        .addHeader(Authorization(OAuth2BearerToken(token.value)))

      request ~> route ~> check {
        status should be(StatusCodes.Unauthorized)
      }
    }

    "authenticate a valid service token" in {
      val content: ClaimType = ServiceClaim(
        List(
          OrganizationRole(
            OrganizationId(1).inject[RoleIdentifier[OrganizationId]],
            Role.Owner
          )
        )
      )

      val claim = Jwt.generateClaim(content, 10.seconds)
      val token = Jwt.generateToken(claim)

      val request = Get(uri = "/test")
        .addHeader(Authorization(OAuth2BearerToken(token.value)))

      request ~> route ~> check {
        status should be(StatusCodes.OK)
      }
    }

    "authenticate a valid user token" in {
      val content: ClaimType = UserClaim(
        UserId(1),
        List(
          OrganizationRole(
            OrganizationId(1).inject[RoleIdentifier[OrganizationId]],
            Role.Owner
          )
        )
      )

      val claim = Jwt.generateClaim(content, 10.seconds)
      val token = Jwt.generateToken(claim)

      val request = Get(uri = "/test")
        .addHeader(Authorization(OAuth2BearerToken(token.value)))

      request ~> route ~> check {
        status should be(StatusCodes.OK)
      }
    }
  }
}
