package mpbuilder.pricing
package impl
package legacy

import mpbuilder.commons.*
import mpbuilder.domain.codec.ConfigurationCodecs.given
import mpbuilder.domain.model as dm
import mpbuilder.domain.pricing as dp
import mpbuilder.domain.sample.{SampleCustomers, SampleDiscountCodes, SamplePricelist, SampleTierRestrictions}
import mpbuilder.domain.service.{DiscountCodeService, DiscountValidationContext, TierRestrictionValidator}
import zio.*
import zio.json.*
import zio.prelude.Validation

/** [[PricingService]] backed by the legacy `domain` pricing engine and the bundled sample
  * pricelists.
  *
  * Replaced wholesale in Phase 8, when the pricing model moves into `pricing/01-core/impl`. Until
  * then this is the only thing in the system that knows both vocabularies.
  */
private[pricing] final class LegacyPricingService(
    pricelists: Map[Currency, dp.Pricelist],
    discountCodes: List[dm.DiscountCode],
    restrictions: List[mpbuilder.domain.manufacturing.TierRestriction],
    customerPricing: String => Option[dp.CustomerPricing],
    context: dp.PricingContext,
) extends PricingService:

  def quote(request: QuoteRequest): IO[PricingError, PriceQuote] =
    for
      config <- decode(request.spec)
      pricelist <- pricelistFor(request.currency, request.customerId, config)
      speed <- ZIO.succeed(Mapping.toDomainSpeed(request.speed))
      breakdown <- price(withSpeed(config, speed), pricelist, request.language)
    yield Mapping.toQuote(breakdown, pricelist.version)

  def quoteAll(requests: List[QuoteRequest]): IO[PricingError, BasketQuote] =
    ZIO.foreach(requests)(quote).map { quotes =>
      val currency = quotes.headOption.map(_.currency).getOrElse(Currency.CZK)
      BasketQuote(
        lines = quotes,
        total = quotes.map(_.total).foldLeft(Money.zero)(_ + _),
        currency = currency,
      )
    }

  def speedOffers(
      spec: ProductSpec,
      currency: Currency,
  ): IO[PricingError, NonEmptyChunk[SpeedOffer]] =
    for
      config <- decode(spec)
      pricelist <- pricelistFor(currency, None, config)
      base <- price(config, pricelist, Language.En)
      offers <- ZIO.foreach(Chunk(ProductionSpeed.Express, ProductionSpeed.Standard, ProductionSpeed.Economy))(
        offerFor(_, config, pricelist, base.total)
      )
    yield NonEmptyChunk.fromChunk(offers).getOrElse(
      NonEmptyChunk(SpeedOffer.Available(ProductionSpeed.Standard, Money.zero))
    )

  def applyDiscount(code: String, ctx: DiscountContext): IO[PricingError, DiscountOutcome] =
    val domainCtx = DiscountValidationContext(
      orderValue = ctx.orderValue,
      categoryIds = ctx.categoryIds.map(dm.CategoryId.unsafe),
      customerType = ctx.customerType.flatMap(t => dm.CustomerType.values.find(_.toString.equalsIgnoreCase(t))),
      customerId = ctx.customerId.map(dm.CustomerId.unsafe),
      now = ctx.now.epochMillis,
    )
    ZIO.succeed(
      DiscountCodeService.applyDiscount(discountCodes, code, ctx.orderValue, domainCtx).toEither match
        case Right(result) =>
          DiscountOutcome.Applied(
            code = result.appliedCode.code,
            discount = result.discountAmount,
            finalTotal = result.finalTotal,
          )
        case Left(errors) =>
          // A refused code is an answer, not a fault — the customer sees why and tries another.
          DiscountOutcome.Refused(
            code = code,
            reason = LocalizedString(
              errors.toList.map(_.message(Language.En)).mkString("; "),
              errors.toList.map(_.message(Language.Cs)).mkString("; "),
            ),
          )
    )

  // ── internals ────────────────────────────────────────────────────────────

  private def offerFor(
      speed: ProductionSpeed,
      config: dm.ProductConfiguration,
      pricelist: dp.Pricelist,
      baseTotal: Money,
  ): IO[PricingError, SpeedOffer] =
    Restrictions.check(speed, config, restrictions, context) match
      case Some(reason) => ZIO.succeed(SpeedOffer.Unavailable(speed, reason))
      case None =>
        price(withSpeed(config, Mapping.toDomainSpeed(speed)), pricelist, Language.En)
          .map(b => SpeedOffer.Available(speed, Money(b.total.value - baseTotal.value)))

  /** The speed tier is carried as a specification, so switching tiers means re-specifying. */
  private def withSpeed(
      config: dm.ProductConfiguration,
      speed: dm.ManufacturingSpeed,
  ): dm.ProductConfiguration =
    config.copy(specifications =
      dm.ProductSpecifications(
        config.specifications.specs + (dm.SpecKind.ManufacturingSpeed -> dm.SpecValue.ManufacturingSpeedSpec(speed))
      )
    )

  private def decode(spec: ProductSpec): IO[PricingError, dm.ProductConfiguration] =
    ZIO
      .fromEither(spec.payload.fromJson[dm.ProductConfiguration])
      .mapError(PricingError.MalformedSpec(_))

  private def pricelistFor(
      currency: Currency,
      customerId: Option[String],
      config: dm.ProductConfiguration,
  ): IO[PricingError, dp.Pricelist] =
    ZIO.fromOption(pricelists.get(currency)).orElseFail(PricingError.NoPricelist(currency)).map { base =>
      // A customer's negotiated pricing is resolved here, inside pricing — never handed out.
      customerId.flatMap(customerPricing) match
        case Some(overlay) =>
          dp.CustomerPricelistResolver.resolve(base, overlay, Some(config.category.id))
        case None => base
    }

  private def price(
      config: dm.ProductConfiguration,
      pricelist: dp.Pricelist,
      lang: Language,
  ): IO[PricingError, dp.PriceBreakdown] =
    toZIO(dp.PriceCalculator.calculateWithContext(config, pricelist, context, lang))

  private def toZIO[A](v: Validation[dp.PricingError, A]): IO[PricingError, A] =
    v.toEither match
      case Right(a) => ZIO.succeed(a)
      case Left(errors) =>
        val problems = NonEmptyChunk
          .fromIterableOption(errors.toList.map(Mapping.toProblem))
          .getOrElse(NonEmptyChunk(Problem("Unknown", "Could not be priced", "Nelze ocenit")))
        ZIO.fail(PricingError.Rejected(problems))

object LegacyPricingService:

  private val samplePricelists: Map[Currency, dp.Pricelist] = Map(
    Currency.CZK -> SamplePricelist.pricelistCzkSheet,
    Currency.USD -> SamplePricelist.pricelist,
  )

  /** Negotiated pricing lives with pricing, keyed by customer id — the customers context stores
    * only the reference. Until there is a store, the sample customers provide the overlays.
    */
  private val sampleCustomerPricing: String => Option[dp.CustomerPricing] =
    val byId = SampleCustomers.all.map(c => c.id.value -> c.pricing).toMap
    id => byId.get(id)

  val layer: ULayer[PricingService] = ZLayer.succeed(
    new LegacyPricingService(
      pricelists = samplePricelists,
      discountCodes = SampleDiscountCodes.all,
      restrictions = SampleTierRestrictions.restrictions,
      customerPricing = sampleCustomerPricing,
      context = dp.PricingContext.default,
    )
  )

  def of(
      pricelists: Map[Currency, dp.Pricelist] = samplePricelists,
      discountCodes: List[dm.DiscountCode] = SampleDiscountCodes.all,
      restrictions: List[mpbuilder.domain.manufacturing.TierRestriction] = SampleTierRestrictions.restrictions,
      customerPricing: String => Option[dp.CustomerPricing] = sampleCustomerPricing,
      context: dp.PricingContext = dp.PricingContext.default,
  ): PricingService =
    new LegacyPricingService(pricelists, discountCodes, restrictions, customerPricing, context)
