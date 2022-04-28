// Copyright (c) 2021 University of Pennsylvania. All Rights Reserved.

package com.pennsieve.auth.middleware

import java.time.Instant

import com.pennsieve.auth.middleware._
import com.pennsieve.utilities.circe._
import com.pennsieve.models.{ CognitoId, Role }
import enumeratum._
import enumeratum.values._
import enumeratum.EnumEntry._

import io.circe.Decoder.Result
import io.circe._
import io.circe.shapes._
import io.circe.generic.extras.auto._
import io.circe.generic.extras.semiauto._
import io.circe.syntax._

import cats.implicits._

import scala.collection.immutable
import scala.language.experimental.macros

sealed trait Identifier extends Any
sealed trait NodeIdentifier extends Any

case class UserId(val value: Int) extends AnyVal
case class OrganizationId(val value: Int) extends AnyVal with Identifier
case class DatasetId(val value: Int) extends AnyVal with Identifier
case class WorkspaceId(val value: Int) extends AnyVal with Identifier

case class UserNodeId(val value: String) extends AnyVal
case class OrganizationNodeId(val value: String)
    extends AnyVal
    with NodeIdentifier
case class DatasetNodeId(val value: String) extends AnyVal with NodeIdentifier
// Note: no node ID for Workspaces

case class EncryptionKeyId(val value: String) extends AnyVal

case object Wildcard {
  final val symbol: String = "*"

  implicit def encoder: Encoder[Wildcard.type] =
    Encoder.encodeString.contramap[Wildcard.type](_.toString)

  implicit def decoder: Decoder[Wildcard.type] =
    Decoder.decodeString.emap {
      case `symbol` => Right(this)
      case _ => Left("invalid wildcard")
    }

  override def toString: String = symbol
}

sealed trait CognitoSession {
  val id: CognitoId
  val exp: Instant

  def isBrowser: Boolean = this match {
    case _: CognitoSession.Browser => true
    case _ => false
  }

  def isAPI: Boolean = this match {
    case _: CognitoSession.API => true
    case _ => false
  }
}

object CognitoSession {
  implicit def cognitoSessionEncoder: Encoder[CognitoSession] =
    deriveEncoder[CognitoSession]
  implicit def cognitoSessionDecoder: Decoder[CognitoSession] =
    deriveDecoder[CognitoSession]

  implicit def instantDecoder: Decoder[Instant] =
    Decoder.decodeInt.emap { instantCode =>
      Either
        .catchNonFatal {
          Instant.ofEpochSecond(instantCode)
        }
        .leftMap(t => "exp")
    }

  implicit val instantEncoder: Encoder[Instant] =
    Encoder.instance(time => Json.fromLong(time.toEpochMilli / 1000))

  case class Browser(id: CognitoId.UserPoolId, exp: Instant)
      extends CognitoSession
  case class API(id: CognitoId.TokenPoolId, exp: Instant) extends CognitoSession
}

sealed trait ClaimType {
  val roles: List[Jwt.Role]
}

object ClaimType {
  implicit def claimTypeEncoder: Encoder[ClaimType] = deriveEncoder[ClaimType]
  implicit def claimTypeDecoder: Decoder[ClaimType] = deriveDecoder[ClaimType]
}

case class UserClaim(
  id: UserId,
  roles: List[Jwt.Role],
  cognito: Option[CognitoSession] = None,
  node_id: Option[UserNodeId] = None
) extends ClaimType

object UserClaim {
  implicit def userClaimEncoder: Encoder[UserClaim] = deriveEncoder[UserClaim]
  implicit def userClaimDecoder: Decoder[UserClaim] = deriveDecoder[UserClaim]
}

case class ServiceClaim(roles: List[Jwt.Role]) extends ClaimType

object ServiceClaim {
  implicit def serviceClaimEncoder: Encoder[ServiceClaim] =
    deriveEncoder[ServiceClaim]
  implicit def serviceClaimDecoder: Decoder[ServiceClaim] =
    deriveDecoder[ServiceClaim]
}
