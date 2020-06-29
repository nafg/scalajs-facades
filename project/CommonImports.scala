object CommonImports {
  def react(s: String): String = "japgolly.scalajs.react." + s

  val | = Set("scala.scalajs.js.|")
  val Callback = Set(react("Callback"), "io.github.nafg.simplefacade.Implicits.callbackToWriter")
  val Element = Set("org.scalajs.dom.Element")
  val ElementType = Set("io.github.nafg.simplefacade.Implicits.elementTypeWriter")
  val ReactEvent = Set(react("ReactEvent"))
  val VdomElement = Set(react("vdom.VdomElement"), "io.github.nafg.simplefacade.Implicits.vdomElementWriter")
  val VdomNode = Set(react("vdom.VdomNode"), "io.github.nafg.simplefacade.Implicits.vdomNodeWriter")
}
