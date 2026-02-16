package mpbuilder.domain.service

import zio.prelude.*
import mpbuilder.domain.model.*
import mpbuilder.domain.pricing.*

final case class BasketCalculation(
    items: List[BasketItem],
    subtotal: Money,
    total: Money,
    currency: Currency,
)

object BasketService:

  def empty(basketId: BasketId): Basket =
    Basket(id = basketId, items = List.empty)

  def addItem(
      basket: Basket,
      configuration: ProductConfiguration,
      quantity: Int,
      pricelist: Pricelist,
  ): Validation[BasketError, Basket] =
    if quantity <= 0 then
      Validation.fail(BasketError.InvalidQuantity(quantity))
    else
      PriceCalculator.calculate(configuration, pricelist)
        .mapError(BasketError.PricingFailed.apply)
        .map { priceBreakdown =>
          val newItem = BasketItem(configuration, quantity, priceBreakdown)
          basket.copy(items = basket.items :+ newItem)
        }

  def removeItem(
      basket: Basket,
      configurationId: ConfigurationId,
  ): Basket =
    basket.copy(items = basket.items.filterNot(_.configuration.id == configurationId))

  def updateQuantity(
      basket: Basket,
      configurationId: ConfigurationId,
      newQuantity: Int,
  ): Validation[BasketError, Basket] =
    if newQuantity <= 0 then
      Validation.fail(BasketError.InvalidQuantity(newQuantity))
    else
      basket.items.find(_.configuration.id == configurationId) match
        case Some(item) =>
          val updatedItem = item.copy(quantity = newQuantity)
          val updatedItems = basket.items.map { i =>
            if i.configuration.id == configurationId then updatedItem else i
          }
          Validation.succeed(basket.copy(items = updatedItems))
        case None =>
          Validation.fail(BasketError.ConfigurationNotFound(configurationId))

  def calculateTotal(basket: Basket): BasketCalculation =
    basket.items.headOption match
      case None =>
        BasketCalculation(
          items = List.empty,
          subtotal = Money.zero,
          total = Money.zero,
          currency = Currency.USD, // default currency for empty basket
        )
      case Some(firstItem) =>
        val currency = firstItem.priceBreakdown.currency
        val subtotal = basket.items.foldLeft(Money.zero) { (acc, item) =>
          acc + (item.priceBreakdown.total * item.quantity)
        }
        BasketCalculation(
          items = basket.items,
          subtotal = subtotal.rounded,
          total = subtotal.rounded,
          currency = currency,
        )

  def clear(basket: Basket): Basket =
    basket.copy(items = List.empty)
