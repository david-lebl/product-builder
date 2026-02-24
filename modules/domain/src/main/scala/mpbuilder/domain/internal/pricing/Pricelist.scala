package mpbuilder.domain.internal.pricing

final case class Pricelist(
    rules: List[PricingRule],
    currency: Currency,
    version: String,
)
