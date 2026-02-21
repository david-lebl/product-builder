# Pricing System

## Overview

The pricing layer computes a detailed price breakdown from a valid `ProductConfiguration`. It follows the same **rules-as-data** pattern used by the compatibility system — pricing rules are declarative data interpreted by a pure calculator, not scattered business logic.

The entire pricing pipeline is pure (no side effects, no ZIO). It takes a `ProductConfiguration` and a `Pricelist`, and returns either a `PriceBreakdown` or accumulated `PricingError`s via ZIO Prelude `Validation`.

## Core Concepts

### Money

All monetary values use the `Money` opaque type backed by `BigDecimal` — never `Double`. This avoids floating-point rounding errors that are unacceptable in financial calculations. Final totals are rounded to 2 decimal places using `HALF_UP` rounding.

```scala
val price = Money("0.12")       // from string — preferred for precision
val total = price * 500         // Money("60.00")
val rounded = total.rounded     // ensures 2 decimal places
```

### Pricelist

A `Pricelist` bundles a list of `PricingRule`s together with a `Currency` and a version string. This is the pricing equivalent of a `CompatibilityRuleset` — a self-contained, versionable pricing configuration.

```scala
Pricelist(
  rules = List(...),
  currency = Currency.USD,
  version = "1.0.0",
)
```

### Pricing Rules

There are 8 rule types, each a variant of the `PricingRule` sealed enum:

| Rule | Purpose | Example |
|------|---------|---------|
| `MaterialBasePrice` | Flat per-unit price for a material | Coated 300gsm = $0.12/unit |
| `MaterialAreaPrice` | Price per square meter (for large-format) | Vinyl = $18.00/m² |
| `FinishSurcharge` | Per-unit surcharge for a specific finish (by ID) | Matte lamination = $0.03/unit |
| `FinishTypeSurcharge` | Per-unit surcharge for a finish type | UV coating = $0.04/unit |
| `PrintingProcessSurcharge` | Per-unit surcharge for a printing process | Letterpress = $0.20/unit |
| `CategorySurcharge` | Per-unit surcharge for a product category | (e.g., packaging premium) |
| `QuantityTier` | Multiplier applied to the subtotal based on product quantity | 1000+ units = 0.80× |
| `SheetQuantityTier` | Multiplier applied to the subtotal based on total physical sheets | 250+ sheets = 0.80× |

## How Pricing Works

### Step-by-step Calculation

Given a valid `ProductConfiguration` and a `Pricelist`, the `PriceCalculator` performs these steps:

**1. Extract quantity** from the configuration's specifications. Fails with `NoQuantityInSpecifications` if absent.

**2. Resolve material unit price.** The calculator checks for an area-based rule first:
   - If a `MaterialAreaPrice` exists for this material, compute: `pricePerSqMeter × (width × height / 1,000,000)`. Fails with `NoSizeForAreaPricing` if no size spec is present.
   - Otherwise, fall back to `MaterialBasePrice`. Fails with `NoBasePriceForMaterial` if neither rule exists.

**3. Compute finish surcharges.** For each finish on the configuration:
   - Look for a `FinishSurcharge` matching the finish's ID (most specific).
   - If not found, look for a `FinishTypeSurcharge` matching the finish's type.
   - If neither exists, the finish is free (gracefully skipped).

**4. Find process surcharge.** If a `PrintingProcessSurcharge` matches the configuration's printing process type, add it.

**5. Find category surcharge.** If a `CategorySurcharge` matches the configuration's category ID, add it.

**6. Sum all line items** into a subtotal.

**7. Apply quantity tier.** Two tier mechanisms are checked in order:
   - **Sheet quantity tier:** If any component uses sheet-based pricing (`sheetsUsed > 0`) and `SheetQuantityTier` rules exist, the calculator sums `sheetsUsed` across all components and finds the best matching sheet tier. This gives discounts proportional to the actual press run (number of physical sheets), not the product quantity.
   - **Product quantity tier (fallback):** If no sheet tier applies (no sheet-priced components, or no `SheetQuantityTier` rules in the pricelist), the best matching `QuantityTier` is used based on product quantity. This preserves backward compatibility for pricelists that only define `QuantityTier` rules.
   - In both cases, the "best" tier is the one with the highest `minQuantity`/`minSheets` that is still ≤ the actual quantity/sheet count.

**8. Round the total** to 2 decimal places.

### Specificity / Precedence

The system uses a specificity model for finish pricing:

- **ID-level rules override type-level rules.** If both a `FinishSurcharge(finishId=X)` and a `FinishTypeSurcharge(finishType=Lamination)` exist, and finish X is a Lamination, the ID-level surcharge is used.
- This mirrors how CSS specificity works — more specific selectors win.

For quantity tiers, the **most specific matching tier wins** — the tier with the highest `minQuantity` that is ≤ the actual quantity.

### Worked Example: Business Cards

Configuration: 500× Coated Art Paper 300gsm + Matte Lamination + Offset Printing

```
Material: Coated Art Paper 300gsm    $0.12 × 500 =  $60.00
Finish: Matte Lamination             $0.03 × 500 =  $15.00
                                              ─────────────
Subtotal                                         =  $75.00
Quantity tier (250–999)                           ×    0.90
                                              ─────────────
Total                                            =  $67.50
```

### Worked Example: Banner (Area-Based)

Configuration: 10× Adhesive Vinyl (1000×500mm) + UV Coating + UV Curable Inkjet

```
Area per unit: 1000mm × 500mm = 0.5 m²
Material: Adhesive Vinyl       $18.00/m² × 0.5 = $9.00/unit
Material line:                  $9.00 × 10 =  $90.00
Finish: UV Coating              $0.04 × 10 =   $0.40
                                        ─────────────
Subtotal                                   =  $90.40
Quantity tier (1–249)                      ×    1.00
                                        ─────────────
Total                                     =  $90.40
```

## Output: PriceBreakdown

The calculator returns a `PriceBreakdown` containing:

| Field | Description |
|-------|-------------|
| `componentBreakdowns` | List of per-component breakdowns (see below) |
| `processSurcharge` | Optional printing process `LineItem` |
| `categorySurcharge` | Optional category `LineItem` |
| `subtotal` | Sum of all lines before tier multiplier |
| `quantityMultiplier` | The applied tier multiplier (1.0 = no discount) |
| `total` | Final price: subtotal × multiplier, rounded to 2dp |
| `currency` | Currency from the pricelist |

Each `ComponentBreakdown` contains:

| Field | Description |
|-------|-------------|
| `role` | Component role (Main, Cover, Body) |
| `materialLine` | Base material cost as a `LineItem` |
| `cuttingLine` | Optional cutting surcharge `LineItem` (sheet pricing only) |
| `inkConfigLine` | Optional ink configuration adjustment `LineItem` |
| `finishLines` | List of finish surcharge `LineItem`s (one per priced finish) |
| `sheetsUsed` | Number of physical sheets consumed (0 for non-sheet-priced components) |

Each `LineItem` contains a `label`, `unitPrice`, `quantity`, and `lineTotal`.

## Error Handling

The calculator returns `Validation[PricingError, PriceBreakdown]` — it fails fast on missing quantity or missing material price (these are prerequisites), and reports exactly what is missing:

| Error | When |
|-------|------|
| `NoQuantityInSpecifications` | No `QuantitySpec` in the configuration's specs |
| `NoBasePriceForMaterial(id)` | No `MaterialBasePrice`, `MaterialSheetPrice`, or `MaterialAreaPrice` for the material |
| `NoSizeForAreaPricing(id)` | A `MaterialAreaPrice` rule exists but no `SizeSpec` in specs |
| `NoSizeForSheetPricing(id)` | A `MaterialSheetPrice` rule exists but no `SizeSpec` in specs |

## Package Structure

```
mpbuilder.domain.pricing/
├── Money.scala          — Money opaque type, Currency enum, Price case class
├── PricingRule.scala     — 10-variant sealed enum (rules as data)
├── Pricelist.scala       — Container: rules + currency + version
├── PricingError.scala    — 4-variant error ADT with exhaustive messages
├── PriceBreakdown.scala  — LineItem + PriceBreakdown output types
└── PriceCalculator.scala — Pure interpreter: config + pricelist → breakdown

mpbuilder.domain.sample/
└── SamplePricelist.scala — Sample pricing data for all 5 materials
```

## Relationship to Compatibility Layer

The pricing layer sits **after** validation. The intended flow is:

```
ConfigurationRequest
  → ConfigurationBuilder.build()     // resolves IDs, validates compatibility
  → ProductConfiguration             // valid configuration
  → PriceCalculator.calculate()      // computes pricing
  → PriceBreakdown                   // detailed price output
```

Pricing assumes it receives a valid configuration. Compatibility rules (material-finish conflicts, weight requirements, etc.) are enforced upstream. The pricing layer only fails for pricing-specific reasons (missing price data or missing specs needed for price computation).
