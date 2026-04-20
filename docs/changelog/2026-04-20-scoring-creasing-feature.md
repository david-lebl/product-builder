# 2026-04-20 — Creasing/Scoring with Configurable Crease Count

**PR:** copilot/add-scoring-feature
**Author:** copilot agent
**Type:** feature

## Summary

Implemented first-class parameterized Scoring/Creasing finish — a configurable crease count (1–N lines per sheet) with tiered per-piece pricing and per-template max-crease caps. The existing `FinishType.Scoring` finish (already in brochure/flyer templates) was parameterized by extending `FinishParameters`, following the same pattern as `RoundCornersParams`. Two new pricing rules cover the cost dimensions, three new `CompatibilityRule` variants enforce max-crease caps, and the UI exposes a radio-group selector clamped to the effective cap.

## Changes Made

### Domain Model
- `modules/domain/src/main/scala/mpbuilder/domain/model/finish.scala`  
  Added `FinishParameters.ScoringParams(creaseCount: Int)`

### Pricing
- `modules/domain/src/main/scala/mpbuilder/domain/pricing/PricingRule.scala`  
  Added `ScoringCountSurcharge(creaseCount: Int, surchargePerUnit: Money)` and `ScoringSetupFee(setupCost: Money)`
- `modules/domain/src/main/scala/mpbuilder/domain/pricing/PricingError.scala`  
  Added `MissingScoringPrice(creaseCount: Int)` with EN/CS localized messages (with correct Czech pluralization: linka / linky / linek)
- `modules/domain/src/main/scala/mpbuilder/domain/pricing/PriceCalculator.scala`  
  - Refactored `computeFinishLines` from `List[LineItem]` → `Validation[PricingError, List[LineItem]]` (enables failing on missing scoring rules)
  - Extracted `computeSingleFinishLine` helper; scoring + `ScoringParams` path uses `ScoringCountSurcharge` (fails with `MissingScoringPrice` when absent); all other finishes remain on existing path
  - `collectSetupFees`: added `ScoringSetupFee` handling — fires once when any Scoring finish is present and takes precedence over `FinishTypeSetupFee(Scoring)`
  - Updated all 4 call sites of `computeFinishLines` to use `.map { finishLines => ComponentBreakdown(...) }`

### Compatibility / Validation
- `modules/domain/src/main/scala/mpbuilder/domain/rules/CompatibilityRule.scala`  
  Added `ScoringMaxCreasesForCategory`, `ScoringMaxCreasesForMaterial`, `ScoringMaxCreasesForPrintingProcess`
- `modules/domain/src/main/scala/mpbuilder/domain/validation/ConfigurationError.scala`  
  Added `ScoringCreaseLimitExceeded(maxCreases, actualCreases, reason)` with EN/CS messages
- `modules/domain/src/main/scala/mpbuilder/domain/validation/ConfigurationValidator.scala`  
  Added `ScoringParams` case in `validateFinishParams`: validates `creaseCount >= 1` and that the finish type is `Scoring`
- `modules/domain/src/main/scala/mpbuilder/domain/validation/RuleEvaluator.scala`  
  Added evaluation of the 3 new scoring cap rules — each inspects `ScoringParams.creaseCount` on affected components

### Service
- `modules/domain/src/main/scala/mpbuilder/domain/service/CatalogQueryService.scala`  
  Added `scoringMaxCreases(categoryId, materialId, ruleset, printingMethodId, catalog): Option[Int]`  
  Returns the min of all applicable `ScoringMaxCreasesFor*` caps, or `None` if no rule applies.  
  Updated `compatibleFinishes` exhaustive match to include the 3 new rule variants (→ `true`).

### Sample Data
- `modules/domain/src/main/scala/mpbuilder/domain/sample/SamplePricelist.scala`  
  Added to CZK pricelist: `ScoringCountSurcharge` for 1–4 creases (0.60 / 1.00 / 1.30 / 1.50 CZK) + `ScoringSetupFee(60 CZK)`  
  Added to USD pricelist: `ScoringCountSurcharge` for 1–4 creases (0.03 / 0.05 / 0.06 / 0.07 USD) + `ScoringSetupFee(2.50 USD)`
- `modules/domain/src/main/scala/mpbuilder/domain/sample/SampleCatalog.scala`  
  Added `scoringId` to flyers `allowedFinishIds`
- `modules/domain/src/main/scala/mpbuilder/domain/sample/SampleRules.scala`  
  Added `ScoringMaxCreasesForCategory(brochuresId, 4, ...)` and `ScoringMaxCreasesForCategory(flyersId, 4, ...)`

### UI
- `modules/ui/src/main/scala/mpbuilder/ui/productbuilder/components/FinishSelector.scala`  
  Added `FinishType.Scoring` case to `defaultParams` (→ `ScoringParams(1)`)  
  Added `FinishType.Scoring` params form: radio group 1..maxCreases, reactive to `scoringMaxCreases` signal
- `modules/ui/src/main/scala/mpbuilder/ui/productbuilder/ProductBuilderViewModel.scala`  
  Added `scoringMaxCreases(role: ComponentRole): Signal[Option[Int]]` — derived from selected category + material + printing method via `CatalogQueryService.scoringMaxCreases`

### Tests
- `modules/domain/src/test/scala/mpbuilder/domain/PriceCalculatorSpec.scala`  
  Added 9 tests in new `scoring / creasing pricing` suite: tiers for 1–4 creases, discount behavior, setup-fee non-discount, `ScoringSetupFee` precedence over `FinishTypeSetupFee`, legacy backward compat, missing-rule error

## Decisions & Rationale

- **Parameterize existing Scoring finish** rather than adding a new top-level concept — reuses `SelectedFinish` / `FinishParameters` / `FinishSelector` / `compatibleFinishes` / `PriceBreakdown`. Follows the exact pattern of `RoundCornersParams`.

- **`ScoringCountSurcharge` (per-piece, discountable) + `ScoringSetupFee` (flat, not discounted)** — two separate rules match the two cost dimensions (machine/labor per piece + one-time knife setup). The sub-linear price table (0.6 / 1.0 / 1.3 / 1.5 CZK) cannot be expressed as a single factor, so each tier is an exact-match rule keyed on `creaseCount`, mirroring `FoldTypeSurcharge`.

- **`ScoringSetupFee` takes precedence over `FinishTypeSetupFee(Scoring)`** — once the new dedicated rule exists, the generic fallback should not double-charge. The CZK pricelist previously had `FinishTypeSetupFee(Scoring, 50)` which is superseded by `ScoringSetupFee(60)`.

- **`computeFinishLines` refactored to return `Validation`** — the scoring path must fail when no `ScoringCountSurcharge` rule is found (silent zero-pricing would be a billing error). The refactor is minimal: a new `computeSingleFinishLine` helper, unchanged logic for all non-scoring finishes.

- **`ScoringMaxCreasesForCategory/Material/ProcesType` as `CompatibilityRule` variants** — max-crease caps can then vary per category/material/process from day one. The effective cap is the `min` across all applicable rules, as required by the spec.

- **Czech pluralization** — `linka` (1) / `linky` (2–4) / `linek` (5+) is the correct Czech pattern. Applied in finish line labels, UI radio labels, and error messages.

## Issues Encountered

- `computeFinishLines` had 4 call sites that used its `List[LineItem]` return value directly inside `Validation.succeed(...)`. After changing the return type to `Validation`, each call site needed to become a `.map { finishLines => ComponentBreakdown(...) }`. The edit was straightforward but required 4 separate targeted edits because the surrounding code context differed at each site.

- `edit` tool returned "Multiple matches found" when the old_str block appeared identically in two places (the `areaTierRule` and `areaRule` paths). Solved by including more context (both paths) in a single edit.

## Follow-up Items

- [ ] Add `ScoringMaxCreasesForCategory` rules for packaging categories once those are added to `SampleCatalog`
- [ ] Verify UI rendering on a running instance: select Scoring on a brochure component, check crease count radio appears and price updates correctly
- [ ] Consider adding `ConfigurationBuilderSpec` tests for the `ScoringCreaseLimitExceeded` error path (crease count > cap)
- [ ] If the price table grows beyond 4 tiers (e.g., for packaging with 10+ creases), consider a `ScoringCountPriceTable(tiers: List[(Int, Money)])` rule variant
