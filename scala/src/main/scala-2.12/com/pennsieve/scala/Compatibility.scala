// Copyright (c) 2021 University of Pennsylvania. All Rights Reserved.

package com.pennsieve.scala
import io.circe.{ Json, Printer }

object Compatibility {

  implicit class CircePrinterCompatibility(val printer: Printer) {
    // io.circe.Printer.pretty(..) was changed to .print(...) in Circe versions > 0.11
    def print(json: Json): String = {
      printer.pretty(json)
    }
  }
}
