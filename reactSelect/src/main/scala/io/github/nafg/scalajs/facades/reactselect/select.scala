package io.github.nafg.scalajs.facades.reactselect

import scala.concurrent.Future

import io.github.nafg.simplefacade.{Facade, Factory}


abstract class Base(val facade: Facade) {
  type AllProps[A, F[_]] <: SelectionProps[A, F]

  protected def mkProps[A, F[_] : SelectionType]: AllProps[A, F]

  trait WithOptionsBase[A] {
    protected def factory[F[_] : SelectionType]: Factory[AllProps[A, F]]

    def apply[F[_]](value: F[A])
                   (getOptionLabel: A => String = (_: A).toString, getOptionValue: A => String = (_: A).toString)
                   (extraProps: Factory.Setting[AllProps[A, F]]*)
                   (implicit selectionType: SelectionType[F]) =
      factory[F]
        .apply(_.value := value, _.getOptionLabel(getOptionLabel), _.getOptionValue(getOptionValue))
        .apply(selectionType.defaultProps[A] ++ extraProps: _*)
  }
}

trait SyncBase extends Base {
  override type AllProps[A, F[_]] <: SelectionProps[A, F] with SyncOptionsProps[A]

  class WithOptions[A](options: Seq[Opt[A]]) extends WithOptionsBase[A] {
    protected def factory[F[_] : SelectionType]: Factory[AllProps[A, F]] =
      facade.factory(mkProps[A, F])(_.options := options)
  }

  def apply[A](options: Seq[Opt[A]]) = new WithOptions[A](options)
}

trait AsyncBase extends Base {
  override type AllProps[A, F[_]] <: SelectionProps[A, F] with AsyncOptionsProps[A]

  class WithOptions[A](loadOptions: String => Future[Seq[Opt[A]]]) extends WithOptionsBase[A] {
    protected def factory[F[_] : SelectionType]: Factory[AllProps[A, F]] =
      facade.factory(mkProps[A, F])(_.loadOptions := (s => loadOptions(s.getOrElse(""))), _.defaultOptions := true)
  }

  def apply[A](loadOptions: String => AsyncResults[A]) = new WithOptions[A](loadOptions.andThen(_.self))
}

object Select extends Base(Facade(ReactSelectRaw.Select)) with SyncBase {
  override type AllProps[A, F[_]] = SelectionProps[A, F] with SyncOptionsProps[A]
  override protected def mkProps[A, F[_] : SelectionType] =
    new SelectionProps[A, F] with SyncOptionsProps[A]
}

object Creatable extends Base(Facade(ReactSelectRaw.SelectCreatable)) with SyncBase {
  override type AllProps[A, F[_]] = SelectionProps[A, F] with SyncOptionsProps[A] with CreatableProps[A, F]
  override protected def mkProps[A, F[_] : SelectionType] =
    new SelectionProps[A, F] with SyncOptionsProps[A] with CreatableProps[A, F]
}

object Async extends Base(Facade(ReactSelectRaw.SelectAsync)) with AsyncBase {
  override type AllProps[A, F[_]] = SelectionProps[A, F] with AsyncOptionsProps[A]
  override protected def mkProps[A, F[_] : SelectionType] =
    new SelectionProps[A, F] with AsyncOptionsProps[A]
}

object AsyncCreatable extends Base(Facade(ReactSelectRaw.SelectAsyncCreatable)) with AsyncBase {
  override type AllProps[A, F[_]] = SelectionProps[A, F] with AsyncOptionsProps[A] with CreatableProps[A, F]
  override protected def mkProps[A, F[_] : SelectionType] =
    new SelectionProps[A, F] with AsyncOptionsProps[A] with CreatableProps[A, F]
}
