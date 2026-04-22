# Ink Configuration Pricing — Analysis & Proposal

> **Status:** Analysis / Pre-implementation.  
> **Scope:** Research document only — no code changes. Implementation belongs to a follow-up PR.  
> **Date:** 2026-04-22

---

## 1. Context & Motivation

### 1.1 Status Quo

The pricing engine expresses ink configuration cost as a **multiplicative factor on material price**:

```scala
// modules/domain/src/main/scala/mpbuilder/domain/pricing/PricingRule.scala:30
case InkConfigurationFactor(frontColorCount: Int, backColorCount: Int, materialMultiplier: BigDecimal)
```

This rule is evaluated in `PriceCalculator.computeInkConfigLine`
([`PriceCalculator.scala:351–374`](../modules/domain/src/main/scala/mpbuilder/domain/pricing/PriceCalculator.scala)):

```scala
private def computeInkConfigLine(
    inkConfig: InkConfiguration,
    rules: List[PricingRule],
    materialUnitPrice: Money,
    materialLineTotal: Money,
    effectiveQuantity: Int,
): Option[LineItem] =
  rules.collectFirst {
    case r: PricingRule.InkConfigurationFactor
        if r.frontColorCount == inkConfig.front.colorCount
           && r.backColorCount == inkConfig.back.colorCount =>
      r.materialMultiplier
  }.flatMap { multiplier =>
    if multiplier == BigDecimal(1) then scala.None
    else
      val adjustmentFactor = multiplier - BigDecimal(1)
      val unitAdjustment   = materialUnitPrice * adjustmentFactor
      val lineTotal        = materialLineTotal  * adjustmentFactor
      Some(LineItem(
        label    = s"Ink configuration: ${inkConfig.notation}",
        unitPrice = unitAdjustment,
        quantity  = effectiveQuantity,
        lineTotal = lineTotal,
      ))
  }
```

Key observations:

- The 4/4 CMYK-both-sides multiplier is always **1.00**, so no ink line item is emitted.
  All other configurations produce a **negative** line item (a discount off material).
- The function is invoked for *all* material pricing modes: area-tier
  ([`PriceCalculator.scala:222`](../modules/domain/src/main/scala/mpbuilder/domain/pricing/PriceCalculator.scala)),
  area price ([`:250`](../modules/domain/src/main/scala/mpbuilder/domain/pricing/PriceCalculator.scala)),
  sheet price, and base price — a single rule applies everywhere regardless of substrate or press type.
- `InkConfiguration` is defined in
  [`specification.scala:29–44`](../modules/domain/src/main/scala/mpbuilder/domain/model/specification.scala),
  with notations `4/4`, `4/0`, `4/1`, `1/0`, `1/1`, `4/0+W`.

### 1.2 Real-World Billing

Three physically distinct press types each bill ink cost differently:

| Press type | Billing model | Example |
|---|---|---|
| Digital (toner/inkjet) | Click charge per sheet × colour/mono rate | Konica Minolta C12000: ~CZK 1.20–3.50/sheet depending on config |
| UV flatbed / UV roll-to-roll | Ink cost per m² of printed area | HP Scitex FB7500: ~USD 0.60–1.80/m² |
| Offset | One-time plate + consumable ink amortised per run | Highly variable per job length |

None of these models is proportional to *substrate price*. Printing CMYK on 350gsm coated board uses essentially the same
toner as printing CMYK on 90gsm coated — the sheet size and pass count matter, not the paper grade.

### 1.3 Link to Product Goal

- **Transparent pricing.** Moving to positive, additive ink line items matches how real press contracts are structured.
- **Future supplier cost integration.** `ProductionCostCalculator`
  ([`ProductionCostCalculator.scala`](../modules/domain/src/main/scala/mpbuilder/domain/pricing/ProductionCostCalculator.scala))
  today has `MaterialAreaCost` and `MaterialUnitCost` cost rules; a future
  `ProductionCostRule.InkSheetCost` mapping click-charge invoices to the same `PrintingMethodId` key
  slots in naturally.
- **Multi-press pricelists.** A shop with both a digital press and a UV inkjet must maintain separate ink tables;
  binding ink rules to `PrintingMethodId` makes that explicit.

---

## 2. Current Model — Walkthrough

### 2.1 Formula

Given:
- `materialLineTotal` = material unit price × effective quantity (or sheets used × sheet price)
- `multiplier` = `InkConfigurationFactor.materialMultiplier`

The engine computes:

```
inkAdjustment = materialLineTotal × (multiplier − 1)
```

Because the multiplier for every non-4/4 config is `< 1`, `(multiplier − 1)` is negative: the line item is a **discount**.
For 4/4 (`multiplier = 1.00`) no line item is emitted — the 4/4 cost is implicit in the material price.

### 2.2 Current Factor Tables

**USD Pricelist** (`SamplePricelist.scala:81–85`):

| Config | Multiplier | Adjustment factor |
|--------|-----------|-------------------|
| 4/4    | 1.00      | 0 (no line)       |
| 4/0    | 0.60      | −0.40             |
| 4/1    | 0.75      | −0.25             |
| 1/0    | 0.40      | −0.60             |
| 1/1    | 0.55      | −0.45             |

**CZK Unit Pricelist** (`SamplePricelist.scala:281–289`):

| Config | Multiplier | Adjustment factor |
|--------|-----------|-------------------|
| 4/4    | 1.00      | 0 (no line)       |
| 4/0    | 0.85      | −0.15             |
| 4/1    | 0.90      | −0.10             |
| 1/0    | 0.55      | −0.45             |
| 1/1    | 0.65      | −0.35             |

**CZK Sheet Pricelist** (`SamplePricelist.scala:641–645`):

| Config | Multiplier | Adjustment factor |
|--------|-----------|-------------------|
| 4/4    | 1.00      | 0 (no line)       |
| 4/0    | 0.85      | −0.15             |
| 4/1    | 0.90      | −0.10             |
| 1/0    | 0.55      | −0.45             |
| 1/1    | 0.65      | −0.35             |

---

## 3. Loopholes & Disadvantages (with Numbers)

### 3.1 Regressive Absolute Cost for Expensive Materials

**Problem.** The ink adjustment scales with material price, so more expensive substrates receive a proportionally larger
"ink credit". A single CMYK-front SRA3 pass consumes the same toner regardless of whether the sheet costs 8 CZK or
20 CZK.

**Worked example — CZK Sheet Pricelist, 4/0 on SRA3:**

| Material (SRA3 sheet) | Sheet price | Multiplier | Total 4/0 | "Ink credit" |
|---|---|---|---|---|
| Coated Glossy 90gsm   | 8.00 CZK  | 0.85 | 6.80 CZK | −1.20 CZK |
| Coated Glossy 200gsm  | 14.00 CZK | 0.85 | 11.90 CZK| −2.10 CZK |
| Coated Glossy 350gsm  | 20.00 CZK | 0.85 | 17.00 CZK| −3.00 CZK |

The implied ink cost for 4/0 on 350gsm (3.00 CZK) is 2.5× that on 90gsm (1.20 CZK), even though the printer meters
identically. The shop implicitly subsidises mono jobs on cheap paper and overcharges single-sided on expensive paper.

### 3.2 Implicit 4/4 Baseline Is Fragile

The 4/4 multiplier of 1.00 acts as the baseline anchor; all other multipliers are calibrated relative to it. This
means:

1. Introducing `5/5` (CMYK + white spot both sides), `6/0` (CMYK + two spots front), or any new ink config requires
   recalibrating *every* existing multiplier because the new configuration's multiplier must be relative to the same
   implicit 4/4 material price.
2. When the shop changes a material's price, *all* implied ink costs for *all* configurations change proportionally —
   even if the press contract did not change.

### 3.3 Cannot Integrate Supplier Costs

Konica Minolta click-charge contracts bill per sheet per ink configuration (e.g., CZK 0.60/mono-sheet,
CZK 2.80/colour-sheet). These are flat per-sheet rates, not percentages of substrate price. To compute a margin
analysis via `ProductionCostCalculator`, a parallel per-sheet cost table is needed — duplicating configuration.
If ink were already a first-class line item keyed by `PrintingMethodId`, the cost rule would be a single new entry in
the same lookup table.

### 3.4 "No Print" (0/0) Is Invisible

Material sold without printing (blank stock) has no `InkConfigurationFactor` for `(0, 0)` — the factor simply
doesn't exist — so `computeInkConfigLine` returns `None` and the full material price is charged. This is
coincidentally correct (no discount), but makes it impossible to *explicitly* represent "no ink applied" with a CZK 0
ink line item, which would be useful for transparent breakdowns.

### 3.5 Large-Format Materials Are Mispriced on the Same Axis

PVC Banner 510g uses `MaterialAreaTier` (price per m² varying by area). Yet the single `InkConfigurationFactor`
multiplier still applies to the *area-based material cost*, not to a separate per-m² ink rate. For a 1 m² vinyl banner
at 420 CZK/m² with 4/0 factor 0.85:

```
material total  = 420.00 CZK
ink adjustment  = 420 × (0.85 − 1) = −63.00 CZK
total           = 357.00 CZK
```

For a 4 m² banner at the same rate:

```
material total  = 1680.00 CZK
ink adjustment  = −252.00 CZK   ← same 15% off regardless of actual ink coverage
total           = 1428.00 CZK
```

Real UV inkjet ink cost scales with *coverage area*, not with substrate price. The existing model conflates them.

---

## 4. Proposed Model

### 4.1 Two New Sealed ADT Variants

Replace `InkConfigurationFactor` with two new `PricingRule` variants, both keyed by `PrintingMethodId`:

```scala
// In PricingRule.scala — after InkConfigurationFactor (or in place of it after migration)
case InkConfigurationSheetPrice(
    printingMethodId: PrintingMethodId,
    frontColorCount: Int,
    backColorCount: Int,
    pricePerSheet: Money,
)

case InkConfigurationAreaPrice(
    printingMethodId: PrintingMethodId,
    frontColorCount: Int,
    backColorCount: Int,
    pricePerSqM: Money,
)
```

These follow the exact syntactic pattern of `MaterialSheetPrice` / `MaterialAreaPrice` / `MaterialAreaTier` already
in the enum, keeping the interpreter shape-match simple and consistent.

### 4.2 Semantics

| Property | Detail |
|---|---|
| **Positive, additive** | Ink is a cost, not a discount. `0/0` produces no line item — correct by omission rather than silent accident. |
| **Printing-method scoped** | A rule fires only when `rule.printingMethodId == config.printingMethod.id` (`configuration.scala:6`). |
| **Explicit miss** | If no rule matches the `(methodId, front, back)` tuple, the engine emits `PricingError.InkConfigurationNotPriced(methodId, front, back)` — not a silent zero. |
| **Basis-appropriate** | `SheetPrice` multiplies by `sheetsUsed` from the existing nesting algorithm (`PriceCalculator.scala:607–618`). `AreaPrice` multiplies by `areaSqM × quantity` (same formula as `MaterialAreaPrice`, `:240–241`). |
| **Selection rule** | The engine uses `SheetPrice` when a `MaterialSheetPrice` or `MaterialBasePrice` rule drives the component; `AreaPrice` when `MaterialAreaPrice` or `MaterialAreaTier` drives. `PrintingMethod.processType` is used only for sanity validation, not as the lookup key. |
| **Quantity tier interaction** | Ink lines remain outside the quantity-tier multiplier (unchanged from current `computeInkConfigLine` behaviour). |
| **Label** | `"Ink configuration: 4/0"` — unchanged. Future enhancement: append method short name for multi-press pricelists. |
| **`sheetCount` booklets** | `ProductComponent.sheetCount` (`component.scala:21`) is already multiplied into `effectiveQuantity`; `InkConfigurationSheetPrice` reuses the same `sheetsUsed` derivation, so booklets get correct per-sheet ink cost automatically. |

### 4.3 Why Two Rules, Not One Tagged Variant

The `PricingRule` enum follows the pattern `MaterialSheetPrice` / `MaterialAreaPrice` — parallel variants for the two
material bases, not a single variant with a `basis: Basis` discriminator. Two rules:

- Match the existing idiom used consistently across the enum.
- Allow pattern-match interpreters to short-circuit on shape without decoding a discriminator field.
- Make it immediately clear at the call site which pricing basis is in use.

### 4.4 Why `PrintingMethodId`, Not `PrintingProcessType`

`PrintingProcessType` (Offset, Digital, Letterpress, UVCurableInkjet, etc.) is a coarse category. Real ink cost
differences happen *per machine*, not per category — two digital presses from different vendors carry different click
charges. Using `PrintingMethodId` (`ids.scala:35–43`) preserves that granularity.

`PrintingProcessType` remains the right key for `PrintingProcessSurcharge`, which models fixed run-mode differences
(e.g., offset setup cost vs. digital no-setup cost). Ink cost and process surcharge are separate concerns.

### 4.5 Sample Entity Keys

Relevant `PrintingMethodId` values from `SampleCatalog.scala`:

| Constant | ID string | Use |
|---|---|---|
| `SampleCatalog.digitalId`   | `"pm-digital"`   | All sheet/unit paper products |
| `SampleCatalog.uvInkjetId`  | `"pm-uv-inkjet"` | Banners, large-format area products |
| `SampleCatalog.offsetId`    | `"pm-offset"`    | (future) Offset print products |

---

## 5. New Price Tables

For each pricelist:
- **Bare material price** = current material price − implied 4/4 ink cost (the anchor).
- **Per-sheet / per-m² ink price** for each config, keyed by the relevant `PrintingMethodId`.
- **Deviation table** comparing new totals to current totals; rows > ±10% are flagged.

The 4/4 deviation is always **0%** by construction (total = bare material + ink_44 = original material).

---

### 5.1 CZK Sheet Pricelist (`pricelistCzkSheet`) — Digital Press (pm-digital)

**Proposed ink prices per SRA3 sheet (`InkConfigurationSheetPrice`, pm-digital):**

| Config | Ink / sheet (CZK) | Description |
|--------|-------------------|-------------|
| 4/4    | 3.00              | Two full-colour CMYK passes |
| 4/0    | 1.50              | One full-colour CMYK pass |
| 4/1    | 2.00              | One CMYK + one mono pass |
| 1/0    | 0.60              | One mono pass |
| 1/1    | 1.00              | Two mono passes |
| 4/0+W  | 2.20              | CMYK + white underlay pass |

**Anchor:** 3.00 CZK / sheet removed from all material sheet prices.

**Proposed bare material sheet prices:**

| Material | Current (CZK) | New bare (CZK) |
|---|---|---|
| Coated Glossy/Matte 90gsm  | 8   | 5   |
| Coated Glossy/Matte 115gsm | 9   | 6   |
| Coated Glossy/Matte 130gsm | 10  | 7   |
| Coated Glossy/Matte 150gsm | 12  | 9   |
| Coated Glossy/Matte 170gsm | 12  | 9   |
| Coated Glossy/Matte 200gsm | 14  | 11  |
| Coated Glossy/Matte 250gsm | 16  | 13  |
| Coated 300gsm / Silk 250gsm| 18  | 15  |
| Coated Glossy/Matte 350gsm | 20  | 17  |
| Uncoated Bond 80gsm        | 8   | 5   |
| Kraft 120gsm               | 16  | 13  |
| Adhesive Stock 80gsm       | 12  | 9   |
| Yupo 100gsm                | 14  | 11  |
| Cotton Rag 100gsm          | 20  | 17  |

**Deviation table — selected representative materials:**

| Material | Config | Current total | Proposed total | Delta |
|---|---|---|---|---|
| 90gsm  | 4/4 | 8.00  | 5.00+3.00=8.00  | 0%    |
| 90gsm  | 4/0 | 6.80  | 5.00+1.50=6.50  | −4.4% |
| 90gsm  | 4/1 | 7.20  | 5.00+2.00=7.00  | −2.8% |
| 90gsm  | 1/0 | 4.40  | 5.00+0.60=5.60  | **+27.3%** ⚠ |
| 90gsm  | 1/1 | 5.20  | 5.00+1.00=6.00  | **+15.4%** ⚠ |
| 200gsm | 4/4 | 14.00 | 11.00+3.00=14.00| 0%    |
| 200gsm | 4/0 | 11.90 | 11.00+1.50=12.50| +5.0% |
| 200gsm | 4/1 | 12.60 | 11.00+2.00=13.00| +3.2% |
| 200gsm | 1/0 | 7.70  | 11.00+0.60=11.60| **+50.6%** ⚠ |
| 200gsm | 1/1 | 9.10  | 11.00+1.00=12.00| **+31.9%** ⚠ |
| 350gsm | 4/4 | 20.00 | 17.00+3.00=20.00| 0%    |
| 350gsm | 4/0 | 17.00 | 17.00+1.50=18.50| +8.8% |
| 350gsm | 4/1 | 18.00 | 17.00+2.00=19.00| +5.6% |
| 350gsm | 1/0 | 11.00 | 17.00+0.60=17.60| **+60.0%** ⚠ |
| 350gsm | 1/1 | 13.00 | 17.00+1.00=18.00| **+38.5%** ⚠ |

**Notes on flagged rows (⚠):**

- **4/0 on all weights:** within ±10% (max +8.8%). ✓
- **4/1 on all weights:** within ±10% (max +5.6%). ✓
- **1/0 and 1/1 on all weights:** deviations of +15% to +60%. This is **deliberate and expected**.
  The current CZK model applied a −45% material discount for mono printing (factor 0.55), implying that
  "mono saves 45% of material cost". In reality, a mono pass saves one colour cycle but not 45% of substrate cost.
  The new model correctly prices mono printing at approximately 0.60 CZK per sheet above bare substrate.
  Existing mono jobs were steeply underpriced; the new prices are economically correct.
  Recommendation: retain the new prices, document the correction in the migration PR changelog, adjust only if
  customer feedback on specific products indicates the new prices are uncompetitive.

**Area-based materials on CZK Sheet Pricelist (UV inkjet, `InkConfigurationAreaPrice`, pm-uv-inkjet):**

| Config | Ink / m² (CZK) |
|--------|----------------|
| 4/4    | 45             |
| 4/0    | 22             |
| 4/1    | 30             |
| 1/0    | 6              |
| 1/1    | 10             |
| 4/0+W  | 33             |

Material price reductions (45 CZK/m² removed):

| Material | Current (CZK/m²) | New bare (CZK/m²) |
|---|---|---|
| Vinyl                    | 420 | 375 |
| Clear Vinyl              | 520 | 475 |
| PVC 510g tier 0–2m²      | 600 | 555 |
| PVC 510g tier 2–5m²      | 500 | 455 |
| PVC 510g tier 5–10m²     | 450 | 405 |
| PVC 510g tier 10m²+      | 400 | 355 |
| Roll-up Film             | 280 | 235 |

Deviation for 4/0 on area materials (deviations exceed ±10% because the current 0.85 factor was proportional
to substrate price, not physical ink coverage):

| Material | Current 4/0 | Proposed 4/0 | Delta |
|---|---|---|---|
| Vinyl 420 CZK/m²        | 357.00 | 375+22=397  | **+11.2%** ⚠ |
| PVC tier 0 (600 CZK/m²) | 510.00 | 555+22=577  | **+13.1%** ⚠ |
| PVC tier 3 (400 CZK/m²) | 340.00 | 355+22=377  | **+10.9%** ⚠ |

These deviations are accepted as a **pricing correction**: the old model implied that 4/0 printing on a 600 CZK/m² PVC
banner costs 90 CZK/m² less in ink than 4/4 (= 600 × 0.15 = 90 CZK/m²), which is not how UV inkjet billing works.
The new per-m² ink cost is physically motivated and should be communicated to customers as a separate line item.

---

### 5.2 CZK Unit Pricelist (`pricelistCzk`) — Digital Press (pm-digital)

The CZK unit pricelist uses `MaterialBasePrice` (per-unit flat price). With the proposed model, the ink rule is
`InkConfigurationSheetPrice` interpreted as a **per-unit** price (1 unit = 1 "pass" for the digital press).

**Proposed ink prices per unit (same as per-sheet — same printer, digital):**

| Config | Ink / unit (CZK) |
|--------|-----------------|
| 4/4    | 3.00            |
| 4/0    | 1.50            |
| 4/1    | 2.00            |
| 1/0    | 0.60            |
| 1/1    | 1.00            |
| 4/0+W  | 2.20            |

**Anchor:** 3.00 CZK removed from all per-unit material base prices.

**Deviation table (representative):**

| Material | Config | Current total | Proposed total | Delta |
|---|---|---|---|---|
| 90gsm (12 CZK/unit)  | 4/4 | 12.00 | 9.00+3.00=12.00 | 0%    |
| 90gsm                | 4/0 | 10.20 | 9.00+1.50=10.50 | +2.9% |
| 90gsm                | 4/1 | 10.80 | 9.00+2.00=11.00 | +1.9% |
| 90gsm                | 1/0 | 6.60  | 9.00+0.60=9.60  | **+45.5%** ⚠ |
| 90gsm                | 1/1 | 7.80  | 9.00+1.00=10.00 | **+28.2%** ⚠ |
| 350gsm (15 CZK/unit) | 4/4 | 15.00 | 12.00+3.00=15.00| 0%    |
| 350gsm               | 4/0 | 12.75 | 12.00+1.50=13.50| +5.9% |
| 350gsm               | 4/1 | 13.50 | 12.00+2.00=14.00| +3.7% |
| 350gsm               | 1/0 | 8.25  | 12.00+0.60=12.60| **+52.7%** ⚠ |
| 350gsm               | 1/1 | 9.75  | 12.00+1.00=13.00| **+33.3%** ⚠ |

Same rationale for 1/0 and 1/1 deviations as §5.1 applies here.

For area-based materials on the CZK unit pricelist, use the same UV inkjet `InkConfigurationAreaPrice` rules as §5.1.

---

### 5.3 USD Pricelist (`pricelist`) — Digital Press (pm-digital) + UV Inkjet (pm-uv-inkjet)

The USD pricelist serves two press types:
- **Sheet/unit paper materials** (MaterialBasePrice): digital press.
- **Area materials** (MaterialAreaPrice for vinyl, clear vinyl, PVC 510g, roll-up film): UV inkjet.

#### 5.3.1 Paper Materials — Digital Press (`InkConfigurationSheetPrice`, pm-digital)

**Proposed ink prices per unit:**

| Config | Ink / unit (USD) |
|--------|-----------------|
| 4/4    | 0.04            |
| 4/0    | 0.02            |
| 4/1    | 0.03            |
| 1/0    | 0.005           |
| 1/1    | 0.008           |

**Anchor:** $0.04 per unit removed from material base prices.

**Proposed bare material prices (USD):**

| Material | Current (USD) | New bare (USD) |
|---|---|---|
| Coated 300gsm       | 0.12 | 0.08 |
| Coated Silk 250gsm  | 0.11 | 0.07 |
| Uncoated Bond       | 0.06 | 0.02 |
| Kraft               | 0.10 | 0.06 |
| Corrugated          | 0.25 | 0.21 |
| Yupo                | 0.18 | 0.14 |
| Adhesive Stock      | 0.14 | 0.10 |
| Cotton              | 0.22 | 0.18 |

**Deviation table:**

| Material | Config | Current total | Proposed total | Delta |
|---|---|---|---|---|
| Coated 300gsm ($0.12) | 4/4 | 0.120 | 0.08+0.04=0.120 | 0%     |
| Coated 300gsm         | 4/0 | 0.072 | 0.08+0.02=0.100 | **+38.9%** ⚠ |
| Coated 300gsm         | 4/1 | 0.090 | 0.08+0.03=0.110 | **+22.2%** ⚠ |
| Coated 300gsm         | 1/0 | 0.048 | 0.08+0.005=0.085| **+77.1%** ⚠ |
| Coated 300gsm         | 1/1 | 0.066 | 0.08+0.008=0.088| **+33.3%** ⚠ |
| Uncoated Bond ($0.06) | 4/4 | 0.060 | 0.02+0.04=0.060 | 0%     |
| Uncoated Bond         | 4/0 | 0.036 | 0.02+0.02=0.040 | +11.1% ⚠ |
| Uncoated Bond         | 4/1 | 0.045 | 0.02+0.03=0.050 | +11.1% ⚠ |
| Uncoated Bond         | 1/0 | 0.024 | 0.02+0.005=0.025| +4.2%  |
| Uncoated Bond         | 1/1 | 0.033 | 0.02+0.008=0.028| **−15.2%** ⚠ |

**Notes on USD paper deviations:**

The USD pricelist used aggressive multiplicative factors (4/0 = 0.60, 1/0 = 0.40), creating very large implicit
ink credits that are not physically motivated. A flat $0.04/unit ink base cannot closely match a 40–60% material
discount for all price points simultaneously:

- On cheap materials ($0.06 bond), the 40% discount for 4/0 was $0.024 — less than the proposed flat $0.02 ink cost,
  so the new model barely deviates.
- On moderately expensive materials ($0.12 coated), the 40% discount for 4/0 was $0.048 vs the proposed $0.02 ink
  — a $0.028 gap, which is +38.9%.

This is **accepted as a pricing correction**: the USD sample pricelist's 0.60 multiplier for 4/0 implied that
40 cents of every dollar of material cost was avoided by printing single-sided, which is not aligned with real press
economics. No actual customer pricelist is sourced from this sample data. The new prices better reflect per-sheet
digital press costs. In practice, a USD pricelist operator would adjust the per-unit ink values to match their actual
click-charge contract — the $0.04/$0.02 values are starting points.

#### 5.3.2 Area Materials — UV Inkjet (`InkConfigurationAreaPrice`, pm-uv-inkjet)

**Proposed ink prices per m² (USD):**

| Config | Ink / m² (USD) |
|--------|---------------|
| 4/4    | 1.80          |
| 4/0    | 0.90          |
| 4/1    | 1.20          |
| 1/0    | 0.25          |
| 1/1    | 0.40          |
| 4/0+W  | 1.30          |

**Anchor:** $1.80/m² removed from all area material prices.

**Proposed bare area material prices (USD):**

| Material | Current (USD/m²) | New bare (USD/m²) |
|---|---|---|
| Vinyl         | 18.00 | 16.20 |
| Clear Vinyl   | 22.00 | 20.20 |
| PVC 510g      | 18.00 | 16.20 |
| Roll-up Film  | 12.00 | 10.20 |

**Deviation table (1 m² example):**

| Material | Config | Current total | Proposed total | Delta |
|---|---|---|---|---|
| Vinyl $18.00/m²      | 4/4 | 18.00 | 16.20+1.80=18.00 | 0%      |
| Vinyl                | 4/0 | 10.80 | 16.20+0.90=17.10 | **+58.3%** ⚠ |
| PVC $18.00/m²        | 4/4 | 18.00 | 16.20+1.80=18.00 | 0%      |
| PVC                  | 4/0 | 10.80 | 16.20+0.90=17.10 | **+58.3%** ⚠ |

The deviations are large but **correct**: the USD model's 0.60 factor implied that a 4/0 banner costs 40% less in ink
than a 4/4 banner, reducing a $18/m² substrate to $10.80/m² just by printing single-sided. At UV inkjet click charges,
a $1.80/m² vs $0.90/m² difference between 4/4 and 4/0 is physically accurate; the current $7.20/m² difference is not.
Any USD pricelist operators running UV inkjet should verify their actual ink costs (typically $0.60–2.50/m²) and adjust
accordingly.

---

## 6. Round-Trip Verification with Existing Test Scenarios

### 6.1 Sheet-Based CZK Test: A4 Flyer 4/0 on 90gsm

Mirrors the worked example from `docs/analysis/sheet-based-pricing.md` and representative assertions in
`PriceCalculatorSpec.scala`.

**Configuration:** 100× A4 flyer (210×297mm) on Coated Glossy 90gsm, ink 4/0, CZK sheet pricelist, quantity tier 1–49
(multiplier 1.0).

Nesting (unchanged):
```
SRA3 320×450mm, bleed 3mm, gutter 2mm
Effective item: 216×303mm
Rotated: cols=floor(322/305)=1, rows=floor(452/218)=2 → 2 pcs/sheet
sheetsUsed = ceil(100/2) = 50
```

| Calculation | Current model | Proposed model |
|---|---|---|
| Material (50 sheets) | 8.00×50 = 400.00 CZK | 5.00×50 = 250.00 CZK |
| Ink config (4/0)     | 400×(0.85−1) = −60.00 CZK | 1.50×50 = 75.00 CZK |
| Cutting (1 cut/sheet)| 0.10×50 = 5.00 CZK | 0.10×50 = 5.00 CZK (unchanged) |
| **Subtotal** | **345.00 CZK** | **330.00 CZK** |
| **Delta** | — | −4.4% ✓ |

### 6.2 Area-Based CZK Test: Vinyl Banner 4/0 (1 m²)

**Configuration:** 1× Vinyl banner (1000×1000mm = 1 m²), ink 4/0, CZK sheet pricelist, pm-uv-inkjet.

| Calculation | Current model | Proposed model |
|---|---|---|
| Material (1 m²)  | 420.00 CZK | 375.00 CZK |
| Ink config (4/0) | 420×(0.85−1) = −63.00 CZK | 22.00×1 = 22.00 CZK |
| **Subtotal** | **357.00 CZK** | **397.00 CZK** |
| **Delta** | — | +11.2% ⚠ (accepted — see §5.1 rationale) |

---

## 7. Future-Proofing

### 7.1 Manufacturing Cost Integration

The per-sheet / per-m² basis maps 1:1 to supplier click-charge contracts. A future
`ProductionCostRule.InkSheetCost(printingMethodId, frontColorCount, backColorCount, costPerSheet)` can be populated
directly from Konica Minolta invoices and fed into `ProductionCostCalculator` for margin analysis — no change to
public price rules needed, and the same `PrintingMethodId` key links the two tables.

### 7.2 Extensibility for New Ink Configurations

Adding `5/5` (CMYK+W both sides), spot-colour combinations, or process-specific variants (UV flatbed vs. UV roll)
is a new row in the ink price table, not a recalibration of all material prices. The `InkConfiguration` model
already supports these via its `front: InkSetup` and `back: InkSetup` fields
([`specification.scala:29`](../modules/domain/src/main/scala/mpbuilder/domain/model/specification.scala)).

### 7.3 Multi-Press Pricelists

Because rules are keyed by `PrintingMethodId`, a shop with multiple presses (Minolta C12000 digital + HP Latex
large-format + Heidelberg offset) maintains one ink table per machine within the same pricelist. Onboarding a new
press adds its ink table without touching any existing rules or prices.

### 7.4 `sheetCount` for Booklets

`ProductComponent.sheetCount`
([`component.scala:21`](../modules/domain/src/main/scala/mpbuilder/domain/model/component.scala))
is already multiplied into `effectiveQuantity` in both `PriceCalculator` and `ProductionCostCalculator`. When
`InkConfigurationSheetPrice` uses `sheetsUsed` (which is derived from `effectiveQuantity`), booklet bodies with
`sheetCount > 1` automatically accumulate the correct total ink cost per physical sheet.

### 7.5 Large-Format Printing Implications

`InkConfigurationAreaPrice` scales with `areaSqM × quantity`, which is the correct physical basis: a 2×3 m banner
costs 2× more in UV inkjet ink than a 1×3 m banner of the same configuration. This is currently impossible to express
correctly with a material multiplier.

---

## 8. Migration Notes (Follow-Up Implementation PR — Not This Plan's Scope)

1. **Add two new variants.** Add `InkConfigurationSheetPrice` and `InkConfigurationAreaPrice` to `PricingRule.scala`.
   Keep `InkConfigurationFactor` temporarily (annotated as deprecated) for backward-compat test verification.

2. **Extend `computeInkConfigLine`.** Update `PriceCalculator.computeInkConfigLine` to:
   - Accept the active `PrintingMethod` and material pricing mode.
   - Try `InkConfigurationSheetPrice` (match by method + colorCounts → multiply by `sheetsUsed`).
   - Try `InkConfigurationAreaPrice` (match by method + colorCounts → multiply by `areaSqM × quantity`).
   - Fall back to `InkConfigurationFactor` for backward compatibility during migration.
   - If none match and no factor exists, emit `PricingError.InkConfigurationNotPriced`.
   - Return a positive `LineItem` (not a negative adjustment).

3. **Update `SamplePricelist.scala`** — remove `InkConfigurationFactor` entries; add
   `InkConfigurationSheetPrice` and `InkConfigurationAreaPrice` entries per the tables in §5.

4. **Update `PriceCalculatorSpec.scala`** — most pricing assertions will change. Expect ~50+ assertions
   to update to new expected values. Key changes:
   - Ink lines become positive, not negative.
   - Subtotals for non-4/4 configs increase on expensive materials.
   - Area-based ink totals increase for 4/0 (the old model heavily discounted them).

5. **Remove `InkConfigurationFactor`** after all tests pass green.

6. **Update `docs/pricing.md`** rule count (currently 22 rules → 23 net: −1 old + 2 new = +1) and the
   rule table.

7. **Run `post-work-docs` skill** to emit a changelog entry and refresh `docs/INDEX.md`.

---

## 9. Decision Log

### D1 — Additive, Not Multiplicative

**Decision:** Ink is a positive cost line item.

**Rationale:** Matches physical cost structure (click-charge per sheet, ink per m²); decouples ink cost from
material price; makes the margin analysis tractable (one entry in the cost table per method+config); supports
future supplier-cost integration without a parallel price table.

**Alternative considered:** Keep multiplicative but cap the denominator at a "reference material price". Rejected:
introduces an artificial reference value that has no physical meaning and would need constant maintenance.

### D2 — Two Rules, Not One with a Basis Discriminator

**Decision:** `InkConfigurationSheetPrice` and `InkConfigurationAreaPrice` as separate variants.

**Rationale:** Follows existing `PricingRule` idiom (`MaterialSheetPrice` / `MaterialAreaPrice`). Pattern-match
on shape is simpler than decoding a discriminator field. It is immediately visible at the definition site whether
a rule is sheet-based or area-based.

**Alternative considered:** `InkConfigurationPrice(basis: InkBasis, ...)` with `InkBasis` enum. Rejected: no other
rule uses this pattern; would require a new ADT with a single use.

### D3 — Bind to `PrintingMethodId`, Not `PrintingProcessType`

**Decision:** Ink rules keyed by `PrintingMethodId`.

**Rationale:** Real ink economics vary per machine (Konica Minolta C12000 vs. Konica Minolta C14000 have different
click rates; HP Latex 700 vs. HP Scitex differ in ink cost/m²). `PrintingMethodId` preserves that granularity.
It is also the key that future `ProductionCostRule.InkSheetCost` rows will use, giving a 1:1 mapping.

`PrintingProcessType` remains the right key for `PrintingProcessSurcharge` (process-mode fixed costs), not for
per-unit consumable ink.

### D4 — Accept Large Deviations for 1/0 and 1/1 (CZK) and All Configs (USD Area)

**Decision:** Do not contort the new ink prices to minimise deviation from the old model.

**Rationale:**

- *CZK mono (1/0, 1/1):* The current 0.55/0.65 factors imply 45%/35% of substrate cost is saved by mono printing,
  which is not physically correct for digital presses. The new prices (0.60 / 1.00 CZK per sheet) are
  economically accurate. Existing mono jobs were underpriced; the correction is appropriate.
- *USD area materials:* The 0.60 factor for 4/0 implied 40% of UV inkjet substrate cost is avoided for
  single-sided printing — impossible (substrate cost is the same; only ink cost differs). The USD area
  sample pricelist was not sourced from a real price list and should not be used as the reference.
- No actual customer pricing is derived from these sample pricelists; the corrections do not risk immediate revenue
  impact.
