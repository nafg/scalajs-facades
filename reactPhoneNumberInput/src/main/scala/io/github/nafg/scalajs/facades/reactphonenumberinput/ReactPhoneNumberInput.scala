package io.github.nafg.scalajs.facades.reactphonenumberinput

import scala.scalajs.js
import scala.scalajs.js.UndefOr
import scala.scalajs.js.annotation.{JSImport, JSName}

import japgolly.scalajs.react.Callback
import io.github.nafg.simplefacade.Implicits.callbackToWriter
import io.github.nafg.simplefacade.{FacadeModule, PropTypes}


object ReactPhoneNumberInput extends FacadeModule.Simple {
  @js.native
  @JSImport("react-phone-number-input", JSImport.Namespace)
  private object module extends js.Any {
    @JSName(JSImport.Default)
    def PhoneInput: js.Object = js.native
  }

  override def raw = module.PhoneInput

  @js.native
  trait GetInputClassNameParam extends js.Object {
    def invalid: UndefOr[Boolean]
    def disabled: UndefOr[Boolean]
  }

  class Props extends PropTypes {
    val value = of[String]
    val onChange = of[Option[String] => Callback]
    val autoComplete = of[String]
    val displayInitialValueAsLocalNumber = of[Boolean]
    val country = of[String]
    val countries = of[Seq[String]]
    val showCountrySelect = of[Boolean]
    val inputClassName = of[String]
    val getInputClassName = of[GetInputClassNameParam => String]
  }

  override def mkProps = new Props
}
