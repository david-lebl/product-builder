package mpbuilder.domain.pricing

final case class Pricelist(
    rules: List[PricingRule],
    currency: Currency,
    version: String,
)
