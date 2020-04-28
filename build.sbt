import sbtdynver.GitDirtySuffix


ThisBuild / crossScalaVersions := Seq("2.12.10", "2.13.1")
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

def basicSettings(npmName: String, npmVersion: String) = Seq(
  name := npmName.stripPrefix("@") + "_" + npmVersion,
  description := s"Facade for $npmName version $npmVersion",
  Compile / npmDependencies += npmName -> npmVersion,
  libraryDependencies ++= Seq(
    "com.github.japgolly.scalajs-react" %%% "extra" % "1.6.0",
    "com.payalabs" %%% "scalajs-react-bridge" % "0.8.2"
  ),
  addCompilerPlugin("io.tryp" % "splain" % "0.5.4" cross CrossVersion.patch),
  scalacOptions += "-P:scalajs:sjsDefinedByDefault"
)


lazy val simpleFacade =
  project
    .enablePlugins(ScalaJSBundlerPlugin)
    .settings(
      description := "Library for react component facades that are simple to write and simple to use",
      libraryDependencies ++= Seq(
        "com.github.japgolly.scalajs-react" %%% "extra" % "1.6.0",
        "com.payalabs" %%% "scalajs-react-bridge" % "0.8.2"
      ),
      scalacOptions += "-P:scalajs:sjsDefinedByDefault"
    )

lazy val reactSelect =
  project
    .enablePlugins(ScalaJSBundlerPlugin)
    .dependsOn(simpleFacade)
    .settings(basicSettings("react-select", "3.0.8"))

lazy val reactSelectPlus =
  project
    .enablePlugins(ScalaJSBundlerPlugin)
    .settings(basicSettings("react-select-plus", "1.2.0"))

lazy val reactInputMask =
  project
    .enablePlugins(ScalaJSBundlerPlugin)
    .settings(basicSettings("react-input-mask", "2.0.4"))

lazy val reactPhoneNumberInput =
  project
    .enablePlugins(ScalaJSBundlerPlugin)
    .dependsOn(simpleFacade)
    .settings(basicSettings("react-phone-number-input", "2.5.1"))


lazy val materialUiCore =
  project
    .enablePlugins(FacadeGeneratorPlugin)
    .dependsOn(simpleFacade)
    .settings(
      basicSettings("@material-ui/core", "4.9.4"),
      reactDocGenRepoUrl := "https://github.com/mui-org/material-ui.git",
      reactDocGenRepoRef := "v4.9.4",
      propInfoTransformer := {
        case (_, p @ PropInfo("classes", _, _, true, _, _, _))                                        =>
          p.copy(required = false)
        case (ComponentInfo("TablePagination", _, _), p @ PropInfo("page" |
                                                                   "count" |
                                                                   "rowsPerPage", _, _, _, _, _, _))  =>
          p.copy(propTypeCode = "Int", imports = Set())
        case (ComponentInfo("TablePagination", _, _), p @ PropInfo("onChangePage", _, _, _, _, _, _)) =>
          p.copy(
            propTypeCode = "(ReactEvent, Int) => Callback",
            imports = p.imports ++ Set("japgolly.scalajs.react.Callback", "japgolly.scalajs.react.ReactEvent")
          )
        case (ComponentInfo("TextField", _, _), p @ PropInfo("onChange", _, _, _, _, _, _)) =>
          p.copy(
            propTypeCode = "ReactEventFromInput => Callback",
            imports = p.imports ++ Set("japgolly.scalajs.react.Callback", "japgolly.scalajs.react.ReactEventFromInput")
          )
        case (_, p)                                                                                   =>
          p
      },
      componentInfoTransformer := { component =>
        val withStyleProp =
          if (component.props.exists(_.name == "style"))
            component
          else
            component.copy(props = component.props :+ PropInfo(
              name = "style",
              ident = "style",
              description = "",
              required = false,
              propType = PropType.Object,
              propTypeCode = "js.Object",
              imports = Set()
            ))
        withStyleProp.copy(props = withStyleProp.props.filterNot(_.description.trim == "@ignore"))
      },
      Compile / sourceGenerators +=
        generateReactDocGenFacades("packages/material-ui/src", "@material-ui/core", "mui"),
      Compile / sourceGenerators +=
        generateReactDocGenFacades("packages/material-ui-styles/src", "@material-ui/styles", "mui.styles"),
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
