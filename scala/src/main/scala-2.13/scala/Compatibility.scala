// Copyright (c) 2021 University of Pennsylvania. All Rights Reserved.

package com.pennsieve.scala
import io.circe.{ Json, Printer }

object Compatibility {

  /*
    The scala-2.12 version of this object provides an implicit io.circe.Printer.print(...)
     method that delegates to io.circe.Printer.pretty(...) provided by Circe 0.11.

     There is no need for that here in scala-2.13 since the later version of Circe being used here has
     already renamed .pretty(...) to .print(...)
 */
}
