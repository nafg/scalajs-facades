package io.github.nafg.simplefacade

import scala.scalajs.js
import japgolly.scalajs.react.vdom.html_<^._


class Tests extends munit.FunSuite {
  test("TagMod setting") {
    assertEquals(
      Factory((), null).apply(VdomAttr[String]("a") := "x").toDict.toMap[String, Any],
      Map("a" -> "x")
    )
  }
  test("PropTypes settings") {
    class MyProps extends PropTypes {
      val a = of[Int]
    }
    assertEquals(
      Factory(new MyProps, null).apply(_.a := 1).toDict.toMap[String, Any],
      Map("a" -> 1)
    )
    assertEquals(
      Factory(new MyProps, null).apply(List[(String, js.Any)]("a" -> 1, "b" -> true)).toDict.toMap[String, Any],
      Map[String, AnyVal]("a" -> 1, "b" -> true)
    )
  }

  test("MergeProps") {
    var x = 1
    val f =
      MergeProps.merge("onEvent", js.Any.fromFunction1(x += (_: Int)), js.Any.fromFunction1(x *= (_: Int)))
        .asInstanceOf[js.Function1[Int, Unit]]
    f(2)
    assertEquals(x, 6)
  }
}
