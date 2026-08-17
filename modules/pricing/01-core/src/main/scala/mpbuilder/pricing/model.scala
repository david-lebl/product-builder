package mpbuilder.pricing

import mpbuilder.commons.*

/** A product to be priced.
  *
  * Pricing declares its own input type rather than borrowing catalog's `ConfigurationSnapshot`:
  * a context's core never depends on another context's core. The payload is opaque here too —
  * pricing's `02-infra` is the only place that knows how to read it — so the two contexts stay
  * independently extractable even though they pass the same bytes around.
  */
final case class ProductSpec(payload: String, catalogVersion: String)

/** How fast the shop should produce this. */
enum ProductionSpeed:
  case Express, Standard, Economy

// ── Quotes ────────────────────────────────────────────────────────────────

final case class LineItemView(
    label: String,
    unitPrice: Money,
    quantity: Int,
    lineTotal: Money,
)

/** The cost of one component (cover, body, …) of a multi-part product. */
final case class ComponentQuote(
    role: String,
    lines: List[LineItemView],
    sheetsUsed: Int,
)

/** A complete, itemized price.
  *
  * Unlike a configuration, this is flat enough to be a plain value — so an order line can freeze
  * the whole thing and re-render it years later without asking pricing anything.
  */
final case class PriceQuote(
    components: List[ComponentQuote],
    surcharges: List[LineItemView],
    setupFees: List[LineItemView],
    subtotal: Money,
    quantityMultiplier: BigDecimal,
    speedSurcharge: Option[LineItemView],
    minimumApplied: Option[Money],
    total: Money,
    currency: Currency,
    quantity: Int,
    pricelistVersion: String,
)

final case class QuoteRequest(
    spec: ProductSpec,
    speed: ProductionSpeed = ProductionSpeed.Standard,
    customerId: Option[String] = None,
    currency: Currency = Currency.CZK,
    language: Language = Language.En,
)

/** Several lines priced together, with their combined total. */
final case class BasketQuote(
    lines: List[PriceQuote],
    total: Money,
    currency: Currency,
)

// ── Speed availability ────────────────────────────────────────────────────

/** Whether a speed tier can be sold for this product, and at what premium.
  *
  * Returning offers rather than a bare `Set[ProductionSpeed]` is deliberate: when Express is off
  * the table the customer is owed an explanation, and the shop's reason (saturated floor vs. a
  * binding method that needs curing time) is exactly what the configurator displays.
  */
enum SpeedOffer:
  case Available(speed: ProductionSpeed, surcharge: Money)
  case Unavailable(speed: ProductionSpeed, reason: SpeedUnavailable)

enum SpeedUnavailable:
  /** The production floor is too busy to promise a rush turnaround at any price. */
  case ShopSaturated

  /** A per-category tier restriction refused it.
    *
    * TODO(Phase 8): split into `QuantityAboveCap`, `BindingRequiresCuring` and `MaterialExcluded`.
    * The legacy `TierRestrictionValidator` reports these as free text, so the structure cannot be
    * recovered here without changing it — which belongs with the pricing extraction, not the façade.
    */
  case Restricted(explanation: LocalizedString)

// ── Discounts ─────────────────────────────────────────────────────────────

final case class DiscountContext(
    orderValue: Money,
    categoryIds: Set[String],
    customerType: Option[String] = None,
    customerId: Option[String] = None,
    now: Timestamp,
)

/** The result of offering a discount code.
  *
  * A refused code is a normal outcome, not an error: the customer mistyped, or the code expired,
  * and the configurator shows them why. Only a genuine fault (unreadable spec, missing pricelist)
  * belongs in the error channel.
  */
enum DiscountOutcome:
  case Applied(code: String, discount: Money, finalTotal: Money)
  case Refused(code: String, reason: LocalizedString)
