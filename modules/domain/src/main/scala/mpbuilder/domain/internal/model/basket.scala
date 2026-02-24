package mpbuilder.domain.internal.model

import mpbuilder.domain.internal.pricing.PriceBreakdown

final case class BasketItem(
    configuration: ProductConfiguration,
    quantity: Int,
    priceBreakdown: PriceBreakdown,
)

final case class Basket(
    id: BasketId,
    items: List[BasketItem],
)
