# Domain Model — Gap Analysis

Comparison of our current domain model against the printing domain analysis.

---

## Material Model

### Current
```scala
MaterialFamily: Paper, Vinyl, Cardboard, Fabric
MaterialProperty: Recyclable, WaterResistant, Glossy, Matte, Textured, SmoothSurface
Material: id, name, family, weight (Option[PaperWeight]), properties
```

### Gaps

| Gap | Issue | Impact |
|---|---|---|
| **Missing families** | No `Synthetic`, `Adhesive`, `Carbonless`, `Acrylic`, `Metal`, `Wood` | Can't model Yupo, stickers, NCR, premium sign materials |
| **No coating/surface type** | Coated Gloss vs Coated Silk vs Coated Matte are distinct substrates — our properties `Glossy`/`Matte` overlap with finish semantics | Material surface is intrinsic; finish is applied. Need `SurfaceCoating` enum (Gloss, Silk, Matte, Uncoated) |
| **No thickness for non-paper** | Rigid substrates (foam board, acrylic, Dibond) measured in mm not gsm | Need `thickness: Option[Thickness]` alongside weight |
| **Missing properties** | No `TearResistant`, `Printable`, `Transparent`, `SelfAdhesive`, `Carbonless`, `Rigid` | Can't express key constraints (e.g. "NCR cannot be laminated", "rigid requires UV-cure") |
| **No material format** | Sheet vs roll vs rigid board distinction missing | Affects what printing methods and finishes are possible |
| **Weight naming** | `PaperWeight` is too paper-specific for vinyl, foam board, etc. | Rename to `SubstrateWeight` or keep as-is but clearly optional |

---

## Finish Model

### Current
```scala
FinishType: Lamination, UVCoating, Embossing, FoilStamping, Varnish, DieCut
FinishSide: Front, Back, Both
Finish: id, name, finishType, side
```

### Gaps

| Gap | Issue | Impact |
|---|---|---|
| **Missing finish types** | No `Debossing`, `Scoring`, `Folding`, `Perforation`, `RoundCorners`, `Drilling`, `Numbering`, `AqueousCoating`, `SoftTouchCoating`, `Binding`, `KissCut`, `ContourCut`, `EdgePainting`, `Grommets`, `Hem`, `Mounting`, `Overlamination`, `Thermography`, `Letterpress` | Can't model brochures (folding), booklets (binding), stickers (kiss-cut), banners (grommets/hem), or many premium finishes |
| **No finish category** | All finishes are flat — no grouping of surface vs structural vs decorative vs large-format | Hard to validate "one lamination type only" generically |
| **No finish parameters** | Lamination has no variant (gloss/matte/soft-touch), foil has no color, fold has no type, binding has no method | Each finish type needs its own parameter data |
| **No minimum weight constraint on finish** | Rules like "lamination requires ≥200gsm" must be expressed externally | Should be on the `Finish` definition or as a dedicated rule type |
| **FinishSide too limited** | Doesn't cover edge (edge painting), spine, top/bottom (hem), or "all edges" (grommets) | Extend or make application area more flexible |

---

## Specification Model

### Current
```scala
SpecKind: Size, Quantity, ColorMode, Orientation, Bleed, Pages, FoldType
SpecValue: SizeSpec, QuantitySpec, ColorModeSpec, OrientationSpec, BleedSpec, PagesSpec, FoldTypeSpec
ColorMode: CMYK, PMS, Grayscale
FoldType: Half, Tri, Gate, Accordion
```

### Gaps

| Gap | Issue | Impact |
|---|---|---|
| **No printing method** | Offset, Digital, Large-format inkjet, Screen, Letterpress are missing | Can't validate method→material or method→finish rules |
| **No ink configuration** | 4/0, 4/4, 1/0, 4/1, CMYK+PMS, CMYK+White are missing | Color mode alone doesn't capture sides or spot additions |
| **Missing fold types** | No `ZFold`, `RollFold`, `FrenchFold`, `CrossFold` | Incomplete brochure options |
| **No binding spec** | `BindingMethod` (saddle stitch, perfect, spiral, wire-o, case) is missing | Can't configure booklets/catalogs |
| **No adhesive type** | Permanent vs removable for stickers | Can't configure sticker products |
| **No parts/plies spec** | NCR forms need 2-part, 3-part, 4-part | Can't configure NCR |
| **No cover vs body distinction** | Booklets have separate cover material/weight from body pages | Need separate material selection for cover and body |
| **No finishing parameters in specs** | Grommet spacing, hem size, corner radius, foil color aren't specs | These are finish-level parameters, not product specs |

---

## Category Model

### Current
```scala
ProductCategory: id, name, allowedMaterialIds, allowedFinishIds, requiredSpecKinds
```

### Gaps

| Gap | Issue | Impact |
|---|---|---|
| **No format classification** | Small-format vs large-format distinction | Affects which printing methods, materials, finishes are relevant |
| **No weight constraints per category** | "Business cards ≥250gsm" can only be expressed as external rules | Category should declare min/max weight range |
| **No multi-component products** | Booklets need cover + body with different materials | Need concept of product "components" (cover, body, insert) |

---

## Rules Model

### Current
```scala
CompatibilityRule:
  MaterialFinishIncompatible(materialId, finishId, reason)
  MaterialRequiresFinish(materialId, requiredFinishIds, reason)
  FinishRequiresMaterialProperty(finishId, requiredProperty, reason)
  FinishMutuallyExclusive(finishIdA, finishIdB, reason)
  SpecConstraint(categoryId, predicate, reason)
```

### Gaps

| Gap | Issue | Impact |
|---|---|---|
| **No weight-based rules** | "Lamination requires ≥200gsm", "Embossing requires ≥250gsm" — can't express | Need `FinishRequiresMinWeight(finishId, minGsm, reason)` or generalize via predicates |
| **No printing method rules** | "Aqueous coating is offset-only", "Letterpress max 2 colors" | Need `PrintMethodMaterialCompatibility`, `PrintMethodFinishCompatibility` |
| **No finish-requires-finish** | "Spot UV requires lamination base", "Folding requires scoring if ≥170gsm" | Need `FinishRequiresFinish(finishId, requiredFinishIds, condition, reason)` |
| **No category-level weight constraint** | "Business cards require cover weight ≥250gsm" | Need `CategoryMaterialConstraint` or predicate-based rule |
| **Rules are ID-specific** | `MaterialFinishIncompatible` uses specific material ID — doesn't generalize to "all textured materials" | Need property-based or family-based rules, not just ID-to-ID |
| **No conditional rules** | "Scoring required for folding IF weight ≥170gsm" — needs condition on when rule applies | `ConfigurationPredicate` exists but isn't wired into rule evaluation |

---

## Summary: Priority Gaps to Address

### High Priority (blocks realistic product configuration)
1. **Printing method** — add to specs and rules (drives material/finish compatibility)
2. **More finish types** — at minimum: Scoring, Folding, Binding, Perforation, RoundCorners, Debossing, AqueousCoating, SoftTouchCoating, Grommets/Hem
3. **Finish parameters** — lamination variant, foil color, binding method, fold type as finish params (not just spec)
4. **Weight-based rules** — "lamination requires ≥200gsm", "embossing ≥250gsm"
5. **Property/family-based rules** — replace some ID-specific rules with generic ones
6. **Finish dependency rules** — "spot UV requires lamination", "folding requires scoring if heavy"

### Medium Priority (needed for full product range)
7. **Missing material families** — Synthetic, Adhesive, Carbonless, Acrylic, Metal
8. **Material surface/coating** as separate attribute from properties
9. **Binding as spec** for booklets/catalogs
10. **Ink configuration** (4/0, 4/4, etc.) beyond simple color mode
11. **Multi-component products** (cover + body)
12. **More fold types** — Z-fold, Roll, French, Cross

### Lower Priority (nice-to-have / future)
13. Material format (sheet/roll/rigid)
14. Finish category grouping (surface/structural/decorative)
15. Large-format specific finishing (contour cut, standoffs)
16. Adhesive type for stickers
17. NCR parts/plies
18. Edge painting, numbering, drilling
