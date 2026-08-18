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

  /** Everything this context offers, sharing one basket store.
    *
    * They must share it: a checkout that priced a different basket from the one the customer was
    * shown would be the worst possible bug here, and two independently-wired layers is how that
    * happens. Placing an order goes through the same checkout, for the same reason.
    */
  def inMemory(defaultCurrency: Currency = Currency.CZK): URLayer[
    cat.CatalogService & pri.PricingService & cus.CustomerService,
    BasketService & CheckoutService & OrderService & OrderLifecycle & OrderOutbox,
  ] =
    ZLayer.fromZIOEnvironment {
      for
        catalog <- ZIO.service[cat.CatalogService]
        pricing <- ZIO.service[pri.PricingService]
        customers <- ZIO.service[cus.CustomerService]
        basketStore <- Ref.make(Map.empty[Basket.Owner, Basket])
        orderStore <- Ref.make(Vector.empty[Order])
        numberStore <- Ref.make(Map.empty[Int, Int])
        eventStore <- Ref.make(Vector.empty[OrderOutboxEntry])
        eventCounter <- Ref.make(0L)
        now = Clock.instant.map(i => Timestamp(i.toEpochMilli))
        baskets = Baskets(new InMemoryBasketRepository(basketStore), RandomIds, now)
        products = new CatalogProductAdapter(catalog)
        quotes = new PricingQuoteAdapter(pricing, now)
        buyers = new CustomerBuyerAdapter(customers)
        outbox = new InMemoryEvents(eventStore, eventCounter)
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
        orderService = OrderServiceLive(
          orders = new InMemoryOrderRepository(orderStore),
          numbers = new InMemoryOrderNumbers(numberStore),
          events = outbox,
          checkout = checkout,
          baskets = baskets,
          buyers = buyers,
          ids = RandomIds,
          now = now,
          // The year the order number is scoped to. UTC, so the sequence cannot go backwards when
          // the shop's own clock crosses midnight into a different offset.
          yearOf = timestamp =>
            java.time.Instant
              .ofEpochMilli(timestamp.epochMillis)
              .atZone(java.time.ZoneOffset.UTC)
              .getYear,
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
        .add[OrderService](orderService)
        // The same object under two names. `OrderService` is what a request reaches;
        // `OrderLifecycle` is what the relay reaches, and it is deliberately kept off the HTTP
        // surface entirely.
        .add[OrderLifecycle](orderService)
        .add[OrderOutbox](outbox)
    }
