import sjsonnew.BasicJsonProtocol._
import sjsonnew.JsonFormat


case class PropInfo(name: String,
                    ident: Identifier,
                    propTypeInfo: PropTypeInfo,
                    description: String,
                    required: Boolean)

object PropInfo {
  def apply(name: String, code: String, imports: Set[String] = Set.empty): PropInfo =
    PropInfo(name, Identifier(name), PropTypeInfo(code, imports), "", required = false)

  def readAll(obj: scala.collection.Map[String, ujson.Value]) =
    obj
      .toSeq
      .sortBy(_._1)
      .flatMap { case (name, spec) =>
        PropType.read(spec("type").obj)
          .map { propType =>
            PropInfo(
              name = name,
              ident = Identifier(name),
              propTypeInfo = PropTypeInfo(propType),
              description = spec("description").str,
              required = spec("required").bool
            )
          }
      }
  implicit val jf: JsonFormat[PropInfo] =
    caseClass(new PropInfo(_, _, _, _, _), unapply)("name", "ident", "propTypeInfo", "description", "required")
}
