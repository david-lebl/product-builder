package mpbuilder.domain

import zio.test.*
import zio.test.Assertion.*
import mpbuilder.domain.model.*
import mpbuilder.domain.sample.*
import mpbuilder.domain.service.*
import mpbuilder.domain.pricing.*
import mpbuilder.domain.validation.*

object CategoryPresetSpec extends ZIOSpecDefault:

  private val catalog = SampleCatalog.catalog
  private val ruleset = SampleRules.ruleset
  private val pricelist = SamplePricelist.pricelistCzkSheet

  /** Build a ConfigurationRequest from a CategoryPreset and its owning category,
    * merging preset spec overrides with category-level defaults.
    */
  private def requestFromPreset(
      cat: ProductCategory,
      preset: CategoryPreset,
  ): ConfigurationRequest =
    val defaultSpecs = defaultSpecsForCategory(cat)
    val overrideKinds = preset.specOverrides.map(SpecValue.specKind).toSet
    val mergedSpecs = defaultSpecs.filterNot(s => overrideKinds.contains(SpecValue.specKind(s))) ++ preset.specOverrides

    ConfigurationRequest(
      categoryId = cat.id,
      printingMethodId = preset.printingMethodId,
      components = preset.componentPresets.map { cp =>
        ComponentRequest(
          role = cp.role,
          materialId = cp.materialId,
          inkConfiguration = cp.inkConfiguration,
          finishes = cp.finishSelections,
        )
      },
      specs = mergedSpecs,
    )

  /** Produce generic default specs for a category's required spec kinds. */
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

  def spec = suite("CategoryPresetSpec")(

    suite("preset model")(
      test("ProductCategory.defaultPreset returns first preset") {
        val cat = SampleCatalog.businessCards
        assertTrue(
          cat.defaultPreset.isDefined,
          cat.defaultPreset.get.id == PresetId.unsafe("preset-bc-basic"),
        )
      },
      test("ProductCategory.presetById finds correct preset") {
        val cat = SampleCatalog.businessCards
        val premium = cat.presetById(PresetId.unsafe("preset-bc-premium"))
        assertTrue(
          premium.isDefined,
          premium.get.name(Language.En) == "Premium",
        )
      },
      test("ProductCategory.presetById returns None for missing preset") {
        assertTrue(SampleCatalog.businessCards.presetById(PresetId.unsafe("nonexistent")).isEmpty)
      },
      test("category without presets returns empty list and None defaultPreset") {
        val cat = SampleCatalog.free
        assertTrue(
          cat.presets.isEmpty,
          cat.defaultPreset.isEmpty,
        )
      },
    ),

    suite("every preset yields a valid configuration")(
      test("all sample category presets build valid configurations") {
        val results = catalog.categories.values.toList.flatMap { cat =>
          cat.presets.map { preset =>
            val request = requestFromPreset(cat, preset)
            val configId = ConfigurationId.unsafe(s"test-${preset.id.value}")
            val result = ConfigurationBuilder.build(request, catalog, ruleset, configId)
            (cat.id, preset.id, result)
          }
        }

        // Every preset should succeed
        val failures = results.collect {
          case (catId, presetId, result) if result.toEither.isLeft =>
            s"${catId.value}/${presetId.value}: validation failed"
        }

        assertTrue(failures.isEmpty)
      },
    ),

    suite("every preset produces a priced configuration")(
      test("all sample category presets can be priced") {
        val results = catalog.categories.values.toList.flatMap { cat =>
          cat.presets.map { preset =>
            val request = requestFromPreset(cat, preset)
            val configId = ConfigurationId.unsafe(s"test-${preset.id.value}")
            val buildResult = ConfigurationBuilder.build(request, catalog, ruleset, configId)
            val config = buildResult.toEitherWith(identity).toOption
            val priceResult = config.map(c =>
              PriceCalculator.calculate(c, pricelist, Language.En)
            )
            (cat.id, preset.id, config.isDefined, priceResult)
          }
        }

        val failures = results.collect {
          case (catId, presetId, false, _) =>
            s"${catId.value}/${presetId.value}: configuration build failed"
          case (catId, presetId, true, Some(priceResult)) if priceResult.toEither.isLeft =>
            s"${catId.value}/${presetId.value}: pricing failed"
        }

        assertTrue(failures.isEmpty)
      },
    ),

    suite("business cards presets")(
      test("basic preset uses coated 300gsm and 4+0 CMYK") {
        val preset = SampleCatalog.businessCards.presets.head
        val cp = preset.componentPresets.head
        assertTrue(
          cp.materialId == SampleCatalog.coated300gsmId,
          cp.inkConfiguration == InkConfiguration.cmyk4_0,
          preset.printingMethodId == SampleCatalog.digitalId,
        )
      },
      test("premium preset uses matte 350gsm and 4+4 CMYK with finishes") {
        val preset = SampleCatalog.businessCards.presets(1)
        val cp = preset.componentPresets.head
        assertTrue(
          cp.materialId == SampleCatalog.coatedMatte350gsmId,
          cp.inkConfiguration == InkConfiguration.cmyk4_4,
          cp.finishSelections.exists(_.finishId == SampleCatalog.matteLaminationId),
          cp.finishSelections.exists(_.finishId == SampleCatalog.roundCornersId),
        )
      },
      test("basic preset spec overrides include business card size and qty 100") {
        val preset = SampleCatalog.businessCards.presets.head
        val sizeOpt = preset.specOverrides.collectFirst { case s: SpecValue.SizeSpec => s }
        val qtyOpt = preset.specOverrides.collectFirst { case q: SpecValue.QuantitySpec => q }
        assertTrue(
          sizeOpt.isDefined,
          sizeOpt.get.dimension == Dimension(85, 55),
          qtyOpt.isDefined,
          qtyOpt.get.quantity.value == 100,
        )
      },
    ),

    suite("flyers preset — single preset")(
      test("flyers category has exactly one preset") {
        assertTrue(SampleCatalog.flyers.presets.size == 1)
      },
    ),

    suite("multi-component presets")(
      test("booklets preset has cover and body components") {
        val preset = SampleCatalog.booklets.presets.head
        val roles = preset.componentPresets.map(_.role).toSet
        assertTrue(
          roles.contains(ComponentRole.Cover),
          roles.contains(ComponentRole.Body),
        )
      },
      test("roll-up economy preset has main and stand components") {
        val preset = SampleCatalog.rollUps.presets.head
        val roles = preset.componentPresets.map(_.role).toSet
        assertTrue(
          roles.contains(ComponentRole.Main),
          roles.contains(ComponentRole.Stand),
        )
      },
      test("roll-ups has two presets (Economy / Premium)") {
        assertTrue(SampleCatalog.rollUps.presets.size == 2)
      },
    ),

    suite("preset localization")(
      test("business cards basic preset has both EN and CS names") {
        val preset = SampleCatalog.businessCards.presets.head
        assertTrue(
          preset.name(Language.En) == "Basic",
          preset.name(Language.Cs) == "Základní",
        )
      },
      test("business cards premium preset has both EN and CS descriptions") {
        val preset = SampleCatalog.businessCards.presets(1)
        assertTrue(
          preset.description.isDefined,
          preset.description.get(Language.En).nonEmpty,
          preset.description.get(Language.Cs).nonEmpty,
        )
      },
    ),
  )
