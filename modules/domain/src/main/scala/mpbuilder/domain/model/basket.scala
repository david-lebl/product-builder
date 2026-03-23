package mpbuilder.domain.model

import mpbuilder.domain.pricing.PriceBreakdown

final case class BasketItem(
    configuration: ProductConfiguration,
    quantity: Int,
    priceBreakdown: PriceBreakdown,
    editorSessionId: Option[String] = None,
)

final case class Basket(
    id: BasketId,
    items: List[BasketItem],
)
