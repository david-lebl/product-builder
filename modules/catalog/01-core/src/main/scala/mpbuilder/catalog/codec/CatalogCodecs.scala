package mpbuilder.catalog.codec

import mpbuilder.catalog.rules.*
import mpbuilder.product.codec.ProductCodecs
import mpbuilder.product.model.*
import zio.json.*

/** JSON codecs for what catalog owns on top of the product kernel: its compatibility rules.
  *
  * The product model's own codecs live in the kernel with the model, and are re-exported here so a
  * caller encoding a ruleset does not have to import two objects to do it.
  */
object CatalogCodecs:

  export ProductCodecs.given

  // ── Predicates ───────────────────────────────────────────────────────────

  given JsonCodec[SpecPredicate] = DeriveJsonCodec.gen[SpecPredicate]
  given JsonCodec[ConfigurationPredicate] = DeriveJsonCodec.gen[ConfigurationPredicate]

  // ── Compatibility rules ──────────────────────────────────────────────────

  given JsonCodec[CompatibilityRule] = DeriveJsonCodec.gen[CompatibilityRule]
  given JsonCodec[CompatibilityRuleset] = DeriveJsonCodec.gen[CompatibilityRuleset]
