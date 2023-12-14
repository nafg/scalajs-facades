import java.io.File

import scala.sys.BooleanProp

import cats.implicits.*
import io.circe.generic.extras.Configuration.kebabCaseTransformation
import os.{Path, ProcessOutput}
import sbt.Cache
import sbt.util.CacheImplicits.*
import sbt.util.{CacheStore, Logger}
import sjsonnew.{IsoString, JsonFormat}


object FacadeGenerator {
  implicit val isoStringPath: IsoString[Path]                                                          =
    IsoString.iso[os.Path](_.toString(), os.Path(_))
  private def comment(str: String, indent: String)                                                     =
    str.trim.linesIterator.toVector match {
      case Seq()        => ""
      case init :+ last =>
        def docComment(ls: Seq[String]) = (s"$indent/**" +: ls.map(" * " + _) :+ " */").mkString("\n" + indent)
        if (last.trim.startsWith("@deprecated "))
          docComment(init) + "\n" + indent + "@deprecated(\"" + last.trim.stripPrefix("@deprecated ") + "\", \"\")"
        else
          docComment(init :+ last)
    }
  def doCached[A: JsonFormat](cacheStore: CacheStore, dir: os.Path, keyExtra: String = "")(f: => A): A = {
    val committedState = os.proc("git", "describe", "--tags", "--dirty").call(cwd = dir).out.trim()
    val modifiedState  = os.proc("git", "diff", "HEAD").call(cwd = dir).out.text()
    val key            =
      dir + "\n" +
        committedState + "\n" +
        modifiedState + "\n" +
        keyExtra
    val cache          = Cache.cached[String, A](cacheStore)(_ => f)
    cache(key)
  }

  private def runReactDocGen(base: Path, repoDir: Path, subDir: String) =
    doCached(CacheStore((base / "docgen-cache.json").toIO), repoDir, subDir) {
      val docGenOutputFile   = base / "react-docgen.json"
      println(s"Writing react-docgen JSON for $subDir to $docGenOutputFile")
      def logFile            = base / "react-docgen.log"
      val logToFile          = BooleanProp.keyExists("docgen.log").value
      val log: ProcessOutput =
        if (logToFile)
          os.Inherit
        else {
          println(s"Writing react-docgen log for $subDir to $logFile")
          logFile
        }
      val res                =
        os.proc(
          "yarn",
          "react-docgen",
          "--out",
          docGenOutputFile.toString(),
          "--exclude",
          ".*\\.test\\.js$",
          "-x",
          "js",
          "-x",
          "tsx",
          subDir
        )
          .call(cwd = repoDir, stderr = log, stdout = log, check = false)
      if (res.exitCode != 0) {
        if (logToFile)
          println(os.read(logFile))
        throw new Exception(s"react-docgen failed with exit code ${res.exitCode}")
      }
      println()
      os.read(docGenOutputFile)
    }

  private def processComponent(
    info: ComponentInfo,
    scalaPackage: String,
    jsPackage: String,
    outputFile: os.Path,
    moduleTraitsOverride: Option[String],
    logger: Logger
  ) = {
    logger.debug("Processing " + info.name + "... ")
    val imports                  = info.props.flatMap(_.`type`.imports).distinct.sorted
    val genInfo                  =
      moduleTraitsOverride.foldLeft(ComponentCodeGenInfo(info)) { (info, moduleTrait) =>
        info.copy(moduleTrait = moduleTrait)
      }
    val requiredNonChildrenProps = info.props.filter(i => i.required && !info.maybeChildrenProp.contains(i))

    def method(name: String, params: Seq[(String, String)], resultType: String)(body: String) =
      s"  def $name(${params.map { case (name, typ) => s"$name: $typ" }.mkString(", ")}): $resultType =\n" +
        s"    $body"

    val factoryMethods                        = requiredNonChildrenProps match {
      case Seq() => ""
      case infos =>
        def mkParams(propInfos: Seq[PropInfo]): Seq[(String, String)]                 =
          propInfos.map(i => i.identifier.toString -> i.`type`.code)
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

        val applyMethod           =
          mkFactoryMethod("apply", mkParams(infos))(
            mkSettingsExprs(infos)(_.identifier, _.identifier.toString)
          )

        val (withPresets, others) = infos.partition(_.`type`.presets.nonEmpty)

        val presetMethods         =
          if (withPresets.isEmpty) Nil
          else
            withPresets.toList.traverse(_.`type`.presets.toList).map {
              case first +: rest =>
                val name = first.name.value + rest.map(_.name.value.capitalize).mkString
                mkFactoryMethod(name, mkParams(others))(
                  mkSettingsExprs(infos.zip(first +: rest))(_._1.identifier, _._2.code) ++
                    mkSettingsExprs(others)(_.identifier, _.identifier.toString)
                )
            }
        (applyMethod +: presetMethods).mkString("\n\n")
    }

    val moduleParent                          =
      genInfo.moduleTrait +
        (if (factoryMethods.nonEmpty) "" else ".Simple") +
        genInfo.moduleTraitParam.fold("")("[" + _ + "]")

    val (propTypesTrait, propTypesTraitParam) =
      info.maybeChildrenProp
        .map(i => "PropTypes.WithChildren" -> Some(i.`type`.code))
        .getOrElse("PropTypes" -> None)

    val propDefs                              =
      info.props.map { propInfo =>
        def typeCode = propInfo.`type`.code
        s"${comment(propInfo.description, "    ")}\n" +
          (if (propInfo.`type`.presets.isEmpty)
             s"def ${propInfo.identifier} = of[$typeCode]"
           else
             s"""object ${propInfo.identifier} extends PropTypes.Prop[$typeCode]("${propInfo.identifier}") {
               |${
                 propInfo.`type`.presets
                   .map { case PropTypeInfo.Preset(name, presetCode) =>
                     s"  val $name = this := $presetCode.asInstanceOf[$typeCode]"
                   }
                   .mkString("\n")
               }
               |}""".stripMargin)
            .linesWithSeparators.map("    " + _).mkString
      }

    val componentDescription                  =
      s"View original docs online: https://mui.com/api/${kebabCaseTransformation(info.name)}/\n\n" +
        info.description
    val factoryImportPart                     =
      if (factoryMethods.isEmpty || info.maybeChildrenProp.isDefined) "" else "Factory, "
    val code                                  =
      s"""|package $scalaPackage
          |
          |${imports.map("import " + _).mkString("\n")}
          |import scala.scalajs.js
          |import scala.scalajs.js.annotation.JSImport
          |import japgolly.scalajs.react.facade.React.ElementType
          |import io.github.nafg.simplefacade.{FacadeModule, ${factoryImportPart}PropTypes}
          |
          |
          |${comment(componentDescription, "")}
          |object ${info.name} extends $moduleParent {
          |  @JSImport("$jsPackage", "${info.name}")
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
    logger.debug("Wrote " + outputFile.toString())
    outputFile
  }

  def run(
    base: os.Path,
    repoDir: os.Path,
    subDir: String,
    jsPackage: String,
    scalaSubPackage: String,
    overrides: Overrides,
    logger: Logger
  ): Seq[File] = {
    val scalaPackage = "io.github.nafg.scalajs.facades." + scalaSubPackage
    val outputDir    = base / scalaPackage.split('.').toList
    os.makeDir.all(outputDir)
    val docgenOutput = runReactDocGen(base, repoDir, subDir)
    if (BooleanProp.keyExists("docgen.dump").value) {
      println("Result:")
      println(ujson.read(docgenOutput).render(indent = 2))
      println()
    }

    val readComponentInfos = ujson.read(docgenOutput).obj.values.toSeq.collect {
      case value if Set("props", "displayName").forall(value.obj.contains) =>
        val componentInfo = ComponentInfo.read(value.obj)
        overrides.getPropInfoOverrides(componentInfo).foldLeft(componentInfo) {
          case (componentInfo, (propName, Overrides.PropInfoOverride(typ, required))) =>
            componentInfo.propsMap.get(propName)
              .map { existingPropInfo =>
                existingPropInfo.copy(
                  required = required.getOrElse(existingPropInfo.required),
                  `type` = typ.getOrElse(existingPropInfo.`type`)
                )
              }
              .orElse(typ.map(propTypeInfo => PropInfo(propName, propTypeInfo)))
              .fold(componentInfo)(componentInfo.withProp)
        }
    }

    val componentInfos =
      readComponentInfos ++
        (overrides.components -- readComponentInfos.map(_.name).toSet).map { case (name, overrides) =>
          ComponentInfo(
            name = name,
            description = "",
            propInfos = overrides.props.toSeq.map { case (propName, Overrides.PropInfoOverride(typ, required)) =>
              PropInfo(
                name = propName,
                identifier = Identifier(propName),
                `type` = typ.getOrElse(PropTypeInfo.jsAny),
                required = required.getOrElse(false)
              )
            }
          )
        }

    for (componentInfo <- componentInfos) yield processComponent(
      info = componentInfo,
      scalaPackage = scalaPackage,
      jsPackage = jsPackage,
      outputFile = outputDir / (componentInfo.name + ".scala"),
      moduleTraitsOverride = overrides.components.get(componentInfo.name).flatMap(_.moduleTrait),
      logger = logger
    ).toIO
  }
}
