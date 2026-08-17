package mpbuilder.pricing

import mpbuilder.commons.*
import mpbuilder.pricing.impl.legacy.LegacyPricingService
import mpbuilder.domain.codec.ConfigurationCodecs.given
import mpbuilder.domain.model as dm
import mpbuilder.domain.pricing as dp
import mpbuilder.domain.rules.CompatibilityRuleset
import mpbuilder.domain.sample.{SampleCatalog, SampleRules}
import mpbuilder.domain.service.{ConfigurationBuilder, ConfigurationRequest}
import zio.*
import zio.json.*
import zio.test.*

/** Contract tests for [[PricingService]].
  *
  * Written against the public surface. The one concession is building the input spec via the
  * legacy `ConfigurationBuilder` — until catalog is extracted there is no other way to produce a
  * valid payload, and using a hand-written fixture would test the fixture rather than the engine.
  */
object PricingServiceSpec extends ZIOSpecDefault:

  private val service = LegacyPricingService.of()

  private def specFor(quantity: Int, binding: Option[dm.BindingMethod] = None): ProductSpec =
    val specs = List(
      dm.SpecValue.SizeSpec(Dimension(90, 55)),
      dm.SpecValue.QuantitySpec(Quantity.unsafe(quantity)),
    ) ++ binding.map(dm.SpecValue.BindingMethodSpec(_))

    val config = ConfigurationBuilder
      .build(
        ConfigurationRequest(
          categoryId = SampleCatalog.businessCardsId,
          printingMethodId = SampleCatalog.digitalId,
          components = List(
            dm.ComponentRequest(
              dm.ComponentRole.Main,
              SampleCatalog.coated300gsmId,
              dm.InkConfiguration.cmyk4_4,
              Nil,
            )
          ),
          specs = specs,
        ),
        SampleCatalog.catalog,
        SampleRules.ruleset,
        dm.ConfigurationId.unsafe("snapshot"),
      )
      .toEither
      .getOrElse(throw new AssertionError("fixture did not build"))

    ProductSpec(config.toJson, "test-catalog")

  private val standard = specFor(500)

  def spec = suite("PricingService")(
    suite("quote")(
      test("prices a valid product") {
        for q <- service.quote(QuoteRequest(standard))
        yield assertTrue(
          q.total.value > BigDecimal(0),
          q.currency == Currency.CZK,
          q.quantity == 500,
          q.components.nonEmpty,
          q.pricelistVersion.nonEmpty,
        )
      },
      test("the itemized lines are present, not just a total") {
        for q <- service.quote(QuoteRequest(standard))
        yield assertTrue(
          q.components.head.lines.nonEmpty,
          q.components.head.lines.forall(_.label.nonEmpty),
        )
      },
      test("Express costs more than Standard, Economy less") {
        for
          express <- service.quote(QuoteRequest(standard, speed = ProductionSpeed.Express))
          std <- service.quote(QuoteRequest(standard, speed = ProductionSpeed.Standard))
          economy <- service.quote(QuoteRequest(standard, speed = ProductionSpeed.Economy))
        yield assertTrue(
          express.total.value > std.total.value,
          economy.total.value < std.total.value,
        )
      },
      test("reports a missing pricelist rather than guessing a currency") {
        for error <- service.quote(QuoteRequest(standard, currency = Currency.GBP)).flip
        yield assertTrue(error == PricingError.NoPricelist(Currency.GBP))
      },
      test("corrupt stored data is reported as malformed, not as a pricing failure") {
        for error <- service.quote(QuoteRequest(ProductSpec("{nope}", "v1"))).flip
        yield assertTrue(error.isInstanceOf[PricingError.MalformedSpec])
      },
      test("errors are bilingual") {
        for error <- service.quote(QuoteRequest(standard, currency = Currency.EUR)).flip
        yield assertTrue(
          error.message(Language.En).nonEmpty,
          error.message(Language.Cs) != error.message(Language.En),
        )
      },
    ),
    suite("quoteAll")(
      test("sums the lines") {
        for
          one <- service.quote(QuoteRequest(standard))
          basket <- service.quoteAll(List(QuoteRequest(standard), QuoteRequest(standard)))
        yield assertTrue(
          basket.lines.size == 2,
          basket.total.value == one.total.value * 2,
        )
      },
      test("fails the whole basket if one line cannot be priced") {
        // A total that silently omitted a line would be worse than no total.
        for result <- service
            .quoteAll(List(QuoteRequest(standard), QuoteRequest(ProductSpec("{nope}", "v1"))))
            .exit
        yield assertTrue(result.isFailure)
      },
    ),
    suite("speedOffers")(
      test("offers all three tiers when nothing is restricted") {
        for offers <- service.speedOffers(standard, Currency.CZK)
        yield assertTrue(
          offers.size == 3,
          offers.exists {
            case SpeedOffer.Available(ProductionSpeed.Express, _) => true
            case _                                                => false
          },
        )
      },
      test("withdraws Express when the shop is saturated, and says why") {
        val saturated = LegacyPricingService.of(
          context = dp.PricingContext.default.copy(globalUtilisation = BigDecimal("0.99"))
        )
        for offers <- saturated.speedOffers(standard, Currency.CZK)
        yield assertTrue(
          offers.exists {
            case SpeedOffer.Unavailable(ProductionSpeed.Express, SpeedUnavailable.ShopSaturated) => true
            case _                                                                                => false
          },
          // Standard is always still on the table.
          offers.exists {
            case SpeedOffer.Available(ProductionSpeed.Standard, _) => true
            case _                                                 => false
          },
        )
      },
      test("an unavailable tier carries an explanation the UI can show") {
        val saturated = LegacyPricingService.of(
          context = dp.PricingContext.default.copy(globalUtilisation = BigDecimal("0.99"))
        )
        for offers <- saturated.speedOffers(standard, Currency.CZK)
        yield assertTrue(offers.exists {
          case SpeedOffer.Unavailable(_, _) => true
          case _                            => false
        })
      },
    ),
    suite("applyDiscount")(
      test("a refused code is an outcome, not an error") {
        // The customer mistyped; that is not a fault, and must not blow up the checkout.
        for outcome <- service.applyDiscount(
            "DEFINITELY-NOT-A-CODE",
            DiscountContext(Money(1000), Set.empty, now = Timestamp(0)),
          )
        yield assertTrue(outcome match
          case DiscountOutcome.Refused(_, reason) =>
            reason(Language.En).nonEmpty && reason(Language.Cs) != reason(Language.En)
          case _ => false
        )
      },
      test("a valid code reduces the total") {
        // An unconstrained, always-valid code, so the assertion does not depend on the clock.
        val unconstrained = mpbuilder.domain.sample.SampleDiscountCodes.all.find { c =>
          c.isActive &&
          c.constraints.validFrom.isEmpty && c.constraints.validUntil.isEmpty &&
          c.constraints.minimumOrderValue.isEmpty &&
          c.constraints.maxUses.isEmpty &&
          c.constraints.allowedCategories.isEmpty &&
          c.constraints.allowedCustomerTypes.isEmpty &&
          c.constraints.allowedCustomerIds.isEmpty
        }

        ZIO
          .fromOption(unconstrained)
          .orElseFail(new AssertionError("sample data has no unconstrained discount code"))
          .flatMap { code =>
            service
              .applyDiscount(code.code, DiscountContext(Money(10000), Set.empty, now = Timestamp(0)))
              .map { outcome =>
                assertTrue(outcome match
                  case DiscountOutcome.Applied(applied, discount, finalTotal) =>
                    applied.equalsIgnoreCase(code.code) &&
                    discount.value > BigDecimal(0) &&
                    finalTotal.value < BigDecimal(10000)
                  case DiscountOutcome.Refused(_, _) => false
                )
              }
          }
          .mapError(e => new RuntimeException(e.toString))
          .orDie
      },
      test("an expired code is refused") {
        val expired = mpbuilder.domain.sample.SampleDiscountCodes.all
          .find(c => c.constraints.validUntil.exists(_ < java.lang.System.currentTimeMillis))

        ZIO
          .fromOption(expired)
          .orElseFail(new AssertionError("sample data has no expired discount code"))
          .flatMap { code =>
            service
              .applyDiscount(
                code.code,
                DiscountContext(Money(10000), Set.empty, now = Timestamp(java.lang.System.currentTimeMillis)),
              )
              .map(outcome => assertTrue(outcome.isInstanceOf[DiscountOutcome.Refused]))
          }
          .mapError(e => new RuntimeException(e.toString))
          .orDie
      },
    ),
  )
