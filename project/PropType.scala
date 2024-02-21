sealed trait PropType

object PropType {
  sealed trait Simple                                extends PropType
  case object Any                                    extends Simple
  case object Bool                                   extends Simple
  case object Element                                extends Simple
  case object ElementType                            extends Simple
  case object Func                                   extends PropType
  case object Node                                   extends Simple
  case object Number                                 extends Simple
  case object Object                                 extends Simple
  case object String                                 extends Simple
  case class ArrayOf(param: PropType)                extends PropType
  case class Union(types: Seq[PropType])             extends PropType
  case class Enum(base: Simple, values: Seq[String]) extends PropType

  def read(obj: scala.collection.Map[String, ujson.Value]): Option[PropType] =
    obj("name").str match {
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
          Some(Any)
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
