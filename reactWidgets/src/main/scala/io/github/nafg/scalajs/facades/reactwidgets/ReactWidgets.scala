package io.github.nafg.scalajs.facades.reactwidgets

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport
import scala.scalajs.js.|

import japgolly.scalajs.react.Callback
import japgolly.scalajs.react.extra.StateSnapshot
import io.github.nafg.simplefacade.Implicits.callbackToWriter
import io.github.nafg.simplefacade._


object ReactWidgets {
  protected type Id[A] = A

  object raw {
    @js.native
    @JSImport("react-widgets/lib/Multiselect", JSImport.Default)
    object Multiselect extends js.Object
    @js.native
    @JSImport("react-widgets/lib/Combobox", JSImport.Default)
    object Combobox extends js.Object
    @js.native
    @JSImport("react-widgets/lib/DropdownList", JSImport.Default)
    object DropdownList extends js.Object
  }

  abstract class Module[F[_]](val raw: js.Object) extends FacadeModuleP {
    trait CommonProps[A] extends PropTypes with HasOpaqueReaderWriter[A] {
      val data = of[Seq[A]]
      val textField = of[A => String]
      def value: PropTypes.Prop[F[A]]
      def onChange: PropTypes.Prop[F[A] => Callback]
    }

    override type Props[A] <: CommonProps[A]

    def apply[A](value: StateSnapshot[F[A]], data: Seq[A])(textField: A => String) =
      factory[A](_.data := data, _.textField := textField, _.value := value.value, _.onChange := value.setState)
  }

  object Multiselect extends Module[Seq](raw.Multiselect) {
    class Props[A] extends CommonProps[A] {
      override val value = of
      override val onChange = of
      val readOnly = of[Boolean]
      val busy = of[Boolean]
      val allowCreate = of[Boolean | String]
      val onCreate = of[String => Callback]
    }
    override def mkProps[A] = new Props[A]
  }

  object Combobox extends Module[Id](raw.Combobox) {
    class Props[A] extends CommonProps[A] {
      override val value = of
      override val onChange = of
      val placeholder = of[String]
      val suggest = of[Boolean]
    }
    override def mkProps[A] = new Props[A]
  }

  object DropdownList extends Module[Id](raw.DropdownList) {
    class Props[A] extends CommonProps[A] {
      override val value = of
      override val onChange = of
      val onFilter = of[Boolean]
      val allowCreate = of[Boolean | String]
      val readOnly = of[Boolean]
      val busy = of[Boolean]
      val suggest = of[Boolean]
      val onSearch = of[String => Callback]
      val searchTerm = of[String]
      val filter = of[(A, String) => Int]
      val defaultSearchTerm = of[String]
      val placeholder = of[String]
      val disabled = of[Seq[A] | Boolean]
      val onCreate = of[String => Callback]
    }
    override def mkProps[A] = new Props[A]
  }
}
