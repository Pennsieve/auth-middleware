// Copyright (c) 2021 University of Pennsylvania. All Rights Reserved.

package com.pennsieve.auth.middleware

import com.pennsieve.auth.middleware.Extractor._
import com.pennsieve.auth.middleware.Jwt.Role.RoleIdentifier
import com.pennsieve.auth.middleware.Jwt.{
  Claim,
  DatasetRole,
  OrganizationRole,
  WorkspaceRole
}
import com.pennsieve.models.Feature.ConceptsFeature
import com.pennsieve.models.{ Feature, Role }
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import shapeless.syntax.inject._

import scala.concurrent.duration.DurationInt

class ExtractorSpec extends AnyWordSpec with Matchers {

  "extractor" should {

    "extract dataset id from claim" in {
      val test_id = "id"
      val content = UserClaim(
        id = UserId(12345),
        roles = List(
          DatasetRole(
            DatasetId(2).inject[RoleIdentifier[DatasetId]],
            Role.Owner,
            Some(DatasetNodeId(test_id))
          )
        )
      )

      val claim = Jwt.generateClaim(content, 10.seconds)
      val datasetId: Option[DatasetId] = getHeadDatasetIdFromClaim(claim)
      datasetId shouldEqual Some(DatasetId(2))
    }

    "extract dataset ids from claim" in {
      val content = UserClaim(
        id = UserId(12345),
        roles = List(
          DatasetRole(
            DatasetId(2).inject[RoleIdentifier[DatasetId]],
            Role.Owner,
            Some(DatasetNodeId("N:dataset:2"))
          ),
          DatasetRole(
            DatasetId(3).inject[RoleIdentifier[DatasetId]],
            Role.Owner,
            Some(DatasetNodeId("N:dataset:3"))
          )
        )
      )

      val claim = Jwt.generateClaim(content, 10.seconds)
      val datasetIds: Seq[DatasetId] = getDatasetIdsFromClaim(claim)
      datasetIds should have length (2)
      datasetIds should contain(DatasetId(2))
      datasetIds should contain(DatasetId(3))
    }

    "extract dataset node id from claim" in {
      val test_id = "id"
      val content = UserClaim(
        id = UserId(12345),
        roles = List(
          DatasetRole(
            DatasetId(2).inject[RoleIdentifier[DatasetId]],
            Role.Owner,
            Some(DatasetNodeId(test_id))
          )
        )
      )

      val claim = Jwt.generateClaim(content, 10.seconds)
      val datasetNodeId = getDatasetNodeIdFromClaim(2, claim)
      datasetNodeId.get.value shouldEqual test_id
    }

    "extract encryption_key_id from claim" in {
      val test_id = "id"
      val content = UserClaim(
        id = UserId(12345),
        roles = List(
          OrganizationRole(
            OrganizationId(1).inject[RoleIdentifier[OrganizationId]],
            Role.Owner,
            Some(EncryptionKeyId(test_id))
          )
        )
      )

      val claim = Jwt.generateClaim(content, 10.seconds)
      val encryptionKeyId = getEncryptionKeyIdFromClaim(1, claim)
      encryptionKeyId.get.value shouldEqual test_id
    }

    "extract organization id from claim" in {
      val test_id = "id"
      val content = UserClaim(
        id = UserId(12345),
        roles = List(
          OrganizationRole(
            OrganizationId(1).inject[RoleIdentifier[OrganizationId]],
            Role.Owner,
            node_id = Some(OrganizationNodeId(test_id))
          )
        )
      )

      val claim = Jwt.generateClaim(content, 10.seconds)
      val organizationId: Option[OrganizationId] =
        getHeadOrganizationIdFromClaim(claim)
      organizationId shouldEqual Some(OrganizationId(1))
    }

    "extract organization ids from claim" in {
      val content = UserClaim(
        id = UserId(12345),
        roles = List(
          OrganizationRole(
            OrganizationId(1).inject[RoleIdentifier[OrganizationId]],
            Role.Owner,
            node_id = Some(OrganizationNodeId("N:org:1"))
          ),
          OrganizationRole(
            OrganizationId(2).inject[RoleIdentifier[OrganizationId]],
            Role.Owner,
            node_id = Some(OrganizationNodeId("N:org:2"))
          )
        )
      )

      val claim = Jwt.generateClaim(content, 10.seconds)
      val organizationIds: Seq[OrganizationId] =
        getOrganizationIdsFromClaim(claim)
      organizationIds should have length (2)
      organizationIds should contain(OrganizationId(1))
      organizationIds should contain(OrganizationId(2))
    }

    "extract organization node id from claim" in {
      val test_id = "id"
      val content = UserClaim(
        id = UserId(12345),
        roles = List(
          OrganizationRole(
            OrganizationId(1).inject[RoleIdentifier[OrganizationId]],
            Role.Owner,
            node_id = Some(OrganizationNodeId(test_id))
          )
        )
      )

      val claim = Jwt.generateClaim(content, 10.seconds)
      val organizationNodeId = getOrganizationNodeIdFromClaim(1, claim)
      organizationNodeId.get.value shouldEqual test_id
    }

    "extract enabled features from claim" in {
      val organizationId: OrganizationId = OrganizationId(1)
      val enabledFeatures: List[Feature] =
        List(ConceptsFeature)

      val content = UserClaim(
        id = UserId(12345),
        roles = List(
          OrganizationRole(
            organizationId.inject[RoleIdentifier[OrganizationId]],
            Role.Owner,
            enabled_features = Some(enabledFeatures)
          )
        )
      )

      val claim = Jwt.generateClaim(content, 10.seconds)
      val features = getEnabledFeaturesFromClaim(organizationId, claim)

      features.get should contain theSameElementsAs (enabledFeatures)
    }

    "extract role from dataset when no dataset roles are present" in {
      val claim: Claim = Jwt.generateClaim(
        UserClaim(
          id = UserId(12345),
          roles = List(
            OrganizationRole(
              OrganizationId(1).inject[RoleIdentifier[OrganizationId]],
              Role.Owner,
              enabled_features = None
            ),
            WorkspaceRole(
              WorkspaceId(1).inject[RoleIdentifier[WorkspaceId]],
              Role.Viewer
            )
          )
        ),
        10.seconds
      )
      getRole(DatasetId(1), claim) should be(None)
    }

    "extract role from dataset with wildcard" in {
      val claim: Claim = Jwt.generateClaim(
        UserClaim(
          id = UserId(12345),
          roles = List(
            OrganizationRole(
              OrganizationId(1).inject[RoleIdentifier[OrganizationId]],
              Role.Owner,
              enabled_features = None
            ),
            DatasetRole(
              Wildcard.inject[RoleIdentifier[DatasetId]],
              Role.Editor
            ),
            WorkspaceRole(
              WorkspaceId(1).inject[RoleIdentifier[WorkspaceId]],
              Role.Viewer
            )
          )
        ),
        10.seconds
      )
      getRole(DatasetId(1), claim) should be(Some(Role.Editor))
    }

    "extract role from dataset with should match id before wildcard" in {
      val claim: Claim = Jwt.generateClaim(
        UserClaim(
          id = UserId(12345),
          roles = List(
            OrganizationRole(
              OrganizationId(1).inject[RoleIdentifier[OrganizationId]],
              Role.Owner,
              enabled_features = None
            ),
            DatasetRole(Wildcard.inject[RoleIdentifier[DatasetId]], Role.Owner),
            DatasetRole(
              DatasetId(2).inject[RoleIdentifier[DatasetId]],
              Role.Editor
            ),
            WorkspaceRole(
              WorkspaceId(1).inject[RoleIdentifier[WorkspaceId]],
              Role.Viewer
            )
          )
        ),
        10.seconds
      )
      getRole(DatasetId(2), claim) should be(Some(Role.Editor))
      getRole(DatasetId(1), claim) should be(Some(Role.Owner))
    }

    "extract role from organization when no organization roles are present" in {
      val claim: Claim = Jwt.generateClaim(
        UserClaim(
          id = UserId(12345),
          roles = List(
            DatasetRole(
              DatasetId(1).inject[RoleIdentifier[DatasetId]],
              Role.Owner
            ),
            WorkspaceRole(
              Wildcard.inject[RoleIdentifier[WorkspaceId]],
              Role.Owner
            )
          )
        ),
        10.seconds
      )
      getRole(OrganizationId(1), claim) should be(None)
    }

    "extract role from organization with wildcard" in {
      val claim: Claim = Jwt.generateClaim(
        UserClaim(
          id = UserId(12345),
          roles = List(
            DatasetRole(
              DatasetId(1).inject[RoleIdentifier[DatasetId]],
              Role.Owner
            ),
            OrganizationRole(
              Wildcard.inject[RoleIdentifier[OrganizationId]],
              Role.Editor,
              enabled_features = None
            ),
            WorkspaceRole(
              WorkspaceId(1).inject[RoleIdentifier[WorkspaceId]],
              Role.Viewer
            )
          )
        ),
        10.seconds
      )
      getRole(OrganizationId(1), claim) should be(Some(Role.Editor))
    }

    "extract role from organization with should match id before wildcard" in {
      val claim: Claim = Jwt.generateClaim(
        UserClaim(
          id = UserId(12345),
          roles = List(
            DatasetRole(
              DatasetId(1).inject[RoleIdentifier[DatasetId]],
              Role.Owner
            ),
            OrganizationRole(
              Wildcard.inject[RoleIdentifier[OrganizationId]],
              Role.Owner
            ),
            OrganizationRole(
              OrganizationId(2).inject[RoleIdentifier[OrganizationId]],
              Role.Editor,
              enabled_features = None
            ),
            WorkspaceRole(
              WorkspaceId(1).inject[RoleIdentifier[WorkspaceId]],
              Role.Viewer
            )
          )
        ),
        10.seconds
      )
      getRole(OrganizationId(2), claim) should be(Some(Role.Editor))
      getRole(OrganizationId(1), claim) should be(Some(Role.Owner))
    }
  }
}
