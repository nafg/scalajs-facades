import scala.collection.immutable.SortedMap

import Overrides.PropInfoOverride
import io.circe.Codec
import io.circe.generic.extras.Configuration
import io.circe.generic.extras.semiauto.deriveConfiguredCodec


case class Overrides(
  common: SortedMap[String, PropInfoOverride] = SortedMap.empty,
  components: SortedMap[String, Overrides.ComponentOverrides] = SortedMap.empty) {

  def get(name: String): Overrides.ComponentOverrides = components.getOrElse(name, Overrides.ComponentOverrides.empty)

  def getPropInfoOverrides(componentInfo: ComponentInfo): Map[String, PropInfoOverride] =
    common ++
      get(componentInfo.name).props

  def applyExtends: Overrides =
    copy(components =
      components.map { case (name, componentOverrides) =>
        name -> componentOverrides.applyExtends(this)
      }
    )
}
object Overrides {
  case class ComponentOverrides(
    props: SortedMap[String, PropInfoOverride] = SortedMap.empty,
    `extends`: Option[String] = None,
    moduleTrait: Option[String] = None) {

    def ++(that: ComponentOverrides) =
      copy(
        props = this.props ++ that.props,
        `extends` = this.`extends`.orElse(that.`extends`),
        moduleTrait = this.moduleTrait.orElse(that.moduleTrait)
      )

    def applyExtends(overrides: Overrides): ComponentOverrides =
      `extends` match {
        case None             => this
        case Some(parentName) =>
          val parent         = overrides.get(parentName)
          val parentExtended = parent.applyExtends(overrides)
          parentExtended ++ this
      }
  }
  object ComponentOverrides {
    val empty = ComponentOverrides()
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
