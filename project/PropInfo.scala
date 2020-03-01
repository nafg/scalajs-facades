case class PropInfo(name: String, description: String, required: Boolean, propType: PropType) {
  lazy val (imports, propTypeCode) = PropType.importsAndCode(propType)
  lazy val ident =
    if (name == "type" || name.contains('-'))
      "`" + name + "`"
    else
      name
}

object PropInfo {
  def readAll(obj: scala.collection.Map[String, ujson.Value]) =
    obj
      .toSeq
      .sortBy(_._1)
      .map { case (name, spec) =>
        PropInfo(name, spec("description").str, spec("required").bool, PropType.read(spec("type").obj))
      }
}
