# 2026-04-21 — Scoring: stepper input and pricing fix for pricelistCzkSheet

**PR:** copilot/add-scoring-feature
**Author:** copilot agent
**Type:** bugfix

## Summary

Fixed two issues with the Scoring (creasing) finish:

1. **Pricing error on scoring selection**: `pricelistCzkSheet` (the pricelist used in the UI) was missing `ScoringCountSurcharge` rules. Selecting the Scoring finish sets `ScoringParams(creaseCount=1)` as the default, causing `PriceCalculator` to fail with `MissingScoringPrice(1)` because the sheet pricelist only had a generic `FinishTypeSurcharge(Scoring)` but no crease-count-specific rules.

2. **Limited crease choice via radio buttons**: The Scoring crease count selector used radio buttons, capped at `maxCreases` (default 4 when no compatibility rule capped it). This prevented customers from specifying more fold lines — for example, packaging boxes that can require 6–8 creases.

## Changes Made

- **`modules/domain/src/main/scala/mpbuilder/domain/sample/SamplePricelist.scala`**
  - Added `ScoringCountSurcharge` rules (1–8 creases) to `pricelistCzkSheet` — matching the CZK pricing from `pricelistCzk` and extended up to 8 creases.
  - Replaced `FinishTypeSetupFee(FinishType.Scoring, Money("50"))` with `ScoringSetupFee(Money("60"))` in `pricelistCzkSheet` for consistency with the crease-specific setup logic.
  - Added explanatory comment in the type-level finish surcharge section explaining why `FinishTypeSurcharge(Scoring)` is absent (now handled by `ScoringCountSurcharge`).

- **`modules/ui/src/main/scala/mpbuilder/ui/productbuilder/components/FinishSelector.scala`**
  - Replaced the radio button group for Scoring crease count with a `<input type="number">` with `min=1` and `max` driven by the `scoringMaxCreases` signal (defaults to 12 when no compatibility cap rule applies).
  - The input is initialised to the current params value (`ScoringParams.creaseCount`) or `"1"` when none.
  - Added an inline `finish-params-error` span that appears when the entered crease count exceeds the allowed maximum.

- **`modules/ui/src/main/resources/utilities.css`**
  - Added `.finish-params-input-group` — flex container for the number input + inline validation message.
  - Added `.finish-params-error` — small red error label styled with `--color-error`.

## Decisions & Rationale

- **ScoringCountSurcharge for creases 1–8**: The domain already supported any crease count; the sample pricelist was simply incomplete. Extending to 8 is conservative but covers all common packaging fold patterns (a standard 4-panel tuck box uses 4 creases; a locked-bottom box needs 8).
- **Default max of 12 when no cap rule**: `None` from `scoringMaxCreases` means "no explicit rule cap", not "zero allowed". Using 12 as the UI fallback max allows generous use while preventing accidental absurd values.
- **Number input over radio buttons**: Radio buttons only scale to small fixed ranges. A number input is more appropriate when the valid range is parameterically determined and can be large (e.g., packaging).
- **`ScoringSetupFee` replaces `FinishTypeSetupFee(Scoring)` in pricelistCzkSheet**: The dedicated setup-fee rule gives correct behavior (the calculator suppresses the generic fallback when `ScoringSetupFee` is present), making the pricing logic consistent across all CZK pricelists.

## Issues Encountered

- `sed -i` could not safely replace a uniquely-identified line because the same pattern appeared in multiple pricelists — used line-number–based replacement.

## Follow-up Items

- [ ] Consider adding `ScoringCountSurcharge` rules beyond 8 if customer feedback shows demand for high-crease packaging.
- [ ] Consider whether `FinishTypeSurcharge(Scoring)` fallback should be removed from `pricelistCzkSheet` entirely (currently it only applies to Scoring finishes that have no `ScoringParams`, which is a legacy/preset-only path).
