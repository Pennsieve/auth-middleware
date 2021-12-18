// Copyright (c) 2021 University of Pennsylvania. All Rights Reserved.

package com.pennsieve.auth.middleware

import com.pennsieve.models.Role
import com.pennsieve.auth.middleware._
import com.pennsieve.auth.middleware.DatasetPermission._
import com.pennsieve.auth.middleware.Jwt.{
  DatasetRole,
  OrganizationRole,
  WorkspaceRole
}
import com.pennsieve.auth.middleware.Jwt.Role.RoleIdentifier
//import com.pennsieve.auth.middleware.WorkspacePermission.{
//  ManageQueries,
//  ViewDashboard
//}
import com.pennsieve.auth.middleware.Resources.readClaim
import com.pennsieve.models.{ CognitoId, Feature }
import com.pennsieve.utilities.circe._
import io.circe.generic.auto._
import io.circe._
import io.circe.parser._
import io.circe.syntax._
import java.time.Instant
import java.util.UUID

import pdi.jwt.{ JwtCirce, JwtClaim }
import shapeless.syntax.inject._
import org.scalatest.{ Matchers, WordSpec }

import scala.concurrent.duration.DurationInt

object TestConfig extends Jwt.Config {
  override val key: String = "testkey"
}

class JwtSpec extends WordSpec with Matchers {

  implicit val config = TestConfig

  "jwt" should {

    "encode and decode simple claim" in {
      val content: ClaimType = ServiceClaim(
        List(
          OrganizationRole(
            OrganizationId(1).inject[RoleIdentifier[OrganizationId]],
            Role.Owner
          ),
          DatasetRole(
            DatasetId(2).inject[RoleIdentifier[DatasetId]],
            Role.Owner
          ),
          WorkspaceRole(
            WorkspaceId(3).inject[RoleIdentifier[WorkspaceId]],
            Role.Owner
          )
        )
      )

      val claim = Jwt.generateClaim(content, 10.seconds)
      val token = Jwt.generateToken(claim)

      Jwt.parseClaim(token).right.get.content shouldEqual content
    }

    "decode a user claim with an explicit Cognito session present" in {
      val jsonClaim = readClaim("claim_with_explicit_session.json")
      val claim = decode[ClaimType](jsonClaim).right.get
      claim.isInstanceOf[UserClaim] should be(true)
      val userClaim = claim.asInstanceOf[UserClaim]
      val cognito = userClaim.cognito.get
      cognito.id should be(
        CognitoId
          .TokenPoolId(UUID.fromString("60e5aea3-1853-4a3f-8bd5-31822868cf41"))
      )
      cognito.isAPI should be(true)
    }

    "decode a user claim with no Cognito session present" in {
      val jsonClaim = readClaim("claim_no_session.json")
      val claim = decode[ClaimType](jsonClaim).right.get
      claim.isInstanceOf[UserClaim] should be(true)
      val userClaim = claim.asInstanceOf[UserClaim]
      userClaim.cognito should be(None)
    }

    /* IMPORTANT: THIS TEST IS CRITICAL AND SHOULD NOT BE ARBITRARILY CHANGED
     * TO ACCOMMODATE LARGER JWT TOKENS
     *
     * If this test is failing we need to reconsider the structure
     * of our JWTs in order to limit its size. Unbounded lists
     * should be removed and/or large objects should be cut down to their
     * essential properties.
     *
     * Or we should re-investigate this methods of authorization.
     */
    "should not exceed default header size limit" in {
      val content: ClaimType = ServiceClaim(
        List(
          OrganizationRole(
            OrganizationId(1).inject[RoleIdentifier[OrganizationId]],
            Role.Owner,
            Some(
              EncryptionKeyId("arn:aws:iam::111122223333:role/KMSAdminRole")
            ),
            Some(
              OrganizationNodeId(
                "N:organization:38e9544e-3f23-4057-a76a-3e2a4f767e61"
              )
            ),
            Some(Feature.values.toList) // use all features here to test upper-limit
          ),
          DatasetRole(
            DatasetId(2).inject[RoleIdentifier[DatasetId]],
            Role.Owner,
            Some(
              DatasetNodeId("N:dataset:38e9544e-3f23-4057-a76a-3e2a4f767e61")
            )
          ),
          WorkspaceRole(
            WorkspaceId(3).inject[RoleIdentifier[WorkspaceId]],
            Role.Owner
          )
        )
      )

      val claim = Jwt.generateClaim(content, 10.seconds)
      val token = Jwt.generateToken(claim)
      // Apache's default limit for headers is 8K.
      //   See: http://httpd.apache.org/docs/2.2/mod/core.html#limitrequestfieldsize
      // Let's ensure our JWT header is less than half that.
      token.value.getBytes().length should be < (8190 / 2)
    }

    "should ignore features that it does not support" in {
      val json =
        readClaim("claim_with_unsupported_features.json")
      val claim = JwtClaim(
        content = Jwt.printer.pretty(parse(json).right.get),
        expiration =
          Some(Instant.now.plusSeconds(10.seconds.toSeconds).getEpochSecond),
        issuedAt = Some(Instant.now.getEpochSecond)
      )

      val token =
        Jwt.Token(JwtCirce.encode(claim, config.key, config.algorithm))

      val parsedClaim = Jwt.parseClaim(token).right.get

      val features: List[Feature] = Extractor
        .getEnabledFeaturesFromClaim(OrganizationId(1), parsedClaim)
        .get

      features should contain theSameElementsAs List()
    }

    "extract typed claim from token" in {
      val content = UserClaim(
        id = UserId(12345),
        roles = List(
          OrganizationRole(
            OrganizationId(1).inject[RoleIdentifier[OrganizationId]],
            Role.Owner
          ),
          DatasetRole(
            DatasetId(2).inject[RoleIdentifier[DatasetId]],
            Role.Owner
          ),
          WorkspaceRole(
            WorkspaceId(3).inject[RoleIdentifier[WorkspaceId]],
            Role.Owner
          )
        )
      )

      val claim = Jwt.generateClaim(content: ClaimType, 10.seconds)
      val token = Jwt.generateToken(claim)

      val parsedClaim: ClaimType = Jwt.parseClaim(token).right.get.content

      parsedClaim match {
        case user: UserClaim => user shouldEqual content
        case _ => false
      }
    }
  }

  "jwt permission checkers" should {

    "handle a user claim" in {
      val claim = Jwt.Claim(
        content = UserClaim(UserId(1), List.empty),
        expiration = Instant.now.plusSeconds(10.seconds.toSeconds)
      )

      Validator.isUserClaim(claim) shouldEqual true
      Validator.isServiceClaim(claim) shouldEqual false
    }

    "handle a service claim" in {
      val claim = Jwt.Claim(
        content = ServiceClaim(List.empty),
        expiration = Instant.now.plusSeconds(10.seconds.toSeconds)
      )

      Validator.isUserClaim(claim) shouldEqual false
      Validator.isServiceClaim(claim) shouldEqual true
    }

    "handle a claim with wildcard org access" in {
      val claim = Jwt.Claim(
        content = ServiceClaim(
          List(
            OrganizationRole(
              Wildcard.inject[RoleIdentifier[OrganizationId]],
              Role.Owner
            )
          )
        ),
        expiration = Instant.now.plusSeconds(10.seconds.toSeconds)
      )

      Validator.hasOrganizationAccess(claim, OrganizationId(1)) shouldEqual true
    }

    "handle a claim with org access" in {
      val claim = Jwt.Claim(
        content = ServiceClaim(
          List(
            OrganizationRole(
              OrganizationId(1).inject[RoleIdentifier[OrganizationId]],
              Role.Owner
            ),
            OrganizationRole(
              OrganizationId(2).inject[RoleIdentifier[OrganizationId]],
              Role.Owner
            )
          )
        ),
        expiration = Instant.now.plusSeconds(10.seconds.toSeconds)
      )

      Validator.hasOrganizationAccess(claim, OrganizationId(2)) shouldEqual true
    }

    "handle a claim with invalid org access" in {
      val claim = Jwt.Claim(
        content = ServiceClaim(
          List(
            OrganizationRole(
              OrganizationId(1).inject[RoleIdentifier[OrganizationId]],
              Role.Owner
            ),
            DatasetRole(
              DatasetId(2).inject[RoleIdentifier[DatasetId]],
              Role.Owner
            ),
            WorkspaceRole(
              WorkspaceId(3).inject[RoleIdentifier[WorkspaceId]],
              Role.Owner
            )
          )
        ),
        expiration = Instant.now.plusSeconds(10.seconds.toSeconds)
      )

      Validator.hasOrganizationAccess(claim, OrganizationId(2)) shouldEqual false
    }

    "handle a claim with dataset access and valid permissions" in {
      val claim = Jwt.Claim(
        content = ServiceClaim(
          List(
            OrganizationRole(
              OrganizationId(1).inject[RoleIdentifier[OrganizationId]],
              Role.Owner
            ),
            DatasetRole(
              DatasetId(2).inject[RoleIdentifier[DatasetId]],
              Role.Owner
            )
          )
        ),
        expiration = Instant.now.plusSeconds(10.seconds.toSeconds)
      )

      Validator.hasDatasetAccess(claim, DatasetId(2), DeleteDataset) shouldEqual true
    }

    "handle a claim with dataset access but invalid permissions" in {
      val claim = Jwt.Claim(
        content = ServiceClaim(
          List(
            OrganizationRole(
              OrganizationId(1).inject[RoleIdentifier[OrganizationId]],
              Role.Owner
            ),
            DatasetRole(
              DatasetId(2).inject[RoleIdentifier[DatasetId]],
              Role.Viewer
            ),
            WorkspaceRole(
              WorkspaceId(3).inject[RoleIdentifier[WorkspaceId]],
              Role.Owner
            )
          )
        ),
        expiration = Instant.now.plusSeconds(10.seconds.toSeconds)
      )

      Validator.hasDatasetAccess(claim, DatasetId(2), DeleteDataset) shouldEqual false
    }

    "handle a claim with dataset access and valid permissions but incorrect dataset" in {
      val claim = Jwt.Claim(
        content = ServiceClaim(
          List(
            OrganizationRole(
              OrganizationId(1).inject[RoleIdentifier[OrganizationId]],
              Role.Owner
            ),
            DatasetRole(
              DatasetId(2).inject[RoleIdentifier[DatasetId]],
              Role.Editor
            ),
            WorkspaceRole(
              WorkspaceId(3).inject[RoleIdentifier[WorkspaceId]],
              Role.Owner
            )
          )
        ),
        expiration = Instant.now.plusSeconds(10.seconds.toSeconds)
      )

      Validator.hasDatasetAccess(claim, DatasetId(3), ViewFiles) shouldEqual false
    }

//    "handle a claim with workspace access and valid permissions" in {
//      val claim = Jwt.Claim(
//        content = ServiceClaim(
//          List(
//            OrganizationRole(
//              OrganizationId(1).inject[RoleIdentifier[OrganizationId]],
//              Role.Owner
//            ),
//            WorkspaceRole(
//              WorkspaceId(3).inject[RoleIdentifier[WorkspaceId]],
//              Role.Editor
//            )
//          )
//        ),
//        expiration = Instant.now.plusSeconds(10.seconds.toSeconds)
//      )
//
//      Validator.hasWorkspaceAccess(claim, WorkspaceId(3), ViewDashboard) shouldEqual true
//    }

//    "handle a claim with workspace access but invalid permissions" in {
//      val claim = Jwt.Claim(
//        content = ServiceClaim(
//          List(
//            OrganizationRole(
//              OrganizationId(1).inject[RoleIdentifier[OrganizationId]],
//              Role.Owner
//            ),
//            DatasetRole(
//              DatasetId(2).inject[RoleIdentifier[DatasetId]],
//              Role.Viewer
//            ),
//            WorkspaceRole(
//              WorkspaceId(3).inject[RoleIdentifier[WorkspaceId]],
//              Role.Editor
//            )
//          )
//        ),
//        expiration = Instant.now.plusSeconds(10.seconds.toSeconds)
//      )
//      Validator.hasWorkspaceAccess(claim, WorkspaceId(3), ManageQueries) shouldEqual false
//    }

//    "handle a claim with workspace access and valid permissions but incorrect workspace" in {
//      val claim = Jwt.Claim(
//        content = ServiceClaim(
//          List(
//            OrganizationRole(
//              OrganizationId(1).inject[RoleIdentifier[OrganizationId]],
//              Role.Owner
//            ),
//            DatasetRole(
//              DatasetId(2).inject[RoleIdentifier[DatasetId]],
//              Role.Editor
//            ),
//            WorkspaceRole(
//              WorkspaceId(3).inject[RoleIdentifier[WorkspaceId]],
//              Role.Owner
//            )
//          )
//        ),
//        expiration = Instant.now.plusSeconds(10.seconds.toSeconds)
//      )
//      Validator.hasWorkspaceAccess(claim, WorkspaceId(4), ViewDashboard) shouldEqual false
//    }
  }
}
