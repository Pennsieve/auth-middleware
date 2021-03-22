// Copyright (c) 2021 University of Pennsylvania. All Rights Reserved.

package com.pennsieve.auth.middleware.cli

import com.pennsieve.auth.middleware.Jwt.Role.RoleIdentifier
import com.pennsieve.auth.middleware.Jwt.{
  Claim,
  Config,
  DatasetRole,
  OrganizationRole,
  Role,
  Token
}
import com.pennsieve.auth.middleware.{
  DatasetId,
  DatasetNodeId,
  OrganizationId,
  OrganizationNodeId,
  ServiceClaim,
  UserClaim,
  UserId,
  Jwt => BfJwt
}
import com.pennsieve.models.{ Feature, Role => PennsieveRole }

import scala.concurrent.duration._

object Jwt {

  /**
    * Default token expiration time in minutes.
    */
  val DEFAULT_EXPIRATION_MINS: Long = 60

  /**
    * Generates a (claim, token) pair from the given JWT parameters.
    *
    * @param role
    * @param userId
    * @param organizationIntId
    * @param organizationNodeId
    * @param datasetIntId
    * @param datasetNodeId
    * @param features
    * @param expiresAfterMinutes
    * @param config
    * @return
    */
  def generate(
    role: PennsieveRole = PennsieveRole.Editor,
    userId: Option[UserId],
    organizationIntId: Option[RoleIdentifier[OrganizationId]] = None,
    organizationNodeId: Option[OrganizationNodeId] = None,
    datasetIntId: Option[RoleIdentifier[DatasetId]] = None,
    datasetNodeId: Option[DatasetNodeId] = None,
    features: List[Feature] = List(),
    expiresAfterMinutes: FiniteDuration = DEFAULT_EXPIRATION_MINS.minutes
  )(
    config: Config
  ): Either[Throwable, (Claim, Token)] = {

    val organizations: List[Role] = organizationIntId.fold(List.empty[Role]) {
      organizationIntId =>
        List(
          OrganizationRole(
            id = organizationIntId,
            role = role,
            encryption_key_id = None,
            node_id = organizationNodeId,
            enabled_features = if (features.isEmpty) None else Some(features)
          )
        )
    }

    val datasets: List[Role] =
      datasetIntId.fold(List.empty[Role]) { datasetIntId =>
        List(
          DatasetRole(id = datasetIntId, role = role, node_id = datasetNodeId)
        )
      }

    val roles: List[Role] = organizations ++ datasets

    val claim: Claim = BfJwt.generateClaim(
      duration = expiresAfterMinutes,
      content = userId match {
        case Some(userId) => UserClaim(userId, roles)
        case None => ServiceClaim(roles)
      }
    )

    val token: Token = BfJwt.generateToken(claim)(config)

    Right((claim, token))
  }
}
