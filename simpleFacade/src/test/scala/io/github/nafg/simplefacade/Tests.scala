package io.github.nafg.simplefacade

import scala.scalajs.js
import japgolly.scalajs.react.vdom.html_<^._


class Tests extends munit.FunSuite {
  class MyProps extends PropTypes {
    val a         = of[Int]
    val b         = of[String]
    val className = of[String]
    val style     = of[js.Object]
    val enabled   = of[Boolean]
    val children  = of[js.Array[js.Any]]
  }
  def myFactory = Factory(new MyProps, null)

  class ClickProps extends PropTypes {
    val onClick = of[js.Function1[Int, Unit]]
  }

  test("TagMod setting") {
    assertEquals(
      Factory((), null).apply(VdomAttr[String]("a") := "x").toDict.toMap[String, Any],
      Map("a" -> "x")
    )
  }

  test("PropTypes settings") {
    assertEquals(
      myFactory.apply(_.a := 1).toDict.toMap[String, Any],
      Map("a" -> 1)
    )
    assertEquals(
      myFactory.apply(List[(String, js.Any)]("a" -> 1, "b" -> true)).toDict.toMap[String, Any],
      Map[String, AnyVal]("a" -> 1, "b" -> true)
    )
  }

  test("Prop :=? with Some") {
    val dict = myFactory.apply(_.a :=? Some(42)).toDict
    assertEquals(dict("a").asInstanceOf[Int], 42)
  }

  test("Prop :=? with None") {
    val dict = myFactory.apply(_.a :=? None).toDict
    assert(js.isUndefined(dict("a")))
  }

  test("Prop setRaw") {
    val raw  = js.Dynamic.literal(x = 1)
    val dict = myFactory.apply(_.a.setRaw(raw)).toDict
    assertEquals(dict("a"), raw.asInstanceOf[js.Any])
  }

  test("Boolean shorthand") {
    val dict = myFactory.apply(_.enabled).toDict
    assertEquals(dict("enabled").asInstanceOf[Boolean], true)
  }

  test("Dynamic props via dyn") {
    val dict = myFactory.apply(_.dyn.`aria-label`("Close")).toDict
    assertEquals(dict("aria-label").asInstanceOf[String], "Close")
  }

  test("Factory chaining accumulates settings") {
    val dict = myFactory.apply(_.a := 1)(_.b := "hello").toDict.toMap[String, Any]
    assertEquals(dict, Map("a" -> 1, "b" -> "hello"))
  }

  test("Duplicate className merges with space") {
    val dict = myFactory.apply(_.className := "a", _.className := "b").toDict
    assertEquals(dict("className").asInstanceOf[String], "a b")
  }

  test("Duplicate style deep merges") {
    val dict  = myFactory
      .apply(
        _.style := js.Dynamic.literal(color = "red", fontSize = "12px").asInstanceOf[js.Object],
        _.style := js.Dynamic.literal(color = "blue", margin = "4px").asInstanceOf[js.Object]
      )
      .toDict
    val style = dict("style").asInstanceOf[js.Dictionary[String]]
    assertEquals(style("color"), "blue")
    assertEquals(style("fontSize"), "12px")
    assertEquals(style("margin"), "4px")
  }

  test("Duplicate event handlers both fire") {
    var calls = List.empty[Int]
    val dict  = Factory(new ClickProps, null)
      .apply(
        _.onClick := js.Any.fromFunction1((n: Int) => calls = calls :+ n),
        _.onClick := js.Any.fromFunction1((n: Int) => calls = calls :+ (n * 10))
      )
      .toDict
    dict("onClick").asInstanceOf[js.Function1[Int, Unit]](5)
    assertEquals(calls, List(5, 50))
  }

  test("Duplicate children arrays concatenate") {
    val dict     = myFactory
      .apply(
        _.children := js.Array[js.Any]("a", "b"),
        _.children := js.Array[js.Any]("c")
      )
      .toDict
    val children = dict("children").asInstanceOf[js.Array[String]]
    assertEquals(children.toList, List("a", "b", "c"))
  }
}
