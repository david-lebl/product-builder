# Compact Builder View (Quick Entry)

An alternative product configuration view targeting experienced employees. Provides the same functionality as the standard Product Parameters view but with a denser, more efficient layout.

## Route & Navigation

- **Route:** `AppRoute.CompactBuilder`
- **Nav tab label:** "Quick Entry" / "Rychlé zadání"
- Accessible from the main navigation bar alongside Products, Product Parameters, etc.

## Key Differences from Standard View

| Aspect | Standard (Product Parameters) | Compact (Quick Entry) |
|--------|-------------------------------|----------------------|
| Label position | Above the field | Left of the field (inline) |
| HelpInfo (`?` / `i` buttons) | Visible | Hidden |
| Info boxes (notes, hints) | Visible | Hidden |
| Section headings | Full `h3` | Compact `h4` |
| Speed selector | Large tier cards | Compact pill-style radio buttons |
| Preset selector | Large cards with descriptions | Small pill-style buttons |
| Artwork labels | Full text | Abbreviated |
| Customer selection | Via OTP login flow | Direct dropdown (no OTP) |

## Customer Selector

At the top of the compact form, an employee can pick a customer from a dropdown. This:
- Shows only **active** customers
- Displays company name and tier in each option
- Applies customer-specific pricing immediately via `selectCustomerDirect()`
- Can be cleared to revert to list pricing
- Shows a brief info line with tier and global discount below the selector

Internally, this creates a `LoginSession` so the existing `currentCustomer` / `customerPricelist` signals work without modification.

## Layout

All form fields use the `.compact-row` pattern:
```
┌──────────────┬────────────────────────────┐
│  Label (110px) │  Field (flex: 1)           │
└──────────────┴────────────────────────────┘
```

On narrow screens (< 600px), fields stack vertically.

## Shared State

Both the standard and compact views share the same `ProductBuilderViewModel` and `BuilderState`. Switching between views preserves all configuration.

## Components

| Component | File |
|-----------|------|
| Main view | `CompactBuilderApp.scala` |
| Form coordinator | `CompactConfigurationForm.scala` |
| Category | `CompactCategorySelector.scala` |
| Presets | `CompactPresetSelector.scala` |
| Specifications | `CompactSpecificationForm.scala` |
| Printing method | `CompactPrintingMethodSelector.scala` |
| Material | `CompactMaterialSelector.scala` |
| Ink config | `CompactInkConfigSelector.scala` |
| Finishes | `CompactFinishSelector.scala` |
| CSS | `compact-builder.css` |
