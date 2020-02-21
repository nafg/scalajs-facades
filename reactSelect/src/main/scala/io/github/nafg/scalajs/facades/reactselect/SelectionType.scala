package io.github.nafg.scalajs.facades.reactselect

import scala.scalajs.js
import scala.scalajs.js.JSConverters._
import scala.scalajs.js.UndefOr

import io.github.nafg.simplefacade.Factory

import com.payalabs.scalajs.react.bridge.JsWriter


trait SelectionType[F[_]] {
  type Js[A]
  implicit def jsWriter[A: JsWriter]: JsWriter[F[A]]
  def defaultProps[A]: Seq[Factory.Setting[CommonProps[A]]]
  def toSeq[A](fa: F[A]): Seq[A]
  def fromJs[A](jsa: Js[A]): F[A]
}

object SelectionType {
  type Id[A] = A

  implicit object Single extends SelectionType[Id] {
    override type Js[A] = A
    override implicit def jsWriter[A](implicit writer: JsWriter[A]): JsWriter[A] = writer
    override def defaultProps[A] = Seq(_.isClearable := false, _.isMulti := false)
    override def toSeq[A](fa: Id[A]) = Seq(fa)
    override def fromJs[A](jsa: A) = jsa
  }
  implicit object Optional extends SelectionType[Option] {
    override type Js[A] = js.UndefOr[A]
    override implicit def jsWriter[A: JsWriter]: JsWriter[Option[A]] = com.payalabs.scalajs.react.bridge.optionWriter[A]
    override def defaultProps[A] = Seq(_.isClearable := true, _.isMulti := false)
    override def toSeq[A](fa: Option[A]) = fa.toSeq
    override def fromJs[A](jsa: UndefOr[A]) = jsa.toOption.filter(_ != null)
  }
  implicit object Multi extends SelectionType[Seq] {
    override type Js[A] = js.Array[A]
    override implicit def jsWriter[A: JsWriter]: JsWriter[Seq[A]] = {
      val elementWriter = implicitly[JsWriter[A]]
      JsWriter(_.map(elementWriter.toJs).toJSArray)
    }

    override def defaultProps[A] = Seq(_.isClearable := true, _.isMulti := true)
    override def toSeq[A](fa: Seq[A]) = fa
    override def fromJs[A](jsa: js.Array[A]) = jsa.toSeq
  }
}
