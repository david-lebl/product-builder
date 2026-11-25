package mpbuilder.design

import mpbuilder.shared.Language
import java.time.{DayOfWeek, LocalDate}

/** One cell of a month grid. `day = None` for leading/trailing blanks. */
final case class CalendarCell(day: Option[Int], weekend: Boolean, holiday: Boolean)

final case class CalendarRow(isoWeek: Int, cells: Vector[CalendarCell])

/** Pure calendar arithmetic (Gregorian, ISO weeks) and EN/CS labels used by calendar templates and renderers. */
object CalendarMath:

  def daysInMonth(year: Int, month: Int): Int = LocalDate.of(year, month, 1).lengthOfMonth()

  def isoWeek(date: LocalDate): Int =
    val wd       = date.getDayOfWeek.getValue // 1 = Monday
    val thursday = date.plusDays((4 - wd).toLong)
    (thursday.getDayOfYear - 1) / 7 + 1

  /** Gregorian Easter Sunday (anonymous / Meeus algorithm). */
  def easterSunday(year: Int): LocalDate =
    val a     = year % 19
    val b     = year / 100
    val c     = year % 100
    val d     = b / 4
    val e     = b % 4
    val f     = (b + 8) / 25
    val g     = (b - f + 1) / 3
    val h     = (19 * a + b - d - g + 15) % 30
    val i     = c / 4
    val k     = c % 4
    val l     = (32 + 2 * e + 2 * i - h - k) % 7
    val m     = (a + 11 * h + 22 * l) / 451
    val month = (h + l - 7 * m + 114) / 31
    val day   = ((h + l - 7 * m + 114) % 31) + 1
    LocalDate.of(year, month, day)

  /** Czech public holidays (státní svátky a ostatní svátky). */
  def czechHolidays(year: Int): Set[LocalDate] =
    val fixed = List((1, 1), (5, 1), (5, 8), (7, 5), (7, 6), (9, 28), (10, 28), (11, 17), (12, 24), (12, 25), (12, 26))
      .map((m, d) => LocalDate.of(year, m, d))
    val easter = easterSunday(year)
    (fixed ++ List(easter.minusDays(2), easter.plusDays(1))).toSet

  def holidays(set: HolidaySet, year: Int): Set[LocalDate] = set match
    case HolidaySet.NoHolidays => Set.empty
    case HolidaySet.Czech      => czechHolidays(year)

  /** Week rows of a month (always complete 7-day rows, blanks as `None`). */
  def monthGrid(spec: CalendarSpec): Vector[CalendarRow] =
    val first    = LocalDate.of(spec.year, spec.month, 1)
    val days     = first.lengthOfMonth()
    val startDow = first.getDayOfWeek.getValue // 1..7 (Mon..Sun)
    val offset = spec.weekStart match
      case WeekStart.Monday => startDow - 1
      case WeekStart.Sunday => startDow % 7
    val hol   = holidays(spec.holidays, spec.year)
    val total = offset + days
    val rows  = (total + 6) / 7
    (0 until rows).toVector.map { r =>
      val cells = (0 until 7).toVector.map { c =>
        val dayNum = r * 7 + c - offset + 1
        val colDow = weekdayAt(spec.weekStart, c)
        val weekend = colDow == DayOfWeek.SATURDAY || colDow == DayOfWeek.SUNDAY
        if dayNum >= 1 && dayNum <= days then
          val date = LocalDate.of(spec.year, spec.month, dayNum)
          CalendarCell(Some(dayNum), weekend, hol.contains(date))
        else CalendarCell(None, weekend, holiday = false)
      }
      val firstDay = cells.flatMap(_.day).headOption.getOrElse(1)
      CalendarRow(isoWeek(LocalDate.of(spec.year, spec.month, firstDay)), cells)
    }

  def weekdayAt(start: WeekStart, column: Int): DayOfWeek = start match
    case WeekStart.Monday => DayOfWeek.of(column + 1)
    case WeekStart.Sunday => DayOfWeek.of(((column + 6) % 7) + 1)

  private val monthsEn = Vector("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December")
  private val monthsCs = Vector("Leden", "Únor", "Březen", "Duben", "Květen", "Červen", "Červenec", "Srpen", "Září", "Říjen", "Listopad", "Prosinec")
  private val daysEn   = Vector("Mo", "Tu", "We", "Th", "Fr", "Sa", "Su")
  private val daysCs   = Vector("Po", "Út", "St", "Čt", "Pá", "So", "Ne")

  def monthName(month: Int, lang: Language): String =
    val names = if lang == Language.Cs then monthsCs else monthsEn
    names((month - 1).max(0).min(11))

  /** Short weekday label for a grid column. */
  def weekdayLabel(start: WeekStart, column: Int, lang: Language): String =
    val dow   = weekdayAt(start, column).getValue - 1
    val names = if lang == Language.Cs then daysCs else daysEn
    names(dow)
