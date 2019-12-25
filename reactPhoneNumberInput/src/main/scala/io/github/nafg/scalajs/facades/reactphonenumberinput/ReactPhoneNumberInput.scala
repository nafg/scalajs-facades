package io.github.nafg.scalajs.facades.reactphonenumberinput

import scala.scalajs.js
import scala.scalajs.js.UndefOr
import scala.scalajs.js.annotation.{JSImport, JSName}

import japgolly.scalajs.react.Callback
import io.github.nafg.simplefacade.{Facade, Factory, PropTypes}


object ReactPhoneNumberInput {
  @js.native
  @JSImport("react-phone-number-input", JSImport.Namespace)
  object raw extends js.Any {
    @JSName(JSImport.Default)
    def PhoneInput: js.Any = js.native
  }

  val facade = Facade(raw.PhoneInput)

  @js.native
  trait GetInputClassNameParam extends js.Object {
    def invalid: UndefOr[Boolean]
    def disabled: UndefOr[Boolean]
  }

  class Props extends PropTypes {
    val value = of[String]
    val onChange = of[js.UndefOr[String] => Callback]
    val autoComplete = of[String]
    val displayInitialValueAsLocalNumber = of[Boolean]
    val country = of[String]
    val countries = of[Seq[String]]
    val showCountrySelect = of[Boolean]
    val inputClassName = of[String]
    val getInputClassName = of[GetInputClassNameParam => String]
  }

  def apply(settings: Factory.Setting[Props]*): Factory[Props] = facade.factory(new Props)(settings: _*)
}
