# CSS Architecture Refactoring Plan

## Executive Summary

The UI's `index.html` file serves as a nearly-empty SPA entry point, but currently contains a single `<style>` block of ~4,861 lines. This creates several compounding problems:

- **Discoverability**: Finding styles for any given component requires scrolling thousands of lines.
- **Duplication**: ~30–40% of CSS is repeated patterns — buttons, form inputs, flex layouts, colors — defined independently in multiple sections.
- **No ui-framework defaults**: The `uikit` components (`Tabs`, `Stepper`, `SplitTableView`, form fields) reference class names like `tab-button--active` and `split-table-row--selected`, but their styles are buried in the app's monolithic stylesheet. A consumer of `uikit` gets no default styling.
- **Hardcoded design tokens**: The primary color `#667eea` appears 100+ times. `#667eea`/`#764ba2` gradients, `#16a34a` success green, `#dc2626` error red — all hardcoded throughout. Any brand change requires a grep-and-replace.
- **No cascade control**: Styles from unrelated features can accidentally bleed into each other. There is no layer or scope separation.

---

## Current State Analysis

### File

`modules/ui/src/main/resources/index.html` — 4,875 lines total, CSS lines 7–4,868.

### CSS Custom Properties (Today)

Only 3 variables exist, all layout dimensions:

```css
* {
    --top-bar-height: 52px;
    --nav-bar-height: 42px;
    --header-total-height: calc(var(--top-bar-height) + var(--nav-bar-height));
}
```

No color tokens. No spacing scale. No typography scale.

### Hardcoded Color Palette

| Role | Value | Occurrences |
|---|---|---|
| Primary accent | `#667eea` | 100+ |
| Primary dark (gradient end) | `#764ba2` | 50+ |
| Success | `#16a34a` / `#22c55e` / `#4caf50` | ~20 (3 variants) |
| Error | `#dc2626` / `#ef4444` / `#ff5252` | ~15 (3 variants) |
| Border | `#e0e0e0` | ~20 |
| Surface | `#f1f5f9` | ~15 |
| Text dark | `#1e293b` | ~10 |

The success and error colors each have **three slightly different values** used interchangeably — an inconsistency that tokens would eliminate.

### Z-Index Stack

| Level | Value | Element |
|---|---|---|
| `--z-topbar` | 200 | Sticky top bar wrapper |
| `--z-basket-overlay` | 250 | Basket drawer backdrop |
| `--z-login-backdrop` | 299 | Login popup backdrop |
| `--z-drawer` | 300 | Login popup / basket drawer |
| Mobile price footer | 100 | Fixed bottom strip |
| Resize handles | 20 | Calendar builder |
| Rotate buttons | 30 | Calendar builder |

### Responsive Breakpoints

| Breakpoint | Usage |
|---|---|
| `768px` | Main layout column collapse, navigation |
| `600px` | Cards, basket items |
| `520px` | Checkout form rows |
| `480px` | Fine-grained mobile adjustments |

### Duplication Highlights

| Pattern | Copies | Issue |
|---|---|---|
| Button base styles | ~8 | `.button`, `.btn-secondary`, `.checkout-btn`, `stepper-btn` all define `border: none; cursor: pointer; border-radius: Xpx; transition: ...` independently |
| Form input base | ~5 | `.form-group input`, `.login-widget-popup input`, `.checkout-form-input` — near-identical rules |
| Tab active state | ~4 | `.active`, `.--active`, `.tab-button--active`, `.sidebar-tab-btn--active` — inconsistent naming |
| `display: flex; align-items: center; gap: Xpx;` | 20+ | Inline per-component instead of composable utilities |
| `transition: all 0.2s;` | 15+ | Should reference a variable |
| `box-shadow: 0 10px 30px rgba(0,0,0,0.2);` | ~8 | Identical card shadow repeated |

---

## Proposed File Structure

Split the monolithic stylesheet into 12 purposeful files:

```
modules/ui/src/main/resources/
├── index.html               (HTML shell only — no <style>)
├── tokens.css               ← NEW: design tokens (colors, spacing, z-index, etc.)
├── reset.css                ← NEW: global reset + base element styles
├── layout.css               ← NEW: top bar, nav bar, main grid, card primitives
├── utilities.css            ← NEW: .btn-*, .badge-*, .error-message, flex helpers
├── uikit.css                ← NEW: all ui-framework component classes
├── login.css                ← NEW: login widget, OTP input, language selector
├── pricing.css              ← NEW: price preview, breakdown, line items
├── basket.css               ← NEW: basket items, basket drawer, mobile price footer
├── checkout.css             ← NEW: multi-step checkout
├── calendar.css             ← NEW: calendar builder canvas + editor
├── manufacturing.css        ← NEW: dashboard, station queue, timeline, analytics
└── order-history.css        ← NEW: order table, status steps, item details

modules/ui-framework/src/main/resources/
└── uikit.css                ← Phase 3: copy of ui/uikit.css shipped with the framework
```

### File Responsibilities

#### `tokens.css`
The single source of truth for all design values. Must be loaded first.

```css
:root {
    /* Layout */
    --top-bar-height: 52px;
    --nav-bar-height: 42px;
    --header-total-height: calc(var(--top-bar-height) + var(--nav-bar-height));

    /* Z-index scale */
    --z-base: 1;
    --z-handles: 20;
    --z-rotate: 30;
    --z-mobile-footer: 100;
    --z-topbar: 200;
    --z-basket-overlay: 250;
    --z-login-backdrop: 299;
    --z-drawer: 300;

    /* Colors */
    --color-primary: #667eea;
    --color-primary-dark: #764ba2;
    --color-primary-gradient: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    --color-success: #16a34a;
    --color-success-light: #dcfce7;
    --color-error: #dc2626;
    --color-error-light: #fee2e2;
    --color-warning: #d97706;
    --color-warning-light: #fef3c7;
    --color-info: #2563eb;
    --color-info-light: #dbeafe;
    --color-border: #e0e0e0;
    --color-surface: #f1f5f9;
    --color-text: #1e293b;
    --color-text-muted: #64748b;
    --color-white: #ffffff;

    /* Spacing */
    --space-xs: 4px;
    --space-sm: 8px;
    --space-md: 16px;
    --space-lg: 24px;
    --space-xl: 32px;

    /* Borders */
    --border-radius-sm: 4px;
    --border-radius-md: 8px;
    --border-radius-lg: 12px;

    /* Shadows */
    --shadow-card: 0 10px 30px rgba(0, 0, 0, 0.2);
    --shadow-sm: 0 2px 4px rgba(0, 0, 0, 0.06);

    /* Transitions */
    --transition: all 0.2s ease;
    --transition-fast: all 0.15s ease;

    /* Typography */
    --font-sans: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
    --font-size-sm: 0.75rem;
    --font-size-base: 0.875rem;
    --font-size-md: 1rem;
    --font-size-lg: 1.125rem;
}
```

#### `reset.css`
Global resets and base element defaults (currently ~30 lines at the top of the style block). No classes — element selectors only.

#### `layout.css`
Structural rules for the sticky top bar, nav bar, main two-column grid, and card container. These are the outermost wrappers rendered by `AppRouter.scala`.

Key classes: `.top-bar-wrapper`, `.nav-bar`, `.main-content`, `.main-layout`, `.card`.

#### `utilities.css`
Shared visual primitives used across multiple features. These are the patterns that are currently duplicated.

Key classes:
- **Buttons**: `.btn` (base), `.btn-primary`, `.btn-secondary`, `.btn-danger`, `.btn-success`, `.btn-warning`, `.btn-sm`
- **Badges**: `.badge`, `.badge-pending`, `.badge-completed`, `.badge-error`, `.badge-warning`, `.badge-info`
- **Alerts**: `.error-message`, `.success-message`, `.info-box`
- **Form base**: `.form-group`, `.form-group input`, `.form-group label`

#### `uikit.css`
All styles for classes emitted by the `uikit` framework components. This file has no domain knowledge — only generic component structure. It should eventually be co-located with the framework source.

Key class groups:
- `tabs`, `tabs-bar`, `tab-button`, `tab-button--active`, `tabs-content`
- `stepper`, `stepper-bar`, `stepper-step`, `stepper-step--active`, `stepper-step--done`, `stepper-step-num`, `stepper-step-label`, `stepper-content`, `stepper-nav`, `stepper-btn`
- `split-table-view`, `split-table-header`, `split-table-search`, `split-table-filters`, `filter-chip`, `filter-chip--active`, `split-table-content`, `split-table-table-wrapper`, `split-table`, `split-table-th`, `split-table-th--sortable`, `split-table-row`, `split-table-row--selected`, `split-table-td`, `split-table-empty`, `split-table-side-panel`
- `form-derived`, `field-error`, `checkbox-label`, `checkbox-set`, `radio-label`, `radio-group`
- `validation-display`

#### Feature files (`login.css`, `pricing.css`, `basket.css`, `checkout.css`, `calendar.css`, `manufacturing.css`, `order-history.css`)
One file per application section. Styles in these files should only reference classes that appear in that section's Scala source files. Cross-cutting classes (buttons, badges) stay in `utilities.css` — feature files only contain layout and component-specific overrides.

---

## Migration Strategy

A phased approach allows incremental delivery with no visual regressions at each step.

### Phase 0 — Extract As-Is (No Selector Changes)

**Goal**: Split the file without modifying any selectors. Zero visual change.

1. Replace the `<style>` block in `index.html` with `<link>` tags.
2. Cut sections from the existing CSS directly into new files based on the ASCII-commented section headers that already exist.
3. Load order in `index.html`:
   ```html
   <link rel="stylesheet" href="tokens.css">
   <link rel="stylesheet" href="reset.css">
   <link rel="stylesheet" href="layout.css">
   <link rel="stylesheet" href="utilities.css">
   <link rel="stylesheet" href="uikit.css">
   <link rel="stylesheet" href="login.css">
   <link rel="stylesheet" href="pricing.css">
   <link rel="stylesheet" href="basket.css">
   <link rel="stylesheet" href="checkout.css">
   <link rel="stylesheet" href="calendar.css">
   <link rel="stylesheet" href="manufacturing.css">
   <link rel="stylesheet" href="order-history.css">
   ```
4. **Verify**: `sbt ui/fastLinkJS`, open all pages in browser, confirm no visual regressions.

### Phase 1 — Introduce Design Tokens

**Goal**: Replace all hardcoded color, shadow, and transition values with CSS variables from `tokens.css`.

Process per file:
1. Search for `#667eea` → replace with `var(--color-primary)`.
2. Search for `#764ba2` → replace with `var(--color-primary-dark)`.
3. Replace gradient strings → `var(--color-primary-gradient)`.
4. Normalise success/error variants to single canonical tokens.
5. Replace `0.2s` transition literals → `var(--transition)`.
6. Replace repeated shadow literals → `var(--shadow-card)`.

**Verify**: All pages look identical. Colors unchanged because tokens resolve to the same values.

### Phase 2 — Consolidate Duplicated Rules

**Goal**: Remove the ~30–40% duplication by unifying buttons, form inputs, and active-state patterns.

Key consolidations:

1. **Button unification**: Define one `.btn` base (padding, border, cursor, radius, transition) in `utilities.css`. Remove the ~7 redundant independent button rule blocks from feature files. Feature files can still have `.checkout-btn` but it should `extend` or supplement, not redefine.

2. **Form input base**: Single rule for `input[type=text], input[type=email], select, textarea` base styling. Feature files override only what differs (e.g., width, margin).

3. **Tab/active naming**: Standardise all `--active` modifier classes. Remove sidebar-specific duplicates if classes can be shared.

4. **Flex utilities**: Consider adding `.flex-row`, `.flex-center`, `.gap-sm/md/lg` utility classes rather than re-declaring inline flex properties in every component.

**Verify**: Run `sbt ui/fastLinkJS`, visually inspect all views.

### Phase 3 — Ship uikit Default Styles with the Framework

**Goal**: Allow any future consumer of `ui-framework` to get default styling out of the box.

1. Copy `uikit.css` to `modules/ui-framework/src/main/resources/uikit.css`.
2. Update the `ui-framework` sbt resource config to include the file in its jar.
3. The `ui` module continues to load it via `<link>` (or references the jar resource), but the file is now the framework's responsibility.
4. Document in the `ui-framework` README: "Default styles are in `uikit.css`. Import it before your application styles."

---

## CSS Layers (Optional Enhancement)

Once the files are split, add `@layer` declarations to make cascade precedence explicit and prevent accidental bleed-through:

```css
/* tokens.css */
@layer tokens, reset, uikit, features, utilities;

@layer tokens { :root { ... } }
```

```css
/* each other file */
@layer uikit { .tabs { ... } }
@layer features { .manufacturing-dashboard { ... } }
@layer utilities { .btn { ... } }
```

Utilities win over features, features win over uikit, avoiding specificity hacks.

---

## Other Observations

- **Calendar builder** CSS (~550 lines) is the largest single feature block. It could be loaded lazily via a `<link rel="preload">` since the calendar editor is a separate, opt-in route.
- **Font stack**: System fonts are already used. No action needed.
- **No bundler for CSS**: The simplest build strategy is multiple `<link>` tags. HTTP/2 multiplexing makes this cost-free. A future step could concatenate via sbt resource generator if desired.
- **No PostCSS / SCSS**: Adding a preprocessor is out of scope. CSS custom properties cover the token use case natively.

---

## Verification Checklist

After each phase:

- [ ] `sbt ui/fastLinkJS` completes without error
- [ ] Top bar, navigation, and login widget render correctly
- [ ] Product configuration form (selectors, pricing preview) renders correctly
- [ ] Basket drawer and mobile price footer render correctly
- [ ] Checkout flow (all steps) renders correctly
- [ ] Calendar builder canvas and element editor render correctly
- [ ] Manufacturing dashboard, station queue, and order approval render correctly
- [ ] Order history table and status timeline render correctly
- [ ] Responsive layout at 768px breakpoint renders correctly
- [ ] No orphaned selectors (use browser DevTools "unused CSS" audit)
