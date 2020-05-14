package io.github.nafg.simplefacade

import scala.language.implicitConversions
import scala.scalajs.js

import japgolly.scalajs.react.React
import japgolly.scalajs.react.vdom.all.EmptyVdom
import japgolly.scalajs.react.vdom.{VdomElement, VdomNode}


trait FacadeModuleBase {
  def raw: js.Object
  lazy val facade = Facade(raw)
}

trait FacadeModule extends FacadeModuleBase {
  type Props <: PropTypes
  def mkProps: Props
  def factory: Factory[Props] = facade.factory(mkProps)
}

object FacadeModule {
  trait Simple extends FacadeModule {
    def apply(settings: Factory.Setting[Props]*): Factory[Props] = factory(settings: _*)
  }

  trait ChildrenOf[C] extends FacadeModule {
    type Props <: PropTypes.WithChildren[C]

    class ApplyChildren(settings: Factory.Setting[Props]*) {
      def apply(children: C): Factory[Props] = factory(settings: _*)(_.children := children)
    }
  }
  object ChildrenOf {
    trait Simple[C] extends ChildrenOf[C] {
      def apply(settings: Factory.Setting[Props]*): ApplyChildren = new ApplyChildren(settings: _*)
    }
  }

  trait NodeChildren extends FacadeModule {
    type Props <: PropTypes.WithChildren[VdomNode]

    def childrenToNode(children: Seq[VdomNode]): VdomNode = children match {
      case Seq()    => EmptyVdom
      case Seq(one) => one
      case many     => React.Fragment(many: _*)
    }

    class ApplyChildren(settings: Factory.Setting[Props]*) {
      def apply(children: VdomNode*): Factory[Props] = factory(settings: _*)(_.children := childrenToNode(children))
    }
    object ApplyChildren {
      implicit def toVdomElement(applyChildren: ApplyChildren): VdomElement = applyChildren()
    }
  }
  object NodeChildren {
    trait Simple extends NodeChildren {
      def apply(settings: Factory.Setting[Props]*): ApplyChildren = new ApplyChildren(settings: _*)
    }
  }
}

trait FacadeModuleP extends FacadeModuleBase {
  type Props[A] <: PropTypes
  def mkProps[A]: Props[A]
  def factory[A]: Factory[Props[A]] = facade.factory(mkProps[A])
}
