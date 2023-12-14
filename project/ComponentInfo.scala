import scala.collection.immutable.SortedMap


case class ComponentInfo(name: String, description: String, propsMap: SortedMap[String, PropInfo]) {
  def modProps(f: Seq[PropInfo] => Seq[PropInfo]) =
    copy(propsMap = ComponentInfo.makePropsMap(f(propsMap.values.toSeq)))

  def props                        = propsMap.values.toSeq
  lazy val maybeChildrenProp       = propsMap.get("children")
  def withProp(propInfo: PropInfo) = copy(propsMap = propsMap + (propInfo.name -> propInfo))
}
object ComponentInfo                                                                               {
  private def makePropsMap(infos: Seq[PropInfo]) = SortedMap(infos.map(p => p.name -> p)*)

  def read(jsonObject: collection.Map[String, ujson.Value]) = {
    val propInfos =
      PropInfo.readAll(jsonObject("props").obj - "key")
        .filterNot(_.description.contains("@ignore"))
    ComponentInfo(
      name = jsonObject("displayName").str,
      description = jsonObject("description").str,
      propsMap = makePropsMap(propInfos)
    )
  }
}
