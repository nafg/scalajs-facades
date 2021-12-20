package io.github.nafg.simplefacade

import scala.language.implicitConversions
import scala.scalajs.js

import japgolly.scalajs.react.component.Js
import japgolly.scalajs.react.vdom.VdomElement
import io.github.nafg.simplefacade.MergeProps.AnyDict


object Factory {
  implicit def toVdomElement(builder: Factory[_]): VdomElement = builder.render.vdomElement

  type Setting[A] = A => PropTypes.Setting
}

case class Factory[A](propTypes: A,
                      component: Facade.JsComponentType,
                      settings: Seq[PropTypes.Setting] = Vector.empty) {
  def apply(pairs: Factory.Setting[A]*): Factory[A] = copy(settings = settings ++ pairs.map(_.apply(propTypes)))
  def rawProps: AnyDict = PropTypes.Setting.toDict(settings: _*)
  def render: Js.UnmountedWithRawType[js.Object, Null, Js.RawMounted[js.Object, Null]] =
    component.apply(rawProps.asInstanceOf[js.Object])
}
