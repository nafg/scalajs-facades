import java.io.File

import scala.sys.BooleanProp

import os.ProcessOutput


object FacadeGenerator {
  private def comment(str: String, indent: String) =
    if (str.trim.isEmpty) ""
    else
      (s"$indent/**" +: str.linesIterator.map(" * " + _).toSeq :+ " */").mkString("\n" + indent)

  def run(base: os.Path,
          repoDir: os.Path,
          subDir: String,
          jsPackage: String,
          scalaSubPackage: String,
          propInfoTransformer: (ComponentInfo, PropInfo) => PropInfo,
          componentInfoTransformer: ComponentInfo => ComponentInfo,
          componentCodeGenInfoTransformer: ComponentCodeGenInfo => ComponentCodeGenInfo): Seq[File] = {
    val scalaPackage = "io.github.nafg.scalajs.facades." + scalaSubPackage
    val outputDir = base / scalaPackage.split('.').toList

    os.makeDir.all(outputDir)

    val docGenOutputFile = os.temp(suffix = ".json")

    println(s"Writing react-docgen JSON for $subDir to $docGenOutputFile")

    val log: ProcessOutput =
      if (BooleanProp.keyExists("docgen.log").value)
        os.Inherit
      else
        base / "react-docgen.log"

    os.proc("yarn", "react-docgen", "--out", docGenOutputFile.toString(), "--exclude", ".*\\.test\\.js$", subDir)
      .call(cwd = repoDir, stderr = log, stdout = log)

    println()

    val docJson = ujson.read(os.read(docGenOutputFile))

    if(BooleanProp.keyExists("docgen.dump").value) {
      println("Result:")
      println(docJson.render(indent = 2))
      println()
    }
    val componentInfos = docJson.obj.values.collect {
      case value if Set("props", "displayName").forall(value.obj.contains) =>
        val info0 = ComponentInfo.read(value.obj)
        componentInfoTransformer(info0)
          .modProps(_.map(propInfoTransformer(info0, _)).sortBy(_.name))
    }

    val allFiles =
      for (info <- componentInfos) yield {
        print("Processing " + info.name + "... ")

        val imports = info.props.flatMap(_.imports).distinct.sorted
        val outputFile = outputDir / (info.name + ".scala")

        val genInfo = componentCodeGenInfoTransformer(ComponentCodeGenInfo(info))

        val requiredNonChildrenProps = info.props.filter(i => i.required && !info.maybeChildrenProp.contains(i))

        val applyMethod = requiredNonChildrenProps match {
          case Seq() => ""
          case infos =>
            val paramsStr = infos.map(i => i.ident + ": " + i.propTypeCode + ",\n            ").mkString
            val settingsExpr =
              infos
                .map(i => s"_.${i.ident} := ${i.ident}")
                .mkString("Seq[Factory.Setting[Props]](", ", ", ") ++ settings: _*")
            if (info.maybeChildrenProp.isDefined)
              s"def apply(${paramsStr}settings: Factory.Setting[Props]*): ApplyChildren =\n" +
                s"    new ApplyChildren($settingsExpr)"
            else
              s"def apply(${paramsStr}settings: Factory.Setting[Props]*): Factory[Props] =\n" +
                s"    factory($settingsExpr)"
        }

        val moduleParent =
          genInfo.moduleTrait +
            (if (applyMethod.nonEmpty) "" else ".Simple") +
            genInfo.moduleTraitParam.fold("")("[" + _ + "]")

        val (propTypesTrait, propTypesTraitParam) =
          info.maybeChildrenProp
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
              |import japgolly.scalajs.react.raw.React.ElementType
              |import io.github.nafg.simplefacade.{FacadeModule, ${if (applyMethod.isEmpty) "" else "Factory, "}PropTypes}
              |
              |
              |${comment(info.description, "")}
              |object ${info.name} extends $moduleParent {
              |  @JSImport("$jsPackage/${info.name}", JSImport.Default)
              |  @js.native
              |  object raw extends js.Object
              |
              |  def asElementType = raw.asInstanceOf[ElementType]
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
        println("wrote " + outputFile.toString())
        outputFile
      }

    allFiles.map(_.toIO).toSeq
  }
}
