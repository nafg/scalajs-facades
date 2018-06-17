ThisBuild / scalaVersion := "2.12.6"
ThisBuild / organization := "io.github.nafg.scalajs-facades"

publish / skip := true

def basicSettings(npmName: String, npmVersion: String) = Seq(
  name := npmName + "_" + npmVersion,
  npmDependencies in Compile += npmName -> npmVersion,
  libraryDependencies ++= Seq(
    "com.github.japgolly.scalajs-react" %%% "extra" % "1.2.0",
    "com.payalabs" %%% "scalajs-react-bridge" % "0.6.0"
  ),
  addCompilerPlugin("io.tryp" % "splain" % "0.3.1" cross CrossVersion.patch),
  scalacOptions += "-P:scalajs:sjsDefinedByDefault"
)


lazy val reactSelectPlus =
  project
    .enablePlugins(ScalaJSBundlerPlugin)
    .settings(
      basicSettings("react-select-plus", "1.2.0"),
      version := "0.3.0"
    )

lazy val reactInputMask =
  project
    .enablePlugins(ScalaJSBundlerPlugin)
    .settings(
      basicSettings("react-input-mask", "1.0.7"),
      version := "0.2.0"
    )
