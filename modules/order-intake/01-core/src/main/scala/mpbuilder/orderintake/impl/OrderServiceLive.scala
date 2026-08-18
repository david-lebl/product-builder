package mpbuilder.orderintake
package impl

import mpbuilder.commons.*
import zio.*

/** Orchestration only. Every rule lives in [[OrderPolicy]] or behind a port.
  *
  * Placing an order goes through [[CheckoutService]] rather than re-implementing the quote: the
  * order must say exactly what the customer was quoted, and the surest way to guarantee that is for
  * both to come from the same code.
  */
private[orderintake] final class OrderServiceLive(
    orders: OrderRepository,
    numbers: OrderNumbers,
    events: Events,
    checkout: CheckoutService,
    baskets: Baskets,
    buyers: BuyerPort,
    ids: Ids,
    now: UIO[Timestamp],
    yearOf: Timestamp => Int,
) extends OrderService:

  def place(actor: Actor, input: PlaceOrder): IO[OrderError, OrderView] =
    for
      _ <- ZIO.fromEither(OrderPolicy.validate(input))
      // Re-validated and re-priced here, not read from the basket's stored quotes. The basket may
      // have been sitting for weeks, and this is the number that becomes binding.
      quote <- checkout
        .quote(
          actor,
          CheckoutDraft(input.deliveryOptionId, input.discountCode, Some(input.paymentMethod)),
        )
        .mapError(toOrderError)
      _ <- ZIO.fail(OrderError.EmptyBasket).when(quote.lines.isEmpty)
      _ <- guardPrice(input.expectedTotal, quote.grandTotal)
      method <- ZIO
        .fromOption(PaymentMethod.parse(input.paymentMethod))
        .orElseFail(OrderError.CheckoutRefused(CheckoutError.UnknownValue("paymentMethod", input.paymentMethod)))
      buyer <- buyers.resolve(actor).mapError(toOrderError)
      timestamp <- now
      number <- numbers.next(yearOf(timestamp))
      id <- ids.next
      lines <- ZIO.foreach(quote.lines.zipWithIndex)(freeze)
      order = Order(
        Order.Id(id),
        Order.Data(
          number = number,
          status = OrderPolicy.initialStatus(method, timestamp),
          buyer = input.buyer,
          customerId = buyer.customerRef,
          lines = lines,
          billing = input.billing,
          shipping = input.shipping,
          delivery = quote.delivery,
          payment = method,
          discount = quote.discount,
          totals = Order.Totals(
            items = quote.itemsTotal,
            discount = quote.discountOff,
            delivery = quote.delivery.map(_.payable).getOrElse(Money.zero),
            grand = quote.grandTotal,
            currency = quote.currency,
          ),
          speed = speedOf(quote),
          note = input.note.map(_.trim).filter(_.nonEmpty),
          placedAt = timestamp,
          history = List(Order.Transition(None, OrderPolicy.initialStatus(method, timestamp), timestamp, actor)),
          version = 0,
        ),
      )
      saved <- orders.save(order)
      // Emptied once the order exists, so pressing the button twice cannot buy the same goods
      // twice. The order is the record now; the basket has nothing left to say.
      _ <- baskets.load(actor).flatMap(b => baskets.save(b.copy(data = b.data.copy(items = Nil)).touched(timestamp)))
      _ <- events.publish(List(placedEvent(saved)))
    yield toView(saved)

  def find(actor: Actor, id: String): IO[OrderError, OrderView] =
    load(actor, id).map(toView)

  def list(actor: Actor, page: Page): IO[OrderError, Paged[OrderSummaryView]] =
    actor match
      case Actor.Authenticated(userId, customerId, _) =>
        orders.forCustomer(customerId.getOrElse(userId), page).map(mapPage)
      // A guest's orders are not reachable by listing: nothing identifies them but the order they
      // were just given. Returning an empty list would suggest they have none.
      case Actor.Anonymous(_) => ZIO.fail(OrderError.NotAuthorised)

  def search(
      actor: Actor,
      criteria: OrderSearch,
      page: Page,
  ): IO[OrderError, Paged[OrderSummaryView]] =
    if !OrderPolicy.isStaff(actor) then ZIO.fail(OrderError.StaffOnly)
    else orders.search(criteria, page).map(mapPage)

  def cancel(actor: Actor, id: String, input: CancelOrder): IO[OrderError, OrderView] =
    change(actor, id)(OrderPolicy.cancel(_, actor, input.reason, _)) { (order, timestamp) =>
      List(
        OrderEvent.OrderCancelled(
          order.id.value,
          order.data.number.toString,
          input.reason,
          timestamp,
        )
      )
    }

  def confirmPayment(actor: Actor, id: String, input: ConfirmPayment): IO[OrderError, OrderView] =
    change(actor, id)(OrderPolicy.confirmPayment(_, actor, input.reference, _)) { (order, timestamp) =>
      List(
        OrderEvent.OrderPaid(
          order.id.value,
          order.data.number.toString,
          order.data.payment.toString,
          input.reference,
          timestamp,
        )
      )
    }

  def reject(actor: Actor, id: String, input: RejectOrder): IO[OrderError, OrderView] =
    change(actor, id)(OrderPolicy.reject(_, actor, input.reason, _)) { (order, timestamp) =>
      List(
        OrderEvent.OrderRejected(
          order.id.value,
          order.data.number.toString,
          input.reason,
          timestamp,
        )
      )
    }

  // ── internals ────────────────────────────────────────────────────────────

  private def change(actor: Actor, id: String)(
      apply: (Order, Timestamp) => Either[OrderError, Order]
  )(eventsOf: (Order, Timestamp) => List[OrderEvent]): IO[OrderError, OrderView] =
    for
      order <- load(actor, id)
      timestamp <- now
      moved <- ZIO.fromEither(apply(order, timestamp))
      saved <- orders.save(moved)
      _ <- events.publish(eventsOf(saved, timestamp))
    yield toView(saved)

  /** An order the actor may not see is reported as missing, not as forbidden: "this order exists
    * but is not yours" is a fact worth nothing to a stranger and something to a guesser.
    */
  private def load(actor: Actor, id: String): IO[OrderError, Order] =
    orders
      .find(Order.Id(id))
      .flatMap {
        case Some(order) if order.isVisibleTo(actor) => ZIO.succeed(order)
        case _                                       => ZIO.fail(OrderError.NotFound(id))
      }

  private def guardPrice(expected: Option[Money], actual: Money): IO[OrderError, Unit] =
    expected match
      case Some(quoted) if quoted.value != actual.value =>
        ZIO.fail(OrderError.PriceChanged(quoted, actual))
      case _ => ZIO.unit

  private def freeze(line: (BasketItemView, Int)): UIO[Order.Line] =
    val (item, index) = line
    ids.next.map(id =>
      Order.Line(
        Order.Line.Id(id),
        Order.Line.Data(
          lineNo = index + 1,
          spec = ProductSpec(item.configuration, ""),
          description = item.description,
          quantity = item.quantity,
          speed = ProductionSpeed.parse(item.speed).getOrElse(ProductionSpeed.Standard),
          artworkId = item.artworkId,
          price = item.price,
        ),
      )
    )

  /** The order's speed is the fastest any line asked for: the shop cannot dispatch the order until
    * its slowest line is done, but the customer paid a rush premium on the fastest.
    */
  private def speedOf(quote: CheckoutQuote): ProductionSpeed =
    val speeds = quote.lines.flatMap(l => ProductionSpeed.parse(l.speed))
    if speeds.contains(ProductionSpeed.Express) then ProductionSpeed.Express
    else if speeds.contains(ProductionSpeed.Standard) then ProductionSpeed.Standard
    else speeds.headOption.getOrElse(ProductionSpeed.Standard)

  private def toOrderError(error: CheckoutError): OrderError = error match
    case CheckoutError.EmptyBasket => OrderError.EmptyBasket
    case other                     => OrderError.CheckoutRefused(other)

  private def placedEvent(order: Order): OrderEvent =
    OrderEvent.OrderPlaced(
      orderId = order.id.value,
      number = order.data.number.toString,
      buyerName = order.data.buyer.displayName,
      customerId = order.data.customerId,
      lines = order.data.lines.map(toLineView),
      speed = order.data.speed.toString,
      total = order.data.totals.grand,
      currency = order.data.totals.currency,
      placedAt = order.data.placedAt,
    )

  private def mapPage(page: Paged[Order]): Paged[OrderSummaryView] =
    Paged(page.items.map(toSummary), page.total, page.offset, page.limit)

  private def toSummary(order: Order): OrderSummaryView =
    OrderSummaryView(
      id = order.id.value,
      number = order.data.number.toString,
      status = toStatusView(order.status),
      buyerName = order.data.buyer.displayName,
      lineCount = order.data.lines.size,
      total = order.data.totals.grand,
      currency = order.data.totals.currency,
      placedAt = order.data.placedAt,
    )

  private def toLineView(line: Order.Line): OrderLineView =
    OrderLineView(
      id = line.id.value,
      lineNo = line.data.lineNo,
      description = line.data.description,
      configuration = line.data.spec.payload,
      quantity = line.data.quantity,
      speed = line.data.speed.toString,
      artworkId = line.data.artworkId,
      price = line.data.price,
    )

  private def toStatusView(status: Order.Status): OrderStatusView =
    val (at, reason) = status match
      case Order.Status.AwaitingPayment(due)     => (Some(due), None)
      case Order.Status.Paid(at, reference)      => (Some(at), Some(reference))
      case Order.Status.InProduction(since)      => (Some(since), None)
      case Order.Status.Completed(at, tracking)  => (Some(at), tracking)
      case Order.Status.Cancelled(at, _, reason) => (Some(at), Some(reason))
      case Order.Status.Rejected(at, _, reason)  => (Some(at), Some(reason))
      case Order.Status.Refunded(at, amount, _)  => (Some(at), Some(s"refunded ${amount.value}"))
    OrderStatusView(status.group, status.kind, at, reason)

  private def toView(order: Order): OrderView =
    OrderView(
      id = order.id.value,
      number = order.data.number.toString,
      status = toStatusView(order.status),
      buyer = order.data.buyer,
      customerId = order.data.customerId,
      lines = order.data.lines.map(toLineView),
      billing = order.data.billing,
      shipping = order.data.shipping,
      delivery = order.data.delivery,
      paymentMethod = order.data.payment.toString,
      discount = order.data.discount,
      totals = OrderTotalsView(
        items = order.data.totals.items,
        discount = order.data.totals.discount,
        delivery = order.data.totals.delivery,
        grand = order.data.totals.grand,
        currency = order.data.totals.currency,
      ),
      speed = order.data.speed.toString,
      note = order.data.note,
      placedAt = order.data.placedAt,
    )
