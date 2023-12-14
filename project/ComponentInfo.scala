import sjsonnew.BasicJsonProtocol._
import sjsonnew.JsonFormat


case class ComponentInfo(name: String, description: String, props: Seq[PropInfo]) {
  lazy val maybeChildrenProp                      = props.find(_.name == "children")
  def modProps(f: Seq[PropInfo] => Seq[PropInfo]) = copy(props = f(props))
  def addPropIfNotExists(propInfo: PropInfo)      =
    if (props.exists(_.name == propInfo.name))
      this
    else
      modProps(_ :+ propInfo)
}
object ComponentInfo                                                              {
  def read(jsonObject: collection.Map[String, ujson.Value]) =
    ComponentInfo(
      name = jsonObject("displayName").str,
      description = jsonObject("description").str,
      props = PropInfo.readAll(jsonObject("props").obj - "key")
    )
  implicit val jf: JsonFormat[ComponentInfo]                = caseClass(apply _, unapply _)("name", "description", "props")
}
