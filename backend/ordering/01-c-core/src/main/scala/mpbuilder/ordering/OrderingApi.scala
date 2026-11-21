package mpbuilder.ordering

import mpbuilder.shared.*
import zio.*
import zio.prelude.Validation
import java.time.Instant

// ── Context-local IDs ────────────────────────────────────────────────────────

opaque type OrderLineId = String
object OrderLineId:
  def apply(raw: String): Validation[String, OrderLineId] =
    if raw.trim.nonEmpty then Validation.succeed(raw.trim)
    else Validation.fail("OrderLineId must not be empty")
  def unsafe(raw: String): OrderLineId = raw
  extension (id: OrderLineId) def value: String = id

opaque type OrderNumber = String
object OrderNumber:
  def apply(raw: String): Validation[String, OrderNumber] =
    if raw.trim.nonEmpty then Validation.succeed(raw.trim)
    else Validation.fail("OrderNumber must not be empty")
  def unsafe(raw: String): OrderNumber = raw
  extension (n: OrderNumber) def value: String = n

// ── Immutable Order Snapshots (No Mutable Catalog Aggregates Embedded!) ──────

final case class ComponentSnapshot(
    key: ComponentKey,
    materialId: MaterialId,
    materialName: LocalizedString,
    printingMethodId: Option[PrintingMethodId],
    inkNotation: Option[String],
    finishIds: List[FinishId],
    flatSize: Dimension2D,
    finishedSize: Dimension2D,
    sheetsUsed: Int,
)

/** Frozen product specification snapshot persisted with an order line. */
final case class ProductSnapshot(
    categoryId: CategoryId,
    categoryName: LocalizedString,
    catalogVersion: Version,
    components: NonEmptyChunk[ComponentSnapshot],
    summaryNotes: Option[String] = None,
)

/** Frozen commercial valuation snapshot persisted with an order line. */
final case class LinePriceSnapshot(
    quoteId: QuoteId,
    pricelistId: PricelistId,
    pricelistVersion: Version,
    currency: Currency,
    netAmount: Money,
    taxRate: TaxRate,
    taxAmount: Money,
    grossAmount: Money,
)

// ── Cart Models ──────────────────────────────────────────────────────────────

final case class CartLine(
    lineId: OrderLineId,
    product: ProductSnapshot,
    runQuantity: Quantity,
    versionCount: VersionCount,
    speed: ManufacturingSpeed,
    price: LinePriceSnapshot,
    artworkIds: Map[ComponentKey, ArtworkId] = Map.empty,
)

final case class CartView(
    id: CartId,
    customerId: Option[CustomerId],
    currency: Currency,
    lines: List[CartLine],
    appliedPromoCode: Option[String],
    netTotal: Money,
    grossTotal: Money,
)

// ── Order Models (Zero UI Wizard State, Zero Passwords) ──────────────────────

final case class OrderCustomerSnapshot(
    customerId: Option[CustomerId],
    segment: CustomerSegment,
    fullName: NonEmptyString,
    email: EmailAddress,
    phone: Option[NonEmptyString] = None,
    companyName: Option[NonEmptyString] = None,
    companyRegNo: Option[NonEmptyString] = None,
    vatId: Option[NonEmptyString] = None,
)

enum DeliverySelection:
  case ShopPickup(locationId: NonEmptyString)
  case Courier(serviceId: NonEmptyString, shippingAddress: Address, shippingFeeNet: Money, shippingTaxRate: TaxRate)

enum PaymentMethod:
  case BankTransferQR
  case Card
  case InvoiceOnAccount

enum PaymentStatus:
  case Pending, Authorized, Confirmed, Failed, Refunded

final case class PaymentRecord(
    method: PaymentMethod,
    status: PaymentStatus,
    externalReference: Option[String] = None,
    confirmedAt: Option[Instant] = None,
)

enum OrderStatus:
  case Placed
  case AwaitingPayment
  case Confirmed
  case InProduction
  case ReadyForDispatch
  case Dispatched
  case Completed
  case Cancelled(reason: NonEmptyString)

final case class OrderLine(
    lineId: OrderLineId,
    product: ProductSnapshot,
    runQuantity: Quantity,
    versionCount: VersionCount,
    speed: ManufacturingSpeed,
    price: LinePriceSnapshot,
    artworkIds: Map[ComponentKey, ArtworkId],
)

final case class OrderTotals(
    linesNetTotal: Money,
    shippingNet: Money,
    taxTotal: Money,
    grandTotalGross: Money,
    currency: Currency,
)

final case class OrderView(
    id: OrderId,
    orderNumber: OrderNumber,
    customer: OrderCustomerSnapshot,
    billingAddress: Address,
    delivery: DeliverySelection,
    payment: PaymentRecord,
    status: OrderStatus,
    lines: NonEmptyChunk[OrderLine],
    totals: OrderTotals,
    customerNote: Option[String],
    placedAt: Instant,
)

// ── Commands & Errors ────────────────────────────────────────────────────────

final case class PlaceOrderCmd(
    orderId: OrderId,
    orderNumber: OrderNumber,
    customer: OrderCustomerSnapshot,
    billingAddress: Address,
    delivery: DeliverySelection,
    paymentMethod: PaymentMethod,
    invoiceApproved: Boolean,
    lines: NonEmptyChunk[OrderLine],
    customerNote: Option[String] = None,
)

enum OrderError(val message: String):
  case CartNotFound(cartId: CartId)
      extends OrderError(s"Cart '${cartId.value}' not found")
  case OrderNotFound(orderId: OrderId)
      extends OrderError(s"Order '${orderId.value}' not found")
  case EmptyCart(cartId: CartId)
      extends OrderError(s"Cannot place order from empty cart '${cartId.value}'")
  case CurrencyMismatch(expected: Currency, actual: Currency)
      extends OrderError(s"Currency mismatch: expected $expected, got $actual")
  case InvoiceNotAllowed(customerEmail: EmailAddress)
      extends OrderError(s"Customer '${customerEmail.value}' is not approved for InvoiceOnAccount payment")
  case InvalidStatusTransition(orderId: OrderId, from: OrderStatus, to: OrderStatus)
      extends OrderError(s"Cannot transition order '${orderId.value}' from $from to $to")
  case OrderAlreadyCancelled(orderId: OrderId)
      extends OrderError(s"Order '${orderId.value}' is already cancelled")

// ── Domain Events ────────────────────────────────────────────────────────────

final case class OrderPlaced(
    orderId: OrderId,
    orderNumber: OrderNumber,
    customerId: Option[CustomerId],
    grandTotalGross: Money,
    currency: Currency,
    occurredAt: Instant,
) extends DomainEvent

final case class OrderConfirmed(
    orderId: OrderId,
    orderNumber: OrderNumber,
    lines: NonEmptyChunk[OrderLine],
    occurredAt: Instant,
) extends DomainEvent

final case class OrderCancelled(
    orderId: OrderId,
    reason: NonEmptyString,
    occurredAt: Instant,
) extends DomainEvent

// ── Public Inbound Ports (Module API) ────────────────────────────────────────

trait CartService:
  def addLine(cartId: CartId, line: CartLine): IO[OrderError, CartView]
  def removeLine(cartId: CartId, lineId: OrderLineId): IO[OrderError, CartView]
  def getCart(cartId: CartId): IO[OrderError.CartNotFound, CartView]

trait OrderService:
  def placeOrder(cmd: PlaceOrderCmd): IO[OrderError, OrderView]
  def confirmPayment(orderId: OrderId, reference: Option[String]): IO[OrderError, OrderView]
  def markInProduction(orderId: OrderId): IO[OrderError, OrderView]
  def markReadyForDispatch(orderId: OrderId): IO[OrderError, OrderView]
  def markDispatched(orderId: OrderId): IO[OrderError, OrderView]
  def complete(orderId: OrderId): IO[OrderError, OrderView]
  def cancel(orderId: OrderId, reason: NonEmptyString): IO[OrderError, OrderView]
  def getOrder(orderId: OrderId): IO[OrderError.OrderNotFound, OrderView]

object OrderService:
  def placeOrder(cmd: PlaceOrderCmd): ZIO[OrderService, OrderError, OrderView] =
    ZIO.serviceWithZIO[OrderService](_.placeOrder(cmd))
