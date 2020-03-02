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
        val propInfos = PropInfo.readAll(obj("props").obj - "key").sortBy(_.name)
        val imports = propInfos.flatMap(_.imports).distinct.sorted

        val outputFile = outputDir / (displayName + ".scala")

        def comment(str: String, indent: String) =
          if (str.trim.isEmpty) ""
          else
            (s"$indent/**" +: str.linesIterator.map(" * " + _).toSeq :+ " */").mkString("\n" + indent)

        val maybeChildrenProp = propInfos.find(_.name == "children")

        val requiredNonChildrenProps = propInfos.filter(i => i.required && !maybeChildrenProp.contains(i))

        val applyMethod = requiredNonChildrenProps match {
          case Seq() => ""
          case infos =>
            val paramsStr = infos.map(i => i.ident + ": " + i.propTypeCode + ",\n            ").mkString
            val settingsExpr =
              infos
                .map(i => s"_.${i.ident} := ${i.ident}")
                .mkString("Seq[Factory.Setting[Props]](", ", ", ") ++ settings")
            if (maybeChildrenProp.isDefined)
              s"def apply(${paramsStr}settings: Factory.Setting[Props]*): ApplyChildren =\n" +
                s"    new ApplyChildren($settingsExpr)"
            else
              s"def apply(${paramsStr}settings: Factory.Setting[Props]*): Factory[Props] =\n" +
                s"    mkFactory($settingsExpr)"
        }

        val (moduleTrait, moduleTraitParam) = maybeChildrenProp match {
          case Some(i) if i.propType == PropType.Node => "FacadeModule.NodeChildren" -> None
          case Some(i)                                => "FacadeModule.ChildrenOf" -> Some(i.propTypeCode)
          case _                                      => "FacadeModule" -> None
        }

        val moduleParent =
          moduleTrait + (if (applyMethod.nonEmpty) "" else ".Simple") + moduleTraitParam.fold("")("[" + _ + "]")

        val (propTypesTrait, propTypesTraitParam) =
          maybeChildrenProp
            .map(i => "PropTypes.WithChildren" -> Some(i.propTypeCode))
            .getOrElse("PropTypes" -> None)

        val propDefs =
          propInfos.map(p => s"${comment(p.description, "    ")}\n    val ${p.ident} = of[${p.propTypeCode}]")
        val code =
          s"""|package $scalaPackage
              |
              |${imports.map("import " + _).mkString("\n")}
              |import scala.scalajs.js
              |import scala.scalajs.js.annotation.JSImport
              |import io.github.nafg.simplefacade.{FacadeModule, ${if (applyMethod.isEmpty) "" else "Factory, "}PropTypes}
              |
              |
              |${comment(obj("description").str, "")}
              |object $displayName extends $moduleParent {
              |  @JSImport("$jsPackage/$displayName", JSImport.Default)
              |  @js.native
              |  object raw extends js.Object
              |
              |  override def mkProps = new Props
              |
              |  class Props extends $propTypesTrait${propTypesTraitParam.fold("")("[" + _ + "]")} {
              |${propDefs.mkString("\n")}
              |  }
              |
              |  $applyMethod
              |}
              |""".stripMargin

        os.write.over(outputFile, code)
        outputFile
      }

    allFiles.map(_.toIO).toSeq
  }
}
