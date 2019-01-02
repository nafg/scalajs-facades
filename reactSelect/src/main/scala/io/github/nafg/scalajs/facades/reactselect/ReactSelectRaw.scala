package io.github.nafg.scalajs.facades.reactselect

import scala.scalajs.js
import scala.scalajs.js.annotation.{JSImport, JSName}


@js.native
@JSImport("react-select", JSImport.Namespace)
object ReactSelectRaw extends js.Any {
  @JSName(JSImport.Default)
  val Select: js.Any = js.native
  @JSName("Async")
  val SelectAsync: js.Any = js.native
  @JSName("Creatable")
  val SelectCreatable: js.Any = js.native
  @JSName("AsyncCreatable")
  val SelectAsyncCreatable: js.Any = js.native
}
