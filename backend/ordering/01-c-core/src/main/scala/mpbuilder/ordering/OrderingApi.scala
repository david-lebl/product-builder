package mpbuilder.ordering

import mpbuilder.shared.*
import kyo.*
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

// The types below mirror the catalog's configuration model so an order line carries the complete
// production specification without `ordering` depending on `catalog` (translated by the BFF ACLs).

/** Frozen finish selection of a component: which finish, on which side, with which settings. */
final case class FinishSnapshot(
    finishId: FinishId,
    name: LocalizedString,
    /** Catalog `FinishType` key (e.g. `Lamination`, `RoundCorners`). */
    finishType: String,
    /** `None` when the finish has no side (e.g. die cut, grommets). */
    side: Option[FinishSnapshot.Side],
    /** `None` when the finish takes no settings. */
    params: Option[FinishSnapshot.Params],
)

object FinishSnapshot:
  enum Side:
    case Front, Back, Both

  enum FoilColor:
    case Gold, Silver, Copper, RoseGold, Holographic

  enum Params:
    case RoundCorners(cornerCount: Int, radiusMm: Int)
    case FoilStamping(color: FoilColor)
    case Grommets(spacingMm: Int)
    case Perforation(pitchMm: Int)
    case LinearLength(lengthMeters: BigDecimal)
    case Scoring(creaseCount: Int)

final case class ComponentSnapshot(
    key: ComponentKey,
    materialId: MaterialId,
    materialName: LocalizedString,
    printingMethodId: Option[PrintingMethodId],
    inkNotation: Option[String],
    finishes: List[FinishSnapshot],
    flatSize: Dimension2D,
    finishedSize: Dimension2D,
    sheetsUsed: Int,
):
  def isDoubleSidedPrint: Boolean = inkNotation.exists(n => !n.endsWith("/0"))

/** Frozen physical specification of the ordered product (catalog `ProductSpecs` without quantity/versions,
  * which live on the line itself).
  */
final case class ProductSpecSnapshot(
    dimensions: ProductSpecSnapshot.Dimensions,
    /** `None` for products without an orientation (e.g. circular, packaging). */
    orientation: Option[ProductSpecSnapshot.Orientation],
    /** Effective bleed: the customer's choice or the default for the printing process, resolved when frozen. */
    bleed: NonNegativeMm,
    /** `None` for unbound products. */
    binding: Option[ProductSpecSnapshot.Binding],
    deliveryFormat: ProductSpecSnapshot.DeliveryFormat,
)

object ProductSpecSnapshot:
  enum Orientation:
    case Portrait, Landscape

  enum FoldType:
    case Half, Tri, Gate, Accordion, ZFold, RollFold, FrenchFold, CrossFold

  enum Dimensions:
    case Flat2D(size: Dimension2D)
    case Folded2D(finishedSize: Dimension2D, foldType: FoldType)
    case Box3D(box: Dimension3D, customBlankSize: Option[Dimension2D] = None)
    case Circular(diameter: PositiveMm)
    case PrintArea(area: Dimension2D, variantLabel: Option[String] = None)

  enum BindingMethod:
    case SaddleStitch, PerfectBinding, SpiralBinding, WireOBinding, CaseBinding

  final case class Binding(method: BindingMethod, totalPages: Int, selfCover: Boolean)

  enum DeliveryFormat:
    case CutSingles
    case KissCutSheets
    case RollLabels(coreDiameterMm: Int, unwindDirection: Int)
    case PackedUnits

/** Frozen product specification snapshot persisted with an order line. */
final case class ProductSnapshot(
    categoryId: CategoryId,
    categoryName: LocalizedString,
    catalogVersion: Version,
    components: NonEmptyChunk[ComponentSnapshot],
    specs: ProductSpecSnapshot,
    summaryNotes: Option[String] = None,
):
  def component(key: ComponentKey): ComponentSnapshot =
    components.find(_.key == key).getOrElse(components.head)

  def bleed: NonNegativeMm = specs.bleed

  /** Pages the customer's artwork for `key` must have: the bound page count for a booklet body
    * (minus a separate 4-page cover), otherwise one page per printed side.
    */
  def expectedArtworkPages(key: ComponentKey): Int =
    val comp  = component(key)
    val sides = if comp.isDoubleSidedPrint then 2 else 1
    specs.binding match
      case Some(b) if comp.key == ComponentKey.Body =>
        val coverPages = if b.selfCover then 0 else 4
        math.max(1, b.totalPages - coverPages)
      case _ => sides

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
    productionNote: Option[String] = None,
):
  def lineNetTotal: Money   = (price.netAmount * BigDecimal(copies.value)).rounded
  def lineTaxTotal: Money   = (price.taxAmount * BigDecimal(copies.value)).rounded
  def lineGrossTotal: Money = (price.grossAmount * BigDecimal(copies.value)).rounded
  def totalWeightGrams: Option[BigDecimal] = weightGrams.map(_ * BigDecimal(copies.value))

  def toOrderLine: OrderLine =
    OrderLine(
      lineId, product, runQuantity, versionCount, speed, price, artworkIds, copies, weightGrams,
      productionNote = ProductionNote.normalize(productionNote),
    )

/** Customer-authored, optional production instructions for one order line ("print the logo side first",
  * "keep the cut tight to the trim", …). A single current value — not a conversation: it is a spec input
  * for prepress/manufacturing, editable by the customer only until the order is accepted.
  */
object ProductionNote:
  val MaxLength: Int = 1000

  /** Trims, maps blank to `None` and caps the length at [[MaxLength]]. */
  def normalize(raw: Option[String]): Option[String] =
    raw.map(_.trim).filter(_.nonEmpty).map(_.take(MaxLength))

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
  case Accepted
  case InProduction
  case ReadyForDispatch
  case Dispatched
  case Completed
  case OnHold(reason: NonEmptyString)
  case Cancelled(reason: NonEmptyString)

  def kind: OrderStatusKind = this match
    case Placed           => OrderStatusKind.Placed
    case AwaitingPayment  => OrderStatusKind.AwaitingPayment
    case Confirmed        => OrderStatusKind.Confirmed
    case Accepted         => OrderStatusKind.Accepted
    case InProduction     => OrderStatusKind.InProduction
    case ReadyForDispatch => OrderStatusKind.ReadyForDispatch
    case Dispatched       => OrderStatusKind.Dispatched
    case Completed        => OrderStatusKind.Completed
    case OnHold(_)        => OrderStatusKind.OnHold
    case Cancelled(_)     => OrderStatusKind.Cancelled

  def isTerminal: Boolean = this match
    case Completed | Cancelled(_) => true
    case _                        => false

  /** Customers may upload or replace print artwork freely while the order has not yet been accepted. */
  def allowsCustomerArtworkUpload: Boolean = this match
    case Placed | AwaitingPayment | Confirmed => true
    case _                                    => false

enum OrderStatusKind:
  case Placed, AwaitingPayment, Confirmed, Accepted, InProduction, ReadyForDispatch, Dispatched, Completed, OnHold, Cancelled

enum OrderActor:
  case System
  case Customer(customerId: Option[CustomerId], name: NonEmptyString)
  case Employee(employeeId: EmployeeId, name: Option[NonEmptyString] = None)

final case class OrderStatusEvent(
    from: Option[OrderStatus],
    to: OrderStatus,
    actor: OrderActor,
    note: Option[String] = None,
    occurredAt: Instant,
)

final case class OrderInternalNote(
    authorId: EmployeeId,
    authorName: Option[NonEmptyString] = None,
    text: NonEmptyString,
    createdAt: Instant,
)

final case class OrderFilter(
    statuses: Set[OrderStatusKind] = Set.empty,
    paymentStatus: Option[PaymentStatus] = None,
    customerId: Option[CustomerId] = None,
    query: Option[String] = None,
)

enum OrderPriority(val rank: Int):
  case Urgent   extends OrderPriority(0)
  case Standard extends OrderPriority(1)
  case Economy  extends OrderPriority(2)

object OrderPriority:
  def fromSpeed(speed: ManufacturingSpeed): OrderPriority = speed match
    case ManufacturingSpeed.Express  => Urgent
    case ManufacturingSpeed.Standard => Standard
    case ManufacturingSpeed.Economy  => Economy

  def turnaroundHours(speed: ManufacturingSpeed): Long = speed match
    case ManufacturingSpeed.Express  => 24L
    case ManufacturingSpeed.Standard => 72L
    case ManufacturingSpeed.Economy  => 120L

  def forLines(lines: NonEmptyChunk[OrderLine]): OrderPriority =
    lines.map(l => fromSpeed(l.speed)).minBy(_.rank)

  def deadlineFor(placedAt: Instant, speed: ManufacturingSpeed): Instant =
    placedAt.plusSeconds(turnaroundHours(speed) * 3600L)

  def orderDeadline(placedAt: Instant, lines: NonEmptyChunk[OrderLine]): Instant =
    lines.map(l => deadlineFor(placedAt, l.speed)).min

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
    /** Internal specification note maintained by staff (not shown to the customer). */
    specNote: Option[String] = None,
    /** Customer's optional production instructions for this line (see [[ProductionNote]]). */
    productionNote: Option[String] = None,
):
  def lineNetTotal: Money   = (price.netAmount * BigDecimal(copies.value)).rounded
  def lineTaxTotal: Money   = (price.taxAmount * BigDecimal(copies.value)).rounded
  def lineGrossTotal: Money = (price.grossAmount * BigDecimal(copies.value)).rounded
  def totalWeightGrams: Option[BigDecimal] = weightGrams.map(_ * BigDecimal(copies.value))
  def estimatedDeadline(placedAt: Instant): Instant = OrderPriority.deadlineFor(placedAt, speed)

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
    statusHistory: List[OrderStatusEvent] = Nil,
    internalNotes: List[OrderInternalNote] = Nil,
):
  def priority: OrderPriority        = OrderPriority.forLines(lines)
  def calculatedDeadline: Instant    = OrderPriority.orderDeadline(placedAt, lines)
  def hasMixedSpeeds: Boolean        = lines.map(_.speed).distinct.size > 1
  def isPaid: Boolean                = payment.status == PaymentStatus.Confirmed

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

final case class EditOrderLineCmd(
    orderId: OrderId,
    lineId: OrderLineId,
    runQuantity: Option[Quantity] = None,
    copies: Option[Quantity] = None,
    speed: Option[ManufacturingSpeed] = None,
    specNote: Option[Option[String]] = None,
)

enum OrderError(val message: String):
  case CartNotFound(cartId: CartId)
      extends OrderError(s"Cart '${cartId.value}' not found")
  case CartLineNotFound(cartId: CartId, lineId: OrderLineId)
      extends OrderError(s"Line '${lineId.value}' not found in cart '${cartId.value}'")
  case OrderNotFound(orderId: OrderId)
      extends OrderError(s"Order '${orderId.value}' not found")
  case OrderLineNotFound(orderId: OrderId, lineId: OrderLineId)
      extends OrderError(s"Line '${lineId.value}' not found in order '${orderId.value}'")
  case OrderNotEditable(orderId: OrderId, status: OrderStatus)
      extends OrderError(s"Order '${orderId.value}' cannot be edited while in status $status")
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
  case OrderNotOnHold(orderId: OrderId, currentStatus: OrderStatus)
      extends OrderError(s"Order '${orderId.value}' is not on hold (current status: $currentStatus)")
  case ProductionNoteLocked(orderId: OrderId, status: OrderStatus)
      extends OrderError(s"Production instructions of order '${orderId.value}' can no longer be changed (status: $status)")

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

final case class OrderAccepted(
    orderId: OrderId,
    orderNumber: OrderNumber,
    acceptedBy: EmployeeId,
    occurredAt: Instant,
) extends DomainEvent

final case class OrderPutOnHold(
    orderId: OrderId,
    reason: NonEmptyString,
    by: EmployeeId,
    occurredAt: Instant,
) extends DomainEvent

final case class OrderStageChanged(
    orderId: OrderId,
    from: OrderStatus,
    to: OrderStatus,
    by: EmployeeId,
    occurredAt: Instant,
) extends DomainEvent

final case class OrderCancelled(
    orderId: OrderId,
    reason: NonEmptyString,
    occurredAt: Instant,
) extends DomainEvent

// ── Public Inbound Ports (Module API) ────────────────────────────────────────

trait CartService:
  def createCart(cartId: CartId, currency: Currency, customerId: Option[CustomerId] = None): CartView < (Async & Abort[OrderError])
  def addLine(cartId: CartId, line: CartLine): CartView < (Async & Abort[OrderError])
  def updateLineCopies(cartId: CartId, lineId: OrderLineId, copies: Quantity): CartView < (Async & Abort[OrderError])
  def updateLineProductionNote(cartId: CartId, lineId: OrderLineId, note: Option[String]): CartView < (Async & Abort[OrderError])
  def replaceLine(cartId: CartId, line: CartLine): CartView < (Async & Abort[OrderError])
  def removeLine(cartId: CartId, lineId: OrderLineId): CartView < (Async & Abort[OrderError])
  def clearCart(cartId: CartId): CartView < (Async & Abort[OrderError])
  def applyPromo(cartId: CartId, promo: AppliedPromoSnapshot): CartView < (Async & Abort[OrderError])
  def removePromo(cartId: CartId): CartView < (Async & Abort[OrderError])
  def assignCustomer(cartId: CartId, customerId: Option[CustomerId]): CartView < (Async & Abort[OrderError])
  def getCart(cartId: CartId): CartView < (Async & Abort[OrderError.CartNotFound])

trait CheckoutOptionsService:
  def availableDeliveryOptions(
      currency: Currency,
      itemsNetSubtotal: Money,
      totalWeightGrams: Option[BigDecimal],
      freeDeliveryPromo: Boolean = false,
  ): List[DeliveryOptionQuote] < Async

  def resolveDelivery(
      choice: DeliveryOptionChoice,
      currency: Currency,
      itemsNetSubtotal: Money,
      totalWeightGrams: Option[BigDecimal],
      freeDeliveryPromo: Boolean = false,
  ): DeliverySelection < (Async & Abort[OrderError])

  def availablePaymentOptions(
      invoiceApproved: Boolean,
      cardEnabled: Boolean = false,
  ): List[PaymentOption] < Async

  def generatePaymentInstructions(
      orderNumber: OrderNumber,
      method: PaymentMethod,
      grandTotalGross: Money,
      currency: Currency,
      invoiceDueDays: Option[Int] = None,
  ): PaymentInstructions < Async

  // ── Admin Delivery & Payment Configuration ─────────────────────────────────
  def listAllPickupLocations: List[ShopPickupLocation] < Async
  def upsertPickupLocation(loc: ShopPickupLocation): ShopPickupLocation < Async
  def deletePickupLocation(id: DeliveryOptionId): Unit < Async
  def listAllCourierOptions: List[CourierServiceOption] < Async
  def upsertCourierOption(opt: CourierServiceOption): CourierServiceOption < Async
  def deleteCourierOption(id: DeliveryOptionId, currency: Currency): Unit < Async
  def listBankAccounts: Map[Currency, BankAccountConfig] < Async
  def upsertBankAccount(currency: Currency, config: BankAccountConfig): BankAccountConfig < Async

trait OrderService:
  def placeOrder(cmd: PlaceOrderCmd): OrderView < (Async & Abort[OrderError])
  def confirmPayment(orderId: OrderId, reference: Option[String]): OrderView < (Async & Abort[OrderError])
  def markInProduction(orderId: OrderId): OrderView < (Async & Abort[OrderError])
  def markReadyForDispatch(orderId: OrderId): OrderView < (Async & Abort[OrderError])
  def markDispatched(orderId: OrderId): OrderView < (Async & Abort[OrderError])
  def complete(orderId: OrderId): OrderView < (Async & Abort[OrderError])
  def cancel(orderId: OrderId, reason: NonEmptyString): OrderView < (Async & Abort[OrderError])
  def getOrder(orderId: OrderId): OrderView < (Async & Abort[OrderError.OrderNotFound])
  def listOrdersByCustomer(customerId: CustomerId): List[OrderView] < Async

  // ── Staff Order Intake & Manual Stage Management ───────────────────────────
  def listOrders(filter: OrderFilter = OrderFilter()): List[OrderView] < Async
  def acceptOrder(
      orderId: OrderId,
      by: EmployeeId,
      byName: Option[NonEmptyString] = None,
      note: Option[String] = None,
  ): OrderView < (Async & Abort[OrderError])
  def putOnHold(
      orderId: OrderId,
      by: EmployeeId,
      byName: Option[NonEmptyString] = None,
      reason: NonEmptyString,
  ): OrderView < (Async & Abort[OrderError])
  def resumeFromHold(
      orderId: OrderId,
      by: EmployeeId,
      byName: Option[NonEmptyString] = None,
      note: Option[String] = None,
  ): OrderView < (Async & Abort[OrderError])
  def advanceStage(
      orderId: OrderId,
      target: OrderStatus,
      by: EmployeeId,
      byName: Option[NonEmptyString] = None,
      note: Option[String] = None,
  ): OrderView < (Async & Abort[OrderError])
  def confirmPaymentByStaff(
      orderId: OrderId,
      by: EmployeeId,
      byName: Option[NonEmptyString] = None,
      reference: Option[String] = None,
  ): OrderView < (Async & Abort[OrderError])
  def markPaymentStatus(
      orderId: OrderId,
      status: PaymentStatus,
      by: EmployeeId,
      byName: Option[NonEmptyString] = None,
      reference: Option[String] = None,
  ): OrderView < (Async & Abort[OrderError])
  def editOrderLine(
      cmd: EditOrderLineCmd,
      by: EmployeeId,
      byName: Option[NonEmptyString] = None,
  ): OrderView < (Async & Abort[OrderError])
  def cancelByStaff(
      orderId: OrderId,
      by: EmployeeId,
      byName: Option[NonEmptyString] = None,
      reason: NonEmptyString,
  ): OrderView < (Async & Abort[OrderError])
  def addInternalNote(
      orderId: OrderId,
      by: EmployeeId,
      byName: Option[NonEmptyString] = None,
      text: NonEmptyString,
  ): OrderView < (Async & Abort[OrderError])
  def attachArtwork(
      orderId: OrderId,
      lineIndex: Int,
      componentKey: ComponentKey,
      artworkId: ArtworkId,
  ): OrderView < (Async & Abort[OrderError])

  /** Customer edits a line's production instructions; allowed only while artwork uploads are open
    * (`OrderStatus.allowsCustomerArtworkUpload`). The change is recorded in the status history.
    */
  def updateLineProductionNote(
      orderId: OrderId,
      lineId: OrderLineId,
      note: Option[String],
      actor: OrderActor,
  ): OrderView < (Async & Abort[OrderError])
