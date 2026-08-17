package mpbuilder.orderintake
package impl

import mpbuilder.commons.*

private[orderintake] final case class Basket(id: Basket.Id, data: Basket.Data)

private[orderintake] object Basket:

  opaque type Id = String
  object Id:
    def apply(raw: String): Id = raw
    extension (id: Id) def value: String = id

  /** Who the basket belongs to.
    *
    * Replaces what would otherwise be a nullable customer id *and* a nullable session token, with
    * an unwritten rule that exactly one is set.
    */
  enum Owner:
    case Anonymous(session: String)
    case Registered(customerId: String)

  final case class Data(
      owner: Owner,
      items: List[Item],
      createdAt: Timestamp,
      updatedAt: Timestamp,
      expiresAt: Timestamp,
      version: Long,
  )

  final case class Item(id: Item.Id, data: Item.Data)
  object Item:
    opaque type Id = String
    object Id:
      def apply(raw: String): Id = raw
      extension (id: Id) def value: String = id

    final case class Data(
        spec: ProductSpec,
        description: LocalizedString,
        quantity: Int,
        speed: ProductionSpeed,
        artworkId: Option[String],
        price: QuotedPrice,
    )

  extension (b: Basket)
    def isExpired(now: Timestamp): Boolean = b.data.expiresAt.epochMillis <= now.epochMillis

    def currency: Option[Currency] = b.data.items.headOption.map(_.data.price.currency)

    def total: Money =
      b.data.items.map(_.data.price.total).foldLeft(Money.zero)(_ + _)

    def itemCount: Int = b.data.items.size

    def findItem(id: Item.Id): Option[Item] = b.data.items.find(_.id == id)

    def touched(now: Timestamp): Basket =
      b.copy(data = b.data.copy(updatedAt = now, version = b.data.version + 1))

/** Every basket invariant, as pure functions.
  *
  * Deliberately free of effects: these are the rules, and rules should be cheap to test and
  * impossible to accidentally couple to a database or a clock.
  */
private[orderintake] object BasketPolicy:

  /** Chosen to be far above any plausible real order while still bounding a runaway client. */
  val MaxItems = 50

  val MinQuantity = 1
  val MaxQuantity = 1_000_000

  /** Anonymous baskets are cheap to abandon and expensive to keep; a signed-in customer's basket is
    * worth remembering across a holiday.
    */
  def expiryFor(owner: Basket.Owner, now: Timestamp): Timestamp = owner match
    case Basket.Owner.Anonymous(_)  => now.plusDays(30)
    case Basket.Owner.Registered(_) => now.plusDays(180)

  def ownerFor(actor: Actor): Basket.Owner = actor match
    case Actor.Anonymous(session)          => Basket.Owner.Anonymous(session)
    case Actor.Authenticated(userId, cid, _) => Basket.Owner.Registered(cid.getOrElse(userId))

  def validQuantity(quantity: Int): Either[BasketError, Int] =
    if quantity >= MinQuantity && quantity <= MaxQuantity then Right(quantity)
    else Left(BasketError.InvalidQuantity(quantity, MinQuantity, MaxQuantity))

  def canAccept(basket: Basket, incoming: Currency): Either[BasketError, Unit] =
    for
      _ <- Either.cond(
        basket.itemCount < MaxItems,
        (),
        BasketError.TooManyItems(MaxItems),
      )
      _ <- basket.currency match
        case Some(existing) if existing != incoming =>
          Left(BasketError.MixedCurrency(existing, incoming))
        case _ => Right(())
    yield ()

  def notExpired(basket: Basket, now: Timestamp): Either[BasketError, Basket] =
    if basket.isExpired(now) then Left(BasketError.Expired) else Right(basket)

  /** The existing line for the same product, if any.
    *
    * "Same product" means same spec, speed and artwork — a different speed is a genuinely different
    * thing to buy, and must not be folded into an existing line.
    */
  def findMatching(basket: Basket, spec: ProductSpec, speed: ProductionSpeed, artworkId: Option[String])
      : Option[Basket.Item] =
    basket.data.items.find { existing =>
      existing.data.spec.fingerprint == spec.fingerprint &&
        existing.data.speed == speed &&
        existing.data.artworkId == artworkId
    }

  /** Place a line, replacing the one with the same id or appending it.
    *
    * Deliberately takes an already-priced line rather than merging quantities itself: a merged line
    * has a *different* quantity, so it needs a fresh quote, and quoting is an effect this pure
    * policy cannot perform. An earlier version added the quantities here and kept the incoming
    * price, which silently under-charged every merged line.
    */
  def upsert(basket: Basket, item: Basket.Item, now: Timestamp): Basket =
    val items =
      if basket.data.items.exists(_.id == item.id) then
        basket.data.items.map(i => if i.id == item.id then item else i)
      else basket.data.items :+ item
    basket.copy(data = basket.data.copy(items = items)).touched(now)
