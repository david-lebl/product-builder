# 2026-05-09 — Sheet Material Size Validation

**PR:** copilot/add-validation-product-size
**Author:** copilot agent
**Type:** bugfix

## Summary

Added validation to prevent configurations where the requested product size is larger than what selected sheet materials can physically support in sheet-fed printing workflows. For sheet-fed processes, A3 (420×297 mm) is now enforced as the maximum sheet limit for paper/cardboard materials.

## Changes Made

### Domain
- `modules/domain/src/main/scala/mpbuilder/domain/validation/ConfigurationValidator.scala`
  - Added structural validation for sheet-material size limits.
  - Added A3 default sheet limit for sheet-fed process types (`Offset`, `Digital`, `Letterpress`) on paper/cardboard materials.
  - Added orientation-aware fit check (product can fit in either portrait or landscape orientation on the sheet).

- `modules/domain/src/main/scala/mpbuilder/domain/validation/ConfigurationError.scala`
  - Added `ProductSizeExceedsSheetMaterial` error case with EN/CS localized messages.

### Tests
- `modules/domain/src/test/scala/mpbuilder/domain/ConfigurationBuilderSpec.scala`
  - Added tests for:
    - rejecting oversized sheet-fed paper products,
    - allowing exact A3 size,
    - not applying A3 sheet limit to large-format workflows.

### Documentation
- `docs/features.md` — updated validation pipeline description to include sheet-material size validation behavior.
- `docs/troubleshooting.md` — added note for using repository `./mill` wrapper when `mill` is not on PATH.
- `docs/changelog/README.md` and `docs/INDEX.md` — added references to this changelog entry.

## Decisions & Rationale

- Implemented this as **structural validation** so the check always runs with core configuration checks and accumulates with other validation errors.
- Applied the limit only to **sheet-fed printing processes** to avoid blocking valid large-format scenarios (e.g., banner workflows).
- Used an **orientation-aware fit check** because sheet imposition can rotate the product.

## Issues Encountered

- Running `mill` directly failed in the sandbox (`mill: command not found`), while the repository-local wrapper (`./mill`) worked as expected.
- Resolved by switching all local build/test commands to `./mill --no-server ...`.

## Follow-up Items

- [ ] Consider adding explicit per-material maximum sheet size metadata in `Material` if future catalog data needs limits different from A3.
- [ ] Consider exposing this validation reason directly in UI helper text near size selection.
