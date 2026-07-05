package mpbuilder.pricing

import java.time.{DayOfWeek, Month, LocalTime}

/** Queue utilisation threshold that triggers an additional pricing multiplier.
  *
  * When globalUtilisation >= minUtilisation, the additionalMultiplier is
  * added to the base manufacturing speed multiplier.
  */
final case class QueueThreshold(
    minUtilisation: BigDecimal,
    additionalMultiplier: BigDecimal,
)

/** Time-based busy period multiplier for dynamic pricing.
  *
  * When the current time matches the configured criteria, the
  * additionalMultiplier is added to the speed surcharge.
  */
final case class BusyPeriodMultiplier(
    dayOfWeek: Option[Set[DayOfWeek]],
    monthRange: Option[(Month, Month)],
    timeAfter: Option[LocalTime],
    additionalMultiplier: BigDecimal,
)
