# Compact Product Builder View

## Overview

The **Compact Builder** is an alternative to the standard Product Builder (`/product-parameters`) that presents the same form with labels to the **left** of each input/select field instead of above it. The view is accessible via the "Compact Builder" / "Kompaktní parametry" navigation tab.

Both views share the same `ProductBuilderViewModel` state, so switching between them preserves all selections.

## Behaviour

### What changes in the compact view

- All standard text/number/select form fields render as a 2-column grid:
  - **Left column (150 px):** right-aligned label
  - **Right column (1fr):** input or select control
- Help buttons (`?` / `i`) appear inline to the right of the select, not absolutely positioned in the top-right corner.
- Section headings (`<h3>`) are smaller, uppercase, and separated by a thin border.
- Input/select padding is reduced (`6px 8px`, `0.88rem` font).
- The `info-box` within selectors (e.g. "11 material(s) available") wraps to its own full-width line below the inline label+field row.

### What is intentionally NOT changed

- **Finish checkboxes** — `label.checkbox-label` elements that wrap both the `<input type="checkbox">` and the finish name are excluded from the horizontal grid layout. The existing `:has(.finish-params[style*="block"])` border-radius behaviour when subforms expand must not be interfered with.
- **Linked-components toggle** — also uses `label.checkbox-label`, excluded for the same reason.
- **Manufacturing speed tier cards** — rendered as card elements, not label+input pairs; no change needed.
- **Preset cards** (Basic / Premium) — tile layout unchanged, only padding is reduced.

## CSS Approach

The entire compact styling is contained in `compact-builder.css` and scoped under `.compact-builder` (the outer div class of `ProductBuilderCompactApp`).

The key selector:

```css
.compact-builder .form-group:has(> label:not(.checkbox-label)) {
    display: grid;
    grid-template-columns: 150px 1fr;
    align-items: center;
    column-gap: 10px;
}
```

This applies horizontal layout exclusively to `.form-group` elements that have a **direct** `label` child that is **not** a `.checkbox-label`. The `:has()` CSS pseudo-class is used (well-supported in modern browsers: Chrome 105+, Firefox 121+, Safari 15.4+).

The `selector-with-help` container (which wraps `SelectField` + `selector-help-buttons` in category/material/method selectors) is changed to `display: flex; flex-wrap: wrap` so the help buttons flow as a flex sibling to the right of the form-group, rather than being absolutely positioned.

## Route

| Route | Component | Nav label |
|-------|-----------|-----------|
| `AppRoute.ProductBuilderCompact` | `ProductBuilderCompactApp` | "Compact Builder" (EN) / "Kompaktní parametry" (CS) |

## Files

| File | Role |
|------|------|
| `modules/ui/src/main/scala/mpbuilder/ui/productbuilder/ProductBuilderCompactApp.scala` | Entry-point component; adds `compact-builder` CSS scope |
| `modules/ui/src/main/resources/compact-builder.css` | All compact layout overrides |
| `modules/ui/src/main/scala/mpbuilder/ui/AppRouter.scala` | Route + nav link |
| `modules/ui/src/main/resources/index.html` | CSS link tag |
