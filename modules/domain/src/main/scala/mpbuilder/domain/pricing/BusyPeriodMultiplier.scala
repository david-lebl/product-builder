package mpbuilder.domain.pricing

import java.time.{DayOfWeek, Month, LocalTime}

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
