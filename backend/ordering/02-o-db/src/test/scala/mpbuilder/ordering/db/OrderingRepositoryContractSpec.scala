package mpbuilder.ordering
package db

import core.*
import mpbuilder.ordering.sample.*
import mpbuilder.platform.PostgresTestSupport
import mpbuilder.platform.db.Database
import mpbuilder.shared.*
import zio.*
import zio.test.*

import java.time.Instant

object OrderingRepositoryContractSpec extends ZIOSpecDefault:

  type Repos = CartRepository & OrderRepository & DeliveryCatalogRepository

  private val emptyDelivery: ULayer[DeliveryCatalogRepository] = ZLayer.fromZIO(
    for
      p <- Ref.make(List.empty[ShopPickupLocation])
      c <- Ref.make(Map.empty[Currency, List[CourierServiceOption]])
      a <- Ref.make(Map.empty[Currency, BankAccountConfig])
    yield new InMemoryDeliveryCatalogRepository(p, c, a)
  )

  val inMemory: ULayer[Repos] =
    OrderingSampleLayer.cartRepositoryLayer(Nil) ++ OrderingSampleLayer.orderRepositoryLayer(Nil) ++ emptyDelivery

  val postgres: URLayer[Database, Repos] =
    ZLayer.fromZIO(ZIO.serviceWithZIO[Database](PostgresTestSupport.truncateSchemas(_, List("ordering")))) >>>
      (OrderingDb.cartRepository ++ OrderingDb.orderRepository ++ OrderingDb.deliveryRepository)

  private val orders = SampleSeedOrders.initialOrders
  private val staff  = OrderActor.Employee(EmployeeId.unsafe("emp-1"), Some(NonEmptyString.unsafe("Admin")))

  def contract[R](label: String, fresh: ZLayer[R, Nothing, Repos]) = suite(label)(
    test("carts: save & find") {
      for
        repo <- ZIO.service[CartRepository]
        cart  = Cart(CartId.unsafe("cart-1"), Some(CustomerId.unsafe("cust-1")), Currency.CZK, Nil, None)
        _    <- repo.save(cart)
        back <- repo.findById(cart.id)
        none <- repo.findById(CartId.unsafe("missing"))
      yield assertTrue(back.contains(cart), none.isEmpty)
    },
    test("sample orders round-trip with full status history, sorted by placedAt") {
      for
        repo <- ZIO.service[OrderRepository]
        _    <- ZIO.foreachDiscard(orders)(repo.save)
        all  <- repo.listAll
        one  <- repo.findById(orders.last.id)
      yield assertTrue(all == orders.sortBy(_.placedAt), one.contains(orders.last))
    },
    test("status transitions append to the immutable audit trail") {
      val placed = orders.find(o => o.status == OrderStatus.Placed || o.status == OrderStatus.AwaitingPayment || o.status == OrderStatus.Confirmed).getOrElse(orders.head)
      val t0     = Instant.parse("2026-03-01T09:00:00Z")
      for
        repo     <- ZIO.service[OrderRepository]
        _        <- repo.save(placed)
        held     <- ZIO.fromEither(placed.putOnHold(NonEmptyString.unsafe("Waiting for customer"), staff, t0)).orDieWith(e => new RuntimeException(e.message))
        _        <- repo.save(held)
        back     <- repo.findById(placed.id).someOrFailException.orDie
      yield assertTrue(
        back == held,
        back.statusHistory.size == placed.statusHistory.size + 1,
        back.statusHistory.last.occurredAt == t0,
      )
    },
    test("orders by customer") {
      val withCustomer = orders.filter(_.customer.customerId.isDefined)
      val cid          = withCustomer.head.customer.customerId.get
      for
        repo <- ZIO.service[OrderRepository]
        _    <- ZIO.foreachDiscard(orders)(repo.save)
        mine <- repo.findByCustomer(cid)
      yield assertTrue(mine.map(_.id).toSet == orders.filter(_.customer.customerId.contains(cid)).map(_.id).toSet, mine.nonEmpty)
    },
    test("delivery settings: pickup locations, couriers per currency, bank accounts") {
      val loc = SampleDeliveryCatalog.pickupLocations.head
      for
        repo  <- ZIO.service[DeliveryCatalogRepository]
        _     <- ZIO.foreachDiscard(SampleDeliveryCatalog.pickupLocations)(repo.savePickupLocation)
        _     <- ZIO.foreachDiscard(SampleDeliveryCatalog.courierOptionsByCurrency.values.flatten)(repo.saveCourierOption)
        _     <- ZIO.foreachDiscard(SampleDeliveryCatalog.bankAccounts.toList)((c, a) => repo.saveBankAccount(c, a))
        locs  <- repo.pickupLocations
        czk   <- repo.courierOptions(Currency.CZK)
        banks <- repo.allBankAccounts
        _     <- repo.deletePickupLocation(loc.id)
        _     <- repo.deleteCourierOption(czk.head.id, Currency.CZK)
        locs2 <- repo.pickupLocations
        czk2  <- repo.courierOptions(Currency.CZK)
        usd   <- repo.courierOptions(Currency.USD)
      yield assertTrue(
        locs == SampleDeliveryCatalog.pickupLocations,
        czk == SampleDeliveryCatalog.courierOptionsByCurrency(Currency.CZK),
        banks == SampleDeliveryCatalog.bankAccounts,
        locs2.size == locs.size - 1,
        czk2.size == czk.size - 1,
        usd.size == SampleDeliveryCatalog.courierOptionsByCurrency.getOrElse(Currency.USD, Nil).size,
      )
    },
  ).provideSomeLayer[R](fresh)

  def spec = suite("Ordering repository contract")(
    contract("in-memory", inMemory),
    contract("postgres", postgres).provideShared(PostgresTestSupport.database(List(OrderingDb.migrations))),
  ) @@ TestAspect.sequential
