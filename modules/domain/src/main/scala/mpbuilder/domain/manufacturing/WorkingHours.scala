package mpbuilder.domain.manufacturing

import java.time.{DayOfWeek, LocalDate, LocalTime, ZoneId}

/** Working hours configuration for the print shop. */
final case class WorkingHours(
    openTime: LocalTime,
    closeTime: LocalTime,
    workDays: Set[DayOfWeek],
    holidays: Set[LocalDate],
    timezone: ZoneId,
)

object WorkingHours:
  /** Default working hours: Mon-Fri, 07:00-17:00, Europe/Prague. */
  val default: WorkingHours = WorkingHours(
    openTime = LocalTime.of(7, 0),
    closeTime = LocalTime.of(17, 0),
    workDays = Set(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY),
    holidays = Set.empty,
    timezone = ZoneId.of("Europe/Prague"),
  )
