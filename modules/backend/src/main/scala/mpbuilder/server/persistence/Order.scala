package mpbuilder.server.persistence

import java.time.Instant
import java.util.UUID

import mpbuilder.api.OrderStatus
import mpbuilder.domain.Currency
import mpbuilder.domain.config.{CustomerContact, ProductConfiguration}
import mpbuilder.domain.pricing.PriceBreakdown

/** A placed order as the backend sees it — the customer's configuration plus
  * the authoritative price snapshot taken at submission time.
  */
final case class Order(
  id: UUID,
  createdAt: Instant,
  contact: CustomerContact,
  configuration: ProductConfiguration,
  breakdown: PriceBreakdown,
  status: OrderStatus,
):
  def currency: Currency        = breakdown.currency
  def totalAmount: BigDecimal   = breakdown.total.amount
