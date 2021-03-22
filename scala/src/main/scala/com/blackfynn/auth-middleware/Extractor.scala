// Copyright (c) 2021 University of Pennsylvania. All Rights Reserved.

package com.pennsieve.auth.middleware

import com.pennsieve.auth.middleware.Jwt.Claim
import com.pennsieve.models.{ Feature, Role => PennsieveRole }
import shapeless.Inl

object Extractor {

  type RoleMatcher = PartialFunction[Jwt.Role, Boolean]

  /**
    * Gets the first dataset ID found in the claim.
    *
    * @param claim
    * @return
    */
  def getHeadDatasetIdFromClaim(claim: Claim): Option[DatasetId] = {
    claim.content.roles.find {
      case Jwt.DatasetRole(Inl(DatasetId(_)), _, _, _) => true
      case _ => false
    } match {
      case Some(Jwt.DatasetRole(Inl(datasetId), _, _, _)) => Some(datasetId)
      case _ => None
    }
  }

  /**
    * Gets all the dataset IDs associated with this claim.
    *
    * @param claim
    * @return
    */
  def getDatasetIdsFromClaim(claim: Claim): List[DatasetId] =
    claim.content.roles.collect {
      case Jwt.DatasetRole(Inl(datasetId), _, _, _) => datasetId
    }

  /**
    * Gets the node ID for the given dataset.
    *
    * @param datasetId
    * @param claim
    * @return
    */
  def getDatasetNodeIdFromClaim(
    datasetId: Int,
    claim: Claim
  ): Option[DatasetNodeId] = {
    claim.content.roles.find {
      case Jwt.DatasetRole(Inl(DatasetId(id)), _, _, _) => (id == datasetId)
      case _ => false
    } match {
      case Some(Jwt.DatasetRole(_, _, datasetNodeId, _)) => datasetNodeId
      case _ => None
    }
  }

  /**
    * Gets the first organization ID found in the claim.
    *
    * @param claim
    * @return
    */
  def getHeadOrganizationIdFromClaim(claim: Claim): Option[OrganizationId] = {
    claim.content.roles.find {
      case Jwt.OrganizationRole(Inl(OrganizationId(_)), _, _, _, _) => true
      case _ => false
    } match {
      case Some(Jwt.OrganizationRole(Inl(organizationId), _, _, _, _)) =>
        Some(organizationId)
      case _ => None
    }
  }

  /**
    * Gets all the organization IDs associated with this claim.
    *
    * @param claim
    * @return
    */
  def getOrganizationIdsFromClaim(claim: Claim): List[OrganizationId] =
    claim.content.roles.collect {
      case Jwt.OrganizationRole(Inl(organizationId), _, _, _, _) =>
        organizationId
    }

  /**
    * Gets the node ID for the given organization.
    *
    * @param organizationId
    * @param claim
    * @return
    */
  def getOrganizationNodeIdFromClaim(
    organizationId: Int,
    claim: Claim
  ): Option[OrganizationNodeId] = {
    claim.content.roles.find {
      case Jwt.OrganizationRole(Inl(OrganizationId(id)), _, _, Some(_), _) =>
        (id == organizationId)
      case _ => false
    } match {
      case Some(Jwt.OrganizationRole(_, _, _, organizationNodeId, _)) =>
        organizationNodeId
      case _ => None
    }
  }

  /**
    * Gets the role from the claim given an identifier.
    *
    * Note: this will match IDs before matching against wildcards.
    *
    * @param identifier
    * @param claim
    * @return
    */
  def getRole(identifier: Identifier, claim: Claim): Option[PennsieveRole] = {

    def matchRole(
      matchId: RoleMatcher,
      matchWildcard: RoleMatcher,
      claim: Claim
    ): Option[Jwt.Role] =
      // applyOrElse(f) = if (f.isDefinedAt(x)) { f(x) } else { (x) => false }
      claim.content.roles
        .find(matchId.applyOrElse(_, (_: Any) => false)) match {
        case role @ Some(_) => role
        case _ =>
          claim.content.roles
            .find(matchWildcard.applyOrElse(_, (_: Any) => false))
      }

    {
      identifier match {
        case OrganizationId(organizationId) => {
          matchRole({
            case Jwt.OrganizationRole(Inl(OrganizationId(id)), _, _, _, _) =>
              id == organizationId
          }, {
            case r @ Jwt.OrganizationRole(_, _, _, _, _) if r.isWildcard => true
          }, claim)
        }
        case DatasetId(datasetId) => {
          matchRole({
            case Jwt.DatasetRole(Inl(DatasetId(id)), _, _, _) => id == datasetId
          }, {
            case r @ Jwt.DatasetRole(_, _, _, _) if r.isWildcard => true
          }, claim)
        }
        case WorkspaceId(workspaceId) => {
          matchRole({
            case Jwt.WorkspaceRole(Inl(WorkspaceId(id)), _) => id == workspaceId
          }, {
            case r @ Jwt.WorkspaceRole(_, _) if r.isWildcard => true
          }, claim)
        }
      }
    }.map(_.role)
  }

  /**
    * Gets the encryption ID associated with the given organization.
    *
    * @param organizationId
    * @param claim
    * @return
    */
  def getEncryptionKeyIdFromClaim(
    organizationId: Int,
    claim: Claim
  ): Option[EncryptionKeyId] = {
    claim.content.roles.find {
      case Jwt.OrganizationRole(Inl(OrganizationId(id)), _, Some(_), _, _) =>
        (id == organizationId)
      case _ => false
    } match {
      case Some(Jwt.OrganizationRole(_, _, encryptionKeyId, _, _)) =>
        encryptionKeyId
      case _ => None
    }
  }

  def getEnabledFeaturesFromClaim(
    organizationId: OrganizationId,
    claim: Claim
  ): Option[List[Feature]] = {
    claim.content.roles.find {
      case Jwt.OrganizationRole(Inl(id), _, _, _, Some(_)) =>
        (id == organizationId)
      case _ => false
    } match {
      case Some(Jwt.OrganizationRole(_, _, _, _, enabledFeatures)) =>
        enabledFeatures
      case _ => None
    }
  }
}
