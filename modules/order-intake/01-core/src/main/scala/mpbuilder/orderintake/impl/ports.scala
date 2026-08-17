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

private[orderintake] trait Ids:
  def next: UIO[String]
