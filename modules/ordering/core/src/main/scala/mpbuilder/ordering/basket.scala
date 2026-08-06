package mpbuilder.ordering

import mpbuilder.catalog.*

import mpbuilder.kernel.*

import mpbuilder.pricing.PriceBreakdown

final case class BasketItem(
    configuration: ProductConfiguration,
    quantity: Int,
    priceBreakdown: PriceBreakdown,
)

final case class Basket(
    id: BasketId,
    items: List[BasketItem],
)
