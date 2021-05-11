package io.github.nafg.scalajs.facades.reactselect

import scala.concurrent.Future
import scala.scalajs.js
import scala.scalajs.js.|

import japgolly.scalajs.react.vdom.VdomNode
import japgolly.scalajs.react.{Callback, ReactEventFromHtml}
import io.github.nafg.scalajs.facades.reactselect.SelectionType.{reader, writer}
import io.github.nafg.simplefacade.Implicits.{callbackToWriter, vdomNodeReader, vdomNodeWriter}
import io.github.nafg.simplefacade.{HasOpaqueReaderWriter, PropTypes}


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

trait CommonProps[A] extends PropTypes with HasOpaqueReaderWriter[A] {
  val isClearable = of[Boolean]
  val isMulti = of[Boolean]
  val closeMenuOnSelect = of[Boolean]
  val placeholder = of[String]
  val className, classNamePrefix = of[String]
  val isLoading = of[Boolean]
  val noOptionsMessage = of[HasInputValue => Option[String]]
  protected val _getOptionLabel = new PropTypes.Prop[A => String]("getOptionLabel")
  protected val _getOptionValue = new PropTypes.Prop[A => String]("getOptionValue")
  val formatGroupLabel = of[OptGroup[A] => VdomNode]
  val formatOptionLabel = of[A => VdomNode]
  val isOptionDisabled = of[A => Boolean]
  val filterOption = of[(FilterParam[A], String) => Boolean]
  protected val _onInputChange = new PropTypes.Prop[(String, InputActionMeta) => Callback]("onInputChange")
  val inputValue = of[String]
  val onMenuOpen = of[() => Callback]
  val onMenuClose = of[() => Callback]
  val onMenuScrollToBottom = of[ReactEventFromHtml => Callback]

  def getOptionLabel(f: A => String) = _getOptionLabel := f
  def getOptionValue(f: A => String) = _getOptionValue := f
  def onInputChange(f: (String, InputActionMeta) => Callback) = _onInputChange := (f(_, _))
}

trait CreatableProps[A, F[_]] extends CommonProps[A] {
  implicit val selectionType: SelectionType[F]
  val onCreateOption = of[String => Callback]
  def isValidNewOption[G[_]](implicit possible: SelectionType.Possible[F, G]) = {
    import possible.reader
    of[(String, G[A], Seq[Opt[A]]) => Boolean]
  }
  val getNewOptionData = of[(String, VdomNode) => A]

  protected def foldNew[R](isExisting: A => R, isNew: js.Dynamic => js.Dynamic): A => R = { a =>
    val raw = a.asInstanceOf[js.Dynamic]
    if (!js.isUndefined(raw.__isNew__))
      isNew(raw).asInstanceOf[R]
    else
      isExisting(a)
  }

  override def getOptionLabel(f: A => String) = super.getOptionLabel(foldNew(f, _.label))
  override def getOptionValue(f: A => String) = super.getOptionValue(foldNew(f, _.value))
}


class SelectionProps[A, F[_]](implicit val selectionType: SelectionType[F]) extends CommonProps[A] {
  val value = of[F[A]]
  val onChange = of[F[A] => Callback]
}

trait SyncOptionsProps[A] extends CommonProps[A] {
  val options = of[Seq[Opt[A]]]
}

trait AsyncOptionsProps[A] extends CommonProps[A] {
  val loadOptions = of[Option[String] => Future[Seq[Opt[A]]]]
  val defaultOptions = of[Boolean | Seq[Opt[A]]]
}
