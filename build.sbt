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

publish / skip := true

def basicSettings(npmName: String, npmVersion: String) = Seq(
  name := npmName.stripPrefix("@") + "_" + npmVersion,
  Compile / npmDependencies += npmName -> npmVersion,
  libraryDependencies ++= Seq(
    "com.github.japgolly.scalajs-react" %%% "extra" % "1.6.0",
    "com.payalabs" %%% "scalajs-react-bridge" % "0.8.2"
  ),
  addCompilerPlugin("io.tryp" % "splain" % "0.5.1" cross CrossVersion.patch),
  scalacOptions += "-P:scalajs:sjsDefinedByDefault"
)


lazy val simpleFacade =
  project
    .enablePlugins(ScalaJSBundlerPlugin)
    .settings(
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
      Compile / sourceGenerators +=
        generateReactDocGenFacades("packages/material-ui/src", "@material-ui/core", "mui"),
      Compile / sourceGenerators +=
        generateReactDocGenFacades("packages/material-ui-styles/src", "@material-ui/styles", "mui.styles"),
      Compile / sourceGenerators +=
        generateReactDocGenFacades("packages/material-ui-lab/src", "@material-ui/lab", "mui.lab")
    )
