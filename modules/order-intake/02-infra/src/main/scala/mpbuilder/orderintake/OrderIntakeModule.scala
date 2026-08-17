package mpbuilder.orderintake

import mpbuilder.catalog as cat
import mpbuilder.commons.*
import mpbuilder.customers as cus
import mpbuilder.orderintake.impl.*
import mpbuilder.orderintake.impl.adapters.{CatalogProductAdapter, CustomerBuyerAdapter, PricingQuoteAdapter}
import mpbuilder.orderintake.impl.memory.{InMemoryBasketRepository, RandomIds, StaticDeliveryCatalog}
import mpbuilder.pricing as pri
import zio.*

/** How the order-intake context is assembled.
  *
  * Takes the other contexts' *public services* as inputs and adapts them internally, so the
  * composition root wires services to services and never sees a port or a repository.
  */
object OrderIntakeModule:

  /** Both services at once, sharing one basket store.
    *
    * They must share it: a checkout that priced a different basket from the one the customer was
    * shown would be the worst possible bug in this context, and two independently-wired layers is
    * how that happens.
    */
  def inMemory(defaultCurrency: Currency = Currency.CZK)
      : URLayer[cat.CatalogService & pri.PricingService & cus.CustomerService, BasketService & CheckoutService] =
    ZLayer.fromZIOEnvironment {
      for
        catalog <- ZIO.service[cat.CatalogService]
        pricing <- ZIO.service[pri.PricingService]
        customers <- ZIO.service[cus.CustomerService]
        store <- Ref.make(Map.empty[Basket.Owner, Basket])
        now = Clock.instant.map(i => Timestamp(i.toEpochMilli))
        baskets = Baskets(new InMemoryBasketRepository(store), RandomIds, now)
        products = new CatalogProductAdapter(catalog)
        quotes = new PricingQuoteAdapter(pricing, now)
      yield ZEnvironment[BasketService](
        BasketServiceLive(
          baskets = baskets,
          products = products,
          quotes = quotes,
          ids = RandomIds,
          now = now,
          defaultCurrency = defaultCurrency,
        )
      ).add[CheckoutService](
        CheckoutServiceLive(
          baskets = baskets,
          products = products,
          quotes = quotes,
          discounts = quotes,
          buyers = new CustomerBuyerAdapter(customers),
          delivery = StaticDeliveryCatalog,
          now = now,
          defaultCurrency = defaultCurrency,
        )
      )
    }
