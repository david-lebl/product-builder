package mpbuilder.orderintake

import mpbuilder.commons.*

/** Who is acting.
  *
  * A sum rather than an `Option[userId]` plus a nullable session: a caller is either anonymous with
  * a session token or authenticated with a user id, never both and never neither.
  */
enum Actor:
  case Anonymous(session: String)
  case Authenticated(userId: String, customerId: Option[String], isStaff: Boolean)

/** A product to be bought.
  *
  * Order-intake never looks inside. The payload is catalog's business; this context stores it,
  * fingerprints it for deduplication, and hands it back. That opacity is what makes "an order
  * cannot be changed by a later catalog edit" structural rather than a rule to remember — this
  * context could not re-resolve a material price if it wanted to, because it cannot see one.
  */
final case class ProductSpec(payload: String, catalogVersion: String):
  def fingerprint: String = payload.hashCode.toHexString

/** How fast the shop should produce this. Order-intake's own vocabulary; pricing has its own. */
enum ProductionSpeed:
  case Express, Standard, Economy

object ProductionSpeed:
  def parse(raw: String): Option[ProductionSpeed] =
    values.find(_.toString.equalsIgnoreCase(raw))

/** A price as it was quoted, frozen with the moment and the pricelist it came from.
  *
  * Kept flat and self-describing so a basket line can be rendered without asking pricing anything —
  * and so the same value can later be frozen onto an order line unchanged.
  */
final case class QuotedPrice(
    total: Money,
    currency: Currency,
    quotedAt: Timestamp,
    pricelistVersion: String,
)

// ── Views ─────────────────────────────────────────────────────────────────

final case class BasketItemView(
    id: String,
    description: LocalizedString,
    quantity: Int,
    speed: String,
    artworkId: Option[String],
    price: QuotedPrice,
)

final case class BasketView(
    id: String,
    items: List[BasketItemView],
    total: Money,
    currency: Currency,
    itemCount: Int,
    expiresAt: Timestamp,
)

// ── Requests ──────────────────────────────────────────────────────────────

/** Add a configured product to the basket.
  *
  * `configuration` is the catalog configuration request, verbatim, as JSON. Order-intake passes it
  * straight to its product port without parsing: duplicating catalog's request shape here would be
  * a large, permanently-drifting copy, and parsing it would make this context depend on catalog's
  * vocabulary — the thing the boundary exists to prevent.
  */
final case class AddItem(
    configuration: String,
    quantity: Int,
    speed: String = "Standard",
    artworkId: Option[String] = None,
)

final case class UpdateQuantity(quantity: Int)
