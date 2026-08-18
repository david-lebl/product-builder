package mpbuilder.orderintake

import mpbuilder.catalog as cat
import mpbuilder.commons.*
import mpbuilder.customers as cus
import mpbuilder.orderintake.impl.*
import mpbuilder.orderintake.impl.adapters.{CatalogProductAdapter, CustomerBuyerAdapter, PricingQuoteAdapter}
import mpbuilder.orderintake.impl.memory.{
  InMemoryBasketRepository,
  InMemoryEvents,
  InMemoryOrderNumbers,
  InMemoryOrderRepository,
  RandomIds,
  StaticDeliveryCatalog,
}
import mpbuilder.pricing as pri
import zio.*

/** How the order-intake context is assembled.
  *
  * Takes the other contexts' *public services* as inputs and adapts them internally, so the
  * composition root wires services to services and never sees a port or a repository.
  */
object OrderIntakeModule:

  /** All three services at once, sharing one basket store.
    *
    * They must share it: a checkout that priced a different basket from the one the customer was
    * shown would be the worst possible bug in this context, and two independently-wired layers is
    * how that happens. Placing an order goes through the same checkout, for the same reason.
    */
  def inMemory(defaultCurrency: Currency = Currency.CZK): URLayer[
    cat.CatalogService & pri.PricingService & cus.CustomerService,
    BasketService & CheckoutService & OrderService,
  ] =
    ZLayer.fromZIOEnvironment {
      for
        catalog <- ZIO.service[cat.CatalogService]
        pricing <- ZIO.service[pri.PricingService]
        customers <- ZIO.service[cus.CustomerService]
        store <- Ref.make(Map.empty[Basket.Owner, Basket])
        orderStore <- Ref.make(Vector.empty[Order])
        numberStore <- Ref.make(Map.empty[Int, Int])
        eventStore <- Ref.make(Vector.empty[OrderEvent])
        now = Clock.instant.map(i => Timestamp(i.toEpochMilli))
        baskets = Baskets(new InMemoryBasketRepository(store), RandomIds, now)
        products = new CatalogProductAdapter(catalog)
        quotes = new PricingQuoteAdapter(pricing, now)
        buyers = new CustomerBuyerAdapter(customers)
        checkout = CheckoutServiceLive(
          baskets = baskets,
          products = products,
          quotes = quotes,
          discounts = quotes,
          buyers = buyers,
          delivery = StaticDeliveryCatalog,
          now = now,
          defaultCurrency = defaultCurrency,
        )
      yield ZEnvironment[BasketService](
        BasketServiceLive(
          baskets = baskets,
          products = products,
          quotes = quotes,
          ids = RandomIds,
          now = now,
          defaultCurrency = defaultCurrency,
        )
      ).add[CheckoutService](checkout)
        .add[OrderService](
          OrderServiceLive(
            orders = new InMemoryOrderRepository(orderStore),
            numbers = new InMemoryOrderNumbers(numberStore),
            events = new InMemoryEvents(eventStore),
            checkout = checkout,
            baskets = baskets,
            buyers = buyers,
            ids = RandomIds,
            now = now,
            // The year the order number is scoped to. UTC, so the sequence cannot go backwards
            // when the shop's own clock crosses midnight into a different offset.
            yearOf = timestamp =>
              java.time.Instant
                .ofEpochMilli(timestamp.epochMillis)
                .atZone(java.time.ZoneOffset.UTC)
                .getYear,
          )
        )
    }
