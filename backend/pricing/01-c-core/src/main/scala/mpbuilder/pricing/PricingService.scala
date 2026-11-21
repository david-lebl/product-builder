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

trait PricingService:
  def quote(cmd: PriceQuoteCmd): IO[PricingError, PriceQuote]
  def analyzeMargin(cmd: PriceQuoteCmd, lowMarginThreshold: Percentage = Percentage.unsafe(BigDecimal(15))): IO[PricingError, MarginAnalysis]

object PricingService:
  def quote(cmd: PriceQuoteCmd): ZIO[PricingService, PricingError, PriceQuote] =
    ZIO.serviceWithZIO[PricingService](_.quote(cmd))
