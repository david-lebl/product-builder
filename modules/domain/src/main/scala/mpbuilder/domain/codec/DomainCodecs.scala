package mpbuilder.domain.codec

import mpbuilder.commons.*

import zio.json.*
import mpbuilder.product.model.*
import mpbuilder.catalog.rules.*
import mpbuilder.catalog.codec.CatalogCodecs.given
import mpbuilder.pricing.engine.*

/** JSON codecs for what the legacy module still owns.
  *
  * Catalog's moved to `mpbuilder.product.codec.ProductCodecs` with the model they encode, and are
  * re-exported here so existing callers keep working: a codec object that only covers half of a
  * payload's types would make every caller import two.
  */
object DomainCodecs:

  export mpbuilder.product.codec.ProductCodecs.given

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
