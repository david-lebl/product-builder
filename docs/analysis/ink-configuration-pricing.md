# Ink Configuration Pricing Analysis

## Background

In sheet printing (offset and digital), the ink configuration describes how many ink colors are printed on each side of a sheet:

| Notation | Front | Back | Description |
|----------|-------|------|-------------|
| 4/4 | 4 | 4 | Full CMYK both sides |
| 4/1 | 4 | 1 | CMYK front, mono back |
| 4/0 | 4 | 0 | CMYK front only |
| 1/1 | 1 | 1 | Mono both sides |
| 1/0 | 1 | 0 | Mono front only |

The ink configuration affects the printing cost because each ink color requires a separate pass through the press, additional plates, and more ink consumption.

## Current Approach: Percentage Multiplier (`InkConfigurationFactor`)

The original rule multiplies the **material cost** by a factor to derive the effective price:

```
effectivePrice = materialCost × multiplier
inkConfigAdjustment = materialCost × (multiplier - 1)   // negative for multiplier < 1
```

With `pricelistCzkSheet` (CZK per SRA3 sheet), the original factors were:

| Config | multiplier | Effective on 8 CZK | Effective on 18 CZK |
|--------|-----------|---------------------|----------------------|
| 4/4    | 1.00      | 8.00 CZK            | 18.00 CZK           |
| 4/0    | 0.85      | 6.80 CZK            | 15.30 CZK           |
| 4/1    | 0.90      | 7.20 CZK            | 16.20 CZK           |
| 1/0    | 0.55      | 4.40 CZK            | 9.90 CZK            |
| 1/1    | 0.65      | 5.20 CZK            | 11.70 CZK           |

### Problems with the Percentage Model

**1. Price scales with material cost, not with printing cost.**

The actual cost of printing 4/4 CMYK versus 1/0 mono on a digital press (e.g., Konica Minolta) is roughly the same *per sheet* regardless of what substrate is loaded. A premium 300gsm board sheet costs more than 90gsm paper because of the material, not because of any difference in printing effort.

With the percentage model, the "printing discount" when switching from 4/4 to 1/0 scales with material cost:
- On 90gsm (8 CZK/sheet): 1/0 discount = 8 × (1.0 - 0.55) = **−3.60 CZK**
- On 300gsm (18 CZK/sheet): 1/0 discount = 18 × (1.0 - 0.55) = **−8.10 CZK**

A customer printing 1-color jobs on premium cotton paper gets a 8 CZK/sheet discount for the ink configuration — more than double the discount for cheap bond paper — even though the actual printing cost difference is identical.

**2. Counter-intuitive for pricelist management.**

Operators must set multipliers (e.g., 0.55, 0.85) and mentally translate these to "how much will this actually cost on each material." With many materials at different price points, it is difficult to reason about whether the resulting prices are fair.

**3. Loophole for expensive materials.**

A customer who selects a very expensive specialty substrate but orders mono-color printing benefits from an outsized discount. The system rewards expensive-material + cheap-printing combinations disproportionately. This does not reflect the real cost structure.

**4. Semantic mismatch for manufacturing cost tracking.**

When separating the **material cost** (substrate) from the **printing cost** (press time, ink, plates), a percentage multiplier on material cost conflates the two. This makes it impossible to use the breakdown to estimate supplier fees such as the per-sheet charges from digital print suppliers like Konica Minolta.

## Proposed Approach: Flat Per-Unit Surcharge (`InkConfigurationSurcharge`)

Replace `InkConfigurationFactor` with a new rule type:

```scala
case InkConfigurationSurcharge(frontColorCount: Int, backColorCount: Int, surchargePerUnit: Money)
```

The material base price represents the **substrate cost only** (or substrate + 1/0 mono printing as the cheapest common case). Each ink configuration adds a **fixed monetary surcharge** per sheet that represents the actual printing cost for that configuration.

```
totalSheetCost = substratePrice + inkConfigSurcharge
```

### Advantages

- **Printing cost is independent of material cost.** A 5 CZK/sheet surcharge for 4/4 CMYK is the same whether you print on 90gsm or 350gsm paper.
- **Transparent and auditable.** Operators can directly set "4/4 CMYK printing costs 5 CZK per sheet on this press" without needing to compute percentages per material.
- **No loophole for expensive materials.** The printing cost does not grow with substrate price.
- **Useful for manufacturing cost separation.** The `surchargePerUnit` directly represents what a supplier like Konica Minolta charges per sheet for each ink configuration, enabling future manufacturing cost analysis.
- **Positive addition framing.** 1/0 (cheapest) = 0 additional surcharge; more complex configs add cost.

### Disadvantages / Trade-offs

- **Prices deviate from the percentage model at the extremes.** A single flat surcharge calibrated to mid-range materials will produce slightly different totals for the lightest and heaviest substrates compared to the old percentage model. This is acceptable because the change reflects reality more accurately (see calibration section below).
- **Backward incompatibility.** Existing pricelists using `InkConfigurationFactor` are not automatically converted. The calculator supports both rule types, with `InkConfigurationSurcharge` taking precedence.

## Recommendation

**Use `InkConfigurationSurcharge` for sheet-based pricelists.** This model is preferred for the following reasons:

1. It correctly separates substrate cost from printing cost, enabling manufacturing cost tracking.
2. It eliminates the expensive-material discount loophole.
3. It aligns with how digital press suppliers (Minolta, HP Indigo, etc.) actually charge — per sheet, per ink configuration, independent of substrate.
4. It produces round, auditable numbers in the price list.

The `InkConfigurationFactor` rule is retained for backward compatibility and may remain appropriate for base-price pricelists where materials span very different categories (promotional items, mugs, t-shirts) and ink configurations are not the primary cost driver.

## Implementation

### New Rule Type

```scala
// In PricingRule:
case InkConfigurationSurcharge(frontColorCount: Int, backColorCount: Int, surchargePerUnit: Money)
```

The `PriceCalculator` checks for `InkConfigurationSurcharge` first. If none is found, it falls back to `InkConfigurationFactor` for backward compatibility.

### Calibration for CZK Sheet Pricelist (`pricelistCzkSheet`)

The surcharges were calibrated to Czech market digital printing rates (per SRA3 sheet 320×450mm):

| Config | Surcharge | Description |
|--------|-----------|-------------|
| 4/4    | 5 CZK     | Full CMYK both sides — highest printing cost |
| 4/1    | 4 CZK     | CMYK front + mono back |
| 4/0    | 3 CZK     | CMYK front only |
| 1/1    | 2 CZK     | Mono both sides |
| 1/0    | 0 CZK     | Mono front only — baseline (no surcharge) |

### Material Sheet Price Adjustment

To keep the 4/4 CMYK effective price equal to the original combined price, each material's `pricePerSheet` was reduced by 5 CZK (the 4/4 surcharge):

| Material | Old pricePerSheet | New pricePerSheet | 4/4 effective (new) |
|----------|------------------|------------------|----------------------|
| 90–130gsm glossy/matte | 8–10 | 3–5 | 8–10 ✓ |
| 150–170gsm glossy/matte | 12 | 7 | 12 ✓ |
| 200gsm glossy/matte, Yupo | 14 | 9 | 14 ✓ |
| 250gsm glossy/matte, Silk | 16 | 11 | 16 ✓ |
| 300gsm coated/matte | 18 | 13 | 18 ✓ |
| 350gsm glossy/matte, Cotton | 20 | 15 | 20 ✓ |
| Adhesive stock | 12 | 7 | 12 ✓ |
| Uncoated bond | 8 | 3 | 8 ✓ |
| Kraft | 16 | 11 | 16 ✓ |

**4/4 CMYK prices are preserved exactly.** Prices for other configurations change as intended — they no longer scale with material cost, but instead reflect a uniform printing cost addition.

### Price Comparison: Mid-range Material (150gsm, old 12 CZK base)

| Config | Old price | New price | Δ |
|--------|-----------|-----------|---|
| 4/4    | 12.00     | 12.00     | 0% |
| 4/0    | 10.20     | 10.00     | −2% |
| 4/1    | 10.80     | 11.00     | +2% |
| 1/0    | 6.60      | 7.00      | +6% |
| 1/1    | 7.80      | 9.00      | +15% |

All 4/4, 4/0, 4/1 prices within ±10%. The 1/0 and 1/1 increases reflect removal of the material-cost discount: mono printing on 150gsm paper is now priced based on the actual substrate cost, not artificially discounted.

### Price Comparison: Light Material (90gsm, old 8 CZK base)

| Config | Old price | New price | Δ |
|--------|-----------|-----------|---|
| 4/4    | 8.00      | 8.00      | 0% |
| 4/0    | 6.80      | 6.00      | −12% |
| 4/1    | 7.20      | 7.00      | −3% |
| 1/0    | 4.40      | 3.00      | −32% |
| 1/1    | 5.20      | 5.00      | −4% |

The 1/0 price on light paper decreases significantly (to match the actual paper cost of 3 CZK + no surcharge). This is realistic: printing 1 black ink on thin bond paper genuinely is a very low-cost operation. The previous percentage model over-priced this combination.

## Future: Manufacturing Cost Tracking

The substrate price and ink surcharge can be used directly for manufacturing cost analysis:
- `MaterialSheetPrice.pricePerSheet` = raw substrate cost (what you pay to the paper merchant per sheet)
- `InkConfigurationSurcharge.surchargePerUnit` = printing cost per sheet for that configuration (what Konica Minolta or similar charges)

Combined with finishing costs, this enables a full manufacturing cost breakdown:

```
manufacturing cost = substrate + printing + finishing
selling price = manufacturing cost × margin multiplier
```

This separation also makes it straightforward to compare offers from multiple print suppliers who charge different rates for the same ink configurations.
