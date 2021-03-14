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
  case class Enum(base: PropType, values: Seq[String]) extends PropType

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
        Some(Enum(Number, values))
      else if (values.forall(s => s.matches("'.*'") | s.matches("\".*\"")))
        Some(Enum(String, values))
      else
        Some(Enum(Any, values))
  }
}
