package mpbuilder.ordering
package core

import mpbuilder.shared.*
import zio.*
import java.time.Instant

/** Internal Cart aggregate (compile-time isolated via `private[ordering]`). */
private[ordering] final case class Cart(
    id: CartId,
    customerId: Option[CustomerId],
    currency: Currency,
    lines: List[CartLine],
    appliedPromoCode: Option[String],
):
  def addLine(line: CartLine): Either[OrderError, Cart] =
    if line.price.currency != currency then Left(OrderError.CurrencyMismatch(currency, line.price.currency))
    else Right(copy(lines = lines :+ line))

  def removeLine(lineId: OrderLineId): Cart =
    copy(lines = lines.filterNot(_.lineId == lineId))

  def toView: CartView =
    val net   = lines.map(_.price.netAmount).foldLeft(Money.zero)(_ + _).rounded
    val gross = lines.map(_.price.grossAmount).foldLeft(Money.zero)(_ + _).rounded
    CartView(id, customerId, currency, lines, appliedPromoCode, net, gross)

/** Internal Order aggregate enforcing legal lifecycle state transitions and currency consistency. */
private[ordering] final case class Order(
    id: OrderId,
    orderNumber: OrderNumber,
    customer: OrderCustomerSnapshot,
    billingAddress: Address,
    delivery: DeliverySelection,
    payment: PaymentRecord,
    status: OrderStatus,
    lines: NonEmptyChunk[OrderLine],
    totals: OrderTotals,
    customerNote: Option[String],
    placedAt: Instant,
):
  def confirmPayment(reference: Option[String], now: Instant): Either[OrderError, Order] =
    status match
      case OrderStatus.Placed | OrderStatus.AwaitingPayment =>
        Right(
          copy(
            payment = payment.copy(
              status = PaymentStatus.Confirmed,
              externalReference = reference.orElse(payment.externalReference),
              confirmedAt = Some(now),
            ),
            status = OrderStatus.Confirmed,
          )
        )
      case other =>
        Left(OrderError.InvalidStatusTransition(id, other, OrderStatus.Confirmed))

  def transitionTo(next: OrderStatus): Either[OrderError, Order] =
    val allowed = (status, next) match
      case (OrderStatus.Confirmed, OrderStatus.InProduction)           => true
      case (OrderStatus.InProduction, OrderStatus.ReadyForDispatch)    => true
      case (OrderStatus.ReadyForDispatch, OrderStatus.Dispatched)      => true
      case (OrderStatus.Dispatched, OrderStatus.Completed)             => true
      case (OrderStatus.Placed | OrderStatus.AwaitingPayment | OrderStatus.Confirmed, _: OrderStatus.Cancelled) =>
        true
      case _ => false

    if allowed then Right(copy(status = next))
    else Left(OrderError.InvalidStatusTransition(id, status, next))

  def toView: OrderView =
    OrderView(id, orderNumber, customer, billingAddress, delivery, payment, status, lines, totals, customerNote, placedAt)

private[ordering] object Order:
  def place(cmd: PlaceOrderCmd, now: Instant): Either[OrderError, Order] =
    val baseCurrency = cmd.lines.head.price.currency
    val mismatched   = cmd.lines.toList.find(_.price.currency != baseCurrency)

    if mismatched.isDefined then
      Left(OrderError.CurrencyMismatch(baseCurrency, mismatched.get.price.currency))
    else if cmd.paymentMethod == PaymentMethod.InvoiceOnAccount && !cmd.invoiceApproved then
      Left(OrderError.InvoiceNotAllowed(cmd.customer.email))
    else
      val linesNet = cmd.lines.map(_.price.netAmount).reduceLeft(_ + _).rounded
      val linesTax = cmd.lines.map(_.price.taxAmount).reduceLeft(_ + _).rounded
      val (shipNet, shipTax) = cmd.delivery match
        case DeliverySelection.ShopPickup(_) => (Money.zero, Money.zero)
        case DeliverySelection.Courier(_, _, feeNet, taxRate) =>
          (feeNet.rounded, taxRate.taxOnNet(feeNet))

      val totalTax   = (linesTax + shipTax).rounded
      val grandGross = (linesNet + shipNet + totalTax).rounded
      val totals     = OrderTotals(linesNet, shipNet, totalTax, grandGross, baseCurrency)

      // Approved InvoiceOnAccount orders skip AwaitingPayment and go straight to Confirmed
      val (initialStatus, paymentStatus) = cmd.paymentMethod match
        case PaymentMethod.InvoiceOnAccount => (OrderStatus.Confirmed, PaymentStatus.Authorized)
        case _                              => (OrderStatus.AwaitingPayment, PaymentStatus.Pending)

      Right(
        Order(
          id = cmd.orderId,
          orderNumber = cmd.orderNumber,
          customer = cmd.customer,
          billingAddress = cmd.billingAddress,
          delivery = cmd.delivery,
          payment = PaymentRecord(cmd.paymentMethod, paymentStatus),
          status = initialStatus,
          lines = cmd.lines,
          totals = totals,
          customerNote = cmd.customerNote,
          placedAt = now,
        )
      )

private[ordering] trait CartRepository:
  def findById(id: CartId): UIO[Option[Cart]]
  def save(cart: Cart): UIO[Unit]

private[ordering] trait OrderRepository:
  def findById(id: OrderId): UIO[Option[Order]]
  def save(order: Order): UIO[Unit]

private[ordering] final class CartServiceLive(repo: CartRepository) extends CartService:
  override def addLine(cartId: CartId, line: CartLine): IO[OrderError, CartView] =
    for
      cart    <- repo.findById(cartId).someOrFail(OrderError.CartNotFound(cartId))
      updated <- ZIO.fromEither(cart.addLine(line))
      _       <- repo.save(updated)
    yield updated.toView

  override def removeLine(cartId: CartId, lineId: OrderLineId): IO[OrderError, CartView] =
    for
      cart   <- repo.findById(cartId).someOrFail(OrderError.CartNotFound(cartId))
      updated = cart.removeLine(lineId)
      _      <- repo.save(updated)
    yield updated.toView

  override def getCart(cartId: CartId): IO[OrderError.CartNotFound, CartView] =
    repo.findById(cartId).someOrFail(new OrderError.CartNotFound(cartId)).map(_.toView)

private[ordering] final class OrderServiceLive(
    repo: OrderRepository,
    eventBus: EventBus,
) extends OrderService:

  override def placeOrder(cmd: PlaceOrderCmd): IO[OrderError, OrderView] =
    for
      now   <- Clock.instant
      order <- ZIO.fromEither(Order.place(cmd, now))
      _     <- repo.save(order)
      _     <- eventBus.publish(OrderPlaced(order.id, order.orderNumber, order.customer.customerId, order.totals.grandTotalGross, order.totals.currency, now))
      _     <- eventBus.publish(OrderConfirmed(order.id, order.orderNumber, order.lines, now))
                 .when(order.status == OrderStatus.Confirmed)
    yield order.toView

  override def confirmPayment(orderId: OrderId, reference: Option[String]): IO[OrderError, OrderView] =
    for
      now     <- Clock.instant
      order   <- repo.findById(orderId).someOrFail(OrderError.OrderNotFound(orderId))
      updated <- ZIO.fromEither(order.confirmPayment(reference, now))
      _       <- repo.save(updated)
      _       <- eventBus.publish(OrderConfirmed(updated.id, updated.orderNumber, updated.lines, now))
    yield updated.toView

  override def markInProduction(orderId: OrderId): IO[OrderError, OrderView] =
    transition(orderId, OrderStatus.InProduction)

  override def markReadyForDispatch(orderId: OrderId): IO[OrderError, OrderView] =
    transition(orderId, OrderStatus.ReadyForDispatch)

  override def markDispatched(orderId: OrderId): IO[OrderError, OrderView] =
    transition(orderId, OrderStatus.Dispatched)

  override def complete(orderId: OrderId): IO[OrderError, OrderView] =
    transition(orderId, OrderStatus.Completed)

  override def cancel(orderId: OrderId, reason: NonEmptyString): IO[OrderError, OrderView] =
    for
      now     <- Clock.instant
      order   <- repo.findById(orderId).someOrFail(OrderError.OrderNotFound(orderId))
      updated <- ZIO.fromEither(order.transitionTo(OrderStatus.Cancelled(reason)))
      _       <- repo.save(updated)
      _       <- eventBus.publish(OrderCancelled(updated.id, reason, now))
    yield updated.toView

  override def getOrder(orderId: OrderId): IO[OrderError.OrderNotFound, OrderView] =
    repo.findById(orderId).someOrFail(new OrderError.OrderNotFound(orderId)).map(_.toView)

  private def transition(orderId: OrderId, next: OrderStatus): IO[OrderError, OrderView] =
    for
      order   <- repo.findById(orderId).someOrFail(OrderError.OrderNotFound(orderId))
      updated <- ZIO.fromEither(order.transitionTo(next))
      _       <- repo.save(updated)
    yield updated.toView

private[ordering] object OrderingCoreLayers:
  val cartLayer: ZLayer[CartRepository, Nothing, CartService] =
    ZLayer.fromFunction(CartServiceLive.apply)

  val orderLayer: ZLayer[OrderRepository & EventBus, Nothing, OrderService] =
    ZLayer.fromFunction(OrderServiceLive.apply)
