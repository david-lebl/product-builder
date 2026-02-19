# Multi-Component Product Configuration — Design & Implementation

## Status: ✅ Implemented

This document describes the multi-component product configuration system that enables booklets, catalogs, calendars, and similar bound products to have **independent material, weight, finish, and ink configuration per component** (e.g., cover vs body pages).

## Problem

Booklets, catalogs, and similar bound products require **independent material, weight, finish, and ink configuration per component** (e.g., cover vs body pages). The original `ProductConfiguration` supported only a single material and set of finishes, making realistic booklet/catalog configuration impossible.

### Example: A typical booklet

| Component | Material | Weight | Finishes | Ink Config |
|-----------|----------|--------|----------|------------|
| **Cover** | Coated Art Paper | 300gsm | Matte lamination, Spot UV | 4/4 |
| **Body** | Uncoated Bond | 120gsm | None | 4/4 |

---

## Design Goals

1. **Backward compatible** — Single-component products (business cards, flyers, banners) continue to work unchanged.
2. **Minimal API surface change** — Keep the existing `ConfigurationRequest` → `ProductConfiguration` flow.
3. **Independent validation** — Each component validates its own material/finish constraints independently.
4. **Independent pricing** — Each component has its own pricing line items.
5. **Composable with existing rules** — Reuse `CompatibilityRule` evaluation per component.

---

## Implemented Model

### New Types (model/component.scala)

```scala
enum ComponentRole:
  case Cover, Body

final case class ProductComponent(
    role: ComponentRole,
    material: Material,
    finishes: List[Finish],
    inkConfiguration: Option[InkConfiguration],
)

final case class ComponentRequest(
    role: ComponentRole,
    materialId: MaterialId,
    finishIds: List[FinishId],
    inkConfiguration: Option[InkConfiguration],
)
```

### Updated `ProductConfiguration`

```scala
final case class ProductConfiguration(
    id: ConfigurationId,
    category: ProductCategory,
    material: Material,                         // primary material (= Cover for multi-component)
    printingMethod: PrintingMethod,
    finishes: List[Finish],                     // primary finishes (= Cover for multi-component)
    specifications: ProductSpecifications,
    components: List[ProductComponent] = Nil,   // empty for single-component products
)
```

### Updated `ProductCategory`

```scala
final case class ProductCategory(
    id: CategoryId,
    name: LocalizedString,
    allowedMaterialIds: Set[MaterialId],
    allowedFinishIds: Set[FinishId],
    requiredSpecKinds: Set[SpecKind],
    allowedPrintingMethodIds: Set[PrintingMethodId],
    componentRoles: Set[ComponentRole] = Set.empty,
    allowedMaterialIdsByRole: Map[ComponentRole, Set[MaterialId]] = Map.empty,
)
```

### Updated `ConfigurationRequest`

```scala
final case class ConfigurationRequest(
    categoryId: CategoryId,
    materialId: MaterialId,
    printingMethodId: PrintingMethodId,
    finishIds: List[FinishId],
    specs: List[SpecValue],
    components: List[ComponentRequest] = Nil,   // empty for single-component
)
```

---

## Validation

### Structural Validation (multi-component)

1. All required roles from `category.componentRoles` must be present in the request
2. Each component's material is checked against `allowedMaterialIdsByRole(role)` (falls back to `allowedMaterialIds`)
3. Each component's finishes are checked against `allowedFinishIds`
4. Each component must have an ink configuration
5. Each component's ink config is validated against the printing method's `maxColorCount`
6. Shared spec requirements (Size, Quantity, Pages, BindingMethod) are checked at the configuration level
7. `InkConfig` is excluded from shared spec checks for multi-component (it's per-component)

### Rule Evaluation (multi-component)

Material/finish compatibility rules (`CompatibilityRule`) evaluate once per component against that component's material and finishes. Spec-level rules (SpecConstraint) continue to evaluate at the shared specification level.

### New Error Variants

| Error | When |
|-------|------|
| `MissingRequiredComponent(categoryId, role)` | A required component role is missing |
| `InvalidComponentMaterial(categoryId, role, materialId)` | Component material not allowed for role |
| `MissingComponentInkConfig(role)` | Component has no ink configuration |
| `ComponentInkConfigExceedsMethodColorLimit(role, pmId, inkConfig, max)` | Component ink exceeds printing method limits |

---

## Pricing

### Multi-Component Pricing

For multi-component products, each component contributes its own material, ink, and finish line items:

```scala
final case class ComponentLineItems(
    role: ComponentRole,
    materialLine: LineItem,
    inkConfigLine: Option[LineItem],
    finishLines: List[LineItem],
)
```

**Body material scaling**: The body material cost is multiplied by the number of body sheets derived from the page count:
- Cover: 1 sheet per unit (4 cover pages)
- Body: `(totalPages - 4) / 2` sheets per unit

**Updated `PriceBreakdown`** includes `componentLines: List[ComponentLineItems]` (empty for single-component products).

### Worked Example: 32-page Booklet

Configuration: 500× Booklet, Cover: Coated 300gsm + Matte Lamination, Body: Uncoated Bond 120gsm

```
Cover material: Coated 300gsm     $0.12 × 1 sheet  × 500 =  $60.00
Body material:  Uncoated Bond     $0.06 × 14 sheets × 500 = $420.00
  (32 pages - 4 cover = 28 body pages / 2 = 14 sheets)
Cover finish:   Matte Lamination  $0.03 × 500              =  $15.00
                                                    ─────────────────
Subtotal                                                     $495.00
Quantity tier (250–999)                                    ×    0.90
                                                    ─────────────────
Total                                                        $445.50
```

---

## CatalogQueryService

New role-aware methods:

```scala
def availableMaterialsForRole(categoryId, role, catalog): List[Material]
def compatibleFinishesForRole(categoryId, role, materialId, catalog, ruleset, printingMethodId): List[Finish]
```

These respect `allowedMaterialIdsByRole` overrides and fall back to the category-level `allowedMaterialIds` when no role-specific override exists.

---

## Sample Data

### Booklets Category (multi-component)

- `componentRoles = Set(Cover, Body)`
- Cover materials: Coated 300gsm, Coated Silk 250gsm
- Body materials: Uncoated Bond, Coated 300gsm, Coated Silk 250gsm
- Shared specs: Size, Quantity, Pages, BindingMethod (SaddleStitch, PerfectBinding)
- Binding: SaddleStitch, PerfectBinding

### Calendars Category (multi-component)

- `componentRoles = Set(Cover, Body)`
- Cover materials: Coated 300gsm, Coated Silk 250gsm
- Body materials: Coated 300gsm, Coated Silk 250gsm, Uncoated Bond
- Shared specs: Size, Quantity, Pages, BindingMethod (SpiralBinding, WireOBinding)

---

## UI Integration

The UI automatically detects multi-component categories and:

1. Hides the single-component Material/Finish/Ink selectors
2. Shows separate **Component Editor** panels for Cover and Body
3. Each editor has its own Material, Finishes, and Ink Configuration selectors
4. Shared specs (Size, Quantity, Pages, Binding Method) remain at the top level
5. Price preview shows breakdown by component

---

## Test Coverage

- **14 new tests** added across ConfigurationBuilder, PriceCalculator, and CatalogQueryService
- All 118 tests pass (7 existing booklet/calendar tests updated for multi-component)
- Tests cover: valid multi-component builds, missing components, invalid materials, missing ink configs, ink config limits, role-aware queries, component pricing, backward compatibility