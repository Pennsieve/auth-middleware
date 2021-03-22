// Copyright (c) 2019 Blackfynn, Inc. All Rights Reserved.

package com.blackfynn.auth.middleware

import io.circe.parser.decode
import io.circe.syntax._
import shapeless.syntax.inject._
import com.blackfynn.models.Role
import com.blackfynn.models.Feature.ConceptsFeature
import com.blackfynn.auth.middleware._
import com.blackfynn.auth.middleware.DatasetPermission._
import com.blackfynn.auth.middleware.Jwt.{
  DatasetRole,
  OrganizationRole,
  WorkspaceRole
}
import com.blackfynn.auth.middleware.Session
import com.blackfynn.auth.middleware.Jwt.Role.RoleIdentifier
import com.blackfynn.auth.middleware.Resources.readClaim
import org.scalatest.{ Matchers, WordSpec }

class ModelsSpec extends WordSpec with Matchers {

  "session types" should {
    "encode browser type" in {
      val json = readClaim("claim_browser_type.json")
      val session = decode[Session](json).right.get
      session.isBrowser should be(true)
      session.isInstanceOf[Session.Browser] should be(true)
    }

    "encode API type" in {
      val json = readClaim("claim_api_type.json")
      val session = decode[Session](json).right.get
      session.isAPI should be(true)
      session.isInstanceOf[Session.API] should be(true)
    }

    "encode temporary type" in {
      val json = readClaim("claim_temporary_type.json")
      val session = decode[Session](json).right.get
      session.isTemporary should be(true)
      session.isInstanceOf[Session.Temporary] should be(true)
    }

    "reject invalid type" in {
      val json = readClaim("claim_invalid_type.json")
      decode[Session](json).isLeft should be(true)
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
