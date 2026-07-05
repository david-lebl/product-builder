package mpbuilder.ordering

import mpbuilder.kernel.*

/** A completed order */
final case class Order(
    id: OrderId,
    basket: Basket,
    checkoutInfo: CheckoutInfo,
    total: Money,
    currency: Currency,
    customerId: Option[CustomerId] = None,
)
