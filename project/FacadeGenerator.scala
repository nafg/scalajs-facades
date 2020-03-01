import java.io.File

import os.Path


object FacadeGenerator {
  os.proc("yarn", "global", "add", "react-docgen")
    .call(stderr = os.Inherit, stdout = os.Inherit)

  val yarnDir = os.proc("yarn", "global", "bin").call().out.text().trim

  def run(base: Path,
          repoDir: Path,
          subDir: String,
          jsPackage: String,
          scalaSubPackage: String): Seq[File] = {
    val scalaPackage = "io.github.nafg.scalajs.facades." + scalaSubPackage
    val outputDir = base / scalaPackage.split('.').toList

    os.makeDir.all(outputDir)

    val docGenOutputFile = os.temp()

    val log = base / "react-docgen.log"

    os.proc(yarnDir + "/react-docgen", "-o", docGenOutputFile.toString(), subDir)
      .call(cwd = repoDir, stderr = log, stdout = log)

    val docJson = ujson.read(os.read(docGenOutputFile))

    val allFiles =
      for (item <- docJson.obj.values; obj = item.obj; if Set("props", "displayName").forall(obj.contains)) yield {
        val displayName = obj("displayName").str
        val propInfos = PropInfo.readAll(obj("props").obj).sortBy(_.name)
        val imports = propInfos.flatMap(_.imports).distinct.sorted

        val outputFile = outputDir / (displayName + ".scala")

        def comment(str: String, indent: String) =
          if (str.trim.isEmpty) ""
          else
            (s"$indent/**" +: str.linesIterator.map(" * " + _).toSeq :+ " */").mkString("\n" + indent)

        val propDefs =
          propInfos.map(p => s"${comment(p.description, "    ")}\n    val ${p.ident} = of[${p.propTypeCode}]")

        val code =
          s"""|package $scalaPackage
              |
              |${imports.map("import " + _).mkString("\n")}
              |import scala.scalajs.js
              |import scala.scalajs.js.annotation.JSImport
              |import io.github.nafg.simplefacade.{FacadeModule, PropTypes}
              |
              |
              |${comment(obj("description").str, "")}
              |object $displayName extends FacadeModule {
              |  @JSImport("$jsPackage/$displayName", JSImport.Default)
              |  @js.native
              |  object raw extends js.Object
              |
              |  override def mkProps = new Props
              |
              |  class Props extends PropTypes {
              |${propDefs.mkString("\n")}
              |  }
              |}
              |""".stripMargin

        os.write.over(outputFile, code)
        outputFile
      }

    allFiles.map(_.toIO).toSeq
  }
}
