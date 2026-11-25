package mpbuilder.ordering
package wiring

import core.Cart
import db.OrderingDb
import kyo.*
import mpbuilder.ordering.sample.{SampleDeliveryCatalog, SampleSeedOrders}
import mpbuilder.platform.db.Database
import mpbuilder.shared.*

object OrderingWiring:
  /** Seeds delivery/bank settings, showcase carts and the 4 demo orders on an empty database. */
  def seedIfEmpty(db: Database): Boolean < Async =
    OrderingDb.seedIfEmpty(
      db,
      pickups = SampleDeliveryCatalog.pickupLocations,
      couriers = SampleDeliveryCatalog.courierOptionsByCurrency.values.flatten.toList,
      bankAccounts = SampleDeliveryCatalog.bankAccounts,
      carts = List(
        Cart(CartId.unsafe("cart-showcase-czk"), None, Currency.CZK, Nil, None),
        Cart(CartId.unsafe("cart-showcase-usd"), None, Currency.USD, Nil, None),
      ),
      orders = SampleSeedOrders.initialOrders,
    )
