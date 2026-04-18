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

There are 17 rule types, each a variant of the `PricingRule` sealed enum:

| Rule | Purpose | Example |
|------|---------|---------|
| `MaterialBasePrice` | Flat per-unit price for a material | Coated 300gsm = $0.12/unit |
| `MaterialAreaPrice` | Price per square meter (for large-format) | Vinyl = $18.00/m² |
| `MaterialSheetPrice` | Price per physical press sheet | Coated 135gsm = 8 CZK/sheet |
| `FinishSurcharge` | Per-unit surcharge for a specific finish (by ID) | Matte lamination = $0.03/unit |
| `FinishTypeSurcharge` | Per-unit surcharge for a finish type | All lamination = $0.04/unit |
| `PrintingProcessSurcharge` | Per-unit surcharge for a printing process | Letterpress = $0.20/unit |
| `CategorySurcharge` | Per-unit surcharge for a product category | Packaging premium |
| `FoldTypeSurcharge` | Per-unit surcharge for a fold type | Tri-fold = $0.02/unit |
| `BindingMethodSurcharge` | Per-unit surcharge for a binding method | Saddle stitch = $0.05/unit |
| `QuantityTier` | Multiplier on subtotal based on product quantity | 1000+ units = 0.80× |
| `SheetQuantityTier` | Multiplier on subtotal based on total physical sheets | 250+ sheets = 0.80× |
| `InkConfigurationSurcharge` | Fixed per-unit surcharge by ink color counts, added on top of material cost | 4/4 CMYK = +$0.05/unit |
| `CuttingSurcharge` | Per-cut surcharge for sheet-priced materials | 8 CZK/cut |
| `FinishSetupFee` | One-time setup fee for a specific finish (by ID) | Matte lam setup = 50 CZK |
| `FinishTypeSetupFee` | One-time setup fee for a finish type | Any lamination setup = 50 CZK |
| `FoldTypeSetupFee` | One-time setup fee for a fold type | Tri-fold setup = 80 CZK |
| `BindingMethodSetupFee` | One-time setup fee for a binding method | Saddle stitch setup = 150 CZK |
| `MinimumOrderPrice` | Price floor applied after all other calculations | Minimum 500 CZK |

**Per-unit surcharges** (material, finish, fold, binding, process, category) are included in the subtotal and are subject to the quantity tier discount.

**Setup fees** are added _after_ the discount multiplier — they represent real machine setup costs that don't scale with volume, so they are intentionally never discounted.

**Minimum order price** is applied last as a safety net; it raises the total to the floor only if the computed total falls below it.

## How Pricing Works

### Step-by-step Calculation

Given a valid `ProductConfiguration` and a `Pricelist`, the `PriceCalculator` performs these steps:

**1. Extract quantity** from the configuration's specifications. Fails with `NoQuantityInSpecifications` if absent.

**2. Resolve material unit price.** The calculator checks for an area-based rule first:
   - If a `MaterialAreaPrice` exists for this material, compute: `pricePerSqMeter × (width × height / 1,000,000)`. Fails with `NoSizeForAreaPricing` if no size spec is present.
   - If a `MaterialSheetPrice` exists, compute the number of physical sheets needed and the total sheet cost. Fails with `NoSizeForSheetPricing` if no size spec is present.
   - Otherwise, fall back to `MaterialBasePrice`. Fails with `NoBasePriceForMaterial` if no rule exists.

**3. Apply ink configuration surcharge.** If an `InkConfigurationSurcharge` matches the front/back color counts, a fixed per-unit surcharge is added as a separate line item on top of the material cost. The surcharge is completely independent of material price. No rule entry (e.g., `noInk`) means no ink line item.

**4. Compute finish surcharges.** For each finish on the configuration:
   - Look for a `FinishSurcharge` matching the finish's ID (most specific).
   - If not found, look for a `FinishTypeSurcharge` matching the finish's type.
   - If neither exists, the finish is free (gracefully skipped).

**5. Find process surcharge.** If a `PrintingProcessSurcharge` matches the configuration's printing process type, add it.

**6. Find category surcharge.** If a `CategorySurcharge` matches the configuration's category ID, add it.

**7. Find fold type surcharge.** If a `FoldTypeSurcharge` matches the configuration's fold type spec, add it as a per-unit line.

**8. Find binding method surcharge.** If a `BindingMethodSurcharge` matches the configuration's binding method spec, add it as a per-unit line.

**9. Sum all per-unit line items** into a subtotal.

**10. Apply quantity tier.** Two tier mechanisms are checked in order:
   - **Sheet quantity tier:** If any component uses sheet-based pricing (`sheetsUsed > 0`) and `SheetQuantityTier` rules exist, the calculator sums `sheetsUsed` across all components and finds the best matching sheet tier. This gives discounts proportional to the actual press run, not the product quantity.
   - **Product quantity tier (fallback):** If no sheet tier applies, the best matching `QuantityTier` is used based on product quantity.
   - In both cases, the "best" tier is the one with the highest `minQuantity`/`minSheets` that is still ≤ the actual quantity/sheet count.

**11. Collect setup fees.** One-time fees are gathered for each unique finish, fold type, and binding method in the configuration:
   - For finishes: `FinishSetupFee` (by ID) takes precedence over `FinishTypeSetupFee` (by type). If the same finish ID appears on multiple components (e.g., lamination on Cover and Body), the fee is charged only once.
   - For fold type: `FoldTypeSetupFee` matches the configuration's fold type spec.
   - For binding method: `BindingMethodSetupFee` matches the configuration's binding method spec.
   - Setup fees are added to the discounted subtotal — they are **not** reduced by the quantity multiplier.

**12. Apply minimum order price.** If a `MinimumOrderPrice` rule exists and `discountedSubtotal + setupFees < minimum`, the total is raised to the minimum. `minimumApplied` is set to the pre-floor amount so the UI can display the indicator.

**13. Round the total** to 2 decimal places.

### Specificity / Precedence

- **ID-level rules override type-level rules** for both surcharges and setup fees. If both a `FinishSurcharge(finishId=X)` and a `FinishTypeSurcharge(finishType=Lamination)` exist and finish X is a Lamination, the ID-level surcharge is used. Same applies to `FinishSetupFee` vs `FinishTypeSetupFee`.
- For quantity tiers, the **most specific matching tier wins** — the tier with the highest `minQuantity` that is ≤ the actual quantity.

### Worked Example: Business Cards

Configuration: 500× Coated Art Paper 300gsm + Matte Lamination + Offset Printing + 4/4 CMYK (USD pricelist)

```
Material: Coated Art Paper 300gsm    $0.12 × 500 =  $60.00
Ink: 4/4 CMYK surcharge             $0.05 × 500 =  $25.00
Finish: Matte Lamination             $0.03 × 500 =  $15.00
                                              ─────────────
Subtotal                                         = $100.00
Quantity tier (250–999)                          ×    0.90
                                              ─────────────
Total                                            =  $90.00
```

### Worked Example: Tri-fold Brochure with Setup Fee

Configuration: 100× Coated Art Paper 135gsm + Matte Lamination + Tri-fold (CZK sheet pricelist)

```
Material: sheet-based cost           say 20.00 CZK × 100 = 2,000.00 CZK
Finish: Matte Lamination surcharge   2.00 CZK × 100 =   200.00 CZK
Fold: Tri-fold surcharge             1.50 CZK × 100 =   150.00 CZK
                                                    ─────────────
Subtotal                                            = 2,350.00 CZK
Sheet tier multiplier                               ×      0.90
                                                    ─────────────
Discounted subtotal                                 = 2,115.00 CZK
Setup: Matte Lamination (one-time)                  +    50.00 CZK
Setup: Tri-fold (one-time)                          +    80.00 CZK
                                                    ─────────────
Total                                               = 2,245.00 CZK
```

### Worked Example: Banner (Area-Based)

Configuration: 10× Adhesive Vinyl (1000×500mm) + UV Coating + UV Curable Inkjet + 4/4 CMYK (USD pricelist)

```
Area per unit: 1000mm × 500mm = 0.5 m²
Material: Adhesive Vinyl       $18.00/m² × 0.5 = $9.00/unit
Material line:                   $9.00 × 10 =  $90.00
Ink: 4/4 CMYK surcharge         $0.05 × 10 =   $0.50
Finish: UV Coating               $0.04 × 10 =   $0.40
                                         ─────────────
Subtotal                                    =  $90.90
Quantity tier (1–249)                       ×    1.00
                                         ─────────────
Total                                      =  $90.90
```

## Output: PriceBreakdown

The calculator returns a `PriceBreakdown` containing:

| Field | Description |
|-------|-------------|
| `componentBreakdowns` | List of per-component breakdowns (see below) |
| `processSurcharge` | Optional printing process `LineItem` |
| `categorySurcharge` | Optional category `LineItem` |
| `foldSurcharge` | Optional fold type `LineItem` |
| `bindingSurcharge` | Optional binding method `LineItem` |
| `subtotal` | Sum of all per-unit lines before tier multiplier |
| `quantityMultiplier` | The applied tier multiplier (1.0 = no discount) |
| `setupFees` | List of one-time setup fee `LineItem`s (added after discount) |
| `minimumApplied` | `Some(billable)` when the price floor was triggered, `None` otherwise |
| `total` | Final price after all steps, rounded to 2dp |
| `currency` | Currency from the pricelist |
| `quantity` | Number of items ordered (used to compute per-item price) |

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
├── PricingRule.scala    — 17-variant sealed enum (rules as data)
├── Pricelist.scala      — Container: rules + currency + version
├── PricingError.scala   — 4-variant error ADT with exhaustive messages
├── PriceBreakdown.scala — LineItem + ComponentBreakdown + PriceBreakdown output types
└── PriceCalculator.scala — Pure interpreter: config + pricelist → breakdown

mpbuilder.domain.sample/
└── SamplePricelist.scala — Sample pricing data (USD + CZK base + CZK sheet pricelists)
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
