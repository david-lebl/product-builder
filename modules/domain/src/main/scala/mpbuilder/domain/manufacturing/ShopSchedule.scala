package mpbuilder.domain.manufacturing

import java.time.LocalTime

/** Shop schedule with working hours and cutoff times for manufacturing tiers. */
final case class ShopSchedule(
    workingHours: WorkingHours,
    expressCutoffTime: LocalTime,
    standardCutoffTime: LocalTime,
)

object ShopSchedule:
  /** Default schedule with Express cutoff at 14:00, Standard cutoff at 16:00. */
  val default: ShopSchedule = ShopSchedule(
    workingHours = WorkingHours.default,
    expressCutoffTime = LocalTime.of(14, 0),
    standardCutoffTime = LocalTime.of(16, 0),
  )
