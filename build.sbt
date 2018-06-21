import sys.process._


ThisBuild / scalaVersion := "2.12.6"
ThisBuild / organization := "io.github.nafg.scalajs-facades"

publish / skip := true

val publishForTag = taskKey[Unit]("Publish artifacts if the project name matches the git tag prefix")

val publishForTagImpl =
  Def.taskDyn {
    val P = name.value.split('_').init.mkString
    val V = version.value
    val tags = s"git tag --points-at".!!.trim.linesIterator.toList
    if (tags.exists(_.split('@') match { case Array(P, V) => true case _ => false }))
      Def.task {
        publish.value
      }
    else
      Def.task {
        ()
      }
  }

def basicSettings(npmName: String, npmVersion: String) = Seq(
  name := npmName + "_" + npmVersion,
  npmDependencies in Compile += npmName -> npmVersion,
  libraryDependencies ++= Seq(
    "com.github.japgolly.scalajs-react" %%% "extra" % "1.2.0",
    "com.payalabs" %%% "scalajs-react-bridge" % "0.6.0"
  ),
  addCompilerPlugin("io.tryp" % "splain" % "0.3.1" cross CrossVersion.patch),
  scalacOptions += "-P:scalajs:sjsDefinedByDefault",
  publishForTag := publishForTagImpl.value
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
