# 2026-04-21 — Fix Manufacturing UI crash and Pricelist empty table

**PR:** #134 (fix for regressions introduced in #133)
**Author:** agent
**Type:** bugfix

## Summary

Fixed two regressions introduced by PR #133 (Banner Product Overhaul):

1. **Manufacturing UI showed nothing** — the entire Manufacturing app was blank because `ManufacturingViewModel` crashed during initialization.
2. **Catalog Editor / Pricelist table was empty** — the pricing rule table in `PricelistEditorView` showed no rows whenever the selected pricelist contained any of the new rule types added in PR #133.

## Changes Made

- `modules/ui/src/main/scala/mpbuilder/ui/manufacturing/ManufacturingViewModel.scala`
  - Updated `generateSampleOrders()` banner config to use `SampleCatalog.pvc510gId` instead of `SampleCatalog.vinylId` (the Banners category was changed in PR #133 to only allow `pvc510gId`)

- `modules/ui/src/main/scala/mpbuilder/ui/catalog/views/PricelistEditorView.scala`
  - Added match cases for `MaterialAreaTier`, `GrommetSpacingAreaPrice`, `FinishLinearMeterPrice`, `ScoringCountSurcharge`, `ScoringSetupFee` to `pricingRuleSummary`
  - Added match cases for `ScoringCountSurcharge` and `ScoringSetupFee` to `pricingRuleTypeName`

## Decisions & Rationale

- **Banner config material change**: The simplest correct fix. The Banners category template was updated in PR #133 to use PVC 510g (`pvc510gId`) as its only allowed material, but the manufacturing VM's sample data generator still referenced the old `vinylId`. Aligning them was the right fix.

- **Match exhaustiveness**: Scala's sealed enum match is checked at compile time with warnings; at runtime, a missing case throws `MatchError` inside a Laminar signal `.map`, which causes the signal to stop propagating and the reactive UI element to stay empty (or in Scala.js the whole module to crash at init if it occurs at object initialization). Adding all missing cases to both `pricingRuleSummary` and `pricingRuleTypeName` ensures exhaustive coverage.

## Issues Encountered

### 1. Manufacturing UI crash due to `vinylId` in banners config

**Root cause**: `ManufacturingViewModel.generateSampleOrders()` called:
```scala
val banners = buildConfig(
  SampleCatalog.bannersId,
  SampleCatalog.uvInkjetId,
  List(ComponentRequest(ComponentRole.Main, SampleCatalog.vinylId, ...)),  // vinylId no longer allowed
  ...
)
```
`ConfigurationBuilder.build(...)` returned a `Failure` (material not in `allowedMaterialIds`). The code then called `.toEither.toOption.get`, which threw `NoSuchElementException`. Because this happens in the initializer of the `ManufacturingViewModel` singleton object, the entire Scala.js object initialization failed, making the Manufacturing tab completely blank.

**Fix**: Changed `vinylId` to `pvc510gId`.

### 2. Pricelist table empty due to non-exhaustive match

**Root cause**: `pricingRuleSummary` and `pricingRuleTypeName` in `PricelistEditorView` were not updated when new `PricingRule` variants were added. PR #133 added `MaterialAreaTier`, `GrommetSpacingAreaPrice`, `FinishLinearMeterPrice` to `pricingRuleTypeName`, but forgot to add them to `pricingRuleSummary`. Additionally, `ScoringCountSurcharge` and `ScoringSetupFee` (added in a prior session) were missing from both functions. When the sample pricelist (which contains all these rule types) was rendered, the `.map` on the signal threw `MatchError`, causing Laminar to stop updating the `children` of the table body, leaving it empty.

**Fix**: Added all five missing cases to `pricingRuleSummary`; added the two missing cases to `pricingRuleTypeName`.

## Follow-up Items

- [ ] Consider adding a compile-time exhaustiveness check or test to catch non-exhaustive `PricingRule` matches in UI code when new rule variants are added
- [ ] The `ruleTypes` dropdown list in `PricelistEditorView.pricingRuleForm` (line ~134) still does not include `MaterialAreaTier`, `GrommetSpacingAreaPrice`, `FinishLinearMeterPrice`, `ScoringCountSurcharge`, or `ScoringSetupFee` — these rules cannot currently be created via the UI editor (only displayed). This is pre-existing and tracked separately.
