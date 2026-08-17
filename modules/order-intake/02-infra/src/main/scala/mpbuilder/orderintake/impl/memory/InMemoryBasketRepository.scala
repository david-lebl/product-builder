package mpbuilder.orderintake
package impl
package memory

import zio.*

/** Baskets held in a `Ref`, keyed by owner.
  *
  * Keyed by owner rather than by basket id because that is how every operation looks one up — "the
  * basket for this actor". A Postgres repository does the same thing with a unique index.
  */
private[orderintake] final class InMemoryBasketRepository(store: Ref[Map[Basket.Owner, Basket]])
    extends BasketRepository:

  def find(owner: Basket.Owner): UIO[Option[Basket]] = store.get.map(_.get(owner))

  def save(basket: Basket): UIO[Basket] =
    store.update(_ + (basket.data.owner -> basket)).as(basket)

  def delete(owner: Basket.Owner): UIO[Unit] = store.update(_ - owner).unit

private[orderintake] object InMemoryBasketRepository:
  val layer: ULayer[BasketRepository] =
    ZLayer.fromZIO(Ref.make(Map.empty[Basket.Owner, Basket]).map(new InMemoryBasketRepository(_)))

private[orderintake] object RandomIds extends Ids:
  def next: UIO[String] = Random.nextUUID.map(_.toString)
