package mpbuilder.commons

import zio.test.*
import zio.prelude.Validation

object CommonsSpec extends ZIOSpecDefault:

  def spec = suite("commons")(
    suite("Money")(
      test("rounds HALF_UP to two decimal places") {
        assertTrue(
          Money("1.005").rounded.value == BigDecimal("1.01"),
          Money("1.004").rounded.value == BigDecimal("1.00"),
          Money("2.675").rounded.value == BigDecimal("2.68"),
        )
      },
      test("never loses precision the way Double would") {
        // 0.1 + 0.2 == 0.30000000000000004 as Double
        assertTrue((Money("0.1") + Money("0.2")).value == BigDecimal("0.3"))
      },
      test("division uses DECIMAL128 rather than truncating") {
        val third = Money(10) / 3
        assertTrue(third.value.scale > 2, (third * 3).rounded.value == BigDecimal("10.00"))
      },
      test("atLeast applies a floor") {
        assertTrue(
          Money(50).atLeast(Money(200)).value == BigDecimal(200),
          Money(500).atLeast(Money(200)).value == BigDecimal(500),
          Money(200).atLeast(Money(200)).value == BigDecimal(200),
        )
      },
      test("multiplication by quantity is exact") {
        assertTrue((Money("0.07") * 3).value == BigDecimal("0.21"))
      },
    ),
    suite("Price")(
      test("adds within the same currency") {
        val sum = Price(Money(10), Currency.CZK) + Price(Money(5), Currency.CZK)
        assertTrue(sum.amount.value == BigDecimal(15), sum.currency == Currency.CZK)
      },
      test("refuses to add across currencies") {
        val boom = scala.util.Try(Price(Money(10), Currency.CZK) + Price(Money(5), Currency.EUR))
        assertTrue(boom.isFailure)
      },
    ),
    suite("Percentage")(
      test("accepts the closed range 0–100") {
        assertTrue(
          Percentage(BigDecimal(0)).toEither.isRight,
          Percentage(BigDecimal(100)).toEither.isRight,
          Percentage(BigDecimal(50)).toEither.isRight,
        )
      },
      test("rejects values outside 0–100") {
        assertTrue(
          Percentage(BigDecimal(-1)).toEither.isLeft,
          Percentage(BigDecimal(101)).toEither.isLeft,
        )
      },
      test("applyTo discounts rather than scales") {
        // 10% off 100 is 90, not 10 — the direction is easy to get backwards
        assertTrue(
          Percentage.unsafe(BigDecimal(10)).applyTo(Money(100)).value == BigDecimal(90),
          Percentage.unsafe(BigDecimal(0)).applyTo(Money(100)).value == BigDecimal(100),
          Percentage.unsafe(BigDecimal(100)).applyTo(Money(100)).value == BigDecimal(0),
        )
      },
    ),
    suite("Quantity")(
      test("must be positive") {
        assertTrue(
          Quantity(1).toEither.isRight,
          Quantity(0).toEither.isLeft,
          Quantity(-5).toEither.isLeft,
        )
      },
    ),
    suite("LocalizedString")(
      test("returns the requested language when present") {
        val ls = LocalizedString("Business Cards", "Vizitky")
        assertTrue(ls(Language.En) == "Business Cards", ls(Language.Cs) == "Vizitky")
      },
      test("falls back to English when a translation is missing") {
        val ls = LocalizedString("Business Cards")
        assertTrue(ls(Language.Cs) == "Business Cards")
      },
      test("returns empty string when even English is missing") {
        val ls = LocalizedString(Map(Language.Cs -> "Vizitky"))
        assertTrue(ls(Language.En) == "")
      },
    ),
    suite("Language")(
      test("round-trips through its code") {
        assertTrue(Language.values.forall(l => Language.fromCode(l.toCode) == l))
      },
      test("defaults to English for unknown codes") {
        assertTrue(Language.fromCode("de") == Language.En, Language.fromCode("") == Language.En)
      },
      test("is case-insensitive") {
        assertTrue(Language.fromCode("CS") == Language.Cs)
      },
    ),
    suite("Timestamp")(
      test("arithmetic composes in the expected units") {
        val t = Timestamp(0)
        assertTrue(
          t.plusSeconds(1).epochMillis == 1_000L,
          t.plusMinutes(1).epochMillis == 60_000L,
          t.plusHours(1).epochMillis == 3_600_000L,
          t.plusDays(1).epochMillis == 86_400_000L,
        )
      },
      test("orders chronologically") {
        val earlier = Timestamp(100)
        val later = Timestamp(200)
        assertTrue(earlier.isBefore(later), later.isAfter(earlier), !earlier.isAfter(earlier))
      },
    ),
    suite("Estimated")(
      test("Known carries the value") {
        val e = Estimated.known(42)
        assertTrue(e.toOption.contains(42), e.reason.isEmpty, e.getOrElse(0) == 42)
      },
      test("Unavailable carries the reason, not just absence") {
        val e = Estimated.unavailable(Estimated.Reason.IndicativeOnly)
        assertTrue(
          e.toOption.isEmpty,
          e.reason.contains(Estimated.Reason.IndicativeOnly),
          e.getOrElse(0) == 0,
        )
      },
      test("map preserves the reason") {
        val e: Estimated[Int] = Estimated.unavailable(Estimated.Reason.ScheduleUnknown)
        assertTrue(e.map(_ + 1).reason.contains(Estimated.Reason.ScheduleUnknown))
      },
    ),
    suite("DomainError")(
      test("derives the English rendering from the localized one") {
        val err = new DomainError:
          def message(lang: Language): String = lang match
            case Language.En => "Not enough paper"
            case Language.Cs => "Nedostatek papíru"
        assertTrue(err.message == "Not enough paper", err.message(Language.Cs) == "Nedostatek papíru")
      }
    ),
  )
