# 2026-04-15 — Price preview validate button & per-item display layout

**PR:** N/A
**Author:** Copilot Coding Agent
**Type:** feature

## Summary

Updated the Product Builder price preview sidebar so that per-item price is shown in the main total-price card, moved the "Validate price" action from the configuration form into the price preview card, and wired that button to toggle breakdown visibility (expanded by default).

## Changes Made

- Modified `modules/ui/src/main/scala/mpbuilder/ui/productbuilder/components/PricePreview.scala`
  - Added per-item price display directly in the main price card.
  - Added a full-width "Validate price" action under the total card.
  - Added decorative arrow indicator under the action button.
  - Made the button validate configuration and toggle breakdown collapse/expand state.
  - Added accessibility attributes (`aria-expanded`, `aria-controls`) for the toggle button.
- Modified `modules/ui/src/main/scala/mpbuilder/ui/productbuilder/components/ConfigurationForm.scala`
  - Removed the old "Validate price" button from the form section.
- Modified `modules/ui/src/main/resources/pricing.css`
  - Added styles to visually connect the total card and new button.
  - Added non-filled button styling and arrow indicator styling.
  - Added collapsed-state styling for breakdown visibility.

## Decisions & Rationale

- Kept breakdown expanded by default (`Var(true)`) to satisfy current UX requirement while still wiring the button for rollout behavior.
- Kept the moved action label as "Validate price" to preserve familiar wording while changing placement.
- Styled the button as non-filled and full-width with shared card corners so it appears connected to the price display.

## Issues Encountered

- A Laminar type mismatch occurred when returning `emptyNode` from a helper typed as `Element`.
- Resolved by returning `Option[Element]` for the optional per-item line and using `.getOrElse(emptyNode)` at render points.
- Related troubleshooting entry was added to `docs/troubleshooting.md`.

## Follow-up Items

- [ ] Consider adding a dedicated UI test harness for product builder component-level behavior (toggle and card layout assertions).
