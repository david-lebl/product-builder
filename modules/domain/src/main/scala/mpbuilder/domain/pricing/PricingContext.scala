package mpbuilder.domain.pricing

import mpbuilder.domain.model.*

/** Runtime context for dynamic pricing calculations.
  *
  * Carries queue utilisation and time-based information that
  * the PriceCalculator uses for Phase 2 dynamic surcharges.
  */
final case class PricingContext(
    globalUtilisation: BigDecimal = BigDecimal(0),
    busyPeriodMultipliers: List[BusyPeriodMultiplier] = Nil,
    currentDayOfWeek: Option[Int] = None,     // 1=Monday..7=Sunday
    currentMonth: Option[Int] = None,         // 1=January..12=December
    currentMinuteOfDay: Option[Int] = None,   // minutes from midnight
    expressSurchargeCap: BigDecimal = BigDecimal(2), // maximum effective Express multiplier
)

object PricingContext:
  val empty: PricingContext = PricingContext()
