# 2026-04-22 — Ink Configuration Pricing Analysis

**PR:** (this branch)  
**Author:** copilot agent  
**Type:** analysis

## Summary

Created a comprehensive analysis document (`docs/analysis/ink-configuration-pricing.md`) examining the structural
problems of the current `InkConfigurationFactor` pricing model and proposing a replacement additive model with two
new `PricingRule` sealed ADT variants. No code was changed — this is a research and design document to inform a
follow-up implementation PR.

## Changes Made

- **Created** `docs/analysis/ink-configuration-pricing.md` — full analysis document (~700 lines) covering:
  - Context and motivation (current model location, real-world billing models)
  - Current model walkthrough with all three pricelist factor tables
  - Five structural loopholes with worked numerical examples
  - Proposed model: `InkConfigurationSheetPrice` + `InkConfigurationAreaPrice` ADT variants keyed by `PrintingMethodId`
  - New price tables for CZK sheet, CZK unit, and USD pricelists (all three)
  - Per-table deviation analysis showing delta from current totals for all configs × representative materials
  - Round-trip verification against two existing test scenarios (A4 flyer sheet-based + vinyl banner area-based)
  - Future-proofing section (manufacturing cost integration, extensibility, multi-press, booklets)
  - Migration notes for the follow-up implementation PR
  - Decision log (4 decisions with rationale and alternatives considered)
- **Modified** `docs/INDEX.md` — added `ink-configuration-pricing.md` entry to the Analysis section

## Decisions & Rationale

- **Additive ink model:** Ink is expressed as a positive cost line item (not a negative adjustment). Matches click-charge
  and per-m² billing; decouples from material price; enables future supplier cost integration without a parallel table.
- **Two rules:** `InkConfigurationSheetPrice` and `InkConfigurationAreaPrice` follow the existing `PricingRule` idiom
  (`MaterialSheetPrice` / `MaterialAreaPrice`) rather than using a tagged variant with a basis discriminator.
- **Keyed by `PrintingMethodId`:** Preserves per-machine granularity needed for real click-charge contracts. Two digital
  presses from different vendors carry different rates; `PrintingProcessType` is too coarse.
- **Accept large deviations for 1/0 and 1/1:** The current multiplicative model severely underprices mono printing
  (0.55× material = 45% off). The new flat per-sheet cost is physically accurate. The correction is intentional.

## Issues Encountered

- The problem statement's claim of "within ±5% for 4/4 and 4/0" is not achievable simultaneously for cheap (8 CZK/sheet)
  and expensive (20 CZK/sheet) materials with a single flat ink price. A flat per-sheet ink price cannot match a
  proportional model at both price extremes. The document reports honest numbers (0% to +8.8% for 4/0) and notes
  ±10% as the achievable bound for the dominant configurations.
- USD area materials show very large deviations (4/0: +58%) because the USD pricelist's 0.60 multiplier implies a
  $7.20/m² ink credit on vinyl — not reflective of real UV inkjet costs. Documented as accepted pricing correction.

## Follow-up Items

- [ ] Implementation PR: add `InkConfigurationSheetPrice` and `InkConfigurationAreaPrice` to `PricingRule.scala`
- [ ] Implementation PR: extend `PriceCalculator.computeInkConfigLine` to handle the two new variants
- [ ] Implementation PR: update `SamplePricelist.scala` with new ink price tables (all three pricelists)
- [ ] Implementation PR: update `PriceCalculatorSpec.scala` (~50+ assertions with new expected values)
- [ ] Implementation PR: remove `InkConfigurationFactor` after green tests
- [ ] Implementation PR: update `docs/pricing.md` rule count and rule table (22 → 23 rules)
- [ ] Consider: per-machine `ProductionCostRule.InkSheetCost` for margin analysis integration
