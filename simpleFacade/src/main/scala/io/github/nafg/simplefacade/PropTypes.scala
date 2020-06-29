package io.github.nafg.simplefacade

import scala.language.dynamics
import scala.scalajs.js

import japgolly.scalajs.react.Key

import slinky.readwrite.Writer


object PropTypes {
  class Setting(val key: String, val value: js.Any)
  object Setting {
    implicit class fromBooleanProp(prop: Prop[Boolean]) extends Setting(prop.name, true)
  }

  class Prop[A](val name: String)(implicit writer: Writer[A]) {
    def :=(value: A): Setting = new Setting(name, writer.write(value))
    def :=?(value: Option[A]): Setting = new Setting(name, value.map(writer.write).getOrElse(js.undefined))
  }

  trait WithChildren[C] extends PropTypes {
    def children: PropTypes.Prop[C]
  }
}

trait PropTypes extends Dynamic {
  def applyDynamic[A](name: String)(value: A)(implicit writer: Writer[A]): PropTypes.Setting =
    new PropTypes.Setting(name, writer.write(value))

  def of[A: Writer](implicit name: sourcecode.Name) = new PropTypes.Prop[A](name.value)

  val key = of[Key]
}
