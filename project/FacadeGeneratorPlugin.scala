import sbt.Keys._
import sbt._
import scalajsbundler.sbtplugin.ScalaJSBundlerPlugin


object FacadeGeneratorPlugin extends AutoPlugin {
  object autoImport {
    val reactDocGenRepoUrl = settingKey[String]("URL of the git repository to run react-docgen on")
    val reactDocGenRepoRef = settingKey[String]("Git ref to run react-docgen in")
    val reactDocGenDir = taskKey[os.Path]("Directory to run react-docgen inside of")
    val runYarnInstall = taskKey[Unit]("Run yarn install in reactDocGenDir")
    type PropInfoTransformer = PartialFunction[(ComponentInfo, PropInfo), PropInfo]
    type ComponentInfoTransformer = ComponentInfo => ComponentInfo
    type ComponentCodeGenInfoTransformer = PartialFunction[ComponentCodeGenInfo, ComponentCodeGenInfo]
    val propInfoTransformer = settingKey[PropInfoTransformer]("Function to post-process PropInfos")
    val componentInfoTransformer = settingKey[ComponentInfoTransformer]("Function to post-process ComponentInfos")
    val componentCodeGenInfoTransformer =
      settingKey[ComponentCodeGenInfoTransformer]("Function to post-process ComponentCodeGenInfos")

    def generateReactDocGenFacades(subDir: String, jsPackage: String, scalaSubPackage: String) = Def.task {
      runYarnInstall.value

      FacadeGenerator.run(
        base = os.Path((Compile / sourceManaged).value),
        repoDir = reactDocGenDir.value,
        subDir = subDir,
        jsPackage = jsPackage,
        scalaSubPackage = scalaSubPackage,
        propInfoTransformer = Function.untupled(propInfoTransformer.value.orElse { case (_, p) => p }),
        componentInfoTransformer = componentInfoTransformer.value,
        componentCodeGenInfoTransformer = componentCodeGenInfoTransformer.value.orElse { case i => i }
      )
    }
  }

  def cloneOrCheckoutGitRepo = Def.task {
    val url = autoImport.reactDocGenRepoUrl.value
    val ref = autoImport.reactDocGenRepoRef.value
    val repoDir = os.Path(target.value / "docgen" / url.replaceAll("(\\W|/)+", "-") / ref)
    os.makeDir.all(repoDir / os.up)
    if (!os.exists(repoDir) || os.list(repoDir).isEmpty)
      os.proc("git", "clone", "--depth=1", s"--branch=$ref", url, repoDir.toString())
        .call(stderr = os.Inherit, stdout = os.Inherit)
    else {
      os.proc("git", "clean", "-d", "--force").call(cwd = repoDir, stderr = os.Inherit, stdout = os.Inherit)
      os.proc("git", "fetch").call(cwd = repoDir, stderr = os.Inherit, stdout = os.Inherit)
      os.proc("git", "checkout", ref).call(cwd = repoDir, stderr = os.Inherit, stdout = os.Inherit)
    }
    repoDir
  }

  override def requires = ScalaJSBundlerPlugin

  override def projectSettings = Seq(
    autoImport.reactDocGenDir := cloneOrCheckoutGitRepo.value,
    autoImport.runYarnInstall := {
      os.proc("yarn", "install", "--mutex", "network")
        .call(cwd = autoImport.reactDocGenDir.value, stderr = os.Inherit, stdout = os.Inherit)
    },
    autoImport.propInfoTransformer := PartialFunction.empty,
    autoImport.componentInfoTransformer := identity,
    autoImport.componentCodeGenInfoTransformer := PartialFunction.empty,
    Compile / packageSrc / mappings ++= {
      val base = (Compile / sourceManaged).value
      val files = (Compile / managedSources).value
      files.map(f => (f, f.relativeTo(base).get.getPath))
    }
  )
}
