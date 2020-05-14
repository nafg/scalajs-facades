package io.github.nafg.scalajs.facades.reactautocomplete

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import io.github.nafg.simplefacade.Implicits.{callbackToWriter, vdomElementWriter}
import io.github.nafg.simplefacade.{FacadeModuleP, HasOpaqueReaderWriter, PropTypes}


object ReactAutocomplete extends FacadeModuleP {
  @JSImport("react-autocomplete", JSImport.Namespace)
  @js.native
  object raw extends js.Object

  class Props[A] extends PropTypes with HasOpaqueReaderWriter[A] {
    val items = of[Seq[A]]
    val getItemValue = of[A => String]
    val renderItem = of[(A, Boolean, js.Object) => VdomElement]
    val value = of[String]
    val onChange = of[ReactEventFromInput => Callback]
    val onSelect = of[(String, A) => Callback]
    val inputProps = of[js.Object]
    val renderInput = of[js.Dictionary[js.Any] => VdomElement]
    val wrapperStyle = of[js.Object]
    val menuStyle = of[js.Object]
  }

  override def mkProps[A] = new Props[A]

  def simple[A](items: Seq[A])
               (itemLabel: A => String, onSelect: A => Callback, onChange: String => Callback) =
    facade.factory(new Props[A])(
      _.items := items,
      _.getItemValue := itemLabel,
      _.renderItem := { (item: A, highlighted: Boolean, _) =>
        <.div(
          ^.backgroundColor := (if (highlighted) "#ddd" else "transparent"),
          itemLabel(item)
        )
      },
      _.onChange := (e => onChange(e.target.value)),
      _.onSelect := ((_, value) => onSelect(value)),
      _.inputProps := js.Dynamic.literal(className = "form-control"),
      _.renderInput := { props =>
        <.input(^.autoComplete := "never", TagMod.fn(builder => props.foreach { case (k, v) => builder.addAttr(k, v) }))
      },
      _.wrapperStyle := js.Dynamic.literal(),
      _.menuStyle := js.Dynamic.literal(
        borderRadius = "3px",
        boxShadow = "0 2px 12px rgba(0, 0, 0, 0.1)",
        background = "rgba(255, 255, 255, 0.9)",
        padding = "6px 12px",
        position = "absolute",
        overflow = "auto",
        zIndex = "1",
        top = "34px",
        left = "0",
        cursor = "pointer",
        maxHeight = "300px"
      )
    )
}
