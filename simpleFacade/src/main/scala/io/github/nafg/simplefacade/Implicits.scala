package io.github.nafg.simplefacade

import scala.scalajs.js

import japgolly.scalajs.react.CallbackTo
import japgolly.scalajs.react.facade.React
import japgolly.scalajs.react.facade.React.ElementType
import japgolly.scalajs.react.vdom.VdomElement
import japgolly.scalajs.react.vdom.html_<^.VdomNode

import slinky.readwrite.{Reader, Writer}


object Implicits {
  implicit val vdomNodeWriter: Writer[VdomNode]             = _.rawNode.asInstanceOf[js.Object]
  implicit val vdomNodeReader: Reader[VdomNode]             = raw => VdomNode(raw.asInstanceOf[React.Node])
  implicit val vdomElementWriter: Writer[VdomElement]       = _.rawNode.asInstanceOf[js.Object]
  implicit val undefinedWriter: Writer[js.UndefOr[Nothing]] = _ => js.undefined.asInstanceOf[js.Object]
  implicit val elementTypeWriter: Writer[ElementType]       = _.asInstanceOf[js.Object]

  implicit def callbackToWriter[A](implicit A: Writer[A]): Writer[CallbackTo[A]] = _.map(A.write).runNow()
}
