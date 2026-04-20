# 2026-04-20 — Banner Product Overhaul

**PR:** copilot/update-banner-pricing-model
**Author:** copilot agent
**Type:** feature

## Summary

Overhauled the banner product (`cat-banners`) to use realistic materials, pricing, and rules. The old configuration used generic Adhesive Vinyl at a flat 420 CZK/m² with dead grommet spacing data and no dimension constraint. The new configuration uses PVC Banner 510g with area-tiered pricing, grommet area-based pricing driven by spacing, a gum rope accessory with linear-metre pricing, a max-dimension compatibility rule (150×150 cm), and a dependency rule (gum rope requires grommets).

All changes follow the existing "rules as data" architecture — three new `PricingRule` variants, one new `FinishType`, one new `FinishParameters`, and one new `ConfigurationPredicate`.

## Changes Made

### Domain model
- `modules/domain/src/main/scala/mpbuilder/domain/model/finish.scala`
  - Added `FinishType.RopeAccessory` (assigned to `FinishCategory.LargeFormat`)
  - Added `FinishParameters.RopeParams(lengthMeters: BigDecimal)`
- `modules/domain/src/main/scala/mpbuilder/domain/pricing/PricingRule.scala`
  - Added `MaterialAreaTier(materialId, tiers: List[AreaTier])` — picks highest matching tier by `minSqm`
  - Added `GrommetSpacingAreaPrice(finishId, tiers: List[GrommetSpacingTier])` — area price keyed by grommet spacing
  - Added `FinishLinearMeterPrice(finishId, pricePerMeter)` — for rope/accessory finishes
  - Added supporting data classes `AreaTier` and `GrommetSpacingTier`
- `modules/domain/src/main/scala/mpbuilder/domain/rules/predicates.scala`
  - Added `ConfigurationPredicate.HasFinishId(finishId: FinishId)` — true iff a component has the finish selected
- `modules/domain/src/main/scala/mpbuilder/domain/validation/RuleEvaluator.scala`
  - Added evaluator case for `HasFinishId`
- `modules/domain/src/main/scala/mpbuilder/domain/validation/ConfigurationValidator.scala`
  - Added validation case for `RopeParams` (length 0 < m ≤ 50, finish type must be `RopeAccessory`)

### Pricing engine
- `modules/domain/src/main/scala/mpbuilder/domain/pricing/PriceCalculator.scala`
  - `calculateComponentBreakdown`: checks `MaterialAreaTier` before `MaterialAreaPrice` (tier wins)
  - `computeFinishLines`: added three-priority logic: GrommetSpacingAreaPrice > FinishLinearMeterPrice > existing FinishSurcharge/FinishTypeSurcharge
  - Fixed structural bug: refactored the `areaRule`/`sheetRule`/`baseRule` cascade from broken unreachable-case structure to correct nested `match`

### Sample data
- `modules/domain/src/main/scala/mpbuilder/domain/sample/SampleCatalog.scala`
  - Added `mat-pvc-510g` (PVC Banner 510g, Vinyl family, WaterResistant + SmoothSurface properties)
  - Added `fin-gum-rope` (Gum rope, RopeAccessory type, Both sides)
  - Updated `cat-banners` component template: `allowedMaterialIds = Set(pvc510gId)` (replaced vinylId)
  - Added `fin-gum-rope` to banner `allowedFinishIds`
  - Updated banner presets: Standard (1000×1000mm), Outdoor with Grommets (1000×1500mm) — both within the new 1500×1500 max
- `modules/domain/src/main/scala/mpbuilder/domain/sample/SampleRules.scala`
  - Added `SpecConstraint(MaxDimension(1500, 1500))` — banners up to 150×150 cm
  - Added `ConfigurationConstraint(Or(Not(HasFinishId(gumRopeId)), HasFinishId(grommetsId)))` — gum rope requires grommets
- `modules/domain/src/main/scala/mpbuilder/domain/sample/SamplePricelist.scala`
  - CZK: replaced flat 420 CZK/m² vinyl price for banners with `MaterialAreaTier` for pvc510g (600/500/450/400 CZK/m² at 0/2/5/10 m²)
  - CZK: replaced flat 8 CZK grommet surcharge with `GrommetSpacingAreaPrice` (40 CZK/m² at 500mm, 60 CZK/m² at 300mm)
  - CZK+Sheet: added `FinishLinearMeterPrice(gumRopeId, 18 CZK/m)`
  - USD: added flat `MaterialAreaPrice(pvc510gId, 18 USD/m²)`; added `FinishLinearMeterPrice(gumRopeId, 0.80 USD/m)`

### Codecs
- `modules/domain/src/main/scala/mpbuilder/domain/codec/DomainCodecs.scala`
  - Added `JsonCodec[AreaTier]`, `JsonCodec[GrommetSpacingTier]`, `JsonCodec[FinishParameters.RopeParams]`

### UI
- `modules/ui/src/main/scala/mpbuilder/ui/productbuilder/components/FinishSelector.scala`
  - Added `RopeAccessory` branch: length input (0.5–50m, step 0.5), warning when >20m, info note when grommets not selected
  - Added default params initialization for `RopeAccessory` (5m initial length)
  - Added `import mpbuilder.domain.sample.SampleCatalog` for `grommetsId` reference
- `modules/ui/src/main/scala/mpbuilder/ui/productbuilder/components/BasketView.scala`
  - Added `RopeParams` case to `finishDescription` for exhaustive match
- `modules/ui/src/main/scala/mpbuilder/ui/catalog/views/PricelistEditorView.scala`
  - Added `MaterialAreaTier`, `GrommetSpacingAreaPrice`, `FinishLinearMeterPrice` cases to `pricingRuleTypeName` for exhaustive match

### Tests
- `modules/domain/src/test/scala/mpbuilder/domain/PriceCalculatorSpec.scala`
  - Updated existing "banner with area-based calculation" test (vinyl → pvc510g, USD flat price)
  - Updated "area pricing without size spec" test (vinyl → pvc510g)
  - Added 4 CZK tier tests (1m², 2m², 5m², 10m²)
  - Added 2 grommet area-price tests (500mm → 40 CZK/m², 300mm → 60 CZK/m²)
  - Added 2 gum rope tests (10m → 180 CZK, 25m → 450 CZK)
- `modules/domain/src/test/scala/mpbuilder/domain/ConfigurationBuilderSpec.scala`
  - Updated all banner tests to use `pvc510gId` instead of `vinylId`
  - Added "gum rope without grommets is rejected" test
  - Added "banner 200×100 cm exceeds max dimension" test
- `modules/domain/src/test/scala/mpbuilder/domain/CatalogQueryServiceSpec.scala`
  - Updated "returns only vinyl for banners" → "returns only PVC 510g for banners"
- `modules/domain/src/test/scala/mpbuilder/domain/WorkflowGeneratorSpec.scala`
  - Updated UV inkjet test to use `pvc510gId` for banners
- `modules/domain/src/test/scala/mpbuilder/domain/LocalizationSpec.scala`
  - Updated banner description assertion: "vinyl banners" → "PVC banners"

## Decisions & Rationale

- **Area-tiered pricing as data**: `MaterialAreaTier` follows the existing "rules as sealed ADTs" principle — it's fully serializable and can be exported to JSON.
- **Grommet area pricing**: Uses `2·(w+h)/spacing + 4` to display approximate grommet count in the line item label, while the actual price is area-based to avoid corner/rounding ambiguity.
- **USD pricelist stays flat**: The CZK pricelist gets `MaterialAreaTier` for PVC 510g; the USD pricelist stays flat (`MaterialAreaPrice`) to minimize test churn. Revisit when USD customers need tiered pricing.
- **`Implies` via `Or(Not(a), b)`**: No new combinator needed — the existing `Or`/`Not` predicates compose to implication.
- **MaxDimension(1500, 1500)**: The `RuleEvaluator` already supported this predicate. No evaluator change needed.
- **Preset dimensions revised**: The previous outdoor preset at 1500×3000mm and standard at 1000×2000mm both exceeded the new 1500mm per-dimension limit. Updated to 1000×1500mm and 1000×1000mm respectively.
- **`FinishSelector` imports `SampleCatalog`**: For the "requires grommets" UX hint, the UI checks whether `grommetsId` is selected. This is a pragmatic choice; a cleaner alternative would be a ViewModel method, but that adds indirection for a single ID check.

## Issues Encountered

- **Broken match structure in `PriceCalculator`**: The initial edit used `areaTierRule.orElse(areaRule.map(_ => null).flatMap(_ => None)) match` with `case _ =>` and `case None =>` at the same level. Since `case _` is exhaustive, `case None =>` was unreachable, so the sheet/base pricing path was dead. Fixed by replacing with a clean nested `areaTierRule match { case Some => …; case None => areaRule match … }`.
- **Pre-existing UI compile errors**: `PricelistEditorView.scala` and `FinishEditorView.scala` had pre-existing compile errors (FormComponents API mismatch) unrelated to this PR. The UI compiled with warnings (not errors) before, and my changes only added match cases without touching the broken API calls.
- **Outdoor banner preset exceeded MaxDimension**: The preset at 1500×3000mm violated the new rule. Resized to 1000×1500mm.
- **`LocalizationSpec` text mismatch**: The test expected "vinyl banners" in the category description; the new description says "PVC banners". Updated the test.

## Follow-up Items

- [ ] USD pricelist: add `MaterialAreaTier` for pvc510g when USD customers require tiered banner pricing
- [ ] Fix pre-existing UI compile errors in `PricelistEditorView.scala` and `FinishEditorView.scala` (FormComponents API mismatch — unrelated to this PR)
- [ ] Consider adding a `FinishId` field to `LineItem` for more robust test assertions and UI rendering
- [ ] UI smoke test: verify grommet price changes at tier boundaries, gum rope validation, rope pricing
- [ ] Consider externally-produced banners >150 cm as a future product variant
