package mpbuilder.orderintake

import mpbuilder.commons.*
import mpbuilder.orderintake.impl.*
import zio.*
import zio.test.*

/** What placing an order tells the rest of the system.
  *
  * Worth its own suite because nothing consumes these events yet: manufacturing subscribes in the
  * next phase. An event that quietly stopped being published would be discovered then, by which
  * point the change that broke it is cold.
  *
  * Wired to hand-written fakes rather than through the module, because the point is to watch the
  * ports — the outbox in particular — and the module deliberately hides them.
  */
object OrderEventsSpec extends ZIOSpecDefault:

  private final class RecordingEvents(store: Ref[Vector[OrderEvent]]) extends Events:
    def publish(events: List[OrderEvent]): UIO[Unit] = store.update(_ ++ events).unit
    def published: UIO[List[OrderEvent]] = store.get.map(_.toList)

  private final class MemoryOrders(store: Ref[Vector[Order]]) extends OrderRepository:
    def find(id: Order.Id): UIO[Option[Order]] = store.get.map(_.find(_.id == id))
    def save(order: Order): UIO[Order] =
      store.update(o => if o.exists(_.id == order.id) then o.map(x => if x.id == order.id then order else x) else o :+ order).as(order)
    def forCustomer(customerId: String, page: Page): UIO[Paged[Order]] =
      store.get.map(o => Paged(o.toList, o.size, 0, page.limit))
    def search(criteria: OrderSearch, page: Page): UIO[Paged[Order]] =
      store.get.map(o => Paged(o.toList, o.size, 0, page.limit))

  private object FixedNumbers extends OrderNumbers:
    def next(year: Int): UIO[Order.Number] = ZIO.succeed(Order.Number(year, 1))

  private final class CountingIds(counter: Ref[Int]) extends Ids:
    def next: UIO[String] = counter.updateAndGet(_ + 1).map(n => s"id-$n")

  private object NoBaskets extends BasketRepository:
    def find(owner: Basket.Owner): UIO[Option[Basket]] = ZIO.succeed(None)
    def save(basket: Basket): UIO[Basket] = ZIO.succeed(basket)
    def delete(owner: Basket.Owner): UIO[Unit] = ZIO.unit

  private object GuestBuyers extends BuyerPort:
    def resolve(actor: Actor): IO[CheckoutError, Buyer] = ZIO.succeed(Buyer.Guest)

  private val quotedAt = Timestamp(1_700_000_000_000L)

  private val line = BasketItemView(
    id = "line-1",
    description = LocalizedString("500× Business Cards", "500× Vizitky"),
    configuration = """{"categoryId":"cat-business-cards"}""",
    quantity = 2,
    speed = "Express",
    artworkId = None,
    price = QuotedPrice(Money(1000), Currency.CZK, quotedAt, "pricelist-v1"),
  )

  /** A checkout that always answers with the same quote, so the assertions are about what the
    * order service does with it rather than about pricing.
    */
  private final class FixedCheckout(quote: CheckoutQuote) extends CheckoutService:
    def options(actor: Actor): IO[CheckoutError, CheckoutOptions] =
      ZIO.succeed(CheckoutOptions(Buyer.Guest, Nil, Nil, Currency.CZK))
    def quote(actor: Actor, draft: CheckoutDraft): IO[CheckoutError, CheckoutQuote] =
      ZIO.succeed(quote)
    def applyDiscount(actor: Actor, code: String): IO[CheckoutError, DiscountDecision] =
      ZIO.succeed(DiscountDecision.Refused(code, LocalizedString("no", "ne")))
    def speedOffers(actor: Actor, itemId: String): IO[CheckoutError, List[SpeedOfferView]] =
      ZIO.succeed(Nil)

  private val quote = CheckoutQuote(
    lines = List(line),
    itemsTotal = Money(1000),
    discount = None,
    discountOff = Money.zero,
    delivery = None,
    grandTotal = Money(1000),
    currency = Currency.CZK,
    payment = Some(PaymentMethod.BankTransferQR),
    quotedAt = quotedAt,
  )

  private val input = PlaceOrder(
    buyer = OrderBuyer.Individual("Jan", "Novák", "jan@example.com", "+420123456789"),
    billing = Address("Street 1", "Praha", "11000", "CZ"),
    paymentMethod = "BankTransferQR",
  )

  private def serviceWith(quote: CheckoutQuote) =
    for
      orderStore <- Ref.make(Vector.empty[Order])
      eventStore <- Ref.make(Vector.empty[OrderEvent])
      idCounter <- Ref.make(0)
      recorder = RecordingEvents(eventStore)
      service = OrderServiceLive(
        orders = MemoryOrders(orderStore),
        numbers = FixedNumbers,
        events = recorder,
        checkout = FixedCheckout(quote),
        baskets = Baskets(NoBaskets, CountingIds(idCounter), ZIO.succeed(quotedAt)),
        buyers = GuestBuyers,
        ids = CountingIds(idCounter),
        now = ZIO.succeed(quotedAt),
        yearOf = _ => 2026,
      )
    yield (service, recorder)

  def spec = suite("placing an order publishes what happened")(
    test("an OrderPlaced event is published") {
      for
        wired <- serviceWith(quote)
        (service, events) = wired
        _ <- service.place(Actor.Anonymous("s"), input)
        published <- events.published
      yield assertTrue(published.size == 1, published.head.isInstanceOf[OrderEvent.OrderPlaced])
    },
    test("the event carries everything a consumer needs, so nobody has to ask back") {
      // Manufacturing generates workflows from this alone. An event that forces a lookup is a
      // distributed join, and the reason `ManufacturingOrder` embeds `Order` today.
      for
        wired <- serviceWith(quote)
        (service, events) = wired
        order <- service.place(Actor.Anonymous("s"), input)
        published <- events.published
      yield assertTrue(published.head match
        case placed: OrderEvent.OrderPlaced =>
          placed.orderId == order.id &&
          placed.number == order.number &&
          placed.buyerName.contains("Novák") &&
          placed.lines.size == 1 &&
          placed.lines.head.quantity == 2 &&
          placed.lines.head.configuration.contains("cat-business-cards") &&
          placed.speed == "Express" &&
          placed.total == Money(1000) &&
          placed.currency == Currency.CZK
        case _ => false
      )
    },
    test("nothing is published when the order is refused") {
      // The outbox is the shop's word to the rest of the system. Publishing an order that was
      // never placed would have manufacturing making goods nobody bought.
      for
        wired <- serviceWith(quote)
        (service, events) = wired
        _ <- service.place(Actor.Anonymous("s"), input.copy(expectedTotal = Some(Money(1)))).flip
        _ <- service
          .place(Actor.Anonymous("s"), input.copy(billing = Address("", "", "", "")))
          .flip
        published <- events.published
      yield assertTrue(published.isEmpty)
    },
    test("cancelling publishes the cancellation, with its reason") {
      for
        wired <- serviceWith(quote)
        (service, events) = wired
        order <- service.place(Actor.Anonymous("s"), input)
        _ <- service.cancel(
          Actor.Authenticated("u", None, isStaff = true),
          order.id,
          CancelOrder("out of stock"),
        )
        published <- events.published
      yield assertTrue(published.size == 2, published.last match
        case cancelled: OrderEvent.OrderCancelled =>
          cancelled.orderId == order.id && cancelled.reason == "out of stock"
        case _ => false
      )
    },
    test("confirming payment publishes it, with the reference") {
      for
        wired <- serviceWith(quote)
        (service, events) = wired
        order <- service.place(Actor.Anonymous("s"), input)
        _ <- service.confirmPayment(
          Actor.Authenticated("u", None, isStaff = true),
          order.id,
          ConfirmPayment("VS-2026-1"),
        )
        published <- events.published
      yield assertTrue(published.last match
        case paid: OrderEvent.OrderPaid =>
          paid.reference == "VS-2026-1" && paid.method == "BankTransferQR"
        case _ => false
      )
    },
  )
