package mpbuilder.domain.model

import mpbuilder.catalog.*

import mpbuilder.kernel.*

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
