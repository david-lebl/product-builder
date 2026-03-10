package mpbuilder.domain.pricing

import mpbuilder.domain.model.ComponentRole

final case class LineItem(
    label: String,
    unitPrice: Money,
    quantity: Int,
    lineTotal: Money,
)

final case class ComponentBreakdown(
    role: ComponentRole,
    materialLine: LineItem,
    cuttingLine: Option[LineItem],
    inkConfigLine: Option[LineItem],
    finishLines: List[LineItem],
    sheetsUsed: Int,
)

final case class PriceBreakdown(
    componentBreakdowns: List[ComponentBreakdown],
    processSurcharge: Option[LineItem],
    categorySurcharge: Option[LineItem],
    foldSurcharge: Option[LineItem],
    bindingSurcharge: Option[LineItem],
    subtotal: Money,
    quantityMultiplier: BigDecimal,
    setupFees: List[LineItem],
    minimumApplied: Option[Money],
    total: Money,
    currency: Currency,
    quantity: Int,
)
