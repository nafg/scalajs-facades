package io.github.nafg.scalajs.facades.reactphonenumberinput

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

import japgolly.scalajs.react.Callback
import japgolly.scalajs.react.raw.React.ElementType
import io.github.nafg.simplefacade.Implicits.{callbackToWriter, elementTypeWriter}
import io.github.nafg.simplefacade.{FacadeModule, PropTypes}


object ReactPhoneNumberInput extends FacadeModule.Simple {
  @JSImport("react-phone-number-input", JSImport.Default)
  @js.native
  object raw extends js.Object

  trait CommonProps extends PropTypes {
    val value = of[String]
    val onChange = of[Option[String] => Callback]
    val defaultCountry = of[String]
    val inputComponent = of[ElementType]
  }

  class Props extends CommonProps {
    val autoComplete = of[String]
    val displayInitialValueAsLocalNumber = of[Boolean]
    val countries = of[Seq[String]]
    val numberInputProps = of[js.Object]
  }

  override def mkProps = new Props

  object input extends FacadeModule.Simple {
    @JSImport("react-phone-number-input/input", JSImport.Default)
    @js.native
    object raw extends js.Object

    class Props extends CommonProps {
      val useNationalFormatForDefaultCountryValue = of[Boolean]
      val country = of[String]
    }

    override def mkProps = new Props
  }
}
