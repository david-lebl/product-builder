package mpbuilder.pricing

import mpbuilder.catalog.*

/** A complete catalog export with catalog, rules, and pricelist(s). */
final case class CatalogExport(
    catalog: ProductCatalog,
    ruleset: CompatibilityRuleset,
    pricelists: List[Pricelist],
)
