import sys.process._


ThisBuild / scalaVersion := "2.12.10"
ThisBuild / organization := "io.github.nafg.scalajs-facades"

publish / skip := true

val publishForTag = taskKey[Unit]("Publish artifacts if the project name matches the git tag prefix")

val publishForTagImpl =
  Def.taskDyn {
    val P = name.value.takeWhile(_ != '_')
    val V = version.value
    val tags = s"git tag --points-at".!!.trim.linesIterator.toList
    if (tags.exists(_.split('@') match { case Array(P, V) => true case _ => false }))
      Def.task {
        publish.value
      }
    else
      Def.task {
        streams.value.log.info(s"$P@$V not in tags ${tags.mkString("[", ",", "]")}")
      }
  }

def basicSettings(npmName: String, npmVersion: String) = Seq(
  name := npmName + "_" + npmVersion,
  npmDependencies in Compile += npmName -> npmVersion,
  libraryDependencies ++= Seq(
    "com.github.japgolly.scalajs-react" %%% "extra" % "1.4.2",
    "com.payalabs" %%% "scalajs-react-bridge" % "0.8.1"
  ),
  addCompilerPlugin("io.tryp" % "splain" % "0.5.0" cross CrossVersion.patch),
  scalacOptions += "-P:scalajs:sjsDefinedByDefault",
  publishForTag := publishForTagImpl.value
)


lazy val simpleFacade =
  project
    .enablePlugins(ScalaJSBundlerPlugin)
    .settings(
      libraryDependencies ++= Seq(
        "com.github.japgolly.scalajs-react" %%% "extra" % "1.4.2",
        "com.payalabs" %%% "scalajs-react-bridge" % "0.8.1"
      ),
      scalacOptions += "-P:scalajs:sjsDefinedByDefault",
      version := "0.6.0",
      publishForTag := publishForTagImpl.value
    )

lazy val reactSelect =
  project
    .enablePlugins(ScalaJSBundlerPlugin)
    .dependsOn(simpleFacade)
    .settings(
      basicSettings("react-select", "2.4.2"),
      version := "0.6.2"
    )

lazy val reactSelectPlus =
  project
    .enablePlugins(ScalaJSBundlerPlugin)
    .settings(
      basicSettings("react-select-plus", "1.2.0"),
      version := "0.5.0"
    )

lazy val reactInputMask =
  project
    .enablePlugins(ScalaJSBundlerPlugin)
    .settings(
      basicSettings("react-input-mask", "2.0.4"),
      version := "0.3.0"
    )

lazy val reactPhoneNumberInput =
  project
    .enablePlugins(ScalaJSBundlerPlugin)
    .dependsOn(simpleFacade)
    .settings(
      basicSettings("react-phone-number-input", "2.5.1"),
      version := "0.1.0"
    )
