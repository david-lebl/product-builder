# 2026-07-05 — Intra-Module File Regrouping

**PR:** N/A (branch `refactor/module-file-grouping`, stacked on `refactor/context-modules`)
**Author:** Claude (agent session)
**Type:** refactoring

## Summary

Follow-up to the bounded-context module split: regroup files *within* each module so every file holds one aggregate or one concern. No type, package or behavior changes — code moved verbatim; all 22 suites pass unchanged.

## Changes Made

- **manufacturing**: `manufacturing.scala` (437 lines, ~25 types) split into `workflow.scala`, `manufacturingOrder.scala`, `station.scala` (absorbing `StationTimeEstimate`/`StationUtilisation`), `artworkCheck.scala`, `fulfilment.scala`, `employee.scala`, `machine.scala`; `WorkingHours`+`ShopSchedule` → `schedule.scala`; `CategoryTierConfig`+`TierRestriction` → `tierRestriction.scala`.
- **catalog**: ink types extracted to `inkConfiguration.scala`; `SpecKind` moved from `category.scala` to `specification.scala` (it keys `ProductSpecifications`); `FinishSelection` (with its Finishing-rewrite TODO) moved from `component.scala` to `finish.scala`; `predicates`+`CompatibilityRule`+`CompatibilityRuleset` → `rules.scala`; `WeightError`+`WeightBreakdown`+`WeightCalculator` → `weight.scala`.
- **ordering**: checkout flow types (`CheckoutStep`, `CheckoutInfo`, `DeliveryOption`, `PaymentMethod`, `ShopLocation`, `CourierService`) → `checkout.scala`; `order.scala` keeps the `Order` aggregate.
- **pricing**: `PriceCalculator` (713 lines) reduced to the public orchestration + speed surcharge; extracted `private[pricing]` objects `ComponentPricing` (material/ink/cutting lines, sheet nesting), `FinishPricing` (+ `FinishPricingBasis`), `SetupFees`, `Surcharges` (surcharge finders, tier lookups, fold/binding names). `Pricelist` merged into `PricingRule.scala`; `QueueThreshold`+`BusyPeriodMultiplier` → `busyPeriod.scala`.
- **samples**: `SampleCatalog` (2,191 lines, 207 vals) split into `SampleIds`, `SampleMaterials`, `SamplePrintingMethods`, `SampleFinishes`, `SampleCategories`; `SampleCatalog` remains as a facade `export`-ing all members and assembling the `ProductCatalog` — the ~487 existing `SampleCatalog.*` references are untouched. `SamplePricelist` split per pricelist (`SamplePricelistUsd`/`Czk`/`CzkSheet`) behind the same facade pattern.

## Decisions & Rationale

- **Export facades for samples**: rewriting 487 call sites for a data-file split would be churn without benefit; Scala 3 `export` gives stable aliases so consumers keep `SampleCatalog.coated300gsmId`.
- **`private[pricing]` helper objects instead of methods on `PriceCalculator`**: keeps the public API a single object while letting each concern live in its own file; tests in `mpbuilder.pricing` can still reach them.
- **Reusable material ID sets became `private[samples]`** (were `private`) since they now span `SampleMaterials` → `SampleCategories`.
- **File naming convention**: lowercase files = aggregate/vocabulary (`workflow.scala`), PascalCase = one service/calculator object (`WorkflowEngine.scala`).

## Issues Encountered

- macOS case-insensitive filesystem: writing `tierRestriction.scala` while `TierRestriction.scala` still existed targeted the same file — delete the original before creating the differently-cased replacement.

## Follow-up Items

- [ ] Consider splitting `SampleRules` (556 lines) and `SampleShowcase` (700 lines) the same way if they keep growing.
- [ ] `RuleEvaluator.evaluate` (190-line match) is the next candidate if it grows further.
