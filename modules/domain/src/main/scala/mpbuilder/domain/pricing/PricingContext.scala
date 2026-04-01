package mpbuilder.domain.pricing

import mpbuilder.domain.model.ManufacturingSpeed

/** Dynamic pricing context provided at calculation time.
  *
  * Contains queue utilisation and time-based multipliers that affect
  * manufacturing speed surcharges.
  */
final case class PricingContext(
    globalUtilisation: BigDecimal,
    busyPeriodMultipliers: List[BusyPeriodMultiplier],
    currentTimeMillis: Long,
    expressSurchargeCap: BigDecimal,
)

object PricingContext:
  /** Default context with no dynamic adjustments (for backward compatibility). */
  val default: PricingContext = PricingContext(
    globalUtilisation = BigDecimal(0),
    busyPeriodMultipliers = List.empty,
    currentTimeMillis = 0L,
    expressSurchargeCap = BigDecimal(2),
  )
