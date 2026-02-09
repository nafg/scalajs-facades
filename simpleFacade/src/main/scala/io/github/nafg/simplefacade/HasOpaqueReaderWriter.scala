package io.github.nafg.simplefacade

import scala.scalajs.js

import slinky.readwrite.{Reader, Writer}


/** Provides Writer/Reader instances that pass values through as-is (via `asInstanceOf`).
  *
  * Mix this into a Props trait when the component is generic over a type `A` that is opaque to JS — the JavaScript side
  * passes values through without needing a certain type or structure (e.g., option values in a select component).
  */
trait HasOpaqueReaderWriter[A] {
  protected implicit val opaqueWriter: Writer[A] = _.asInstanceOf[js.Object]
  protected implicit val opaqueReader: Reader[A] = _.asInstanceOf[A]
}
