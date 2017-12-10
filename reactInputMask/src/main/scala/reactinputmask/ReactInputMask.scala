package reactinputmask

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

import com.payalabs.scalajs.react.bridge.{ReactBridgeComponent, WithPropsNoChildren}
import japgolly.scalajs.react._


@JSImport("react-input-mask", JSImport.Default)
@js.native
object ReactInputMaskFacade extends js.Object

object ReactInputMask extends ReactBridgeComponent {
  override protected lazy val jsComponent = JsComponent[js.Object, Children.Varargs, Null](ReactInputMaskFacade)

  def apply(mask: String,
            maskChar: Option[String] = None,
            formatChars: Option[js.Dictionary[String]] = None,
            alwaysShowMask: Boolean = false): WithPropsNoChildren = autoNoChildren
}
