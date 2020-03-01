package io.github.nafg.simplefacade

import scala.scalajs.js


trait FacadeModule {
  def raw: js.Object
  lazy val facade = Facade(raw)
  type Props <: PropTypes
  def mkProps: Props
  def apply(settings: Factory.Setting[Props]*): Factory[Props] = facade.factory(mkProps)(settings: _*)
}
