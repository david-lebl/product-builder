package mpbuilder.ordering

import mpbuilder.ordering.core.*
import mpbuilder.shared.*
import zio.*
import zio.test.*
import java.time.Instant

object OrderingDomainSpec extends ZIOSpecDefault:

  private val now = Instant.parse("2026-03-10T10:00:00Z")

  private val address = Address(
    street = NonEmptyString.unsafe("Karlova 12"),
    city = NonEmptyString.unsafe("Praha"),
    postalCode = NonEmptyString.unsafe("11000"),
    countryCode = NonEmptyString.unsafe("CZ"),
  )

  private val customerSnap = OrderCustomerSnapshot(
    customerId = Some(CustomerId.unsafe("cust-1")),
    segment = CustomerSegment.Business,
    fullName = NonEmptyString.unsafe("Petra Svobodová"),
    email = EmailAddress.unsafe("petra@agency.cz"),
    companyName = Some(NonEmptyString.unsafe("Agency s.r.o.")),
  )

  private def sampleLine(id: String, currency: Currency, net: Money): OrderLine =
    val tax   = TaxRate.czStandard.taxOnNet(net)
    val gross = (net + tax).rounded
    OrderLine(
      lineId = OrderLineId.unsafe(id),
      product = ProductSnapshot(
        categoryId = CategoryId.unsafe("business-cards"),
        categoryName = LocalizedString("Business Cards"),
        catalogVersion = Version.unsafe("2026.1"),
        components = NonEmptyChunk(
          ComponentSnapshot(
            key = ComponentKey.Main,
            materialId = MaterialId.unsafe("art-350"),
            materialName = LocalizedString("Art 350g"),
            printingMethodId = Some(PrintingMethodId.unsafe("digital")),
            inkNotation = Some("4/4"),
            finishIds = List(FinishId.unsafe("matte-lam")),
            flatSize = Dimension2D.unsafeMm(90, 50),
            finishedSize = Dimension2D.unsafeMm(90, 50),
            sheetsUsed = 25,
          )
        ),
      ),
      runQuantity = Quantity.unsafe(500),
      versionCount = VersionCount.unsafe(5),
      speed = ManufacturingSpeed.Standard,
      price = LinePriceSnapshot(
        quoteId = QuoteId.unsafe(s"q-$id"),
        pricelistId = PricelistId.unsafe("czk-2026"),
        pricelistVersion = Version.unsafe("2026.1"),
        currency = currency,
        netAmount = net,
        taxRate = TaxRate.czStandard,
        taxAmount = tax,
        grossAmount = gross,
      ),
      artworkIds = Map(ComponentKey.Main -> ArtworkId.unsafe("art-1")),
    )

  private def orderServiceLayer: ULayer[OrderService & CartService & CheckoutOptionsService & EventBus] =
    val busLayer = EventBus.live
    val orderRepoLayer: ULayer[OrderRepository] = ZLayer.fromZIO {
      Ref.make(Map.empty[OrderId, Order]).map { ref =>
        new OrderRepository:
          override def findById(id: OrderId): UIO[Option[Order]] = ref.get.map(_.get(id))
          override def findByCustomer(customerId: CustomerId): UIO[List[Order]] =
            ref.get.map(_.values.filter(_.customer.customerId.contains(customerId)).toList)
          override def listAll: UIO[List[Order]]     = ref.get.map(_.values.toList)
          override def save(order: Order): UIO[Unit] = ref.update(_ + (order.id -> order))
      }
    }
    val cartRepoLayer: ULayer[CartRepository] = ZLayer.fromZIO {
      Ref.make(Map.empty[CartId, Cart]).map { ref =>
        new CartRepository:
          override def findById(id: CartId): UIO[Option[Cart]] = ref.get.map(_.get(id))
          override def save(cart: Cart): UIO[Unit]             = ref.update(_ + (cart.id -> cart))
      }
    }
    val deliveryRepoLayer: ULayer[DeliveryCatalogRepository] = ZLayer.succeed {
      new DeliveryCatalogRepository:
        override def pickupLocations: UIO[List[ShopPickupLocation]] = ZIO.succeed(
          List(
            ShopPickupLocation(
              id = DeliveryOptionId.unsafe("shop-prague"),
              name = LocalizedString("Prague HQ", "Praha HQ"),
              address = LocalizedString("Wenceslas Sq 1", "Václavské nám. 1"),
            )
          )
        )
        override def courierOptions(currency: Currency): UIO[List[CourierServiceOption]] = ZIO.succeed(
          if currency == Currency.CZK then
            List(
              CourierServiceOption(
                id = DeliveryOptionId.unsafe("courier-standard"),
                name = LocalizedString("Standard Courier", "Standardní kurýr"),
                estimatedDelivery = LocalizedString("2-3 days", "2-3 dny"),
                feeNet = Money("100.00"),
                currency = Currency.CZK,
                taxRate = TaxRate.czStandard,
                freeShippingThresholdNet = Some(Money("5000.00")),
                maxWeightGrams = Some(BigDecimal(20000)),
              )
            )
          else Nil
        )
        override def bankAccount(currency: Currency): UIO[Option[BankAccountConfig]] = ZIO.succeed(
          Some(BankAccountConfig(NonEmptyString.unsafe("CZ6508000000192000145399"), None, NonEmptyString.unsafe("PrintShop s.r.o.")))
        )
    }
    busLayer ++
      (cartRepoLayer >>> OrderingCoreLayers.cartLayer) ++
      (deliveryRepoLayer >>> OrderingCoreLayers.checkoutOptionsLayer) ++
      ((orderRepoLayer ++ busLayer) >>> OrderingCoreLayers.orderLayer)

  def spec = suite("OrderingDomainSpec")(
    test("Cart enforces currency consistency across lines, scales copies, and applies cart-level promo with VAT") {
      val cart = Cart(CartId.unsafe("cart-1"), None, Currency.CZK, List.empty, None)
      val czkLine = sampleLine("l1", Currency.CZK, Money(1000))
      val eurLine = sampleLine("l2", Currency.EUR, Money(40))
      val cartLineCzk = CartLine(czkLine.lineId, czkLine.product, czkLine.runQuantity, czkLine.versionCount, czkLine.speed, czkLine.price, weightGrams = Some(BigDecimal(500)))
      val cartLineEur = CartLine(eurLine.lineId, eurLine.product, eurLine.runQuantity, eurLine.versionCount, eurLine.speed, eurLine.price)

      val added1  = cart.addLine(cartLineCzk)
      val added2  = added1.flatMap(_.addLine(cartLineEur))
      val doubled = added1.flatMap(_.updateLineCopies(cartLineCzk.lineId, Quantity.unsafe(2)))
      val promo   = AppliedPromoSnapshot(DiscountCodeId.unsafe("save10"), NonEmptyString.unsafe("SAVE10"), Money("200.00"))
      val withPr  = doubled.map(_.withPromo(promo).toView)

      // 2 copies × 1000 net = 2000 net; -200 promo = 1800 net; 21% VAT = 378.00; gross = 2178.00; weight = 1000g
      assertTrue(
        added1.map(_.toView.netTotal) == Right(Money("1000.00")),
        added2 == Left(OrderError.CurrencyMismatch(Currency.CZK, Currency.EUR)),
        withPr.map(_.linesNetTotal) == Right(Money("2000.00")),
        withPr.map(_.promoDiscountNet) == Right(Money("200.00")),
        withPr.map(_.netTotal) == Right(Money("1800.00")),
        withPr.map(_.taxTotal) == Right(Money("378.00")),
        withPr.map(_.grossTotal) == Right(Money("2178.00")),
        withPr.map(_.totalWeightGrams) == Right(Some(BigDecimal(1000))),
      )
    },
    test("Order.place rejects unapproved InvoiceOnAccount, computes shipping VAT, and auto-confirms approved InvoiceOnAccount") {
      val line = sampleLine("l1", Currency.CZK, Money(1000))
      val courier = DeliverySelection.Courier(
        serviceId = NonEmptyString.unsafe("ppl-standard"),
        shippingAddress = address,
        shippingFeeNet = Money(100),
        shippingTaxRate = TaxRate.czStandard,
      )
      val unapprovedCmd = PlaceOrderCmd(
        orderId = OrderId.unsafe("ord-unapp"),
        orderNumber = OrderNumber.unsafe("ORD-2026-0001"),
        customer = customerSnap,
        billingAddress = address,
        delivery = courier,
        paymentMethod = PaymentMethod.InvoiceOnAccount,
        invoiceApproved = false,
        lines = NonEmptyChunk.single(line),
      )
      val approvedCmd = unapprovedCmd.copy(orderId = OrderId.unsafe("ord-app"), invoiceApproved = true)

      val rejected = Order.place(unapprovedCmd, now)
      val accepted = Order.place(approvedCmd, now)

      // Lines net=1000 (tax 210), Shipping net=100 (tax 21) -> total tax=231, grand gross=1331
      assertTrue(
        rejected == Left(OrderError.InvoiceNotAllowed(customerSnap.email)),
        accepted.map(_.status) == Right(OrderStatus.Confirmed),
        accepted.map(_.payment.status) == Right(PaymentStatus.Authorized),
        accepted.map(_.totals.taxTotal) == Right(Money("231.00")),
        accepted.map(_.totals.grandTotalGross) == Right(Money("1331.00")),
      )
    },
    test("OrderServiceLive enforces strict state machine transitions, emits events, and lists orders by customer") {
      val line = sampleLine("l1", Currency.CZK, Money(2000))
      val cardCmd = PlaceOrderCmd(
        orderId = OrderId.unsafe("ord-card"),
        orderNumber = OrderNumber.unsafe("ORD-2026-0002"),
        customer = customerSnap,
        billingAddress = address,
        delivery = DeliverySelection.ShopPickup(NonEmptyString.unsafe("prague-hq")),
        paymentMethod = PaymentMethod.Card,
        invoiceApproved = false,
        lines = NonEmptyChunk.single(line),
      )

      ZIO.scoped {
        for
          svc        <- ZIO.service[OrderService]
          bus        <- ZIO.service[EventBus]
          queue      <- bus.subscribe
          placed     <- svc.placeOrder(cardCmd)
          history    <- svc.listOrdersByCustomer(customerSnap.customerId.get)
          illegal1   <- svc.markInProduction(placed.id).either // cannot start production before payment!
          confirmed  <- svc.confirmPayment(placed.id, Some("STRIPE-999"))
          inProd     <- svc.markInProduction(placed.id)
          cantCancel <- svc.cancel(placed.id, NonEmptyString.unsafe("Changed mind")).either // cannot cancel once InProduction!
          ready      <- svc.markReadyForDispatch(placed.id)
          shipped    <- svc.markDispatched(placed.id)
          done       <- svc.complete(placed.id)
          ev1        <- queue.take
          ev2        <- queue.take
        yield assertTrue(
          placed.status == OrderStatus.AwaitingPayment,
          history.map(_.id) == List(placed.id),
          illegal1.isLeft,
          confirmed.status == OrderStatus.Confirmed,
          inProd.status == OrderStatus.InProduction,
          cantCancel.isLeft,
          ready.status == OrderStatus.ReadyForDispatch,
          shipped.status == OrderStatus.Dispatched,
          done.status == OrderStatus.Completed,
          ev1.isInstanceOf[OrderPlaced],
          ev2.isInstanceOf[OrderConfirmed],
        )
      }.provide(orderServiceLayer)
    },
    test("CheckoutOptionsService computes courier fees, honors FREESHIP / threshold / weight caps, and builds SPD QR payload") {
      for
        optsSvc      <- ZIO.service[CheckoutOptionsService]
        stdQuotes    <- optsSvc.availableDeliveryOptions(Currency.CZK, Money("1000"), Some(BigDecimal(500)), freeDeliveryPromo = false)
        freeByPromo  <- optsSvc.availableDeliveryOptions(Currency.CZK, Money("1000"), Some(BigDecimal(500)), freeDeliveryPromo = true)
        freeByThresh <- optsSvc.availableDeliveryOptions(Currency.CZK, Money("6000"), Some(BigDecimal(500)), freeDeliveryPromo = false)
        overweight   <- optsSvc.resolveDelivery(
                          DeliveryOptionChoice.Courier(DeliveryOptionId.unsafe("courier-standard"), address),
                          Currency.CZK,
                          Money("1000"),
                          Some(BigDecimal(25000)),
                          freeDeliveryPromo = false,
                        ).either
        guestPay     <- optsSvc.availablePaymentOptions(invoiceApproved = false)
        corpPay      <- optsSvc.availablePaymentOptions(invoiceApproved = true)
        qrInstr      <- optsSvc.generatePaymentInstructions(
                          OrderNumber.unsafe("ORD-2026-0042"),
                          PaymentMethod.BankTransferQR,
                          Money("1331.00"),
                          Currency.CZK,
                        )
      yield assertTrue(
        stdQuotes.find(_.id == DeliveryOptionId.unsafe("courier-standard")).map(_.feeNet) == Some(Money("100.00")),
        freeByPromo.find(_.id == DeliveryOptionId.unsafe("courier-standard")).exists(q => q.feeNet == Money.zero && q.freeShippingApplied),
        freeByThresh.find(_.id == DeliveryOptionId.unsafe("courier-standard")).exists(q => q.feeNet == Money.zero && q.freeShippingApplied),
        overweight.isLeft,
        !guestPay.find(_.method == PaymentMethod.InvoiceOnAccount).get.available,
        corpPay.find(_.method == PaymentMethod.InvoiceOnAccount).get.available,
        qrInstr match
          case PaymentInstructions.BankTransferQR(_, _, _, vs, amt, cur, payload) =>
            vs.value == "20260042" && amt == Money("1331.00") && cur == Currency.CZK && payload.startsWith("SPD*1.0*ACC:")
          case _ => false,
      )
    }.provide(orderServiceLayer),
    test("Staff Order Intake supports accept, hold/resume restoring prior stage, payment confirmation, manual stage progression, notes, and filters") {
      val line      = sampleLine("l-intake", Currency.CZK, Money(1500))
      val staffId   = EmployeeId.unsafe("emp-intake-1")
      val staffName = Some(NonEmptyString.unsafe("Operations Lead"))
      val cmd = PlaceOrderCmd(
        orderId = OrderId.unsafe("ord-intake-1"),
        orderNumber = OrderNumber.unsafe("ORD-2026-0101"),
        customer = customerSnap,
        billingAddress = address,
        delivery = DeliverySelection.ShopPickup(NonEmptyString.unsafe("prague-hq")),
        paymentMethod = PaymentMethod.BankTransferQR,
        invoiceApproved = false,
        lines = NonEmptyChunk.single(line),
      )

      for
        svc        <- ZIO.service[OrderService]
        placed     <- svc.placeOrder(cmd)
        accepted   <- svc.acceptOrder(placed.id, staffId, staffName, Some("Checked artwork and stock"))
        paid       <- svc.confirmPaymentByStaff(placed.id, staffId, staffName, Some("WIRE-771"))
        onHold     <- svc.putOnHold(placed.id, staffId, staffName, NonEmptyString.unsafe("Waiting for bleed fix"))
        resumed    <- svc.resumeFromHold(placed.id, staffId, staffName, Some("Corrected PDF received"))
        noted      <- svc.addInternalNote(placed.id, staffId, staffName, NonEmptyString.unsafe("Customer picks up after 16:00"))
        inProd     <- svc.advanceStage(placed.id, OrderStatus.InProduction, staffId, staffName, Some("Press #2"))
        ready      <- svc.advanceStage(placed.id, OrderStatus.ReadyForDispatch, staffId, staffName, None)
        completed  <- svc.advanceStage(placed.id, OrderStatus.Completed, staffId, staffName, Some("Handed over at reception"))
        byStatus   <- svc.listOrders(OrderFilter(statuses = Set(OrderStatusKind.Completed)))
        byQuery    <- svc.listOrders(OrderFilter(query = Some("0101")))
        byPaid     <- svc.listOrders(OrderFilter(paymentStatus = Some(PaymentStatus.Confirmed)))
        badResume  <- svc.resumeFromHold(placed.id, staffId, staffName, None).either
      yield assertTrue(
        placed.status == OrderStatus.AwaitingPayment,
        accepted.status == OrderStatus.Accepted,
        paid.status == OrderStatus.Accepted,
        paid.payment.status == PaymentStatus.Confirmed,
        onHold.status == OrderStatus.OnHold(NonEmptyString.unsafe("Waiting for bleed fix")),
        resumed.status == OrderStatus.Accepted,
        noted.internalNotes.size == 1,
        inProd.status == OrderStatus.InProduction,
        ready.status == OrderStatus.ReadyForDispatch,
        completed.status == OrderStatus.Completed,
        completed.statusHistory.size == 8,
        byStatus.map(_.id) == List(placed.id),
        byQuery.map(_.id) == List(placed.id),
        byPaid.map(_.id) == List(placed.id),
        badResume == Left(OrderError.OrderNotOnHold(placed.id, OrderStatus.Completed)),
      )
    }.provide(orderServiceLayer),
  )
