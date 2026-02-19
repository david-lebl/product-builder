package mpbuilder.domain.model

final case class ProductConfiguration(
    id: ConfigurationId,
    category: ProductCategory,
    material: Material,
    printingMethod: PrintingMethod,
    finishes: List[Finish],
    specifications: ProductSpecifications,
    components: List[ProductComponent] = Nil,
)
