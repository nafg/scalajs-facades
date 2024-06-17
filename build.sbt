import _root_.io.github.nafg.scalacoptions.*


def myScalacOptions(version: String) =
  ScalacOptions.all(version)(
    (opts: options.Common) =>
      opts.deprecation ++
        opts.unchecked ++
        opts.feature,
    (_: options.V2).explaintypes,
    (_: options.V2_13).Xlint("_"),
    (opts: options.V2_13_6_+) =>
      opts.WdeadCode ++
        opts.WextraImplicit ++
        opts.WnumericWiden ++
        opts.XlintUnused ++
        opts.WvalueDiscard ++
        opts.Xsource("3")
  )

inThisBuild(List(
  organization       := "io.github.nafg.scalajs-facades",
  crossScalaVersions := Seq("2.13.14", "3.3.3"),
  scalaVersion       := (ThisBuild / crossScalaVersions).value.last,
  scalacOptions ++= myScalacOptions(scalaVersion.value)
))

sonatypeProfileName := "io.github.nafg"

publish / skip := true

def sjsCrossTarget = crossTarget ~= (new File(_, "sjs" + scalaJSVersion))

lazy val simpleFacade =
  project
    .enablePlugins(ScalaJSBundlerPlugin)
    .settings(
      description         := "Library for react component facades that are simple to write and simple to use",
      sjsCrossTarget,
      sonatypeProfileName := "io.github.nafg",
      libraryDependencies ++= Seq(
        "com.github.japgolly.scalajs-react" %%% "core"             % "2.1.1",
        "com.github.japgolly.scalajs-react" %%% "extra"            % "2.1.1",
        "me.shadaj"                         %%% "slinky-readwrite" % "0.7.4",
        "org.scalameta"                     %%% "munit"            % "1.0.0" % Test
      )
    )

def moduleConfig(npmName: String, npmVersion: String): Project => Project =
  _.enablePlugins(ScalaJSBundlerPlugin)
    .dependsOn(simpleFacade)
    .settings(
      name                                 := npmName.stripPrefix("@") + "_" + (npmVersion match {
        case VersionNumber(Seq(n, _, _), _, _) => n
        case s                                 => s.takeWhile(_ != '.')
      }),
      description                          := s"Facade for $npmName version $npmVersion",
      sjsCrossTarget,
      useYarn                              := true,
      sonatypeProfileName                  := "io.github.nafg",
      Compile / npmDependencies += npmName -> npmVersion,
      scalacOptions ++= (if (scalaJSVersion.startsWith("0.6.")) Seq("-P:scalajs:sjsDefinedByDefault") else Nil)
    )

lazy val reactSelect           = project.configure(moduleConfig("react-select", "5.8.0"))
lazy val reactInputMask        = project.configure(moduleConfig("react-input-mask", "2.0.4"))
lazy val reactPhoneNumberInput = project.configure(moduleConfig("react-phone-number-input", "3.4.3"))
lazy val reactAutocomplete     = project.configure(moduleConfig("react-autocomplete", "1.8.1"))
lazy val reactWidgets          = project.configure(moduleConfig("react-widgets", "5.8.4"))
lazy val reactWaypoint         = project.configure(moduleConfig("react-waypoint", "10.3.0"))
lazy val reactDatepicker       = project.configure(moduleConfig("react-datepicker", "7.0.0"))

def materialUiCoreVersion = "5.15.3"

val emotionNpmDeps = Compile / npmDependencies ++= Seq(
  "@emotion/react"  -> "11.11.4",
  "@emotion/styled" -> "11.11.5"
)

def materialUiGitUrl = "https://github.com/mui-org/material-ui.git"

lazy val materialUiBase =
  project
    .configure(moduleConfig("@mui/base", "5.0.0-beta.40"))
    .enablePlugins(FacadeGeneratorPlugin)
    .settings(
      emotionNpmDeps,
      reactDocGenRepoUrl := materialUiGitUrl,
      reactDocGenRepoRef := ("v" + materialUiCoreVersion),
      Compile / sourceGenerators += generateReactDocGenFacades("packages/mui-base/src", "@mui/base", "mui.base")
    )

lazy val materialUiCore =
  project
    .configure(moduleConfig("@mui/material", materialUiCoreVersion))
    .enablePlugins(FacadeGeneratorPlugin)
    .settings(
      emotionNpmDeps,
      reactDocGenRepoUrl := materialUiGitUrl,
      reactDocGenRepoRef := ("v" + materialUiCoreVersion),
      Compile / sourceGenerators += generateReactDocGenFacades("packages/mui-material/src", "@mui/material", "mui")
    )

lazy val materialUiLab =
  project
    .configure(moduleConfig("@mui/lab", "5.0.0-alpha.170"))
    .enablePlugins(FacadeGeneratorPlugin)
    .settings(
      emotionNpmDeps,
      reactDocGenRepoUrl := materialUiGitUrl,
      reactDocGenRepoRef := ("v" + materialUiCoreVersion),
      Compile / sourceGenerators += generateReactDocGenFacades("packages/mui-lab/src", "@mui/lab", "mui.lab")
    )

val generateInstallInstructions = taskKey[Unit]("Generate install instructions in README.md")

generateInstallInstructions := {
  val info           =
    Def.task((projectID.value, description.value, (publish / skip).value)).all(ScopeFilter(inAnyProject)).value
  val lines          =
    for ((moduleId, description, noPublish) <- info.sortBy(_._1.name) if !noPublish) yield "// " + description + "\n" +
      s"""libraryDependencies += "${moduleId.organization}" %%% "${moduleId.name}" % "${moduleId.revision}""""
  val block          =
    s"""|## Installation
        |<!-- Begin autogenerated via sbt generateInstallInstructions -->
        |```scala
        |resolvers += Resolver.jcenterRepo
        |${lines.mkString("\n")}
        |```
        |<!-- End autogenerated via sbt generateInstallInstructions -->
        |
        |""".stripMargin
  val readmePath     = os.pwd / "README.md"
  val currentReadme  = os.read.lines(readmePath)
  val (before, rest) = currentReadme.span(_.trim != "## Installation")
  val (_, after)     = rest.span(s => s.trim == "## Installation" || !s.startsWith("##"))
  val newReadme      = before.map(_ + "\n").mkString + block + after.map(_ + "\n").mkString
  os.write.over(readmePath, newReadme)
}
commands += Command.command("testQuickAndPublishLocal")("testQuick" :: "publishLocal" :: _)
