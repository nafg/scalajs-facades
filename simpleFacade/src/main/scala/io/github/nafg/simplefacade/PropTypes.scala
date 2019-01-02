package io.github.nafg.simplefacade

import scala.scalajs.js

import com.payalabs.scalajs.react.bridge.JsWriter


object PropTypes {
  type Setting = (String, js.Any)

  class Prop[A](name: String)(implicit jsWriter: JsWriter[A]) {
    def :=(value: A): Setting = name -> jsWriter.toJs(value)
  }
}

trait PropTypes {
  protected def writeJsOpaque[A] = JsWriter[A](_.asInstanceOf[js.Any])

  def of[A](implicit name: sourcecode.Name, jsWriter: JsWriter[A]) = new PropTypes.Prop[A](name.value)
}
