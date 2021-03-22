// Copyright (c) 2019 Pennsieve, Inc. All Rights Reserved.

package com.blackfynn.auth

import io.circe.generic.extras._

package object middleware {
  final private val discriminator: String = "type"

  implicit val configuration: Configuration =
    Configuration.default
      .withDiscriminator(discriminator)
      .withSnakeCaseConstructorNames
}
