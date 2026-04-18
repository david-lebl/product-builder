# 2026-04-18 — Calendar Covers, Binding Colors & Binding Method Rename

**PR:** copilot/add-calendar-cover-options
**Author:** agent
**Type:** feature

## Summary

Three related improvements for calendars and bound products:

1. **Binding method rename** — `SpiralBinding` → `PlasticOBinding` and `WireOBinding` → `MetalWireBinding` to clearly distinguish plastic o-ring binding from metal wire binding.
2. **Binding color selection** — New `BindingColor` spec (Black, White, Silver, Blue, Red, Clear) for calendars and booklets that use plastic o-binding or metal wire binding.
3. **Calendar protective covers** — Optional `FrontCover` (transparent plastic) and `BackCover` (350g cardboard in 4 color options) components for calendar products.

## Changes Made

### Domain model
- `modules/domain/src/main/scala/mpbuilder/domain/model/specification.scala` — Added `BindingColor` enum, `SpecValue.BindingColorSpec`, `SpecKind.BindingColor`; renamed binding method variants
- `modules/domain/src/main/scala/mpbuilder/domain/model/component.scala` — Added `ComponentRole.FrontCover` and `ComponentRole.BackCover`
- `modules/domain/src/main/scala/mpbuilder/domain/model/material.scala` — Added `MaterialFamily.Plastic`
- `modules/domain/src/main/scala/mpbuilder/domain/model/category.scala` — Added `SpecKind.BindingColor`

### Rules & validation
- `modules/domain/src/main/scala/mpbuilder/domain/rules/predicates.scala` — Added `SpecPredicate.AllowedBindingColors`
- `modules/domain/src/main/scala/mpbuilder/domain/validation/RuleEvaluator.scala` — Evaluation logic for `AllowedBindingColors`

### Pricing
- `modules/domain/src/main/scala/mpbuilder/domain/pricing/PriceCalculator.scala` — Display names for new binding colors and renamed binding methods

### Sample data
- `modules/domain/src/main/scala/mpbuilder/domain/sample/SampleCatalog.scala` — 5 new materials (transparent plastic, 4 cardboard colors), updated calendar category with optional FrontCover/BackCover, presets with covers, binding colors
- `modules/domain/src/main/scala/mpbuilder/domain/sample/SampleRules.scala` — Binding color rules for calendars and booklets
- `modules/domain/src/main/scala/mpbuilder/domain/sample/SamplePricelist.scala` — Pricing for new materials in USD, CZK, CZK-sheet pricelists

### Codecs
- `modules/domain/src/main/scala/mpbuilder/domain/codec/DomainCodecs.scala` — JSON codecs for `BindingColor`

### Services
- `modules/domain/src/main/scala/mpbuilder/domain/service/ConfigurationBuilder.scala` — `deriveSheetCount` for new roles

### UI
- `modules/ui/src/main/scala/mpbuilder/ui/productbuilder/components/SpecificationForm.scala` — Binding color selector, updated help text
- `modules/ui/src/main/scala/mpbuilder/ui/productbuilder/components/ConfigurationForm.scala` — Role labels for FrontCover/BackCover
- `modules/ui/src/main/scala/mpbuilder/ui/productbuilder/components/PricePreview.scala` — Role labels for FrontCover/BackCover
- `modules/ui/src/main/scala/mpbuilder/ui/productbuilder/ProductBuilderViewModel.scala` — `selectedBindingColor` signal
- `modules/ui/src/main/scala/mpbuilder/ui/visualeditor/EditorBridge.scala` — Updated binding method references

### Tests
- `modules/domain/src/test/scala/mpbuilder/domain/PriceCalculatorSpec.scala` — Updated for renamed binding methods
- `modules/domain/src/test/scala/mpbuilder/domain/ConfigurationBuilderSpec.scala` — Added `BindingColorSpec` to calendar tests

## Decisions & Rationale

- **Binding method names**: "Plastic O-Binding" and "Metal Wire Binding" are more descriptive than "Spiral" and "Wire-O" for users who aren't familiar with printing terminology.
- **BindingColor as required spec for calendars only**: Booklets have binding color as allowed but not required, since saddle stitch and perfect binding don't use colored rings.
- **Optional FrontCover/BackCover**: Used `ComponentTemplate(optional = true)` so calendars can be configured with or without protective covers.
- **Transparent plastic as a separate MaterialFamily.Plastic**: It's clearly not paper, cardboard, vinyl, or fabric. Adding Plastic keeps the taxonomy clean.

## Issues Encountered

- **JSON codec missing for BindingColor**: The zio-json `DeriveJsonCodec.gen[SpecValue]` failed because `BindingColor` had no encoder/decoder. Fixed by adding explicit String-based codecs in `DomainCodecs.scala`.
- **Test assertion checking label string "Spiral"**: One PriceCalculator test checked for `"Spiral"` in a setup fee label. Updated to match new `"Plastic O-Binding"` label.

## Follow-up Items

- [ ] Consider making binding color conditionally required only when PlasticOBinding or MetalWireBinding is selected (currently it's always required for calendars)
- [ ] Add binding color surcharge pricing (currently binding color choice is free)
- [ ] Consider adding more protective cover material options (e.g., frosted plastic, leather-look cardboard)
