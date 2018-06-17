package reactselectplus

import scala.concurrent.{ExecutionContext, Future}
import scala.language.implicitConversions
import scala.scalajs.js
import scala.scalajs.js.JSConverters.JSRichGenTraversableOnce
import scala.scalajs.js.annotation.{JSImport, JSName}

import japgolly.scalajs.react.vdom.VdomElement
import japgolly.scalajs.react.{Callback, CallbackTo}

import com.payalabs.scalajs.react.bridge.{JsWriter, ReactBridgeComponent}


@js.native
@JSImport("react-select-plus", JSImport.Namespace)
object ReactSelectPlusFacade extends js.Any {
  @JSName(JSImport.Default)
  val Select: js.Any = js.native

  @JSName("Async")
  val SelectAsync: js.Any = js.native
  @JSName("Creatable")
  val SelectCreatable: js.Any = js.native
  @JSName("AsyncCreatable")
  val SelectAsyncCreatable: js.Any = js.native
}

trait SelectOption[+A] extends js.Object {
  val label: String
}

abstract class DataSelectOption[A] extends SelectOption[A] {
  val value: String
  def data: A
}

abstract class GroupSelectOption[A] extends SelectOption[A] with OptionsWrapper[A]

@js.native
trait IsOptionUniqueArg[A] extends js.Object {
  val option: DataSelectOption[A]
  val options: js.Array[DataSelectOption[A]]
  val labelKey: String
  val valueKey: String
}

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

object Select extends ReactBridgeComponent {
  override protected lazy val componentValue = ReactSelectPlusFacade.Select

  def apply[A](value: Option[String], options: Seq[SelectOption[A]])
              (valueRenderer: Option[DataSelectOption[A] => VdomElement] = None,
               optionRenderer: Option[SelectOption[A] => VdomElement] = None,
               filterOption: Option[(DataSelectOption[A], String) => Boolean] = None,
               clearable: Boolean = false,
               placeholder: Option[String] = None,
               isLoading: Boolean = false,
               multi: Boolean = false,
               onClose: () => Callback,
               onOpen: () => Callback,
               onMenuScrollToBottom: () => Callback,
               onInputChange: String => CallbackTo[String])
              (onChange: js.UndefOr[DataSelectOption[A]] => Callback): VdomElement = autoNoTagModsNoChildren

  object multi {
    def apply[A](value: Seq[String], options: Seq[SelectOption[A]])
                (valueRenderer: Option[DataSelectOption[A] => VdomElement] = None,
                 optionRenderer: Option[SelectOption[A] => VdomElement] = None,
                 filterOption: Option[(DataSelectOption[A], String) => Boolean] = None,
                 clearable: Boolean = false,
                 placeholder: Option[String] = None,
                 isLoading: Boolean = false,
                 multi: Boolean = true,
                 onClose: () => Callback,
                 onOpen: () => Callback,
                 onMenuScrollToBottom: () => Callback,
                 onInputChange: String => CallbackTo[String])
                (onChange: js.Array[DataSelectOption[A]] => Callback): VdomElement = autoNoTagModsNoChildren
  }

  object Async extends ReactBridgeComponent {
    override protected lazy val componentValue = ReactSelectPlusFacade.SelectAsync

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
    override protected lazy val componentValue = ReactSelectPlusFacade.SelectCreatable

    def apply[A](value: Option[String], options: Seq[SelectOption[A]])
                (valueRenderer: Option[DataSelectOption[A] => VdomElement] = None,
                 optionRenderer: Option[SelectOption[A] => VdomElement] = None,
                 filterOption: Option[(DataSelectOption[A], String) => Boolean] = None,
                 isOptionUnique: Option[IsOptionUniqueArg[A] => Boolean] = None,
                 clearable: Boolean = false,
                 placeholder: Option[String] = None,
                 onInputChange: Option[String => Callback] = None,
                 isLoading: Boolean = false)
                (onChange: DataSelectOption[A] => Callback)
                (implicit executionContext: ExecutionContext): VdomElement = autoNoTagModsNoChildren
  }

  object AsyncCreatable extends ReactBridgeComponent {
    override protected lazy val componentValue = ReactSelectPlusFacade.SelectAsyncCreatable

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
