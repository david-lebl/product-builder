# UI Design System

The Storefront, Backoffice and Calculator share one typed design system in `modules/ui-core` (`AppTheme`, `Icons`, `Controls`), extended by `AdminTheme`.

## Theming

- Colors are design tokens: `AppTheme.Palette.*` resolve to CSS variables `--mp-<name>`, each with a light and dark value.
- Modes: **System** (follows OS `prefers-color-scheme`), **Light**, **Dark**. The active mode is `data-theme="system|light|dark"` on `<html>`.
- `ThemeController` (shared by Storefront and Backoffice) holds the preference; browser hosts persist it in `localStorage["mpbuilder.theme"]` and apply it before first paint.
- Users switch via the header theme button (cycles System → Light → Dark) or the three-way selector in the mobile menu.
- The embeddable calculator renders in light theme by default.

## Typography & Motion

- Inter (loaded by host pages) with system-UI fallback; tabular numerals for tables and KPIs.
- Short entry animations for drawers, modals, menus and toasts; disabled under `prefers-reduced-motion`.
- Keyboard focus is shown with `:focus-visible` rings on buttons, pills, tabs, segments and cards.

## Components

| Component | API / class | Use |
|---|---|---|
| Icons | `Icons(Icon.X, size)` | Stroke icons inheriting text color |
| Segmented control | `Controls.segmented` (`mp-seg`) | Single-select multi-button (currency, language, theme) |
| Button group | `mp-btn-group` | Joined `mp-btn` buttons |
| Icon button | `Controls.iconButton` (`mp-icon-btn`) | Icon-only actions with optional count badge |
| Avatar | `Controls.avatar` | Initials avatar for staff/customers |
| Toast | `Controls.toast` | Status messages with dismiss |
| Empty state | `Controls.emptyState` | No-results / empty lists |
| Tooltip | `data-tooltip="…"` | Hover/focus hint |
| Combo box | `ComboBox.render` + `ComboBox.Handle` (`mp-combo-*`) | Searchable rich single-select: grouped options, match highlighting, keyboard nav, result count (configurator material picker) |

## Responsive Behavior

- ≤ 1200px: storefront header nav becomes icon-only (labels kept for screen readers).
- ≤ 960px: desktop nav hidden, hamburger menu shown; backoffice sidebar replaced by a slide-down nav.
- ≤ 640px: single-column cards, full-width drawer and toasts.
