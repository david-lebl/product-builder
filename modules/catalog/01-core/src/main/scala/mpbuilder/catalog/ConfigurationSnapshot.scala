package mpbuilder.catalog

import mpbuilder.commons.*

/** Identifies the state of the catalog a snapshot was taken against. */
final case class CatalogVersion(value: String)

/** An immutable, self-contained record of one configured product.
  *
  * The payload is deliberately opaque to everyone but catalog. Other contexts — order-intake above
  * all — store it, hash it, and hand it back; they must never reach inside it. That is what makes
  * "an order cannot be changed by a later catalog edit" a structural property rather than a rule
  * someone has to remember: order-intake could not re-resolve a material price if it wanted to,
  * because it cannot see one.
  *
  * The payload embeds the resolved catalog entities in full, so it stays readable even after the
  * material, finish or category it was built from has been renamed, repriced, or deleted.
  */
final case class ConfigurationSnapshot(payload: String, catalogVersion: CatalogVersion):

  /** Stable identity for deduplication — two basket items with the same spec are the same product.
    * Compares the payload, not the version, so a re-quote against a newer catalog still matches.
    */
  def fingerprint: String = payload.hashCode.toHexString

/** A snapshot together with the parts of it a caller is allowed to display. */
final case class ConfigurationView(
    snapshot: ConfigurationSnapshot,
    description: LocalizedString,
)
