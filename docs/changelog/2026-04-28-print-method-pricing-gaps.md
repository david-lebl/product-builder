# 2026-04-28 — Print Method Pricing Gaps & New Sticker Methods

**PR:** copilot/check-prices-for-print-methods
**Author:** copilot agent
**Type:** feature

## Summary

Analysed the sample catalog to identify print methods that lacked pricing configuration. Fixed three methods with missing `InkConfigurationSheetPrice` entries (`screenPrintId`, `dtgId`, `sublimationId`), added two brand-new printing methods for the `SolventInkjet` and `LatexInkjet` process types (which had no methods at all), and updated the stickers category to support all three area-priced methods with the CZK prices specified by the user (solvent 300 CZK/m², UV 360 CZK/m², Epson 8-color 420 CZK/m²).

## Changes Made

### `modules/domain/src/main/scala/mpbuilder/domain/sample/SampleCatalog.scala`
- Added IDs: `solventInkjetId` (`pm-solvent-inkjet`) and `epson8ColorId` (`pm-epson-8color`)
- Added `solventInkjetMethod` (process type `SolventInkjet`) with bilingual description
- Added `epson8ColorMethod` (process type `LatexInkjet`, 8-channel extended-gamut inkjet) with bilingual description
- Extended stickers category:
  - Added `vinylId` to `allowedMaterialIds` (self-adhesive vinyl for outdoor stickers)
  - Added `solventInkjetId` and `epson8ColorId` to `allowedPrintingMethodIds`
  - Added preset `preset-stickers-solvent` — Outdoor Vinyl (Solvent), 100×100 mm, 250 pcs
  - Added preset `preset-stickers-epson8color` — Premium 8-Color (Epson), 50×50 mm, 250 pcs
- Added `solventInkjetMethod` and `epson8ColorMethod` to the `printingMethods` catalog map

### `modules/domain/src/main/scala/mpbuilder/domain/sample/SamplePricelist.scala`

**USD pricelist:**
- Added `InkConfigurationSheetPrice` for `screenPrintId` (4+0 = 0.04, 4+4 = 0.06 USD/unit)
- Added `InkConfigurationSheetPrice` for `dtgId` (same scale as `digitalId`)
- Added `InkConfigurationSheetPrice` for `sublimationId` (same scale as `digitalId`)
- Added `InkConfigurationAreaPrice` for `solventInkjetId` (4+0 = 13.00 USD/m²)
- Added `InkConfigurationAreaPrice` for `epson8ColorId` (4+0 = 18.00 USD/m²)

**CZK pricelist:**
- Added `InkConfigurationSheetPrice` for `screenPrintId` (4+0 = 2 CZK/unit)
- Added `InkConfigurationSheetPrice` for `dtgId` (4+0 = 1.50 CZK/unit, same as `digitalId`)
- Added `InkConfigurationSheetPrice` for `sublimationId` (4+0 = 1.50 CZK/unit, same as `digitalId`)
- **Updated** `uvInkjetId` area prices: 4+0 22 → **360 CZK/m²**, 4+4 45 → **720 CZK/m²** (proportional)
- Added `InkConfigurationAreaPrice` for `solventInkjetId` (4+0 = **300 CZK/m²**, 4+4 = 600 CZK/m²)
- Added `InkConfigurationAreaPrice` for `epson8ColorId` (4+0 = **420 CZK/m²**, 4+4 = 840 CZK/m²)

### `modules/domain/src/main/scala/mpbuilder/domain/sample/SampleProductionCosts.scala`
- Added `ProcessCost(SolventInkjet, 0.04 USD)` and `ProcessCost(LatexInkjet, 0.05 USD)` in USD cost sheet
- Added `ProcessCost(SolventInkjet, 0.60 CZK)` and `ProcessCost(LatexInkjet, 0.80 CZK)` in CZK cost sheet

## Decisions & Rationale

### Gap analysis — what was missing
| Method | Was missing |
|--------|-------------|
| `screenPrintId` | `InkConfigurationSheetPrice` — only had a `PrintingProcessSurcharge` |
| `dtgId` | `InkConfigurationSheetPrice` — keyed by method ID, not inferred from `Digital` process type |
| `sublimationId` | Same as DTG — separate method ID needs its own entries |
| `SolventInkjet` | No printing method defined at all |
| `LatexInkjet` | No printing method defined at all |

### New methods mapped to process types
- **Solvent Inkjet** → `SolventInkjet` (obvious match)
- **Epson 8-color extended-gamut** → `LatexInkjet` (closest available type for non-UV, non-solvent wide-gamut inkjet; the method name/description clarifies the actual technology to avoid brand confusion)

### UV inkjet area price update
The user specified UV sticker printing at 360 CZK/m² (4+0). The previous value (22 CZK/m²) was set when the CZK pricelist was first introduced for sheet-format products and was not revised when large-format / sticker use cases were added. The updated prices now reflect realistic Czech market rates for vinyl sticker printing.

### 4+4 / other configs scaled proportionally
Only 4+0 prices were explicitly specified by the user. All other ink configurations (4+4, 4+1, 1+0, 1+1) were derived by applying the same ratio as in the original UV inkjet entries (4+4 = 2× the 4+0 price, etc.).

## Issues Encountered

No build or test failures. All 181 tests pass after changes.

---

## 2026-04-28 (follow-up) — Sticker Material/Print-Method Constraints & Bug Tests

### Summary

Added `ConfigurationConstraint` rules to `SampleRules` to prevent the silent ink-cost pricing bug for sticker configurations, and added 14 new tests (6 pricing + 8 validation) to document and guard against the issue.

### Root Cause: Silent Ink Cost Bug

`PriceCalculator` determines how to look up an ink configuration price based on the material pricing strategy:

- **Base-priced material** (`MaterialBasePrice`) → `InkPricingBasis.SheetOrUnit` → looks for `InkConfigurationSheetPrice`
- **Area-priced material** (`MaterialAreaPrice`/`MaterialAreaTier`) → `InkPricingBasis.Area` → looks for `InkConfigurationAreaPrice`

Large-format methods (UV inkjet, solvent, Epson 8-color) **only** have `InkConfigurationAreaPrice` rules. Digital/offset **only** have `InkConfigurationSheetPrice` rules. If these are mixed:

- **adhesiveStock (base) + UV inkjet** → looks for `InkConfigurationSheetPrice(uvInkjetId, ...)` → not found → `inkConfigLine = None` → ink cost = 0
- **vinyl (area) + digital** → looks for `InkConfigurationAreaPrice(digitalId, ...)` → not found → `inkConfigLine = None` → ink cost = 0

The bug is silent (no error returned) — the total is simply understated.

### Fix: ConfigurationConstraints for Stickers

Two constraints added to `SampleRules.scala`:

1. **Vinyl family → large-format inkjet required**: If the component material is `MaterialFamily.Vinyl`, the printing method must use `UVCurableInkjet`, `SolventInkjet`, or `LatexInkjet` process type.
2. **Large-format inkjet → vinyl required**: If the printing method uses one of those large-format process types, the component material must be `MaterialFamily.Vinyl`.

Together, these ensure only compatible combinations can be built:
- `vinyl + digital` → rejected (constraint 1)
- `adhesiveStock + UV inkjet` → rejected (constraint 2)
- `vinyl + UV inkjet` → allowed ✓
- `adhesiveStock + digital` → allowed ✓

### Additional Changes

**`modules/domain/src/main/scala/mpbuilder/domain/sample/SampleRules.scala`**
- Added `ConfigurationConstraint` (vinyl → large-format inkjet) for `stickersId`
- Added `ConfigurationConstraint` (large-format inkjet → vinyl) for `stickersId`

**`modules/domain/src/test/scala/mpbuilder/domain/PriceCalculatorSpec.scala`**
- Added suite `"sticker large-format print methods"` with 6 tests:
  - 3 correct area-pricing tests (clearVinyl+UV, vinyl+solvent, clearVinyl+Epson)
  - 1 correct base-pricing test (adhesiveStock+digital)
  - 2 BUG-documenting tests showing silent zero-ink (adhesiveStock+UV, vinyl+digital)

**`modules/domain/src/test/scala/mpbuilder/domain/ConfigurationBuilderSpec.scala`**
- Added suite `"sticker material / print-method compatibility"` with 8 tests (4 valid, 4 rejected)
- Updated existing misleading test "CMYK + white underlay on clear vinyl with digital printing is rejected (non-UV inkjet)": the assertion was `isRight` but is now correctly `isLeft` + `ConfigurationConstraintViolation` because clear vinyl (Vinyl family) requires large-format inkjet under the new rules.

### Issues Encountered

- **One existing test had a contradictory name/assertion**: The test was named "…rejected (non-UV inkjet)" but asserted `isRight`. It was testing that the white-ink `TechnologyConstraint` was satisfied for transparent clear vinyl + digital — which was true. However, the new `ConfigurationConstraint` now correctly rejects vinyl + digital regardless of ink type. The test was updated to reflect the new (correct) behavior with an explanatory comment.

## Follow-up Items

- [ ] Verify whether banners should keep the updated UV inkjet price (360 CZK/m²) or get a separate printing method for large-format banner UV printing at a lower rate
- [ ] Consider adding `solventInkjetId` to the banners category (solvent is a common outdoor large-format method)
- [ ] Add screen-print, DTG, and sublimation per-unit USD prices — current values are rough estimates
- [ ] Consider whether the `PriceCalculator` itself should return a `PricingError` when the ink-pricing basis and method type are incompatible, rather than silently returning `None`
