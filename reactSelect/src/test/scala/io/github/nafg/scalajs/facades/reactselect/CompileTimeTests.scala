package io.github.nafg.scalajs.facades.reactselect

import io.github.nafg.scalajs.facades.reactselect.SelectionType.Id


object CompileTimeTests {
  Creatable[String](List(Opt("hello"), Opt("goodbye")))("hello": Id[String])()(
    _.isValidNewOption := ((_: String, _: Option[String], _: Seq[Opt[String]]) => true)
  )

  Creatable[String](List(Opt("hello"), Opt("goodbye")))(Option.empty[String])()(
    _.isValidNewOption := ((_: String, _: Option[String], _: Seq[Opt[String]]) => true)
  )

  Creatable[String](List(Opt("hello"), Opt("goodbye")))(Seq("hello"))()(
    _.isValidNewOption := ((_: String, _: Seq[String], _: Seq[Opt[String]]) => true)
  )
}
