package mpbuilder.domain.sample

import mpbuilder.domain.model.StationType
import mpbuilder.domain.manufacturing.*
import java.time.*

/** Sample manufacturing configuration data for station time estimates and shop schedule. */
object SampleManufacturing:

  /** Default station time estimates calibrated for a typical print shop. */
  val stationTimeEstimates: List[StationTimeEstimate] = List(
    StationTimeEstimate(StationType.Prepress,           baseTimeMinutes = 30,  perUnitSeconds = BigDecimal("0"),   maxParallelUnits = 1),
    StationTimeEstimate(StationType.DigitalPrinter,     baseTimeMinutes = 15,  perUnitSeconds = BigDecimal("0.5"), maxParallelUnits = 1),
    StationTimeEstimate(StationType.OffsetPress,        baseTimeMinutes = 45,  perUnitSeconds = BigDecimal("0.1"), maxParallelUnits = 1),
    StationTimeEstimate(StationType.LargeFormatPrinter, baseTimeMinutes = 20,  perUnitSeconds = BigDecimal("2.0"), maxParallelUnits = 1),
    StationTimeEstimate(StationType.Letterpress,        baseTimeMinutes = 30,  perUnitSeconds = BigDecimal("1.0"), maxParallelUnits = 1),
    StationTimeEstimate(StationType.Laminator,          baseTimeMinutes = 10,  perUnitSeconds = BigDecimal("0.3"), maxParallelUnits = 1),
    StationTimeEstimate(StationType.UVCoater,           baseTimeMinutes = 10,  perUnitSeconds = BigDecimal("0.2"), maxParallelUnits = 1),
    StationTimeEstimate(StationType.EmbossingFoil,      baseTimeMinutes = 20,  perUnitSeconds = BigDecimal("1.5"), maxParallelUnits = 1),
    StationTimeEstimate(StationType.Cutter,             baseTimeMinutes = 5,   perUnitSeconds = BigDecimal("0.2"), maxParallelUnits = 1),
    StationTimeEstimate(StationType.Folder,             baseTimeMinutes = 5,   perUnitSeconds = BigDecimal("0.3"), maxParallelUnits = 1),
    StationTimeEstimate(StationType.Binder,             baseTimeMinutes = 10,  perUnitSeconds = BigDecimal("0.5"), maxParallelUnits = 1),
    StationTimeEstimate(StationType.LargeFormatFinishing, baseTimeMinutes = 15, perUnitSeconds = BigDecimal("3.0"), maxParallelUnits = 1),
    StationTimeEstimate(StationType.QualityControl,     baseTimeMinutes = 15,  perUnitSeconds = BigDecimal("0"),   maxParallelUnits = 1),
    StationTimeEstimate(StationType.Packaging,          baseTimeMinutes = 10,  perUnitSeconds = BigDecimal("0"),   maxParallelUnits = 1),
  )

  /** Default shop schedule for a Central European print shop. */
  val shopSchedule: ShopSchedule = ShopSchedule.default

  /** Sample busy period multipliers for dynamic pricing. */
  val busyPeriodMultipliers: List[mpbuilder.domain.pricing.BusyPeriodMultiplier] = List(
    // Monday and Friday are peak order days
    mpbuilder.domain.pricing.BusyPeriodMultiplier(
      dayOfWeek = Some(Set(DayOfWeek.MONDAY, DayOfWeek.FRIDAY)),
      monthRange = None,
      timeAfter = None,
      additionalMultiplier = BigDecimal("0.05"),
    ),
    // Pre-Christmas season (November-December)
    mpbuilder.domain.pricing.BusyPeriodMultiplier(
      dayOfWeek = None,
      monthRange = Some((Month.NOVEMBER, Month.DECEMBER)),
      timeAfter = None,
      additionalMultiplier = BigDecimal("0.10"),
    ),
    // Conference season (September)
    mpbuilder.domain.pricing.BusyPeriodMultiplier(
      dayOfWeek = None,
      monthRange = Some((Month.SEPTEMBER, Month.SEPTEMBER)),
      timeAfter = None,
      additionalMultiplier = BigDecimal("0.05"),
    ),
  )
