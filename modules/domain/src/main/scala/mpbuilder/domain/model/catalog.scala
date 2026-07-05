package mpbuilder.domain.model

import mpbuilder.kernel.*

final case class ProductCatalog(
    categories: Map[CategoryId, ProductCategory],
    materials: Map[MaterialId, Material],
    finishes: Map[FinishId, Finish],
    printingMethods: Map[PrintingMethodId, PrintingMethod],
)
