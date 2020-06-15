sealed trait PropType

object PropType {
  case object Any extends PropType
  case object Bool extends PropType
  case object Element extends PropType
  case object ElementType extends PropType
  case object Func extends PropType
  case object Node extends PropType
  case object Number extends PropType
  case object Object extends PropType
  case object String extends PropType
  case class ArrayOf(param: PropType) extends PropType
  case class Union(types: Seq[PropType]) extends PropType

  def read(obj: scala.collection.Map[String, ujson.Value]): Option[PropType] = obj("name").str match {
    case "any"                             => Some(Any)
    case "array"                           => Some(ArrayOf(Any))
    case "bool"                            => Some(Bool)
    case "element"                         => Some(Element)
    case "elementType"                     => Some(ElementType)
    case "func"                            => Some(Func)
    case "node"                            => Some(Node)
    case "number"                          => Some(Number)
    case "string"                          => Some(String)
    case "arrayOf"                         => read(obj("value").obj).map(ArrayOf)
    case "shape" | "instanceOf" | "object" => Some(Object)
    case "custom"                          =>
      if (obj.get("raw").flatMap(_.strOpt).contains("unsupportedProp"))
        None
      else
        Some(Object)
    case "union"                           =>
      Some(Union(obj("value").arr.flatMap(o => read(o.obj))))
        .filter(_.types.nonEmpty)
    case "enum"                            =>
      val values = obj("value").arr.map(_.obj("value").str)
      if (values.forall(_.matches("\\d+")))
        Some(Number)
      else if (values.forall(s => s.matches("'.*'") | s.matches("\".*\"")))
        Some(String)
      else
        Some(Any)
  }

  def importsAndCode(propType: PropType): (Set[String], String) = {
    def loop(propType: PropType): (Set[String], String) = propType match {
      case Any            => Set.empty[String] -> "js.Any"
      case Bool           => Set.empty[String] -> "Boolean"
      case Element        =>
        Set("japgolly.scalajs.react.vdom.VdomElement", "io.github.nafg.simplefacade.Implicits.vdomElementWriter") ->
          "VdomElement"
      case ElementType    =>
        Set("io.github.nafg.simplefacade.Implicits.elementTypeWriter") -> "japgolly.scalajs.react.raw.React.ElementType"
      case Func           => Set.empty[String] -> "(js.Any => js.Any)"
      case Node           =>
        Set("japgolly.scalajs.react.vdom.VdomNode", "io.github.nafg.simplefacade.Implicits.vdomNodeWriter") ->
          "VdomNode"
      case Number         => Set.empty[String] -> "Double"
      case Object         => Set.empty[String] -> "js.Object"
      case String         => Set.empty[String] -> "String"
      case ArrayOf(param) =>
        val (paramImports, paramCode) = loop(param)
        paramImports -> s"Seq[$paramCode]"
      case Union(types)   =>
        val (paramsImports, paramsCodes) = types.map(loop).unzip
        (paramsImports.flatten.toSet ++
          Set("scala.scalajs.js.|")) -> paramsCodes.mkString("(", " | ", ")")
    }

    def unwrap(s: String) =
      if (s.headOption.contains('(') && s.lastOption.contains(')'))
        s.drop(1).dropRight(1)
      else
        s

    val (imports, code) = loop(propType)
    (imports, unwrap(code))
  }
}
