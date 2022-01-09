package io.github.nafg.simplefacade

import scala.language.{dynamics, implicitConversions}
import scala.scalajs.js
import japgolly.scalajs.react.Key
import japgolly.scalajs.react.vdom.TagMod
import io.github.nafg.simplefacade.MergeProps.AnyDict
import slinky.readwrite.Writer


object PropTypes {
  sealed trait Setting {
    def applyToDict(dict: AnyDict): Unit
  }
  object Setting {
    class Single(val key: String, val value: js.Any) extends Setting {
      override def toString = s"""$key: $value"""
      override def applyToDict(dict: AnyDict): Unit = {
        val existingValue: js.Any = if (dict.contains(key)) js.Any.wrapDictionary(dict)(key) else js.undefined
        dict(key) = MergeProps.merge(key, existingValue, value)
      }
    }

    implicit class FromBooleanProp(prop: Prop[Boolean]) extends Single(prop.name, true)

    implicit class Multiple(val settings: Iterable[Setting]) extends Setting {
      override def applyToDict(dict: AnyDict): Unit = settings.foreach(_.applyToDict(dict))
    }

    implicit def fromConvertibleToIterablePairs[A](pairs: A)(implicit view: A => Iterable[(String, js.Any)]): Setting =
      new Multiple(view(pairs).map { case (k, v) => new Single(k, v) })

    implicit def fromTagMod(tagMod: TagMod): Setting = {
      val raw = tagMod.toJs
      raw.addKeyToProps()
      raw.addStyleToProps()
      raw.addClassNameToProps()
      new Multiple(
        raw.nonEmptyChildren.toList.map(new Single("children", _)) :+
          new Multiple(raw.props.asInstanceOf[AnyDict].map { case (k, v) => new Single(k, v) })
      )
    }

    implicit def toFactorySetting[A](value: A)(implicit view: A => Setting): Any => Setting = _ => view(value)

    def toDict(settings: Setting*): AnyDict = {
      val base = js.Dictionary.empty[js.Any]
      settings.foreach(_.applyToDict(base))
      base
    }
  }

  class Prop[A](val name: String)(implicit writer: Writer[A]) {
    def :=(value: A): Setting = new Setting.Single(name, writer.write(value))
    def :=?(value: Option[A]): Setting = new Setting.Single(name, value.map(writer.write).getOrElse(js.undefined))
    def setAs[B](value: B)(implicit B: Writer[B]): Setting = new Setting.Single(name, B.write(value))
    def setRaw(value: js.Any): Setting = new Setting.Single(name, value)
  }

  trait WithChildren[C] extends PropTypes {
    def children: PropTypes.Prop[C]
  }
}

trait PropTypes {
  object dyn extends Dynamic {
    def applyDynamic[A](name: String)(value: A)(implicit writer: Writer[A]): PropTypes.Setting =
      new PropTypes.Setting.Single(name, writer.write(value))
  }

  def of[A: Writer](implicit name: sourcecode.Name) = new PropTypes.Prop[A](name.value)
  val key = of[Key]
}
