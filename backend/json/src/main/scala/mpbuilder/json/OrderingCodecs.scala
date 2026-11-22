package mpbuilder.json

import com.github.plokhotnyuk.jsoniter_scala.core.*
import com.github.plokhotnyuk.jsoniter_scala.macros.*
import mpbuilder.json.JsonSupport.*
import mpbuilder.json.SharedCodecs.given
import mpbuilder.ordering.*
import zio.NonEmptyChunk

/** Codecs for the public `ordering` model (cart, checkout, orders, delivery settings). */
object OrderingCodecs:
  given JsonValueCodec[OrderLineId]      = stringCodec("OrderLineId")(OrderLineId(_))(_.value)
  given JsonValueCodec[OrderNumber]      = stringCodec("OrderNumber")(OrderNumber(_))(_.value)
  given JsonValueCodec[DeliveryOptionId] = stringCodec("DeliveryOptionId")(DeliveryOptionId(_))(_.value)

  given necComponentSnapshot: JsonValueCodec[NonEmptyChunk[ComponentSnapshot]] =
    nonEmptyChunkCodec(JsonCodecMaker.make[List[ComponentSnapshot]](config), "ComponentSnapshot list")
  given necOrderLine: JsonValueCodec[NonEmptyChunk[OrderLine]] =
    nonEmptyChunkCodec(JsonCodecMaker.make[List[OrderLine]](config), "OrderLine list")

  given JsonValueCodec[ProductSnapshot]       = JsonCodecMaker.make(config)
  given JsonValueCodec[LinePriceSnapshot]     = JsonCodecMaker.make(config)
  given JsonValueCodec[AppliedPromoSnapshot]  = JsonCodecMaker.make(config)
  given JsonValueCodec[ShopPickupLocation]    = JsonCodecMaker.make(config)
  given JsonValueCodec[CourierServiceOption]  = JsonCodecMaker.make(config)
  given JsonValueCodec[BankAccountConfig]     = JsonCodecMaker.make(config)
  given JsonValueCodec[DeliveryOptionChoice]  = JsonCodecMaker.make(config)
  given JsonValueCodec[DeliveryOptionQuote]   = JsonCodecMaker.make(config)
  given JsonValueCodec[PaymentOption]         = JsonCodecMaker.make(config)
  given JsonValueCodec[PaymentInstructions]   = JsonCodecMaker.make(config)
  given JsonValueCodec[CartLine]              = JsonCodecMaker.make(config)
  given JsonValueCodec[CartView]              = JsonCodecMaker.make(config)
  given JsonValueCodec[OrderCustomerSnapshot] = JsonCodecMaker.make(config)
  given JsonValueCodec[DeliverySelection]     = JsonCodecMaker.make(config)
  given JsonValueCodec[PaymentMethod]         = JsonCodecMaker.make(config)
  given JsonValueCodec[PaymentStatus]         = JsonCodecMaker.make(config)
  given JsonValueCodec[PaymentRecord]         = JsonCodecMaker.make(config)
  given JsonValueCodec[OrderStatus]           = JsonCodecMaker.make(config)
  given JsonValueCodec[OrderStatusKind]       = JsonCodecMaker.make(config)
  given JsonValueCodec[OrderActor]            = JsonCodecMaker.make(config)
  given JsonValueCodec[OrderStatusEvent]      = JsonCodecMaker.make(config)
  given JsonValueCodec[OrderInternalNote]     = JsonCodecMaker.make(config)
  given JsonValueCodec[OrderFilter]           = JsonCodecMaker.make(config)
  given JsonValueCodec[OrderPriority]         = JsonCodecMaker.make(config)
  given JsonValueCodec[OrderLine]             = JsonCodecMaker.make(config)
  given JsonValueCodec[OrderTotals]           = JsonCodecMaker.make(config)
  given JsonValueCodec[OrderView]             = JsonCodecMaker.make(config)
  given JsonValueCodec[PlaceOrderCmd]         = JsonCodecMaker.make(config)
  given JsonValueCodec[EditOrderLineCmd]      = JsonCodecMaker.make(config)
  given JsonValueCodec[OrderError]            = JsonCodecMaker.make(config)
