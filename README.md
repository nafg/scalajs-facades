[![Build Status](https://travis-ci.org/nafg/scalajs-facades.svg?branch=master)](https://travis-ci.org/nafg/scalajs-facades)

# Scala.js Facades

Facades for react-select, react-input-mask, react-phone-number-input, and material-UI, plus...

## Installation
<!-- Begin autogenerated via sbt generateInstallInstructions -->
```scala
// Facade for @material-ui/core version 4.9.4
libraryDependencies += "io.github.nafg.scalajs-facades" %%% "material-ui-core_4-9-4" % "0.11.0"
// Facade for react-input-mask version 2.0.4
libraryDependencies += "io.github.nafg.scalajs-facades" %%% "react-input-mask_2-0-4" % "0.11.0"
// Facade for react-phone-number-input version 2.5.1
libraryDependencies += "io.github.nafg.scalajs-facades" %%% "react-phone-number-input_2-5-1" % "0.11.0"
// Facade for react-select-plus version 1.2.0
libraryDependencies += "io.github.nafg.scalajs-facades" %%% "react-select-plus_1-2-0" % "0.11.0"
// Facade for react-select version 3.0.8
libraryDependencies += "io.github.nafg.scalajs-facades" %%% "react-select_3-0-8" % "0.11.0"
// Library for react component facades that are simple to write and simple to use
libraryDependencies += "io.github.nafg.scalajs-facades" %%% "simplefacade" % "0.11.0"
```
<!-- End autogenerated via sbt generateInstallInstructions -->

## Simple Facade
A better way to define and use Scala.js facades for React components

Existing approaches suffer from some issues:
1. Require dealing with low-level javascript interop types like js.UndefOr and union types
2. Pass all props, necessitating everything to be optional (js.UndefOr, or Option, if you solve the first issue)
3. Reliance on macros
4. Tedious to define or to use


```scala
import io.github.nafg.simplefacade.Implicits.elementTypeWriter
import io.github.nafg.simplefacade.Implicits.vdomNodeWriter
import japgolly.scalajs.react.vdom.VdomNode
import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport
import io.github.nafg.simplefacade.{FacadeModule, PropTypes}


object Badge extends FacadeModule.NodeChildren.Simple {
  @JSImport("@material-ui/core/Badge", JSImport.Default) @js.native object raw extends js.Object

  override def mkProps = new Props

  class Props extends PropTypes {
    val anchorOrigin = of[js.Object]
    val badgeContent = of[VdomNode]
    val className = of[String]
    val classes = of[js.Object]
    val color = of[String]
    // more props
  }
}

// in some render method
Badge( _.badgeContent := "1", _.color := "secondary", _.dynamicProp(72))(
  Icon(_.children :=  "mail")
)

```

## Facade code generation using react-docgen

Currently, the Material-UI facade is generated by extracting the components and props from react-docgen.
