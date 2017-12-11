scalaVersion in ThisBuild := "2.12.4"
organization in ThisBuild := "io.github.nafg"

def basicSettings(npmName: String, npmVersion: String) = Seq(
  name := "scalajs-facade-" + npmName.filter(_ != '-') + "_" + npmVersion,
  npmDependencies in Compile += npmName -> npmVersion,
  libraryDependencies ++= Seq(
    "com.github.japgolly.scalajs-react" %%% "extra" % "1.1.1",
    "com.payalabs" %%% "scalajs-react-bridge" % "0.4.0"
  ),
  addCompilerPlugin("io.tryp" % "splain" % "0.2.7" cross CrossVersion.patch)
)


lazy val reactSelectPlus =
  project
    .enablePlugins(ScalaJSBundlerPlugin)
    .settings(
      basicSettings("react-select-plus", "1.0.0-rc.10.patch1"),
      version := "0.1.1"
    )

lazy val reactInputMask =
  project
    .enablePlugins(ScalaJSBundlerPlugin)
    .settings(
      basicSettings("react-input-mask", "1.0.7"),
      version := "0.1.0"
    )
