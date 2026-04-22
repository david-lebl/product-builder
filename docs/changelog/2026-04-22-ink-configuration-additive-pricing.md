# 2026-04-22 — Additive Ink Configuration Pricing

**PR:** #135 (copilot/update-ink-configuration-pricing)
**Author:** copilot agent
**Type:** feature

## Summary

Replaced the `InkConfigurationFactor` pricing rule (a material cost multiplier) with two new
additive rules: `InkConfigurationSheetPrice` and `InkConfigurationAreaPrice`. Both new rules are
keyed by `PrintingMethodId` in addition to front/back color counts, making ink costs visible as
explicit line items and decoupling them from material prices.

Simultaneously reduced sample material prices to reflect a clean material-only cost (no ink
baked in), keeping overall subtotals roughly equivalent.

## Changes Made

### Domain
- **`PricingRule.scala`** — removed `InkConfigurationFactor`; added `InkConfigurationSheetPrice(printingMethodId, frontColorCount, backColorCount, pricePerSheet)` and `InkConfigurationAreaPrice(printingMethodId, frontColorCount, backColorCount, pricePerSqM)`
- **`PriceCalculator.scala`** — refactored `computeInkConfigLine` to accept `printingMethodId` and `InkPricingBasis` (sealed enum `SheetOrUnit(count)` / `Area(sqM, qty)`); updated all 4 call sites in `calculateComponentBreakdown` to pass the printing method ID from the config

### Sample data
- **`SamplePricelist.scala`** — three pricelists updated:
  - USD pricelist: replaced 5 `InkConfigurationFactor` lines with per-method `InkConfigurationSheetPrice` (offset/digital/letterpress) and `InkConfigurationAreaPrice` (uv-inkjet); material base prices and area prices reduced (~30%)
  - CZK unit pricelist: same ink rule replacement; material prices reduced
  - CZK sheet pricelist: same ink rule replacement; material sheet prices reduced by ~4 CZK/sheet

### Tests
- **`PriceCalculatorSpec.scala`** — updated ~35 assertions: `materialLine.unitPrice`, `materialLine.lineTotal`, `inkConfigLine.lineTotal`, banner tier values, sheet test values; changed "4/4 produces no ink line" test from `isEmpty` to `isDefined`
- **`ProductionCostSpec.scala`** — removed `InkConfigurationFactor` from custom test pricelist
- **`CustomerPricelistResolverSpec.scala`** — updated ~15 material-price-dependent assertions

### UI
- **`PricelistEditorView.scala`** — replaced `InkConfigurationFactor` form with two new rule types; added `printingMethodIdVar` and printing-method text field; updated `buildPricingRule`, `pricingRuleTypeName`, `extractFrontColor/BackColor`, added `extractPrintingMethodId` and `extractPricingAmount` cases

### Documentation
- **`docs/pricing.md`** — updated rule table (22→23 rules), step 3 description, and all three worked examples
- **`docs/features.md`** — updated rule table row

## Decisions & Rationale

- **Additive not multiplicative**: ink is now a separate `LineItem` added to the component subtotal, not a factor applied to material cost. This makes ink costs transparent in the breakdown UI and allows independent discounting of material vs ink.
- **Keyed by `PrintingMethodId`**: offset litho, digital, letterpress, and UV inkjet have fundamentally different ink economics. The same 4/4 config costs very differently on an offset press vs a digital device.
- **`InkPricingBasis` private enum**: the calculator's `computeInkConfigLine` uses a private sealed enum (`SheetOrUnit` / `Area`) to dispatch to the right rule type without exposing implementation complexity.
- **Material prices reduced**: since material base/sheet prices previously baked in ink cost, they were reduced proportionally. Overall subtotals are unchanged for equivalent 4/4 configurations.
- **All subtotals preserved**: key end-to-end totals (business cards $75, banners $90.40, booklets $520) were deliberately kept identical, so no pricing change is visible to end users on existing configurations.

## Issues Encountered

- **Edit tool unicode mismatch**: `docs/pricing.md` contains non-ASCII characters (×, –, ─) that the `edit` tool matched against escaped ASCII strings. Used Python scripting to perform the replacements.
- **Test file ordering issue**: a `report_progress`-style edit accidentally removed a `},` closure when editing an assertion block. Identified by viewing context and corrected immediately.
- **`CustomerPricelistResolverSpec` failures**: this spec was not in scope initially but depended on the material prices that changed. Updated all 12 affected assertions after the first test run.

## Follow-up Items

- [ ] Consider adding `InkConfigurationSheetPrice` / `InkConfigurationAreaPrice` to the catalog editor UI as proper form fields (currently uses a plain text field for `printingMethodId`; a select populated from the catalog's printing methods would be better)
- [ ] Customer pricelist discounts currently do not apply to ink lines — evaluate whether ink should be discountable
