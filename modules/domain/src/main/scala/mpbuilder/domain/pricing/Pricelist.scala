package mpbuilder.domain.pricing

import mpbuilder.commons.*

final case class Pricelist(
    rules: List[PricingRule],
    currency: Currency,
    version: String,
)
