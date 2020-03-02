package io.github.nafg.simplefacade

import scala.scalajs.js

import japgolly.scalajs.react.React
import japgolly.scalajs.react.vdom.VdomNode


trait FacadeModule {
  def raw: js.Object
  lazy val facade = Facade(raw)
  type Props <: PropTypes
  def mkProps: Props
  def mkFactory(settings: Seq[Factory.Setting[Props]]): Factory[Props] = facade.factory(mkProps)(settings: _*)
}

object FacadeModule {
  trait Simple extends FacadeModule {
    def apply(settings: Factory.Setting[Props]*): Factory[Props] = mkFactory(settings)
  }

  trait ChildrenOf[C] extends FacadeModule {
    type Props <: PropTypes.WithChildren[C]

    class ApplyChildren(settings: Seq[Factory.Setting[Props]]) {
      def apply(children: C): Factory[Props] = mkFactory(settings)(_.children := children)
    }
  }
  object ChildrenOf {
    trait Simple[C] extends ChildrenOf[C] {
      def apply(settings: Factory.Setting[Props]*): ApplyChildren = new ApplyChildren(settings)
    }
  }

  trait NodeChildren extends FacadeModule {
    type Props <: PropTypes.WithChildren[VdomNode]

    class ApplyChildren(settings: Seq[Factory.Setting[Props]]) {
      def apply(children: VdomNode*): Factory[Props] = mkFactory(settings)(_.children := React.Fragment(children: _*))
    }
  }
  object NodeChildren {
    trait Simple extends NodeChildren {
      def apply(settings: Factory.Setting[Props]*): ApplyChildren = new ApplyChildren(settings)
    }
  }
}
