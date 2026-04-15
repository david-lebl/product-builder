# 2026-04-15 — Compact Product Builder View

**PR:** copilot/add-compact-product-builder-view
**Author:** GitHub Copilot coding agent
**Type:** feature

## Summary

Created a compact alternative view of the Product Builder that presents all form fields with their labels **to the left** of the input/select element (inline/horizontal layout), rather than above. The view has identical functionality to the standard `ProductBuilder` view but is significantly more space-efficient.

A new nav tab "Compact Builder" / "Kompaktní parametry" was added to the top navigation bar.

## Changes Made

### New files
- `modules/ui/src/main/scala/mpbuilder/ui/productbuilder/ProductBuilderCompactApp.scala`
  — Minimal wrapper around the existing `ConfigurationForm`, `PricePreview`, `ValidationMessages`, and `BasketView` components; adds `compact-builder` CSS scope class.
- `modules/ui/src/main/resources/compact-builder.css`
  — CSS-only compact layout: uses `:has(> label:not(.checkbox-label))` grid override so only true label+field groups get horizontal layout. Finish-section checkboxes and the linked-components toggle are intentionally excluded from horizontal layout. `selector-with-help` containers are restyled as flex rows so help buttons appear inline next to their select fields.

### Modified files
- `modules/ui/src/main/scala/mpbuilder/ui/AppRouter.scala`
  — Added `AppRoute.ProductBuilderCompact`, navigation link, route rendering, basket button visibility update.
- `modules/ui/src/main/resources/index.html`
  — Added `<link rel="stylesheet" href="compact-builder.css">`.

## Decisions & Rationale

### CSS-only scoping (no new components)
All existing sub-components (`ConfigurationForm`, `FinishSelector`, `MaterialSelector`, …) are reused as-is. The compact layout is achieved entirely through CSS under the `.compact-builder` scope class, so there is zero duplication of business logic or DOM structure.

### `:has(> label:not(.checkbox-label))` selector
The key challenge was applying horizontal layout only to standard label+field groups while leaving:
- **Finish checkboxes** – their `label.checkbox-label` already wraps both the `<input type="checkbox">` and the finish name span. Applying horizontal grid to these would conflict with the existing `:has(.finish-params[style*="block"])` border-radius rule that changes when a subform expands.
- **linked-components toggle** – also uses `label.checkbox-label`.
- **FinishSelector outer group** – its `div.form-group` has a `div.label-with-help` as first child, not a direct `label`, so it naturally falls outside the selector.

### `selector-with-help` flex reflow
In the standard layout, `selector-help-buttons` is `position: absolute; top: 0; right: 0` relative to `selector-with-help`. In compact mode this would overlap the inline label. The fix changes `selector-with-help` to `display: flex` and `selector-help-buttons` to `position: static` so the `?` / `i` buttons appear inline to the right of the select field.

### Shared ViewModel state
Both views (`ProductBuilderApp` and `ProductBuilderCompactApp`) read from the same `ProductBuilderViewModel` singleton. Switching between the two tabs preserves all selections.

## Issues Encountered

- **Help button rendering below selector** – First iteration had `selector-help-buttons` as `position: static` flowing below the form-group as a block element. Fixed by changing `selector-with-help` to `display: flex; flex-wrap: wrap` so the help buttons appear as a flex sibling to the right of the form-group.
- **Empty CSS rules from review** – Code review flagged two empty placeholder rules; removed them and consolidated the artwork section comment.

## Follow-up Items

- [ ] Consider adding a toggle button on the standard Product Builder view to switch to the compact view in place (rather than a separate nav tab).
- [ ] Consider persisting the user's preferred view (compact vs standard) in `localStorage`.
