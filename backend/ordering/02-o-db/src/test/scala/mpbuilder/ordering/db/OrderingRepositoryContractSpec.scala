package mpbuilder.ordering
package db

import core.*
import mpbuilder.ordering.sample.*
import mpbuilder.platform.PostgresTestSupport
import mpbuilder.shared.*
import kyo.*
import kyo.test.*

import java.time.Instant

class OrderingRepositoryContractSpec extends Test[Any]:

  private final case class Repos(carts: CartRepository, orders: OrderRepository, delivery: DeliveryCatalogRepository)

  private type Fresh = Repos < (Async & Scope & Abort[Throwable])

  private def inMemory: Fresh =
    for
      carts  <- OrderingSampleWiring.cartRepository(Nil)
      orders <- OrderingSampleWiring.orderRepository(Nil)
      p      <- StateRef.init(List.empty[ShopPickupLocation])
      c      <- StateRef.init(Map.empty[Currency, List[CourierServiceOption]])
      a      <- StateRef.init(Map.empty[Currency, BankAccountConfig])
    yield Repos(carts, orders, new InMemoryDeliveryCatalogRepository(p, c, a))

  private def postgres: Fresh =
    PostgresTestSupport.database(List(OrderingDb.migrations)).map { db =>
      val r = OrderingDb.repositories(db)
      Repos(r.carts, r.orders, r.delivery)
    }

  private val orders = SampleSeedOrders.initialOrders
  private val staff  = OrderActor.Employee(EmployeeId.unsafe("emp-1"), Some(NonEmptyString.unsafe("Admin")))

  private def contract(label: String, fresh: => Fresh): Unit =
    label - {
      "carts: save & find" in {
        for
          repo <- fresh.map(_.carts)
          cart  = Cart(CartId.unsafe("cart-1"), Some(CustomerId.unsafe("cust-1")), Currency.CZK, Nil, None)
          _    <- repo.save(cart)
          back <- repo.findById(cart.id)
          none <- repo.findById(CartId.unsafe("missing"))
        yield {
          assert(back.contains(cart))
          assert(none.isEmpty)
        }
      }

      "sample orders round-trip with full status history, sorted by placedAt" in {
        for
          repo <- fresh.map(_.orders)
          _    <- Kyo.foreachDiscard(orders)(repo.save)
          all  <- repo.listAll
          one  <- repo.findById(orders.last.id)
        yield {
          assert(all == orders.sortBy(_.placedAt))
          assert(one.contains(orders.last))
        }
      }

      "status transitions append to the immutable audit trail" in {
        val placed = orders.find(o => o.status == OrderStatus.Placed || o.status == OrderStatus.AwaitingPayment || o.status == OrderStatus.Confirmed).getOrElse(orders.head)
        val t0     = Instant.parse("2026-03-01T09:00:00Z")
        for
          repo     <- fresh.map(_.orders)
          _        <- repo.save(placed)
          held     <- Kyo.lift(placed.putOnHold(NonEmptyString.unsafe("Waiting for customer"), staff, t0).fold(e => throw new RuntimeException(e.message), identity))
          _        <- repo.save(held)
          back     <- repo.findById(placed.id).map(_.get)
        yield {
          assert(back == held)
          assert(back.statusHistory.size == placed.statusHistory.size + 1)
          assert(back.statusHistory.last.occurredAt == t0)
        }
      }

      "orders by customer" in {
        val withCustomer = orders.filter(_.customer.customerId.isDefined)
        val cid          = withCustomer.head.customer.customerId.get
        for
          repo <- fresh.map(_.orders)
          _    <- Kyo.foreachDiscard(orders)(repo.save)
          mine <- repo.findByCustomer(cid)
        yield {
          assert(mine.map(_.id).toSet == orders.filter(_.customer.customerId.contains(cid)).map(_.id).toSet)
          assert(mine.nonEmpty)
        }
      }

      "delivery settings: pickup locations, couriers per currency, bank accounts" in {
        val loc = SampleDeliveryCatalog.pickupLocations.head
        for
          repo  <- fresh.map(_.delivery)
          _     <- Kyo.foreachDiscard(SampleDeliveryCatalog.pickupLocations)(repo.savePickupLocation)
          _     <- Kyo.foreachDiscard(SampleDeliveryCatalog.courierOptionsByCurrency.values.flatten)(repo.saveCourierOption)
          _     <- Kyo.foreachDiscard(SampleDeliveryCatalog.bankAccounts.toList)((c, a) => repo.saveBankAccount(c, a))
          locs  <- repo.pickupLocations
          czk   <- repo.courierOptions(Currency.CZK)
          banks <- repo.allBankAccounts
          _     <- repo.deletePickupLocation(loc.id)
          _     <- repo.deleteCourierOption(czk.head.id, Currency.CZK)
          locs2 <- repo.pickupLocations
          czk2  <- repo.courierOptions(Currency.CZK)
          usd   <- repo.courierOptions(Currency.USD)
        yield {
          assert(locs == SampleDeliveryCatalog.pickupLocations)
          assert(czk == SampleDeliveryCatalog.courierOptionsByCurrency(Currency.CZK))
          assert(banks == SampleDeliveryCatalog.bankAccounts)
          assert(locs2.size == locs.size - 1)
          assert(czk2.size == czk.size - 1)
          assert(usd.size == SampleDeliveryCatalog.courierOptionsByCurrency.getOrElse(Currency.USD, Nil).size)
        }
      }
    }

  "Ordering repository contract" - {
    contract("in-memory", inMemory)
    contract("postgres", postgres)
  }
