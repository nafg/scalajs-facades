package reactinputmask

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

import io.github.nafg.simplefacade.{FacadeModule, Factory, PropTypes}


object ReactInputMask extends FacadeModule {
  @JSImport("react-input-mask", JSImport.Default)
  @js.native
  object raw extends js.Object

  class Props extends PropTypes {
    val mask           = of[String]
    val maskChar       = of[String]
    val formatChars    = of[Map[String, String]]
    val alwaysShowMask = of[Boolean]
  }

  override def mkProps = new Props

  def apply(mask: String): Factory[Props] = factory(_.mask := mask)
}
