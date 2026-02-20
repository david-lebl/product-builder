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

    def bodyComponent: Option[ProductComponent] =
      config.components.find(_.role == ComponentRole.Body)
