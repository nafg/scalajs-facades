package io.github.nafg.scalajs.facades.reactselect

import scala.concurrent.Future
import scala.language.higherKinds
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.scalajs.js
import scala.scalajs.js.|

import japgolly.scalajs.react.vdom.VdomNode
import japgolly.scalajs.react.{Callback, ReactEventFromHtml}
import io.github.nafg.simplefacade.PropTypes

import com.payalabs.scalajs.react.bridge.JsWriter


@js.native
trait HasData[A] extends js.Object {
  def data: A
}
@js.native
trait FilterParam[A] extends HasData[A] {
  def label: String
  def value: String
}
@js.native
trait InputActionMeta extends js.Object {
  def action: String
}
@js.native
trait HasInputValue extends js.Object {
  def inputValue: String
}

trait CommonProps[A] extends PropTypes {
  val isClearable = of[Boolean]
  val isMulti = of[Boolean]
  val closeMenuOnSelect = of[Boolean]
  val placeholder = of[String]
  val className, classNamePrefix = of[String]
  val isLoading = of[Boolean]
  val noOptionsMessage = of[HasInputValue => Option[String]]
  protected val getOptionLabel0 = new PropTypes.Prop[js.Any => String]("getOptionLabel")
  protected val getOptionValue0 = new PropTypes.Prop[js.Any => String]("getOptionValue")
  val formatGroupLabel = of[OptGroup[A] => VdomNode]
  val formatOptionLabel = of[A => VdomNode]
  val filterOption = of[(FilterParam[A], String) => Boolean]
  val onInputChange = of[(String, InputActionMeta) => Callback]
  val onMenuOpen = of[() => Callback]
  val onMenuClose = of[() => Callback]
  val onMenuScrollToBottom = of[ReactEventFromHtml => Callback]

  def getOptionLabel(f: A => String): PropTypes.Setting = getOptionLabel0 := (x => f(x.asInstanceOf[A]))
  def getOptionValue(f: A => String): PropTypes.Setting = getOptionValue0 := (x => f(x.asInstanceOf[A]))
}

trait CreatableProps[A, F[_]] extends CommonProps[A] {
  private implicit val writeA: JsWriter[A] = writeJsOpaque[A]

  val selectionType: SelectionType[F]

  val onCreateOption = of[String => Callback]
  val isValidNewOption = of[(String, selectionType.Js[A], js.Array[Opt[A]]) => Boolean]
  val getNewOptionData = of[(String, VdomNode) => A]

  protected def foldNew[R](isExisting: A => R, isNew: js.Dynamic => R): js.Any => R = { x =>
    val raw = x.asInstanceOf[js.Dynamic]
    if (!js.isUndefined(raw.__isNew__))
      isNew(raw)
    else
      isExisting(x.asInstanceOf[A])
  }

  override def getOptionLabel(f: A => String) = getOptionLabel0 := foldNew(f, _.label.asInstanceOf[String])
  override def getOptionValue(f: A => String) = getOptionValue0 := foldNew(f, _.value.asInstanceOf[String])
}


class SelectionProps[A, F[_]](implicit val selectionType: SelectionType[F]) extends CommonProps[A] {
  private implicit val writeA: JsWriter[A] = writeJsOpaque[A]

  import selectionType.jsWriter


  val value = of[F[A]]
  private val onChange = of[selectionType.Js[A] => Callback]
  def onChange(f: F[A] => Callback): PropTypes.Setting = onChange := (f compose selectionType.fromJs)
}

trait SyncOptionsProps[A] extends CommonProps[A] {
  private implicit val writeA: JsWriter[Opt[A]] = writeJsOpaque[Opt[A]]

  val options = of[Seq[Opt[A]]]
}

trait AsyncOptionsProps[A] extends CommonProps[A] {
  private implicit val writeA: JsWriter[Opt[A]] = writeJsOpaque[Opt[A]]

  val loadOptions = of[js.UndefOr[String] => Future[Seq[Opt[A]]]]
  val defaultOptions = of[Boolean | Seq[Opt[A]]]
}
