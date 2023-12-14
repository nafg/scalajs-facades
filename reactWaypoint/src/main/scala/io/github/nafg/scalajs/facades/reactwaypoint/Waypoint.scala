package io.github.nafg.scalajs.facades.reactwaypoint

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

import japgolly.scalajs.react.Callback
import io.github.nafg.simplefacade.Implicits.callbackToWriter
import io.github.nafg.simplefacade.{Facade, PropTypes}


object Waypoint {
  @JSImport("react-waypoint", "Waypoint")
  @js.native
  private object raw extends js.Object

  class Props extends PropTypes {
    val bottomOffset = of[String]
    val onEnter      = of[() => Callback]
  }
  val component = Facade(raw).factory(new Props)
}
