package io.github.nafg.simplefacade

import scala.language.implicitConversions
import scala.scalajs.js
import scala.scalajs.js.JSConverters._

import japgolly.scalajs.react.component.Js
import japgolly.scalajs.react.vdom.VdomElement


object Factory {
  implicit def toVdomElement(builder: Factory[_]): VdomElement = builder.render.vdomElement

  type Setting[A] = A => PropTypes.Setting
}

case class Factory[A](propTypes: A, component: Facade.JsComponentType, values: Seq[PropTypes.Setting] = Vector.empty) {
  def apply(pairs: Factory.Setting[A]*): Factory[A] = copy(values = values ++ pairs.map(_.apply(propTypes)))
  def rawProps: js.Object = MergeProps(values.toJSArray.map(_.toRawProps))
  def render: Js.UnmountedWithRawType[js.Object, Null, Js.RawMounted[js.Object, Null]] = {
    component.apply(rawProps)()
  }
}
