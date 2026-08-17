package mpbuilder.orderintake

import mpbuilder.commons.*

// ── Delivery ──────────────────────────────────────────────────────────────

/** Whether the goods are collected or shipped.
  *
  * The distinction is not cosmetic: only a shipped order needs a delivery address, so the client
  * can drive that part of the form from the kind rather than from a hardcoded list of ids.
  */
enum DeliveryKind:
  case Pickup, Courier

/** One way the customer may take delivery.
  *
  * Pickup points and couriers are one flat list rather than two, because at the moment of choosing
  * they are alternatives to each other. Grouping for display is the client's business.
  */
final case class DeliveryOption(
    id: String,
    kind: DeliveryKind,
    name: LocalizedString,
    /** The shop's address for a pickup point; the expected transit time for a courier. */
    detail: LocalizedString,
    surcharge: Money,
    currency: Currency,
)

/** The chosen delivery, priced. */
final case class DeliveryCharge(
    optionId: String,
    name: LocalizedString,
    surcharge: Money,
    /** True when a free-delivery discount code cancelled the surcharge. */
    waived: Boolean,
):
  /** What is actually added to the total. */
  def payable: Money = if waived then Money.zero else surcharge

// ── Payment ───────────────────────────────────────────────────────────────

enum PaymentMethod:
  /** Bank transfer with a QR code. Always available — it needs no trust and no integration. */
  case BankTransferQR

  /** Card payment. Described in the product spec, not yet built. */
  case Card

  /** Invoice on account, settled after delivery. Only for approved corporate customers. */
  case InvoiceOnAccount

object PaymentMethod:
  def parse(raw: String): Option[PaymentMethod] =
    values.find(_.toString.equalsIgnoreCase(raw))

/** Whether a payment method may be used, and if not, why.
  *
  * Mirrors pricing's `SpeedOffer` deliberately. A bare list of permitted methods would leave the
  * customer to guess why invoicing is missing; a corporate buyer whose account is still awaiting
  * approval is owed the actual reason.
  */
enum PaymentOffer:
  case Available(method: PaymentMethod, name: LocalizedString)
  case Unavailable(method: PaymentMethod, name: LocalizedString, reason: LocalizedString)

  def method: PaymentMethod

// ── Buyer ─────────────────────────────────────────────────────────────────

/** Who order-intake believes is buying.
  *
  * A guest is not a degenerate customer with blank fields — it is the absence of a customer record,
  * so everything customer-derived is absent with it.
  */
enum Buyer:
  case Guest
  case Known(
      customerId: String,
      displayName: String,
      email: String,
      customerType: String,
      canPayOnAccount: Boolean,
  )

  /** The customer record behind this buyer, if there is one. */
  def customerRef: Option[String] = this match
    case Guest    => None
    case k: Known => Some(k.customerId)

  /** The type discount rules dispatch on. A guest is its own type, not a missing one. */
  def customerTypeName: String = this match
    case Guest    => "Guest"
    case k: Known => k.customerType

// ── Options, drafts and quotes ────────────────────────────────────────────

/** Everything the checkout form needs in order to be rendered. */
final case class CheckoutOptions(
    buyer: Buyer,
    delivery: List[DeliveryOption],
    payment: List[PaymentOffer],
    currency: Currency,
)

/** The choices made so far.
  *
  * Every field is optional because the customer quotes as they go: a total with no delivery chosen
  * yet is a legitimate thing to ask for, and refusing to answer until the form is complete would
  * make the running total appear only at the last step.
  */
final case class CheckoutDraft(
    deliveryOptionId: Option[String] = None,
    discountCode: Option[String] = None,
    paymentMethod: Option[String] = None,
)

/** What an accepted code is worth. Order-intake's own vocabulary; pricing has its own. */
enum DiscountBenefit:
  case Amount(off: Money)
  case FreeDelivery

enum DiscountDecision:
  case Applied(code: String, benefit: DiscountBenefit)
  case Refused(code: String, reason: LocalizedString)

/** The basket priced as it would be ordered.
  *
  * Lines are re-quoted against today's prices rather than read from the stored quotes, because this
  * is the number the customer is about to agree to pay.
  */
final case class CheckoutQuote(
    lines: List[BasketItemView],
    itemsTotal: Money,
    /** Absent when no code was offered. A refused code is present, and says why. */
    discount: Option[DiscountDecision],
    discountOff: Money,
    delivery: Option[DeliveryCharge],
    grandTotal: Money,
    currency: Currency,
    payment: Option[PaymentMethod],
    quotedAt: Timestamp,
)

/** A speed tier for a basket line: what it would cost, or why it cannot be sold. */
enum SpeedOfferView:
  case Available(speed: ProductionSpeed, surcharge: Money)
  case Unavailable(speed: ProductionSpeed, reason: LocalizedString)

  def speed: ProductionSpeed

/** Change how fast an existing line is produced. */
final case class UpdateSpeed(speed: String)
