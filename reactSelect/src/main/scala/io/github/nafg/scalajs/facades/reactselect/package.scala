package io.github.nafg.scalajs.facades

import scala.concurrent.Future
import scala.language.implicitConversions
import scala.scalajs.js
import scala.scalajs.js.|

import slinky.readwrite.{Reader, Writer}


package object reactselect {
  type Opt[A] = A | OptGroup[A]
  implicit def readOptA[A]: Reader[Opt[A]]  = _.asInstanceOf[Opt[A]]
  implicit def writeOptA[A]: Writer[Opt[A]] = _.asInstanceOf[js.Object]

  def Opt[A](value: A): Opt[A] = value

  implicit class Opt_fold[A](self: Opt[A]) {
    def foldRaw[R](groupF: OptGroup[A] => R, optionF: A => R): R =
      if ((self: Any).isInstanceOf[OptGroup[_]])
        groupF(self.asInstanceOf[OptGroup[A]])
      else
        optionF(self.asInstanceOf[A])
  }

  implicit class AsyncResults[A](val self: Future[Seq[Opt[A]]]) extends AnyVal
  object AsyncResults {
    implicit def simple[A](fut: Future[Seq[A]]): AsyncResults[A] =
      new AsyncResults[A](fut.asInstanceOf[Future[Seq[Opt[A]]]])
  }
}
