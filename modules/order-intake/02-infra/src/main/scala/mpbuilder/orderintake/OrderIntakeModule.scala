package mpbuilder.orderintake

import mpbuilder.catalog as cat
import mpbuilder.commons.*
import mpbuilder.orderintake.impl.*
import mpbuilder.orderintake.impl.adapters.{CatalogProductAdapter, PricingQuoteAdapter}
import mpbuilder.orderintake.impl.memory.{InMemoryBasketRepository, RandomIds}
import mpbuilder.pricing as pri
import zio.*

/** How the order-intake context is assembled.
  *
  * Takes the other contexts' *public services* as inputs and adapts them internally, so the
  * composition root wires services to services and never sees a port or a repository.
  */
object OrderIntakeModule:

  def inMemory(defaultCurrency: Currency = Currency.CZK)
      : URLayer[cat.CatalogService & pri.PricingService, BasketService] =
    ZLayer {
      for
        catalog <- ZIO.service[cat.CatalogService]
        pricing <- ZIO.service[pri.PricingService]
        store <- Ref.make(Map.empty[Basket.Owner, Basket])
      yield BasketServiceLive(
        baskets = new InMemoryBasketRepository(store),
        products = new CatalogProductAdapter(catalog),
        quotes = new PricingQuoteAdapter(pricing, Clock.instant.map(i => Timestamp(i.toEpochMilli))),
        ids = RandomIds,
        now = Clock.instant.map(i => Timestamp(i.toEpochMilli)),
        defaultCurrency = defaultCurrency,
      )
    }
