package io.github.nafg.simplefacade

import scala.scalajs.js


// Based loosely on https://github.com/zhangkaiyulw/react-merge-props
private[simplefacade] object MergeProps {
  private trait AnyJsFunction extends js.ThisFunction {
    def apply(_this: js.Any, args: js.Any*): js.Any
  }

  private def AnyJsFunction(f: AnyJsFunction): AnyJsFunction = f
  private def mergeChildrenArrays(value1: js.Any, value2: js.Any) =
    if (value1.asInstanceOf[js.Array[js.Any]].length == 0) value2
    else if (value2.asInstanceOf[js.Array[js.Any]].length == 0) value1
    else
      value1
        .asInstanceOf[js.Array[js.Any]]
        .concat(value2.asInstanceOf[js.Array[js.Any]])
  private def mergeClassNames(value1: js.Any, value2: js.Any): js.Any =
    if (value1.asInstanceOf[String] == "") value2
    else if (value2.asInstanceOf[String] == "") value1
    else value1.toString + " " + value2
  private def mergeStyleObjects(value1: js.Any, value2: js.Any) =
    js.Object.assign(
      js.Object(),
      value1.asInstanceOf[js.Object],
      value2.asInstanceOf[js.Object]
    )
  private def mergeEventHandlers(value1: js.Any, value2: js.Any): js.ThisFunction =
    AnyJsFunction { (_this, args) =>
      value1.asInstanceOf[js.Function].call(_this, args: _*)
      value2.asInstanceOf[js.Function].call(_this, args: _*)
    }
  private def merge(key: String, value1: js.Any, value2: js.Any): js.Any =
    if (js.isUndefined(value1))
      value2
    else if (js.isUndefined(value2))
      value1
    else if (js.typeOf(value1) != js.typeOf(value2))
      value2
    else if (js.typeOf(value2) == "array" && key == "children")
      mergeChildrenArrays(value1, value2)
    else if (js.typeOf(value2) == "string" && key == "className")
      mergeClassNames(value1, value2)
    else if (js.typeOf(value2) == "object" && key == "style")
      mergeStyleObjects(value1, value2)
    else if (js.typeOf(value2) == "function" && key.startsWith("on"))
      mergeEventHandlers(value1, value2)
    else
      value2
  def apply(propsObjects: js.Array[js.Object]): js.Object =
    if (propsObjects.length == 0)
      js.Object()
    else {
      val firstObject = propsObjects(0)
      if (propsObjects.length == 1)
        firstObject
      else {
        val firstObjectAsDict = firstObject.asInstanceOf[js.Dictionary[js.Any]]
        var isFirst = true
        for (props <- propsObjects)
          if (isFirst)
            isFirst = false
          else
            js.Object.keys(props).foreach { key =>
              val value = props.asInstanceOf[js.Dictionary[js.Any]](key)
              if (!firstObjectAsDict.contains(key))
                firstObjectAsDict(key) = value
              else {
                val baseValue = firstObjectAsDict(key)
                if (js.isUndefined(baseValue))
                  firstObjectAsDict(key) = value
                else
                  firstObjectAsDict(key) = merge(key, baseValue, value)
              }
            }
        firstObject
      }
    }
}
