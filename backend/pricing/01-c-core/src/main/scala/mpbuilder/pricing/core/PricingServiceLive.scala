package mpbuilder.pricing
package core

import mpbuilder.shared.*
import zio.*

/** Live ZIO implementation of `PricingService` (compile-time isolated inside `pricing`). */
private[pricing] final class PricingServiceLive(
    pricelistRepo: PricelistRepository,
    agreementRepo: CustomerAgreementRepository,
    promoRepo: PromoCodeRepository,
    costSheetRepo: CostSheetRepository,
    capacityPort: ShopCapacityPort,
    eventBus: EventBus,
) extends PricingService:

  override def quote(cmd: PriceQuoteCmd): IO[PricingError, PriceQuote] =
    for
      now       <- Clock.instant
      pricelist <- pricelistRepo.findActive(cmd.currency)
      context   <- capacityPort.currentPricingContext
      agreement <- resolveAgreement(cmd.customerId, cmd.customerTier, cmd.currency)
      promoOpt  <- ZIO.foreach(cmd.promoCode) { codeStr =>
                     promoRepo.findByCode(codeStr).someOrFail(PricingError.PromoCodeNotFound(codeStr))
                   }
      promoCtx   = PromoContext(now = now, customerId = cmd.customerId, customerSegment = cmd.customerSegment)
      q         <- PricingEngine
                     .calculate(cmd.quoteId, cmd.subject, pricelist, context, agreement, promoOpt, Some(promoCtx), now)
                     .fold(
                       errs => ZIO.fail(if errs.size == 1 then errs.head else PricingError.PricingValidationFailed(errs)),
                       ZIO.succeed(_),
                     )
      _         <- eventBus.publish(QuoteIssued(q.quoteId, q.pricelistId, q.currency, q.netTotal, q.grossTotal, now))
    yield q

  override def analyzeMargin(
      cmd: PriceQuoteCmd,
      lowMarginThreshold: Percentage,
  ): IO[PricingError, MarginAnalysis] =
    for
      q             <- quote(cmd)
      costSheetOpt  <- costSheetRepo.findActive(cmd.currency)
      costSheet     <- ZIO.fromOption(costSheetOpt).orElseFail(PricingError.PricelistNotFound(cmd.currency))
      analysis      <- PricingEngine
                         .analyzeMargin(cmd.subject, q, costSheet, lowMarginThreshold)
                         .fold(errs => ZIO.fail(errs.head), ZIO.succeed(_))
    yield analysis

  override def evaluatePromoCode(cmd: EvaluatePromoCmd): IO[PricingError, PromoEvaluation] =
    for
      now   <- Clock.instant
      promo <- promoRepo.findByCode(cmd.code).someOrFail(PricingError.PromoCodeNotFound(cmd.code))
      ctx    = PromoContext(now = now, customerId = cmd.customerId, customerSegment = cmd.customerSegment)
      res   <- PricingEngine
                 .evaluatePromo(promo, cmd.categoryIds, cmd.orderNetTotal, cmd.currency, ctx)
                 .fold(
                   errs => ZIO.fail(if errs.size == 1 then errs.head else PricingError.PricingValidationFailed(errs)),
                   ZIO.succeed(_),
                 )
    yield res

  override def redeemPromoCode(codeId: DiscountCodeId): IO[PricingError, Unit] =
    promoRepo.incrementUsage(codeId)

  override def speedMultipliers(currency: Currency): IO[PricingError.PricelistNotFound, List[SpeedMultiplierSummary]] =
    for
      pricelist <- pricelistRepo.findActive(currency)
      context   <- capacityPort.currentPricingContext
    yield ManufacturingSpeed.values.toList.map { speed =>
      PricingEngine.speedMultiplierSummary(speed, pricelist, context)
    }

  // ── Pricing Administration ─────────────────────────────────────────────────

  override def listPricelists: UIO[List[Pricelist]] =
    pricelistRepo.listAll.map(_.sortBy(_.currency.toString))

  override def savePricelist(pricelist: Pricelist): IO[PricingError, Pricelist] =
    pricelistRepo.save(pricelist).as(pricelist)

  override def listAgreements: UIO[List[CustomerPriceAgreement]] =
    agreementRepo.listAll

  override def saveAgreement(agreement: CustomerPriceAgreement): IO[PricingError, CustomerPriceAgreement] =
    agreementRepo.save(agreement).as(agreement)

  override def deleteAgreement(target: AgreementTarget, currency: Currency): UIO[Unit] =
    agreementRepo.delete(target, currency)

  override def listPromoCodes: UIO[List[PromoCode]] =
    promoRepo.listAll.map(_.sortBy(_.code.value))

  override def savePromoCode(promo: PromoCode): IO[PricingError, PromoCode] =
    promoRepo.save(promo).as(promo)

  override def setPromoCodeActive(id: DiscountCodeId, active: Boolean): IO[PricingError, PromoCode] =
    for
      promo  <- promoRepo.findById(id).someOrFail(PricingError.PromoCodeNotFound(id.value))
      updated = promo.copy(isActive = active)
      _      <- promoRepo.save(updated)
    yield updated

  override def deletePromoCode(id: DiscountCodeId): UIO[Unit] =
    promoRepo.delete(id)

  override def listCostSheets: UIO[List[ProductionCostSheet]] =
    costSheetRepo.listAll.map(_.sortBy(_.currency.toString))

  override def saveCostSheet(sheet: ProductionCostSheet): IO[PricingError, ProductionCostSheet] =
    costSheetRepo.save(sheet).as(sheet)

  private def resolveAgreement(
      customerId: Option[CustomerId],
      customerTier: Option[CustomerTier],
      currency: Currency,
  ): UIO[Option[CustomerPriceAgreement]] =
    customerId match
      case Some(cid) =>
        agreementRepo.findForCustomer(cid, currency).flatMap {
          case some @ Some(_) => ZIO.succeed(some)
          case None           => ZIO.foreach(customerTier)(t => agreementRepo.findForTier(t, currency)).map(_.flatten)
        }
      case None =>
        ZIO.foreach(customerTier)(t => agreementRepo.findForTier(t, currency)).map(_.flatten)

private[pricing] object PricingServiceLive:
  val layer: ZLayer[
    PricelistRepository & CustomerAgreementRepository & PromoCodeRepository & CostSheetRepository & ShopCapacityPort & EventBus,
    Nothing,
    PricingService,
  ] = ZLayer.fromFunction(PricingServiceLive.apply)
