package mpbuilder.orderintake

import mpbuilder.commons.*

// ── Who is buying, and where it goes ──────────────────────────────────────

/** The buyer's own details, as given at checkout.
  *
  * A sum rather than a record with three co-dependent optionals (company / registration / VAT id).
  * Today's `ContactInfo` carries all three as `Option`s with an unwritten rule that they arrive
  * together, and nothing stops a VAT id on an order with no company name.
  */
enum OrderBuyer:
  case Individual(firstName: String, lastName: String, email: String, phone: String)
  case Business(
      firstName: String,
      lastName: String,
      email: String,
      phone: String,
      company: String,
      businessId: Option[String],
      vatId: Option[String],
  )

  def firstName: String
  def lastName: String
  def email: String
  def phone: String

  def displayName: String = this match
    case i: Individual => s"${i.firstName} ${i.lastName}"
    case b: Business   => s"${b.company} (${b.firstName} ${b.lastName})"

final case class Address(street: String, city: String, zip: String, country: String)

// ── Requests ──────────────────────────────────────────────────────────────

/** Everything the checkout wizard collected, submitted at once.
  *
  * One command rather than a server-side state machine: the client holds the wizard, and the whole
  * thing is validated together so every problem comes back in one response. A step-by-step server
  * would throw that away and report one problem per round trip.
  */
final case class PlaceOrder(
    buyer: OrderBuyer,
    billing: Address,
    /** Absent means "ship to the billing address". */
    shipping: Option[Address] = None,
    deliveryOptionId: Option[String] = None,
    paymentMethod: String,
    discountCode: Option[String] = None,
    note: Option[String] = None,
    /** The total the customer was shown, if the client wants the price guarded.
      *
      * Pricing is queue-driven and genuinely time-varying, so a basket quoted an hour ago may cost
      * more now. Supplying this turns a moved price into a refusal the customer can confirm,
      * instead of a silent charge they never agreed to.
      */
    expectedTotal: Option[Money] = None,
)

final case class CancelOrder(reason: String)
final case class RejectOrder(reason: String)
final case class ConfirmPayment(reference: String)

/** Which slice of a list to return. */
final case class Page(offset: Int = 0, limit: Int = 20)

final case class Paged[A](items: List[A], total: Int, offset: Int, limit: Int)

/** Staff order search.
  *
  * Filters on the status *group*, never on a leaf, so adding a leaf cannot break a saved filter.
  */
final case class OrderSearch(statusGroup: Option[String] = None, query: Option[String] = None)

// ── Views ─────────────────────────────────────────────────────────────────

/** The order's status, flattened for a client.
  *
  * `group` is what a UI should branch on — a badge, a filter — and `kind` is the detail. A client
  * that switches on `group` keeps working when a leaf is added; one that switches on `kind` gets a
  * sensible default rather than a crash.
  */
final case class OrderStatusView(
    group: String,
    kind: String,
    at: Option[Timestamp],
    reason: Option[String],
)

final case class OrderLineView(
    id: String,
    lineNo: Int,
    description: LocalizedString,
    configuration: String,
    quantity: Int,
    speed: String,
    artworkId: Option[String],
    price: QuotedPrice,
)

final case class OrderTotalsView(
    items: Money,
    discount: Money,
    delivery: Money,
    grand: Money,
    currency: Currency,
)

final case class OrderView(
    id: String,
    number: String,
    status: OrderStatusView,
    buyer: OrderBuyer,
    customerId: Option[String],
    lines: List[OrderLineView],
    billing: Address,
    shipping: Option[Address],
    delivery: Option[DeliveryCharge],
    paymentMethod: String,
    discount: Option[DiscountDecision],
    totals: OrderTotalsView,
    speed: String,
    note: Option[String],
    placedAt: Timestamp,
)

/** Enough to render a row in an order list, and no more. */
final case class OrderSummaryView(
    id: String,
    number: String,
    status: OrderStatusView,
    buyerName: String,
    lineCount: Int,
    total: Money,
    currency: Currency,
    placedAt: Timestamp,
)

// ── Events ────────────────────────────────────────────────────────────────

/** What order-intake tells the rest of the system.
  *
  * Public, because other contexts consume these — manufacturing generates workflows from
  * `OrderPlaced`, notifications sends the confirmation. They carry everything a consumer needs, so
  * nobody has to call back and ask: an event that forces a lookup is a distributed join.
  */
enum OrderEvent:
  case OrderPlaced(
      orderId: String,
      number: String,
      buyerName: String,
      customerId: Option[String],
      lines: List[OrderLineView],
      speed: String,
      total: Money,
      currency: Currency,
      placedAt: Timestamp,
  )
  case OrderPaid(orderId: String, number: String, method: String, reference: String, at: Timestamp)
  case OrderCancelled(orderId: String, number: String, reason: String, at: Timestamp)
  case OrderRejected(orderId: String, number: String, reason: String, at: Timestamp)

  def orderId: String
  def number: String
