package mpbuilder.domain.codec

import zio.json.*
import mpbuilder.catalog.*
import mpbuilder.domain.pricing.*

/** JSON codecs for all domain types needed for catalog and pricelist persistence.
  *
  * Usage:
  * {{{
  * import mpbuilder.domain.codec.DomainCodecs.given
  *
  * val json = catalog.toJson
  * val catalog = json.fromJson[ProductCatalog]
  * }}}
  */
object DomainCodecs:

  // ── Kernel & catalog codecs (re-exported) ────────────────────────────────

  export mpbuilder.catalog.CatalogCodecs.given

  // ── Queue thresholds ─────────────────────────────────────────────────────

  given JsonCodec[QueueThreshold] = DeriveJsonCodec.gen[QueueThreshold]

  // ── Pricing rules ────────────────────────────────────────────────────────

  given JsonCodec[AreaTier] = DeriveJsonCodec.gen[AreaTier]
  given JsonCodec[GrommetSpacingTier] = DeriveJsonCodec.gen[GrommetSpacingTier]
  given JsonCodec[PricingRule] = DeriveJsonCodec.gen[PricingRule]
  given JsonCodec[Pricelist] = DeriveJsonCodec.gen[Pricelist]

  // ── Combined export type ─────────────────────────────────────────────────

  /** A complete catalog export with catalog, rules, and pricelist(s). */
  final case class CatalogExport(
    catalog: ProductCatalog,
    ruleset: CompatibilityRuleset,
    pricelists: List[Pricelist],
  )

  given JsonCodec[CatalogExport] = DeriveJsonCodec.gen[CatalogExport]
