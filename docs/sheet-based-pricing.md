# Sheet-Based Material Pricing

## The Problem

In the printing industry, materials are purchased as standard sheets (A3, SRA3, B2, etc.), and finished products are cut
from those sheets. A printer doesn't pay "per business card" for paper — they pay per sheet of SRA3 stock and then cut
as many cards from it as possible. The per-piece cost therefore depends on **how many finished items fit on one sheet
** (imposition/nesting).

The original pricing model had two material pricing strategies:

- **`MaterialBasePrice`** — a flat per-unit price, independent of dimensions
- **`MaterialAreaPrice`** — price per square meter, used for large-format materials like vinyl

Neither captures the economics of sheet-fed printing. A flat per-unit price ignores the relationship between item size
and sheet utilization. An A6 flyer and an A3 flyer would have the same material cost, even though two A4s or four A6s
fit on the same sheet as one A3.

## The Solution: Sheet Nesting

The new `MaterialSheetPrice` rule models real sheet-fed pricing:

```
MaterialSheetPrice(
  materialId,        // which material
  pricePerSheet,     // cost of one sheet of stock
  sheetWidthMm,      // sheet dimensions
  sheetHeightMm,
  bleedMm,           // bleed added per side to each item
  gutterMm,          // spacing between items on the sheet
  minUnitPrice,      // floor price per piece
)
```

### Nesting Algorithm

Given the item dimensions from the product's `SizeSpec`, the calculator computes the **effective item size** by adding
bleed on all four sides:

```
effectiveWidth  = itemWidth  + 2 * bleedMm
effectiveHeight = itemHeight + 2 * bleedMm
```

It then calculates how many items fit on the sheet using a simple grid layout:

```
cols = floor((sheetWidth  + gutter) / (effectiveWidth  + gutter))
rows = floor((sheetHeight + gutter) / (effectiveHeight + gutter))
piecesPerSheet = cols * rows
```

Both orientations (normal and 90-degree rotated) are tried, and the better one is used. The result is clamped to a
minimum of 1 (an oversized item that doesn't fully fit still counts as 1 per sheet).

### Unit Price Derivation

```
rawUnitPrice = pricePerSheet / piecesPerSheet
unitPrice    = max(rawUnitPrice, minUnitPrice)
```

The `minUnitPrice` floor prevents unrealistically cheap pricing for very small items where dozens fit on a single sheet
but the actual handling and finishing cost per piece doesn't scale down to zero.

### Cutting Surcharge

A companion rule `CuttingSurcharge(costPerCut)` models the cost of cutting items from the sheet. It only applies when
sheet-based pricing is in use:

```
numCuts      = piecesPerSheet - 1
costPerPiece = (numCuts * costPerCut) / piecesPerSheet
```

This appears as a separate line item in the breakdown, making the cost structure transparent.

### Precedence

Material pricing rules are evaluated in specificity order:

```
MaterialAreaPrice  >  MaterialSheetPrice  >  MaterialBasePrice
```

Area pricing (for large-format materials like vinyl banners) is the most specific. Sheet pricing sits in the middle.
Base pricing is the fallback. A pricelist can define multiple rule types for the same material — only the
highest-precedence one is used.

### Worked Example: A4 Flyer on SRA3

Configuration: 100x A4 flyer (210x297mm) on Coated Glossy 90gsm, 4/4 CMYK

```
Sheet: SRA3 320x450mm, price = 8.00 CZK
Bleed: 3mm per side, Gutter: 2mm

Effective item: 216 x 303mm
Normal:  cols=floor(322/218)=1, rows=floor(452/305)=1 → 1 piece
Rotated: cols=floor(322/305)=1, rows=floor(452/218)=2 → 2 pieces
Best: 2 pieces per sheet

Material: 8.00 / 2 = 4.00 CZK/piece × 100 = 400.00 CZK
Cutting:  1 cut × 0.10 / 2 = 0.05 CZK/piece × 100 =   5.00 CZK
                                                    ──────────────
Subtotal                                            =  405.00 CZK
```

## Sheet Quantity Tiers

### The Problem

Standard `QuantityTier` discounts use the product quantity (e.g., 100 business cards) for tier lookup. But material
consumption varies hugely by product format:

- 100 business cards (90×55mm, 21/sheet) = **5 sheets**
- 100 A4 flyers (210×297mm, 2/sheet) = **50 sheets**
- 100 A4 booklets (8 components, 2/sheet) = **400 sheets**

All three would get the same discount with product-quantity tiers, which doesn't reflect actual print shop economics.
Discounts should be proportional to the actual press run — the number of physical sheets.

### The Solution: `SheetQuantityTier`

The `SheetQuantityTier` rule applies a multiplier based on the total number of physical sheets across all components:

```
SheetQuantityTier(
  minSheets,    // minimum sheet count for this tier
  maxSheets,    // optional upper bound (None = unlimited)
  multiplier,   // discount multiplier (e.g., 0.90 = 10% off)
)
```

### How It Works

**1. Compute sheets per component.** For each component, if a `MaterialSheetPrice` rule exists for its material:

```
piecesPerSheet = SheetNesting(sheet, item + bleed, gutter)
sheetsUsed     = ceil(effectiveQuantity / piecesPerSheet)
```

Components without sheet pricing get `sheetsUsed = 0`.

**2. Sum total sheets** across all components.

**3. Select the tier.** If `totalSheets > 0` and `SheetQuantityTier` rules exist in the pricelist, use the sheet tier.
Otherwise, fall back to `QuantityTier` (backward compatible).

### Worked Example: Business Cards vs. A4 Flyers

Both orders are 100 pieces on the CZK sheet pricelist:

```
Business cards (90×55mm):
  21 pieces/sheet → ceil(100/21) = 5 sheets
  Sheet tier 1-49: multiplier = 1.0 (no discount)

A4 flyers (210×297mm):
  2 pieces/sheet → ceil(100/2) = 50 sheets
  Sheet tier 50-249: multiplier = 0.90 (10% discount)
```

Same product quantity, different discounts — reflecting the actual press run difference.

### Multi-Component Products

For products with multiple components (e.g., booklets with cover + body), the `sheetsUsed` from each component is
summed. This means a booklet with a heavy cover and many body pages will correctly accumulate a higher sheet count than
a simple single-sheet product.

### Backward Compatibility

Pricelists that only define `QuantityTier` rules (no `SheetQuantityTier`) continue to work exactly as before — the
calculator falls back to product-quantity-based tier lookup. This means existing pricelists don't need to be updated.

### Sample Sheet Quantity Tiers (CZK Sheet Pricelist)

| Sheets | Multiplier | Discount |
|--------|------------|----------|
| 1–49   | 1.00       | None     |
| 50–249 | 0.90       | 10%      |
| 250–999| 0.80       | 20%      |
| 1000+  | 0.70       | 30%      |

## Design Decisions and Alternatives Considered

### Where to put sheet dimensions

**Chosen: On the pricing rule (`MaterialSheetPrice`)**

Sheet dimensions live on the pricing rule, not on the `Material` domain model.

*Why:* Different pricelists can use different sheet stock for the same material. A printer with an SRA3 press and one
with a B2 press both sell "Coated Glossy 90gsm" but their sheet economics differ completely. The sheet size is a
property of the *price offering*, not the material itself. Putting it on `Material` would conflate the domain model with
pricing concerns and prevent multiple pricelists from coexisting for the same catalog.

*Alternative considered — sheet dimensions on Material:* This would be simpler (one source of truth) but would mean the
domain model knows about printing logistics. It also couldn't handle the case where a printer switches stock sizes or
has multiple presses with different maximum sheet formats.

*Alternative considered — separate `SheetFormat` entity in catalog:* A `SheetFormat(id, width, height)` entity
referenced by ID from the pricing rule. This adds a level of indirection that isn't justified yet — there are only a
handful of standard sheet sizes, and they're unlikely to be shared across rules in complex ways. Can be introduced later
if needed.

### Where to put bleed and gutter

**Chosen: On the pricing rule**

Bleed and gutter values are on `MaterialSheetPrice`, not derived from the product's `BleedSpec`.

*Why:* The bleed used for imposition (how much extra paper is needed around each piece on the press sheet) is a print
production parameter, not a design parameter. The `BleedSpec` on the product configuration describes the *design
bleed* (how far artwork extends past the trim line). These are conceptually related but can differ in practice — a
printer may impose with 3mm bleed regardless of whether the designer used 3mm or 5mm design bleed, because excess is
simply trimmed. Coupling imposition bleed to design bleed would introduce a fragile dependency between pricing and the
creative specification.

*Alternative considered — derive from BleedSpec:* Would avoid duplication when the values happen to match, but would
make pricing depend on a spec that serves a different purpose (design intent vs. production logistics). Would also mean
that changing the design bleed changes the material price, which is counterintuitive.

### Cutting surcharge: global vs. per-material

**Chosen: Global (`CuttingSurcharge` is material-independent, one per pricelist)**

*Why:* Cutting cost is driven by the cutting equipment and labor, not by the material being cut. A guillotine cut costs
the same whether the sheet is 90gsm or 350gsm paper. Having one global cutting rate keeps the pricelist simple and
matches how print shops think about it.

*Alternative considered — per-material cutting cost:* Would allow different costs for materials that are harder to cut (
e.g., thick board vs. thin paper). Not needed yet — if it becomes necessary, the `CuttingSurcharge` rule can be extended
to take an optional `materialId` filter without breaking existing pricelists.

*Alternative considered — cutting cost embedded in `MaterialSheetPrice`:* Would reduce the number of rule types but
would duplicate the cutting cost across every sheet-priced material. Would also prevent the cutting cost from applying
uniformly when it changes.

### Minimum unit price: embedded vs. separate rule

**Chosen: `minUnitPrice` field on `MaterialSheetPrice`**

*Why:* The minimum price floor is fundamentally tied to sheet-based pricing — it only makes sense when the unit price is
*derived* from sheet nesting. For flat-rate or area-based pricing, the unit price is already explicitly set by the
pricelist author. A separate `MinimumUnitPrice` rule type would add complexity (another rule to match, another error
case if missing) for a concept that has no meaning outside sheet pricing. Embedding it keeps related parameters together
and makes the rule self-contained.

*Alternative considered — separate `MinimumUnitPrice(materialId, floor)` rule:* More flexible (could apply to any
pricing mode) but over-general for the current need. Would also require coordinating two rules for every sheet-priced
material.

### Nesting algorithm: grid-only vs. mixed layouts

**Chosen: Simple grid with two orientations**

The algorithm tries only uniform grid layouts in two orientations (normal and 90-degree rotated) and picks the better
one.

*Why:* This covers the vast majority of real-world imposition scenarios. Commercial print shops almost always use
uniform grid layouts for identical pieces because they're simple to set up, cut, and quality-control. The algorithm is
deterministic, easy to verify, and produces results that match industry expectations.

*Alternative considered — mixed-orientation nesting:* Some items could fit more efficiently if some are placed normally
and others rotated within the same sheet (e.g., alternating rows). This is a variant of the 2D bin-packing problem,
which is NP-hard in general. The marginal improvement (typically 0-2 extra pieces on edge cases) doesn't justify the
complexity, non-determinism risks, or difficulty of explaining the result to users.

*Alternative considered — guillotine-cut constraint:* Real cutting typically uses guillotine cuts (straight edge-to-edge
cuts), which constrains layouts beyond a simple grid. However, for uniform rectangular items on a regular grid, all
layouts are naturally guillotine-compatible. This constraint only becomes relevant for mixed-size impositions, which are
out of scope.

### Cutting line: per-component vs. global

**Chosen: Per-component (`cuttingLine` on `ComponentBreakdown`)**

*Why:* Different components (cover vs. body) may use different materials with different sheet pricing. A booklet cover
on thick card stock and body pages on thin paper would have different nesting counts and therefore different cutting
costs. Putting the cutting line on each component keeps the breakdown accurate and consistent with how `materialLine`
and `inkConfigLine` already work per-component.

*Alternative considered — single global cutting line on `PriceBreakdown`:* Simpler output structure but would either
require summing across components (losing per-component detail) or arbitrarily picking one component's cutting cost.
Would also break the pattern established by the existing per-component breakdown structure.
