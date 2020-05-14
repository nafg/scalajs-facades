import sbtdynver.GitDirtySuffix


ThisBuild / crossScalaVersions := Seq("2.12.11", "2.13.2")
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

lazy val simpleFacade =
  project
    .enablePlugins(ScalaJSBundlerPlugin)
    .settings(
      description := "Library for react component facades that are simple to write and simple to use",
      libraryDependencies ++= Seq(
        "com.github.japgolly.scalajs-react" %%% "extra" % "1.7.0",
        "me.shadaj" %%% "slinky-readwrite" % "0.6.5"
      )
    )

def moduleConfig(npmName: String, npmVersion: String): Project => Project =
  _.enablePlugins(ScalaJSBundlerPlugin)
    .dependsOn(simpleFacade)
    .settings(
      name := npmName.stripPrefix("@") + "_" + (npmVersion match {
        case VersionNumber(Seq(n, _, _), _, _) => n
        case s                                 => s
      }),
      description := s"Facade for $npmName version $npmVersion",
      Compile / npmDependencies += npmName -> npmVersion,
      libraryDependencies += "com.github.japgolly.scalajs-react" %%% "extra" % "1.7.0",
      addCompilerPlugin("io.tryp" % "splain" % "0.5.6" cross CrossVersion.patch),
      scalacOptions += "-P:scalajs:sjsDefinedByDefault"
    )

lazy val reactSelect = project.configure(moduleConfig("react-select", "3.1.0"))
lazy val reactInputMask = project.configure(moduleConfig("react-input-mask", "2.0.4"))
lazy val reactPhoneNumberInput = project.configure(moduleConfig("react-phone-number-input", "3.0.22"))
lazy val reactAutocomplete = project.configure(moduleConfig("react-autocomplete", "1.8.1"))
lazy val reactWidgets = project.configure(moduleConfig("react-widgets", "4.5.0"))
lazy val reactWaypoint = project.configure(moduleConfig("react-waypoint", "9.0.2"))
lazy val reactDatepicker = project.configure(moduleConfig("react-datepicker", "2.15.0"))


val materialUiVersion = "4.9.14"

lazy val materialUiCore =
  project
    .configure(moduleConfig("@material-ui/core", materialUiVersion))
    .enablePlugins(FacadeGeneratorPlugin)
    .settings(
      reactDocGenRepoUrl := "https://github.com/mui-org/material-ui.git",
      reactDocGenRepoRef := ("v" + materialUiVersion),
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
            imports =
              p.imports ++
                Set(
                  "japgolly.scalajs.react.Callback",
                  "japgolly.scalajs.react.ReactEvent",
                  "io.github.nafg.simplefacade.Implicits.callbackToWriter"
                )
          )
        case (ComponentInfo("TextField", _, _), p @ PropInfo("onChange", _, _, _, _, _, _)) =>
          p.copy(
            propTypeCode = "ReactEventFromInput => Callback",
            imports =
              p.imports ++
                Set(
                  "japgolly.scalajs.react.Callback",
                  "japgolly.scalajs.react.ReactEventFromInput",
                  "io.github.nafg.simplefacade.Implicits.callbackToWriter"
                )
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
