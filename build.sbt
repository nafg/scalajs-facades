import sbtdynver.GitDirtySuffix


ThisBuild / crossScalaVersions := Seq("2.12.13", "2.13.5")
ThisBuild / scalaVersion := (ThisBuild / crossScalaVersions).value.last
ThisBuild / organization := "io.github.nafg.scalajs-facades"

ThisBuild / scalacOptions ++= Seq(
  "-deprecation",
  "-feature",
  "-unchecked",
  "-explaintypes",
  "-Xlint:_",
  "-Ywarn-dead-code",
  "-Ywarn-extra-implicit",
  "-Ywarn-numeric-widen",
  "-Ywarn-unused:_",
  "-Ywarn-value-discard"
)

ThisBuild / scalacOptions ++=
  (if (scalaVersion.value.startsWith("2.12."))
    List("-language:higherKinds", "-Xfuture", "-Ypartial-unification")
  else
    Nil)

ThisBuild / dynverGitDescribeOutput ~= (_.map(o => o.copy(dirtySuffix = GitDirtySuffix(""))))
ThisBuild / dynverSonatypeSnapshots := true

publish / skip := true

def sjsCrossTarget = crossTarget ~= (new File(_, "sjs" + scalaJSVersion))

lazy val simpleFacade =
  project
    .enablePlugins(ScalaJSBundlerPlugin)
    .settings(
      description := "Library for react component facades that are simple to write and simple to use",
      sjsCrossTarget,
      libraryDependencies ++= Seq(
        "com.github.japgolly.scalajs-react" %%% "extra" % "1.7.7",
        "me.shadaj" %%% "slinky-readwrite" % "0.6.7"
      )
    )

def moduleConfig(npmName: String, npmVersion: String): Project => Project =
  _.enablePlugins(ScalaJSBundlerPlugin)
    .dependsOn(simpleFacade)
    .settings(
      name := npmName.stripPrefix("@") + "_" + (npmVersion match {
        case VersionNumber(Seq(n, _, _), _, _) => n
        case s                                 => s.takeWhile(_ != '.')
      }),
      description := s"Facade for $npmName version $npmVersion",
      sjsCrossTarget,
      Compile / npmDependencies += npmName -> npmVersion,
      libraryDependencies += "com.github.japgolly.scalajs-react" %%% "extra" % "1.7.7",
      addCompilerPlugin("io.tryp" % "splain" % "0.5.8" cross CrossVersion.patch),
      scalacOptions ++= (if (scalaJSVersion.startsWith("0.6.")) Seq("-P:scalajs:sjsDefinedByDefault") else Nil)
)

lazy val reactSelect = project.configure(moduleConfig("react-select", "3.1.0"))
lazy val reactInputMask = project.configure(moduleConfig("react-input-mask", "2.0.4"))
lazy val reactPhoneNumberInput = project.configure(moduleConfig("react-phone-number-input", "3.0.25"))
lazy val reactAutocomplete = project.configure(moduleConfig("react-autocomplete", "1.8.1"))
lazy val reactWidgets = project.configure(moduleConfig("react-widgets", "4.5.0"))
lazy val reactWaypoint = project.configure(moduleConfig("react-waypoint", "9.0.3"))
lazy val reactDatepicker = project.configure(moduleConfig("react-datepicker", "3.1.3"))


def materialUiCoreVersion = "4.11.0"

def commonPropInfoTransformer: PropInfoTransformer = {
  case (_, p @ PropInfo("classes", _, _, _, true, _)) =>
    p.copy(required = false)
}

def commonComponentInfoTransformer: ComponentInfoTransformer =
  _.modProps(_.filterNot(_.description.trim == "@ignore"))
    .addPropIfNotExists(PropInfo("style", "js.Object"))
    .addPropIfNotExists(
      PropInfo(
        "onClick",
        "ReactMouseEventFromHtml => Callback",
        CommonImports.Callback + CommonImports.react("ReactMouseEventFromHtml")
      ))

def materialUiGitUrl = "https://github.com/mui-org/material-ui.git"

lazy val materialUiCore =
  project
    .configure(moduleConfig("@material-ui/core", materialUiCoreVersion))
    .enablePlugins(FacadeGeneratorPlugin)
    .settings(
      reactDocGenRepoUrl := materialUiGitUrl,
      reactDocGenRepoRef := ("v" + materialUiCoreVersion),
      propInfoTransformer := commonPropInfoTransformer.orElse {
        case (ComponentInfo("ClickAwayListener" | "Tooltip", _, _), p @ PropInfo("children", _, _, _, _, _))         =>
          p.copy(
            required = true,
            propTypeCode = "VdomElement",
            imports = CommonImports.VdomElement
          )
        case (ComponentInfo("ClickAwayListener", _, _), p @ PropInfo("onClickAway", _, _, _, _, _))                  =>
          p.copy(
            propTypeCode = "() => Callback",
            imports = CommonImports.Callback
          )
        case (ComponentInfo("IconButton" | "ListItem", _, _), p @ PropInfo("children", _, _, _, _, _))               =>
          p.copy(
            propTypeCode = "VdomNode",
            imports = CommonImports.VdomNode
          )
        case (ComponentInfo("Menu", _, _), p @ PropInfo("anchorEl", _, _, _, _, _))                                  =>
          p.copy(
            propTypeCode = "Element | (js.Object => Element)",
            imports = CommonImports.Element ++ CommonImports.|
          )
        case (ComponentInfo("Menu", _, _), p @ PropInfo("onClose", _, _, _, _, _))                                   =>
          p.copy(
            propTypeCode = "(ReactEvent, String) => Callback",
            imports = CommonImports.ReactEvent ++ CommonImports.Callback
          )
        case (ComponentInfo("Paper", _, _), p @ PropInfo("elevation", _, _, _, _, _))                                =>
          p.copy(propTypeCode = "Int")
        case (ComponentInfo("Popper", _, _), p @ PropInfo("anchorEl", _, _, _, _, _))                                =>
          p.copy(
            propTypeCode = "Element | js.Object | (js.Object => (Element | js.Object))",
            imports = CommonImports.Element ++ CommonImports.|
          )
        case (ComponentInfo("TablePagination", _, _), p @ PropInfo("page" | "count" | "rowsPerPage", _, _, _, _, _)) =>
          p.copy(propTypeCode = "Int", imports = Set())
        case (ComponentInfo("TablePagination", _, _), p @ PropInfo("onChangePage", _, _, _, _, _))                   =>
          p.copy(
            propTypeCode = "(ReactEvent, Int) => Callback",
            imports = CommonImports.Callback ++ CommonImports.ReactEvent
          )
        case (ComponentInfo("InputBase" | "OutlinedInput" | "TextField", _, _), p @ PropInfo("onChange", _, _, _, _, _))                             =>
          p.copy(
            propTypeCode = "ReactEventFromInput => Callback",
            imports = CommonImports.Callback + CommonImports.react("ReactEventFromInput")
          )
      },
      componentInfoTransformer := commonComponentInfoTransformer.andThen {
        case c if Set("Container", "TextField").contains(c.name) =>
          c.addPropIfNotExists(PropInfo("children", "VdomNode", CommonImports.VdomNode))
        case c                                                   =>
          c
      },
      componentCodeGenInfoTransformer := {
        case c @ ComponentCodeGenInfo(ComponentInfo("Accordion" | "ButtonGroup" | "List", _, _), _, _) =>
          c.copy(moduleTrait = "FacadeModule.ArrayChildren")
      },
      Compile / sourceGenerators += generateReactDocGenFacades("packages/material-ui/src", "@material-ui/core", "mui")
    )

lazy val materialUiLab =
  project
    .configure(moduleConfig("@material-ui/lab", "4.0.0-alpha.56"))
    .enablePlugins(FacadeGeneratorPlugin)
    .settings(
      reactDocGenRepoUrl := materialUiGitUrl,
      reactDocGenRepoRef := ("v" + materialUiCoreVersion),
      propInfoTransformer := commonPropInfoTransformer.orElse {
        case (ComponentInfo("ToggleButtonGroup", _, _), p @ PropInfo("onChange", _, _, _, _, _)) =>
          p.copy(
            propTypeCode = "(ReactEvent, js.Any) => Callback",
            imports = CommonImports.Callback ++ CommonImports.ReactEvent
          )
        case (ComponentInfo("Autocomplete", _, _), p)                                            =>
          p.name match {
            case "filterOptions"  => p.copy(propTypeCode = "(Seq[js.Any], js.Object) => Seq[js.Any]")
            case "getOptionLabel" => p.copy(propTypeCode = "js.Any => String")
            case "onChange"       =>
              p.copy(
                propTypeCode = "(ReactEvent, js.Any) => Callback",
                imports = CommonImports.Callback ++ CommonImports.ReactEvent
              )
            case "onInputChange"  =>
              p.copy(
                propTypeCode = "(ReactEvent, String, String) => Callback",
                imports = CommonImports.Callback ++ CommonImports.ReactEvent
              )
            case "renderInput"    =>
              p.copy(propTypeCode = "js.Dictionary[js.Any] => VdomNode", imports = CommonImports.VdomNode)
            case "renderOption"   =>
              p.copy(propTypeCode = "js.Any => VdomNode", imports = CommonImports.VdomNode)
            case _                =>
              p
          }
      },
      componentInfoTransformer := commonComponentInfoTransformer,
      componentCodeGenInfoTransformer := {
        case c if Set("Breadcrumbs", "ToggleButtonGroup").contains(c.componentInfo.name) =>
          c.copy(moduleTrait = "FacadeModule.ArrayChildren")
      },
      Compile / sourceGenerators +=
        generateReactDocGenFacades("packages/material-ui-lab/src", "@material-ui/lab", "mui.lab")
    )

val generateInstallInstructions = taskKey[Unit]("Generate install instructions in README.md")

generateInstallInstructions := {
  val info = Def.task((projectID.value, description.value, (publish / skip).value)).all(ScopeFilter(inAnyProject)).value
  val lines =
    for ((moduleId, descr, noPublish) <- info.sortBy(_._1.name) if !noPublish) yield {
      "// " + descr + "\n" +
        s"""libraryDependencies += "${moduleId.organization}" %%% "${moduleId.name}" % "${moduleId.revision}""""
    }
  val block =
    s"""|## Installation
        |<!-- Begin autogenerated via sbt generateInstallInstructions -->
        |```scala
        |resolvers += Resolver.jcenterRepo
        |${lines.mkString("\n")}
        |```
        |<!-- End autogenerated via sbt generateInstallInstructions -->
        |
        |""".stripMargin
  val readmePath = os.pwd / "README.md"
  val currentReadme = os.read.lines(readmePath)
  val (before, rest) = currentReadme.span(_.trim != "## Installation")
  val (_, after) = rest.span(s => s.trim == "## Installation" || !s.startsWith("##"))
  val newReadme = before.map(_ + "\n").mkString + block + after.map(_ + "\n").mkString
  os.write.over(readmePath, newReadme)
}
