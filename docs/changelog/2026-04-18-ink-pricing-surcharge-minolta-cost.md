# 2026-04-18 — Ink Pricing Per-Unit Surcharge + Minolta Sheet Print Cost

**PR:** N/A
**Author:** claude-sonnet-4-6
**Type:** feature

## Summary

Replaced the percentage-based `InkConfigurationFactor` pricing rule with an additive per-unit `InkConfigurationSurcharge`. Each ink configuration now carries a fixed positive `Money` surcharge that is always added on top of the material/paper cost — fully independent of the paper price. Also added `ProductionCostRule.SheetPrintCost` to model per-sheet printer click charges (Konica Minolta C4080), using sheet nesting to calculate how many physical sheets are consumed.

## Changes Made

### New files
- `modules/domain/src/main/scala/mpbuilder/domain/pricing/SheetNesting.scala` — extracted from a private inner object in `PriceCalculator.scala` so both `PriceCalculator` and `ProductionCostCalculator` can reuse the sheet nesting algorithm

### Modified files
- `modules/domain/src/main/scala/mpbuilder/domain/pricing/PricingRule.scala` — replaced `InkConfigurationFactor(frontColorCount, backColorCount, materialMultiplier: BigDecimal)` with `InkConfigurationSurcharge(frontColorCount, backColorCount, surchargePerUnit: Money)`
- `modules/domain/src/main/scala/mpbuilder/domain/pricing/PriceCalculator.scala` — updated `computeInkConfigLine` to use flat surcharge math; removed private `SheetNesting` inner object; updated 3 call sites
- `modules/domain/src/main/scala/mpbuilder/domain/pricing/ProductionCost.scala` — added `ProductionCostRule.SheetPrintCost` variant for per-sheet printer click charges
- `modules/domain/src/main/scala/mpbuilder/domain/pricing/ProductionCostCalculator.scala` — added `calculateSheetPrintCost` helper using `SheetNesting`; threaded `processType` through `calculateComponentCost`
- `modules/domain/src/main/scala/mpbuilder/domain/sample/SamplePricelist.scala` — replaced `InkConfigurationFactor` rules with `InkConfigurationSurcharge` in all three pricelists (USD, CZK unit-based, CZK sheet-based)
- `modules/domain/src/main/scala/mpbuilder/domain/sample/SampleProductionCosts.scala` — added Minolta C4080 `SheetPrintCost` rules to both USD and CZK cost sheets
- `modules/domain/src/test/scala/mpbuilder/domain/PriceCalculatorSpec.scala` — updated all expected values to reflect the additive ink surcharge
- `modules/domain/src/test/scala/mpbuilder/domain/ProductionCostSpec.scala` — updated selling-price/margin expectations; fixed `InkConfigurationFactor` usage in custom test pricelist
- `modules/domain/src/test/scala/mpbuilder/domain/BasketServiceSpec.scala` — updated total price assertions
- `modules/domain/src/test/scala/mpbuilder/domain/ExpressManufacturingSpec.scala` — updated speed surcharge totals

## Decisions & Rationale

- **All-positive surcharges, no backward compat.** An earlier plan considered using 4/4 CMYK as a baseline (most expensive) and applying negative discounts for cheaper configurations. The user clarified this was backwards — adding cost is more natural — and backward compatibility was not required. Final approach: every ink configuration has an explicit non-negative per-unit surcharge on top of material cost.

- **Decoupled from material price.** The old `InkConfigurationFactor` multiplied the material cost, creating a loophole where cheap paper stocks had negligible ink cost adjustments. A Konica Minolta C4080 charges a fixed click rate per sheet regardless of paper; a percentage of paper cost misrepresents this entirely. The new surcharge is fully independent of material cost.

- **`SheetNesting` extraction.** The nesting algorithm (how many product items fit on one physical sheet, testing normal and rotated orientations) was private to `PriceCalculator`. Extracting it to a package-level object allowed `ProductionCostCalculator` to reuse it for `SheetPrintCost` without code duplication.

- **`SheetPrintCost` is self-contained.** The rule bundles the sheet dimensions (320×450mm SRA3), bleed (3mm), and gutter (2mm) alongside the cost, so nesting is calculated internally. No external sheet configuration is needed at the call site.

- **Sample values (USD):** `4/4 → $0.05`, `4/0 → $0.03`, `4/1 → $0.04`, `1/0 → $0.01`, `1/1 → $0.02` per unit. CZK equivalents multiplied by ~25 (`4/4 → 1.50 CZK`, etc.). Minolta click charges (USD): `4/4 → $0.16/sheet`, `4/0 → $0.08`, `4/1 → $0.12`, `1/0 → $0.03`, `1/1 → $0.06`.

## Issues Encountered

- **Third hidden pricelist.** `SamplePricelist.scala` contains three pricelists (`pricelist`, `pricelistCzk`, `pricelistCzkSheet`). The initial update only covered the first two; a compile error revealed the third at lines 567–571. Fixed by applying the same replacement.

- **Cascade of test failures.** Adding a positive 4/4 ink surcharge ($0.05/unit) broke ~30 tests that previously used `InkConfigurationFactor(4,4, 1.00)` (identity — no price change). Recalculated all expected values systematically. Largest change was the booklet test (body `sheetCount=7`, 500 quantity → `effectiveQty=3500`, ink `+$175`).

- **Low-margin threshold assertion.** After raising the test threshold from 80% to 130% (the new $90 selling price gives ~123% margin vs. $40.25 cost, above the old 80%), the inline assertion `threshold.value == BigDecimal("80")` was not updated in the same pass. Caught in the second compile/test cycle.

## Follow-up Items

- [ ] Consider exposing `SheetPrintCost` in the UI pricing breakdown (currently it only affects production cost, not the selling-price breakdown shown to users)
- [ ] `noInk` (0/0) configuration has no surcharge rule entry — verify this is the desired behavior for pure-material products
