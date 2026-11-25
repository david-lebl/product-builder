# 2026-11-24 — UI Visual & UX Modernization (Design Tokens, Dark Mode, Icons, Controls)

**PR:** N/A (branch `t3/optimise-ui-visual-ux`)
**Author:** agent (Claude)
**Type:** feature

## Summary

Modernized the Storefront and Staff Backoffice look and feel: a token-based theme with light / dark / system modes, an SVG icon set, reusable controls (segmented multi-buttons, icon buttons with count badges, avatars, theme toggle, toast, empty state), a redesigned storefront header and catalog, and a redesigned backoffice shell and dashboard. Also fixed two long-standing layout bugs (phantom form gaps; hamburger menu never visible on tablet/mobile).

## Changes Made

- **New** `modules/ui-core/.../theme/ThemeController.scala` — `ThemeMode` (System/Light/Dark) + shared reactive controller; DOM-free (hosts apply `data-theme` via callback).
- **New** `modules/ui-core/.../components/Icons.scala` — ~70 stroke icons (`kyo.Svg`, `currentColor`, Lucide geometry, ISC).
- **New** `modules/ui-core/.../components/Controls.scala` — `segmented`, `languageSwitch`, `themeToggle`, `themeSegmented`, `iconButton`, `avatar`, `countPill`, `toast`, `emptyState`.
- `AppTheme.scala` — `Palette` now resolves to CSS variables (`--mp-*`) with light/dark values; `themeStylesheet`, `motionStylesheet` (keyframes), Inter `fontStack`, `documentCss` (glass blur, color-scheme, tooltips, select chevron, reduced motion), new component classes (`mp-seg*`, `mp-btn-group`, `mp-icon-btn*`, `mp-count-badge`, `mp-avatar*`, `mp-logo-mark`, `mp-search*`, `mp-product-*`, `mp-chip`, `mp-empty*`, `mp-page-title`, `mp-main`, `mp-hide-sm`, `mp-nav-label`), `:focus-visible` rings, entry animations on drawer/modal/toast/menus.
- `AdminTheme.scala` — sticky sidebar, refined nav, KPI anatomy (`mp-kpi-icon`, `mp-kpi-value`), profile card, `mp-list-row`.
- `StorefrontApp`, `CatalogView` — icon nav, segmented currency/language, theme toggle, avatar account button, cart with count pill, mobile menu with theme selector; catalog hero feature chips, search with icon + localized placeholder, filter pills with counts, new product card anatomy, empty state.
- `BackofficeApp`, `DashboardView`, `AdminLoginView` — icon sidebar with count badges, breadcrumb topbar, icon actions, KPI tiles with tinted icons (pipeline row + business row), consistent order rows with avatars.
- Replaced unicode pseudo-icons (`✕ ⬇ 🔗 ▸ ▾ ←/→`) with icons in drawers, modals, DataTable, ActionMenu, artwork actions, order detail.
- `CommonComponents` — `groupIcon` now uses icon glyphs + per-group accent tokens; `paymentMethodLabel`; `pluralKey` (EN/CS plural rules).
- i18n: new catalog keys (plurals, results count, hero features); arrows removed from `adm-*` labels (icons used instead).
- `StorefrontState` / `BackofficeState` — optional shared `theme` param.
- Hosts (`modules/ui/Main.scala`, `modules/ui-admin/AdminMain.scala`, both `index.html`) — Inter font, pre-paint theme script, `documentCss` injection, theme persistence (`mpbuilder.theme`).

## Decisions & Rationale

- **CSS variables over parallel palettes**: every `Palette.x` stays a `Color`, so ~16k lines of views re-theme without changes.
- Theme attribute on `<html>`, set by browser glue, keeps `ui-core`/`ui-admin-core` free of `org.scalajs.dom`.
- Calculator widget gets tokens (light by default) but not `documentCss`, to avoid styling third-party host pages.
- Icon-only header nav below 1200px uses a visually-hidden label (not `display:none`) to keep accessible names.

## Issues Encountered

- `mp-field` had `flex-basis: 180px`, which became a 180px *height* in column containers (big gaps under inputs). See troubleshooting.
- `mp-mobile-only` was never shown on tablet/mobile because `Style.row` doesn't emit `display`. See troubleshooting.
- `<button>` flex columns centered shrink-wrapped children → option cards now use `align-items: stretch`.

## Follow-up Items

- [ ] Apply icons/empty states to remaining large views (Catalog/Pricing/Customer managers, Checkout, Account).
- [ ] Theme the standalone calculator demo page and consider an opt-in dark mode for embeds.
