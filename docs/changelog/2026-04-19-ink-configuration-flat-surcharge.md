# 2026-04-19 — Ink Configuration Pricing: Flat Per-Unit Surcharge

**PR:** copilot/change-ink-pricing-rules
**Author:** copilot agent
**Type:** feature | analysis

## Summary

Researched and documented the trade-offs between the existing percentage-based `InkConfigurationFactor` pricing model and a flat per-unit `InkConfigurationSurcharge` model for sheet printing. Concluded the flat surcharge model is superior: it correctly separates substrate cost from printing cost, eliminates an unfair discount loophole for expensive materials, and enables future manufacturing cost tracking (e.g., recording Konica Minolta per-sheet charges). Implemented the change for `pricelistCzkSheet`, preserving exact 4/4 CMYK prices while making all material sheet prices and ink configuration rules whole numbers.

## Changes Made

**New files:**
- `docs/analysis/ink-configuration-pricing.md` — Full analysis of both models, recommendation, calibration tables, and future manufacturing cost tracking notes.

**Modified files:**
- `modules/domain/src/main/scala/mpbuilder/domain/pricing/PricingRule.scala` — Added new `InkConfigurationSurcharge(frontColorCount, backColorCount, surchargePerUnit: Money)` variant alongside the existing `InkConfigurationFactor`.
- `modules/domain/src/main/scala/mpbuilder/domain/pricing/PriceCalculator.scala` — Updated `computeInkConfigLine` to check for `InkConfigurationSurcharge` first; falls back to `InkConfigurationFactor` for backward compatibility.
- `modules/domain/src/main/scala/mpbuilder/domain/sample/SamplePricelist.scala` — `pricelistCzkSheet`: replaced 5× `InkConfigurationFactor` rules with 5× `InkConfigurationSurcharge` rules (4/4=5, 4/1=4, 4/0=3, 1/1=2, 1/0=0 CZK/sheet); reduced all material sheet prices by 5 CZK to keep 4/4 effective totals unchanged.
- `modules/domain/src/test/scala/mpbuilder/domain/PriceCalculatorSpec.scala` — Updated 6 sheet-based pricing tests to reflect new material prices and ink surcharge behaviour.
- `modules/ui/src/main/scala/mpbuilder/ui/catalog/views/PricelistEditorView.scala` — Added UI editor support for `InkConfigurationSurcharge`: new rule type in the dropdown, form fields (front/back color + amount), description text, build/extract helpers.
- `docs/pricing.md` — Updated rule count (17→18), added `InkConfigurationSurcharge` to the rule table, updated step 3 of the calculation pipeline.
- `docs/INDEX.md` — Added entry for the new analysis document.

## Decisions & Rationale

**Why flat surcharge instead of percentage multiplier?**
The percentage model multiplies the material cost, so more expensive substrates produce larger absolute discounts for simpler ink configurations. This is incorrect: printing 1-color vs 4-color costs the same per sheet regardless of whether the substrate is 90gsm paper or 300gsm cotton board. The flat surcharge reflects actual press costs (what a digital print supplier charges per sheet per configuration).

**Why keep `InkConfigurationFactor`?**
Backward compatibility with existing pricelists (`pricelistUsd`, `pricelistCzk`) that use `InkConfigurationFactor`. These base-price pricelists cover heterogeneous materials (mugs, t-shirts, promotional items) where the percentage model may still be appropriate and a single flat surcharge cannot cover the diversity. The calculator checks `InkConfigurationSurcharge` first and falls back to `InkConfigurationFactor`.

**Why 4/4 surcharge = 5 CZK?**
Calibrated so that all 4/4 CMYK effective prices (substrate + surcharge) equal the original combined prices exactly. The 5 CZK value reflects Czech market digital printing rates for an SRA3 sheet (Konica Minolta / HP Indigo tier). This gives a clean separation: substrate = what you pay the paper merchant, ink = what you pay the press supplier.

**Why 1/0 surcharge = 0?**
1/0 mono is the cheapest common printing configuration. Using it as the baseline (0 additional cost) means the material price represents "substrate + cheapest printing" and all more complex configurations add a positive amount. This matches the "positive addition" model requested.

**Why only update `pricelistCzkSheet` and not `pricelistCzk`?**
The task specifically says "in sheet printing". `pricelistCzk` uses `MaterialBasePrice` (per unit, not per sheet) and serves a wider set of product categories. The analysis document notes that `pricelistCzk` could benefit from a similar split in a future session.

## Issues Encountered

- **6 failing tests after material price update:** The sheet-based pricing tests hard-coded old material prices (8 CZK for 90gsm, 18 CZK for 300gsm). Updated test expected values to match new substrate prices plus the explicit ink surcharge amounts. One test also required updating the subtotal to include the new `inkConfigLine` amount that was previously zero (4/4 factor = 1.0 produced no line item, but 4/4 surcharge = 5 CZK now always creates a line item).

## Follow-up Items

- [ ] Update `pricelistCzk` (base CZK pricelist) to also use `InkConfigurationSurcharge` for paper materials — currently still uses `InkConfigurationFactor`.
- [ ] Update `pricelistUsd` if USD sheet-based pricing is introduced in future.
- [ ] Use `surchargePerUnit` from `InkConfigurationSurcharge` in manufacturing cost analysis to track press supplier charges per configuration.
- [ ] Consider adding a production cost test that demonstrates substrate vs. printing cost separation using the new model.
