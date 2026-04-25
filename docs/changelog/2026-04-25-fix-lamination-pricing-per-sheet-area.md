# 2026-04-25 — Fix Lamination Pricing: Per Sheet and Per Area

**PR:** N/A
**Author:** Claude Code
**Type:** bugfix

## Summary

Fixed `FinishSurcharge` / `FinishTypeSurcharge` calculation so that lamination (and other
per-unit finish surcharges) use the correct pricing basis depending on the material pricing
model. Previously all finish surcharges were multiplied by the order quantity (items),
regardless of whether the material was sheet-priced or area-priced.

## Changes Made

- **`modules/domain/src/main/scala/mpbuilder/domain/pricing/PriceCalculator.scala`**
  - Added `private enum FinishPricingBasis` with three cases: `PerItem`, `PerSheet`, `PerArea`
  - Updated `computeFinishLines` and `computeSingleFinishLine` signatures to accept both
    `quantity: Int` (kept for per-piece finishes: scoring, grommets, rope) and a new
    `basis: FinishPricingBasis` parameter for the `FinishSurcharge`/`FinishTypeSurcharge`
    fallback path
  - **Sheet-priced** materials pass `FinishPricingBasis.PerSheet(sheetsUsed)` — finish
    `LineItem.quantity` now equals `sheetsUsed`, not order quantity
  - **Area-priced** materials pass `FinishPricingBasis.PerArea(areaSqM, effectiveQuantity)` —
    `unitPrice = (surcharge × areaSqM).rounded`, `quantity = effectiveQuantity`
  - **Base-priced** materials pass `FinishPricingBasis.PerItem(quantity)` — unchanged behavior
- **`modules/domain/src/test/scala/mpbuilder/domain/PriceCalculatorSpec.scala`**
  - Updated `"banner with area-based calculation"` expected values:
    - `finishLines.head.unitPrice`: `Money("0.04")` → `Money("0.02")` (0.04/m² × 0.5 m²)
    - `breakdown.subtotal`: `Money("90.40")` → `Money("90.20")`
    - `breakdown.total`: `Money("90.40")` → `Money("90.20")`
- **`docs/pricing.md`**: Updated the "Banner (Area-Based)" worked example to reflect the
  corrected per-m² finish pricing

## Decisions & Rationale

- **`FinishPricingBasis` instead of changing `FinishSurcharge` semantics globally**: the basis
  is derived from the material pricing context at call time, so the same pricelist rule value
  already has the correct "unit" meaning: per-sheet for sheet pricelists, per-m² for area
  pricelists, per-item for base pricelists.
- **Kept `quantity: Int` alongside `basis`**: per-piece finishes (scoring, grommets, rope)
  always charge per finished item regardless of material type — a scoring crease happens once
  per finished brochure, not once per press sheet. Reusing the item `quantity` parameter for
  those paths avoids introducing a second axis of variation there.
- **No sample pricelist value changes needed**: the existing CZK area pricelist value of
  60 CZK for `FinishTypeSurcharge(Overlamination)` is a sensible per-m² rate. The USD
  UV-coating value of $0.04 is similarly a per-m² rate that produces $0.02/banner at 0.5 m².

## Issues Encountered

None — the change compiled cleanly on the first attempt and only one test required updating.

## Follow-up Items

- [ ] Consider whether the "Tri-fold Brochure" worked example in pricing.md should be updated
  to show the concrete per-sheet finish calculation rather than the placeholder `2.00 CZK × 100`
- [ ] The sheet pricelist lamination values (6 CZK matte/gloss, 9 CZK soft-touch) are now
  interpreted as per-press-sheet amounts — verify they reflect realistic Czech print market rates
