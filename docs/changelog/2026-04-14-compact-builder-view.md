# 2026-04-14 — Compact Product Builder View

**PR:** Add compact product builder view for experienced employees
**Author:** agent (copilot)
**Type:** feature

## Summary

Created an alternative "Quick Entry" view for product parameters targeting experienced employees. The view has the exact same functionality as the existing Product Parameters view but with a more compact, inline layout. Labels are positioned to the left of input fields (not above), help/info elements are hidden, and a customer selector is placed at the top for employees to pick a customer for the order.

## Changes Made

### New Files
- `modules/ui/src/main/scala/mpbuilder/ui/productbuilder/CompactBuilderApp.scala` — Main compact view with customer selector and price/basket panels
- `modules/ui/src/main/scala/mpbuilder/ui/productbuilder/components/CompactConfigurationForm.scala` — Form layout coordinator
- `modules/ui/src/main/scala/mpbuilder/ui/productbuilder/components/CompactCategorySelector.scala` — Inline category selector
- `modules/ui/src/main/scala/mpbuilder/ui/productbuilder/components/CompactMaterialSelector.scala` — Inline material selector
- `modules/ui/src/main/scala/mpbuilder/ui/productbuilder/components/CompactInkConfigSelector.scala` — Inline ink config selector
- `modules/ui/src/main/scala/mpbuilder/ui/productbuilder/components/CompactPrintingMethodSelector.scala` — Inline printing method selector
- `modules/ui/src/main/scala/mpbuilder/ui/productbuilder/components/CompactFinishSelector.scala` — Compact finish selector with inline params
- `modules/ui/src/main/scala/mpbuilder/ui/productbuilder/components/CompactSpecificationForm.scala` — Compact specs (quantity, size, pages, orientation, fold, binding) and speed selector
- `modules/ui/src/main/scala/mpbuilder/ui/productbuilder/components/CompactPresetSelector.scala` — Pill-style preset buttons
- `modules/ui/src/main/resources/compact-builder.css` — Dedicated CSS for the compact view

### Modified Files
- `modules/ui/src/main/scala/mpbuilder/ui/AppRouter.scala` — Added `CompactBuilder` route, navigation tab, and basket visibility
- `modules/ui/src/main/scala/mpbuilder/ui/productbuilder/ProductBuilderViewModel.scala` — Added `selectCustomerDirect()` and `clearCustomerSelection()` methods
- `modules/ui/src/main/resources/index.html` — Added `compact-builder.css` link

## Decisions & Rationale

- **Separate compact components instead of a CSS-only approach**: Each compact selector is a separate Scala object rather than just CSS modifications to the existing components. This avoids coupling: the compact view can omit `HelpInfo` calls entirely rather than hiding them with CSS after rendering, and the inline `<select>` elements are rendered directly rather than going through the `FormGroup` wrapper which always puts labels above.
- **Direct customer selection (no OTP)**: Added `selectCustomerDirect()` to ViewModel as a shortcut for employees. This creates a `LoginSession` so that the existing `currentCustomer` / `customerPricelist` signals work seamlessly, reusing all customer pricing logic.
- **Pill-style speed selector instead of cards**: The original Product Parameters view uses large speed-tier cards. The compact view uses compact radio-pill buttons, which are more space-efficient for users who already know the speed tiers.
- **Shared ViewModel**: Both views share the same `ProductBuilderViewModel` and `BuilderState`, so switching between views preserves configuration state.

## Issues Encountered

- No issues encountered. Compilation and all 567 domain tests passed on first attempt.

## Follow-up Items

- [ ] Consider adding keyboard shortcuts for the compact view (e.g., Tab navigation between fields)
- [ ] Consider persisting view preference (compact vs standard) per employee
- [ ] The compact view could benefit from an "order template" feature where common configurations are saved and reloaded
