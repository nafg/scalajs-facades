package io.github.nafg.simplefacade

import scala.scalajs.js

import japgolly.scalajs.react.CallbackTo
import japgolly.scalajs.react.raw.React
import japgolly.scalajs.react.raw.React.ElementType
import japgolly.scalajs.react.vdom.VdomElement
import japgolly.scalajs.react.vdom.html_<^.VdomNode

import slinky.readwrite.{Reader, Writer}


object Implicits {
  implicit def vdomNodeWriter: Writer[VdomNode] = _.rawNode.asInstanceOf[js.Object]
  implicit def vdomNodeReader: Reader[VdomNode] = raw => VdomNode(raw.asInstanceOf[React.Node])
  implicit def vdomElementWriter: Writer[VdomElement] = _.rawNode.asInstanceOf[js.Object]
  implicit def elementTypeWriter: Writer[ElementType] = _.asInstanceOf[js.Object]
  implicit def callbackToWriter[A](implicit A: Writer[A]): Writer[CallbackTo[A]] = _.map(A.write).runNow()
}
