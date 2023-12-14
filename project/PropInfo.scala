case class PropInfo(
  name: String,
  identifier: Identifier,
  `type`: PropTypeInfo,
  description: String = "",
  required: Boolean = false)

object PropInfo {
  def apply(name: String, propTypeInfo: PropTypeInfo): PropInfo =
    PropInfo(name, Identifier(name), propTypeInfo, "", required = false)

  def readAll(obj: scala.collection.Map[String, ujson.Value]) =
    obj
      .toSeq
      .sortBy(_._1)
      .flatMap { case (name, spec) =>
        if (!spec.obj.contains("type"))
          None
        else
          PropType.read(spec("type").obj)
            .map { propType =>
              PropInfo(
                name = name,
                identifier = Identifier(name),
                `type` = PropTypeInfo(propType),
                description = spec("description").str,
                required = spec("required").bool
              )
            }
      }
}
