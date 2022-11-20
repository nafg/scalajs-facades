case class ComponentCodeGenInfo(componentInfo: ComponentInfo, moduleTrait: String, moduleTraitParam: Option[String])
object ComponentCodeGenInfo {
  def apply(componentInfo: ComponentInfo): ComponentCodeGenInfo = {
    val (moduleTrait, moduleTraitParam) = componentInfo.maybeChildrenProp match {
      case Some(i) if i.propTypeInfo.code == "VdomNode" => "FacadeModule.NodeChildren" -> None
      case Some(i)                                      => "FacadeModule.ChildrenOf"   -> Some(i.propTypeInfo.code)
      case _                                            => "FacadeModule"              -> None
    }
    ComponentCodeGenInfo(
      componentInfo = componentInfo,
      moduleTrait = moduleTrait,
      moduleTraitParam = moduleTraitParam
    )
  }
}
