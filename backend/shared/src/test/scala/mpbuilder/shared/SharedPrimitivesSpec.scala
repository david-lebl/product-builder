package mpbuilder.shared

import zio.*
import zio.test.*
import java.time.Instant

object SharedPrimitivesSpec extends ZIOSpecDefault:

  private final case class TestEvent(name: String, occurredAt: Instant) extends DomainEvent

  def spec = suite("SharedPrimitivesSpec")(
    suite("Money & Price")(
      test("rounds HALF_UP to 2 decimal places without floating-point drift") {
        val a = Money("10.005")
        val b = Money("10.004")
        val c = Money("0.1") + Money("0.2")
        assertTrue(
          a.rounded == Money("10.01"),
          b.rounded == Money("10.00"),
          c == Money("0.3"),
          Money.nonNegative(BigDecimal("-0.01")).isFailure,
          Money.nonNegative(BigDecimal("0")).isSuccess,
        )
      },
      test("Price enforces currency consistency on addition") {
        val czk1 = Price(Money(100), Currency.CZK)
        val czk2 = Price(Money(50), Currency.CZK)
        val eur1 = Price(Money(10), Currency.EUR)
        assertTrue(
          (czk1 + czk2).toEither == Right(Price(Money(150), Currency.CZK)),
          (czk1 + eur1).isFailure,
        )
      },
      test("Percentage and TaxRate compute discounts and VAT with HALF_UP rounding") {
        val pct15 = Percentage.unsafe(BigDecimal(15))
        val net   = Money("199.99")
        assertTrue(
          Percentage(BigDecimal(-1)).isFailure,
          Percentage(BigDecimal(101)).isFailure,
          pct15.applyDiscount(Money(200)) == Money("170.00"),
          TaxRate.czStandard.taxOnNet(Money(1000)) == Money("210.00"),
          TaxRate.czStandard.netToGross(Money(1000)) == Money("1210.00"),
          TaxRate.czStandard.taxOnNet(net) == Money("42.00"),
        )
      },
    ),
    suite("Measurements & Quantities")(
      test("Dimension2D computes exact area, perimeter, rotation, and containment") {
        val a4 = Dimension2D.A4 // 210 x 297 mm
        val sra3 = Dimension2D.SRA3 // 320 x 450 mm
        assertTrue(
          a4.areaSqM.value == BigDecimal("0.06237"),
          a4.perimeterMeters == BigDecimal("1.014"),
          a4.fitsInside(sra3),
          a4.rotated.fitsInside(sra3),
          !sra3.fitsInside(a4),
          Dimension2D.mm(0, 100).isFailure,
          Dimension2D.mm(100, -5).isFailure,
        )
      },
      test("Dimension3D approximates flat blank footprint") {
        val box = Dimension3D(PositiveMm.unsafe(100), PositiveMm.unsafe(50), PositiveMm.unsafe(200))
        val blank = box.approximateBlank2D
        assertTrue(
          blank.width.value == BigDecimal(330),
          blank.height.value == BigDecimal(330),
        )
      },
      test("Quantity and VersionCount enforce positive values and extraVersions") {
        assertTrue(
          Quantity(0).isFailure,
          Quantity(-10).isFailure,
          Quantity(100).toEither.map(_.value) == Right(100),
          VersionCount(0).isFailure,
          VersionCount.single.extraVersions == 0,
          VersionCount.unsafe(5).extraVersions == 4,
        )
      },
      test("ComponentKey supports custom extensible keys alongside standard keys") {
        val custom = ComponentKey("presentation-pocket")
        assertTrue(
          ComponentKey.Cover.value == "cover",
          ComponentKey.Stand.value == "stand",
          custom.toEither.map(_.value) == Right("presentation-pocket"),
          ComponentKey("   ").isFailure,
        )
      },
    ),
    suite("CommonTypes & EventBus")(
      test("LocalizedString falls back cleanly to English") {
        val ls = LocalizedString("Matte Lamination", "Matná laminace")
        val enOnly = LocalizedString("UV Varnish")
        assertTrue(
          ls(Language.Cs) == "Matná laminace",
          ls(Language.En) == "Matte Lamination",
          enOnly(Language.Cs) == "UV Varnish",
        )
      },
      test("EventBus publishes events to subscribers in-process") {
        val now = Instant.parse("2026-01-15T10:00:00Z")
        ZIO.scoped {
          for
            bus   <- ZIO.service[EventBus]
            queue <- bus.subscribe
            _     <- bus.publish(TestEvent("order.confirmed", now))
            ev    <- queue.take
          yield assertTrue(ev == TestEvent("order.confirmed", now))
        }.provide(EventBus.live)
      },
    ),
  )
