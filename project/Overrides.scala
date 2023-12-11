import Overrides.PropInfoOverride
import io.circe.Codec
import io.circe.generic.extras.Configuration
import io.circe.generic.extras.semiauto.deriveConfiguredCodec


case class Overrides(
  common: Map[String, PropInfoOverride] = Map.empty,
  components: Map[String, Overrides.ComponentOverrides] = Map.empty,
  moduleTraits: Map[String, String] = Map.empty) {

  def getPropInfoOverrides(componentInfo: ComponentInfo): Map[String, PropInfoOverride] =
    common ++
      components.getOrElse(componentInfo.name, Overrides.ComponentOverrides.NoOp).props
}
object Overrides {
  case class ComponentOverrides(props: Map[String, PropInfoOverride] = Map.empty, moduleTrait: Option[String] = None)
  object ComponentOverrides {
    val NoOp = ComponentOverrides()
  }

  case class PropInfoOverride(`type`: Option[PropTypeInfo] = None, required: Option[Boolean] = None)

  private implicit val circeConfig                                        = Configuration.default.withDefaults
  private implicit val propInfoOverrideCodec: Codec[PropInfoOverride]     =
    Util.withoutEmpty(deriveConfiguredCodec[PropInfoOverride])
  private implicit val propInfoCodec: Codec[PropInfo]                     =
    Util.withoutEmpty(deriveConfiguredCodec[PropInfo])
  private implicit val componentOverridesCodec: Codec[ComponentOverrides] =
    Util.withoutEmpty(deriveConfiguredCodec[ComponentOverrides])
  implicit val overridesCodec: Codec[Overrides]                           =
    Util.withoutEmpty(deriveConfiguredCodec[Overrides])
}
