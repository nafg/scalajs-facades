case class PropInfo(name: String,
                    ident: String,
                    propTypeCode: String,
                    description: String,
                    required: Boolean,
                    imports: Set[String])
object PropInfo {
  private def nameToIdent(name: String) =
    if (name == "type" || name.contains('-'))
      "`" + name + "`"
    else
      name

  def apply(name: String, code: String, imports: Set[String] = Set.empty): PropInfo =
    PropInfo(name, nameToIdent(name), code, "", required = false, imports = imports)

  def readAll(obj: scala.collection.Map[String, ujson.Value]) =
    obj
      .toSeq
      .sortBy(_._1)
      .flatMap { case (name, spec) =>
        PropType.read(spec("type").obj)
          .map { propType =>
            val (imports, propTypeCode) = PropType.importsAndCode(propType)

            PropInfo(
              name = name,
              ident = nameToIdent(name),
              propTypeCode = propTypeCode,
              description = spec("description").str,
              required = spec("required").bool,
              imports = imports
            )
          }
      }
}
