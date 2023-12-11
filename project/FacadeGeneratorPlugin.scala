import java.io.FileReader

import sbt.*
import sbt.Keys.*
import sbt.util.StampedFormat.UnitJsonFormat
import scalajsbundler.sbtplugin.ScalaJSBundlerPlugin
import _root_.io.circe.yaml.parser


object FacadeGeneratorPlugin extends AutoPlugin {
  object autoImport {
    val reactDocGenRepoUrl = settingKey[String]("URL of the git repository to run react-docgen on")
    val reactDocGenRepoRef = settingKey[String]("Git ref to run react-docgen in")
    val reactDocGenDir     = taskKey[os.Path]("Directory to run react-docgen inside of")
    val runYarnInstall     = taskKey[Unit]("Run yarn install in reactDocGenDir")
    val overrides          = settingKey[Overrides]("Overrides for component info")

    def generateReactDocGenFacades(subDir: String, jsPackage: String, scalaSubPackage: String) =
      Def.task {
        runYarnInstall.value
        val logger          = streams.value.log
        val overrides       =
          parser.parse(new FileReader("overrides.yml")).toTry.get
            .as[Map[String, Overrides]].toTry.get.apply(scalaSubPackage)
        FacadeGenerator.run(
          base = os.Path((Compile / sourceManaged).value),
          repoDir = reactDocGenDir.value,
          subDir = subDir,
          jsPackage = jsPackage,
          scalaSubPackage = scalaSubPackage,
          overrides = overrides,
          logger = logger
        )
      }
  }

  def cloneOrCheckoutGitRepo =
    Def.task {
      val url     = autoImport.reactDocGenRepoUrl.value
      val ref     = autoImport.reactDocGenRepoRef.value
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

  override def projectSettings =
    Seq(
      autoImport.reactDocGenDir := cloneOrCheckoutGitRepo.value,
      autoImport.runYarnInstall := {
        val dir = autoImport.reactDocGenDir.value
        FacadeGenerator.doCached[Unit](streams.value.cacheStoreFactory.make("yarn-install"), dir) {
          os.proc("yarn", "install", "--mutex", "network", "--prefer-offline")
            .call(cwd = dir, stderr = os.Inherit, stdout = os.Inherit)
        }
      },
      watchSources += file("overrides.yml"),
      Compile / packageSrc / mappings ++= {
        val base  = (Compile / sourceManaged).value
        val files = (Compile / managedSources).value
        files.map(f => (f, f.relativeTo(base).get.getPath))
      }
    )
}
