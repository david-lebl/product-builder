package mpbuilder.pricing
package impl
package legacy

import mpbuilder.commons.*
import mpbuilder.domain.model as dm
import mpbuilder.domain.pricing as dp

/** Translation between the public pricing contract and the legacy domain model.
  *
  * The whole anti-corruption layer for pricing, in one file — so a domain rename has a one-file
  * blast radius.
  */
private[pricing] object Mapping:

  def toDomainSpeed(speed: ProductionSpeed): dm.ManufacturingSpeed = speed match
    case ProductionSpeed.Express  => dm.ManufacturingSpeed.Express
    case ProductionSpeed.Standard => dm.ManufacturingSpeed.Standard
    case ProductionSpeed.Economy  => dm.ManufacturingSpeed.Economy

  def toLineItem(item: dp.LineItem): LineItemView =
    LineItemView(item.label, item.unitPrice, item.quantity, item.lineTotal)

  def toQuote(b: dp.PriceBreakdown, pricelistVersion: String): PriceQuote =
    PriceQuote(
      components = b.componentBreakdowns.map(toComponent),
      surcharges = List(
        b.processSurcharge,
        b.categorySurcharge,
        b.foldSurcharge,
        b.bindingSurcharge,
      ).flatten.map(toLineItem),
      setupFees = b.setupFees.map(toLineItem),
      subtotal = b.subtotal,
      quantityMultiplier = b.quantityMultiplier,
      speedSurcharge = b.speedSurcharge.map(toLineItem),
      minimumApplied = b.minimumApplied,
      total = b.total,
      currency = b.currency,
      quantity = b.quantity,
      pricelistVersion = pricelistVersion,
    )

  private def toComponent(c: dp.ComponentBreakdown): ComponentQuote =
    ComponentQuote(
      role = c.role.toString,
      lines = (List(c.materialLine) ++ c.cuttingLine ++ c.inkConfigLine ++ c.finishLines).map(toLineItem),
      sheetsUsed = c.sheetsUsed,
    )

  def toProblem(error: dp.PricingError): Problem =
    Problem(
      code = error.toString.takeWhile(_ != '('),
      message = LocalizedString(error.message(Language.En), error.message(Language.Cs)),
    )
