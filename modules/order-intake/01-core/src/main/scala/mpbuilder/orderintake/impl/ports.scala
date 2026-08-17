package mpbuilder.orderintake
package impl

import mpbuilder.commons.*
import zio.{IO, UIO}

private[orderintake] trait BasketRepository:
  def find(owner: Basket.Owner): UIO[Option[Basket]]
  def save(basket: Basket): UIO[Basket]
  def delete(owner: Basket.Owner): UIO[Unit]

/** What order-intake needs to know about products, in order-intake's own words.
  *
  * Nothing here mentions catalog. An adapter in `02-infra` satisfies it by calling catalog's public
  * service; when catalog becomes a separate deployable, only that adapter changes.
  */
private[orderintake] trait ProductPort:
  /** Turn a client's configuration request into an opaque, storable spec. */
  def specFor(configurationJson: String): IO[BasketError, (ProductSpec, LocalizedString)]

  /** Is this spec still buildable against today's catalog? */
  def stillBuildable(spec: ProductSpec): IO[BasketError, Unit]

/** What order-intake needs to know about prices, in order-intake's own words. */
private[orderintake] trait QuotePort:
  def quote(
      spec: ProductSpec,
      quantity: Int,
      speed: ProductionSpeed,
      customerId: Option[String],
      currency: Currency,
  ): IO[BasketError, QuotedPrice]

  /** Every speed tier for this product: what it costs, or why it cannot be sold.
    *
    * Order-intake never decides this. Whether the shop floor can promise a rush turnaround is a
    * pricing-and-capacity question, and duplicating the rule here is exactly how the two answers
    * would drift apart.
    */
  def speedOffers(spec: ProductSpec, currency: Currency): IO[BasketError, List[SpeedOfferView]]

/** Offering a discount code. Separate from [[QuotePort]] because a discount is a decision about the
  * whole basket, not a price for one line — and because the two may well end up in different
  * services.
  */
private[orderintake] trait DiscountPort:
  def offer(
      code: String,
      orderValue: Money,
      specs: List[ProductSpec],
      buyer: Buyer,
      now: Timestamp,
  ): IO[CheckoutError, DiscountDecision]

/** Who is buying, in order-intake's own words. Nothing here mentions the customers context. */
private[orderintake] trait BuyerPort:
  /** The buyer behind an actor. An actor with no customer record is a guest, not a failure. */
  def resolve(actor: Actor): IO[CheckoutError, Buyer]

/** The delivery options the shop offers.
  *
  * A port rather than a constant because pickup points open and close and courier prices change —
  * this is configuration, and configuration belongs behind a seam even while it is hardcoded.
  */
private[orderintake] trait DeliveryCatalog:
  def options(currency: Currency): UIO[List[DeliveryOption]]

private[orderintake] trait Ids:
  def next: UIO[String]
