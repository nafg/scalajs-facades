scalaVersion in ThisBuild := "2.12.5"
organization in ThisBuild := "io.github.nafg.scalajs-facades"

publish / skip := true

def basicSettings(npmName: String, npmVersion: String) = Seq(
  name := npmName + "_" + npmVersion,
  npmDependencies in Compile += npmName -> npmVersion,
  libraryDependencies ++= Seq(
    "com.github.japgolly.scalajs-react" %%% "extra" % "1.2.0",
    "com.payalabs" %%% "scalajs-react-bridge" % "0.6.0"
  ),
  addCompilerPlugin("io.tryp" % "splain" % "0.2.10" cross CrossVersion.patch)
)


lazy val reactSelectPlus =
  project
    .enablePlugins(ScalaJSBundlerPlugin)
    .settings(
      basicSettings("react-select-plus", "1.0.0-rc.10.patch1"),
      version := "0.2.0"
    )

lazy val reactInputMask =
  project
    .enablePlugins(ScalaJSBundlerPlugin)
    .settings(
      basicSettings("react-input-mask", "1.0.7"),
      version := "0.2.0"
    )
