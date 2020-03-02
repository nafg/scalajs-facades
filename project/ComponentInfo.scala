case class ComponentInfo(displayName: String, description: String, props: Seq[PropInfo])
object ComponentInfo {
  def read(jsonObject: collection.Map[String, ujson.Value]) =
    ComponentInfo(
      displayName = jsonObject("displayName").str,
      description = jsonObject("description").str,
      props = PropInfo.readAll(jsonObject("props").obj - "key").sortBy(_.name)
    )
}
