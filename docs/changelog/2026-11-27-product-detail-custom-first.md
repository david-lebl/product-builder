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
