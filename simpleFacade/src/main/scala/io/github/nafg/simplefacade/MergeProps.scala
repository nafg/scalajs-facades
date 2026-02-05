package io.github.nafg.simplefacade

import scala.scalajs.js


/** Intelligently merges duplicate prop values when the same key is set multiple times.
  *
  * Unlike React's default behavior (last value wins), MergeProps applies type-aware merging for known prop patterns:
  *   - '''className''': concatenated with a space (e.g., `"btn" + " " + "btn-primary"`)
  *   - '''style''': deep-merged via `Object.assign` (later properties override earlier ones)
  *   - '''on*''' (event handlers): chained so both handlers fire in sequence
  *   - '''children''' (arrays): concatenated
  *   - All other props, or mismatched types: last value wins
  *
  * Based loosely on [[https://github.com/zhangkaiyulw/react-merge-props react-merge-props]].
  */
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
  def merge(key: String, value1: js.Any, value2: js.Any): js.Any                =
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
}
