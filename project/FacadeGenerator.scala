import java.io.File

import scala.sys.BooleanProp

import cats.implicits.*
import io.circe.generic.extras.Configuration.kebabCaseTransformation
import os.{Path, ProcessOutput}
import sbt.Cache
import sbt.util.CacheImplicits.*
import sbt.util.CacheStore
import sjsonnew.{IsoString, JsonFormat}


object FacadeGenerator {
  implicit val isoStringPath: IsoString[Path] = IsoString.iso[os.Path](_.toString(), os.Path(_))
  private def comment(str: String, indent: String) =
    str.trim.linesIterator.toVector match {
      case Seq()        => ""
      case init :+ last =>
        def docComment(ls: Seq[String]) =
          (s"$indent/**" +: ls.map(" * " + _) :+ " */").mkString("\n" + indent)
        if (last.trim.startsWith("@deprecated "))
          docComment(init) + "\n" + indent + "@deprecated(\"" + last.trim.stripPrefix("@deprecated ") + "\", \"\")"
        else
          docComment(init :+ last)
    }
  def doCached[A: JsonFormat](cacheStore: CacheStore, dir: os.Path, keyExtra: String = "")(f: => A): A = {
    val committedState = os.proc("git", "describe", "--tags", "--dirty").call(cwd = dir).out.trim()
    val modifiedState = os.proc("git", "diff", "HEAD").call(cwd = dir).out.text()
    val key =
      dir + "\n" +
        committedState + "\n" +
        modifiedState + "\n" +
        keyExtra
    val cache = Cache.cached[String, A](cacheStore)(_ => f)
    cache(key)
  }
  private def runReactDocGen(base: Path, repoDir: Path, subDir: String) =
    doCached(CacheStore((base / "docgen-cache.json").toIO), repoDir, subDir) {
      val docGenOutputFile = base / "react-docgen.json"
      println(s"Writing react-docgen JSON for $subDir to $docGenOutputFile")
      val log: ProcessOutput =
        if (BooleanProp.keyExists("docgen.log").value)
          os.Inherit
        else
          base / "react-docgen.log"
      os.proc("yarn", "react-docgen", "--out", docGenOutputFile.toString(), "--exclude", ".*\\.test\\.js$", subDir)
        .call(cwd = repoDir, stderr = log, stdout = log)
      println()
      os.read(docGenOutputFile)
    }
  private def processComponent(info: ComponentInfo,
                               scalaPackage: String,
                               jsPackage: String,
                               outputFile: os.Path,
                               componentCodeGenInfoTransformer: ComponentCodeGenInfo => ComponentCodeGenInfo) = {
    print("Processing " + info.name + "... ")
    val imports = info.props.flatMap(_.propTypeInfo.imports).distinct.sorted
    val genInfo = componentCodeGenInfoTransformer(ComponentCodeGenInfo(info))
    val requiredNonChildrenProps = info.props.filter(i => i.required && !info.maybeChildrenProp.contains(i))
    def method(name: String, params: Seq[(String, String)], resultType: String)(body: String) =
      s"  def $name(${params.map { case (name, typ) => s"$name: $typ" }.mkString(", ")}): $resultType =\n" +
        s"    $body"
    val factoryMethods = requiredNonChildrenProps match {
      case Seq() => ""
      case infos =>
        def mkParams(propInfos: Seq[PropInfo]): Seq[(String, String)] =
          propInfos.map(i => i.ident.toString -> i.propTypeInfo.code)
        def mkSettingsExprs[A](xs: Seq[A])(ident: A => Identifier, code: A => String) =
          xs.map(x => s"_.${ident(x)} := ${code(x)}")
        val settingsVarArgParam = "settings" -> "Setting*"
        def mkFactoryMethod(name: String, params: Seq[(String, String)])(settingsExprs: Seq[String]) = {
          val settingsExpr = settingsExprs.mkString("Seq[Setting](", ", ", ") ++ settings: _*")
          if (info.maybeChildrenProp.isDefined)
            method(name, params :+ settingsVarArgParam, "ApplyChildren")(s"new ApplyChildren($settingsExpr)")
          else
            method(name, params :+ settingsVarArgParam, "Factory[Props]")(s"factory($settingsExpr)")
        }
        val applyMethod =
          mkFactoryMethod("apply", mkParams(infos))(
            mkSettingsExprs(infos)(_.ident, _.ident.toString)
          )
        val (withPresets, others) = infos.partition(_.propTypeInfo.presets.nonEmpty)
        val presetMethods =
          if (withPresets.isEmpty) Nil
          else
            withPresets.toList.traverse(_.propTypeInfo.presets.toList).map {
              case first +: rest =>
                val name = first.name.value + rest.map(_.name.value.capitalize).mkString
                mkFactoryMethod(name, mkParams(others))(
                  mkSettingsExprs(infos.zip(first +: rest))(_._1.ident, _._2.code) ++
                    mkSettingsExprs(others)(_.ident, _.ident.toString)
                )
            }
        (applyMethod +: presetMethods).mkString("\n\n")
    }
    val moduleParent =
      genInfo.moduleTrait +
        (if (factoryMethods.nonEmpty) "" else ".Simple") +
        genInfo.moduleTraitParam.fold("")("[" + _ + "]")
    val (propTypesTrait, propTypesTraitParam) =
      info.maybeChildrenProp
        .map(i => "PropTypes.WithChildren" -> Some(i.propTypeInfo.code))
        .getOrElse("PropTypes" -> None)
    val propDefs =
      info.props.map { case PropInfo(_, ident, PropTypeInfo(code, _, presets), description, _) =>
        s"${comment(description, "    ")}\n" +
          (if (presets.isEmpty)
            s"def $ident = of[$code]"
          else {
            s"""object $ident extends PropTypes.Prop[$code]("$ident") {
               |${
              presets
                .map { case PropTypeInfo.Preset(name, code) => s"  val $name = this := $code" }
                .mkString("\n")
            }
               |}""".stripMargin
          })
            .linesWithSeparators.map("    " + _).mkString
      }
    val componentDescription =
      s"View original docs online: https://v4.mui.com/api/${kebabCaseTransformation(info.name)}/\n\n" +
        info.description
    val code =
      s"""|package $scalaPackage
          |
          |${imports.map("import " + _).mkString("\n")}
          |import scala.scalajs.js
          |import scala.scalajs.js.annotation.JSImport
          |import japgolly.scalajs.react.facade.React.ElementType
          |import io.github.nafg.simplefacade.{FacadeModule, ${if (factoryMethods.isEmpty || info.maybeChildrenProp.isDefined) "" else "Factory, "}PropTypes}
          |
          |
          |${comment(componentDescription, "")}
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
          |$factoryMethods
          |}
          |""".stripMargin
    os.write.over(outputFile, code)
    println("wrote " + outputFile.toString())
    outputFile
  }

  private case class ProcessComponentsInfo(componentInfos: Seq[ComponentInfo],
                                           scalaPackage: String,
                                           jsPackage: String,
                                           outputDir: os.Path)

  private implicit val processComponentsInfoJF: JsonFormat[ProcessComponentsInfo] =
    caseClass(ProcessComponentsInfo.apply _, ProcessComponentsInfo.unapply _)(
      "componentInfos",
      "scalaPackage",
      "jsPackage",
      "outputDir"
    )
  private def processComponents(base: os.Path,
                                info: ProcessComponentsInfo,
                                componentCodeGenInfoTransformer: ComponentCodeGenInfo => ComponentCodeGenInfo): Seq[os.Path] = {
    val cacheFile = base / "component-info-cache.json"
    val componentInfoCache =
      Cache.cached[ProcessComponentsInfo, Seq[Path]](cacheFile.toIO) {
        case ProcessComponentsInfo(componentInfos, scalaPackage, jsPackage, outputDir) =>
          for (info <- componentInfos) yield {
            val outputFile = outputDir / (info.name + ".scala")
            processComponent(info, scalaPackage, jsPackage, outputFile, componentCodeGenInfoTransformer)
          }
      }
    componentInfoCache(info)
  }
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
    val docgenOutput = runReactDocGen(base, repoDir, subDir)
    if (BooleanProp.keyExists("docgen.dump").value) {
      println("Result:")
      println(ujson.read(docgenOutput).render(indent = 2))
      println()
    }
    val componentInfos = ujson.read(docgenOutput).obj.values.collect {
      case value if Set("props", "displayName").forall(value.obj.contains) =>
        val info0 = ComponentInfo.read(value.obj)
        componentInfoTransformer(info0)
          .modProps(_.map(propInfoTransformer(info0, _)).sortBy(_.name))
    }
    val processComponentsInfo = ProcessComponentsInfo(componentInfos.toSeq, scalaPackage, jsPackage, outputDir)
    processComponents(base, processComponentsInfo, componentCodeGenInfoTransformer)
      .map(_.toIO)
  }
}
