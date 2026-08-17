package mpbuilder.orderintake
package impl

import mpbuilder.commons.*
import zio.*

/** Fetching the actor's basket, creating one if absent and replacing it if expired.
  *
  * Shared by the basket and checkout services so the two cannot disagree about what "the current
  * basket" is — an expired basket that checkout still priced would be the worst possible version of
  * that disagreement.
  */
private[orderintake] final class Baskets(
    repository: BasketRepository,
    ids: Ids,
    now: UIO[Timestamp],
):

  /** An expired basket is discarded rather than refused: the customer's prices are stale, but
    * nothing about that should stop them starting again.
    */
  def load(actor: Actor): UIO[Basket] =
    val owner = BasketPolicy.ownerFor(actor)
    for
      timestamp <- now
      existing <- repository.find(owner)
      basket <- existing.filterNot(_.isExpired(timestamp)) match
        case Some(live) => ZIO.succeed(live)
        case None       => fresh(owner, timestamp).flatMap(repository.save)
    yield basket

  def save(basket: Basket): UIO[Basket] = repository.save(basket)

  def find(owner: Basket.Owner): UIO[Option[Basket]] = repository.find(owner)

  def delete(owner: Basket.Owner): UIO[Unit] = repository.delete(owner)

  private def fresh(owner: Basket.Owner, timestamp: Timestamp): UIO[Basket] =
    ids.next.map { id =>
      Basket(
        Basket.Id(id),
        Basket.Data(
          owner = owner,
          items = Nil,
          createdAt = timestamp,
          updatedAt = timestamp,
          expiresAt = BasketPolicy.expiryFor(owner, timestamp),
          version = 0,
        ),
      )
    }
