package io.github.nafg.simplefacade

import scala.language.dynamics
import scala.scalajs.js

import japgolly.scalajs.react.Key
import io.github.nafg.simplefacade.Implicits.unionWriter

import com.payalabs.scalajs.react.bridge.JsWriter


object PropTypes {
  class Setting(val key: String, val value: js.Any)
  object Setting {
    implicit class fromBooleanProp(prop: Prop[Boolean]) extends Setting(prop.name, true)
  }

  class Prop[A](val name: String)(implicit jsWriter: JsWriter[A]) {
    def :=(value: A): Setting = new Setting(name, jsWriter.toJs(value))
  }

  trait WithChildren[C] extends PropTypes {
    def children: PropTypes.Prop[C]
  }
}

trait PropTypes extends Dynamic {
  protected def writeJsOpaque[A] = JsWriter[A](_.asInstanceOf[js.Any])

  def applyDynamic[A](name: String)(value: A)(implicit jsWriter: JsWriter[A]): PropTypes.Setting =
    new PropTypes.Setting(name, jsWriter.toJs(value))

  def of[A](implicit name: sourcecode.Name, jsWriter: JsWriter[A]) = new PropTypes.Prop[A](name.value)

  val key = of[Key]
}
