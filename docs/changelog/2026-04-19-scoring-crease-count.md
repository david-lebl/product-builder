# 2026-04-19 — Standalone Crease (Scoring) Count Support

**PR:** TBD
**Author:** agent
**Type:** feature

## Summary

Extended the existing `Scoring` finish to support a configurable crease count (1–4 per page). Previously, Scoring was a binary finish (on/off) with a flat per-unit surcharge. Now it accepts `ScoringParams(creaseCount)` with tiered pricing per crease count, enabling standalone crease lines without folding.

## Changes Made

- **`modules/domain/src/main/scala/mpbuilder/domain/model/finish.scala`** — Added `ScoringParams(creaseCount: Int)` to `FinishParameters` sealed trait
- **`modules/domain/src/main/scala/mpbuilder/domain/pricing/PricingRule.scala`** — Added `ScoringCountSurcharge(count: Int, surchargePerUnit: Money)` rule variant
- **`modules/domain/src/main/scala/mpbuilder/domain/pricing/PriceCalculator.scala`** — Updated `computeFinishLines` to check for `ScoringCountSurcharge` when `ScoringParams` is present (takes precedence over flat `FinishTypeSurcharge`)
- **`modules/domain/src/main/scala/mpbuilder/domain/codec/DomainCodecs.scala`** — Added `ScoringParams` JSON codec
- **`modules/domain/src/main/scala/mpbuilder/domain/sample/SamplePricelist.scala`** — Added `ScoringCountSurcharge` entries for 1–4 creases in both USD and CZK pricelists
- **`modules/domain/src/main/scala/mpbuilder/domain/sample/SampleCatalog.scala`** — Added `scoringId` to flyers category `allowedFinishIds`
- **`modules/ui/src/main/scala/mpbuilder/ui/productbuilder/components/FinishSelector.scala`** — Added crease count radio buttons (1–4) for Scoring finish type; defaults to `ScoringParams(1)` on first selection
- **`modules/ui/src/main/scala/mpbuilder/ui/catalog/views/PricelistEditorView.scala`** — Added `ScoringCountSurcharge` to rule type list, display, form fields, builder, and extractors

## Decisions & Rationale

- **Model as finish parameter, not a new spec** — Crease-without-fold is an optional finishing operation, not a product-defining specification like `FoldType`. Using `FinishParameters` follows the same pattern as `RoundCornersParams(cornerCount)`.
- **Reuse `Scoring` finish type** — The existing `FinishType.Scoring` semantically covers both "scoring for fold lines" and "standalone crease". No new `FinishType` needed.
- **Tiered pricing via lookup table** — The non-linear price progression (0.60, 1.00, 1.30, 1.50 CZK) doesn't fit a simple base+per-crease formula, so a `ScoringCountSurcharge(count, price)` lookup is used instead.
- **Backward compatibility preserved** — When no `ScoringParams` is present, the existing flat `FinishTypeSurcharge(Scoring)` still applies. The count-based surcharge only takes effect when `ScoringParams` is explicitly set.

## Issues Encountered

- **Pre-existing UI compilation errors** — The `ui` module has 7 pre-existing compilation errors in `PricelistEditorView.scala` (missing `placeholder` and `display` arguments to `FormComponents` methods). These are unrelated to this feature and were confirmed by building the baseline before any changes.

## Follow-up Items

- [ ] Fix the 7 pre-existing UI compilation errors in `PricelistEditorView.scala`
- [ ] Consider adding `scoringId` to presentation folders and poster categories
- [ ] Add dedicated PriceCalculator test cases for `ScoringCountSurcharge` pricing
