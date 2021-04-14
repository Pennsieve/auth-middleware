// Copyright (c) 2021 University of Pennsylvania. All Rights Reserved.

package com.pennsieve.auth.middleware

import io.circe.parser.decode
import io.circe.syntax._
import shapeless.syntax.inject._
import com.pennsieve.models.Role
import com.pennsieve.models.Feature.ConceptsFeature
import com.pennsieve.auth.middleware._
import com.pennsieve.auth.middleware.DatasetPermission._
import com.pennsieve.auth.middleware.Jwt.{
  DatasetRole,
  OrganizationRole,
  WorkspaceRole
}
import com.pennsieve.auth.middleware.Jwt.Role.RoleIdentifier
import com.pennsieve.auth.middleware.Resources.readClaim
import org.scalatest.{ Matchers, WordSpec }
import java.time.Instant

class ModelsSpec extends WordSpec with Matchers {

  "cognito accounts" should {

    "encode and decode user login" in {
      val json = readClaim("claim_browser_type.json")
      val session = decode[CognitoSession](json).right.get
      session.isBrowser should be(true)
      session.isInstanceOf[CognitoSession.Browser] should be(true)
      session.exp.toString should be("2021-04-13T18:37:24Z")
      val json2 = session.asJson
      json2 shouldBe json
    }

    "encode API type" in {
      val json = readClaim("claim_api_type.json")
      val session = decode[CognitoSession](json).right.get
      session.isAPI should be(true)
      session.isInstanceOf[CognitoSession.API] should be(true)
    }

    "reject invalid type" in {
      val json = readClaim("claim_invalid_type.json")
      decode[CognitoSession](json).isLeft should be(true)
    }
  }

  "models" should {

    "encode simple user" in {

      val json = readClaim("claim_simple_user.json")
      val claim: ClaimType = UserClaim(
        UserId(1),
        List(
          OrganizationRole(
            Wildcard.inject[RoleIdentifier[OrganizationId]],
            Role.Owner
          )
        )
      )

      decode[ClaimType](json) shouldBe Right(claim)
      Jwt.printer.pretty(claim.asJson) shouldBe json.split("\\s+").mkString
    }

    "encode simple user with node id" in {

      val json = readClaim("claim_simple_user_with_node_id.json")
      val claim: ClaimType = UserClaim(
        id = UserId(1),
        roles = List(
          OrganizationRole(
            Wildcard.inject[RoleIdentifier[OrganizationId]],
            Role.Owner
          )
        ),
        node_id =
          Some(UserNodeId("N:user:d39c79c0-1fbb-4b3e-aab1-77323d965101"))
      )

      decode[ClaimType](json) shouldBe Right(claim)
      Jwt.printer.pretty(claim.asJson) shouldBe json.split("\\s+").mkString
    }

    "encode simple service" in {
      val json = readClaim("claim_simple_service.json")
      val claim: ClaimType = ServiceClaim(
        List(
          OrganizationRole(
            Wildcard.inject[RoleIdentifier[OrganizationId]],
            Role.Owner
          )
        )
      )

      decode[ClaimType](json) shouldBe Right(claim)

      Jwt.printer.pretty(claim.asJson) shouldBe json.split("\\s+").mkString
    }

    "encode complex roles" in {
      val datasetId: DatasetId = DatasetId(123)
      val datasetNodeId: DatasetNodeId =
        DatasetNodeId("N:dataset:38e9544e-3f23-4057-a76a-3e2a4f767e61")
      val workspaceId: WorkspaceId = WorkspaceId(456)

      val json = readClaim("claim_complex_roles.json")

      val claim: ClaimType = UserClaim(
        UserId(1),
        List(
          OrganizationRole(
            Wildcard.inject[RoleIdentifier[OrganizationId]],
            Role.Owner
          ),
          DatasetRole(
            datasetId.inject[RoleIdentifier[DatasetId]],
            Role.Owner,
            Some(datasetNodeId)
          ),
          WorkspaceRole(
            workspaceId.inject[RoleIdentifier[WorkspaceId]],
            Role.Owner
          )
        )
      )

      decode[ClaimType](json) shouldBe Right(claim)

      Jwt.printer.pretty(claim.asJson) shouldBe json.split("\\s+").mkString
    }

    "encode dataset roles with locked statue" in {
      val datasetId: DatasetId = DatasetId(123)
      val datasetNodeId: DatasetNodeId =
        DatasetNodeId("N:dataset:38e9544e-3f23-4057-a76a-3e2a4f767e61")

      val json = readClaim("claim_locked_datasets.json")

      val claim: ClaimType = UserClaim(
        UserId(1),
        List(
          OrganizationRole(
            Wildcard.inject[RoleIdentifier[OrganizationId]],
            Role.Owner
          ),
          DatasetRole(
            datasetId.inject[RoleIdentifier[DatasetId]],
            Role.Owner,
            Some(datasetNodeId),
            Some(true)
          ),
          // TODO: prevent construction of a wildcard role with a locked value
          DatasetRole(
            Wildcard.inject[RoleIdentifier[DatasetId]],
            Role.Owner,
            None,
            Some(true)
          )
        )
      )

      decode[ClaimType](json) shouldBe Right(claim)

      Jwt.printer.pretty(claim.asJson) shouldBe json.split("\\s+").mkString
    }

    "encode organization secret key id for a write organization role" in {
      val organizationId = OrganizationId(1)
      val organizationSecretKeyId = EncryptionKeyId("secret-key-id")
      val organizationNodeId: OrganizationNodeId =
        OrganizationNodeId(
          "N:organization:38e9544e-3f23-4057-a76a-3e2a4f767e61"
        )

      val json = readClaim("claim_secret_key_id.json")
      val claim: ClaimType =
        UserClaim(
          UserId(1),
          List(
            OrganizationRole(
              organizationId.inject[RoleIdentifier[OrganizationId]],
              Role.Editor,
              encryption_key_id = Some(organizationSecretKeyId),
              node_id = Some(organizationNodeId),
              enabled_features = Some(List(ConceptsFeature))
            )
          )
        )

      decode[ClaimType](json) shouldBe Right(claim)

      Jwt.printer.pretty(claim.asJson) shouldBe json.split("\\s+").mkString
    }
  }
}
