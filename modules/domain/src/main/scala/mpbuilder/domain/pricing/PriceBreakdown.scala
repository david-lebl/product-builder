package mpbuilder.domain.pricing

import mpbuilder.domain.model.ComponentRole

final case class LineItem(
    label: String,
    unitPrice: Money,
    quantity: Int,
    lineTotal: Money,
)

final case class ComponentLineItems(
    role: ComponentRole,
    materialLine: LineItem,
    inkConfigLine: Option[LineItem],
    finishLines: List[LineItem],
)

final case class PriceBreakdown(
    materialLine: LineItem,
    inkConfigLine: Option[LineItem],
    finishLines: List[LineItem],
    processSurcharge: Option[LineItem],
    categorySurcharge: Option[LineItem],
    componentLines: List[ComponentLineItems] = Nil,
    subtotal: Money,
    quantityMultiplier: BigDecimal,
    total: Money,
    currency: Currency,
)
