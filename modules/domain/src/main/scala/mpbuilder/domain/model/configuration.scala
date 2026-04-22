package mpbuilder.domain.model

final case class ProductConfiguration(
    id: ConfigurationId,
    category: ProductCategory,
    printingMethod: PrintingMethod,
    components: List[ProductComponent],
    specifications: ProductSpecifications,
)

object ProductConfiguration:
  extension (config: ProductConfiguration)
    def mainComponent: Option[ProductComponent] =
      config.components.find(_.role == ComponentRole.Main)

    def coverComponent: Option[ProductComponent] =
      config.components.find(_.role == ComponentRole.Cover)

    def frontCoverComponent: Option[ProductComponent] =
      config.components.find(_.role == ComponentRole.FrontCover)

    def backCoverComponent: Option[ProductComponent] =
      config.components.find(_.role == ComponentRole.BackCover)

    def bindingComponent: Option[ProductComponent] =
      config.components.find(_.role == ComponentRole.Binding)

    def bodyComponent: Option[ProductComponent] =
      config.components.find(_.role == ComponentRole.Body)
