package mpbuilder.domain.pricing

final case class LineItem(
    label: String,
    unitPrice: Money,
    quantity: Int,
    lineTotal: Money,
)

final case class PriceBreakdown(
    materialLine: LineItem,
    finishLines: List[LineItem],
    processSurcharge: Option[LineItem],
    categorySurcharge: Option[LineItem],
    subtotal: Money,
    quantityMultiplier: BigDecimal,
    total: Money,
    currency: Currency,
)
