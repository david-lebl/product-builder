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

There are 23 rule types, each a variant of the `PricingRule` sealed enum:

| Rule | Purpose | Example |
|------|---------|---------|
| `MaterialBasePrice` | Flat per-unit price for a material | Coated 300gsm = $0.08/unit |
| `MaterialAreaPrice` | Flat price per square meter (for large-format) | Vinyl = $16.20/m² |
| `MaterialAreaTier` | Area-tiered price per square meter — picks highest matching tier | PVC 510g: 555/455/405/355 CZK/m² |
| `MaterialSheetPrice` | Price per physical press sheet | Coated 90gsm = 4 CZK/sheet |
| `FinishSurcharge` | Per-unit surcharge for a specific finish (by ID) | Matte lamination = $0.03/unit |
| `FinishTypeSurcharge` | Per-unit surcharge for a finish type | All lamination = $0.04/unit |
| `GrommetSpacingAreaPrice` | Area-based grommet price keyed by grommet spacing | 40 CZK/m² at 500mm spacing |
| `FinishLinearMeterPrice` | Price per linear metre for rope/accessory finishes | Gum rope = 18 CZK/m |
| `ScoringCountSurcharge` | Per-unit surcharge for creasing, keyed on crease count (discountable) | 2 creases = 1.00 CZK/unit |
| `ScoringSetupFee` | One-time flat setup fee for any Scoring finish (not discounted; overrides `FinishTypeSetupFee` for Scoring) | 60 CZK |
| `PrintingProcessSurcharge` | Per-unit surcharge for a printing process | Letterpress = $0.20/unit |
| `CategorySurcharge` | Per-unit surcharge for a product category | Packaging premium |
| `FoldTypeSurcharge` | Per-unit surcharge for a fold type | Tri-fold = $0.02/unit |
| `BindingMethodSurcharge` | Per-unit surcharge for a binding method | Saddle stitch = $0.05/unit |
| `QuantityTier` | Multiplier on subtotal based on product quantity | 1000+ units = 0.80× |
| `SheetQuantityTier` | Multiplier on subtotal based on total physical sheets | 250+ sheets = 0.80× |
| `InkConfigurationSheetPrice` | Additive per-sheet (or per-unit) ink cost keyed by printing method and front/back color counts | Offset 4/4 = 4 CZK/sheet |
| `InkConfigurationAreaPrice` | Additive per-m² ink cost keyed by printing method and front/back color counts | UV Inkjet 4/4 = 45 CZK/m² |
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

**2. Resolve material unit price.** The calculator checks for pricing rules in priority order:
   - **`MaterialAreaTier`** (highest priority): if a tiered area rule exists for this material, compute `pricePerSqMeter × area_m²` using the tier with the highest `minSqm ≤ area`. Fails with `NoSizeForAreaPricing` if no size spec is present.
   - **`MaterialAreaPrice`**: if a flat area rule exists, compute `pricePerSqMeter × area_m²`. Fails with `NoSizeForAreaPricing` if no size spec is present.
   - **`MaterialSheetPrice`**: compute the number of physical sheets needed and the total sheet cost. Fails with `NoSizeForSheetPricing` if no size spec is present.
   - **`MaterialBasePrice`** (fallback): flat per-unit price. Fails with `NoBasePriceForMaterial` if no rule exists.

**3. Apply ink configuration pricing.** If an `InkConfigurationSheetPrice` or `InkConfigurationAreaPrice` rule matches the front/back color counts and printing method ID, an additive ink cost line is added:
   - **`InkConfigurationSheetPrice`**: used for sheet-priced materials (`count = sheetsUsed`) and base-priced materials (`count = effectiveQuantity`). Line = `pricePerSheet × count`.
   - **`InkConfigurationAreaPrice`**: used for area-priced materials. Line = `(pricePerSqM × areaSqM) × effectiveQuantity`.
   - If no matching rule exists, no ink line is added (ink cost is assumed to be included elsewhere).

**4. Compute finish surcharges.** For each finish on the configuration, four pricing mechanisms are tried in order:
   - **`ScoringCountSurcharge`** (highest priority for Scoring): if the finish has `ScoringParams(creaseCount)`, find the rule matching that exact `creaseCount`. Fails with `MissingScoringPrice(creaseCount)` if no matching rule exists — silent zero-pricing is never allowed for parameterized scoring.
   - **`GrommetSpacingAreaPrice`**: if the finish has `GrommetParams`, find the tier with the highest `spacingMm ≤ selected spacing` and compute `pricePerSqMeter × area_m²`. The line item label shows the approximate grommet count (`2·(w+h)/spacing + 4 corners`).
   - **`FinishLinearMeterPrice`**: if the finish has `RopeParams`, compute `pricePerMeter × lengthMeters`.
   - **`FinishSurcharge` / `FinishTypeSurcharge`** (fallback): ID-level surcharge takes precedence over type-level surcharge. A plain Scoring finish with no `ScoringParams` is also priced here for backward compatibility. The surcharge amount is interpreted relative to the material's pricing basis:
     - **Base-priced materials**: `surcharge × quantity` (per finished item)
     - **Sheet-priced materials**: `surcharge × sheetsUsed` (per press sheet)
     - **Area-priced materials**: `(surcharge × areaSqM) × effectiveQuantity` (per m², scaled by size)
   - If none of the above apply, the finish is free (gracefully skipped).

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
   - For the Scoring finish specifically: `ScoringSetupFee` fires once if any Scoring finish is present, and suppresses `FinishTypeSetupFee(Scoring)` — the dedicated rule takes precedence.
   - For fold type: `FoldTypeSetupFee` matches the configuration's fold type spec.
   - For binding method: `BindingMethodSetupFee` matches the configuration's binding method spec.
   - Setup fees are added to the discounted subtotal — they are **not** reduced by the quantity multiplier.

**12. Apply minimum order price.** If a `MinimumOrderPrice` rule exists and `discountedSubtotal + setupFees < minimum`, the total is raised to the minimum. `minimumApplied` is set to the pre-floor amount so the UI can display the indicator.

**13. Round the total** to 2 decimal places.

### Specificity / Precedence

- **ID-level rules override type-level rules** for both surcharges and setup fees. If both a `FinishSurcharge(finishId=X)` and a `FinishTypeSurcharge(finishType=Lamination)` exist and finish X is a Lamination, the ID-level surcharge is used. Same applies to `FinishSetupFee` vs `FinishTypeSetupFee`.
- **`ScoringSetupFee` overrides `FinishTypeSetupFee(Scoring)`** — once `ScoringSetupFee` exists in the pricelist, the generic type-level fallback is suppressed for the Scoring finish type.
- **Parameterized Scoring overrides the legacy finish path** — a `SelectedFinish` with `ScoringParams` is priced exclusively by `ScoringCountSurcharge`; the `FinishSurcharge`/`FinishTypeSurcharge` path is bypassed. A plain Scoring finish without params still uses the legacy path for backward compatibility.
- For quantity tiers, the **most specific matching tier wins** — the tier with the highest `minQuantity` that is ≤ the actual quantity.

### Worked Example: Business Cards

Configuration: 500× Coated Art Paper 300gsm + Matte Lamination + Offset Printing 4/4 (USD pricelist)

```
Material: Coated Art Paper 300gsm    $0.08 × 500 =  $40.00
Ink: Offset 4/4                      $0.04 × 500 =  $20.00
Finish: Matte Lamination             $0.03 × 500 =  $15.00
                                             ─────────────
Subtotal                                         =  $75.00
Quantity tier (250–999)                          ×    0.90
                                             ─────────────
Total                                            =  $67.50
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

### Worked Example: Creased Brochure (Scoring)

Configuration: 500× Coated Art Paper 300gsm + Scoring (2 creases) + Digital Printing (CZK pricelist)

```
Material: Coated Art Paper 300gsm    12.00 CZK × 500 = 6,000.00 CZK
Ink: Digital 4/4                      3.00 CZK × 500 = 1,500.00 CZK
Finish: Creasing (2 creases)          1.00 CZK × 500 =   500.00 CZK
                                                   ─────────────
Subtotal                                           = 8,000.00 CZK
Quantity tier (500–999)                            ×      0.85
                                                   ─────────────
Discounted subtotal                                = 6,800.00 CZK
Setup: Creasing (one-time)                         +    60.00 CZK
                                                   ─────────────
Total                                              = 6,860.00 CZK
```

Note: the `ScoringCountSurcharge(2, 1.00 CZK)` rule is an exact-match on crease count (not a per-crease unit price), so selecting 3 creases would use `ScoringCountSurcharge(3, 1.30 CZK)` and produce a different price point.

### Worked Example: Banner (Area-Based)

Configuration: 10× Adhesive Vinyl (1000×500mm) + UV Coating + UV Curable Inkjet

Finish surcharges for area-priced materials are multiplied by the item's area (m²), giving a
per-banner cost that scales with size rather than being a flat per-item fee.

```
Area per unit: 1000mm × 500mm = 0.5 m²
Material: Adhesive Vinyl       $16.20/m² × 0.5 =  $8.10/unit
Ink: UV Inkjet 4/4              $1.80/m² × 0.5 =  $0.90/unit
Finish: UV Coating (area)       $0.04/m² × 0.5 =  $0.02/unit
Material line:                  $8.10 × 10 =  $81.00
Ink line:                       $0.90 × 10 =   $9.00
Finish line:                    $0.02 × 10 =   $0.20
                                        ─────────────
Subtotal                                   =  $90.20
Quantity tier (1–249)                      ×    1.00
                                        ─────────────
Total                                     =  $90.20
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
