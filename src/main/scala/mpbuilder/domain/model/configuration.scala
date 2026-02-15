package mpbuilder.domain.model

final case class ProductConfiguration(
    id: ConfigurationId,
    category: ProductCategory,
    material: Material,
    finishes: List[Finish],
    specifications: ProductSpecifications,
)
