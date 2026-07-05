package mpbuilder.pricing

import mpbuilder.kernel.*

final case class Pricelist(
    rules: List[PricingRule],
    currency: Currency,
    version: String,
)
