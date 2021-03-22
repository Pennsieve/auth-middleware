// Copyright (c) 2019 Pennsieve, Inc. All Rights Reserved.

package com.blackfynn.auth.middleware.cli

import com.blackfynn.auth.middleware.Jwt.{ Claim, Config, Token }
import com.blackfynn.auth.middleware.Jwt.Claim._
import com.blackfynn.auth.middleware.Jwt.Role.RoleIdentifier
import com.blackfynn.auth.middleware.{
  DatasetId,
  DatasetNodeId,
  Identifier,
  OrganizationId,
  OrganizationNodeId,
  UserId,
  Wildcard
}
import com.blackfynn.models.{ Feature, Role => PennsieveRole }
import com.blackfynn.models.Role._
import org.rogach.scallop._
import io.circe._
import io.circe.parser._
import io.circe.syntax._
import shapeless.syntax.inject._

import scala.concurrent.duration._
import scala.util.Try

class Args(arguments: Seq[String]) extends ScallopConf(arguments) {

  banner("""Generate a JWT""")

  // --key=<string>
  //   The key the JWT will be encrypted with.
  val key = opt[String](
    "key",
    required = true,
    argName = "key",
    descr = "The token encryption key"
  )

  // --role=<string>
  //   The role attached to the JWT.
  val role = opt[String](
    "role",
    argName = "role",
    descr = "The user role. If omitted, 'Viewer' will be used"
  )

  // --user=<int>
  //   Specify which user to use when generating the JWT.
  val user = opt[Int]("user", argName = "user", descr = "Integer user ID")

  // --organization=<int>
  //   Specify which organization to use when generating the JWT.
  val organization =
    opt[String](
      "organization",
      required = true,
      argName = "organization",
      descr = "Integer organization ID | *"
    )

  // --organizationNode=<string>
  //   Specify which organization to use when generating the JWT.
  val organizationNode =
    opt[String](
      "organizationNode",
      argName = "organizationNode",
      descr = "Organization node ID"
    )

  // --dataset=<int>
  //   Specify which dataset to use when generating the JWT
  val dataset =
    opt[String]("dataset", argName = "dataset", descr = "Integer dataset ID")

  // --datasetNode=<string>
  //   Specify which dataset to use when generating the JWT
  val datasetNode =
    opt[String](
      "datasetNode",
      argName = "datasetNode",
      descr = "Dataset node ID"
    )

  //--verbose
  val verbose = opt[Boolean](
    "verbose",
    argName = "verbose",
    descr = "Enables verbose output"
  )

  //--feature
  val features =
    opt[List[String]](
      name = "feature",
      argName = "feature",
      descr =
        "A feature to add; multiple features can be specified: --feature=\"foo\" --feature=\"bar\""
    )

  // --all-features
  val allFeatures =
    opt[Boolean](
      name = "all-features",
      argName = "all-features",
      default = Some(false),
      descr = "If given, all features will be included in the JWT"
    )

  // --expires
  val expires =
    opt[Long](
      name = "expires",
      argName = "expires",
      default = Some(60L),
      descr = "Expiration time in minutes"
    )

  verify()
}

object Args {
  def parse(arguments: Seq[String]) = new Args(arguments)
}

object Main {

  private def parseIdentifier[T <: Identifier](
    input: String,
    f: Int => T
  ): Option[RoleIdentifier[T]] = {
    input match {
      case "*" => Some(Wildcard.inject[RoleIdentifier[T]])
      case numericId =>
        Try(numericId.toInt).toOption.map(f(_).inject[RoleIdentifier[T]])
    }
  }

  def main(arguments: Array[String]): Unit = {

    val args = Args.parse(arguments)

    val config: Config = new Config {
      val key = args.key()
    }

    val verbose = args.verbose()

    val role: PennsieveRole = args.role.toOption
      .flatMap(
        (role: String) => decode[PennsieveRole](role.toLowerCase.trim).toOption
      )
      .getOrElse(PennsieveRole.Viewer)

    val organizationIntId: RoleIdentifier[OrganizationId] =
      parseIdentifier(args.organization(), OrganizationId)
        .getOrElse(throw new Throwable("bad organization"))

    val organizationNodeId: Option[OrganizationNodeId] =
      args.organizationNode.toOption.map(OrganizationNodeId)

    val userId: Option[UserId] = args.user.toOption.map(UserId(_))

    val datasetIntId: Option[RoleIdentifier[DatasetId]] =
      args.dataset.toOption.map(
        (datasetId: String) =>
          parseIdentifier(datasetId, DatasetId)
            .getOrElse(throw new Throwable("bad dataset"))
      )

    val datasetNodeId: Option[DatasetNodeId] =
      args.datasetNode.toOption.map(DatasetNodeId)

    val useAllFeatures: Boolean = args.allFeatures()

    val features = if (useAllFeatures) {
      Feature.values.toList
    } else {
      args.features.toOption
        .map { features: List[String] =>
          features.map { featureArg: String =>
            decode[Feature](featureArg).toOption
              .getOrElse(throw new Exception(s"Invalid feature: ${featureArg}"))
          }
        }
        .getOrElse(List.empty)
    }

    val expiresAfterMinutes: FiniteDuration = args.expires().minutes

    Jwt.generate(
      role = role,
      userId = userId,
      organizationIntId = Some(organizationIntId),
      organizationNodeId = organizationNodeId,
      datasetIntId = datasetIntId,
      datasetNodeId = datasetNodeId,
      features = features,
      expiresAfterMinutes = expiresAfterMinutes
    )(config) match {
      case Right((claim, token)) => {
        if (verbose) {
          println(s"CLAIM = ${claim.content.asJson.spaces4}\n")
          println(s"EXPIRES = ${claim.expiration}\n")
          println(s"TOKEN = ${token.value}\n")
        } else {
          println(token.value)
        }
      }
      case Left(err) => throw err
    }
  }
}
