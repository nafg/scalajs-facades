import sjsonnew.BasicJsonProtocol._
import sjsonnew.JsonFormat


case class PropTypeInfo(code: String, imports: Set[String] = Set.empty, presets: Seq[PropTypeInfo.Preset] = Nil)

object PropTypeInfo {
  case class Preset(name: Identifier, code: String)

  private val stringEnumValueRE               = "'(.*)'".r
  private val litEnumValueRE                  = """(true|false|-?\d+\.\d+|-?\d+)""".r
  def apply(propType: PropType): PropTypeInfo = {
    def loop(propType: PropType): PropTypeInfo =
      propType match {
        case PropType.Any                => PropTypeInfo("js.Any")
        case PropType.Bool               => PropTypeInfo("Boolean")
        case PropType.Element            => PropTypeInfo("VdomElement", CommonImports.VdomElement)
        case PropType.ElementType        => PropTypeInfo("ElementType", CommonImports.ElementType)
        case PropType.Func               => PropTypeInfo("(js.Any => js.Any)")
        case PropType.Node               => PropTypeInfo("VdomNode", CommonImports.VdomNode)
        case PropType.Number             => PropTypeInfo("Double")
        case PropType.Object             => PropTypeInfo("js.Object")
        case PropType.String             => PropTypeInfo("String")
        case PropType.ArrayOf(param)     =>
          val PropTypeInfo(paramCode, paramImports, _) = loop(param)
          PropTypeInfo(s"Seq[$paramCode]", paramImports)
        case PropType.Union(types)       =>
          val (paramsImports, paramsCodes, presets) =
            types.map(loop).map(pti => (pti.imports, pti.code, pti.presets)).unzip3
          PropTypeInfo(
            code = paramsCodes.mkString("(", " | ", ")"),
            imports = paramsImports.flatten.toSet ++ CommonImports.|,
            presets = presets.flatten
          )
        case PropType.Enum(base, values) =>
          val res = loop(base)
          res.copy(presets =
            res.presets ++
              values.collect {
                case litEnumValueRE(s)    => Preset(Identifier(s), s)
                case stringEnumValueRE(s) => Preset(Identifier(s), '"' + s + '"')
              }
          )
      }
    def unwrap(s: String)                      =
      if (s.headOption.contains('(') && s.lastOption.contains(')'))
        s.drop(1).dropRight(1)
      else
        s
    val propTypeInfo                           = loop(propType)
    propTypeInfo.copy(code = unwrap(propTypeInfo.code))
  }
  implicit val presetJF: JsonFormat[Preset]   = caseClass(Preset.apply _, Preset.unapply _)("name", "code")
  implicit val rw: JsonFormat[PropTypeInfo]   =
    caseClass(new PropTypeInfo(_, _, _), unapply)("code", "imports", "presets")
}
