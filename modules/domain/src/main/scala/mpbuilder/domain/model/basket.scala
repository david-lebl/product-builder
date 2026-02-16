package mpbuilder.domain.model

import mpbuilder.domain.pricing.PriceBreakdown

final case class BasketItem(
    configuration: ProductConfiguration,
    quantity: Int,
    priceBreakdown: PriceBreakdown,
)

final case class Basket(
    id: BasketId,
    items: List[BasketItem],
)
