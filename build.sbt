ThisBuild / scalaVersion := "2.12.10"
ThisBuild / organization := "io.github.nafg.scalajs-facades"

publish / skip := true

def basicSettings(npmName: String, npmVersion: String) = Seq(
  name := npmName + "_" + npmVersion,
  npmDependencies in Compile += npmName -> npmVersion,
  libraryDependencies ++= Seq(
    "com.github.japgolly.scalajs-react" %%% "extra" % "1.5.0",
    "com.payalabs" %%% "scalajs-react-bridge" % "0.8.1"
  ),
  addCompilerPlugin("io.tryp" % "splain" % "0.5.0" cross CrossVersion.patch),
  scalacOptions += "-P:scalajs:sjsDefinedByDefault"
)


lazy val simpleFacade =
  project
    .enablePlugins(ScalaJSBundlerPlugin)
    .settings(
      libraryDependencies ++= Seq(
        "com.github.japgolly.scalajs-react" %%% "extra" % "1.5.0",
        "com.payalabs" %%% "scalajs-react-bridge" % "0.8.1"
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
