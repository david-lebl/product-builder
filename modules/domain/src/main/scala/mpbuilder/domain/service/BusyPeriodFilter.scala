package mpbuilder.domain.service

import mpbuilder.domain.pricing.BusyPeriodMultiplier
import java.time.{LocalDateTime, DayOfWeek, Month}

object BusyPeriodFilter:
  def filterActive(
      multipliers: List[BusyPeriodMultiplier],
      now: LocalDateTime,
  ): List[BusyPeriodMultiplier] =
    multipliers.filter { m =>
      val dayMatch = m.dayOfWeek.forall(_.contains(now.getDayOfWeek))
      val monthMatch = m.monthRange.forall { case (start, end) =>
        val month = now.getMonth
        if start.getValue <= end.getValue then
          month.getValue >= start.getValue && month.getValue <= end.getValue
        else
          month.getValue >= start.getValue || month.getValue <= end.getValue
      }
      val timeMatch = m.timeAfter.forall(t => !now.toLocalTime.isBefore(t))
      dayMatch && monthMatch && timeMatch
    }
