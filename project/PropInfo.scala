case class PropInfo(name: String,
                    ident: String,
                    description: String,
                    required: Boolean,
                    propType: PropType,
                    propTypeCode: String,
                    imports: Set[String]) {
}
object PropInfo {
  def readAll(obj: scala.collection.Map[String, ujson.Value]) =
    obj
      .toSeq
      .sortBy(_._1)
      .flatMap { case (name, spec) =>
        PropType.read(spec("type").obj)
          .map { propType =>
            val ident =
              if (name == "type" || name.contains('-'))
                "`" + name + "`"
              else
                name

            val (imports, propTypeCode) = PropType.importsAndCode(propType)

            PropInfo(
              name = name,
              ident = ident,
              description = spec("description").str,
              required = spec("required").bool,
              propType = propType,
              propTypeCode = propTypeCode,
              imports = imports
            )
          }
      }
}
