package mpbuilder.json

import kyo.*
import mpbuilder.json.JsonSupport.*
import mpbuilder.shared.*
import mpbuilder.json.SharedCodecs.given
import mpbuilder.json.JsonSupport.given
import mpbuilder.ordering.*

/** Schemas for the public `ordering` model (cart, checkout, orders, delivery settings). */
object OrderingCodecs:
  given Schema[OrderLineId]      = stringOpaque("OrderLineId")(OrderLineId(_))(_.value)
  given Schema[OrderNumber]      = stringOpaque("OrderNumber")(OrderNumber(_))(_.value)
  given Schema[DeliveryOptionId] = stringOpaque("DeliveryOptionId")(DeliveryOptionId(_))(_.value)


  given Schema[ProductSnapshot]       = Schema.derived
  given Schema[LinePriceSnapshot]     = Schema.derived
  given Schema[AppliedPromoSnapshot]  = Schema.derived
  given Schema[ShopPickupLocation]    = Schema.derived
  given Schema[CourierServiceOption]  = Schema.derived
  given Schema[BankAccountConfig]     = Schema.derived
  given Schema[DeliveryOptionChoice]  = Schema.derived
  given Schema[DeliveryOptionQuote]   = Schema.derived
  given Schema[PaymentOption]         = Schema.derived
  given Schema[PaymentInstructions]   = Schema.derived
  given Schema[CartLine]              = Schema.derived
  given Schema[CartView]              = Schema.derived
  given Schema[OrderCustomerSnapshot] = Schema.derived
  given Schema[DeliverySelection]     = Schema.derived
  given Schema[PaymentMethod]         = Schema.derived
  given Schema[PaymentStatus]         = Schema.derived
  given Schema[PaymentRecord]         = Schema.derived
  given Schema[OrderStatus]           = Schema.derived
  given Schema[OrderStatusKind]       = Schema.derived
  given Schema[OrderActor]            = Schema.derived
  given Schema[OrderStatusEvent]      = Schema.derived
  given Schema[OrderInternalNote]     = Schema.derived
  given Schema[OrderFilter]           = Schema.derived
  given Schema[OrderPriority]         = Schema.derived
  given Schema[OrderLine]             = Schema.derived
  given Schema[OrderTotals]           = Schema.derived
  given Schema[OrderView]             = Schema.derived
  given Schema[PlaceOrderCmd]         = Schema.derived
  given Schema[EditOrderLineCmd]      = Schema.derived
  given Schema[OrderError]            = Schema.derived
