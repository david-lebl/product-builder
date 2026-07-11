# 2026-05-09 — Material Sheet Size Validation

**PR:** copilot/add-validation-product-size-again
**Author:** Copilot agent
**Type:** feature

## Summary

Added a validation that a product's configured size cannot exceed the maximum sheet size of the selected material. For sheet-based printing (offset, digital, letterpress), the physical sheet dimensions limit what product sizes can be produced. This validation is now enforced in the configuration builder and reported as a clear, localised error.

## Changes Made

- **`modules/domain/src/main/scala/mpbuilder/domain/model/material.scala`** — Added `maxSheetSize: Option[Dimension] = None` field to the `Material` case class. Defaults to `None` (no limit), so roll-fed and non-sheet materials are unaffected.
- **`modules/domain/src/main/scala/mpbuilder/domain/validation/ConfigurationError.scala`** — Added `ProductExceedsMaterialSheetSize(materialId, productSize, maxSheetSize)` error case with EN/CS localised messages.
- **`modules/domain/src/main/scala/mpbuilder/domain/validation/ConfigurationValidator.scala`** — Added sheet size check in `validateStructural`. For each component whose material has a `maxSheetSize`, verifies that the product's `SizeSpec` fits within the sheet in either natural or rotated orientation (portrait/landscape swap).
- **`modules/domain/src/main/scala/mpbuilder/domain/sample/SampleCatalog.scala`** — Set `maxSheetSize = Some(Dimension(420, 297))` (A3) for all paper and cardboard materials (24 materials total). Vinyl, PVC banner, hardware, and fabric materials remain `None`.
- **`modules/domain/src/test/scala/mpbuilder/domain/ConfigurationBuilderSpec.scala`** — Added `suite("material sheet size validation")` with 8 tests.

## Decisions & Rationale

- **`maxSheetSize` on `Material`, not a `CompatibilityRule`** — The maximum sheet size is an intrinsic physical property of the material (the largest sheet available from the supplier), not a policy decision. Encoding it on the material is more self-documenting and avoids needing a separate rule per material.
- **Orientation-aware fit check** — A product of `(w, h)` fits if `(w ≤ maxW && h ≤ maxH)` OR `(w ≤ maxH && h ≤ maxW)`. This models the fact that a sheet can be fed in either portrait or landscape orientation.
- **A3 (420×297 mm) as the default for standard sheet materials** — As stated in the problem description, A3 is the most common upper limit for sheet-fed printing. Different materials can be set to different sizes simply by changing their `maxSheetSize` value.
- **`Option[Dimension]` with default `None`** — Roll materials (vinyl banners, PVC), hardware components (stands), and fabric substrates have no sheet size concept. Using `None` means "no sheet size restriction" and avoids false rejections for large-format products.

## Issues Encountered

- **Syntax error in `ConfigurationValidator.scala`** — The first `edit` call (replacing `val allChecks = ...`) left the old `allChecks.foldLeft(...)` call concatenated on the same line due to an incomplete `old_str` match. Fixed by a second targeted edit to add the missing newline.

## Follow-up Items

- [ ] Expose `maxSheetSize` in the catalog UI editor when Phase 9 (catalog editor) is implemented, so shop operators can configure it per material.
- [ ] Consider adding a `minSheetSize` if small-format lower bounds become a requirement.
- [ ] `ProductExceedsMaterialSheetSize` could also surface as a warning in the progressive disclosure (`CatalogQueryService`) — currently it only appears at full-validation time.
