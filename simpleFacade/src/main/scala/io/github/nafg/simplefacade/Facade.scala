package io.github.nafg.simplefacade

import scala.scalajs.js

import japgolly.scalajs.react.component.Js
import japgolly.scalajs.react.{Children, CtorType, JsComponent}


object Facade {
  type JsComponentType =
    Js.ComponentSimple[
      js.Object,
      CtorType.Summoner.Aux[js.Object, Children.Varargs, CtorType.Props]#CT,
      Js.UnmountedWithRawType[js.Object, Null, Js.RawMounted[js.Object, Null]]
    ]

  def apply(component: Facade.JsComponentType): Facade = new Facade(component)

  def apply(component: js.Any, name: Option[String] = None)(implicit where: sourcecode.File, line: sourcecode.Line)
    : Facade =
    try
      apply(JsComponent[js.Object, Children.None, Null](component))
    catch {
      case ex: Throwable =>
        throw new Exception(s"Could not create Facade for $name", ex)
    }
}

class Facade(component: Facade.JsComponentType) {
  def factory[A](propTypes: A): Factory[A] = new Factory[A](propTypes, component)
}
