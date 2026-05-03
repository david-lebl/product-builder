# 2026-05-03 — Promotional Ink Config Pricing Fix (DTG, Sublimation, UV Flatbed Mug)

**PR:** copilot/check-prices-for-print-methods
**Author:** copilot agent
**Type:** bugfix

## Summary

Fixed missing `InkConfigurationSheetPrice` rules in `pricelistCzkSheet` (the UI pricelist) for three promotional printing methods — Screen Print, DTG, and Dye Sublimation — and for UV inkjet when used on per-unit (base-priced) materials such as mugs. These omissions caused zero ink cost in price breakdowns for those products.

## Changes Made

- `modules/domain/src/main/scala/mpbuilder/domain/sample/SamplePricelist.scala`
  - **`pricelist` (USD)**: added `InkConfigurationSheetPrice` for `uvInkjetId` (per-unit UV flatbed: 4/4→1.30, 4/0→0.75, 4/1→1.00, 1/0→0.22, 1/1→0.38 USD/unit)
  - **`pricelistCzk`**: added `InkConfigurationSheetPrice` for `uvInkjetId` (per-unit UV flatbed: 4/4→35, 4/0→20, 4/1→27, 1/0→6, 1/1→10 CZK/unit); clarified UV inkjet area comment
  - **`pricelistCzkSheet`** (UI pricelist): added per-unit ink config prices for:
    - `screenPrintId` (4/4→4, 4/0→2, 4/1→3, 1/0→0.60, 1/1→1 CZK)
    - `dtgId` (4/4→6, 4/0→3, 4/1→4, 1/0→1, 1/1→1.80 CZK — slightly higher than sublimation)
    - `sublimationId` (4/4→5, 4/0→2.50, 4/1→3.50, 1/0→0.80, 1/1→1.50 CZK)
    - `uvInkjetId` per-unit (same values as pricelistCzk: 4/4→35, 4/0→20, etc. CZK/unit)

- `modules/domain/src/test/scala/mpbuilder/domain/PriceCalculatorSpec.scala`
  - Updated "BUG: adhesiveStock sticker + UV inkjet → ink config line absent" test: the bug is fixed — ink config line is now present. Updated test name, comment, and assertions to reflect the correct post-fix behavior.
  - Added new suite "pricelistCzkSheet promotional ink configuration pricing" with 3 tests:
    - `cotton t-shirt + DTG 4/0 → ink config line present in breakdown (CzkSheet)`
    - `polyester t-shirt + sublimation 4/0 → ink config line present in breakdown (CzkSheet)`
    - `ceramic mug + UV inkjet 4/0 → ink config line present in breakdown (CzkSheet)`

## Decisions & Rationale

- **Why per-unit (not area) for DTG/sublimation?** T-shirts, bags, and mugs all have `MaterialBasePrice` rules. The `PriceCalculator` uses `InkPricingBasis.SheetOrUnit(quantity)` for base-priced materials, so `InkConfigurationSheetPrice` rules are required. Using area-based pricing for garments would require a per-item area calculation that the current model doesn't support.

- **Why dual rules for UV inkjet?** UV inkjet has both `InkConfigurationAreaPrice` (for vinyl stickers, MaterialAreaPrice materials) and the newly added `InkConfigurationSheetPrice` (for mugs, MaterialBasePrice materials). The calculator dispatches to the appropriate rule type based on the material's pricing basis, so both rules coexist without conflict.

- **DTG vs sublimation pricing**: DTG ink cost is slightly higher (3 CZK vs 2.50 CZK for 4/0) because DTG printers use more ink per print than sublimation transfer. This reflects typical industry pricing ratios.

- **Screen print ink config**: The `PrintingProcessSurcharge(ScreenPrint, 4 CZK)` was already present in `pricelistCzkSheet`, but it applies uniformly regardless of ink configuration. Adding `InkConfigurationSheetPrice` for screen print introduces proper per-color-count pricing (e.g., 4-color CMYK vs 1-color grayscale).

- **Screen print per item or per area?** For garments (t-shirts, bags), screen printing is always priced per item — the screen setup cost is amortized over the run. Area-based pricing is only used for large-format flat surfaces (banners, stickers). The current `MaterialBasePrice` path in the calculator correctly handles this.

## Issues Encountered

- The old "BUG" test for `adhesiveStock + UV inkjet` was asserting `inkConfigLine.isEmpty` (documenting the known zero-ink-cost bug). Adding the UV inkjet per-unit prices caused the test to fail because the calculator now correctly prices the combination. Updated the test to assert the ink config line IS present and verify the exact amounts.

## Follow-up Items

- [ ] DTG and sublimation currently use `processType = PrintingProcessType.Digital`. Consider adding `DirectToGarment` and `DyeSublimation` process type variants to enable type-specific constraints (e.g., DTG only on cotton/blended fabrics, sublimation only on polyester/coated) and more precise process surcharges.
- [ ] Screen print on t-shirts: `4/4` config means "print on both sides of the garment", which is valid for t-shirts but unusual for mugs. A category-specific InkConfig filter (similar to the large-format inkjet single-sided filter) could be added for mugs.
- [ ] Sublimation on mugs: sublimation wrap covers the entire mug surface (effectively single-sided from the model's perspective). The `4/4` option in the InkConfigSelector for sublimation + mug is confusing UI-wise.
