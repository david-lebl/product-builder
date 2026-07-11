# 2026-05-09 — Binding Color and Spiral/Wire Cover Options

**PR:** copilot/add-binding-color-options
**Author:** copilot-swe-agent
**Type:** feature

## Summary

Added configurable binding color selection and spiral/wire front/back cover-type options to the product configuration flow, backed by domain spec types and catalog material data.

## Changes Made

- Modified domain model to add:
  - `SpecKind.BindingColor`, `SpecKind.FrontCoverType`, `SpecKind.BackCoverType`
  - `SpecValue.BindingColorSpec`, `SpecValue.FrontCoverTypeSpec`, `SpecValue.BackCoverTypeSpec`
  - `BindingCoverType` (`Transparent`, `Carton`)
  - `Material.color` metadata field
  - `MaterialProperty.BindingColorOption`
- Updated codecs to support the new domain enums/fields:
  - `modules/domain/src/main/scala/mpbuilder/domain/codec/DomainCodecs.scala`
- Added catalog support for selectable binding-color stock materials:
  - New sample materials (red/black/white binding colors) in `SampleCatalog`
  - `CatalogQueryService.availableBindingColorMaterials`
- Updated product builder behavior:
  - Added binding color and front/back cover selectors to `SpecificationForm`
  - Added view-model selectors and query accessors in `ProductBuilderViewModel`
  - Added email summary formatting for new spec values in `EmailOrderModal`
- Updated catalog material editor to expose `Material.color`:
  - Display color column in table
  - Add EN/CS color inputs in edit form
- Updated docs:
  - `docs/features.md`
  - `docs/troubleshooting.md`
  - `docs/INDEX.md`
  - `docs/changelog/README.md`

## Decisions & Rationale

- Modeled binding color as `BindingColorSpec(materialId)` rather than a free-text enum so available colors are driven by catalog stock configuration and stay consistent with existing material-management workflows.
- Introduced `MaterialProperty.BindingColorOption` to explicitly mark materials intended for binding/cover color selection, keeping color-option filtering declarative and simple.
- Added dedicated front/back cover specs only for spiral/wire bindings, because those methods require separate cover-sheet selection while other bindings do not.

## Issues Encountered

- Full `ui.compile` currently fails in unrelated catalog views due to pre-existing `FormComponents` signature mismatches (missing placeholder/display args). This is documented in `docs/troubleshooting.md` under “Catalog UI compile errors after `FormComponents` signature tightening”.
- Validation was completed via `domain.jvm.compile`, `domain.js.compile`, and `domain.jvm.test`, which cover the domain-side behavior for this feature.

## Follow-up Items

- [ ] Fix remaining catalog view call sites to match current `FormComponents` signatures so full `ui.compile` passes.
- [ ] Consider adding explicit compatibility/validation rules for when binding color or cover-type specs are required/missing based on selected binding method.
- [ ] Add UI-level tests for conditional rendering and defaults of the new binding fields.
