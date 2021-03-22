// Copyright (c) 2021 University of Pennsylvania. All Rights Reserved.

package com.pennsieve.auth.middleware

import os.{ Path, RelPath }

object Resources {

  val testResources: Path = os.pwd / RelPath("../resources")

  def readClaim(filename: String): String =
    os.read(testResources / filename).stripMargin
}
