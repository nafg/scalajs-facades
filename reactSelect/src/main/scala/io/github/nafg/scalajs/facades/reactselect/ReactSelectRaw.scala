package io.github.nafg.scalajs.facades.reactselect

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport


object ReactSelectRaw {
  @js.native
  @JSImport("react-select", JSImport.Default)
  object Select               extends js.Object
  @js.native
  @JSImport("react-select/async", JSImport.Default)
  object SelectAsync          extends js.Object
  @js.native
  @JSImport("react-select/creatable", JSImport.Default)
  object SelectCreatable      extends js.Object
  @js.native
  @JSImport("react-select/async-creatable", JSImport.Default)
  object SelectAsyncCreatable extends js.Object
}
