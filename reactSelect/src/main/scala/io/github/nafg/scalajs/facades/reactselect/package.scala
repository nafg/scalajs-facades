package io.github.nafg.scalajs.facades

import scala.concurrent.Future
import scala.language.implicitConversions
import scala.scalajs.js
import scala.scalajs.js.JSConverters.JSRichGenTraversableOnce
import scala.scalajs.js.|

import japgolly.scalajs.react.vdom.VdomNode

import com.payalabs.scalajs.react.bridge.JsWriter


package object reactselect {
  class OptGroup[A](val label: String, val options: js.Array[A]) extends js.Object

  type Opt[A] = A | OptGroup[A]

  def OptGroup[A](label: String)(options: Seq[A]): Opt[A] =
    new OptGroup[A](label, options.toJSArray)

  def Opt[A](value: A): Opt[A] = value

  implicit class Opt_fold[A](self: Opt[A]) {
    def foldRaw[R](groupF: OptGroup[A] => R, optionF: A => R): R =
      if (self.isInstanceOf[OptGroup[_]])
        groupF(self.asInstanceOf[OptGroup[A]])
      else
        optionF(self.asInstanceOf[A])
  }

  implicit class AsyncResults[A](val self: Future[Seq[Opt[A]]]) extends AnyVal
  object AsyncResults {
    implicit def simple[A](fut: Future[Seq[A]]): AsyncResults[A] =
      new AsyncResults[A](fut.asInstanceOf[Future[Seq[Opt[A]]]])
  }

  implicit object vdomNodeWriter extends JsWriter[VdomNode] {
    override def toJs(value: VdomNode) = value.rawNode.asInstanceOf[js.Any]
  }
}
