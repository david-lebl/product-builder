package mpbuilder.pricing

import zio.json.*

/** JSON codecs for pricing types (pricelists, pricing rules, catalog export).
  *
  * Re-exports the catalog codecs (which in turn re-export the kernel codecs),
  * so a single
  * {{{
  * import mpbuilder.pricing.PricingCodecs.given
  * }}}
  * brings everything needed for catalog and pricelist persistence.
  */
object PricingCodecs:

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

  given JsonCodec[CatalogExport] = DeriveJsonCodec.gen[CatalogExport]
