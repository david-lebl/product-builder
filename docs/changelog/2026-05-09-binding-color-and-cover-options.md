# 2026-05-09 — Binding Color and Spiral/Wire Cover Options

**PR:** copilot/add-binding-method-options
**Author:** copilot agent
**Type:** feature

## Summary

Added three new configurable options for binding-based products (booklets, calendars):

1. **Binding color** — When the customer selects Spiral Binding, Wire-O Binding, or Case Binding, a "Binding Color" dropdown appears. For spiral/wire methods it shows ring colors; for case binding it shows desk cover colors.
2. **Front cover** — For Spiral and Wire-O bindings, the customer can choose between a Transparent or Carton front cover.
3. **Back cover** — Same choice as front cover, independently selectable.

Binding colors are backed by a new `MaterialFamily.BindingMaterial` family so they are fully manageable in the material catalog alongside other stocks (paper, vinyl, etc.).

## Changes Made

### Domain model
- `modules/domain/src/main/scala/mpbuilder/domain/model/material.scala` — Added `BindingMaterial` to `MaterialFamily` enum
- `modules/domain/src/main/scala/mpbuilder/domain/model/specification.scala` — Added `SpiralCoverType` enum (`Transparent | Carton`) and three new `SpecValue` / `SpecKind` variants: `BindingColorSpec(materialId)`, `SpiralFrontCoverSpec(coverType)`, `SpiralBackCoverSpec(coverType)` with corresponding `SpecKind.BindingColor`, `SpiralFrontCover`, `SpiralBackCover`
- `modules/domain/src/main/scala/mpbuilder/domain/codec/DomainCodecs.scala` — Added JSON codec for `SpiralCoverType` (alongside existing enum codecs)

### Sample data
- `modules/domain/src/main/scala/mpbuilder/domain/sample/SampleCatalog.scala` — Added 10 binding material entries: 6 spiral/wire ring colors (black, silver, red, blue, white, gold) and 4 desk cover colors (black, bordeaux, navy, brown). All use `MaterialFamily.BindingMaterial`.

### UI / ViewModel
- `modules/ui/src/main/scala/mpbuilder/ui/productbuilder/ProductBuilderViewModel.scala` — Added signals `selectedBindingColor`, `selectedSpiralFrontCover`, `selectedSpiralBackCover`; added `availableBindingMaterials` (static list) and `availableBindingColorMaterials` (signal that filters by binding method — ring colors for spiral/wire, desk covers for case binding)
- `modules/ui/src/main/scala/mpbuilder/ui/productbuilder/components/SpecificationForm.scala` — Added two conditional UI sections: (1) "Binding Color" shown for Spiral, Wire-O, CaseBinding; (2) "Front Cover" + "Back Cover" shown only for Spiral and Wire-O; changing binding method clears dependent specs; added `spiralCoverLabel` helper
- `modules/ui/src/main/scala/mpbuilder/ui/productbuilder/components/EmailOrderModal.scala` — Extended spec formatter to cover `BindingColorSpec`, `SpiralFrontCoverSpec`, `SpiralBackCoverSpec`, and `BleedSpec` (with comment explaining why bleed is omitted from email summaries)

## Decisions & Rationale

- **`MaterialFamily.BindingMaterial` instead of a new field on `ProductCategory`** — Binding colors are simply a new kind of stock. Adding them to the existing material catalog (with a new family variant) lets them be managed through the normal catalog editor without model changes to `ProductCategory` or `ComponentTemplate`.
- **`SpecValue` variants for color/cover, not `FinishParameters`** — Binding color and cover choices are product specifications, not post-print finishes. Modeling them as specs is consistent with how `BindingMethodSpec` is already handled.
- **Filtering colors by binding method via ID prefix** — Spiral/wire ring colors have IDs starting with `mat-binding-`, desk covers start with `mat-desk-`. The signal `availableBindingColorMaterials` applies this filter reactively so the dropdown always shows only relevant options. A more general approach (e.g. a material metadata field) was deemed over-engineering for the current scope.
- **Clearing dependent specs on method change** — When the binding method changes, `BindingColorSpec`, `SpiralFrontCoverSpec`, and `SpiralBackCoverSpec` are cleared so stale color/cover selections cannot persist.

## Issues Encountered

None significant. Compilation succeeded on first attempt. The only adjustment was extending the non-exhaustive match in `EmailOrderModal` to handle the three new `SpecValue` variants (compiler warned during the initial compile).

## Follow-up Items

- [ ] Consider adding a more formal "binding material type" property to `Material` (e.g., `MaterialProperty.SpiralWireRing` / `MaterialProperty.DeskCover`) to remove the ID-prefix convention dependency
- [ ] Cover selection options could be linked to pricing rules (e.g., transparent cover costs more than carton)
- [ ] Validation rule: ensure a binding color is selected when a binding method that requires it is chosen
