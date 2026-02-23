# Domain Model — Gap Analysis (Updated 2026-02-23)

Comparison of our current domain model against real-world printing domain requirements.
Focus: **small-format sheet printing** (business cards, flyers, brochures, booklets, calendars).

---

## Current Model Summary

```scala
// Materials
MaterialFamily: Paper, Vinyl, Cardboard, Fabric
MaterialProperty: Recyclable, WaterResistant, Glossy, Matte, Textured, SmoothSurface
PaperWeight: opaque Int (1–2000 gsm)
Material: id, name, family, weight (Option[PaperWeight]), properties

// Finishes
FinishCategory: Surface, Decorative, Structural, LargeFormat  // derived from FinishType
FinishType (23): Lamination, Overlamination, UVCoating, AqueousCoating, SoftTouchCoating,
  Varnish, Embossing, Debossing, FoilStamping, Thermography, EdgePainting,
  DieCut, ContourCut, KissCut, Scoring, Perforation, RoundCorners, Drilling,
  Numbering, Binding, Mounting, Grommets, Hem
FinishSide: Front, Back, Both
Finish: id, name, finishType, side

// Specifications
SpecKind: Size, Quantity, Orientation, Bleed, Pages, FoldType, BindingMethod
FoldType: Half, Tri, Gate, Accordion, ZFold, RollFold, FrenchFold, CrossFold
BindingMethod: SaddleStitch, PerfectBinding, SpiralBinding, WireOBinding, CaseBinding
InkType: CMYK, PMS, Grayscale
InkSetup: inkType, colorCount
InkConfiguration: front (InkSetup), back (InkSetup)  // e.g. 4/0, 4/4, 4/1

// Printing
PrintingProcessType: Offset, Digital, Letterpress, ScreenPrint, UVCurableInkjet, LatexInkjet, SolventInkjet
PrintingMethod: id, name, processType, maxColorCount

// Categories
ProductCategory: id, name, components (List[ComponentTemplate]), requiredSpecKinds, allowedPrintingMethodIds

// Rules (14 variants)
CompatibilityRule:
  MaterialFinishIncompatible(materialId, finishId, reason)
  MaterialRequiresFinish(materialId, requiredFinishIds, reason)
  FinishRequiresMaterialProperty(finishId, requiredProperty, reason)
  FinishMutuallyExclusive(finishIdA, finishIdB, reason)
  SpecConstraint(categoryId, predicate, reason)
  MaterialPropertyFinishTypeIncompatible(property, finishType, reason)
  MaterialFamilyFinishTypeIncompatible(family, finishType, reason)
  MaterialWeightFinishType(finishType, minWeightGsm, reason)
  FinishTypeMutuallyExclusive(finishTypeA, finishTypeB, reason)
  FinishCategoryExclusive(category, reason)
  FinishRequiresFinishType(finishId, requiredFinishType, reason)
  FinishRequiresPrintingProcess(finishType, requiredProcessTypes, reason)
  ConfigurationConstraint(categoryId, predicate, reason)
  TechnologyConstraint(predicate, reason)           // NEW: global, not category-scoped

// Predicates (boolean algebra)
ConfigurationPredicate: Spec, HasMaterialProperty, HasMaterialFamily, HasPrintingProcess,
  HasMinWeight, AllowedInkTypes, MaxColorCountPerSide, BindingMethodIs, And, Or, Not
SpecPredicate: MinDimension, MaxDimension, MinQuantity, MaxQuantity,
  AllowedBindingMethods, AllowedFoldTypes, MinPages, MaxPages, PagesDivisibleBy
```

---

## Sample Catalog — Coverage vs. Analysis Document

### What Is in the Sample Catalog

| Category | Status | Notes |
|---|---|---|
| **Business Cards** | ✅ In catalog | Size 50–100mm × 25–60mm; qty min 100; digital + letterpress only |
| **Flyers / Leaflets** | ✅ In catalog | A6–A3 range; digital only (offset planned) |
| **Brochures** | ✅ In catalog | Folded; digital only; fold type required |
| **Banners** | ✅ In catalog | Vinyl; UV inkjet; min 300×200mm; CMYK only |
| **Packaging** | ✅ In catalog | Kraft, corrugated, yupo; offset + digital |
| **Booklets** | ✅ In catalog | Multi-component (cover + body); saddle stitch/perfect/spiral/wire-o; 8–96 pages |
| **Calendars** | ✅ In catalog | Multi-component; spiral/wire-o; 12–28 pages |
| **Postcards** | ✅ In catalog | Heavy coated + silk; offset + digital; lamination, UV, foil options |
| **Stickers & Labels** | ✅ In catalog | Adhesive stock + yupo; digital + UV inkjet; kiss cut, die cut |
| **Free Configuration** | ✅ In catalog | All materials, all finishes; all printing methods |
| **Letterheads** | ❌ Not in catalog | A4; uncoated 80–120gsm; no lamination |
| **Envelopes** | ❌ Not in catalog | DL/C5/C4; no folding, no lamination |
| **NCR Forms** | ❌ Not in catalog | Carbonless multi-part; perforation; niche |
| **Posters** | ❌ Not in catalog | Large format A3–A0+; wide-format inkjet |
| **Notepads** | ❌ Not in catalog | Glue-bound; uncoated bond; 50+ sheets |
| **Tickets / Vouchers** | ❌ Not in catalog | Perforated / numbered; niche |
| **Greeting Cards** | ❌ Not in catalog | Folded + envelopes; premium finishes |
| **Presentation Folders** | ❌ Not in catalog | Pocketed; A4; die-cut |

### What Is in the Sample Materials

| Material | In Catalog | Notes |
|---|---|---|
| Coated Art Paper Glossy (90–350gsm) | ✅ | 8 weight variants |
| Coated Art Paper Matte (90–350gsm) | ✅ | 9 weight variants |
| Coated Silk/Satin 250gsm | ✅ | Single variant |
| Uncoated Bond/Writing 120gsm | ✅ | Single variant |
| Kraft Paper 250gsm | ✅ | Brown/natural; eco |
| Adhesive Stock 100gsm | ✅ | Used in stickers category |
| Cotton Paper 300gsm | ✅ | Premium; letterpress |
| Coated Art Paper 300gsm (generic) | ✅ | Legacy generic; used in several categories |
| Vinyl (Adhesive) | ✅ | Large format banners |
| Corrugated Cardboard | ✅ | Packaging |
| Yupo Synthetic 200μm | ✅ | Waterproof; stickers + business cards |
| Textured Linen / Felt / Laid | ❌ | Not in catalog |
| Recycled Paper | ❌ | Not in catalog |
| Translucent / Vellum | ❌ | Not in catalog |
| Carbonless (NCR) | ❌ | Niche; not in catalog |
| PVC Vinyl Banner (13–18oz) | ❌ | Large format; not modeled separately |

### What Is in the Sample Finishes

| Finish | In Catalog | Notes |
|---|---|---|
| Matte Lamination | ✅ | Both sides |
| Gloss Lamination | ✅ | Both sides |
| Soft-Touch Coating | ✅ | Both sides |
| UV Coating (Spot/Flood) | ✅ | Front only |
| Aqueous Coating | ✅ | Both sides; offset-only rule |
| Spot Varnish | ✅ | Requires lamination base rule |
| Embossing | ✅ | Requires ≥250gsm + smooth surface |
| Debossing | ✅ | Requires ≥250gsm |
| Foil Stamping | ✅ | Requires smooth surface |
| Die Cut | ✅ | Custom shapes |
| Kiss Cut | ✅ | Sticker sheets; cuts face not backing |
| Scoring | ✅ | Fold preparation |
| Perforation | ✅ | Tear-off lines |
| Round Corners | ✅ | Radius corners |
| Grommets | ✅ | Banner hanging hardware |
| Thermography | ❌ | Not in catalog (FinishType exists) |
| Edge Painting | ❌ | Not in catalog (FinishType exists) |
| Drilling / Hole Punching | ❌ | Not in catalog (FinishType exists) |
| Hem / Pole Pocket | ❌ | Not in catalog (FinishType exists) |
| Contour Cut | ❌ | Not in catalog (FinishType exists) |

### Binding Methods Coverage

| Binding | In Catalog Rules | Constraints |
|---|---|---|
| **Saddle Stitch** | ✅ | Pages divisible by 4; ≤80 pages on ≥300gsm stock |
| **Perfect Binding** | ✅ | Pages divisible by 2 |
| **Spiral Binding** | ✅ | Pages divisible by 2 |
| **Wire-O Binding** | ✅ | Pages divisible by 2 |
| **Case Binding** | ❌ | No constraints defined (booklets reject it) |
| **Stapling (corner/side)** | ❌ | Not modeled |
| **Padding / Glue Binding** | ❌ | Not modeled |

---

## Remaining Gaps

### Material Model

| Gap | Issue | Impact | Priority |
|---|---|---|---|
| **No surface/coating attribute** | Coated Gloss vs Coated Silk vs Coated Matte are distinct paper stocks; our `Glossy`/`Matte` properties conflate intrinsic surface with applied finish | Can't distinguish "Coated Silk 300gsm" from "Coated Gloss 300gsm" as different substrates. Need `SurfaceCoating` enum (Gloss, Silk, Matte, Uncoated) or similar | High |
| **Missing families** | `Synthetic` (Yupo is modeled as Paper), `Adhesive` (sticker stock is Paper) — semantically wrong | Rules can't target synthetic or adhesive materials by family; workaround is material properties | Medium |
| **Missing properties** | No `TearResistant`, `SelfAdhesive`, `Transparent`, `Carbonless` | Can't express constraints like "self-adhesive can't be laminated" generically | Medium |
| **No thickness** | Rigid substrates (foam board, acrylic) measured in mm not gsm | Not relevant for small-format sheet printing | Low |
| **No material format** | Sheet vs roll vs rigid board distinction | Small-format is always sheets; matters for pricing (waste calculation) | Low |
| **Weight naming** | `PaperWeight` is paper-specific | Cosmetic — works fine as optional field | Low |

### Finish Model

| Gap | Issue | Impact | Priority |
|---|---|---|---|
| **No finish parameters** | Foil color, corner radius, grommet spacing are not modeled | Affects quoting accuracy — gold foil vs silver foil have different costs. Lamination variants are handled via separate `Finish` instances (matte-lam, gloss-lam), which works | Medium |
| **FinishSide too limited** | Doesn't cover edge (edge painting), spine, or "all edges" (grommets) | Adequate for small-format sheets (Front/Back/Both suffices) | Low |

### Specification Model

| Gap | Issue | Impact | Priority |
|---|---|---|---|
| **No adhesive type** | Permanent vs removable for stickers | Only relevant if stickers are in scope | Low |
| **No NCR parts/plies** | 2-part, 3-part, 4-part carbonless forms | Niche product type | Low |

### Category Model

| Gap | Issue | Impact | Priority |
|---|---|---|---|
| **Missing categories** | Letterheads, Envelopes, NCR Forms, Notepads, Greeting Cards, Tickets/Vouchers, Posters, Presentation Folders not in sample catalog | Real-world shops offer these; each needs category rules | Medium |
| **No weight constraints per category** | "Business cards must be ≥250gsm" expressed via `ConfigurationConstraint` + `HasMinWeight` predicate | Works via rules but verbose; a category-level `minWeight`/`maxWeight` field would be cleaner | Low |
| **No format classification** | Small-format vs large-format distinction on category | Could help with UI filtering; currently implicit via allowed printing methods | Low |

### Rules Model

| Gap | Issue | Impact | Priority |
|---|---|---|---|
| **No conditional finish dependencies** | "Scoring required for folding IF weight ≥170gsm" needs a condition | `ConfigurationConstraint` with `And(HasMinWeight, ...)` can express the condition, but can't auto-add scoring — only reject | Medium |
| **No auto-suggestion rules** | Rules can reject but can't recommend ("you should add scoring") | UI can only show errors, not helpful suggestions | Low |

---

## Summary: Prioritized Gaps for Small-Format Sheets

### High Priority (blocks realistic product configuration/pricing)

| # | Gap | Why |
|---|-----|-----|
| 1 | **Material surface/coating** | Every print shop distinguishes Coated Gloss / Coated Silk / Coated Matte / Uncoated as separate substrate lines. Our `Glossy`/`Matte` properties blur intrinsic surface with applied finish. |

### Medium Priority (improves correctness and range)

| # | Gap | Why |
|---|-----|-----|
| 2 | **Finish parameters** | Foil color (gold/silver/custom), corner radius, perforation pitch — needed for accurate quoting. Lamination variants already work via separate finish instances. |
| 3 | **Missing material families** | `Synthetic` and `Adhesive` would let rules target these substrates generically instead of by individual material ID. |
| 4 | **Conditional finish dependencies** | "Add scoring when folding heavy stock" — currently can only reject, not suggest. |
| 5 | **Missing categories** | Letterheads, Envelopes, Notepads, Greeting Cards, Tickets/Vouchers are common products that should be in the catalog. |

### Lower Priority (nice-to-have for small-format)

| # | Gap | Why |
|---|-----|-----|
| 6 | Material format (sheet/roll) | Always sheets for small-format; relevant for waste/pricing later |
| 7 | Missing material properties | `SelfAdhesive`, `TearResistant` — needed when stickers/synthetic are in scope |
| 8 | Auto-suggestion rules | UX improvement, not a data model gap |
| 9 | Adhesive type, NCR plies | Niche products, can defer |

---

## Previously Identified Gaps (Now Resolved)

The following gaps from the original analysis have been implemented:

- **Finish types**: 23 variants now cover Debossing, Scoring, Perforation, RoundCorners, Drilling, Numbering, AqueousCoating, SoftTouchCoating, Binding, KissCut, ContourCut, EdgePainting, Grommets, Hem, Mounting, Overlamination, Thermography
- **Finish categories**: `FinishCategory` enum (Surface, Decorative, Structural, LargeFormat) derived from `FinishType`
- **Printing methods**: `PrintingMethod` with `PrintingProcessType` (7 variants) and `maxColorCount`
- **Fold types**: `ZFold`, `RollFold`, `FrenchFold`, `CrossFold` added
- **Binding spec**: `BindingMethod` enum with 5 variants (SaddleStitch, PerfectBinding, SpiralBinding, WireOBinding, CaseBinding)
- **Weight-based rules**: `MaterialWeightFinishType(finishType, minWeightGsm, reason)`
- **Printing method rules**: `FinishRequiresPrintingProcess(finishType, requiredProcessTypes, reason)`
- **Finish dependency rules**: `FinishRequiresFinishType(finishId, requiredFinishType, reason)`
- **Property/family-based rules**: `MaterialPropertyFinishTypeIncompatible`, `MaterialFamilyFinishTypeIncompatible`, `FinishTypeMutuallyExclusive`
- **Conditional rules**: `ConfigurationConstraint` with `ConfigurationPredicate` boolean algebra (And/Or/Not/HasMinWeight/HasMaterialProperty/HasMaterialFamily/HasPrintingProcess)
- **Category printing methods**: `allowedPrintingMethodIds` on `ProductCategory`
- **Finish category exclusion**: `FinishCategoryExclusive` rule variant
- **Ink configuration**: `InkType`/`InkSetup`/`InkConfiguration` replace `ColorMode`. Per-side ink setup (e.g., 4/0, 4/4, 4/1) with `InkConfigurationFactor` pricing rule. Structural validation checks `maxColorCount` against `PrintingMethod.maxColorCount`
- **Multi-component products**: `ComponentTemplate` and `ComponentRole` allow booklets/calendars with separate cover and body components, each with independent materials and finishes
- **Technology constraints**: `TechnologyConstraint` rule variant applies globally regardless of category (e.g. binding page divisibility). `BindingMethodIs` ConfigurationPredicate and `PagesDivisibleBy` SpecPredicate support cross-cutting binding rules
- **Binding validation**: Saddle stitch requires pages divisible by 4; other bindings (perfect, spiral, wire-o) require pages divisible by 2; saddle stitch on ≥300gsm stock limited to 80 pages
- **New categories**: Postcards (heavy coated papers, offset+digital) and Stickers & Labels (adhesive stock + yupo, kiss cut, digital + UV inkjet) added to sample catalog
- **KissCut finish**: `fin-kiss-cut` added to the sample catalog for sticker/label products