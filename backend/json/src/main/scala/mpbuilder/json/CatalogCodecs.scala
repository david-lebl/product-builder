package mpbuilder.json

import kyo.*
import mpbuilder.catalog.*
import mpbuilder.json.JsonSupport.*
import mpbuilder.shared.*
import mpbuilder.json.SharedCodecs.given
import mpbuilder.json.JsonSupport.given

/** Schemas for the public `catalog` model (catalog, rules, configuration). */
object CatalogCodecs:

  given Schema[RuleId]          = stringOpaque("RuleId")(RuleId(_))(_.value)
  given Schema[PresetId]        = stringOpaque("PresetId")(PresetId(_))(_.value)
  given Schema[ConfigurationId] = stringOpaque("ConfigurationId")(ConfigurationId(_))(_.value)

  given Schema[FinishType] = Schema.derived
  given finishTypeMap[V: Schema]: Schema[Map[FinishType, V]] =
    keyedMap("FinishType")(enumKey("FinishType", FinishType.values))(_.toString)


  given Schema[Material]             = Schema.derived
  given Schema[Finish]               = Schema.derived
  given Schema[PrintingMethod]       = Schema.derived
  given Schema[ProductSpecs]         = Schema.derived
  given Schema[ProductCategory]      = Schema.derived
  given Schema[ShowcaseListing]      = Schema.derived
  given Schema[ProductCatalog]       = Schema.derived
  // Recursive rule predicates: named givens so the derivation's self-references resolve to them.
  given componentPredicateSchema: Schema[ComponentPredicate] = Schema.derived
  given dimensionPredicateSchema: Schema[DimensionPredicate] = Schema.derived
  given quantityPredicateSchema: Schema[QuantityPredicate]   = Schema.derived
  given bindingPredicateSchema: Schema[BindingPredicate]     = Schema.derived
  given foldPredicateSchema: Schema[FoldPredicate]           = Schema.derived
  given configPredicateSchema: Schema[ConfigPredicate]       = Schema.derived

  given Schema[CompatibilityRule]    = Schema.derived
  given Schema[CompatibilityRuleset] = Schema.derived
  given Schema[ConfigureProductCmd]  = Schema.derived
  given Schema[ConfiguredProduct]    = Schema.derived
  given Schema[ConfigurationResult]  = Schema.derived
  given Schema[CatalogError]         = Schema.derived
