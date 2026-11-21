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

opaque type DeliveryOptionId = String
object DeliveryOptionId:
  def apply(raw: String): Validation[String, DeliveryOptionId] =
    if raw.trim.nonEmpty then Validation.succeed(raw.trim)
    else Validation.fail("DeliveryOptionId must not be empty")
  def unsafe(raw: String): DeliveryOptionId = raw
  extension (id: DeliveryOptionId) def value: String = id

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

/** Frozen commercial valuation snapshot persisted with an order line (per 1 copy/set). */
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

/** Frozen cart/order-level promo code discount snapshot (validated by `pricing`). */
final case class AppliedPromoSnapshot(
    codeId: DiscountCodeId,
    code: NonEmptyString,
    discountNet: Money,
    freeDelivery: Boolean = false,
)

// ── Delivery Catalog & Checkout Option Models ────────────────────────────────

final case class ShopPickupLocation(
    id: DeliveryOptionId,
    name: LocalizedString,
    address: LocalizedString,
    openingHours: Option[LocalizedString] = None,
    isActive: Boolean = true,
)

final case class CourierServiceOption(
    id: DeliveryOptionId,
    name: LocalizedString,
    estimatedDelivery: LocalizedString,
    feeNet: Money,
    currency: Currency,
    taxRate: TaxRate,
    freeShippingThresholdNet: Option[Money] = None,
    maxWeightGrams: Option[BigDecimal] = None,
    isActive: Boolean = true,
)

final case class BankAccountConfig(
    iban: NonEmptyString,
    bic: Option[NonEmptyString] = None,
    accountHolder: NonEmptyString,
)

/** Customer's selection during checkout — server resolves authoritative shipping fee from this. */
enum DeliveryOptionChoice:
  case ShopPickup(locationId: DeliveryOptionId)
  case Courier(serviceId: DeliveryOptionId, shippingAddress: Address)

/** Priced and eligibility-evaluated delivery option presented to the storefront UI. */
final case class DeliveryOptionQuote(
    id: DeliveryOptionId,
    isPickup: Boolean,
    name: LocalizedString,
    description: LocalizedString,
    feeNet: Money,
    taxRate: TaxRate,
    feeGross: Money,
    currency: Currency,
    freeShippingApplied: Boolean,
    available: Boolean,
    unavailableReason: Option[String] = None,
)

/** Eligibility-evaluated payment option presented to the storefront UI. */
final case class PaymentOption(
    method: PaymentMethod,
    available: Boolean,
    unavailableReason: Option[String] = None,
)

/** Actionable payment details returned after order placement. */
enum PaymentInstructions:
  case BankTransferQR(
      iban: NonEmptyString,
      bic: Option[NonEmptyString],
      accountHolder: NonEmptyString,
      variableSymbol: NonEmptyString,
      amount: Money,
      currency: Currency,
      qrPayload: String,
  )
  case CardPending(checkoutUrl: Option[String] = None, message: String)
  case InvoiceScheduled(dueDays: Int, invoiceReference: NonEmptyString)

// ── Cart Models ──────────────────────────────────────────────────────────────

final case class CartLine(
    lineId: OrderLineId,
    product: ProductSnapshot,
    runQuantity: Quantity,
    versionCount: VersionCount,
    speed: ManufacturingSpeed,
    price: LinePriceSnapshot,
    artworkIds: Map[ComponentKey, ArtworkId] = Map.empty,
    copies: Quantity = Quantity.one,
    weightGrams: Option[BigDecimal] = None,
):
  def lineNetTotal: Money   = (price.netAmount * BigDecimal(copies.value)).rounded
  def lineTaxTotal: Money   = (price.taxAmount * BigDecimal(copies.value)).rounded
  def lineGrossTotal: Money = (price.grossAmount * BigDecimal(copies.value)).rounded
  def totalWeightGrams: Option[BigDecimal] = weightGrams.map(_ * BigDecimal(copies.value))

  def toOrderLine: OrderLine =
    OrderLine(lineId, product, runQuantity, versionCount, speed, price, artworkIds, copies, weightGrams)

final case class CartView(
    id: CartId,
    customerId: Option[CustomerId],
    currency: Currency,
    lines: List[CartLine],
    appliedPromo: Option[AppliedPromoSnapshot],
    linesNetTotal: Money,
    promoDiscountNet: Money,
    netTotal: Money,
    taxTotal: Money,
    grossTotal: Money,
    totalWeightGrams: Option[BigDecimal],
):
  def appliedPromoCode: Option[String] = appliedPromo.map(_.code.value)

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
    copies: Quantity = Quantity.one,
    weightGrams: Option[BigDecimal] = None,
):
  def lineNetTotal: Money   = (price.netAmount * BigDecimal(copies.value)).rounded
  def lineTaxTotal: Money   = (price.taxAmount * BigDecimal(copies.value)).rounded
  def lineGrossTotal: Money = (price.grossAmount * BigDecimal(copies.value)).rounded
  def totalWeightGrams: Option[BigDecimal] = weightGrams.map(_ * BigDecimal(copies.value))

final case class OrderTotals(
    linesNetTotal: Money,
    shippingNet: Money,
    taxTotal: Money,
    grandTotalGross: Money,
    currency: Currency,
    promoDiscountNet: Money = Money.zero,
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
    appliedPromo: Option[AppliedPromoSnapshot] = None,
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
    appliedPromo: Option[AppliedPromoSnapshot] = None,
)

enum OrderError(val message: String):
  case CartNotFound(cartId: CartId)
      extends OrderError(s"Cart '${cartId.value}' not found")
  case CartLineNotFound(cartId: CartId, lineId: OrderLineId)
      extends OrderError(s"Line '${lineId.value}' not found in cart '${cartId.value}'")
  case OrderNotFound(orderId: OrderId)
      extends OrderError(s"Order '${orderId.value}' not found")
  case EmptyCart(cartId: CartId)
      extends OrderError(s"Cannot place order from empty cart '${cartId.value}'")
  case CurrencyMismatch(expected: Currency, actual: Currency)
      extends OrderError(s"Currency mismatch: expected $expected, got $actual")
  case InvoiceNotAllowed(customerEmail: EmailAddress)
      extends OrderError(s"Customer '${customerEmail.value}' is not approved for InvoiceOnAccount payment")
  case DeliveryOptionNotFound(optionId: DeliveryOptionId)
      extends OrderError(s"Delivery option '${optionId.value}' not found or inactive")
  case DeliveryOptionUnavailable(optionId: DeliveryOptionId, reason: String)
      extends OrderError(s"Delivery option '${optionId.value}' is unavailable: $reason")
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
  def createCart(cartId: CartId, currency: Currency, customerId: Option[CustomerId] = None): IO[OrderError, CartView]
  def addLine(cartId: CartId, line: CartLine): IO[OrderError, CartView]
  def updateLineCopies(cartId: CartId, lineId: OrderLineId, copies: Quantity): IO[OrderError, CartView]
  def replaceLine(cartId: CartId, line: CartLine): IO[OrderError, CartView]
  def removeLine(cartId: CartId, lineId: OrderLineId): IO[OrderError, CartView]
  def clearCart(cartId: CartId): IO[OrderError, CartView]
  def applyPromo(cartId: CartId, promo: AppliedPromoSnapshot): IO[OrderError, CartView]
  def removePromo(cartId: CartId): IO[OrderError, CartView]
  def assignCustomer(cartId: CartId, customerId: Option[CustomerId]): IO[OrderError, CartView]
  def getCart(cartId: CartId): IO[OrderError.CartNotFound, CartView]

trait CheckoutOptionsService:
  def availableDeliveryOptions(
      currency: Currency,
      itemsNetSubtotal: Money,
      totalWeightGrams: Option[BigDecimal],
      freeDeliveryPromo: Boolean = false,
  ): UIO[List[DeliveryOptionQuote]]

  def resolveDelivery(
      choice: DeliveryOptionChoice,
      currency: Currency,
      itemsNetSubtotal: Money,
      totalWeightGrams: Option[BigDecimal],
      freeDeliveryPromo: Boolean = false,
  ): IO[OrderError, DeliverySelection]

  def availablePaymentOptions(
      invoiceApproved: Boolean,
      cardEnabled: Boolean = false,
  ): UIO[List[PaymentOption]]

  def generatePaymentInstructions(
      orderNumber: OrderNumber,
      method: PaymentMethod,
      grandTotalGross: Money,
      currency: Currency,
      invoiceDueDays: Option[Int] = None,
  ): UIO[PaymentInstructions]

trait OrderService:
  def placeOrder(cmd: PlaceOrderCmd): IO[OrderError, OrderView]
  def confirmPayment(orderId: OrderId, reference: Option[String]): IO[OrderError, OrderView]
  def markInProduction(orderId: OrderId): IO[OrderError, OrderView]
  def markReadyForDispatch(orderId: OrderId): IO[OrderError, OrderView]
  def markDispatched(orderId: OrderId): IO[OrderError, OrderView]
  def complete(orderId: OrderId): IO[OrderError, OrderView]
  def cancel(orderId: OrderId, reason: NonEmptyString): IO[OrderError, OrderView]
  def getOrder(orderId: OrderId): IO[OrderError.OrderNotFound, OrderView]
  def listOrdersByCustomer(customerId: CustomerId): UIO[List[OrderView]]

object OrderService:
  def placeOrder(cmd: PlaceOrderCmd): ZIO[OrderService, OrderError, OrderView] =
    ZIO.serviceWithZIO[OrderService](_.placeOrder(cmd))
