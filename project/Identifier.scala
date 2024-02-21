import io.circe.{Codec, Decoder, Encoder}
import io.circe.generic.extras.Configuration
import io.circe.generic.extras.semiauto.deriveConfiguredCodec
import sjsonnew.BasicJsonProtocol.*
import sjsonnew.JsonFormat


case class Identifier(value: String) {
  override def toString = Identifier.quotedIfNecessary(value)
}

object Identifier {
  val keywords                            = Set("type", "true", "false")
  def quotedIfNecessary(value: String)    =
    if (value == "")
      "`''`"
    else if (
      keywords.contains(value) ||
      value.headOption.exists(!Character.isJavaIdentifierStart(_)) ||
      value.exists(!Character.isJavaIdentifierPart(_))
    )
      "`" + value + "`"
    else
      value
  implicit val jf: JsonFormat[Identifier] = caseClass(apply _, unapply(_: Identifier))("value")

  private implicit val circeConfig                = Configuration.default.withDefaults
  implicit val identifierCodec: Codec[Identifier] =
    Codec.from(Decoder[String], Encoder[String]).iemap(s => Right(Identifier(s)))(_.value)
}
