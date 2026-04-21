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

    def frontCoverComponent: Option[ProductComponent] =
      config.components.find(_.role == ComponentRole.FrontCover)

    def backCoverComponent: Option[ProductComponent] =
      config.components.find(_.role == ComponentRole.BackCover)

    def bodyComponent: Option[ProductComponent] =
      config.components.find(_.role == ComponentRole.Body)

    def bindingComponent: Option[ProductComponent] =
      config.components.find(_.role == ComponentRole.Binding)
