# 2026-04-19 — Extended Binding and Cover Configuration

**PR:** copilot/extend-binding-options-calendar-products
**Author:** copilot-swe-agent
**Type:** feature

## Summary

Implemented the full plan for extended binding and cover configuration, significantly expanding the domain model to support real-world calendar and booklet product requirements. This includes renaming binding methods for clarity, splitting cover roles, modeling binding as a configurable component, adding binding edge/pitch specifications, new finish types, and comprehensive compatibility rules.

## Changes Made

### Model Layer
- **`model/finish.scala`** — Renamed `SpiralBinding` → `PlasticCoilBinding`, `WireOBinding` → `MetalWireBinding`; added `HangingStrip`, `IndexTab`, `ShrinkWrap` finish types; added `SaddleStitchParams`, `DrillingParams`, `IndexTabParams` finish parameters
- **`model/component.scala`** — Added 7 new `ComponentRole` variants: `FrontCover`, `BackCover`, `Binding`, `HangingStrip`, `CaseBoard`, `Endpaper`, `Packaging`
- **`model/specification.scala`** — Added `BindingEdge` enum (Top/Left/Right), `BindingPitch` enum (ThreeToOne/FourToOne), corresponding `SpecValue` and `SpecKind` variants
- **`model/configuration.scala`** — Added `frontCoverComponent`, `backCoverComponent`, `bindingComponent` helper methods to `ProductConfiguration`

### Rules & Validation
- **`rules/predicates.scala`** — Added `HasComponentRole`, `BindingMaterialIs`, `BindingEdgeIs` configuration predicates
- **`validation/RuleEvaluator.scala`** — Implemented evaluation for new predicates
- **`validation/ConfigurationValidator.scala`** — Added validation for `SaddleStitchParams`, `DrillingParams`, `IndexTabParams`

### Sample Data
- **`sample/SampleCatalog.scala`** — Added 7 binding hardware materials, 3 calendar cover materials, 10 new material IDs; updated `calendars` category with `FrontCover`/`BackCover`/`Binding` components and premium preset; updated `booklets` with `Binding` component and wire binding preset
- **`sample/SampleRules.scala`** — Updated binding method references; added 5 new compatibility rules for binding material types, binding edge constraints
- **`sample/SamplePricelist.scala`** — Updated binding method references across all 3 pricelists; added pricing for all new materials

### Services
- **`service/ConfigurationBuilder.scala`** — Updated `deriveSheetCount` for all new component roles
- **`service/PresetPriceService.scala`** — Added default `BindingEdgeSpec` for categories requiring it
- **`pricing/PriceCalculator.scala`** — Updated binding method display names (EN/CS)
- **`codec/DomainCodecs.scala`** — Added JSON codecs for `BindingEdge`, `BindingPitch`, and new `FinishParameters` variants

### UI
- **`SpecificationForm.scala`** — Added binding edge selector UI; updated help text; added `bindingEdgeLabel` method
- **`ConfigurationForm.scala`** — Added labels for all 7 new component roles (EN/CS)
- **`PricePreview.scala`** — Added labels for all 7 new component roles (EN/CS)
- **`EditorBridge.scala`** — Updated binding method references
- **`ProductBuilderViewModel.scala`** — Added default `BindingEdgeSpec` when `BindingEdge` is required

### Tests
- **`ConfigurationBuilderSpec.scala`** — Added `BindingEdgeSpec` to all booklet/calendar test configurations
- **`PriceCalculatorSpec.scala`** — Updated setup fee label assertion from "Spiral" to "Plastic Coil"
- **`WorkflowGeneratorSpec.scala`** — Added `BindingEdgeSpec` to booklet test configurations
- **`WorkflowEngineSpec.scala`** — Added `BindingEdgeSpec` to booklet test configurations
- **`CatalogQueryServiceSpec.scala`** — Updated required spec kinds assertion for booklets

## Decisions & Rationale

- **Renamed `SpiralBinding`/`WireOBinding`** → `PlasticCoilBinding`/`MetalWireBinding` — industry-standard terms prevent UX confusion and make compatibility rules self-documenting
- **Kept `Cover` role** alongside new `FrontCover`/`BackCover` — booklets use a single undifferentiated cover (same sheet front/back); calendars need separate front (transparent) and back (cardboard)
- **Binding as a component (not a spec value)** — leverages existing material/pricing infrastructure; enables color/type selection per binding element
- **`BindingEdgeIs` predicate** — needed a predicate that returns `false` when no binding edge is set (unlike `AllowedBindingEdges` which passes validation when no spec exists). This prevents global technology constraints from failing on products that don't use binding
- **Hardware materials use `MaterialFamily.Hardware`** — binding wire/coil and cover sheets aren't paper; using Hardware family prevents paper-specific rules from applying
- **Calendar cover materials (`clearPvcCover`, `clearPetCover`)** also use `MaterialFamily.Hardware` — they're plastic sheets, not printable paper

## Issues Encountered

- **Global TechnologyConstraint rules evaluated on all products** — The initial `AllowedBindingEdges` spec predicate passed validation when no binding edge was set (returning `Validation.unit`), but when combined with `Not(...)` in a technology constraint, it caused the rule to fail for products like business cards and banners. Fixed by creating a dedicated `BindingEdgeIs` configuration predicate that returns `false` when no binding edge spec exists.
- **87 test failures after adding `BindingEdge` as required spec** — All tests constructing booklet/calendar configurations needed `BindingEdgeSpec` added. Resolved with systematic updates across 5 test files.
- **CategoryPresetSpec pricing failures** — New presets (`premium wall calendar`, `metal wire booklet`) included materials not yet priced in the `pricelistCzkSheet`. Added `MaterialBasePrice` rules for all new materials across all 3 pricelists.

## Follow-up Items

- [ ] Add UI support for `BindingPitch` spec selector (enum exists but no UI form element yet)
- [ ] Implement `HangingStrip` component template for wall calendars
- [ ] Add `CaseBoard` and `Endpaper` component templates for case binding products
- [ ] Add `Packaging` component templates (shrink wrap, poly bag)
- [ ] Add pricing rules for `IndexTab`, `ShrinkWrap`, `HangingStrip` finish types
- [ ] Add more compatibility rules for cover minimum weight (250gsm structural rigidity)
- [ ] Consider adding `BindingPitch` selector to UI for wire/coil products
