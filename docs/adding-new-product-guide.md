# Adding a New Product to the Catalog — Implementation Guide

> Step-by-step checklist of everything that needs to be done when adding a new product
> into the product builder catalog, including implications for the visual editor,
> pricing engine, manufacturing workflow, and UI modules.

---

## Table of Contents

1. [Overview](#1-overview)
2. [Domain Layer Changes](#2-domain-layer-changes)
3. [Pricing Engine Changes](#3-pricing-engine-changes)
4. [Compatibility Rules](#4-compatibility-rules)
5. [Manufacturing Workflow Implications](#5-manufacturing-workflow-implications)
6. [Product Catalog UI](#6-product-catalog-ui)
7. [Product Builder UI](#7-product-builder-ui)
8. [Visual Editor Implications](#8-visual-editor-implications)
9. [Catalog Editor (Admin UI)](#9-catalog-editor-admin-ui)
10. [Testing Checklist](#10-testing-checklist)
11. [Complete Checklist](#11-complete-checklist)

---

## 1. Overview

Adding a new product to the catalog touches multiple layers of the application. The system is designed so that the **domain layer** is the source of truth — the UI components dynamically adapt to whatever categories, materials, finishes, and printing methods are defined in the `ProductCatalog`.

However, some parts of the system use **hardcoded category ID matching** (e.g., the visual editor's overlay rendering and the editor bridge's product type inference), which require explicit updates.

### Architecture Recap

```
ProductCatalog (domain)
├── ProductCategory         → defines allowed materials, finishes, specs, printing methods
├── Material                → substrate / base material
├── Finish                  → post-processing operations
├── PrintingMethod          → how ink is applied
├── CompatibilityRuleset    → rules that constrain valid configurations
└── Pricelist               → pricing rules for cost calculation

ShowcaseProduct (domain)    → marketing content for catalog display
ProductBuilderViewModel (UI) → configuration form driven by catalog
ProductCatalogApp (UI)      → customer-facing catalog grid + detail pages
VisualEditorViewModel (UI)  → visual design editor with product context
WorkflowGenerator (domain)  → manufacturing step generation
```

---

## 2. Domain Layer Changes

### 2.1 Define Category ID

**File**: `modules/domain/src/main/scala/mpbuilder/domain/sample/SampleCatalog.scala`

Add a new `CategoryId` using the opaque type's `unsafe` constructor:

```scala
val newProductId: CategoryId = CategoryId.unsafe("cat-new-product")
```

**Convention**: Category IDs use the `cat-` prefix followed by a kebab-case name.

### 2.2 Define New Materials (if needed)

**File**: `modules/domain/src/main/scala/mpbuilder/domain/sample/SampleCatalog.scala`

If the product requires materials not already in the catalog, define them:

```scala
val newMaterialId: MaterialId = MaterialId.unsafe("mat-new-material")

val newMaterial: Material = Material(
  id = newMaterialId,
  name = LocalizedString("Material Name EN", "Material Name CS"),
  family = MaterialFamily.Paper,  // or Vinyl, Cardboard, Fabric, Hardware
  weight = Some(PaperWeight.unsafe(300)),  // optional, in gsm
  properties = Set(MaterialProperty.Glossy, MaterialProperty.SmoothSurface),
  description = Some(LocalizedString("EN description", "CS description")),
)
```

**Important**: New materials must also be added to the `materials` map in the `ProductCatalog` constructor at the bottom of `SampleCatalog.scala`.

### 2.3 Define New Finishes (if needed)

**File**: `modules/domain/src/main/scala/mpbuilder/domain/sample/SampleCatalog.scala`

```scala
val newFinishId: FinishId = FinishId.unsafe("fin-new-finish")

val newFinish: Finish = Finish(
  id = newFinishId,
  name = LocalizedString("Finish Name EN", "Finish Name CS"),
  finishType = FinishType.Lamination,  // pick from 23 existing types
  side = FinishSide.Both,
  description = Some(LocalizedString("EN description", "CS description")),
)
```

**Important**: New finishes must also be added to the `finishes` map in the `ProductCatalog` constructor.

### 2.4 Define New Printing Methods (if needed)

**File**: `modules/domain/src/main/scala/mpbuilder/domain/sample/SampleCatalog.scala`

```scala
val newMethodId: PrintingMethodId = PrintingMethodId.unsafe("pm-new-method")

val newMethod: PrintingMethod = PrintingMethod(
  id = newMethodId,
  name = LocalizedString("Method EN", "Method CS"),
  processType = PrintingProcessType.Digital,  // 7 existing process types
  maxColorCount = 4,
)
```

**Important**: New printing methods must also be added to the `printingMethods` map in the `ProductCatalog` constructor.

### 2.5 Define the ProductCategory

**File**: `modules/domain/src/main/scala/mpbuilder/domain/sample/SampleCatalog.scala`

```scala
val newProduct: ProductCategory = ProductCategory(
  id = newProductId,
  name = LocalizedString("Product Name EN", "Název produktu CS"),
  components = List(
    ComponentTemplate(
      ComponentRole.Main,  // or Cover, Body, Stand for multi-component
      allowedMaterialIds = Set(mat1Id, mat2Id),
      allowedFinishIds = Set(fin1Id, fin2Id),
    ),
  ),
  requiredSpecKinds = Set(SpecKind.Size, SpecKind.Quantity),
  allowedPrintingMethodIds = Set(pm1Id, pm2Id),
  description = Some(LocalizedString("EN description", "CS description")),
  presets = List(/* see §2.6 */),
)
```

**Key decisions**:

| Decision | Options | Guidance |
|----------|---------|----------|
| **Single vs multi-component** | `Main` only, or `Cover` + `Body` | Use multi-component for products with different substrates (e.g., booklet cover vs inner pages) |
| **Required specs** | `Size`, `Quantity`, `Orientation`, `Pages`, `FoldType`, `BindingMethod` | Only include specs that are meaningful for this product |
| **Printing method restrictions** | Explicit set, or empty set (= all allowed) | Empty set means all printing methods are valid |

### 2.6 Define Category Presets

Presets are pre-configured product variants that customers can select for quick configuration:

```scala
CategoryPreset(
  id = PresetId.unsafe("preset-new-standard"),
  name = LocalizedString("Standard", "Standardní"),
  description = Some(LocalizedString("EN desc", "CS popis")),
  printingMethodId = digitalId,
  componentPresets = List(
    ComponentPreset(
      role = ComponentRole.Main,
      materialId = newMaterialId,
      inkConfiguration = InkConfiguration.cmyk4_4,
      finishSelections = List(FinishSelection(matteLaminationId)),
    ),
  ),
  specOverrides = List(
    SpecValue.SizeSpec(Dimension(90, 50)),
    SpecValue.QuantitySpec(Quantity.unsafe(100)),
  ),
),
```

### 2.7 Register in ProductCatalog

**File**: `modules/domain/src/main/scala/mpbuilder/domain/sample/SampleCatalog.scala`

Add to the `catalog` val at the bottom:

```scala
val catalog: ProductCatalog = ProductCatalog(
  categories = Map(
    // ... existing categories ...
    newProductId -> newProduct,  // ADD THIS
  ),
  materials = Map(
    // ... existing materials ...
    newMaterialId -> newMaterial,  // ADD IF NEW
  ),
  finishes = Map(
    // ... existing finishes ...
    newFinishId -> newFinish,  // ADD IF NEW
  ),
  printingMethods = Map(
    // ... existing methods ...
    newMethodId -> newMethod,  // ADD IF NEW
  ),
)
```

### 2.8 CatalogGroup Extension (if needed)

**File**: `modules/domain/src/main/scala/mpbuilder/domain/model/showcase.scala`

If the product doesn't fit existing groups (`Sheet`, `LargeFormat`, `Bound`, `Specialty`), add a new variant:

```scala
enum CatalogGroup:
  case Sheet, LargeFormat, Bound, Specialty
  case Promotional  // NEW — if adding merchandise products
```

**Impact**: This requires UI updates in `ProductCatalogApp.scala` (see §6).

---

## 3. Pricing Engine Changes

### 3.1 Material Pricing Rules

**File**: `modules/domain/src/main/scala/mpbuilder/domain/sample/SamplePricelist.scala`

Every new material needs a base price rule in **ALL pricelists** (the UI uses `pricelistCzkSheet`):

```scala
// For unit-based pricing:
PricingRule.MaterialBasePrice(newMaterialId, Money("0.12"))

// For area-based pricing (large format):
PricingRule.MaterialAreaPrice(newMaterialId, Money("18.00"))

// For sheet-based pricing:
PricingRule.MaterialSheetPrice(newMaterialId, Money("8"), sheetWidthMm = 640, sheetHeightMm = 450, bleedMm = 3, gutterMm = 5)
```

### 3.2 Category Surcharge (optional)

```scala
PricingRule.CategorySurcharge(newProductId, Money("0.05"))
```

### 3.3 Finish Pricing

New finishes need both per-unit surcharges and one-time setup fees:

```scala
// Per-unit surcharge (applied per item, subject to quantity discount)
PricingRule.FinishSurcharge(newFinishId, Money("0.03"))

// One-time setup fee (NOT subject to quantity discount)
PricingRule.FinishSetupFee(newFinishId, Money("50"))
```

### 3.4 Manufacturing Speed Surcharges

If the product supports express manufacturing, add speed surcharge rules. These **must be present in ALL pricelists** or the price calculator returns `None` for speed surcharges.

### 3.5 Minimum Order Price (optional)

```scala
PricingRule.MinimumOrderPrice(Money("500"))
```

### 3.6 Quantity Tiers

Existing quantity tiers may apply, or new tiers can be defined per-category requirements. Quantity tiers are global (not category-scoped) in the current model.

---

## 4. Compatibility Rules

### 4.1 Material–Finish Rules

**File**: `modules/domain/src/main/scala/mpbuilder/domain/sample/SampleRules.scala`

Add rules that express physical constraints:

```scala
// Example: vinyl can't be embossed
CompatibilityRule.MaterialFamilyFinishTypeIncompatible(
  MaterialFamily.Vinyl,
  FinishType.Embossing,
  "Vinyl material cannot be embossed",
)

// Example: specific material–finish incompatibility
CompatibilityRule.MaterialFinishIncompatible(
  newMaterialId,
  somFinishId,
  "This material is not compatible with this finish",
)
```

### 4.2 Specification Constraints

```scala
// Size constraints
CompatibilityRule.SpecConstraint(
  newProductId,
  SpecPredicate.MinDimension(50, 50),
  "Product minimum size is 50×50mm",
)

// Quantity constraints
CompatibilityRule.SpecConstraint(
  newProductId,
  SpecPredicate.MinQuantity(10),
  "Minimum order quantity is 10",
)
```

### 4.3 Technology Constraints

For cross-cutting rules that apply regardless of category:

```scala
CompatibilityRule.TechnologyConstraint(
  ConfigurationPredicate.And(
    ConfigurationPredicate.HasPrintingProcess(PrintingProcessType.ScreenPrint),
    ConfigurationPredicate.MaxColorCountPerSide(8),
  ),
  "Screen printing supports maximum 8 colors per side",
)
```

---

## 5. Manufacturing Workflow Implications

### 5.1 Station Mapping

**File**: `modules/domain/src/main/scala/mpbuilder/domain/service/WorkflowGenerator.scala`

The `WorkflowGenerator` automatically maps `PrintingProcessType` to `StationType`:

| Process Type | Station | Status |
|-------------|---------|--------|
| Digital | DigitalPrinter | ✅ Already mapped |
| Offset | OffsetPress | ✅ Already mapped |
| ScreenPrint | DigitalPrinter | ⚠️ Currently maps to DigitalPrinter |
| UVCurableInkjet | LargeFormatPrinter | ✅ Already mapped |
| Letterpress | Letterpress | ✅ Already mapped |

**Action needed**: If screen printing for T-shirts/mugs requires a separate station type, the `StationType` enum and `WorkflowGenerator.printingStationFor` need updating. Currently, `ScreenPrint` maps to `DigitalPrinter`, which is not semantically correct for garment printing.

### 5.2 New Station Types (optional)

For promotional products, consider adding:

```scala
enum StationType:
  // ... existing 14 types ...
  case ScreenPrinter    // Screen printing press
  case HeatPress        // Heat transfer / sublimation
  case Embroidery       // Embroidery machine
```

**Impact**: New station types require:
- Updated `WorkflowGenerator` mapping logic
- New station entries in `SampleStations`
- Updated manufacturing UI views (Station Queue, Machines)

### 5.3 Finishing Steps

The `WorkflowGenerator` already handles finish-type-to-station mapping. New finishes using existing `FinishType` variants will automatically generate appropriate workflow steps (e.g., `Mounting` → no additional station, `Overlamination` → Laminator station).

### 5.4 Complete Workflow Example (T-Shirt)

```
Prepress → Screen Printer (or DTG) → Heat Press (if transfer) → Quality Control → Fold & Bag → Packaging
```

---

## 6. Product Catalog UI

### 6.1 Showcase Product Entry

**File**: `modules/domain/src/main/scala/mpbuilder/domain/sample/SampleShowcase.scala`

Add a `ShowcaseProduct` with marketing content:

```scala
val newProduct: ShowcaseProduct = ShowcaseProduct(
  categoryId = SampleCatalog.newProductId,
  group = CatalogGroup.Specialty,  // or new Promotional group
  tagline = LocalizedString("EN tagline", "CS tagline"),
  detailedDescription = LocalizedString("EN description...", "CS popis..."),
  imageUrl = "/images/products/new-product.jpg",
  galleryImageUrls = List("/images/products/new-product-1.jpg"),
  variations = List(
    ProductVariation(
      name = LocalizedString("Standard", "Standardní"),
      description = LocalizedString("EN desc", "CS popis"),
      presetId = Some(PresetId.unsafe("preset-new-standard")),
    ),
  ),
  features = List(
    ProductFeature(
      icon = "🎨",
      title = LocalizedString("Feature EN", "Vlastnost CS"),
      description = LocalizedString("EN desc", "CS popis"),
    ),
  ),
  instructions = Some(LocalizedString(
    "1. Choose material  2. Select printing method  3. Upload design  4. Review and order",
    "1. Vyberte materiál  2. Zvolte tiskovou metodu  3. Nahrajte design  4. Zkontrolujte a objednejte",
  )),
  popularFinishes = List("Matte Lamination", "UV Coating"),
  turnaroundDays = Some("5-7"),
  sortOrder = 15,  // determines position in catalog grid
)
```

**Important**: Add the product to `allProducts` list in `SampleShowcase`:

```scala
val allProducts: List[ShowcaseProduct] = List(
  // ... existing products ...
  newProduct,  // ADD THIS
).sortBy(_.sortOrder)
```

### 6.2 CatalogGroup UI Updates (if new group added)

**File**: `modules/ui/src/main/scala/mpbuilder/ui/productcatalog/ProductCatalogApp.scala`

If a new `CatalogGroup` variant was added:

1. **Tab filter button**: Add a new filter button in the `apply()` method
2. **Group label**: Update `groupLabel` match expression
3. **Badge label**: Update `groupBadgeLabel` match expression

```scala
// In groupLabel:
case Some(CatalogGroup.Promotional) => lang match
  case Language.En => "Promotional Products"
  case Language.Cs => "Propagační produkty"

// In groupBadgeLabel:
case CatalogGroup.Promotional => lang match
  case Language.En => "Promo"
  case Language.Cs => "Promo"
```

### 6.3 Product Images

Product images are referenced by URL. For the sample catalog, placeholder images or stock photo URLs are used. In production, images would be served from a CDN or asset server.

---

## 7. Product Builder UI

### 7.1 Automatic Adaptation

The Product Builder UI is **data-driven** — it reads the `ProductCatalog` and dynamically generates:

- **Category selector dropdown** — automatically includes all categories
- **Material selector** — filtered by category's `allowedMaterialIds`
- **Finish checkboxes** — filtered by category's `allowedFinishIds` + compatibility rules
- **Printing method selector** — filtered by category's `allowedPrintingMethodIds`
- **Specification form** — shows only fields for `requiredSpecKinds`
- **Preset selector** — shows presets defined on the category

**No code changes** are needed in the Product Builder UI for basic product addition.

### 7.2 Component Configuration

If the product has multiple components (e.g., Cover + Body), the `ConfigurationForm` automatically generates separate material/finish sections per component role. The component role labels are defined in `ConfigurationForm.scala` and `PricePreview.scala`.

**Action needed if new ComponentRole added**: Update the `componentRoleLabel` match in:
- `modules/ui/src/main/scala/mpbuilder/ui/productbuilder/components/ConfigurationForm.scala`
- `modules/ui/src/main/scala/mpbuilder/ui/productbuilder/components/PricePreview.scala`

### 7.3 Specification Form

The `SpecificationForm` component renders inputs based on `requiredSpecKinds`. All 8 existing spec kinds have UI controls:

| Spec Kind | UI Control | Notes |
|-----------|-----------|-------|
| Size | Width × Height inputs | Dimension in mm |
| Quantity | Numeric input | With validation |
| Orientation | Radio: Portrait/Landscape | |
| Bleed | Numeric input | In mm |
| Pages | Numeric input | With divisibility validation |
| FoldType | Dropdown | 8 fold type options |
| BindingMethod | Dropdown | 5 binding method options |
| ManufacturingSpeed | Managed by speed cards | Auto-configured |

**Action needed if new SpecKind added** (e.g., `GarmentSize`): Add a new UI control in `SpecificationForm.scala`.

---

## 8. Visual Editor Implications

The visual editor is the most significant area requiring manual updates. Several components use **hardcoded category ID matching**.

### 8.1 Editor Bridge — Product Type Inference

**File**: `modules/ui/src/main/scala/mpbuilder/ui/visualeditor/EditorBridge.scala`

The `inferVisualProductType` method maps category IDs to visual product types:

```scala
private def inferVisualProductType(config: ProductConfiguration): Option[VisualProductType] =
  config.category.id.value match
    case "cat-calendars" => Some(VisualProductType.MonthlyCalendar)
    case "cat-booklets"  => Some(VisualProductType.PhotoBook)
    case "cat-roll-ups"  => Some(VisualProductType.WallPicture)
    case "cat-posters"   => Some(VisualProductType.WallPicture)
    case _               => None  // Falls back to GenericProduct
```

**For new products**: If the product doesn't need a specialized visual product type, `None` (GenericProduct) is used automatically — **no change needed**. If the product needs special treatment (e.g., wrap-around mug design), a new `VisualProductType` variant would be needed.

### 8.2 Product Overlays

**File**: `modules/ui/src/main/scala/mpbuilder/ui/visualeditor/overlays/ProductOverlay.scala`

Visual overlays render non-interactive decorations on the canvas (wire binding, roll-up stand, frame border). The matching is done by `categoryId` string contains:

```scala
if catId.contains("calendar") then renderWireBinding(format)
else if catId.contains("roll-up") then renderRollUpStand(format)
else if catId.contains("poster") then renderFrame(format)
else emptyMod
```

**For new products**: If the product needs a visual overlay (e.g., mug handle, T-shirt collar outline), add a new case and render method. If no overlay is needed, **no change required** — it falls through to `emptyMod`.

**Potential overlays for new products**:

| Product | Possible Overlay | Description |
|---------|-----------------|-------------|
| T-Shirt | T-shirt silhouette outline | Shows printable area on garment shape |
| Eco Bag | Bag outline with handles | Shows printable area on bag front |
| Cup/Mug | Mug shape with handle | Shows wrap-around print area |
| Pin Badge | Circular/custom shape boundary | Shows actual badge shape and bleed |

### 8.3 Visual Product Types & Formats

**File**: `modules/ui/src/main/scala/mpbuilder/ui/visualeditor/EditorModel.scala`

If adding a specialized visual product type:

```scala
enum VisualProductType:
  // ... existing types ...
  case TShirtDesign    // NEW: single-page, front/back
  case MugWrapAround   // NEW: single-page, wrap-around cylindrical projection
```

Each new type needs:
- `ProductFormat.formatsFor(pt)` — list of available formats
- `ProductFormat.defaultFor(pt)` — default format
- Editor state initialization in `EditorState.create()`

**Recommendation for promotional products**: Use `GenericProduct` type initially. It supports any dimensions and a single page. Custom visual product types should only be added when the editor needs special behavior (e.g., cylindrical projection for mugs).

### 8.4 Canvas Dimensions

The `ProductContext` passes physical dimensions (mm) from the product builder to the visual editor:

```scala
ProductContext(
  widthMm = 200.0,   // from Size spec
  heightMm = 250.0,
  pageCount = Some(1),
  categoryId = Some("cat-tshirts"),
  categoryName = Some("T-Shirts"),
  visualProductType = None,  // uses GenericProduct
)
```

The canvas scales these dimensions to pixel coordinates automatically. **No change needed** for basic support.

### 8.5 Summary of Visual Editor Changes by Product

| Product | Visual Product Type | Overlay Needed | Format Needed | Effort |
|---------|-------------------|----------------|---------------|--------|
| T-Shirt | GenericProduct (default) | Optional (silhouette) | No | Low |
| Eco Bag | GenericProduct (default) | Optional (bag outline) | No | Low |
| Pin Badge | GenericProduct (default) | Optional (circle) | No | Low |
| Cup/Mug | GenericProduct or new MugWrapAround | Recommended (handle) | Optional | Medium |

---

## 9. Catalog Editor (Admin UI)

### 9.1 Automatic Support

The Catalog Editor (`modules/ui/src/main/scala/mpbuilder/ui/catalog/`) is fully data-driven and supports:

- Adding/editing/removing categories via `CategoryEditorView`
- Adding/editing/removing materials via `MaterialEditorView`
- Adding/editing/removing finishes via `FinishEditorView`
- Adding/editing/removing printing methods
- Editing compatibility rules via `RulesEditorView`
- Editing pricing rules via `PricelistEditorView`

**No code changes needed** in the Catalog Editor for new products. All new entities defined in `SampleCatalog` will appear in the editor automatically.

### 9.2 Export/Import

The catalog editor has export functionality that serializes the full catalog (categories, materials, finishes, rules, pricelists) to JSON. New entities are included automatically.

---

## 10. Testing Checklist

### 10.1 Domain Tests

**File**: `modules/domain/src/test/scala/mpbuilder/domain/`

| Test | File | What to Verify |
|------|------|---------------|
| Showcase validity | `ShowcaseSpec.scala` | New product references valid category ID, has tagline/description/image |
| Configuration building | `ConfigurationBuilderSpec.scala` | New category can build valid configurations with its materials/finishes |
| Price calculation | `PriceCalculatorSpec.scala` | New materials/finishes have pricing rules, price calculation returns valid breakdown |
| Compatibility rules | `CompatibilityRuleEngineSpec.scala` | New rules are evaluated correctly |
| Workflow generation | `WorkflowGeneratorSpec.scala` | New product generates sensible manufacturing workflow |

### 10.2 Existing Test Validation

The following existing tests will validate new products without modification:

- **ShowcaseSpec**: `"all products reference valid category IDs"` — ensures showcase product's `categoryId` exists in catalog
- **ShowcaseSpec**: `"all products have non-empty tagline and description"` — ensures localized strings are provided
- **ShowcaseSpec**: `"byGroup covers all products"` — ensures group mapping is complete

### 10.3 UI Testing

- Verify new product appears in catalog grid under correct group tab
- Verify product detail page renders with correct marketing content
- Verify product builder can configure the product (materials, finishes, printing methods)
- Verify price breakdown is calculated and displayed
- Verify visual editor opens from product builder with correct dimensions
- Verify manufacturing workflow visualization shows correct steps

---

## 11. Complete Checklist

### Domain Layer
- [ ] Define `CategoryId` in `SampleCatalog.scala`
- [ ] Define new `Material`(s) with IDs in `SampleCatalog.scala`
- [ ] Define new `Finish`(es) with IDs in `SampleCatalog.scala` (if needed)
- [ ] Define new `PrintingMethod`(s) with IDs in `SampleCatalog.scala` (if needed)
- [ ] Define `ProductCategory` with components, allowed materials/finishes/printing methods, specs
- [ ] Define `CategoryPreset`(s) for quick configuration
- [ ] Register all new entities in the `ProductCatalog` constructor (categories, materials, finishes, printingMethods maps)
- [ ] Add `CatalogGroup` variant if needed (`showcase.scala`)

### Pricing
- [ ] Add `MaterialBasePrice` (or `MaterialAreaPrice`/`MaterialSheetPrice`) for each new material in **all 3 pricelists** (USD, CZK, CZK Sheet)
- [ ] Add `FinishSurcharge` for each new finish in all pricelists
- [ ] Add `FinishSetupFee` for each new finish in all pricelists
- [ ] Add `CategorySurcharge` if applicable
- [ ] Add `ManufacturingSpeedSurcharge` rules if express is supported (all pricelists!)
- [ ] Add `MinimumOrderPrice` if applicable

### Compatibility Rules
- [ ] Add material–finish incompatibility rules in `SampleRules.scala`
- [ ] Add spec constraints (size ranges, quantity limits) per category
- [ ] Add technology constraints if needed (e.g., printing process limitations)

### Showcase / Catalog UI
- [ ] Create `ShowcaseProduct` in `SampleShowcase.scala` with tagline, description, image, variations, features, instructions
- [ ] Add to `allProducts` list
- [ ] Add group filter tab in `ProductCatalogApp.scala` if new `CatalogGroup` was added
- [ ] Add group label and badge translations

### Visual Editor (only if needed)
- [ ] Add category mapping in `EditorBridge.inferVisualProductType` (or verify GenericProduct fallback works)
- [ ] Add product overlay in `ProductOverlay.render` (optional, for visual context)
- [ ] Add `VisualProductType` variant (only if specialized editor behavior needed)
- [ ] Add `ProductFormat` entries (only if specialized formats needed)
- [ ] Update `docs/visual-product-types.md`

### Manufacturing
- [ ] Verify `WorkflowGenerator.printingStationFor` maps new printing process types correctly
- [ ] Add new `StationType` variants if needed
- [ ] Update `SampleStations` with new station entries if new types added

### Testing
- [ ] Run `ShowcaseSpec` — validates showcase product integrity
- [ ] Run `ConfigurationBuilderSpec` — add test for new category configuration
- [ ] Run `PriceCalculatorSpec` — add test for new product pricing
- [ ] Run `WorkflowGeneratorSpec` — add test for new product workflow
- [ ] Run full domain test suite: `mill domain.jvm.test` or `sbt domainJVM/test`
- [ ] Compile UI: `mill ui.compile` or `sbt ui/compile`
- [ ] Manual UI verification: catalog display, product builder, visual editor

### Documentation
- [ ] Update `docs/features.md` with new product information
- [ ] Update `docs/visual-product-types.md` if editor types/formats added
- [ ] Update `docs/analysis/domain-model-gap-analysis.md` coverage tables
- [ ] Update `README.md` if significant capability added

---

## Appendix: Files Modified When Adding a Product

| File | Change Type | Always Needed |
|------|------------|---------------|
| `SampleCatalog.scala` | Category, materials, finishes, printing methods, presets | ✅ Yes |
| `SamplePricelist.scala` | Pricing rules (all 3 pricelists) | ✅ Yes |
| `SampleRules.scala` | Compatibility rules | ✅ Yes |
| `SampleShowcase.scala` | Showcase marketing content | ✅ Yes |
| `showcase.scala` | CatalogGroup enum (if new group) | Only if new group |
| `ProductCatalogApp.scala` | Group labels/badges | Only if new group |
| `EditorBridge.scala` | Visual product type mapping | Only if specialized editor |
| `ProductOverlay.scala` | Visual overlay rendering | Only if overlay desired |
| `EditorModel.scala` | VisualProductType, ProductFormat | Only if specialized editor |
| `WorkflowGenerator.scala` | Station mapping | Only if new process type |
| `material.scala` | MaterialFamily enum | Only if new family |
| `finish.scala` | FinishType enum | Only if new finish type |
| `category.scala` | SpecKind enum | Only if new spec kind |
| `component.scala` | ComponentRole enum | Only if new role |
| `ConfigurationForm.scala` | Component role labels | Only if new role |
| `PricePreview.scala` | Component role labels | Only if new role |
| `SpecificationForm.scala` | New spec kind input | Only if new spec kind |

---

## Appendix: Effort Estimation

| Product Type | Domain Changes | Pricing | Rules | Catalog UI | Visual Editor | Manufacturing | Total Effort |
|-------------|---------------|---------|-------|-----------|--------------|---------------|-------------|
| T-Shirts | Medium (new materials) | Medium | Low | Low | Low–Medium | Low | **Medium** |
| Eco Bags | Medium (new materials) | Medium | Low | Low | Low | Low | **Medium** |
| Pin Badges | Medium (new materials) | Medium | Low | Low | Low | Low | **Low–Medium** |
| Cups & Mugs | Medium (new materials) | Medium | Low | Low | Medium (wrap-around) | Low | **Medium** |
| Pens | Low (reuse Hardware) | Low | Low | Low | Low | Low | **Low** |
| Notebooks | Low (reuse Paper) | Low | Low | Low | Low | Low | **Low** |

*Low = 1–2 hours, Medium = 2–4 hours, per product*
