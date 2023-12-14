import sjsonnew.BasicJsonProtocol._
import sjsonnew.JsonFormat


case class Identifier(value: String) {
  override def toString = Identifier.quotedIfNecessary(value)
}

object Identifier {
  val keywords                            = Set("type", "true", "false")
  def quotedIfNecessary(value: String)    =
    if (keywords.contains(value) || value.contains('-') || value.headOption.forall(_.isDigit))
      "`" + value + "`"
    else
      value
  implicit val jf: JsonFormat[Identifier] = caseClass(apply _, unapply(_: Identifier))("value")
}
