package mpbuilder.orderintake
package impl

import mpbuilder.commons.*
import zio.*

/** Orchestration only. Every rule lives in [[BasketPolicy]] or behind a port. */
private[orderintake] final class BasketServiceLive(
    baskets: BasketRepository,
    products: ProductPort,
    quotes: QuotePort,
    ids: Ids,
    now: UIO[Timestamp],
    defaultCurrency: Currency,
) extends BasketService:

  def current(actor: Actor): IO[BasketError, BasketView] =
    load(actor).map(toView)

  def addItem(actor: Actor, input: AddItem): IO[BasketError, BasketView] =
    for
      quantity <- ZIO.fromEither(BasketPolicy.validQuantity(input.quantity))
      speed <- ZIO
        .fromOption(ProductionSpeed.parse(input.speed))
        .orElseFail(BasketError.UnknownValue("speed", input.speed))
      basket <- load(actor)
      specAndText <- products.specFor(input.configuration)
      (spec, description) = specAndText
      existing = BasketPolicy.findMatching(basket, spec, speed, input.artworkId)
      // Folding into an existing line changes its quantity, so the whole line is re-quoted at the
      // combined amount. Keeping the incoming quote would price four copies as two.
      combined = existing.map(_.data.quantity).getOrElse(0) + quantity
      price <- quotes.quote(spec, combined, speed, customerIdOf(actor), defaultCurrency)
      _ <- ZIO.fromEither(BasketPolicy.canAccept(basket, price.currency)).when(existing.isEmpty)
      item <- existing match
        case Some(line) =>
          ZIO.succeed(line.copy(data = line.data.copy(quantity = combined, price = price)))
        case None =>
          ids.next.map(id =>
            Basket.Item(
              Basket.Item.Id(id),
              Basket.Item.Data(spec, description, combined, speed, input.artworkId, price),
            )
          )
      timestamp <- now
      saved <- baskets.save(BasketPolicy.upsert(basket, item, timestamp))
    yield toView(saved)

  def updateQuantity(actor: Actor, itemId: String, input: UpdateQuantity): IO[BasketError, BasketView] =
    for
      quantity <- ZIO.fromEither(BasketPolicy.validQuantity(input.quantity))
      basket <- load(actor)
      item <- ZIO
        .fromOption(basket.findItem(Basket.Item.Id(itemId)))
        .orElseFail(BasketError.ItemNotFound(itemId))
      // Quantity drives tier discounts and setup-fee amortisation, so changing it changes the
      // price. Keeping the old quote would show the customer a total that is simply wrong.
      price <- quotes.quote(
        item.data.spec,
        quantity,
        item.data.speed,
        customerIdOf(actor),
        basket.currency.getOrElse(defaultCurrency),
      )
      timestamp <- now
      updated = basket.data.items.map { i =>
        if i.id == item.id then i.copy(data = i.data.copy(quantity = quantity, price = price)) else i
      }
      saved <- baskets.save(basket.copy(data = basket.data.copy(items = updated)).touched(timestamp))
    yield toView(saved)

  def removeItem(actor: Actor, itemId: String): IO[BasketError, BasketView] =
    for
      basket <- load(actor)
      _ <- ZIO
        .fromOption(basket.findItem(Basket.Item.Id(itemId)))
        .orElseFail(BasketError.ItemNotFound(itemId))
      timestamp <- now
      remaining = basket.data.items.filterNot(_.id == Basket.Item.Id(itemId))
      saved <- baskets.save(basket.copy(data = basket.data.copy(items = remaining)).touched(timestamp))
    yield toView(saved)

  def clear(actor: Actor): IO[BasketError, BasketView] =
    for
      basket <- load(actor)
      timestamp <- now
      saved <- baskets.save(basket.copy(data = basket.data.copy(items = Nil)).touched(timestamp))
    yield toView(saved)

  def requote(actor: Actor): IO[BasketError, BasketView] =
    for
      basket <- load(actor)
      currency = basket.currency.getOrElse(defaultCurrency)
      repriced <- ZIO.foreach(basket.data.items) { item =>
        // A line that can no longer be built is a real answer, not a fault to swallow: the
        // customer has to be told before they try to pay for it.
        products.stillBuildable(item.data.spec) *>
          quotes
            .quote(item.data.spec, item.data.quantity, item.data.speed, customerIdOf(actor), currency)
            .map(price => item.copy(data = item.data.copy(price = price)))
      }
      timestamp <- now
      saved <- baskets.save(basket.copy(data = basket.data.copy(items = repriced)).touched(timestamp))
    yield toView(saved)

  def merge(actor: Actor, fromSession: String): IO[BasketError, BasketView] =
    for
      target <- load(actor)
      source <- baskets.find(Basket.Owner.Anonymous(fromSession))
      timestamp <- now
      merged <- ZIO.foldLeft(source.toList.flatMap(_.data.items))(target) { (acc, incoming) =>
        val d = incoming.data
        BasketPolicy.findMatching(acc, d.spec, d.speed, d.artworkId) match
          case Some(line) =>
            // Same product on both sides: quantities add, so the combined line is re-quoted —
            // for the same reason `addItem` does.
            val combined = line.data.quantity + d.quantity
            quotes
              .quote(d.spec, combined, d.speed, customerIdOf(actor), defaultCurrency)
              .map(price =>
                BasketPolicy.upsert(
                  acc,
                  line.copy(data = line.data.copy(quantity = combined, price = price)),
                  timestamp,
                )
              )
          case None => ZIO.succeed(BasketPolicy.upsert(acc, incoming, timestamp))
      }
      saved <- baskets.save(merged)
      // The anonymous basket is gone once folded in, so a shared or replayed session token cannot
      // resurrect items into someone else's basket a second time.
      _ <- ZIO.foreachDiscard(source)(_ => baskets.delete(Basket.Owner.Anonymous(fromSession)))
    yield toView(saved)

  // ── internals ────────────────────────────────────────────────────────────

  /** Fetches the actor's basket, creating one if absent and replacing it if expired.
    *
    * An expired basket is discarded rather than refused: the customer's prices are stale, but
    * nothing about that should stop them starting again.
    */
  private def load(actor: Actor): IO[BasketError, Basket] =
    val owner = BasketPolicy.ownerFor(actor)
    for
      timestamp <- now
      existing <- baskets.find(owner)
      basket <- existing.filterNot(_.isExpired(timestamp)) match
        case Some(live) => ZIO.succeed(live)
        case None       => fresh(owner, timestamp).flatMap(baskets.save)
    yield basket

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

  private def customerIdOf(actor: Actor): Option[String] = actor match
    case Actor.Anonymous(_)                    => None
    case Actor.Authenticated(_, customerId, _) => customerId

  private def toView(basket: Basket): BasketView =
    BasketView(
      id = basket.id.value,
      items = basket.data.items.map { item =>
        BasketItemView(
          id = item.id.value,
          description = item.data.description,
          quantity = item.data.quantity,
          speed = item.data.speed.toString,
          artworkId = item.data.artworkId,
          price = item.data.price,
        )
      },
      total = basket.total,
      currency = basket.currency.getOrElse(defaultCurrency),
      itemCount = basket.itemCount,
      expiresAt = basket.data.expiresAt,
    )
