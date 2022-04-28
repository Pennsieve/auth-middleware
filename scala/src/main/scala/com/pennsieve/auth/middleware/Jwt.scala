// Copyright (c) 2021 University of Pennsylvania. All Rights Reserved.

package com.pennsieve.auth.middleware

import com.pennsieve.utilities.circe._
// This is only needed for compiling a version that uses Circe 0.11.
// It can be deleted when only version of Circe required is >= 0.12.
// (That is, when no longer compiling for Scala 2.12.)
import com.pennsieve.scala.Compatibility._

import com.pennsieve.models.{ Feature, Role => PennsieveRole }
import io.circe.{ Decoder, Encoder, Json, Printer }
import io.circe.parser.decode
import io.circe.syntax._
import io.circe.shapes._
import io.circe.generic.extras.auto._
import io.circe.generic.extras.semiauto._
import pdi.jwt.{ JwtAlgorithm, JwtCirce, JwtClaim }
import shapeless._
import java.time.Instant

import com.pennsieve.auth.middleware.Jwt.Role.RoleIdentifier

import scala.concurrent.duration.FiniteDuration

object Jwt {
  sealed trait Role {
    import Role._

    val id: RoleIdentifier[Identifier]
    val role: PennsieveRole
    val node_id: Option[NodeIdentifier]

    def isWildcard: Boolean = id.select[Wildcard.type].isDefined
  }

  object Role {
    type RoleIdentifier[T <: Identifier] = T :+: Wildcard.type :+: CNil
  }

  case class DatasetRole private (
    id: Role.RoleIdentifier[DatasetId],
    role: PennsieveRole,
    node_id: Option[DatasetNodeId] = None,
    locked: Option[Boolean] = None
  ) extends Role

  case class OrganizationRole(
    id: Role.RoleIdentifier[OrganizationId],
    role: PennsieveRole,
    encryption_key_id: Option[EncryptionKeyId] = None,
    node_id: Option[OrganizationNodeId] = None,
    enabled_features: Option[List[Feature]] = None
  ) extends Role

  object OrganizationRole {
    implicit val decodeFeatures: Decoder[List[Feature]] =
      Decoder
        .decodeList(Decoder[Feature].either(Decoder[Json]))
        .map(_.flatMap(_.left.toOption))

    implicit def organizationRoleEncoder: Encoder[OrganizationRole] =
      deriveEncoder[OrganizationRole]
    implicit def organizationRoleDecoder: Decoder[OrganizationRole] =
      deriveDecoder[OrganizationRole]
  }

  case class WorkspaceRole(id: RoleIdentifier[WorkspaceId], role: PennsieveRole)
      extends Role {
    val node_id = None
  }

  final val printer: Printer = Printer.noSpaces.copy(dropNullValues = true)

  trait Config {
    def key: String
    def algorithm = JwtAlgorithm.HS256
  }

  case class Token(val value: String) extends AnyVal

  case class Claim(
    content: ClaimType,
    expiration: Instant,
    issuedAt: Instant = Instant.now
  ) {
    def toJwtClaim: JwtClaim = {

      JwtClaim(
        content = printer.print(content.asJson),
        expiration = Some(expiration.getEpochSecond),
        issuedAt = Some(issuedAt.getEpochSecond)
      )
    }

    def isValid: Boolean = {
      expiration.isAfter(Instant.now)
    }
  }

  def generateClaim(content: ClaimType, duration: FiniteDuration): Claim = {
    val expiration = Instant.now.plusSeconds(duration.toSeconds)

    Claim(content, expiration)
  }

  def generateToken(claim: Claim)(implicit config: Config): Token = {
    val token = JwtCirce.encode(claim.toJwtClaim, config.key, config.algorithm)

    Token(token)
  }

  def parseClaim(
    token: Token
  )(implicit
    config: Config
  ): Either[Throwable, Claim] =
    for {
      claim <- JwtCirce
        .decode(token.value, config.key, Seq(config.algorithm))
        .toEither
      content <- decode[ClaimType](claim.content)
    } yield
      Claim(
        content = content,
        expiration = Instant.ofEpochSecond(claim.expiration.getOrElse(0)),
        issuedAt = Instant.ofEpochSecond(claim.issuedAt.getOrElse(0))
      )
}
