package mpbuilder.shared

import kyo.*
import kyo.test.*
import java.time.Instant

class SharedPrimitivesSpec extends Test[Any]:

  private final case class TestEvent(name: String, occurredAt: Instant) extends DomainEvent

  "SharedPrimitivesSpec" - {
    "Money & Price" - {
      "rounds HALF_UP to 2 decimal places without floating-point drift" in {
        val a = Money("10.005")
        val b = Money("10.004")
        val c = Money("0.1") + Money("0.2")
        assert(a.rounded == Money("10.01"))
        assert(b.rounded == Money("10.00"))
        assert(c == Money("0.3"))
        assert(Money.nonNegative(BigDecimal("-0.01")).isFailure)
        assert(Money.nonNegative(BigDecimal("0")).isSuccess)
      }

      "Price enforces currency consistency on addition" in {
        val czk1 = Price(Money(100), Currency.CZK)
        val czk2 = Price(Money(50), Currency.CZK)
        val eur1 = Price(Money(10), Currency.EUR)
        assert((czk1 + czk2).toEither == Right(Price(Money(150), Currency.CZK)))
        assert((czk1 + eur1).isFailure)
      }

      "Percentage and TaxRate compute discounts and VAT with HALF_UP rounding" in {
        val pct15 = Percentage.unsafe(BigDecimal(15))
        val net   = Money("199.99")
        assert(Percentage(BigDecimal(-1)).isFailure)
        assert(Percentage(BigDecimal(101)).isFailure)
        assert(pct15.applyDiscount(Money(200)) == Money("170.00"))
        assert(TaxRate.czStandard.taxOnNet(Money(1000)) == Money("210.00"))
        assert(TaxRate.czStandard.netToGross(Money(1000)) == Money("1210.00"))
        assert(TaxRate.czStandard.taxOnNet(net) == Money("42.00"))
      }
    }

    "Measurements & Quantities" - {
      "Dimension2D computes exact area, perimeter, rotation, and containment" in {
        val a4 = Dimension2D.A4 // 210 x 297 mm
        val sra3 = Dimension2D.SRA3 // 320 x 450 mm
        assert(a4.areaSqM.value == BigDecimal("0.06237"))
        assert(a4.perimeterMeters == BigDecimal("1.014"))
        assert(a4.fitsInside(sra3))
        assert(a4.rotated.fitsInside(sra3))
        assert(!sra3.fitsInside(a4))
        assert(Dimension2D.mm(0, 100).isFailure)
        assert(Dimension2D.mm(100, -5).isFailure)
      }

      "Dimension3D approximates flat blank footprint" in {
        val box = Dimension3D(PositiveMm.unsafe(100), PositiveMm.unsafe(50), PositiveMm.unsafe(200))
        val blank = box.approximateBlank2D
        assert(blank.width.value == BigDecimal(330))
        assert(blank.height.value == BigDecimal(330))
      }

      "Quantity and VersionCount enforce positive values and extraVersions" in {
        assert(Quantity(0).isFailure)
        assert(Quantity(-10).isFailure)
        assert(Quantity(100).toEither.map(_.value) == Right(100))
        assert(VersionCount(0).isFailure)
        assert(VersionCount.single.extraVersions == 0)
        assert(VersionCount.unsafe(5).extraVersions == 4)
      }

      "ComponentKey supports custom extensible keys alongside standard keys" in {
        val custom = ComponentKey("presentation-pocket")
        assert(ComponentKey.Cover.value == "cover")
        assert(ComponentKey.Stand.value == "stand")
        assert(custom.toEither.map(_.value) == Right("presentation-pocket"))
        assert(ComponentKey("   ").isFailure)
      }
    }

    "CommonTypes & EventBus" - {
      "LocalizedString falls back cleanly to English" in {
        val ls = LocalizedString("Matte Lamination", "Matná laminace")
        val enOnly = LocalizedString("UV Varnish")
        assert(ls(Language.Cs) == "Matná laminace")
        assert(ls(Language.En) == "Matte Lamination")
        assert(enOnly(Language.Cs) == "UV Varnish")
      }

      "EventBus publishes events to subscribers in-process" in {
        val now = Instant.parse("2026-01-15T10:00:00Z")
        for
          bus    <- EventBus.init
          sub    <- bus.subscribe
          _      <- bus.publish(TestEvent("order.confirmed", now))
          events <- sub.drain
        yield assert(events == Chunk(TestEvent("order.confirmed", now)))
      }
    }
  }
