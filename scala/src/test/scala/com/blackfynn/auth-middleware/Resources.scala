// Copyright (c) 2019 Blackfynn, Inc. All Rights Reserved.

package com.blackfynn.auth.middleware

import os.{ Path, RelPath }

object Resources {

  val testResources: Path = os.pwd / RelPath("../resources")

  def readClaim(filename: String): String =
    os.read(testResources / filename).stripMargin
}
