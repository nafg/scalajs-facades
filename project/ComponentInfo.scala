case class ComponentInfo(name: String, description: String, props: Seq[PropInfo])
object ComponentInfo {
  def read(jsonObject: collection.Map[String, ujson.Value]) =
    ComponentInfo(
      name = jsonObject("displayName").str,
      description = jsonObject("description").str,
      props = PropInfo.readAll(jsonObject("props").obj - "key").sortBy(_.name)
    )
}
