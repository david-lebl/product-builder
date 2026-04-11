package mpbuilder.domain.service

import mpbuilder.domain.model.*
import mpbuilder.domain.pricing.*
import mpbuilder.domain.rules.*
import zio.prelude.Validation

/** Computes a starting price for a [[CategoryPreset]] by building a
  * configuration from the preset defaults and pricing it.
  *
  * The returned price represents the total for the preset's default quantity.
  * Callers can use this for "starting from X" labels in the catalog.
  */
object PresetPriceService:

  /** Compute the price for a preset's default configuration.
    *
    * Returns `None` when the preset cannot be built or priced (e.g. missing
    * pricing rules).
    */
  def computePrice(
      category: ProductCategory,
      preset: CategoryPreset,
      catalog: ProductCatalog,
      ruleset: CompatibilityRuleset,
      pricelist: Pricelist,
  ): Option[PriceBreakdown] =
    val componentRequests = preset.componentPresets.map { cp =>
      ComponentRequest(
        role = cp.role,
        materialId = cp.materialId,
        inkConfiguration = cp.inkConfiguration,
        finishes = cp.finishSelections,
      )
    }

    // Use the preset spec overrides, filling in generic defaults for any
    // missing required specs.
    val defaultSpecs = defaultSpecsForCategory(category)
    val overrideKinds = preset.specOverrides.map(SpecValue.specKind).toSet
    val mergedSpecs = defaultSpecs.filterNot(s => overrideKinds.contains(SpecValue.specKind(s))) ++ preset.specOverrides

    val request = ConfigurationRequest(
      categoryId = category.id,
      printingMethodId = preset.printingMethodId,
      components = componentRequests,
      specs = mergedSpecs,
    )

    val configId = ConfigurationId.unsafe(s"price-check-${preset.id.value}")
    val configV = ConfigurationBuilder.build(request, catalog, ruleset, configId)
    configV match
      case Validation.Success(_, config) =>
        PriceCalculator.calculate(config, pricelist) match
          case Validation.Success(_, breakdown) => Some(breakdown)
          case _                                => None
      case _ => None

  private def defaultSpecsForCategory(cat: ProductCategory): List[SpecValue] =
    val kinds = cat.requiredSpecKinds
    val specs = List.newBuilder[SpecValue]
    if kinds.contains(SpecKind.Quantity) then
      specs += SpecValue.QuantitySpec(Quantity.unsafe(1))
    if kinds.contains(SpecKind.Size) then
      specs += SpecValue.SizeSpec(Dimension(210, 297))
    if kinds.contains(SpecKind.Orientation) then
      specs += SpecValue.OrientationSpec(Orientation.Portrait)
    if kinds.contains(SpecKind.FoldType) then
      specs += SpecValue.FoldTypeSpec(FoldType.Half)
    if kinds.contains(SpecKind.BindingMethod) then
      specs += SpecValue.BindingMethodSpec(BindingMethod.SaddleStitch)
    if kinds.contains(SpecKind.Pages) then
      specs += SpecValue.PagesSpec(8)
    specs.result()
