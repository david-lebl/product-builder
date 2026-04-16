# 2026-04-16 — Compact UI Redesign for ProductBuilderApp

**PR:** copilot/update-ui-style-productbuilderapp
**Author:** agent
**Type:** refactoring

## Summary

Rewrote the ProductBuilderApp UI to be more compact and space-efficient, inspired by Vaadin component patterns. Key changes include horizontal form layouts (label left, input right), inline help-info buttons next to labels, horizontal manufacturing speed cards, slimmer info notes, always-visible size fields, and a subtle linked-components toggle.

## Changes Made

### Scala Components (modules/ui/src/main/scala/mpbuilder/ui/productbuilder/components/)
- **CategorySelector.scala** — Replaced selector-with-help pattern with form-group--horizontal + label-with-help. Built select element inline instead of via SelectField wrapper.
- **PrintingMethodSelector.scala** — Same horizontal layout refactor with help-info moved to label.
- **MaterialSelector.scala** — Horizontal layout, info-box replaced with slim info-note.
- **InkConfigSelector.scala** — Horizontal layout with inline select.
- **SpecificationForm.scala** — Quantity/Pages use horizontal layout. Size field now always shows W×H inputs (disabled when preset selected, editable when custom). Removed duplicate "Number of" prefix from Pages label. Info-box note replaced with slim info-note. Manufacturing speed section no longer has redundant "Manufacturing Speed:" label. Speed cards use horizontal CSS modifier class.
- **FinishSelector.scala** — Shortened label text ("Finishes:" instead of "Finishes (select multiple):"). Removed the count info-box. Empty state uses info-note.
- **ConfigurationForm.scala** — Removed duplicate section 5 heading for Manufacturing Speed. Linked-components toggle uses new subtle style with hint text.

### UI Framework (modules/ui-framework/src/main/scala/mpbuilder/uikit/fields/)
- **FormGroup.scala** — Wraps content in form-group__control div; applies form-group--horizontal class for CSS grid horizontal layout.

### CSS (modules/ui/src/main/resources/)
- **utilities.css** — Added `.form-group--horizontal` grid layout (140px label column + 1fr control). Added `.form-group__control`. Normalized input/select height to 36px. Added `.info-note` class. Added `.size-composite-field`, `.size-dimensions-row`, `.size-dim-input`, `.size-dim-separator`. Added `.linked-toggle` and `.linked-toggle__hint`. Made `.speed-tier-cards--horizontal` flex-row layout. Made finish items, checkbox labels, radio labels slimmer. Responsive fallback to stacked on narrow viewports.
- **pricing.css** — Reduced price display padding and font sizes for compactness.
- **layout.css** — Reduced card padding and heading sizes.
- **uikit.css** — Reduced help-info trigger size (16px) for better fit next to labels.

## Decisions & Rationale

- **Horizontal form layout** — Placing labels left and controls right saves vertical space significantly when fields are stacked. This is the standard Vaadin/desktop-app pattern. Falls back to stacked on mobile.
- **Always-visible W×H inputs** — Instead of hiding/showing custom size fields, they're always visible but disabled when a preset is selected. This avoids layout shifts and makes the UI more predictable.
- **Removed duplicate headings** — The manufacturing speed section had both a ConfigurationForm h3 heading and a form-label inside SpecificationForm. Removed the inner label since the section heading already provides context, and the speed tier cards are self-descriptive.
- **Linked toggle without border** — Multi-component "same material" toggle now uses a subtle inline style with gray hint text instead of a bordered card, reducing visual noise.
- **Inline help-info** — Help buttons moved from absolute-positioned top-right corner to inline next to labels. This is more discoverable and follows Vaadin's pattern.

## Issues Encountered

- Mill build has SSL certificate issues in the sandbox environment, preventing compilation verification. Changes were validated by code review.

## Follow-up Items

- [ ] Verify the build compiles successfully in CI
- [ ] Visual testing of the new compact layout across screen sizes
- [ ] Consider adding CSS container queries for more granular responsive behavior
