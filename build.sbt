import sbtdynver.GitDirtySuffix


inThisBuild(List(
  organization := "io.github.nafg.scalajs-facades",
  homepage := Some(url("https://github.com/nafg/scalajs-facades")),
  licenses := List("Apache-2.0" -> url("https://www.apache.org/licenses/LICENSE-2.0")),
  developers := List(
    Developer("nafg", "Naftoli Gugenheim", "98384+nafg@users.noreply.github.com", url("https://github.com/nafg"))
  ),
  crossScalaVersions := Seq("2.12.15", "2.13.7"),
  scalaVersion := (ThisBuild / crossScalaVersions).value.last,
  scalacOptions ++= Seq(
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
  ),
  scalacOptions ++=
    (if (scalaVersion.value.startsWith("2.12."))
      List("-language:higherKinds", "-Xfuture", "-Ypartial-unification")
    else
      Nil),
  dynverGitDescribeOutput ~= (_.map(o => o.copy(dirtySuffix = GitDirtySuffix("")))),
  dynverSonatypeSnapshots := true,
  githubWorkflowJobSetup +=
    WorkflowStep.Use(UseRef.Public("actions", "setup-node", "v2"), params = Map("node-version" -> "10")),
  githubWorkflowTargetTags ++= Seq("v*"),
  githubWorkflowPublishTargetBranches := Seq(RefPredicate.StartsWith(Ref.Tag("v"))),
  githubWorkflowPublish := Seq(
    WorkflowStep.Sbt(
      List("ci-release"),
      env = Map(
        "PGP_PASSPHRASE" -> "${{ secrets.PGP_PASSPHRASE }}",
        "PGP_SECRET" -> "${{ secrets.PGP_SECRET }}",
        "SONATYPE_PASSWORD" -> "${{ secrets.SONATYPE_PASSWORD }}",
        "SONATYPE_USERNAME" -> "${{ secrets.SONATYPE_USERNAME }}"
      )
    )
  )
))

sonatypeProfileName := "io.github.nafg"

publish / skip := true

def sjsCrossTarget = crossTarget ~= (new File(_, "sjs" + scalaJSVersion))

lazy val simpleFacade =
  project
    .enablePlugins(ScalaJSBundlerPlugin)
    .settings(
      description := "Library for react component facades that are simple to write and simple to use",
      sjsCrossTarget,
      sonatypeProfileName := "io.github.nafg",
      libraryDependencies ++= Seq(
        "com.github.japgolly.scalajs-react" %%% "extra" % "1.7.7",
        "me.shadaj" %%% "slinky-readwrite" % "0.6.8"
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
      useYarn := true,
      sonatypeProfileName := "io.github.nafg",
      Compile / npmDependencies += npmName -> npmVersion,
      libraryDependencies += "com.github.japgolly.scalajs-react" %%% "extra" % "1.7.7",
      scalacOptions ++= (if (scalaJSVersion.startsWith("0.6.")) Seq("-P:scalajs:sjsDefinedByDefault") else Nil)
)

lazy val reactSelect = project.configure(moduleConfig("react-select", "4.3.1"))
lazy val reactInputMask = project.configure(moduleConfig("react-input-mask", "2.0.4"))
lazy val reactPhoneNumberInput = project.configure(moduleConfig("react-phone-number-input", "3.1.21"))
lazy val reactAutocomplete = project.configure(moduleConfig("react-autocomplete", "1.8.1"))
lazy val reactWidgets = project.configure(moduleConfig("react-widgets", "5.1.1"))
lazy val reactWaypoint = project.configure(moduleConfig("react-waypoint", "10.1.0"))
lazy val reactDatepicker = project.configure(moduleConfig("react-datepicker", "3.8.0"))


def materialUiCoreVersion = "4.11.4"

def commonPropInfoTransformer: PropInfoTransformer = {
  case (_, p @ PropInfo("classes", _, _, _, true)) =>
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
        case (ComponentInfo("ClickAwayListener" | "Tooltip", _, _), p @ PropInfo("children", _, _, _, _))             =>
          p.copy(
            required = true,
            propTypeInfo = PropTypeInfo("VdomElement", CommonImports.VdomElement)
          )
        case (ComponentInfo("ClickAwayListener", _, _), p @ PropInfo("onClickAway", _, _, _, _))                      =>
          p.copy(
            propTypeInfo = PropTypeInfo("() => Callback", CommonImports.Callback)
          )
        case (ComponentInfo("IconButton" | "ListItem", _, _), p @ PropInfo("children", _, _, _, _))                   =>
          p.copy(
            propTypeInfo = PropTypeInfo("VdomNode", CommonImports.VdomNode)
          )
        case (ComponentInfo("Menu", _, _), p @ PropInfo("anchorEl", _, _, _, _))                                      =>
          p.copy(
            propTypeInfo = PropTypeInfo("Element | (js.Object => Element)", CommonImports.Element ++ CommonImports.|)
          )
        case (ComponentInfo("Menu", _, _), p @ PropInfo("onClose", _, _, _, _))                                       =>
          p.copy(
            propTypeInfo =
              PropTypeInfo(
                "(ReactEvent, String) => Callback",
                CommonImports.ReactEvent ++ CommonImports.Callback
              )
          )
        case (ComponentInfo("Paper", _, _), p @ PropInfo("elevation", _, _, _, _))                                    =>
          p.copy(propTypeInfo = PropTypeInfo("Int"))
        case (ComponentInfo("Popper", _, _), p @ PropInfo("anchorEl", _, _, _, _))                                    =>
          p.copy(
            propTypeInfo =
              PropTypeInfo(
                "Element | js.Object | (js.Object => (Element | js.Object))",
                CommonImports.Element ++ CommonImports.|
              )
          )
        case (ComponentInfo("TablePagination", _, _), p @ PropInfo("page" | "count" | "rowsPerPage", _, _, _, _))     =>
          p.copy(propTypeInfo = PropTypeInfo("Int"))
        case (ComponentInfo("TablePagination", _, _), p @ PropInfo("onChangePage", _, _, _, _))                       =>
          p.copy(
            propTypeInfo =
              PropTypeInfo("(ReactEvent, Int) => Callback", CommonImports.Callback ++ CommonImports.ReactEvent)
          )
        case (ComponentInfo("InputBase" | "OutlinedInput" | "TextField", _, _), p @ PropInfo("onChange", _, _, _, _)) =>
          p.copy(
            propTypeInfo =
              PropTypeInfo(
                "ReactEventFromInput => Callback",
                CommonImports.Callback + CommonImports.react("ReactEventFromInput")
              )
          )
      },
      componentInfoTransformer := commonComponentInfoTransformer.andThen {
        case c if Set("Container", "TextField").contains(c.name) =>
          c.addPropIfNotExists(PropInfo("children", "VdomNode", CommonImports.VdomNode))
        case c                                                   =>
          c
      },
      componentCodeGenInfoTransformer := {
        case c @ ComponentCodeGenInfo(ComponentInfo("ButtonGroup" | "List", _, _), _, _) =>
          c.copy(moduleTrait = "FacadeModule.ArrayChildren")
      },
      Compile / sourceGenerators += generateReactDocGenFacades("packages/material-ui/src", "@material-ui/core", "mui")
    )

lazy val materialUiLab =
  project
    .configure(moduleConfig("@material-ui/lab", "4.0.0-alpha.58"))
    .enablePlugins(FacadeGeneratorPlugin)
    .settings(
      reactDocGenRepoUrl := materialUiGitUrl,
      reactDocGenRepoRef := ("v" + materialUiCoreVersion),
      propInfoTransformer := commonPropInfoTransformer.orElse {
        case (ComponentInfo("ToggleButtonGroup", _, _), p @ PropInfo("onChange", _, _, _, _)) =>
          p.copy(
            propTypeInfo =
              PropTypeInfo("(ReactEvent, js.Any) => Callback", CommonImports.Callback ++ CommonImports.ReactEvent)
          )
        case (ComponentInfo("Autocomplete", _, _), p)                                         =>
          p.name match {
            case "filterOptions"  => p.copy(propTypeInfo = PropTypeInfo("(Seq[js.Any], js.Object) => Seq[js.Any]"))
            case "getOptionLabel" => p.copy(propTypeInfo = PropTypeInfo("js.Any => String"))
            case "onChange"       =>
              p.copy(
                propTypeInfo =
                  PropTypeInfo(
                    "(ReactEvent, js.Any) => Callback",
                    CommonImports.Callback ++ CommonImports.ReactEvent
                  )
              )
            case "onInputChange"  =>
              p.copy(
                propTypeInfo =
                  PropTypeInfo(
                    "(ReactEvent, String, String) => Callback",
                    CommonImports.Callback ++ CommonImports.ReactEvent
                  )
              )
            case "renderInput"    =>
              p.copy(propTypeInfo = PropTypeInfo("js.Dictionary[js.Any] => VdomNode", CommonImports.VdomNode))
            case "renderOption"   =>
              p.copy(propTypeInfo = PropTypeInfo("js.Any => VdomNode", CommonImports.VdomNode))
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
