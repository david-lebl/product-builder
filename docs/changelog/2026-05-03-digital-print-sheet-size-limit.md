# 2026-05-03 — Digital Print Sheet Size Limit (A3)

**PR:** copilot/limit-digital-print-to-a3
**Author:** copilot agent
**Type:** feature

## Summary

Implemented a size constraint for digital (sheet-fed) printing: the product size must
fit within the material's sheet dimension. All standard paper materials (coated art
paper, uncoated bond, silk, cotton, adhesive stock) are assigned an A3 sheet dimension
(297×420 mm), matching the maximum sheet that typical desktop digital presses can handle.

## Changes Made

- `modules/domain/src/main/scala/mpbuilder/domain/model/material.scala`  
  Added `sheetDimension: Option[Dimension] = None` field to `Material`.

- `modules/domain/src/main/scala/mpbuilder/domain/rules/predicates.scala`  
  Added `SizeWithinMaterialSheet` case to `ConfigurationPredicate` (with doc comment).

- `modules/domain/src/main/scala/mpbuilder/domain/validation/RuleEvaluator.scala`  
  Added evaluation logic: checks spec `SizeSpec` width/height against each component
  material's `sheetDimension`; materials without a sheet dimension always pass.

- `modules/domain/src/main/scala/mpbuilder/domain/sample/SampleCatalog.scala`  
  Added `sheetDimension = Some(Dimension(297, 420))` to paper materials:
  `coated300gsm`, `uncoatedBond`, `coatedSilk250gsm`, `adhesiveStock`, `cotton`,
  all 8 `coatedGlossy*` variants (90–350 gsm), all 9 `coatedMatte*` variants (90–350 gsm).  
  Packaging/specialty materials (`kraft`, `corrugated`, `yupo`) intentionally omitted —
  they are used for applications larger than A3.

- `modules/domain/src/main/scala/mpbuilder/domain/sample/SampleRules.scala`  
  Added `TechnologyConstraint`:
  ```
  Or(Not(HasPrintingProcess(Digital)), SizeWithinMaterialSheet)
  ```
  Error message: "Digital printing is limited by the material sheet size (max A3: 297×420mm)".

- `modules/domain/src/test/scala/mpbuilder/domain/ConfigurationBuilderSpec.scala`  
  Added suite `"digital print sheet size limit (max A3: 297×420 mm)"` with 6 tests:
  - Valid at exactly A3 (297×420 mm)
  - Valid smaller than A3 (A4 210×297 mm)
  - Rejected when width exceeds A3 (420×297 mm)
  - Rejected when height exceeds A3 (200×500 mm)
  - Offset printing not restricted by digital sheet constraint
  - Material without sheet dimension (kraft) is unrestricted

## Decisions & Rationale

- **`sheetDimension` on `Material` rather than `PrintingMethod`**: The actual sheet size
  is a property of the substrate (what fits in the press tray), not of the process itself.
  This also supports future multi-method catalogs where the same material might be loaded
  into different presses with different tray limits.

- **A3 (297×420 mm) as the limit**: A3 is the maximum standard sheet size for typical
  desktop digital presses (HP Indigo, Xerox Versant, Konica Minolta). The problem statement
  explicitly requested this limit.

- **Packaging materials excluded**: `kraft`, `corrugated`, and `yupo` are all allowed in
  the packaging category (`cat-packaging`), whose presets use `Dimension(300, 200)` —
  300 mm wide, which slightly exceeds A3's 297 mm width. Packaging is printed on
  larger-format flatbed or wide digital presses, so no A3 sheet limit applies.

- **`adhesiveStock` included**: Self-adhesive label/sticker paper is typically run on
  sheet-fed digital presses (HP Indigo, etc.) that do use A3 sheets, so the constraint is
  appropriate.

## Issues Encountered

- **Packaging preset size `300×200 mm` exceeds A3 width `297 mm`**: After initially
  assigning A3 to `kraft`, the `CategoryPresetSpec` tests failed because the packaging
  preset `preset-packaging-standard` uses `Dimension(300, 200)` with kraft paper and
  digital printing. Fixed by removing `sheetDimension` from `kraft`, `corrugated`, and
  `yupo` (packaging materials used on larger presses).

## Follow-up Items

- [ ] Consider adding per-material `sheetDimension` to remaining specialty materials
  (e.g., `cotton` paper, `coatedSilk250gsm`) with verified real-world press specs.
- [ ] Surface the sheet dimension limit in the UI configurator (e.g., size field hint
  showing "max 297×420 mm for this material").
- [ ] Consider SRA3 (320×450 mm) as an alternative limit for presses that accept
  oversized sheets (currently the full A3 is used as a conservative value).
