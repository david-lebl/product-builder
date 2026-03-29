package mpbuilder.ui.orders

import mpbuilder.domain.model.*
import mpbuilder.domain.pricing.*

/** Navigation sections within the internal order entry UI. */
enum OrderEntrySection:
  case OrderItems
  case OrderSummary

/** Represents a single line item in the internal order entry form.
  *
  * Each line has a product configuration derived from compact selections,
  * a calculated price from the pricing engine, and an optional custom price override.
  * Manufacturing cost is estimated as a percentage of the base price for display.
  */
final case class OrderLineItem(
    lineId: String,
    categoryId: Option[CategoryId] = None,
    printingMethodId: Option[PrintingMethodId] = None,
    materialId: Option[MaterialId] = None,
    inkConfiguration: InkConfiguration = InkConfiguration.cmyk4_4,
    finishIds: List[FinishId] = Nil,
    width: Int = 0,
    height: Int = 0,
    quantity: Int = 0,
    pages: Int = 0,
    foldType: Option[FoldType] = None,
    bindingMethod: Option[BindingMethod] = None,
    configuration: Option[ProductConfiguration] = None,
    priceBreakdown: Option[PriceBreakdown] = None,
    customPriceOverride: Option[BigDecimal] = None,
    notes: String = "",
)

object OrderLineItem:
  /** Estimated manufacturing cost as a fraction of calculated price.
    * This is a simplified estimate; real cost tracking would come from production data.
    */
  val EstimatedCostFactor: BigDecimal = BigDecimal("0.40")

  /** Margin thresholds for visual indicators. */
  val MarginGoodThreshold: BigDecimal = BigDecimal(30)
  val MarginOkThreshold: BigDecimal   = BigDecimal(15)

  extension (item: OrderLineItem)
    /** The effective price: custom override or calculated total. */
    def effectivePrice: Money =
      item.customPriceOverride match
        case Some(v) => Money(v).rounded
        case None    => item.priceBreakdown.map(_.total).getOrElse(Money.zero)

    /** Estimated manufacturing cost (simplified estimate). */
    def estimatedCost: Money =
      item.priceBreakdown.map(bd => (bd.total * EstimatedCostFactor).rounded).getOrElse(Money.zero)

    /** Margin = effective price - estimated cost. */
    def margin: Money =
      Money(item.effectivePrice.value - item.estimatedCost.value).rounded

    /** Margin percentage. */
    def marginPercent: BigDecimal =
      val eff = item.effectivePrice
      if eff.value > 0 then
        ((Money(eff.value - item.estimatedCost.value).value / eff.value) * 100)
          .setScale(1, BigDecimal.RoundingMode.HALF_UP)
      else BigDecimal(0)

    /** Short description for display. */
    def description(catalog: ProductCatalog): String =
      val catName = item.categoryId.flatMap(id => catalog.categories.get(id)).map(_.name.value).getOrElse("—")
      val matName = item.materialId.flatMap(id => catalog.materials.get(id)).map(_.name.value).getOrElse("")
      val qty = if item.quantity > 0 then s" × ${item.quantity}" else ""
      s"$catName $matName$qty"

    /** CSS class suffix for margin indicator coloring. */
    def marginCssClass: String =
      if item.priceBreakdown.isEmpty then ""
      else if item.marginPercent >= MarginGoodThreshold then " margin-good"
      else if item.marginPercent >= MarginOkThreshold then " margin-ok"
      else " margin-low"

  /** Format a Money value to 2 decimal places. */
  def formatMoney(m: Money): String =
    m.rounded.value.setScale(2).toString

/** The complete state of the internal order entry UI. */
final case class InternalOrderEntryState(
    selectedCustomer: Option[Customer] = None,
    lineItems: List[OrderLineItem] = Nil,
    activeSection: OrderEntrySection = OrderEntrySection.OrderItems,
    orderNotes: String = "",
    nextLineId: Int = 1,
)
