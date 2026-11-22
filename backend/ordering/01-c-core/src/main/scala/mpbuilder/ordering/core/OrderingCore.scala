package mpbuilder.ordering
package core

import mpbuilder.shared.*
import zio.*
import java.time.Instant

// ── Shared Tax / Promo Calculation Helper ────────────────────────────────────

private[ordering] object TotalsCalculator:

  /** Computes discounted net and tax across lines when a cart/order-level promo discount applies.
    *
    * When all lines share a tax rate, this matches `taxRate.taxOnNet(discountedNet)` exactly;
    * for mixed tax rates, it prorates the discount proportionally across each line's net value.
    */
  def computeLinesNetAndTax(
      linesNetAndRate: List[(Money, TaxRate)],
      promoDiscountNet: Money,
  ): (Money, Money, Money) =
    val rawNet       = linesNetAndRate.map(_._1).foldLeft(Money.zero)(_ + _).rounded
    val effDiscount  = promoDiscountNet.atMost(rawNet).atLeast(Money.zero).rounded
    val netAfterDisc = (rawNet - effDiscount).rounded

    val taxTotal =
      if !rawNet.isPositive || !netAfterDisc.isPositive then Money.zero
      else
        val distinctRates = linesNetAndRate.map(_._2).distinct
        distinctRates match
          case single :: Nil =>
            single.taxOnNet(netAfterDisc)
          case _ =>
            val ratio = netAfterDisc.value / rawNet.value
            linesNetAndRate
              .map { case (lineNet, rate) => rate.taxOnNet((lineNet * ratio).rounded) }
              .foldLeft(Money.zero)(_ + _)
              .rounded

    (rawNet, effDiscount, taxTotal)

  def aggregateWeight(weights: List[Option[BigDecimal]]): Option[BigDecimal] =
    if weights.nonEmpty && weights.forall(_.isDefined) then Some(weights.flatten.sum)
    else None

/** Internal Cart aggregate (compile-time isolated via `private[ordering]`). */
private[ordering] final case class Cart(
    id: CartId,
    customerId: Option[CustomerId],
    currency: Currency,
    lines: List[CartLine],
    appliedPromo: Option[AppliedPromoSnapshot] = None,
):
  def addLine(line: CartLine): Either[OrderError, Cart] =
    if line.price.currency != currency then Left(OrderError.CurrencyMismatch(currency, line.price.currency))
    else Right(copy(lines = lines :+ line))

  def updateLineCopies(lineId: OrderLineId, copies: Quantity): Either[OrderError, Cart] =
    if !lines.exists(_.lineId == lineId) then Left(OrderError.CartLineNotFound(id, lineId))
    else Right(copy(lines = lines.map(l => if l.lineId == lineId then l.copy(copies = copies) else l)))

  def replaceLine(line: CartLine): Either[OrderError, Cart] =
    if line.price.currency != currency then Left(OrderError.CurrencyMismatch(currency, line.price.currency))
    else if !lines.exists(_.lineId == line.lineId) then Left(OrderError.CartLineNotFound(id, line.lineId))
    else Right(copy(lines = lines.map(l => if l.lineId == line.lineId then line else l)))

  def removeLine(lineId: OrderLineId): Cart =
    val remaining = lines.filterNot(_.lineId == lineId)
    copy(lines = remaining, appliedPromo = if remaining.isEmpty then None else appliedPromo)

  def clear: Cart =
    copy(lines = Nil, appliedPromo = None)

  def withPromo(promo: AppliedPromoSnapshot): Cart =
    copy(appliedPromo = Some(promo))

  def withoutPromo: Cart =
    copy(appliedPromo = None)

  def withCustomer(cid: Option[CustomerId]): Cart =
    copy(customerId = cid)

  def toView: CartView =
    val (rawNet, discNet, taxTotal) = TotalsCalculator.computeLinesNetAndTax(
      lines.map(l => (l.lineNetTotal, l.price.taxRate)),
      appliedPromo.map(_.discountNet).getOrElse(Money.zero),
    )
    val netTotal    = (rawNet - discNet).rounded
    val grossTotal  = (netTotal + taxTotal).rounded
    val totalWeight = TotalsCalculator.aggregateWeight(lines.map(_.totalWeightGrams))
    CartView(
      id = id,
      customerId = customerId,
      currency = currency,
      lines = lines,
      appliedPromo = appliedPromo,
      linesNetTotal = rawNet,
      promoDiscountNet = discNet,
      netTotal = netTotal,
      taxTotal = taxTotal,
      grossTotal = grossTotal,
      totalWeightGrams = totalWeight,
    )

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
    appliedPromo: Option[AppliedPromoSnapshot] = None,
    statusHistory: List[OrderStatusEvent] = Nil,
    internalNotes: List[OrderInternalNote] = Nil,
    previousStatusBeforeHold: Option[OrderStatus] = None,
):
  def confirmPayment(
      reference: Option[String],
      now: Instant,
      actor: OrderActor = OrderActor.System,
  ): Either[OrderError, Order] =
    status match
      case OrderStatus.Placed | OrderStatus.AwaitingPayment =>
        val next = OrderStatus.Confirmed
        val ev   = OrderStatusEvent(Some(status), next, actor, reference.map(r => s"Payment confirmed ($r)"), now)
        Right(
          copy(
            payment = payment.copy(
              status = PaymentStatus.Confirmed,
              externalReference = reference.orElse(payment.externalReference),
              confirmedAt = Some(now),
            ),
            status = next,
            statusHistory = statusHistory :+ ev,
          )
        )
      // Allow staff to confirm payment even if they already accepted the order or placed it on hold
      case OrderStatus.Accepted | OrderStatus.InProduction | OrderStatus.ReadyForDispatch | OrderStatus.OnHold(_)
          if payment.status != PaymentStatus.Confirmed =>
        val ev = OrderStatusEvent(Some(status), status, actor, Some(reference.fold("Payment confirmed")(r => s"Payment confirmed ($r)")), now)
        Right(
          copy(
            payment = payment.copy(
              status = PaymentStatus.Confirmed,
              externalReference = reference.orElse(payment.externalReference),
              confirmedAt = Some(now),
            ),
            statusHistory = statusHistory :+ ev,
          )
        )
      case other =>
        Left(OrderError.InvalidStatusTransition(id, other, OrderStatus.Confirmed))

  def accept(actor: OrderActor, note: Option[String], now: Instant): Either[OrderError, Order] =
    status match
      case OrderStatus.Placed | OrderStatus.AwaitingPayment | OrderStatus.Confirmed =>
        val ev = OrderStatusEvent(Some(status), OrderStatus.Accepted, actor, note, now)
        Right(copy(status = OrderStatus.Accepted, statusHistory = statusHistory :+ ev))
      case other =>
        Left(OrderError.InvalidStatusTransition(id, other, OrderStatus.Accepted))

  def putOnHold(reason: NonEmptyString, actor: OrderActor, now: Instant): Either[OrderError, Order] =
    status match
      case OrderStatus.Completed | _: OrderStatus.Cancelled | _: OrderStatus.OnHold =>
        Left(OrderError.InvalidStatusTransition(id, status, OrderStatus.OnHold(reason)))
      case current =>
        val next = OrderStatus.OnHold(reason)
        val ev   = OrderStatusEvent(Some(current), next, actor, Some(reason.value), now)
        Right(
          copy(
            status = next,
            previousStatusBeforeHold = Some(current),
            statusHistory = statusHistory :+ ev,
          )
        )

  def resumeFromHold(actor: OrderActor, note: Option[String], now: Instant): Either[OrderError, Order] =
    status match
      case _: OrderStatus.OnHold =>
        val restored = previousStatusBeforeHold.getOrElse(OrderStatus.Accepted)
        val ev       = OrderStatusEvent(Some(status), restored, actor, note, now)
        Right(
          copy(
            status = restored,
            previousStatusBeforeHold = None,
            statusHistory = statusHistory :+ ev,
          )
        )
      case other =>
        Left(OrderError.OrderNotOnHold(id, other))

  def transitionTo(
      next: OrderStatus,
      actor: OrderActor = OrderActor.System,
      note: Option[String] = None,
      now: Instant = placedAt,
  ): Either[OrderError, Order] =
    val allowed = (status, next) match
      case (OrderStatus.Placed | OrderStatus.AwaitingPayment | OrderStatus.Confirmed, OrderStatus.Accepted) => true
      case (OrderStatus.Confirmed | OrderStatus.Accepted, OrderStatus.InProduction)                         => true
      case (OrderStatus.Accepted | OrderStatus.InProduction, OrderStatus.ReadyForDispatch)                  => true
      case (OrderStatus.ReadyForDispatch, OrderStatus.Dispatched)                                           => true
      case (OrderStatus.Dispatched | OrderStatus.ReadyForDispatch, OrderStatus.Completed)                   => true
      case (s, _: OrderStatus.OnHold) if !s.isTerminal && !s.isInstanceOf[OrderStatus.OnHold]               => true
      case (OrderStatus.Placed | OrderStatus.AwaitingPayment | OrderStatus.Confirmed | OrderStatus.Accepted | _: OrderStatus.OnHold, _: OrderStatus.Cancelled) =>
        true
      case _                                                                                                => false

    if allowed then
      val ev = OrderStatusEvent(Some(status), next, actor, note, now)
      val prevHold = next match
        case _: OrderStatus.OnHold => Some(status)
        case _                     => None
      Right(copy(status = next, previousStatusBeforeHold = prevHold, statusHistory = statusHistory :+ ev))
    else Left(OrderError.InvalidStatusTransition(id, status, next))

  def setPaymentStatus(
      nextPaymentStatus: PaymentStatus,
      reference: Option[String],
      now: Instant,
      actor: OrderActor,
  ): Either[OrderError, Order] =
    status match
      case _: OrderStatus.Cancelled if nextPaymentStatus != PaymentStatus.Refunded =>
        Left(OrderError.OrderAlreadyCancelled(id))
      case _ =>
        val nextConfirmedAt =
          if nextPaymentStatus == PaymentStatus.Confirmed then payment.confirmedAt.orElse(Some(now))
          else None
        val nextOrderStatus =
          if nextPaymentStatus == PaymentStatus.Confirmed && status == OrderStatus.AwaitingPayment then
            OrderStatus.Confirmed
          else status
        val label = nextPaymentStatus match
          case PaymentStatus.Confirmed  => "Payment marked as Paid"
          case PaymentStatus.Pending    => "Payment marked as Unpaid (Pending)"
          case PaymentStatus.Authorized => "Payment marked as Authorized"
          case PaymentStatus.Failed     => "Payment marked as Failed"
          case PaymentStatus.Refunded   => "Payment marked as Refunded"
        val noteText = reference.filter(_.trim.nonEmpty).fold(label)(r => s"$label ($r)")
        val ev       = OrderStatusEvent(Some(status), nextOrderStatus, actor, Some(noteText), now)
        Right(
          copy(
            payment = payment.copy(
              status = nextPaymentStatus,
              externalReference = reference.filter(_.trim.nonEmpty).orElse(payment.externalReference),
              confirmedAt = nextConfirmedAt,
            ),
            status = nextOrderStatus,
            statusHistory = statusHistory :+ ev,
          )
        )

  def editLine(
      cmd: EditOrderLineCmd,
      actor: OrderActor,
      now: Instant,
  ): Either[OrderError, Order] =
    status match
      case OrderStatus.Dispatched | OrderStatus.Completed | _: OrderStatus.Cancelled =>
        Left(OrderError.OrderNotEditable(id, status))
      case _ =>
        lines.find(_.lineId == cmd.lineId) match
          case None => Left(OrderError.OrderLineNotFound(id, cmd.lineId))
          case Some(target) =>
            val nextQty    = cmd.runQuantity.getOrElse(target.runQuantity)
            val nextCopies = cmd.copies.getOrElse(target.copies)
            val nextSpeed  = cmd.speed.getOrElse(target.speed)
            val nextNote   = cmd.specNote.map(_.map(_.trim).filter(_.nonEmpty)).getOrElse(target.specNote)

            // Scale per-copy net price proportionally if runQuantity changed (keeps subtotal accurate in-memory)
            val nextPrice =
              if nextQty != target.runQuantity && target.runQuantity.value > 0 then
                val factor  = BigDecimal(nextQty.value) / BigDecimal(target.runQuantity.value)
                val newNet  = (target.price.netAmount * factor).rounded
                val newTax  = target.price.taxRate.taxOnNet(newNet)
                val newGrs  = (newNet + newTax).rounded
                target.price.copy(netAmount = newNet, taxAmount = newTax, grossAmount = newGrs)
              else target.price

            val updatedLine = target.copy(
              runQuantity = nextQty,
              copies = nextCopies,
              speed = nextSpeed,
              price = nextPrice,
              specNote = nextNote,
            )
            val updatedLines = lines.map(l => if l.lineId == cmd.lineId then updatedLine else l)
            val promoRaw     = appliedPromo.map(_.discountNet).getOrElse(Money.zero)
            val (linesNet, promoDiscount, linesTax) = TotalsCalculator.computeLinesNetAndTax(
              updatedLines.toList.map(l => (l.lineNetTotal, l.price.taxRate)),
              promoRaw,
            )
            val (shipNet, shipTax) = delivery match
              case DeliverySelection.ShopPickup(_) => (Money.zero, Money.zero)
              case DeliverySelection.Courier(_, _, feeNet, taxRate) =>
                (feeNet.rounded, taxRate.taxOnNet(feeNet))
            val discountedLinesNet = (linesNet - promoDiscount).rounded
            val totalTax           = (linesTax + shipTax).rounded
            val grandGross         = (discountedLinesNet + shipNet + totalTax).rounded
            val newTotals          = totals.copy(
              linesNetTotal = linesNet,
              shippingNet = shipNet,
              taxTotal = totalTax,
              grandTotalGross = grandGross,
              promoDiscountNet = promoDiscount,
            )
            val changes = List(
              Option.when(nextQty != target.runQuantity)(s"qty ${target.runQuantity.value}→${nextQty.value}"),
              Option.when(nextCopies != target.copies)(s"copies ${target.copies.value}→${nextCopies.value}"),
              Option.when(nextSpeed != target.speed)(s"speed ${target.speed}→$nextSpeed"),
              Option.when(nextNote != target.specNote)("specification note updated"),
            ).flatten
            val summary =
              s"Edited line ${target.product.categoryName(Language.En)}: " +
                (if changes.nonEmpty then changes.mkString(", ") else "no changes")
            val ev = OrderStatusEvent(Some(status), status, actor, Some(summary), now)
            Right(copy(lines = updatedLines, totals = newTotals, statusHistory = statusHistory :+ ev))

  def addNote(authorId: EmployeeId, authorName: Option[NonEmptyString], text: NonEmptyString, now: Instant): Order =
    copy(internalNotes = internalNotes :+ OrderInternalNote(authorId, authorName, text, now))

  def attachArtwork(lineIndex: Int, componentKey: ComponentKey, artworkId: ArtworkId): Either[OrderError, Order] =
    if lineIndex < 0 || lineIndex >= lines.size then
      Left(OrderError.OrderLineNotFound(id, OrderLineId.unsafe(s"index-$lineIndex")))
    else
      val updatedLines = lines.zipWithIndex.map { case (line, idx) =>
        if idx == lineIndex then line.copy(artworkIds = line.artworkIds + (componentKey -> artworkId))
        else line
      }
      Right(copy(lines = updatedLines))

  def toView: OrderView =
    OrderView(
      id = id,
      orderNumber = orderNumber,
      customer = customer,
      billingAddress = billingAddress,
      delivery = delivery,
      payment = payment,
      status = status,
      lines = lines,
      totals = totals,
      customerNote = customerNote,
      placedAt = placedAt,
      appliedPromo = appliedPromo,
      statusHistory = statusHistory,
      internalNotes = internalNotes,
    )

private[ordering] object Order:
  def place(cmd: PlaceOrderCmd, now: Instant): Either[OrderError, Order] =
    val baseCurrency = cmd.lines.head.price.currency
    val mismatched   = cmd.lines.toList.find(_.price.currency != baseCurrency)

    if mismatched.isDefined then
      Left(OrderError.CurrencyMismatch(baseCurrency, mismatched.get.price.currency))
    else if cmd.paymentMethod == PaymentMethod.InvoiceOnAccount && !cmd.invoiceApproved then
      Left(OrderError.InvoiceNotAllowed(cmd.customer.email))
    else
      val promoRaw = cmd.appliedPromo.map(_.discountNet).getOrElse(Money.zero)
      val (linesNet, promoDiscount, linesTax) = TotalsCalculator.computeLinesNetAndTax(
        cmd.lines.toList.map(l => (l.lineNetTotal, l.price.taxRate)),
        promoRaw,
      )
      val (shipNet, shipTax) = cmd.delivery match
        case DeliverySelection.ShopPickup(_) => (Money.zero, Money.zero)
        case DeliverySelection.Courier(_, _, feeNet, taxRate) =>
          (feeNet.rounded, taxRate.taxOnNet(feeNet))

      val discountedLinesNet = (linesNet - promoDiscount).rounded
      val totalTax           = (linesTax + shipTax).rounded
      val grandGross         = (discountedLinesNet + shipNet + totalTax).rounded
      val totals             = OrderTotals(
        linesNetTotal = linesNet,
        shippingNet = shipNet,
        taxTotal = totalTax,
        grandTotalGross = grandGross,
        currency = baseCurrency,
        promoDiscountNet = promoDiscount,
      )

      // Approved InvoiceOnAccount orders skip AwaitingPayment and go straight to Confirmed
      val (initialStatus, paymentStatus) = cmd.paymentMethod match
        case PaymentMethod.InvoiceOnAccount => (OrderStatus.Confirmed, PaymentStatus.Authorized)
        case _                              => (OrderStatus.AwaitingPayment, PaymentStatus.Pending)

      val initialEvent = OrderStatusEvent(
        from = None,
        to = initialStatus,
        actor = OrderActor.Customer(cmd.customer.customerId, cmd.customer.fullName),
        note = cmd.customerNote,
        occurredAt = now,
      )

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
          appliedPromo = cmd.appliedPromo,
          statusHistory = List(initialEvent),
          internalNotes = Nil,
        )
      )

// ── Outbound Ports ───────────────────────────────────────────────────────────

private[ordering] trait CartRepository:
  def findById(id: CartId): UIO[Option[Cart]]
  def save(cart: Cart): UIO[Unit]

private[ordering] trait OrderRepository:
  def findById(id: OrderId): UIO[Option[Order]]
  def findByCustomer(customerId: CustomerId): UIO[List[Order]]
  def listAll: UIO[List[Order]]
  def save(order: Order): UIO[Unit]

private[ordering] trait DeliveryCatalogRepository:
  def pickupLocations: UIO[List[ShopPickupLocation]]
  def savePickupLocation(loc: ShopPickupLocation): UIO[Unit] = ZIO.unit
  def deletePickupLocation(id: DeliveryOptionId): UIO[Unit]  = ZIO.unit
  def courierOptions(currency: Currency): UIO[List[CourierServiceOption]]
  def allCourierOptions: UIO[List[CourierServiceOption]] =
    ZIO.foreach(Currency.values.toList)(courierOptions).map(_.flatten)
  def saveCourierOption(opt: CourierServiceOption): UIO[Unit]              = ZIO.unit
  def deleteCourierOption(id: DeliveryOptionId, currency: Currency): UIO[Unit] = ZIO.unit
  def bankAccount(currency: Currency): UIO[Option[BankAccountConfig]]
  def allBankAccounts: UIO[Map[Currency, BankAccountConfig]] =
    ZIO.foreach(Currency.values.toList)(c => bankAccount(c).map(_.map(c -> _))).map(_.flatten.toMap)
  def saveBankAccount(currency: Currency, config: BankAccountConfig): UIO[Unit] = ZIO.unit

// ── Live Service Implementations ─────────────────────────────────────────────

private[ordering] final class CartServiceLive(repo: CartRepository) extends CartService:

  override def createCart(cartId: CartId, currency: Currency, customerId: Option[CustomerId]): IO[OrderError, CartView] =
    val cart = Cart(cartId, customerId, currency, Nil, None)
    repo.save(cart).as(cart.toView)

  override def addLine(cartId: CartId, line: CartLine): IO[OrderError, CartView] =
    for
      cart    <- repo.findById(cartId).someOrFail(OrderError.CartNotFound(cartId))
      updated <- ZIO.fromEither(cart.addLine(line))
      _       <- repo.save(updated)
    yield updated.toView

  override def updateLineCopies(cartId: CartId, lineId: OrderLineId, copies: Quantity): IO[OrderError, CartView] =
    for
      cart    <- repo.findById(cartId).someOrFail(OrderError.CartNotFound(cartId))
      updated <- ZIO.fromEither(cart.updateLineCopies(lineId, copies))
      _       <- repo.save(updated)
    yield updated.toView

  override def replaceLine(cartId: CartId, line: CartLine): IO[OrderError, CartView] =
    for
      cart    <- repo.findById(cartId).someOrFail(OrderError.CartNotFound(cartId))
      updated <- ZIO.fromEither(cart.replaceLine(line))
      _       <- repo.save(updated)
    yield updated.toView

  override def removeLine(cartId: CartId, lineId: OrderLineId): IO[OrderError, CartView] =
    for
      cart   <- repo.findById(cartId).someOrFail(OrderError.CartNotFound(cartId))
      updated = cart.removeLine(lineId)
      _      <- repo.save(updated)
    yield updated.toView

  override def clearCart(cartId: CartId): IO[OrderError, CartView] =
    for
      cart   <- repo.findById(cartId).someOrFail(OrderError.CartNotFound(cartId))
      updated = cart.clear
      _      <- repo.save(updated)
    yield updated.toView

  override def applyPromo(cartId: CartId, promo: AppliedPromoSnapshot): IO[OrderError, CartView] =
    for
      cart   <- repo.findById(cartId).someOrFail(OrderError.CartNotFound(cartId))
      updated = cart.withPromo(promo)
      _      <- repo.save(updated)
    yield updated.toView

  override def removePromo(cartId: CartId): IO[OrderError, CartView] =
    for
      cart   <- repo.findById(cartId).someOrFail(OrderError.CartNotFound(cartId))
      updated = cart.withoutPromo
      _      <- repo.save(updated)
    yield updated.toView

  override def assignCustomer(cartId: CartId, customerId: Option[CustomerId]): IO[OrderError, CartView] =
    for
      cart   <- repo.findById(cartId).someOrFail(OrderError.CartNotFound(cartId))
      updated = cart.withCustomer(customerId)
      _      <- repo.save(updated)
    yield updated.toView

  override def getCart(cartId: CartId): IO[OrderError.CartNotFound, CartView] =
    repo.findById(cartId).someOrFail(new OrderError.CartNotFound(cartId)).map(_.toView)

private[ordering] final class CheckoutOptionsServiceLive(
    deliveryRepo: DeliveryCatalogRepository,
) extends CheckoutOptionsService:

  override def availableDeliveryOptions(
      currency: Currency,
      itemsNetSubtotal: Money,
      totalWeightGrams: Option[BigDecimal],
      freeDeliveryPromo: Boolean,
  ): UIO[List[DeliveryOptionQuote]] =
    for
      pickups  <- deliveryRepo.pickupLocations
      couriers <- deliveryRepo.courierOptions(currency)
    yield
      val pickupQuotes = pickups.filter(_.isActive).map { loc =>
        DeliveryOptionQuote(
          id = loc.id,
          isPickup = true,
          name = loc.name,
          description = loc.address,
          feeNet = Money.zero,
          taxRate = TaxRate.zero,
          feeGross = Money.zero,
          currency = currency,
          freeShippingApplied = false,
          available = true,
        )
      }
      val courierQuotes = couriers.filter(_.isActive).map { svc =>
        val weightExceeded = (svc.maxWeightGrams, totalWeightGrams) match
          case (Some(maxG), Some(actualG)) if actualG > maxG =>
            Some(s"Shipment weight (${actualG.setScale(0, BigDecimal.RoundingMode.CEILING)} g) exceeds courier limit (${maxG.setScale(0, BigDecimal.RoundingMode.CEILING)} g)")
          case _ => None

        val freeThresholdMet = svc.freeShippingThresholdNet.exists(th => itemsNetSubtotal >= th)
        val isFree           = freeDeliveryPromo || freeThresholdMet
        val effFeeNet        = if isFree then Money.zero else svc.feeNet.rounded
        val effFeeGross      = (effFeeNet + svc.taxRate.taxOnNet(effFeeNet)).rounded

        DeliveryOptionQuote(
          id = svc.id,
          isPickup = false,
          name = svc.name,
          description = svc.estimatedDelivery,
          feeNet = effFeeNet,
          taxRate = svc.taxRate,
          feeGross = effFeeGross,
          currency = svc.currency,
          freeShippingApplied = isFree && svc.feeNet.isPositive,
          available = weightExceeded.isEmpty,
          unavailableReason = weightExceeded,
        )
      }
      pickupQuotes ++ courierQuotes

  override def resolveDelivery(
      choice: DeliveryOptionChoice,
      currency: Currency,
      itemsNetSubtotal: Money,
      totalWeightGrams: Option[BigDecimal],
      freeDeliveryPromo: Boolean,
  ): IO[OrderError, DeliverySelection] =
    choice match
      case DeliveryOptionChoice.ShopPickup(locId) =>
        deliveryRepo.pickupLocations.flatMap { locs =>
          locs.find(l => l.id == locId && l.isActive) match
            case None => ZIO.fail(OrderError.DeliveryOptionNotFound(locId))
            case Some(loc) =>
              ZIO.succeed(DeliverySelection.ShopPickup(NonEmptyString.unsafe(loc.id.value)))
        }
      case DeliveryOptionChoice.Courier(svcId, shippingAddress) =>
        availableDeliveryOptions(currency, itemsNetSubtotal, totalWeightGrams, freeDeliveryPromo).flatMap { quotes =>
          quotes.find(q => !q.isPickup && q.id == svcId) match
            case None =>
              ZIO.fail(OrderError.DeliveryOptionNotFound(svcId))
            case Some(q) if !q.available =>
              ZIO.fail(OrderError.DeliveryOptionUnavailable(svcId, q.unavailableReason.getOrElse("Unavailable")))
            case Some(q) =>
              ZIO.succeed(
                DeliverySelection.Courier(
                  serviceId = NonEmptyString.unsafe(q.id.value),
                  shippingAddress = shippingAddress,
                  shippingFeeNet = q.feeNet,
                  shippingTaxRate = q.taxRate,
                )
              )
        }

  override def availablePaymentOptions(
      invoiceApproved: Boolean,
      cardEnabled: Boolean,
  ): UIO[List[PaymentOption]] =
    ZIO.succeed(
      List(
        PaymentOption(PaymentMethod.BankTransferQR, available = true),
        PaymentOption(
          method = PaymentMethod.Card,
          available = cardEnabled,
          unavailableReason = if cardEnabled then None else Some("Online card payment coming soon"),
        ),
        PaymentOption(
          method = PaymentMethod.InvoiceOnAccount,
          available = invoiceApproved,
          unavailableReason =
            if invoiceApproved then None
            else Some("Available only for approved corporate accounts with NetInvoice terms"),
        ),
      )
    )

  override def generatePaymentInstructions(
      orderNumber: OrderNumber,
      method: PaymentMethod,
      grandTotalGross: Money,
      currency: Currency,
      invoiceDueDays: Option[Int],
  ): UIO[PaymentInstructions] =
    method match
      case PaymentMethod.BankTransferQR =>
        deliveryRepo.bankAccount(currency).map { acctOpt =>
          val acct = acctOpt.getOrElse(
            BankAccountConfig(
              iban = NonEmptyString.unsafe("CZ6508000000192000145399"),
              bic = Some(NonEmptyString.unsafe("GIBACZPX")),
              accountHolder = NonEmptyString.unsafe("PrintShop s.r.o."),
            )
          )
          val digitsOnly = orderNumber.value.filter(_.isDigit)
          val vsRaw      = if digitsOnly.nonEmpty then digitsOnly.takeRight(10) else "0"
          val vs         = NonEmptyString.unsafe(vsRaw)
          val amountStr  = grandTotalGross.rounded.value.setScale(2, BigDecimal.RoundingMode.HALF_UP).toString
          val qrPayload  = s"SPD*1.0*ACC:${acct.iban.value}*AM:$amountStr*CC:${currency.toString}*X-VS:${vs.value}*MSG:Order ${orderNumber.value}"
          PaymentInstructions.BankTransferQR(
            iban = acct.iban,
            bic = acct.bic,
            accountHolder = acct.accountHolder,
            variableSymbol = vs,
            amount = grandTotalGross.rounded,
            currency = currency,
            qrPayload = qrPayload,
          )
        }
      case PaymentMethod.Card =>
        ZIO.succeed(PaymentInstructions.CardPending(None, "Redirect to card payment gateway"))
      case PaymentMethod.InvoiceOnAccount =>
        ZIO.succeed(
          PaymentInstructions.InvoiceScheduled(
            dueDays = invoiceDueDays.getOrElse(14),
            invoiceReference = NonEmptyString.unsafe(orderNumber.value),
          )
        )

  override def listAllPickupLocations: UIO[List[ShopPickupLocation]] =
    deliveryRepo.pickupLocations

  override def upsertPickupLocation(loc: ShopPickupLocation): UIO[ShopPickupLocation] =
    deliveryRepo.savePickupLocation(loc).as(loc)

  override def deletePickupLocation(id: DeliveryOptionId): UIO[Unit] =
    deliveryRepo.deletePickupLocation(id)

  override def listAllCourierOptions: UIO[List[CourierServiceOption]] =
    deliveryRepo.allCourierOptions

  override def upsertCourierOption(opt: CourierServiceOption): UIO[CourierServiceOption] =
    deliveryRepo.saveCourierOption(opt).as(opt)

  override def deleteCourierOption(id: DeliveryOptionId, currency: Currency): UIO[Unit] =
    deliveryRepo.deleteCourierOption(id, currency)

  override def listBankAccounts: UIO[Map[Currency, BankAccountConfig]] =
    deliveryRepo.allBankAccounts

  override def upsertBankAccount(currency: Currency, config: BankAccountConfig): UIO[BankAccountConfig] =
    deliveryRepo.saveBankAccount(currency, config).as(config)

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
    confirmPaymentInternal(orderId, reference, OrderActor.System)

  override def markInProduction(orderId: OrderId): IO[OrderError, OrderView] =
    transition(orderId, OrderStatus.InProduction, OrderActor.System, None)

  override def markReadyForDispatch(orderId: OrderId): IO[OrderError, OrderView] =
    transition(orderId, OrderStatus.ReadyForDispatch, OrderActor.System, None)

  override def markDispatched(orderId: OrderId): IO[OrderError, OrderView] =
    transition(orderId, OrderStatus.Dispatched, OrderActor.System, None)

  override def complete(orderId: OrderId): IO[OrderError, OrderView] =
    transition(orderId, OrderStatus.Completed, OrderActor.System, None)

  override def cancel(orderId: OrderId, reason: NonEmptyString): IO[OrderError, OrderView] =
    cancelInternal(orderId, reason, OrderActor.System)

  override def getOrder(orderId: OrderId): IO[OrderError.OrderNotFound, OrderView] =
    repo.findById(orderId).someOrFail(new OrderError.OrderNotFound(orderId)).map(_.toView)

  override def listOrdersByCustomer(customerId: CustomerId): UIO[List[OrderView]] =
    repo.findByCustomer(customerId).map(_.map(_.toView).sortBy(_.placedAt)(using Ordering[Instant].reverse))

  override def listOrders(filter: OrderFilter): UIO[List[OrderView]] =
    repo.listAll.map { orders =>
      val qOpt = filter.query.map(_.trim.toLowerCase).filter(_.nonEmpty)
      orders
        .filter { o =>
          val statusOk = filter.statuses.isEmpty || filter.statuses.contains(o.status.kind)
          val payOk    = filter.paymentStatus.forall(_ == o.payment.status)
          val custOk   = filter.customerId.forall(cid => o.customer.customerId.contains(cid))
          val queryOk  = qOpt.forall { q =>
            o.orderNumber.value.toLowerCase.contains(q) ||
            o.customer.fullName.value.toLowerCase.contains(q) ||
            o.customer.email.value.toLowerCase.contains(q) ||
            o.customer.companyName.exists(_.value.toLowerCase.contains(q)) ||
            o.customer.companyRegNo.exists(_.value.toLowerCase.contains(q))
          }
          statusOk && payOk && custOk && queryOk
        }
        .map(_.toView)
        .sortBy(_.placedAt)(using Ordering[Instant].reverse)
    }

  override def acceptOrder(
      orderId: OrderId,
      by: EmployeeId,
      byName: Option[NonEmptyString],
      note: Option[String],
  ): IO[OrderError, OrderView] =
    for
      now     <- Clock.instant
      order   <- repo.findById(orderId).someOrFail(OrderError.OrderNotFound(orderId))
      updated <- ZIO.fromEither(order.accept(OrderActor.Employee(by, byName), note, now))
      _       <- repo.save(updated)
      _       <- eventBus.publish(OrderAccepted(updated.id, updated.orderNumber, by, now))
      _       <- eventBus.publish(OrderStageChanged(updated.id, order.status, updated.status, by, now))
    yield updated.toView

  override def putOnHold(
      orderId: OrderId,
      by: EmployeeId,
      byName: Option[NonEmptyString],
      reason: NonEmptyString,
  ): IO[OrderError, OrderView] =
    for
      now     <- Clock.instant
      order   <- repo.findById(orderId).someOrFail(OrderError.OrderNotFound(orderId))
      updated <- ZIO.fromEither(order.putOnHold(reason, OrderActor.Employee(by, byName), now))
      _       <- repo.save(updated)
      _       <- eventBus.publish(OrderPutOnHold(updated.id, reason, by, now))
      _       <- eventBus.publish(OrderStageChanged(updated.id, order.status, updated.status, by, now))
    yield updated.toView

  override def resumeFromHold(
      orderId: OrderId,
      by: EmployeeId,
      byName: Option[NonEmptyString],
      note: Option[String],
  ): IO[OrderError, OrderView] =
    for
      now     <- Clock.instant
      order   <- repo.findById(orderId).someOrFail(OrderError.OrderNotFound(orderId))
      updated <- ZIO.fromEither(order.resumeFromHold(OrderActor.Employee(by, byName), note, now))
      _       <- repo.save(updated)
      _       <- eventBus.publish(OrderStageChanged(updated.id, order.status, updated.status, by, now))
    yield updated.toView

  override def advanceStage(
      orderId: OrderId,
      target: OrderStatus,
      by: EmployeeId,
      byName: Option[NonEmptyString],
      note: Option[String],
  ): IO[OrderError, OrderView] =
    target match
      case OrderStatus.Accepted          => acceptOrder(orderId, by, byName, note)
      case OrderStatus.OnHold(reason)    => putOnHold(orderId, by, byName, reason)
      case OrderStatus.Cancelled(reason) => cancelByStaff(orderId, by, byName, reason)
      case other =>
        for
          now     <- Clock.instant
          order   <- repo.findById(orderId).someOrFail(OrderError.OrderNotFound(orderId))
          updated <- ZIO.fromEither(order.transitionTo(other, OrderActor.Employee(by, byName), note, now))
          _       <- repo.save(updated)
          _       <- eventBus.publish(OrderStageChanged(updated.id, order.status, updated.status, by, now))
        yield updated.toView

  override def confirmPaymentByStaff(
      orderId: OrderId,
      by: EmployeeId,
      byName: Option[NonEmptyString],
      reference: Option[String],
  ): IO[OrderError, OrderView] =
    confirmPaymentInternal(orderId, reference, OrderActor.Employee(by, byName))

  override def markPaymentStatus(
      orderId: OrderId,
      status: PaymentStatus,
      by: EmployeeId,
      byName: Option[NonEmptyString],
      reference: Option[String],
  ): IO[OrderError, OrderView] =
    for
      now     <- Clock.instant
      order   <- repo.findById(orderId).someOrFail(OrderError.OrderNotFound(orderId))
      updated <- ZIO.fromEither(order.setPaymentStatus(status, reference, now, OrderActor.Employee(by, byName)))
      _       <- repo.save(updated)
      _       <- eventBus.publish(OrderConfirmed(updated.id, updated.orderNumber, updated.lines, now))
                   .when(order.status != OrderStatus.Confirmed && updated.status == OrderStatus.Confirmed)
    yield updated.toView

  override def editOrderLine(
      cmd: EditOrderLineCmd,
      by: EmployeeId,
      byName: Option[NonEmptyString],
  ): IO[OrderError, OrderView] =
    for
      now     <- Clock.instant
      order   <- repo.findById(cmd.orderId).someOrFail(OrderError.OrderNotFound(cmd.orderId))
      updated <- ZIO.fromEither(order.editLine(cmd, OrderActor.Employee(by, byName), now))
      _       <- repo.save(updated)
    yield updated.toView

  override def cancelByStaff(
      orderId: OrderId,
      by: EmployeeId,
      byName: Option[NonEmptyString],
      reason: NonEmptyString,
  ): IO[OrderError, OrderView] =
    cancelInternal(orderId, reason, OrderActor.Employee(by, byName))

  override def addInternalNote(
      orderId: OrderId,
      by: EmployeeId,
      byName: Option[NonEmptyString],
      text: NonEmptyString,
  ): IO[OrderError, OrderView] =
    for
      now     <- Clock.instant
      order   <- repo.findById(orderId).someOrFail(OrderError.OrderNotFound(orderId))
      updated  = order.addNote(by, byName, text, now)
      _       <- repo.save(updated)
    yield updated.toView

  override def attachArtwork(
      orderId: OrderId,
      lineIndex: Int,
      componentKey: ComponentKey,
      artworkId: ArtworkId,
  ): IO[OrderError, OrderView] =
    for
      order   <- repo.findById(orderId).someOrFail(OrderError.OrderNotFound(orderId))
      updated <- ZIO.fromEither(order.attachArtwork(lineIndex, componentKey, artworkId))
      _       <- repo.save(updated)
    yield updated.toView

  private def confirmPaymentInternal(
      orderId: OrderId,
      reference: Option[String],
      actor: OrderActor,
  ): IO[OrderError, OrderView] =
    for
      now     <- Clock.instant
      order   <- repo.findById(orderId).someOrFail(OrderError.OrderNotFound(orderId))
      updated <- ZIO.fromEither(order.confirmPayment(reference, now, actor))
      _       <- repo.save(updated)
      _       <- eventBus.publish(OrderConfirmed(updated.id, updated.orderNumber, updated.lines, now))
                   .when(order.status != OrderStatus.Confirmed && updated.status == OrderStatus.Confirmed)
    yield updated.toView

  private def cancelInternal(
      orderId: OrderId,
      reason: NonEmptyString,
      actor: OrderActor,
  ): IO[OrderError, OrderView] =
    for
      now     <- Clock.instant
      order   <- repo.findById(orderId).someOrFail(OrderError.OrderNotFound(orderId))
      updated <- ZIO.fromEither(order.transitionTo(OrderStatus.Cancelled(reason), actor, Some(reason.value), now))
      _       <- repo.save(updated)
      _       <- eventBus.publish(OrderCancelled(updated.id, reason, now))
    yield updated.toView

  private def transition(
      orderId: OrderId,
      next: OrderStatus,
      actor: OrderActor,
      note: Option[String],
  ): IO[OrderError, OrderView] =
    for
      now     <- Clock.instant
      order   <- repo.findById(orderId).someOrFail(OrderError.OrderNotFound(orderId))
      updated <- ZIO.fromEither(order.transitionTo(next, actor, note, now))
      _       <- repo.save(updated)
    yield updated.toView

private[ordering] object OrderingCoreLayers:
  val cartLayer: ZLayer[CartRepository, Nothing, CartService] =
    ZLayer.fromFunction(CartServiceLive.apply)

  val checkoutOptionsLayer: ZLayer[DeliveryCatalogRepository, Nothing, CheckoutOptionsService] =
    ZLayer.fromFunction(CheckoutOptionsServiceLive.apply)

  val orderLayer: ZLayer[OrderRepository & EventBus, Nothing, OrderService] =
    ZLayer.fromFunction(OrderServiceLive.apply)
