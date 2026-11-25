package mpbuilder.pricing
package core

import mpbuilder.shared.*
import kyo.*
import java.time.Instant

/** Live Kyo implementation of `PricingService` (compile-time isolated inside `pricing`). */
private[pricing] final class PricingServiceLive(
    pricelistRepo: PricelistRepository,
    agreementRepo: CustomerAgreementRepository,
    promoRepo: PromoCodeRepository,
    costSheetRepo: CostSheetRepository,
    capacityPort: ShopCapacityPort,
    eventBus: EventBus,
) extends PricingService:

  override def quote(cmd: PriceQuoteCmd): PriceQuote < (Async & Abort[PricingError]) =
    for
      now       <- Effects.now
      pricelist <- pricelistRepo.findActive(cmd.currency)
      context   <- capacityPort.currentPricingContext
      agreement <- resolveAgreement(cmd.customerId, cmd.customerTier, cmd.currency)
      promoOpt  <- Effects.traverse(cmd.promoCode) { codeStr =>
                     promoRepo.findByCode(codeStr).someOrFail(PricingError.PromoCodeNotFound(codeStr))
                   }
      promoCtx   = PromoContext(now = now, customerId = cmd.customerId, customerSegment = cmd.customerSegment)
      q         <- PricingEngine
                     .calculate(cmd.quoteId, cmd.subject, pricelist, context, agreement, promoOpt, Some(promoCtx), now)
                     .fold(
                       errs => Abort.fail(if errs.size == 1 then errs.head else PricingError.PricingValidationFailed(errs)),
                       Kyo.lift(_),
                     )
      _         <- eventBus.publish(QuoteIssued(q.quoteId, q.pricelistId, q.currency, q.netTotal, q.grossTotal, now))
    yield q

  override def analyzeMargin(
      cmd: PriceQuoteCmd,
      lowMarginThreshold: Percentage,
  ): MarginAnalysis < (Async & Abort[PricingError]) =
    for
      q             <- quote(cmd)
      costSheetOpt  <- costSheetRepo.findActive(cmd.currency)
      costSheet     <- costSheetOpt.orFail(PricingError.PricelistNotFound(cmd.currency))
      analysis      <- PricingEngine
                         .analyzeMargin(cmd.subject, q, costSheet, lowMarginThreshold)
                         .fold(errs => Abort.fail(errs.head), Kyo.lift(_))
    yield analysis

  override def evaluatePromoCode(cmd: EvaluatePromoCmd): PromoEvaluation < (Async & Abort[PricingError]) =
    for
      now   <- Effects.now
      promo <- promoRepo.findByCode(cmd.code).someOrFail(PricingError.PromoCodeNotFound(cmd.code))
      ctx    = PromoContext(now = now, customerId = cmd.customerId, customerSegment = cmd.customerSegment)
      res   <- PricingEngine
                 .evaluatePromo(promo, cmd.categoryIds, cmd.orderNetTotal, cmd.currency, ctx)
                 .fold(
                   errs => Abort.fail(if errs.size == 1 then errs.head else PricingError.PricingValidationFailed(errs)),
                   Kyo.lift(_),
                 )
    yield res

  override def redeemPromoCode(codeId: DiscountCodeId): Unit < (Async & Abort[PricingError]) =
    promoRepo.incrementUsage(codeId)

  override def speedMultipliers(currency: Currency): List[SpeedMultiplierSummary] < (Async & Abort[PricingError.PricelistNotFound]) =
    for
      pricelist <- pricelistRepo.findActive(currency)
      context   <- capacityPort.currentPricingContext
    yield ManufacturingSpeed.values.toList.map { speed =>
      PricingEngine.speedMultiplierSummary(speed, pricelist, context)
    }

  // ── Pricing Administration ─────────────────────────────────────────────────

  override def listPricelists: List[Pricelist] < Async =
    pricelistRepo.listAll.map(_.sortBy(_.currency.toString))

  override def savePricelist(pricelist: Pricelist): Pricelist < (Async & Abort[PricingError]) =
    pricelistRepo.save(pricelist).as(pricelist)

  override def listAgreements: List[CustomerPriceAgreement] < Async =
    agreementRepo.listAll

  override def saveAgreement(agreement: CustomerPriceAgreement): CustomerPriceAgreement < (Async & Abort[PricingError]) =
    agreementRepo.save(agreement).as(agreement)

  override def deleteAgreement(target: AgreementTarget, currency: Currency): Unit < Async =
    agreementRepo.delete(target, currency)

  override def listPromoCodes: List[PromoCode] < Async =
    promoRepo.listAll.map(_.sortBy(p => (p.createdAt, p.code.value))(using Ordering.Tuple2(using Ordering[Instant].reverse, Ordering.String)))

  override def savePromoCode(promo: PromoCode): PromoCode < (Async & Abort[PricingError]) =
    for
      now      <- Effects.now
      existing <- promoRepo.findById(promo.id)
      stamped   = existing match
                    case Some(prev) if promo.createdAt == Instant.EPOCH => promo.copy(createdAt = prev.createdAt)
                    case None if promo.createdAt == Instant.EPOCH       => promo.copy(createdAt = now)
                    case _                                              => promo
      _        <- promoRepo.save(stamped)
    yield stamped

  override def setPromoCodeActive(id: DiscountCodeId, active: Boolean): PromoCode < (Async & Abort[PricingError]) =
    for
      promo  <- promoRepo.findById(id).someOrFail(PricingError.PromoCodeNotFound(id.value))
      updated = promo.copy(isActive = active)
      _      <- promoRepo.save(updated)
    yield updated

  override def deletePromoCode(id: DiscountCodeId): Unit < Async =
    promoRepo.delete(id)

  override def listCostSheets: List[ProductionCostSheet] < Async =
    costSheetRepo.listAll.map(_.sortBy(_.currency.toString))

  override def saveCostSheet(sheet: ProductionCostSheet): ProductionCostSheet < (Async & Abort[PricingError]) =
    costSheetRepo.save(sheet).as(sheet)

  private def resolveAgreement(
      customerId: Option[CustomerId],
      customerTier: Option[CustomerTier],
      currency: Currency,
  ): Option[CustomerPriceAgreement] < Async =
    customerId match
      case Some(cid) =>
        agreementRepo.findForCustomer(cid, currency).flatMap {
          case some @ Some(_) => Kyo.lift(some)
          case None           => Effects.traverse(customerTier)(t => agreementRepo.findForTier(t, currency)).map(_.flatten)
        }
      case None =>
        Effects.traverse(customerTier)(t => agreementRepo.findForTier(t, currency)).map(_.flatten)

private[pricing] object PricingServiceLive:
  def make(
      pricelists: PricelistRepository,
      agreements: CustomerAgreementRepository,
      promoCodes: PromoCodeRepository,
      costSheets: CostSheetRepository,
      capacity: ShopCapacityPort,
      eventBus: EventBus,
  ): PricingService = PricingServiceLive(pricelists, agreements, promoCodes, costSheets, capacity, eventBus)
