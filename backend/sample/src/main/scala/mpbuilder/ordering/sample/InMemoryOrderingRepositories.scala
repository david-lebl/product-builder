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

  override def save(order: Order): UIO[Unit] =
    ref.update(_ + (order.id -> order))

  def listAll: UIO[List[OrderView]] =
    ref.get.map(_.values.map(_.toView).toList.sortBy(_.placedAt))

final class InMemoryDeliveryCatalogRepository(
    pickups: List[ShopPickupLocation] = SampleDeliveryCatalog.pickupLocations,
    couriers: Map[Currency, List[CourierServiceOption]] = SampleDeliveryCatalog.courierOptionsByCurrency,
    accounts: Map[Currency, BankAccountConfig] = SampleDeliveryCatalog.bankAccounts,
) extends DeliveryCatalogRepository:

  override def pickupLocations: UIO[List[ShopPickupLocation]] =
    ZIO.succeed(pickups)

  override def courierOptions(currency: Currency): UIO[List[CourierServiceOption]] =
    ZIO.succeed(couriers.getOrElse(currency, Nil))

  override def bankAccount(currency: Currency): UIO[Option[BankAccountConfig]] =
    ZIO.succeed(accounts.get(currency))

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

  val orderRepositoryLayer: ULayer[InMemoryOrderRepository] =
    ZLayer.fromZIO(Ref.make(Map.empty[OrderId, Order]).map(new InMemoryOrderRepository(_)))

  val deliveryCatalogLayer: ULayer[DeliveryCatalogRepository] =
    ZLayer.succeed(new InMemoryDeliveryCatalogRepository())

  val live: ZLayer[EventBus, Nothing, CartService & CheckoutOptionsService & OrderService] =
    (cartRepositoryLayer() >>> OrderingCoreLayers.cartLayer) ++
      (deliveryCatalogLayer >>> OrderingCoreLayers.checkoutOptionsLayer) ++
      ((orderRepositoryLayer ++ ZLayer.service[EventBus]) >>> OrderingCoreLayers.orderLayer)
