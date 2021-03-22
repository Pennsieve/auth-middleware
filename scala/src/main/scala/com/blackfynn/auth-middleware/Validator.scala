// Copyright (c) 2021 University of Pennsylvania. All Rights Reserved.

package com.pennsieve.auth.middleware

import com.pennsieve.auth.middleware.Jwt.Claim
import com.pennsieve.models.Feature

object Validator {
  def isUserClaim(claim: Claim): Boolean = {
    claim.content match {
      case _: UserClaim => true
      case _ => false
    }
  }

  def isServiceClaim(claim: Claim): Boolean = {
    claim.content match {
      case _: ServiceClaim => true
      case _ => false
    }
  }

  def hasOrganizationAccess(
    claim: Claim,
    organizationId: OrganizationId
  ): Boolean = {
    claim.content.roles.exists {
      case role: Jwt.OrganizationRole =>
        role.id.toEither match {
          case Right(Wildcard) => true
          case Left(OrganizationId(id)) => id == organizationId.value
          case _ => false
        }
      case _ => false
    }
  }

  def hasDatasetAccess(
    claim: Claim,
    datasetId: DatasetId,
    permission: Permission
  ): Boolean = {
    claim.content.roles.exists {
      case role: Jwt.DatasetRole =>
        Permission.hasPermission(role.role)(permission) && {
          role.id.toEither match {
            case Right(Wildcard) => true
            case Left(DatasetId(id)) => id == datasetId.value
            case _ => false
          }
        }
      case _ => false
    }
  }

  def hasWorkspaceAccess(
    claim: Claim,
    workspaceId: WorkspaceId,
    permission: Permission
  ): Boolean = {
    claim.content.roles.exists {
      case role: Jwt.WorkspaceRole =>
        Permission.hasPermission(role.role)(permission) && {
          role.id.toEither match {
            case Right(Wildcard) => true
            case Left(WorkspaceId(id)) => id == workspaceId.value
            case _ => false
          }
        }
      case _ => false
    }
  }

  def hasFeatureEnabled(
    claim: Claim,
    organizationId: OrganizationId,
    feature: Feature
  ): Boolean = {
    Extractor.getEnabledFeaturesFromClaim(organizationId, claim) match {
      case None => false
      case Some(features) => features.contains(feature)
    }
  }
}
