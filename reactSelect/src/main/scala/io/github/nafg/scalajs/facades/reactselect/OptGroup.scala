package io.github.nafg.scalajs.facades.reactselect

import scala.scalajs.js
import scala.scalajs.js.JSConverters._


class OptGroup[A](val label: String, val options: js.Array[A]) extends js.Object
object OptGroup {
  def apply[A](label: String)(options: Seq[A]): Opt[A] = new OptGroup[A](label, options.toJSArray)
}
