package io.github.nafg.simplefacade

import scala.language.implicitConversions
import scala.scalajs.js

import japgolly.scalajs.react.component.Js
import japgolly.scalajs.react.vdom.VdomElement
import io.github.nafg.simplefacade.MergeProps.AnyDict


object Factory {

  /** Implicit conversion allowing a [[Factory]] to be used directly where a `VdomElement` is expected. */
  implicit def toVdomElement(builder: Factory[_]): VdomElement = builder.render.vdomElement

  /** A setting is a function from the Props object to a [[PropTypes.Setting]]. This is why prop assignment uses the
    * `_.propName := value` syntax — the `_` is the Props instance.
    */
  type Setting[A] = A => PropTypes.Setting
}

case class Factory[A](
  propTypes: A,
  component: Facade.JsComponentType,
  settings: Seq[PropTypes.Setting] = Vector.empty) {
  def apply(pairs: Factory.Setting[A]*): Factory[A] = copy(settings = settings ++ pairs.map(_.apply(propTypes)))
  def toDict: AnyDict                               = PropTypes.Setting.toDict(settings: _*)
  def rawProps: js.Object                           = toDict.asInstanceOf[js.Object]

  def render: Js.UnmountedWithRawType[js.Object, Null, Js.RawMounted[js.Object, Null]] = component.apply(rawProps)
}
