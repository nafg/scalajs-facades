package reactselectplus

import scala.concurrent.{ExecutionContext, Future}
import scala.language.implicitConversions
import scala.scalajs.js
import scala.scalajs.js.JSConverters.JSRichGenTraversableOnce
import scala.scalajs.js.annotation.{JSImport, ScalaJSDefined}

import com.payalabs.scalajs.react.bridge.{JsWriter, ReactBridgeComponent}
import japgolly.scalajs.react.vdom.VdomElement
import japgolly.scalajs.react.{Callback, Children, JsComponent}


@JSImport("react-select-plus", JSImport.Namespace)
@js.native
object ReactSelectPlusFacade extends js.Object {
  val Async: js.Any = js.native
  val Creatable: js.Any = js.native
  val AsyncCreatable: js.Any = js.native
}

@ScalaJSDefined
trait SelectOption[+A] extends js.Object {
  val label: String
}

@ScalaJSDefined
abstract class DataSelectOption[A] extends SelectOption[A] {
  val value: String
  def data: A
}

@ScalaJSDefined
abstract class GroupSelectOption[A] extends SelectOption[A] with OptionsWrapper[A]

object SelectOption {
  def apply[A](value0: String, data0: A): DataSelectOption[A] = apply(value0, data0, data0.toString)

  def apply[A](value0: String, data0: A, label0: String): DataSelectOption[A] =
    new DataSelectOption[A] {
      val value = value0
      val data = data0
      val label = label0
    }

  def group[A](label0: String, children: Seq[SelectOption[A]]) = new GroupSelectOption[A] {
    val label = label0
    val options = children.toJSArray
  }

  implicit class ExtensionMethods[A](self: SelectOption[A]) {
    def foldRaw[T](groupF: GroupSelectOption[A] => T, dataF: DataSelectOption[A] => T): T =
      if (self.hasOwnProperty("options")) groupF(self.asInstanceOf[GroupSelectOption[A]])
      else {
        val data = self.asInstanceOf[DataSelectOption[A]]
        dataF(data)
      }

    def fold[T](groupF: Seq[SelectOption[A]] => T, dataF: (String, A) => T): T =
      foldRaw(gso => groupF(gso.options), dso => dataF(dso.value, dso.data))
  }
}

@ScalaJSDefined
trait OptionsWrapper[A] extends js.Object {
  val options: js.Array[SelectOption[A]]
}

case class AsyncResult[+A](options: Seq[SelectOption[A]], complete: Boolean = false)
object AsyncResult {
  implicit def AsyncResultWriter[A]: JsWriter[AsyncResult[A]] =
    JsWriter(a => new OptionsWrapper[A] {
      override val options = a.options.toJSArray
      val complete = if (a.complete) js.defined(true) else js.undefined
    })
}

object Select {
  object Async extends ReactBridgeComponent {
    override protected lazy val jsComponent =
      JsComponent[js.Object, Children.Varargs, Null](ReactSelectPlusFacade.Async)

    def apply[A](value: Option[String], loadOptions: String => Future[AsyncResult[A]])
                (valueRenderer: Option[DataSelectOption[A] => VdomElement] = None,
                 optionRenderer: Option[SelectOption[A] => VdomElement] = None,
                 filterOption: Option[(DataSelectOption[A], String) => Boolean] = None,
                 clearable: Boolean = false,
                 placeholder: Option[String] = None,
                 cache: Option[js.Object] = None,
                 isLoading: Boolean = false)
                (onChange: DataSelectOption[A] => Unit)
                (implicit executionContext: ExecutionContext): VdomElement = autoNoTagModsNoChildren


    def simple[A](value: Option[String], fetch: String => Future[Seq[A]])
                 (key: A => String = (_: A).toString,
                  label: A => String = (_: A).toString,
                  valueRenderer: Option[A => VdomElement] = None,
                  optionRenderer: Option[SelectOption[A] => VdomElement] = None,
                  filterOption: Option[(A, String) => Boolean] = None,
                  clearable: Boolean = false,
                  placeholder: Option[String] = None)
                 (onChange: A => Unit)
                 (implicit executionContext: ExecutionContext) =
      apply[A](value, fetch.andThen(_.map(xs => AsyncResult(xs.map(a => SelectOption(key(a), a, label(a)))))))(
        valueRenderer = valueRenderer.map(_.compose[DataSelectOption[A]](_.data)),
        optionRenderer = optionRenderer,
        filterOption = filterOption.map(f => (dso: DataSelectOption[A], q: String) => f(dso.data, q)),
        clearable = clearable,
        placeholder = placeholder
      )(onChange = onChange.compose[DataSelectOption[A]](_.data))
  }

  object Creatable extends ReactBridgeComponent {
    override protected lazy val jsComponent =
      JsComponent[js.Object, Children.Varargs, Null](ReactSelectPlusFacade.Creatable)

    def apply[A](value: Option[String], options: Seq[SelectOption[A]])
                (valueRenderer: Option[DataSelectOption[A] => VdomElement] = None,
                 optionRenderer: Option[SelectOption[A] => VdomElement] = None,
                 filterOption: Option[(DataSelectOption[A], String) => Boolean] = None,
                 clearable: Boolean = false,
                 placeholder: Option[String] = None,
                 onInputChange: Option[String => Callback] = None,
                 isLoading: Boolean = false)
                (onChange: DataSelectOption[A] => Callback)
                (implicit executionContext: ExecutionContext): VdomElement = autoNoTagModsNoChildren
  }

  object AsyncCreatable extends ReactBridgeComponent {
    override protected lazy val jsComponent =
      JsComponent[js.Object, Children.Varargs, Null](ReactSelectPlusFacade.AsyncCreatable)

    def apply[A](value: Option[String], loadOptions: String => Future[AsyncResult[A]])
                (valueRenderer: Option[DataSelectOption[A] => VdomElement] = None,
                 optionRenderer: Option[SelectOption[A] => VdomElement] = None,
                 filterOption: Option[(DataSelectOption[A], String) => Boolean] = None,
                 clearable: Boolean = false,
                 placeholder: Option[String] = None,
                 cache: Option[js.Object] = None,
                 isLoading: Boolean = false)
                (onChange: DataSelectOption[A] => Unit)
                (onNewOptionClick: Option[SelectOption[A] => Unit] = None)
                (implicit executionContext: ExecutionContext): VdomElement = autoNoTagModsNoChildren
  }
}
