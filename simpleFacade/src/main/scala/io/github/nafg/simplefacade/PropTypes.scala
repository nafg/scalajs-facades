package io.github.nafg.simplefacade

import scala.language.{dynamics, implicitConversions}
import scala.scalajs.js
import japgolly.scalajs.react.Key
import japgolly.scalajs.react.vdom.TagMod

import slinky.readwrite.Writer


object PropTypes {
  sealed trait Setting {
    def toMap: Map[String, js.Any]
  }
  object Setting {
    class Single(val key: String, val value: js.Any) extends Setting {
      override def toMap = Map(key -> value)
      override def toString = s"""$key: $value"""
    }
    implicit class FromBooleanProp(prop: Prop[Boolean]) extends Single(prop.name, true)
    implicit class Multiple(val settings: Seq[Setting]) extends Setting {
      override def toMap = settings.flatMap(_.toMap).toMap
    }

    implicit def fromConvertibleToIterablePairs[A](pairs: A)(implicit view: A => Iterable[(String, js.Any)]): Setting =
      new Multiple(view(pairs).map { case (k, v) => new Single(k, v) }.toSeq)
    implicit def fromTagMod(tagMod: TagMod): Setting = {
      val raw = tagMod.toJs
      raw.addKeyToProps()
      raw.addStyleToProps()
      raw.addClassNameToProps()
      new Multiple(
        raw.nonEmptyChildren.toList.map(new Single("children", _)) :+
          fromConvertibleToIterablePairs(raw.props.asInstanceOf[js.Dictionary[js.Any]])
      )
    }
    implicit def toFactorySetting[A](value: A)(implicit view: A => Setting): Any => Setting = _ => view(value)
  }

  class Prop[A](val name: String)(implicit writer: Writer[A]) {
    def :=(value: A): Setting = new Setting.Single(name, writer.write(value))
    def :=?(value: Option[A]): Setting = new Setting.Single(name, value.map(writer.write).getOrElse(js.undefined))
  }

  trait WithChildren[C] extends PropTypes {
    def children: PropTypes.Prop[C]
  }
}

trait PropTypes extends Dynamic {
  def applyDynamic[A](name: String)(value: A)(implicit writer: Writer[A]): PropTypes.Setting =
    new PropTypes.Setting.Single(name, writer.write(value))

  def of[A: Writer](implicit name: sourcecode.Name) = new PropTypes.Prop[A](name.value)

  val key = of[Key]
}
