# 2026-04-22 — Calendar/Binding Domain Redesign

**PR:** copilot/calendar-binding-domain-redesign
**Author:** copilot agent
**Type:** feature

## Summary

Redesigned the calendar product and binding domain to replace the coarse `SpiralBinding`/`WireOBinding` pair with a single `LoopBinding` method, introduce explicit `FrontCover`, `BackCover`, and `Binding` component roles, and add a rich material model for binding consumables (plastic coils, wire-o, transparent covers). The calendar category now requires four components: an optional transparent front cover, a printed back cover, body pages, and a binding material component. Binding material pricing supports per-meter linear pricing (`MaterialLinearPrice`) and flat per-unit pricing (`MaterialFixedPrice`), with the bound edge determined by the new `BoundEdge` category field.

## Changes Made

### Domain model (`modules/domain/src/main/scala/mpbuilder/domain/model/`)
- **`material.scala`**: Added `Plastic`, `Metal` variants to `MaterialFamily`; new `HexColor` opaque type; new `MaterialAttribute` sealed trait with `MaxBoundEdgeLengthMm(mm: Int)` and `Color(hex: HexColor)` cases; added `attributes: Set[MaterialAttribute]` field to `Material`; added `isPrintable` helper
- **`component.scala`**: Added `FrontCover`, `BackCover`, `Binding` to `ComponentRole` enum (kept `Cover` for backward compat)
- **`specification.scala`**: Replaced `SpiralBinding` + `WireOBinding` with `LoopBinding` in `BindingMethod` enum
- **`category.scala`**: Added `BoundEdge` enum (`LongEdge`, `ShortEdge`, `Width`, `Height`); added optional `boundEdge: Option[BoundEdge]` field to `ProductCategory`
- **`configuration.scala`**: Added `frontCoverComponent`, `backCoverComponent`, `bindingComponent` helper methods on `ProductConfiguration`

### Rules & validation (`modules/domain/src/main/scala/mpbuilder/domain/rules/`, `validation/`)
- **`CompatibilityRule.scala`**: Added `BindingMaterialConstrainsSize(reason)` and `ComponentRequired(role, whenBindingMethod, reason)` cases
- **`ConfigurationError.scala`**: Added `BindingMaterialSizeExceeded` and `ComponentRequiredForBindingMethod` error cases
- **`ConfigurationValidator.scala`**: Updated to pass full `ProductCategory` to `RuleEvaluator.evaluate`
- **`RuleEvaluator.scala`**: Evaluates new `BindingMaterialConstrainsSize` and `ComponentRequired` rules; signature updated to receive `ProductCategory` instead of `CategoryId`

### Pricing (`modules/domain/src/main/scala/mpbuilder/domain/pricing/`)
- **`PricingRule.scala`**: Added `MaterialLinearPrice(materialId, pricePerMeter)` and `MaterialFixedPrice(materialId, pricePerUnit)` cases
- **`PriceCalculator.scala`**: Interprets new pricing rules for `Binding` components; linear pricing uses `category.boundEdge` to select the correct edge dimension; `calculateComponentBreakdown` now receives `ProductCategory`

### Services (`modules/domain/src/main/scala/mpbuilder/domain/service/`)
- **`ConfigurationBuilder.scala`**: Added `FrontCover`, `BackCover`, `Binding` to `deriveSheetCount` (all return 1)
- **`CatalogQueryService.scala`**: Added `BindingMaterialConstrainsSize` and `ComponentRequired` to exhaustive match

### Codecs (`modules/domain/src/main/scala/mpbuilder/domain/codec/`)
- **`DomainCodecs.scala`**: Added `JsonEncoder`/`JsonDecoder` for `HexColor`, `JsonCodec[MaterialAttribute]`, `JsonEncoder`/`JsonDecoder` for `BoundEdge`

### Sample data (`modules/domain/src/main/scala/mpbuilder/domain/sample/`)
- **`SampleCatalog.scala`**: Added 8 new materials:
  - Plastic coils: `plasticCoilA4Black`, `plasticCoilA4White`, `plasticCoilA3Black`
  - Metal wire-o: `metalWireOA4Silver`, `metalWireOA4Black`
  - Case binding: `caseBindingBoardBlack`
  - Transparent covers: `plasticClear200mic`, `plasticClear300mic`
  - Calendar category redesigned: `FrontCover` (optional) + `BackCover` + `Body` + `Binding`; `boundEdge = Some(ShortEdge)` for A4 portrait
  - Updated wall/desk calendar presets to use new component structure with `LoopBinding`
- **`SamplePricelist.scala`**: All 3 pricelists (`pricelist` USD, `pricelistCzk`, `pricelistCzkSheet`) updated:
  - `SpiralBinding`/`WireOBinding` surcharges/setup fees → `LoopBinding`
  - Added `MaterialLinearPrice` for coils/wire-o, `MaterialFixedPrice` for case binding board
  - Added `MaterialBasePrice` for transparent cover materials
- **`SampleRules.scala`**: Updated binding method references to `LoopBinding`; updated technology constraint
- **`SampleShowcase.scala`**: Updated calendar showcase description and variation text

### UI (`modules/ui/src/main/scala/mpbuilder/ui/`)
- **`SpecificationForm.scala`**: Updated `bindingMethodLabel` for `LoopBinding` (removed `SpiralBinding`/`WireOBinding` cases)
- **`ConfigurationForm.scala`**: Added `FrontCover`, `BackCover`, `Binding` to `componentRoleLabel`
- **`PricePreview.scala`**: Added `FrontCover`, `BackCover`, `Binding` to `componentRoleLabel`
- **`EditorBridge.scala`**: Updated `SpiralBinding`/`WireOBinding` pattern → `LoopBinding`
- **`RulesEditorView.scala`**: Added all new rule types to `ruleSummary`, `ruleTypeName`, `extractReason` match expressions; includes `ScoringMax*`, `BindingMaterialConstrainsSize`, `ComponentRequired`
- **`BasketView.scala`**: Added `ScoringParams` case to `finishDescription` (previously non-exhaustive warning)

### Tests (`modules/domain/src/test/scala/mpbuilder/domain/`)
- **`ConfigurationBuilderSpec.scala`**: Updated all calendar tests to use new 4-component structure (`FrontCover`+`BackCover`+`Body`+`Binding`); updated binding method to `LoopBinding`; fixed `RuleEvaluator.evaluate` call to use `ProductCategory` instead of `CategoryId`
- **`PriceCalculatorSpec.scala`**: Updated calendar test to use new component roles and `LoopBinding`; added binding material line assertion; fixed expected bound edge calculation (ShortEdge 210mm)

## Decisions & Rationale

- **`SpiralBinding`/`WireOBinding` → `LoopBinding`**: Both are topologically equivalent "loop through holes" mechanisms. Distinguishing them at the binding-method level was unnecessary; the *material* (plastic coil vs metal wire) captures the real distinction. `LoopBinding` is the method; the binding `Material` carries the specific product.
- **`FrontCover`/`BackCover` roles**: Calendars have asymmetric covers (transparent front, printed back), so the generic `Cover` role was insufficient. Kept `Cover` for backward compatibility with booklets and other products.
- **`Binding` role**: Binding material (coil, wire, board) is now a first-class component with its own pricing rules, rather than being implied by the binding method surcharge alone.
- **`MaterialLinearPrice`**: Coils and wire-o are sold by linear meter; pricing by the bound-edge length is the natural model.
- **`BoundEdge` on category**: The bound edge varies by product (calendars bind on the short edge, booklets on the long/spine edge). Making it a category-level field keeps the pricing engine generic.
- **`HexColor` / `MaterialAttribute`**: Enables filtering binding materials by color/length constraints in future UI without requiring separate material types for every color variant.

## Issues Encountered

- **`MaterialAttribute` JSON codec**: `DeriveJsonCodec.gen[MaterialAttribute]` failed until `HexColor` got its own `JsonEncoder`/`JsonDecoder` (opaque type not automatically derived). Fixed by adding explicit codecs before the `Material` codec.
- **Non-exhaustive match warnings**: Several UI files (`RulesEditorView`, `PricePreview`, `ConfigurationForm`) and `CatalogQueryService` / `ConfigurationBuilder` had non-exhaustive matches that needed updating for the new enum cases.
- **`CategoryPresetSpec` pricing failure**: The `pricelistCzkSheet` (3rd pricelist) was missing `MaterialLinearPrice`/`MaterialFixedPrice`/`MaterialBasePrice` rules for the new materials. Fixed by adding the rules to all 3 pricelists.
- **`RuleEvaluator.evaluate` signature change**: Test was passing `CategoryId` (`bannersId`) instead of `ProductCategory` (`banners`) after the signature update. Fixed.
- **Bound edge direction**: Initial implementation hardcoded `heightMm` for linear pricing. Code review caught this; fixed to use `category.boundEdge` with a `BoundEdge` pattern match.

## Follow-up Items

- [ ] UI: Add binding material selector in `ConfigurationForm` for the `Binding` component (currently shows material dropdown same as other roles)
- [ ] UI: Show `FrontCover` as optional in the component selector with a clear "skip" affordance
- [ ] Rules: Wire up `BindingMaterialConstrainsSize` evaluation in `RuleEvaluator` (currently the rule exists but evaluation returns success unconditionally — needs `MaxBoundEdgeLengthMm` attribute check)
- [ ] Rules: Wire up `ComponentRequired` evaluation (currently returns success unconditionally)
- [ ] Add `BindingMaterialConstrainsSize` sample rules for coil materials with their actual max-page capacities
- [ ] Consider `ComponentRequired` rules for calendars requiring a `Binding` component when `LoopBinding` is selected
