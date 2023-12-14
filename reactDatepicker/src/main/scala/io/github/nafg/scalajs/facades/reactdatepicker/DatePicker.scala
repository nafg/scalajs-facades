package io.github.nafg.scalajs.facades.reactdatepicker

import java.time.LocalDate

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

import japgolly.scalajs.react.Callback
import japgolly.scalajs.react.extra.StateSnapshot
import io.github.nafg.simplefacade.Implicits.callbackToWriter
import io.github.nafg.simplefacade.{FacadeModule, Factory, PropTypes}

import slinky.readwrite.{Reader, Writer}


object DatePicker extends FacadeModule {
  @JSImport("react-datepicker", JSImport.Default)
  @js.native
  object raw extends js.Object

  class Props extends PropTypes {
    protected implicit val dateReader: Reader[LocalDate] = { o =>
      val d = o.asInstanceOf[js.Date]
      LocalDate.of(d.getFullYear().toInt, d.getMonth().toInt + 1, d.getDate().toInt)
    }
    protected implicit val dateWriter: Writer[LocalDate] =
      d => new js.Date(d.getYear, d.getMonthValue - 1, d.getDayOfMonth)

    val selected         = of[Option[LocalDate]]
    val className        = of[String]
    val isClearable      = of[Boolean]
    val showYearDropdown = of[Boolean]
    val fixedHeight      = of[Boolean]
    val placeholderText  = of[String]
    val dateFormat       = of[String]
    val onChange         = of[Option[LocalDate] => Callback]
  }

  override def mkProps = new Props

  def apply(selected: Option[LocalDate])(onChange: Option[LocalDate] => Callback): Factory[Props] =
    factory(_.selected := selected, _.onChange := onChange)

  def apply(snapshot: StateSnapshot[Option[LocalDate]]): Factory[Props] = apply(snapshot.value)(snapshot.setState)
}
