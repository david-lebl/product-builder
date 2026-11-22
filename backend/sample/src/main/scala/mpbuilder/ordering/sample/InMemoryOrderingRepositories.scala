package mpbuilder.ordering.sample

import mpbuilder.ordering.*
import mpbuilder.ordering.core.{Cart, CartRepository, DeliveryCatalogRepository, Order, OrderRepository, OrderingCoreLayers}
import mpbuilder.shared.*
import zio.*

/** Sample shop pickup locations, courier options, and bank account matching the frontend checkout. */
object SampleDeliveryCatalog:

  val shopPragueId: DeliveryOptionId   = DeliveryOptionId.unsafe("shop-prague")
  val shopBrnoId: DeliveryOptionId     = DeliveryOptionId.unsafe("shop-brno")
  val shopOstravaId: DeliveryOptionId  = DeliveryOptionId.unsafe("shop-ostrava")

  val courierStandardId: DeliveryOptionId = DeliveryOptionId.unsafe("courier-standard")
  val courierExpressId: DeliveryOptionId  = DeliveryOptionId.unsafe("courier-express")
  val courierEconomyId: DeliveryOptionId  = DeliveryOptionId.unsafe("courier-economy")

  val pickupLocations: List[ShopPickupLocation] = List(
    ShopPickupLocation(
      id = shopPragueId,
      name = LocalizedString("Prague — Wenceslas Square 1", "Praha — Václavské náměstí 1"),
      address = LocalizedString("Wenceslas Square 1, 110 00 Prague 1", "Václavské náměstí 1, 110 00 Praha 1"),
      openingHours = Some(LocalizedString("Mon–Fri 08:00–18:00", "Po–Pá 08:00–18:00")),
    ),
    ShopPickupLocation(
      id = shopBrnoId,
      name = LocalizedString("Brno — Freedom Square 5", "Brno — náměstí Svobody 5"),
      address = LocalizedString("Freedom Square 5, 602 00 Brno", "náměstí Svobody 5, 602 00 Brno"),
      openingHours = Some(LocalizedString("Mon–Fri 08:30–17:00", "Po–Pá 08:30–17:00")),
    ),
    ShopPickupLocation(
      id = shopOstravaId,
      name = LocalizedString("Ostrava — Masaryk Square 3", "Ostrava — Masarykovo náměstí 3"),
      address = LocalizedString("Masaryk Square 3, 702 00 Ostrava", "Masarykovo náměstí 3, 702 00 Ostrava"),
      openingHours = Some(LocalizedString("Mon–Fri 08:30–16:30", "Po–Pá 08:30–16:30")),
    ),
  )

  val courierOptionsByCurrency: Map[Currency, List[CourierServiceOption]] = Map(
    Currency.CZK -> List(
      CourierServiceOption(
        id = courierStandardId,
        name = LocalizedString("Standard Delivery", "Standardní doručení"),
        estimatedDelivery = LocalizedString("3–5 business days", "3–5 pracovních dní"),
        feeNet = Money("99.00"),
        currency = Currency.CZK,
        taxRate = TaxRate.czStandard,
        freeShippingThresholdNet = Some(Money("5000.00")),
        maxWeightGrams = Some(BigDecimal(30000)),
      ),
      CourierServiceOption(
        id = courierExpressId,
        name = LocalizedString("Express Delivery", "Expresní doručení"),
        estimatedDelivery = LocalizedString("1–2 business days", "1–2 pracovní dny"),
        feeNet = Money("249.00"),
        currency = Currency.CZK,
        taxRate = TaxRate.czStandard,
        maxWeightGrams = Some(BigDecimal(15000)),
      ),
      CourierServiceOption(
        id = courierEconomyId,
        name = LocalizedString("Economy Delivery", "Ekonomické doručení"),
        estimatedDelivery = LocalizedString("5–10 business days", "5–10 pracovních dní"),
        feeNet = Money("49.00"),
        currency = Currency.CZK,
        taxRate = TaxRate.czStandard,
        freeShippingThresholdNet = Some(Money("3000.00")),
        maxWeightGrams = Some(BigDecimal(20000)),
      ),
    ),
    Currency.USD -> List(
      CourierServiceOption(
        id = courierStandardId,
        name = LocalizedString("Standard Delivery", "Standardní doručení"),
        estimatedDelivery = LocalizedString("3–5 business days", "3–5 pracovních dní"),
        feeNet = Money("4.50"),
        currency = Currency.USD,
        taxRate = TaxRate.zero,
        freeShippingThresholdNet = Some(Money("200.00")),
        maxWeightGrams = Some(BigDecimal(30000)),
      ),
      CourierServiceOption(
        id = courierExpressId,
        name = LocalizedString("Express Delivery", "Expresní doručení"),
        estimatedDelivery = LocalizedString("1–2 business days", "1–2 pracovní dny"),
        feeNet = Money("11.00"),
        currency = Currency.USD,
        taxRate = TaxRate.zero,
        maxWeightGrams = Some(BigDecimal(15000)),
      ),
      CourierServiceOption(
        id = courierEconomyId,
        name = LocalizedString("Economy Delivery", "Ekonomické doručení"),
        estimatedDelivery = LocalizedString("5–10 business days", "5–10 pracovních dní"),
        feeNet = Money("2.50"),
        currency = Currency.USD,
        taxRate = TaxRate.zero,
        freeShippingThresholdNet = Some(Money("150.00")),
        maxWeightGrams = Some(BigDecimal(20000)),
      ),
    ),
  )

  val bankAccounts: Map[Currency, BankAccountConfig] = Map(
    Currency.CZK -> BankAccountConfig(
      iban = NonEmptyString.unsafe("CZ6508000000192000145399"),
      bic = Some(NonEmptyString.unsafe("GIBACZPX")),
      accountHolder = NonEmptyString.unsafe("MP PrintShop s.r.o."),
    ),
    Currency.USD -> BankAccountConfig(
      iban = NonEmptyString.unsafe("CZ6508000000192000145399"),
      bic = Some(NonEmptyString.unsafe("GIBACZPX")),
      accountHolder = NonEmptyString.unsafe("MP PrintShop s.r.o."),
    ),
  )

/** In-memory `CartRepository` with convenience `createCart` for UI showcase sessions. */
final class InMemoryCartRepository(
    ref: Ref[Map[CartId, Cart]],
) extends CartRepository:

  override def findById(id: CartId): UIO[Option[Cart]] =
    ref.get.map(_.get(id))

  override def save(cart: Cart): UIO[Unit] =
    ref.update(_ + (cart.id -> cart))

  def createCart(cartId: CartId, currency: Currency, customerId: Option[CustomerId] = None): UIO[CartView] =
    val cart = Cart(cartId, customerId, currency, Nil, None)
    save(cart).as(cart.toView)

final class InMemoryOrderRepository(
    ref: Ref[Map[OrderId, Order]],
) extends OrderRepository:

  override def findById(id: OrderId): UIO[Option[Order]] =
    ref.get.map(_.get(id))

  override def findByCustomer(customerId: CustomerId): UIO[List[Order]] =
    ref.get.map(_.values.filter(_.customer.customerId.contains(customerId)).toList)

  override def listAll: UIO[List[Order]] =
    ref.get.map(_.values.toList.sortBy(_.placedAt))

  override def save(order: Order): UIO[Unit] =
    ref.update(_ + (order.id -> order))

  def listAllViews: UIO[List[OrderView]] =
    listAll.map(_.map(_.toView))

final class InMemoryDeliveryCatalogRepository(
    pickupsRef: Ref[List[ShopPickupLocation]],
    couriersRef: Ref[Map[Currency, List[CourierServiceOption]]],
    accountsRef: Ref[Map[Currency, BankAccountConfig]],
) extends DeliveryCatalogRepository:

  override def pickupLocations: UIO[List[ShopPickupLocation]] =
    pickupsRef.get

  override def savePickupLocation(loc: ShopPickupLocation): UIO[Unit] =
    pickupsRef.update(list => list.filterNot(_.id == loc.id) :+ loc)

  override def deletePickupLocation(id: DeliveryOptionId): UIO[Unit] =
    pickupsRef.update(_.filterNot(_.id == id))

  override def courierOptions(currency: Currency): UIO[List[CourierServiceOption]] =
    couriersRef.get.map(_.getOrElse(currency, Nil))

  override def allCourierOptions: UIO[List[CourierServiceOption]] =
    couriersRef.get.map(_.values.flatten.toList)

  override def saveCourierOption(opt: CourierServiceOption): UIO[Unit] =
    couriersRef.update { m =>
      val current = m.getOrElse(opt.currency, Nil).filterNot(_.id == opt.id)
      m + (opt.currency -> (current :+ opt))
    }

  override def deleteCourierOption(id: DeliveryOptionId, currency: Currency): UIO[Unit] =
    couriersRef.update { m =>
      m.updatedWith(currency)(_.map(_.filterNot(_.id == id)))
    }

  override def bankAccount(currency: Currency): UIO[Option[BankAccountConfig]] =
    accountsRef.get.map(_.get(currency))

  override def allBankAccounts: UIO[Map[Currency, BankAccountConfig]] =
    accountsRef.get

  override def saveBankAccount(currency: Currency, config: BankAccountConfig): UIO[Unit] =
    accountsRef.update(_ + (currency -> config))

object SampleSeedOrders:
  import java.time.Instant

  private val baseInstant = Instant.parse("2026-06-15T08:30:00Z")
  private val adminId     = EmployeeId.unsafe("emp-1")
  private val adminName   = Some(NonEmptyString.unsafe("Admin Operator"))
  private val managerId   = EmployeeId.unsafe("emp-3")
  private val managerName = Some(NonEmptyString.unsafe("Operations Manager"))

  private def mkLine(
      lineId: String,
      catId: String,
      catEn: String,
      catCs: String,
      matId: String,
      matEn: String,
      matCs: String,
      pmId: String,
      ink: String,
      wMm: Int,
      hMm: Int,
      qty: Int,
      sheets: Int,
      net: BigDecimal,
      speed: ManufacturingSpeed = ManufacturingSpeed.Standard,
  ): OrderLine =
    val netMoney = Money(net)
    val taxAmt   = (netMoney * (TaxRate.czStandard.value / BigDecimal(100))).rounded
    val gross    = netMoney + taxAmt
    OrderLine(
      lineId = OrderLineId.unsafe(lineId),
      product = ProductSnapshot(
        categoryId = CategoryId.unsafe(catId),
        categoryName = LocalizedString(catEn, catCs),
        catalogVersion = Version.unsafe("1.0"),
        components = NonEmptyChunk.single(
          ComponentSnapshot(
            key = ComponentKey.Main,
            materialId = MaterialId.unsafe(matId),
            materialName = LocalizedString(matEn, matCs),
            printingMethodId = Some(PrintingMethodId.unsafe(pmId)),
            inkNotation = Some(ink),
            finishIds = Nil,
            flatSize = Dimension2D(PositiveMm.unsafe(wMm), PositiveMm.unsafe(hMm)),
            finishedSize = Dimension2D(PositiveMm.unsafe(wMm), PositiveMm.unsafe(hMm)),
            sheetsUsed = sheets,
          )
        ),
      ),
      runQuantity = Quantity.unsafe(qty),
      versionCount = VersionCount.single,
      speed = speed,
      price = LinePriceSnapshot(
        quoteId = QuoteId.unsafe(s"q-$lineId"),
        pricelistId = PricelistId.unsafe("pl-czk-sheet-2026"),
        pricelistVersion = Version.unsafe("1.0"),
        currency = Currency.CZK,
        netAmount = netMoney,
        taxRate = TaxRate.czStandard,
        taxAmount = taxAmt,
        grossAmount = gross,
      ),
      artworkIds = Map.empty,
    )

  /** 4 realistic demo orders across the intake workflow:
    *   - ORD-2026-0901: AwaitingPayment (Bank transfer, walk-in customer)
    *   - ORD-2026-0902: Confirmed (NetInvoice, Print Shop Pro s.r.o. — ready for intake acceptance)
    *   - ORD-2026-0903: Accepted (NetInvoice, MegaCorp a.s. — accepted by Operations Manager)
    *   - ORD-2026-0904: InProduction (Paid, Graphic Design Co. — active print job)
    */
  private[ordering] val initialOrders: List[Order] =
    val ord1Line = mkLine(
      "seed-line-1", "cat-business-cards", "Business Cards", "Vizitky",
      "mat-coated-350", "Coated Silk 350g", "Křída matná 350g",
      "pm-digital", "4/4", 90, 50, 500, 26, BigDecimal("690.00"),
    )
    val ord1 = Order
      .place(
        PlaceOrderCmd(
          orderId = OrderId.unsafe("ord-seed-0901"),
          orderNumber = OrderNumber.unsafe("ORD-2026-0901"),
          customer = OrderCustomerSnapshot(
            customerId = None,
            segment = CustomerSegment.Individual,
            fullName = NonEmptyString.unsafe("Lukáš Dvořák"),
            email = EmailAddress.unsafe("lukas.dvorak@email.cz"),
            phone = Some(NonEmptyString.unsafe("+420 603 221 900")),
          ),
          billingAddress = Address(
            NonEmptyString.unsafe("Korunní 45"),
            NonEmptyString.unsafe("Praha"),
            NonEmptyString.unsafe("12000"),
            NonEmptyString.unsafe("CZ"),
          ),
          delivery = DeliverySelection.ShopPickup(NonEmptyString.unsafe(SampleDeliveryCatalog.shopPragueId.value)),
          paymentMethod = PaymentMethod.BankTransferQR,
          invoiceApproved = false,
          lines = NonEmptyChunk.single(ord1Line),
          customerNote = Some("Please double-check bleed on the logo edge."),
        ),
        baseInstant,
      )
      .toOption
      .get

    val ord2Line = mkLine(
      "seed-line-2", "cat-flyers", "Flyers", "Letáky",
      "mat-coated-170", "Coated Silk 170g", "Křída matná 170g",
      "pm-digital", "4/4", 148, 210, 1000, 260, BigDecimal("2450.00"),
      speed = ManufacturingSpeed.Express,
    )
    val ord2 = Order
      .place(
        PlaceOrderCmd(
          orderId = OrderId.unsafe("ord-seed-0902"),
          orderNumber = OrderNumber.unsafe("ORD-2026-0902"),
          customer = OrderCustomerSnapshot(
            customerId = Some(CustomerId.unsafe("cust-print-shop-pro")),
            segment = CustomerSegment.Agency,
            fullName = NonEmptyString.unsafe("Jan Novák"),
            email = EmailAddress.unsafe("jan@printshoppro.cz"),
            phone = Some(NonEmptyString.unsafe("+420 123 456 789")),
            companyName = Some(NonEmptyString.unsafe("Print Shop Pro s.r.o.")),
            companyRegNo = Some(NonEmptyString.unsafe("12345678")),
            vatId = Some(NonEmptyString.unsafe("CZ12345678")),
          ),
          billingAddress = Address(
            NonEmptyString.unsafe("Vinohradská 10"),
            NonEmptyString.unsafe("Praha"),
            NonEmptyString.unsafe("12000"),
            NonEmptyString.unsafe("CZ"),
          ),
          delivery = DeliverySelection.Courier(
            serviceId = NonEmptyString.unsafe(SampleDeliveryCatalog.courierExpressId.value),
            shippingAddress = Address(
              NonEmptyString.unsafe("Vinohradská 10"),
              NonEmptyString.unsafe("Praha"),
              NonEmptyString.unsafe("12000"),
              NonEmptyString.unsafe("CZ"),
            ),
            shippingFeeNet = Money("249.00"),
            shippingTaxRate = TaxRate.czStandard,
          ),
          paymentMethod = PaymentMethod.InvoiceOnAccount,
          invoiceApproved = true,
          lines = NonEmptyChunk.single(ord2Line),
          customerNote = Some("Rush event on Friday morning."),
        ),
        baseInstant.plusSeconds(1800),
      )
      .toOption
      .get

    val ord3Line = mkLine(
      "seed-line-3", "cat-posters", "Posters", "Plakáty",
      "mat-coated-200", "Coated Gloss 200g", "Křída lesklá 200g",
      "pm-large-format", "4/0", 420, 594, 50, 50, BigDecimal("3120.00"),
    )
    val ord3 = Order
      .place(
        PlaceOrderCmd(
          orderId = OrderId.unsafe("ord-seed-0903"),
          orderNumber = OrderNumber.unsafe("ORD-2026-0903"),
          customer = OrderCustomerSnapshot(
            customerId = Some(CustomerId.unsafe("cust-mega-corp")),
            segment = CustomerSegment.Agency,
            fullName = NonEmptyString.unsafe("Petr Dvořák"),
            email = EmailAddress.unsafe("petr@megacorp.cz"),
            phone = Some(NonEmptyString.unsafe("+420 111 222 333")),
            companyName = Some(NonEmptyString.unsafe("MegaCorp a.s.")),
            companyRegNo = Some(NonEmptyString.unsafe("11223344")),
            vatId = Some(NonEmptyString.unsafe("CZ11223344")),
          ),
          billingAddress = Address(
            NonEmptyString.unsafe("Wenceslas Square 1"),
            NonEmptyString.unsafe("Praha"),
            NonEmptyString.unsafe("11000"),
            NonEmptyString.unsafe("CZ"),
          ),
          delivery = DeliverySelection.ShopPickup(NonEmptyString.unsafe(SampleDeliveryCatalog.shopPragueId.value)),
          paymentMethod = PaymentMethod.InvoiceOnAccount,
          invoiceApproved = true,
          lines = NonEmptyChunk.single(ord3Line),
        ),
        baseInstant.plusSeconds(3600),
      )
      .flatMap(_.accept(OrderActor.Employee(managerId, managerName), Some("Specs verified, artwork clean"), baseInstant.plusSeconds(4200)))
      .map(_.addNote(managerId, managerName, NonEmptyString.unsafe("Pack in rigid cardboard tubes for pickup."), baseInstant.plusSeconds(4260)))
      .toOption
      .get

    val ord4Line = mkLine(
      "seed-line-4", "cat-brochures", "Brochures", "Brožury",
      "mat-coated-135", "Coated Silk 135g", "Křída matná 135g",
      "pm-offset", "4/4", 210, 297, 250, 140, BigDecimal("4850.00"),
    )
    val ord4 = Order
      .place(
        PlaceOrderCmd(
          orderId = OrderId.unsafe("ord-seed-0904"),
          orderNumber = OrderNumber.unsafe("ORD-2026-0904"),
          customer = OrderCustomerSnapshot(
            customerId = Some(CustomerId.unsafe("cust-graphic-design-co")),
            segment = CustomerSegment.Agency,
            fullName = NonEmptyString.unsafe("Marie Svobodová"),
            email = EmailAddress.unsafe("marie@graphicdesign.cz"),
            phone = Some(NonEmptyString.unsafe("+420 987 654 321")),
            companyName = Some(NonEmptyString.unsafe("Graphic Design Co.")),
            companyRegNo = Some(NonEmptyString.unsafe("87654321")),
            vatId = Some(NonEmptyString.unsafe("CZ87654321")),
          ),
          billingAddress = Address(
            NonEmptyString.unsafe("Národní 5"),
            NonEmptyString.unsafe("Praha"),
            NonEmptyString.unsafe("11000"),
            NonEmptyString.unsafe("CZ"),
          ),
          delivery = DeliverySelection.Courier(
            serviceId = NonEmptyString.unsafe(SampleDeliveryCatalog.courierStandardId.value),
            shippingAddress = Address(
              NonEmptyString.unsafe("Národní 5"),
              NonEmptyString.unsafe("Praha"),
              NonEmptyString.unsafe("11000"),
              NonEmptyString.unsafe("CZ"),
            ),
            shippingFeeNet = Money.zero,
            shippingTaxRate = TaxRate.czStandard,
          ),
          paymentMethod = PaymentMethod.BankTransferQR,
          invoiceApproved = false,
          lines = NonEmptyChunk.single(ord4Line),
        ),
        baseInstant.minusSeconds(7200),
      )
      .flatMap(_.confirmPayment(Some("BANK-TX-88412"), baseInstant.minusSeconds(5400), OrderActor.Employee(adminId, adminName)))
      .flatMap(_.accept(OrderActor.Employee(adminId, adminName), Some("Payment matched, accepted for print"), baseInstant.minusSeconds(5000)))
      .flatMap(_.transitionTo(OrderStatus.InProduction, OrderActor.Employee(managerId, managerName), Some("Started on Heidelberg press"), baseInstant.minusSeconds(3600)))
      .toOption
      .get

    List(ord1, ord2, ord3, ord4)

object OrderingSampleLayer:

  def cartRepositoryLayer(
      seedCarts: List[(CartId, Currency, Option[CustomerId])] = List(
        (CartId.unsafe("cart-showcase-czk"), Currency.CZK, None),
        (CartId.unsafe("cart-showcase-usd"), Currency.USD, None),
      ),
  ): ULayer[InMemoryCartRepository] =
    ZLayer.fromZIO {
      val initial = seedCarts.map { case (id, cur, cid) => id -> Cart(id, cid, cur, Nil, None) }.toMap
      Ref.make(initial).map(new InMemoryCartRepository(_))
    }

  def orderRepositoryLayer(seedOrders: List[Order] = SampleSeedOrders.initialOrders): ULayer[InMemoryOrderRepository] =
    ZLayer.fromZIO(
      Ref.make(seedOrders.map(o => o.id -> o).toMap).map(new InMemoryOrderRepository(_))
    )

  val deliveryCatalogLayer: ULayer[DeliveryCatalogRepository] =
    ZLayer.fromZIO {
      for
        p <- Ref.make(SampleDeliveryCatalog.pickupLocations)
        c <- Ref.make(SampleDeliveryCatalog.courierOptionsByCurrency)
        a <- Ref.make(SampleDeliveryCatalog.bankAccounts)
      yield new InMemoryDeliveryCatalogRepository(p, c, a)
    }

  def live(seedSampleOrders: Boolean = true): ZLayer[EventBus, Nothing, CartService & CheckoutOptionsService & OrderService] =
    val orders = if seedSampleOrders then SampleSeedOrders.initialOrders else Nil
    (cartRepositoryLayer() >>> OrderingCoreLayers.cartLayer) ++
      (deliveryCatalogLayer >>> OrderingCoreLayers.checkoutOptionsLayer) ++
      ((orderRepositoryLayer(orders) ++ ZLayer.service[EventBus]) >>> OrderingCoreLayers.orderLayer)

  val live: ZLayer[EventBus, Nothing, CartService & CheckoutOptionsService & OrderService] =
    live(seedSampleOrders = true)
