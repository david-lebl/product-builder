# Domain Model — Gap Analysis (Updated 2026-02-19)

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
FinishType (21): Lamination, Overlamination, UVCoating, AqueousCoating, SoftTouchCoating,
  Varnish, Embossing, Debossing, FoilStamping, Thermography, EdgePainting,
  DieCut, ContourCut, KissCut, Scoring, Perforation, RoundCorners, Drilling,
  Numbering, Binding, Mounting, Grommets, Hem
FinishSide: Front, Back, Both
Finish: id, name, finishType, side

// Specifications
SpecKind: Size, Quantity, ColorMode, Orientation, Bleed, Pages, FoldType, BindingMethod
FoldType: Half, Tri, Gate, Accordion, ZFold, RollFold, FrenchFold, CrossFold
BindingMethod: SaddleStitch, PerfectBinding, SpiralBinding, WireOBinding, CaseBinding
ColorMode: CMYK, PMS, Grayscale

// Printing
PrintingProcessType: Offset, Digital, Letterpress, ScreenPrint, UVCurableInkjet, LatexInkjet, SolventInkjet
PrintingMethod: id, name, processType, maxColorCount

// Categories
ProductCategory: id, name, allowedMaterialIds, allowedFinishIds, requiredSpecKinds, allowedPrintingMethodIds

// Rules (13 variants)
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

// Predicates (boolean algebra)
ConfigurationPredicate: Spec, HasMaterialProperty, HasMaterialFamily, HasPrintingProcess,
  HasMinWeight, And, Or, Not
SpecPredicate: MinDimension, MaxDimension, MinQuantity, MaxQuantity, AllowedColorModes,
  AllowedBindingMethods, AllowedFoldTypes, MinPages, MaxPages
```

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
| **No ink configuration** | 4/0, 4/4, 1/0, 4/1, CMYK+PMS, CMYK+White are not modeled | A 4/0 flyer costs roughly half a 4/4 flyer. `ColorMode` alone doesn't capture print sides or spot color additions. This is the single biggest pricing gap for small-format | **High** |
| **No cover vs body distinction** | Booklets need separate cover material/weight from body pages | Can't properly configure a booklet with 300gsm cover and 120gsm body | High |
| **No adhesive type** | Permanent vs removable for stickers | Only relevant if stickers are in scope | Low |
| **No NCR parts/plies** | 2-part, 3-part, 4-part carbonless forms | Niche product type | Low |

### Category Model

| Gap | Issue | Impact | Priority |
|---|---|---|---|
| **No multi-component products** | Booklets/catalogs need cover + body with different materials, weights, and finishes | Currently one material per configuration — blocks realistic booklet configuration | High |
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
| 1 | **Ink configuration** | A 4/0 vs 4/4 distinction is fundamental to every print quote. Affects pricing (halves material cost for single-sided), finish applicability, and is expected by every print buyer. |
| 2 | **Multi-component products** | Booklets and catalogs need cover + body with independent material, weight, finish, and page count. Currently impossible with single-material configurations. |
| 3 | **Material surface/coating** | Every print shop distinguishes Coated Gloss / Coated Silk / Coated Matte / Uncoated as separate substrate lines. Our `Glossy`/`Matte` properties blur intrinsic surface with applied finish. |

### Medium Priority (improves correctness and range)

| # | Gap | Why |
|---|-----|-----|
| 4 | **Finish parameters** | Foil color (gold/silver/custom), corner radius, perforation pitch — needed for accurate quoting. Lamination variants already work via separate finish instances. |
| 5 | **Missing material families** | `Synthetic` and `Adhesive` would let rules target these substrates generically instead of by individual material ID. |
| 6 | **Conditional finish dependencies** | "Add scoring when folding heavy stock" — currently can only reject, not suggest. |

### Lower Priority (nice-to-have for small-format)

| # | Gap | Why |
|---|-----|-----|
| 7 | Material format (sheet/roll) | Always sheets for small-format; relevant for waste/pricing later |
| 8 | Missing material properties | `SelfAdhesive`, `TearResistant` — needed when stickers/synthetic are in scope |
| 9 | Auto-suggestion rules | UX improvement, not a data model gap |
| 10 | Adhesive type, NCR plies | Niche products, can defer |

---

## Previously Identified Gaps (Now Resolved)

The following gaps from the original analysis have been implemented:

- **Finish types**: 21 variants now cover Debossing, Scoring, Perforation, RoundCorners, Drilling, Numbering, AqueousCoating, SoftTouchCoating, Binding, KissCut, ContourCut, EdgePainting, Grommets, Hem, Mounting, Overlamination, Thermography
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