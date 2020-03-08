package io.github.nafg.simplefacade

import scala.language.implicitConversions
import scala.scalajs.js

import japgolly.scalajs.react.React
import japgolly.scalajs.react.vdom.all.EmptyVdom
import japgolly.scalajs.react.vdom.{VdomElement, VdomNode}


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

    def childrenToNode(children: Seq[VdomNode]): VdomNode = children match {
      case Seq()    => EmptyVdom
      case Seq(one) => one
      case many     => React.Fragment(many: _*)
    }

    class ApplyChildren(settings: Seq[Factory.Setting[Props]]) {
      def apply(children: VdomNode*): Factory[Props] = mkFactory(settings)(_.children := childrenToNode(children))
    }
    object ApplyChildren {
      implicit def toVdomElement(applyChildren: ApplyChildren): VdomElement = applyChildren()
    }
  }
  object NodeChildren {
    trait Simple extends NodeChildren {
      def apply(settings: Factory.Setting[Props]*): ApplyChildren = new ApplyChildren(settings)
    }
  }
}
