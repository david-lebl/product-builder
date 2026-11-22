package mpbuilder.pricing

import mpbuilder.shared.*
import zio.*
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
  def quote(cmd: PriceQuoteCmd): IO[PricingError, PriceQuote]
  def analyzeMargin(cmd: PriceQuoteCmd, lowMarginThreshold: Percentage = Percentage.unsafe(BigDecimal(15))): IO[PricingError, MarginAnalysis]
  def evaluatePromoCode(cmd: EvaluatePromoCmd): IO[PricingError, PromoEvaluation]
  def redeemPromoCode(codeId: DiscountCodeId): IO[PricingError, Unit]
  def speedMultipliers(currency: Currency): IO[PricingError.PricelistNotFound, List[SpeedMultiplierSummary]]

  // ── Pricing Administration ─────────────────────────────────────────────────
  def listPricelists: UIO[List[Pricelist]]
  def savePricelist(pricelist: Pricelist): IO[PricingError, Pricelist]
  def listAgreements: UIO[List[CustomerPriceAgreement]]
  def saveAgreement(agreement: CustomerPriceAgreement): IO[PricingError, CustomerPriceAgreement]
  def deleteAgreement(target: AgreementTarget, currency: Currency): UIO[Unit]
  def listPromoCodes: UIO[List[PromoCode]]
  def savePromoCode(promo: PromoCode): IO[PricingError, PromoCode]
  def setPromoCodeActive(id: DiscountCodeId, active: Boolean): IO[PricingError, PromoCode]
  def deletePromoCode(id: DiscountCodeId): UIO[Unit]
  def listCostSheets: UIO[List[ProductionCostSheet]]
  def saveCostSheet(sheet: ProductionCostSheet): IO[PricingError, ProductionCostSheet]

object PricingService:
  def quote(cmd: PriceQuoteCmd): ZIO[PricingService, PricingError, PriceQuote] =
    ZIO.serviceWithZIO[PricingService](_.quote(cmd))
