package io.github.nafg.simplefacade

import scala.scalajs.js


// Based loosely on https://github.com/zhangkaiyulw/react-merge-props
private[simplefacade] object MergeProps {
  private trait AnyJsFunction extends js.ThisFunction {
    def apply(_this: js.Any, args: js.Any*): js.Any
  }

  type AnyDict = js.Dictionary[js.Any]
  private def mergeChildrenArrays(value1: js.Any, value2: js.Any)               =
    if (value1.asInstanceOf[js.Array[js.Any]].length == 0) value2
    else if (value2.asInstanceOf[js.Array[js.Any]].length == 0) value1
    else
      value1
        .asInstanceOf[js.Array[js.Any]]
        .concat(value2.asInstanceOf[js.Array[js.Any]])
  private def mergeClassNames(value1: js.Any, value2: js.Any): js.Any           =
    if (value1.asInstanceOf[String] == "") value2
    else if (value2.asInstanceOf[String] == "") value1
    else value1.toString + " " + value2
  private def mergeStyleObjects(value1: js.Any, value2: js.Any)                 =
    js.Object.assign(
      js.Object(),
      value1.asInstanceOf[js.Object],
      value2.asInstanceOf[js.Object]
    )
  private def mergeEventHandlers(value1: js.Any, value2: js.Any): AnyJsFunction = {
    (_this: js.Any, args: Seq[js.Any]) =>
      value1.asInstanceOf[js.Function].call(_this, args: _*)
      value2.asInstanceOf[js.Function].call(_this, args: _*)
  }
  private def propType(value: js.Any): String = {
    val tpe = js.typeOf(value)
    if (tpe == "object" && js.Array.isArray(value)) "array"
    else tpe
  }
  def merge(key: String, value1: js.Any, value2: js.Any): js.Any                =
    if (js.isUndefined(value1))
      value2
    else if (js.isUndefined(value2))
      value1
    else {
      val tpe = propType(value2)
      if (propType(value1) != tpe)
        value2
      else
        (tpe, key) match {
          case ("array", "children")                 => mergeChildrenArrays(value1, value2)
          case ("string", "className")               => mergeClassNames(value1, value2)
          case ("object", "style")                   => mergeStyleObjects(value1, value2)
          case ("function", k) if k.startsWith("on") => mergeEventHandlers(value1, value2)
          case _                                     => value2
        }
    }
}
