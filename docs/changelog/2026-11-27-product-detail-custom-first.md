# 2026-11-27 — Product Detail: Custom Configuration First

**PR:** N/A
**Author:** Claude Code
**Type:** feature

## Summary

The storefront product detail buy box used to lead with "Add to Cart" / "Quick Order" for a preset, with
the configurator and Design Studio as secondary links. The platform is about custom products and design, so
the hierarchy is flipped: the buy box now leads with the two creation paths, and presets become a secondary
"quick start" for newcomers (still one-click orderable, e.g. business cards).

## Changes Made

- **`modules/ui-core/.../storefront/ProductDetailView.scala`**
  - Buy box order: title → "From <cheapest preset price>" teaser → **Make it your own** (large
    "Configure your own" card → configurator pre-filled with the selected preset, "Design it online" card →
    Design Studio, plus a "Start blank instead" link) → **Quick start: ready-made presets** panel (preset
    radio list, price box, secondary Add to Cart / Quick Order).
- **`modules/ui-core/.../theme/AppTheme.scala`** — new `mp-pd-path*`, `mp-pd-link`, `mp-pd-quick` rules;
  removed unused `mp-pd-studio-btn`; preset price shrunk (26px / 24px mobile).
- **`modules/ui-core/.../i18n/Translations.scala`** — new EN/CS `detail-*` keys for the paths, from-price
  and quick start; reworded `detail-presets-subtitle` / `detail-how-1`; dropped unused `detail-use-preset`.
- **`StorefrontUiSpec`** — product detail test asserts the custom paths render before the quick-start panel,
  in EN and CS without missing keys.

## Follow-up: quick-start quantity (PR review)

- **`StorefrontState`** — new `presetQuantities` (per preset, reset on entering a product detail page) and
  `setPresetQuantity`, which re-prices only that preset via `pricePreset(specsOverride = …)` and drops
  out-of-order responses. `refreshPresetQuotes` (language / currency / catalog refresh) keeps the picked
  quantities; `addPresetToCart` / `startPresetQuickOrder` order the picked quantity.
- **`ProductDetailView`** — quantity picker in the quick-start price box: six log-scale pills around the
  preset default, a custom number field (committed on blur / Enter), and "Reset to preset". Per-piece price and
  preset summaries follow the picked quantity.
- **`StorefrontUiSpec`** — new test: a changed quantity re-prices the preset, renders, and lands in the cart.
