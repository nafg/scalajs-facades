package io.github.nafg.simplefacade

import scala.language.implicitConversions
import scala.scalajs.js

import japgolly.scalajs.react.React
import japgolly.scalajs.react.vdom.all._
import japgolly.scalajs.react.vdom.{VdomElement, VdomNode}


/** Base trait providing the raw JS component reference and lazy [[Facade]] instance. */
trait FacadeModuleBase {
  def raw: js.Object
  lazy val facade = Facade(raw, Some(getClass.getName))
}

/** Base trait for facade modules with a fixed Props type.
  *
  * Choosing the right subtrait:
  *   - '''No children''': extend [[FacadeModule.Simple]]
  *   - '''VdomNode children (varargs)''': extend [[FacadeModule.NodeChildren.Simple]]
  *   - '''VdomNode children as array''' (keyed lists): extend [[FacadeModule.ArrayChildren.Simple]]
  *   - '''Typed children''' (not VdomNode): extend [[FacadeModule.ChildrenOf.Simple]][C]
  *   - '''Generic/parametric props''' (e.g., `Select[A]`): extend [[FacadeModuleP]] instead
  */
trait FacadeModule extends FacadeModuleBase {
  type Props <: PropTypes
  def mkProps: Props
  def factory: Factory[Props] = facade.factory(mkProps)
  type Setting = Factory.Setting[Props]
  def Settings(settings: Setting*): Seq[Setting] = settings
}

object FacadeModule {

  /** Component with no children. Usage: `MyComponent(_.color := "red", _.size := "large")` */
  trait Simple extends FacadeModule {
    def apply(settings: Setting*): Factory[Props] = factory(settings: _*)
  }

  /** Component with typed children of type `C`. */
  trait ChildrenOf[C] extends FacadeModule {
    type Props <: PropTypes.WithChildren[C]

    class ApplyChildren(settings: Setting*) {
      def apply(children: C): Factory[Props] = factory(settings: _*)(_.children := children)
    }
  }
  object ChildrenOf {
    trait Simple[C] extends ChildrenOf[C] {
      def apply(settings: Setting*): ApplyChildren = new ApplyChildren(settings: _*)
    }
  }

  /** Component with VdomNode children (varargs). Usage: `MyComponent(_.prop := value)(child1, child2)`
    *
    * The return value of `apply(settings*)` is an [[ApplyChildren]] which converts implicitly to `VdomElement`
    * (rendering with no children), or can be called with children: `MyComponent(_.prop := value)(child1, child2)`.
    */
  trait NodeChildren extends FacadeModule {
    type Props <: PropTypes.WithChildren[VdomNode]

    def childrenToNode(children: Seq[VdomNode]): VdomNode =
      children match {
        case Seq()    => EmptyVdom
        case Seq(one) => one
        case many     => React.Fragment(many: _*)
      }

    class ApplyChildren(settings: Setting*) {
      def apply(children: VdomNode*): Factory[Props] = factory(settings: _*)(_.children := childrenToNode(children))
    }
    object ApplyChildren                    {
      implicit def toVdomElement(applyChildren: ApplyChildren): VdomElement = applyChildren()
    }
  }
  object NodeChildren {
    trait Simple extends NodeChildren {
      def apply(settings: Setting*): ApplyChildren = new ApplyChildren(settings: _*)
    }
  }

  /** Like [[NodeChildren]] but renders children as a keyed array instead of a Fragment. */
  trait ArrayChildren extends NodeChildren {
    override def childrenToNode(children: Seq[VdomNode]) = children.toVdomArray
  }
  object ArrayChildren {
    trait Simple extends NodeChildren.Simple with ArrayChildren
  }
}

/** Like [[FacadeModule]] but for components whose Props are parametric in a type `A` (e.g., a select component generic
  * over the option type).
  */
trait FacadeModuleP extends FacadeModuleBase {
  type Props[A] <: PropTypes
  def mkProps[A]: Props[A]
  def factory[A]: Factory[Props[A]] = facade.factory(mkProps[A])
}
