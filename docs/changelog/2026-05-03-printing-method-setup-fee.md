# 2026-05-03 — Printing Method Setup Fee

**PR:** copilot/add-setup-fee-for-print-methods
**Author:** copilot agent
**Type:** feature

## Summary

Added a one-time `PrintingMethodSetupFee` pricing rule that charges a fixed machine/plate setup cost per printing method. The fee is added after the quantity discount multiplier (not discounted) and before the minimum order price check — consistent with all other setup fees in the system. This makes small-quantity orders more expensive (setup cost amortised over fewer items) and partially reduces the need for a minimum order price floor.

## Changes Made

### Domain
- `modules/domain/src/main/scala/mpbuilder/domain/pricing/PricingRule.scala` — added `PrintingMethodSetupFee(printingMethodId: PrintingMethodId, setupCost: Money)` case
- `modules/domain/src/main/scala/mpbuilder/domain/pricing/PriceCalculator.scala` — extended `collectSetupFees` signature and body to accept `printingMethod: PrintingMethod` and collect the matching `PrintingMethodSetupFee` rule into a `LineItem`
- `modules/domain/src/main/scala/mpbuilder/domain/sample/SamplePricelist.scala` — added `PrintingMethodSetupFee` rules to all three sample pricelists:
  - **USD pricelist**: offset $15, digital $10, UV inkjet $10, letterpress $25, solvent inkjet $20, Epson 8-color $20, screen print $20, DTG $10, sublimation $10
  - **CZK pricelist**: offset 350, digital 200, UV inkjet 200, letterpress 600, solvent inkjet 400, Epson 8-color 400, screen print 400, DTG 200, sublimation 200
  - **CZK sheet pricelist**: same CZK values as above

### UI
- `modules/ui/src/main/scala/mpbuilder/ui/catalog/views/PricelistEditorView.scala` — updated `PricelistEditorView` to support the new rule type:
  - Added `"PrintingMethodSetupFee"` to the rule type dropdown
  - Added `pricingRuleTypeName` case
  - Added rule description in summary string
  - Added `extractPricingAmount` case (maps to `setupCost`)
  - Added `extractPrintingMethodId` case (maps to `printingMethodId`)
  - Added conditional Printing Method ID field in the form (shared with `InkConfigurationSheetPrice`/`InkConfigurationAreaPrice`)
  - Added `buildPricingRule` case to construct the rule from form state

### Tests
- `modules/domain/src/test/scala/mpbuilder/domain/PriceCalculatorSpec.scala` — updated expected totals throughout to include the printing method setup fee; replaced the `setupFees empty` backward-compat test to use a custom pricelist without setup fees so the test intent (no crash when no setup fee rules exist) is preserved
- `modules/domain/src/test/scala/mpbuilder/domain/BasketServiceSpec.scala` — updated basket total assertions (USD pricelist offset setup fee now included)
- `modules/domain/src/test/scala/mpbuilder/domain/ExpressManufacturingSpec.scala` — updated all speed surcharge total assertions (+$15 offset setup fee per order)
- `modules/domain/src/test/scala/mpbuilder/domain/ProductionCostSpec.scala` — updated selling price and margin assertions; raised low-margin warning threshold from 80% to 120% to keep the test meaningful with new higher margins

## Decisions & Rationale

- **Setup fee is not discounted** — consistent with all other setup fees in the system (`FoldTypeSetupFee`, `BindingMethodSetupFee`, `ScoringSetupFee`). Machine setup cost doesn't scale with volume.
- **Fee is per printing method, not per category** — the same method (e.g. offset) incurs the same setup cost regardless of which product is ordered. This matches real print shop economics.
- **One rule per method in each pricelist** — if a pricelist has no `PrintingMethodSetupFee` for a given method, no setup fee is charged (graceful absence, consistent with other optional rules).
- **No change to minimum order price rules** — setup fees are kept separate from minimum price. The setup fee naturally raises the effective floor for small orders, reducing (but not removing) the need for explicit minimums.
- **CzkSheet pricelistCzkSheet test update** — the 10-pcs UV inkjet sticker test previously relied on the 200 CZK minimum order floor being triggered. After adding the 200 CZK UV inkjet setup fee, the billable total becomes 283.50 CZK > 200 CZK minimum, so `minimumApplied` is now `None` and the total is 283.50 instead of 200.00.

## Issues Encountered

- **Many test totals needed updating** — adding setup fees to all three sample pricelists caused cascading test failures across `PriceCalculatorSpec`, `BasketServiceSpec`, `ExpressManufacturingSpec`, and `ProductionCostSpec`. All tests updated to reflect new expected totals.
- **`setupFees empty` backward-compat test** — the test was using the USD `pricelist` (which now has `PrintingMethodSetupFee` rules) with an assertion that `setupFees.isEmpty`. Fixed by replacing the pricelist reference with a custom minimal pricelist that has no setup fee rules, preserving the test's intent.
- **`low margin warning` threshold** — the test used an 80% threshold that was chosen to be just above the old ~68% margin. After adding the offset setup fee, the margin increased to ~105%, putting it above the 80% threshold and defeating the test. Changed the threshold to 120% to remain above the new margin and keep the test meaningful.

## Follow-up Items

- [ ] Consider adding `PrintingMethodSetupFee` to `adding-new-product-guide.md` checklist
- [ ] UI pricelist display: consider grouping setup fees visually in the rule list
