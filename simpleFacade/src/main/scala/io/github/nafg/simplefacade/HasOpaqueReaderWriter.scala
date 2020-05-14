package io.github.nafg.simplefacade

import scala.scalajs.js

import slinky.readwrite.{Reader, Writer}


trait HasOpaqueReaderWriter[A] {
  protected implicit val opaqueWriter: Writer[A] = _.asInstanceOf[js.Object]
  protected implicit val opaqueReader: Reader[A] = _.asInstanceOf[A]
}
