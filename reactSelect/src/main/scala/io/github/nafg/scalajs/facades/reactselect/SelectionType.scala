package io.github.nafg.scalajs.facades.reactselect

import scala.scalajs.js
import scala.scalajs.js.JSConverters._

import io.github.nafg.simplefacade.Factory

import slinky.readwrite.{Reader, Writer}


trait SelectionType[F[_]] {
  implicit def writer[A](implicit A: Writer[A]): Writer[F[A]]
  implicit def reader[A](implicit A: Reader[A]): Reader[F[A]]
  def defaultProps[A]: Seq[Factory.Setting[CommonProps[A]]]
  def toSeq[A](fa: F[A]): Seq[A]
}

object SelectionType {
  type Id[A] = A

  implicit object Single extends SelectionType[Id] {
    override implicit def writer[A](implicit A: Writer[A]): Writer[Id[A]] = A
    override implicit def reader[A](implicit A: Reader[A]): Reader[Id[A]] = A
    override def defaultProps[A] = Seq(_.isClearable := false, _.isMulti := false)
    override def toSeq[A](fa: Id[A]) = Seq(fa)
  }
  implicit object Optional extends SelectionType[Option] {
    override implicit def writer[A](implicit A: Writer[A]): Writer[Option[A]] = Writer.optionWriter[A]
    override implicit def reader[A](implicit A: Reader[A]): Reader[Option[A]] = Reader.optionReader[A]
    override def defaultProps[A] = Seq(_.isClearable := true, _.isMulti := false)
    override def toSeq[A](fa: Option[A]) = fa.toSeq
  }
  implicit object Multi extends SelectionType[Seq] {
    override implicit def writer[A](implicit A: Writer[A]): Writer[Seq[A]] = _.map(A.write).toJSArray
    override implicit def reader[A](implicit A: Reader[A]): Reader[Seq[A]] = {
      case null => Nil
      case v    => v.asInstanceOf[js.Array[js.Object]].toSeq.map(A.read)
    }
    override def defaultProps[A] = Seq(_.isClearable := true, _.isMulti := true)
    override def toSeq[A](fa: Seq[A]) = fa
  }

  implicit def reader[A: Reader, F[_]](implicit F: SelectionType[F]): Reader[F[A]] = F.reader[A]
  implicit def writer[A: Writer, F[_]](implicit F: SelectionType[F]): Writer[F[A]] = F.writer[A]
}
