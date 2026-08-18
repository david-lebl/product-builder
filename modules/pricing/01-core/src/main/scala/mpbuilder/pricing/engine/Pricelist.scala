package mpbuilder.pricing.engine

import mpbuilder.product.model.*

import mpbuilder.commons.*

final case class Pricelist(
    rules: List[PricingRule],
    currency: Currency,
    version: String,
)
