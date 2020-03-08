package io.github.nafg.simplefacade

import scala.reflect.ClassTag
import scala.scalajs.js
import scala.scalajs.js.|

import japgolly.scalajs.react.raw.React.ElementType
import japgolly.scalajs.react.vdom.html_<^.VdomNode

import com.payalabs.scalajs.react.bridge.JsWriter


object Implicits {
  implicit def vdomNodeWriter: JsWriter[VdomNode] = JsWriter(_.rawNode.asInstanceOf[js.Any])
  implicit def elementTypeWriter: JsWriter[ElementType] = JsWriter(_.asInstanceOf[js.Any])
  implicit def unionWriter[A, B](implicit A: ClassTag[A],
                                 writerA: JsWriter[A],
                                 B: ClassTag[B],
                                 writerB: JsWriter[B]): JsWriter[A | B] =
    JsWriter { value =>
      if (A.runtimeClass == value.getClass)
        writerA.toJs(value.asInstanceOf[A])
      else if (B.runtimeClass == value.getClass)
        writerB.toJs(value.asInstanceOf[B])
      else
        try writerA.toJs(value.asInstanceOf[A])
        catch {
          case _: Throwable =>
            writerB.toJs(value.asInstanceOf[B])
        }
    }
}
