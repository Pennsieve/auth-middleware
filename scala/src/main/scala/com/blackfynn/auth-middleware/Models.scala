// Copyright (c) 2021 University of Pennsylvania. All Rights Reserved.

package com.pennsieve.auth.middleware

import com.pennsieve.auth.middleware._
import com.pennsieve.utilities.circe._
import com.pennsieve.models.Role
import enumeratum._
import enumeratum.values._
import enumeratum.EnumEntry._
import io.circe.Decoder.Result
import io.circe._
import io.circe.shapes._
import io.circe.generic.extras.auto._
import io.circe.generic.extras.semiauto._

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

sealed trait Session {
  val id: String

  def isBrowser: Boolean = this match {
    case _: Session.Browser => true
    case _ => false
  }

  def isAPI: Boolean = this match {
    case _: Session.API => true
    case _ => false
  }

  def isTemporary: Boolean = this match {
    case _: Session.Temporary => true
    case _ => false
  }
}

object Session {
  implicit def sessionEncoder: Encoder[Session] = deriveEncoder[Session]
  implicit def sessionDecoder: Decoder[Session] = deriveDecoder[Session]

  case class Browser(override val id: String) extends Session
  case class API(override val id: String) extends Session
  case class Temporary(override val id: String) extends Session
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
  session: Option[Session] = None,
  node_id: Option[UserNodeId] = None
) extends ClaimType

object UserClaim {

  private def v2Decoder: Decoder[UserClaim] = deriveDecoder[UserClaim]

  private def v1Decoder: Decoder[UserClaim] = new Decoder[UserClaim] {
    final def apply(c: HCursor): Decoder.Result[UserClaim] =
      for {
        id <- c.downField("id").as[UserId]
        roles <- c.downField("roles").as[List[Jwt.Role]]
        session <- c.downField("session").as[String]
        node_id <- c.downField("node_id").as[Option[UserNodeId]]
      } yield
        UserClaim(
          id = id,
          roles = roles,
          session = Some(Session.Browser(session)), // default to a browser session
          node_id = node_id
        )
  }

  implicit def userClaimEncoder: Encoder[UserClaim] = deriveEncoder[UserClaim]
  implicit def userClaimDecoder: Decoder[UserClaim] =
    List(v2Decoder, v1Decoder).reduceLeft(_ or _)
}

case class ServiceClaim(roles: List[Jwt.Role]) extends ClaimType

object ServiceClaim {
  implicit def serviceClaimEncoder: Encoder[ServiceClaim] =
    deriveEncoder[ServiceClaim]
  implicit def serviceClaimDecoder: Decoder[ServiceClaim] =
    deriveDecoder[ServiceClaim]
}
