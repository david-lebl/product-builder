package mpbuilder.pricing

import mpbuilder.shared.*
import kyo.*
import java.time.Instant

// ── Domain Events ────────────────────────────────────────────────────────────

final case class QuoteIssued(
    quoteId: QuoteId,
    pricelistId: PricelistId,
    currency: Currency,
    netTotal: Money,
    grossTotal: Money,
    occurredAt: Instant,
) extends DomainEvent

// ── Public Inbound Port (Module API) ─────────────────────────────────────────

final case class PriceQuoteCmd(
    quoteId: QuoteId,
    subject: PricingSubject,
    currency: Currency,
    customerId: Option[CustomerId] = None,
    customerTier: Option[CustomerTier] = None,
    customerSegment: Option[CustomerSegment] = None,
    promoCode: Option[String] = None,
)

final case class EvaluatePromoCmd(
    code: String,
    orderNetTotal: Money,
    categoryIds: Set[CategoryId],
    currency: Currency,
    customerId: Option[CustomerId] = None,
    customerSegment: Option[CustomerSegment] = None,
)

trait PricingService:
  def quote(cmd: PriceQuoteCmd): PriceQuote < (Async & Abort[PricingError])
  def analyzeMargin(cmd: PriceQuoteCmd, lowMarginThreshold: Percentage = Percentage.unsafe(BigDecimal(15))): MarginAnalysis < (Async & Abort[PricingError])
  def evaluatePromoCode(cmd: EvaluatePromoCmd): PromoEvaluation < (Async & Abort[PricingError])
  def redeemPromoCode(codeId: DiscountCodeId): Unit < (Async & Abort[PricingError])
  def speedMultipliers(currency: Currency): List[SpeedMultiplierSummary] < (Async & Abort[PricingError.PricelistNotFound])

  // ── Pricing Administration ─────────────────────────────────────────────────
  def listPricelists: List[Pricelist] < Async
  def savePricelist(pricelist: Pricelist): Pricelist < (Async & Abort[PricingError])
  def listAgreements: List[CustomerPriceAgreement] < Async
  def saveAgreement(agreement: CustomerPriceAgreement): CustomerPriceAgreement < (Async & Abort[PricingError])
  def deleteAgreement(target: AgreementTarget, currency: Currency): Unit < Async
  def listPromoCodes: List[PromoCode] < Async
  def savePromoCode(promo: PromoCode): PromoCode < (Async & Abort[PricingError])
  def setPromoCodeActive(id: DiscountCodeId, active: Boolean): PromoCode < (Async & Abort[PricingError])
  def deletePromoCode(id: DiscountCodeId): Unit < Async
  def listCostSheets: List[ProductionCostSheet] < Async
  def saveCostSheet(sheet: ProductionCostSheet): ProductionCostSheet < (Async & Abort[PricingError])
