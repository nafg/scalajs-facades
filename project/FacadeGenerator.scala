import java.io.File


object FacadeGenerator {
  os.proc("yarn", "global", "add", "react-docgen")
    .call(stderr = os.Inherit, stdout = os.Inherit)

  val yarnDir = os.proc("yarn", "global", "bin").call().out.text().trim

  private def comment(str: String, indent: String) =
    if (str.trim.isEmpty) ""
    else
      (s"$indent/**" +: str.linesIterator.map(" * " + _).toSeq :+ " */").mkString("\n" + indent)

  def run(base: os.Path,
          repoDir: os.Path,
          subDir: String,
          jsPackage: String,
          scalaSubPackage: String,
          propTransformer: (ComponentInfo, PropInfo) => PropInfo = (_, p) => p,
          componentTransformer: ComponentInfo => ComponentInfo = identity): Seq[File] = {
    val scalaPackage = "io.github.nafg.scalajs.facades." + scalaSubPackage
    val outputDir = base / scalaPackage.split('.').toList

    os.makeDir.all(outputDir)

    val docGenOutputFile = os.temp(suffix = ".json")

    println(s"Writing react-docgen JSON for $subDir to $docGenOutputFile")

    val log = base / "react-docgen.log"

    os.proc(yarnDir + "/react-docgen", "-o", docGenOutputFile.toString(), subDir)
      .call(cwd = repoDir, stderr = log, stdout = log)

    val docJson = ujson.read(os.read(docGenOutputFile))

    val componentInfos = docJson.obj.values.collect {
      case value if Set("props", "displayName").forall(value.obj.contains) =>
        val info0 = ComponentInfo.read(value.obj)
        componentTransformer(info0.copy(props = info0.props.map(propTransformer(info0, _))))
    }

    val allFiles =
      for (info <- componentInfos) yield {
        val imports = info.props.flatMap(_.imports).distinct.sorted
        val outputFile = outputDir / (info.name + ".scala")

        val maybeChildrenProp = info.props.find(_.name == "children")

        val requiredNonChildrenProps = info.props.filter(i => i.required && !maybeChildrenProp.contains(i))

        val applyMethod = requiredNonChildrenProps match {
          case Seq() => ""
          case infos =>
            val paramsStr = infos.map(i => i.ident + ": " + i.propTypeCode + ",\n            ").mkString
            val settingsExpr =
              infos
                .map(i => s"_.${i.ident} := ${i.ident}")
                .mkString("Seq[Factory.Setting[Props]](", ", ", ") ++ settings: _*")
            if (maybeChildrenProp.isDefined)
              s"def apply(${paramsStr}settings: Factory.Setting[Props]*): ApplyChildren =\n" +
                s"    new ApplyChildren($settingsExpr)"
            else
              s"def apply(${paramsStr}settings: Factory.Setting[Props]*): Factory[Props] =\n" +
                s"    factory($settingsExpr)"
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
          info.props.map(p => s"${comment(p.description, "    ")}\n    val ${p.ident} = of[${p.propTypeCode}]")

        val code =
          s"""|package $scalaPackage
              |
              |${imports.map("import " + _).mkString("\n")}
              |import scala.scalajs.js
              |import scala.scalajs.js.annotation.JSImport
              |import io.github.nafg.simplefacade.{FacadeModule, ${if (applyMethod.isEmpty) "" else "Factory, "}PropTypes}
              |
              |
              |${comment(info.description, "")}
              |object ${info.name} extends $moduleParent {
              |  @JSImport("$jsPackage/${info.name}", JSImport.Default)
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
