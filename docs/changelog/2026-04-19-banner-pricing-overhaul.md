# 2026-04-19 — Banner Pricing Overhaul

**PR:** copilot/fix-grommet-pricing-and-options
**Author:** agent
**Type:** feature

## Summary

Overhauled the banner product pricing to match business requirements: PVC 510g material, area-tiered pricing, grommet surcharges by spacing, gum rope finish, and in-house production size limits.

## Changes Made

### Model Changes
- **`finish.scala`**: Added `GumRope` to `FinishType`, `GumRopeParams(lengthMm)` to `FinishParameters`
- **`PricingRule.scala`**: Added 3 new rule variants:
  - `MaterialAreaTier(materialId, minAreaM2, maxAreaM2, pricePerSqMeter)` — volume-based area pricing
  - `GrommetSpacingAreaSurcharge(spacingMm, pricePerSqMeter)` — spacing-dependent grommet pricing
  - `FinishLengthSurcharge(finishId, pricePerMeter)` — per-meter finish pricing
- **`DomainCodecs.scala`**: Added `GumRopeParams` codec
- **`ConfigurationValidator.scala`**: Added `GumRopeParams` validation branch

### Calculator Changes
- **`PriceCalculator.scala`**:
  - Area tier lookup: checks `MaterialAreaTier` rules before falling back to flat `MaterialAreaPrice`
  - Skips ink configuration adjustment for area-tiered materials (price already includes printing)
  - `computeFinishLines` now accepts optional `unitAreaM2` parameter
  - Handles `GrommetSpacingAreaSurcharge` (uses spacing from `GrommetParams`)
  - Handles `FinishLengthSurcharge` (uses length from `GumRopeParams`)
  - New `findBestAreaTier` helper (matches existing `findBestQuantityTier` pattern)

### Sample Data Changes
- **`SampleCatalog.scala`**:
  - Added `pvc510g` material (PVC 510g banner fabric)
  - Added `gumRope` finish definition
  - Banners category now uses `pvc510gId` instead of `vinylId`
  - Updated banner presets to max 1500mm dimensions
- **`SamplePricelist.scala`** (all 3 pricelists):
  - Added area tiers for PVC 510g: 600/500/450/400 CZK/m² (25/21/19/17 USD/m²)
  - Added grommet spacing surcharges: 50cm → 40 CZK/m², 30cm → 60 CZK/m²
  - Added gum rope length surcharge: 18 CZK/m (0.75 USD/m)
- **`SampleRules.scala`**: Added banner MaxDimension(1500, 1500), gum rope requires grommets
- **`SampleShowcase.scala`**: Updated banner features text

### Test Changes
- Updated 6 existing tests (vinyl → pvc510g references)
- Added 7 new `PriceCalculatorSpec` tests for CZK banner pricing

## Decisions & Rationale

- **Area tiers based on total order area** (not per-unit): Provides natural volume discount. 2 banners of 1m² = 2m² total → 500 CZK/m² tier. Consistent with printing industry practice.
- **Ink config factor skipped for area-tiered materials**: For large format printing, the area price already includes printing costs (material + print are one operation). Applying ink config adjustments would incorrectly reduce the price.
- **Grommet pricing as area surcharge**: More intuitive than per-grommet pricing. Larger banners naturally cost more for grommets because they need more. The spacing determines density, reflected in the per-m² rate.
- **Gum rope as per-meter pricing**: Customer specifies the length they need. Priced linearly at 18 CZK/m. Requires grommets as a compatibility rule since the rope threads through eyelets.
- **PVC 510g as distinct material**: Different from adhesive vinyl (used for stickers) and polyester banner film (used for roll-ups). PVC 510g is the standard outdoor banner material.

## Issues Encountered

- 6 tests referenced `vinylId` for banners — updated to `pvc510gId`
- `ConfigurationValidator` had non-exhaustive match on `FinishParameters` — added `GumRopeParams` case
- UI module has pre-existing compilation errors (unrelated to this change)

## Follow-up Items

- [ ] Consider adding `MaterialAreaTier` support to `CustomerPricelistResolver` for custom pricing
- [ ] UI components for gum rope length input and grommet spacing selection
- [ ] External production flow for banners > 150cm (out of scope per requirements)
- [ ] Consider warning when gum rope length seems excessive for the banner size
