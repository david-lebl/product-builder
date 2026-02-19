# Multi-Component Product Configuration — Design Proposal

## Problem

Booklets, catalogs, and similar bound products require **independent material, weight, finish, and ink configuration per component** (e.g., cover vs body pages). The current `ProductConfiguration` supports only a single material and set of finishes, making realistic booklet/catalog configuration impossible.

### Example: A typical booklet

| Component | Material | Weight | Finishes | Ink Config |
|-----------|----------|--------|----------|------------|
| **Cover** | Coated Art Paper | 300gsm | Matte lamination, Spot UV | 4/4 |
| **Body** | Uncoated Bond | 120gsm | None | 4/4 |

---

## Design Goals

1. **Backward compatible** — Single-component products (business cards, flyers, banners) should continue to work unchanged.
2. **Minimal API surface change** — Keep the existing `ConfigurationRequest` → `ProductConfiguration` flow.
3. **Independent validation** — Each component validates its own material/finish constraints independently.
4. **Independent pricing** — Each component has its own pricing line items.
5. **Composable with existing rules** — Reuse `CompatibilityRule` evaluation per component.

---

## Proposed Model Changes

### New Types

```scala
// Identifies a component role within a multi-component product
enum ComponentRole:
  case Cover, Body

// A single component's configuration (material + finishes + ink)
final case class ProductComponent(
    role: ComponentRole,
    material: Material,
    finishes: List[Finish],
    inkConfiguration: Option[InkConfiguration],
)

// Request for a single component
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
    material: Material,                         // primary material (backward compat)
    printingMethod: PrintingMethod,
    finishes: List[Finish],                     // primary finishes (backward compat)
    specifications: ProductSpecifications,
    components: List[ProductComponent],         // NEW: empty for single-component products
)
```

For single-component products (business cards, flyers, etc.), `components` remains empty and the existing `material`/`finishes` fields are used. For multi-component products (booklets, catalogs), the `material`/`finishes` fields represent the cover (or are copied from the first component), while `components` holds the full breakdown.

### Updated `ConfigurationRequest`

```scala
final case class ConfigurationRequest(
    categoryId: CategoryId,
    materialId: MaterialId,                     // primary material (backward compat)
    printingMethodId: PrintingMethodId,
    finishIds: List[FinishId],                  // primary finishes (backward compat)
    specs: List[SpecValue],
    components: List[ComponentRequest],         // NEW: empty for single-component
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
    componentRoles: Set[ComponentRole],         // NEW: empty means single-component
    allowedMaterialIdsByRole: Map[ComponentRole, Set[MaterialId]],  // NEW: per-role overrides
)
```

When `componentRoles` is non-empty, the category expects components. `allowedMaterialIdsByRole` provides per-role material constraints (e.g., cover materials can be heavier stock, body materials lighter).

---

## Validation Changes

### `ConfigurationValidator`

1. **Structural validation** gains a component check:
    - If category has `componentRoles` defined, verify that all required roles are present in `request.components`.
    - For each component, verify material is in `allowedMaterialIdsByRole(role)` (or fall back to `allowedMaterialIds`).
    - For each component, verify finishes are in `allowedFinishIds`.

2. **Rule evaluation** runs per-component:
    - Material/finish compatibility rules evaluate against each component's material+finishes independently.
    - Spec-level rules continue to evaluate at the configuration level (shared specs like size, quantity, binding).

### `RuleEvaluator`

The existing `evaluate` and `evaluateAll` signatures take `material: Material` and `finishes: List[Finish]`. For multi-component, the validator would call these once per component, passing each component's material and finishes.

No changes needed to `RuleEvaluator` itself — the orchestration happens in `ConfigurationValidator`.

---

## Pricing Changes

### `PriceCalculator`

For multi-component products, pricing produces a `LineItem` per component's material + finishes, then sums.

```scala
// NEW
final case class ComponentLineItems(
    role: ComponentRole,
    materialLine: LineItem,
    finishLines: List[LineItem],
    doubleSidedSurcharge: Option[LineItem],
)
```

### Updated `PriceBreakdown`

```scala
final case class PriceBreakdown(
    materialLine: LineItem,                         // primary (backward compat)
    finishLines: List[LineItem],                    // primary (backward compat)
    processSurcharge: Option[LineItem],
    categorySurcharge: Option[LineItem],
    doubleSidedSurcharge: Option[LineItem],         // primary (backward compat)
    componentLines: List[ComponentLineItems],       // NEW: empty for single-component
    subtotal: Money,
    quantityMultiplier: BigDecimal,
    total: Money,
    currency: Currency,
)
```

For single-component products, `componentLines` is empty and the existing fields work as before. For multi-component products, each component contributes its own material and finish line items. The `materialLine` would represent the cover component for backward compatibility.

### Pricing Rule Additions

A new pricing rule variant for component-level material pricing may be needed:

```scala
case ComponentMaterialBasePrice(
    role: ComponentRole,
    materialId: MaterialId,
    unitPrice: Money,
)
```

However, the simpler approach is to reuse existing `MaterialBasePrice` rules — each component's material is priced using the same material price lookup. The body material cost could be multiplied by page count (derived from `PagesSpec`) to reflect the per-page material cost.

---

## CatalogQueryService Changes

New methods for component-aware queries:

```scala
def availableMaterialsForRole(
    categoryId: CategoryId,
    role: ComponentRole,
    catalog: ProductCatalog,
): List[Material]

def compatibleFinishesForRole(
    categoryId: CategoryId,
    role: ComponentRole,
    materialId: MaterialId,
    catalog: ProductCatalog,
    ruleset: CompatibilityRuleset,
    printingMethodId: Option[PrintingMethodId],
): List[Finish]
```

---

## Sample Data Changes

### Updated Booklet Category

```scala
val booklets: ProductCategory = ProductCategory(
    id = bookletsId,
    name = LocalizedString("Booklets", "Brožurky"),
    allowedMaterialIds = Set(coated300gsmId, uncoatedBondId),
    allowedFinishIds = Set(matteLaminationId, glossLaminationId, uvCoatingId, perforationId),
    requiredSpecKinds = Set(SpecKind.Size, SpecKind.Quantity, SpecKind.ColorMode,
                            SpecKind.Pages, SpecKind.BindingMethod),
    allowedPrintingMethodIds = Set(offsetId, digitalId),
    componentRoles = Set(ComponentRole.Cover, ComponentRole.Body),
    allowedMaterialIdsByRole = Map(
        ComponentRole.Cover -> Set(coated300gsmId, coatedSilk250gsmId),
        ComponentRole.Body  -> Set(uncoatedBondId, coated300gsmId),
    ),
)
```

---

## Migration Path

1. **Phase 1** — Add new types (`ComponentRole`, `ProductComponent`, `ComponentRequest`) and new fields with defaults (`components = Nil`, `componentRoles = Set.empty`). All existing code continues to work.
2. **Phase 2** — Update `ConfigurationValidator` to validate components when present.
3. **Phase 3** — Update `PriceCalculator` to price components individually.
4. **Phase 4** — Update `CatalogQueryService` with role-aware queries.
5. **Phase 5** — Update sample data for booklets and calendars with component roles.
6. **Phase 6** — Add comprehensive tests.

Each phase is independently deployable and testable. Existing single-component products are never affected.

---

## Impact Assessment

| Area | Change Size | Risk |
|------|-------------|------|
| `model/` types | Small — additive new types + optional fields | Low |
| `ProductCategory` | Small — two new optional fields | Low |
| `ConfigurationRequest` | Small — one new optional field | Low |
| `ProductConfiguration` | Small — one new optional field | Low |
| `ConfigurationValidator` | Medium — new component validation loop | Medium |
| `PriceCalculator` | Medium — component-level pricing | Medium |
| `CatalogQueryService` | Small — new role-aware methods | Low |
| `SampleCatalog` / `SampleRules` | Small — update booklet/calendar categories | Low |
| Existing tests | None — all pass unchanged | None |
| New tests | Medium — ~20-30 new test cases | N/A |

**Total estimated effort**: Medium. Approximately 200-300 lines of new/changed code spread across 8-10 files, plus ~200 lines of new tests.