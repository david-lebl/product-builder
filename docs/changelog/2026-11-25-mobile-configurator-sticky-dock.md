# 2026-11-25 — Mobile Configurator Sticky Price Dock

**PR:** N/A (branch `t3/mobile-configurator-sticky-bar`)
**Author:** Claude (agent)
**Type:** feature

## Summary

On mobile and tablet (≤ 960px) the configurator's live price and add-to-cart button now stay pinned to the bottom of the screen. The dock expands into a sheet with the full price detail and a configuration recap. Adding to cart gives button feedback and hands off to the cart, which opens as a bottom sheet on phones.

## Changes Made

- `ConfiguratorView.scala` — split `renderQuoteSidebar` into reusable parts (`renderPriceBlock`, `renderSavingsAndBadges`, `renderQuoteFootnotes`, `renderCtas`, `addToCartButton`); added `renderDock` / `renderDockPrice` / `renderDockSheet` / `renderConfigRecap`. `ConfiguratorHost.Storefront.onAddToCart` now returns `Boolean < Async`.
- `ConfiguratorState.scala` — new `CartFeedback` enum (Idle / Adding / Added); the controller gains `dockExpanded`, `cartFeedback`, `toggleDock`, `collapseDock` and `runAddToCart` (debounces double taps; on success collapses the dock and resets "Added" after `AddedFeedback` = 1.8s).
- `AppState.addDraftToCart` returns whether the add succeeded (`addPresetToCart` still returns `Unit`).
- `CartDrawer.scala` — the backdrop gets the `mp-cart-backdrop` class and closes the drawer when tapped; the drawer has `role=dialog`.
- `AppTheme.scala` — `mp-cfg-dock*` rules (hidden on desktop; sticky floating card ≤ 960px with backdrop when expanded), `mp-btn-success`, `mp-slide-up` keyframe, cart drawer as a bottom sheet ≤ 640px, plus document CSS for safe-area and tap highlight.
- `Icons.scala` — `ChevronUp`.
- `Translations.scala` — 12 new EN + CS keys (`cfg-btn-adding`, `cfg-btn-added`, `cfg-dock-*`).
- `docs/ui-guide.md` — documented the dock and the basket bottom sheet.

## Decisions & Rationale

- **`position: sticky` inside the configurator, not `fixed`.** The same view is embedded by the standalone calculator widget in third-party pages, and a sticky dock stays inside the widget. In the storefront it rests above the footer at the end of the page.
- **The desktop sidebar is hidden ≤ 960px.** Its full content moved into the expandable sheet, so the long form doesn't end with a duplicated summary.
- **Bottom-sheet cart on phones** continues the motion from the dock (from the same edge) when adding to the basket. The style is scoped to `mp-cart-backdrop`, so the artwork preview modal (which also uses `mp-backdrop`) is unaffected.
- **"Added ✓" only on success** — hence the `Boolean` result from `onAddToCart`.

## Issues Encountered

- Mill picked up JDK 17 → `UnsupportedClassVersionError` (already in troubleshooting.md; use JDK 25).
- Scala 3 indentation error from a multi-line `if … then` in a for-generator; fixed by extracting a method (`onAdded`).

## Follow-up Items

- [ ] Add `viewport-fit=cover` to `index.html` if the iOS home-indicator inset should apply (the CSS already uses `env(safe-area-inset-bottom)`).
- [ ] Consider a swipe-down gesture to collapse the sheet (needs browser glue in `modules/ui`).
