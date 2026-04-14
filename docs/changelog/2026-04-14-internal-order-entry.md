# 2026-04-14 — Internal Order Entry (Compact Product Parameters View)

**PR:** copilot/add-compact-product-parameters-view-again
**Author:** Copilot agent
**Type:** feature

## Summary

Added a new "Internal Order Entry" view as an alternative to the standard Product Parameters page. The view is designed for experienced employees entering orders on behalf of customers. It presents the same configuration form in a compact, space-efficient layout with inline labels, hidden help/info elements, and a customer selector at the top.

## Changes Made

**New files:**
- `modules/ui/src/main/scala/mpbuilder/ui/internalorder/InternalOrderEntryApp.scala` — new compact view app
- `modules/ui/src/main/resources/internal-order.css` — CSS for compact inline-label layout

**Modified files:**
- `modules/ui/src/main/scala/mpbuilder/ui/productbuilder/ProductBuilderViewModel.scala`
  - Added `internalOrderCustomerId: Option[CustomerId]` to `BuilderState`
  - Added `private val allCustomersById: Map[CustomerId, Customer]` for O(1) lookup
  - Updated `currentCustomer` signal to resolve from `internalOrderCustomerId` when not logged in
  - Updated `validateConfiguration()` to use `allCustomersById` for customer pricelist
  - Added `setInternalOrderCustomer(id: Option[CustomerId])` method
- `modules/ui/src/main/scala/mpbuilder/ui/AppRouter.scala`
  - Added `AppRoute.InternalOrderEntry` case object
  - Added nav link "Internal Order Entry" / "Interní objednávka"
  - Added route handler → `InternalOrderEntryApp()`
  - Added `InternalOrderEntry` to basket button visibility
- `modules/ui/src/main/resources/index.html` — added `internal-order.css` link

## Decisions & Rationale

- **CSS-based compaction** — Rather than creating all-new components, the compact layout is applied via a `.compact-form` wrapper class that overrides `.form-group` to use horizontal flex layout, hides `.help-info-wrapper`, `.info-box`, and `.selector-help-buttons`. This avoids code duplication and keeps the existing components as the source of truth.

- **Shared ViewModel state** — `InternalOrderEntryApp` reuses `ProductBuilderViewModel` directly. Both views configure the same product state, meaning the regular "Product Parameters" view and the internal view stay in sync (same basket, same configuration). This is intentional for the current use case.

- **`internalOrderCustomerId` on `BuilderState`** — Stored in the same state object as the rest of the builder state so customer pricing is applied transparently by the existing `validateConfiguration()` and `autoRecalculate()` logic. `loginState` takes priority over `internalOrderCustomerId` so logged-in customers are not overridden.

- **`allCustomersById` Map** — A pre-computed `Map[CustomerId, Customer]` avoids O(n) linear scans on every signal update and in every price recalculation.

- **No new Artwork section in Internal Order Entry** — The artwork upload/editor section was omitted from the internal order form since employees typically handle artwork separately. The add-to-basket button is still present.

## Issues Encountered

None. Compiled first try, all 567 domain tests passed.

## Follow-up Items

- [ ] Consider whether `internalOrderCustomerId` should reset when navigating away from Internal Order Entry
- [ ] The two views currently share the same basket, which may or may not be desired behavior — could be separated in a future iteration
- [ ] Speed tier cards in compact mode could benefit from an even more compact single-row layout
