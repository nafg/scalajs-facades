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

  def read(obj: scala.collection.Map[String, ujson.Value]): PropType = obj("name").str match {
    case "any"                                        => Any
    case "array"                                      => ArrayOf(Any)
    case "bool"                                       => Bool
    case "element"                                    => Element
    case "elementType"                                => ElementType
    case "func"                                       => Func
    case "node"                                       => Node
    case "number"                                     => Number
    case "shape" | "custom" | "instanceOf" | "object" => Object
    case "string"                                     => String
    case "arrayOf"                                    => ArrayOf(read(obj("value").obj))
    case "union"                                      => Union(obj("value").arr.map(o => read(o.obj)))
    case "enum"                                       =>
      val values = obj("value").arr.map(_.obj("value").str)
      if (values.forall(_.matches("\\d+")))
        Number
      else if (values.forall(s => s.matches("'.*'") | s.matches("\".*\"")))
        String
      else
        Any
  }

  def importsAndCode(propType: PropType): (Set[String], String) = {
    def loop(propType: PropType): (Set[String], String) = propType match {
      case Any            => Set.empty[String] -> "js.Any"
      case Bool           => Set.empty[String] -> "Boolean"
      case Element        => Set("japgolly.scalajs.react.vdom.VdomElement") -> "VdomElement"
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
        (paramImports) -> s"Seq[$paramCode]"
      case Union(types)   =>
        val (paramsImports, paramsCodes) = types.map(loop).unzip
        (paramsImports.flatten.toSet ++
          Set("scala.scalajs.js.|", "io.github.nafg.simplefacade.Implicits.unionWriter")) ->
          paramsCodes.mkString("(", " | ", ")")
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
