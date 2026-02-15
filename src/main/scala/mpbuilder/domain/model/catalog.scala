package mpbuilder.domain.model

final case class ProductCatalog(
    categories: Map[CategoryId, ProductCategory],
    materials: Map[MaterialId, Material],
    finishes: Map[FinishId, Finish],
)
