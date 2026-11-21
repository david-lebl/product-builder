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

  private def orderServiceLayer: ULayer[OrderService & EventBus] =
    val busLayer = EventBus.live
    val repoLayer: ULayer[OrderRepository] = ZLayer.fromZIO {
      Ref.make(Map.empty[OrderId, Order]).map { ref =>
        new OrderRepository:
          override def findById(id: OrderId): UIO[Option[Order]] = ref.get.map(_.get(id))
          override def save(order: Order): UIO[Unit]             = ref.update(_ + (order.id -> order))
      }
    }
    busLayer ++ ((repoLayer ++ busLayer) >>> OrderingCoreLayers.orderLayer)

  def spec = suite("OrderingDomainSpec")(
    test("Cart enforces currency consistency across lines") {
      val cart = Cart(CartId.unsafe("cart-1"), None, Currency.CZK, List.empty, None)
      val czkLine = sampleLine("l1", Currency.CZK, Money(1000))
      val eurLine = sampleLine("l2", Currency.EUR, Money(40))
      val cartLineCzk = CartLine(czkLine.lineId, czkLine.product, czkLine.runQuantity, czkLine.versionCount, czkLine.speed, czkLine.price)
      val cartLineEur = CartLine(eurLine.lineId, eurLine.product, eurLine.runQuantity, eurLine.versionCount, eurLine.speed, eurLine.price)

      val added1 = cart.addLine(cartLineCzk)
      val added2 = added1.flatMap(_.addLine(cartLineEur))
      assertTrue(
        added1.map(_.toView.netTotal) == Right(Money("1000.00")),
        added2 == Left(OrderError.CurrencyMismatch(Currency.CZK, Currency.EUR)),
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
    test("OrderServiceLive enforces strict state machine transitions and emits OrderPlaced & OrderConfirmed") {
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
          svc       <- ZIO.service[OrderService]
          bus       <- ZIO.service[EventBus]
          queue     <- bus.subscribe
          placed    <- svc.placeOrder(cardCmd)
          illegal1  <- svc.markInProduction(placed.id).either // cannot start production before payment!
          confirmed <- svc.confirmPayment(placed.id, Some("STRIPE-999"))
          inProd    <- svc.markInProduction(placed.id)
          cantCancel <- svc.cancel(placed.id, NonEmptyString.unsafe("Changed mind")).either // cannot cancel once InProduction!
          ready     <- svc.markReadyForDispatch(placed.id)
          shipped   <- svc.markDispatched(placed.id)
          done      <- svc.complete(placed.id)
          ev1       <- queue.take
          ev2       <- queue.take
        yield assertTrue(
          placed.status == OrderStatus.AwaitingPayment,
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
  )
