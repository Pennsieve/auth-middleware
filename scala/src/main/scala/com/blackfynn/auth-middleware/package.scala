// Copyright (c) 2021 University of Pennsylvania. All Rights Reserved.

package com.pennsieve.auth

import io.circe.generic.extras._

package object middleware {
  final private val discriminator: String = "type"

  implicit val configuration: Configuration =
    Configuration.default
      .withDiscriminator(discriminator)
      .withSnakeCaseConstructorNames
}
