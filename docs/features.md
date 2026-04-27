# Feature Overview

This document summarises every major feature area of the Product Builder system, including the product configurator, visual editor, pricing engine, manufacturing workflow, and supporting infrastructure.

---

## 1. Product Configuration

The core of the system is a domain-driven product configurator for the printing industry.

### Catalog & Categories

- **11 product categories**: Business Cards, Postcards, Flyers, Brochures, Booklets, Calendars, Banners, Packaging, Stickers & Labels, Roll-Up Banners, Free Configuration
- **13 materials**: papers (coated, uncoated, recycled, premium), cardboard, vinyl, synthetic, adhesive stock — each with family, weight, and properties
- **16 finishes**: lamination, UV coating, soft-touch, embossing, foil stamping, die-cut, scoring, perforation, round corners, grommets, and more
- **4 printing methods**: Digital, Offset, Letterpress, UV Inkjet — each with process type and max color count
- **Multi-component products**: categories define component templates (e.g. booklet cover + body, roll-up banner + optional stand)

### Ink Configuration

Per-side ink setup modelling (`InkSetup` = ink type + color count):
- Presets: `4/4` (CMYK both sides), `4/0` (front only), `4/1`, `1/0`, `1/1`
- Structural validation checks against `PrintingMethod.maxColorCount`

### Compatibility Rules Engine

Rules are **data, not code** — a sealed ADT (`CompatibilityRule`) with 17 variants interpreted by a rule engine:

| Rule | Purpose |
|------|---------|
| `MaterialFinishIncompatible` | Forbids specific material–finish pairs |
| `MaterialRequiresFinish` | Material requires a specific finish type |
| `FinishRequiresMaterialProperty` | Finish requires material to have a property |
| `FinishMutuallyExclusive` | Certain finishes cannot be combined |
| `SpecConstraint` | Category-scoped spec constraints (size ranges, quantity limits) |
| `ConfigurationConstraint` | Category-scoped constraint on the full configuration (ink types, finish combinations) |
| `TechnologyConstraint` | Global constraint applying to all categories (binding method limits, weight-based page caps) |
| `MaterialPropertyFinishTypeIncompatible` | Material property conflicts with finish type |
| `MaterialFamilyFinishTypeIncompatible` | Material family conflicts with finish type |
| `MaterialWeightFinishType` | Finish type requires minimum material weight |
| `FinishTypeMutuallyExclusive` | Finish types that cannot be combined |
| `FinishCategoryExclusive` | Only one finish per category allowed |
| `FinishRequiresFinishType` | Finish depends on another finish type |
| `FinishRequiresPrintingProcess` | Finish requires a specific printing process |
| `ScoringMaxCreasesForCategory` | Maximum crease count for Scoring finish per category |
| `ScoringMaxCreasesForMaterial` | Maximum crease count for Scoring finish per material |
| `ScoringMaxCreasesForPrintingProcess` | Maximum crease count for Scoring finish per printing process |

Predicates (`SpecPredicate`, `ConfigurationPredicate`) support boolean algebra (And/Or/Not) for composable, inspectable rule conditions.

### Validation Pipeline

Two-layer validation using ZIO Prelude `Validation` (error accumulation, not short-circuit):

1. **Structural validation** — category/material/finish selections are valid, required specs present
2. **Rule evaluation** — all compatibility rules checked against the configuration

All errors are collected at once, providing comprehensive feedback.

### Progressive Disclosure

`CatalogQueryService` pre-filters valid options based on the current configuration state, so the UI shows only compatible choices at each step.

---

## 2. Pricing Engine

Declarative pricing layer following the same rules-as-data pattern.

### Money & Pricelist

- **`Money`** opaque type over `BigDecimal` (never `Double`) with `HALF_UP` rounding to 2 dp
- **`Pricelist`** bundles pricing rules with currency and version string

### Pricing Rules (19 variants)

| Rule | Purpose |
|------|---------|
| `MaterialBasePrice` | Flat per-unit price |
| `MaterialAreaPrice` | Price per m² (large format) |
| `MaterialAreaTier` | Area-tiered price per m² (picks best matching tier) |
| `MaterialSheetPrice` | Sheet-based pricing with nesting/imposition |
| `FinishSurcharge` / `FinishTypeSurcharge` | Per-finish surcharge (ID overrides type) |
| `ScoringCountSurcharge` | Per-unit surcharge for creasing, keyed on exact crease count (discountable) |
| `GrommetSpacingAreaPrice` | Area-based grommet surcharge keyed by spacing |
| `FinishLinearMeterPrice` | Linear-metre pricing for rope/accessory finishes |
| `PrintingProcessSurcharge` | Process-specific surcharge |
| `CategorySurcharge` | Category-specific surcharge |
| `InkConfigurationSheetPrice` | Additive per-sheet (or per-unit) ink cost keyed by printing method and front/back color counts |
| `InkConfigurationAreaPrice` | Additive per-m² ink cost keyed by printing method and front/back color counts |
| `CuttingSurcharge` | Per-cut surcharge |
| `FoldTypeSurcharge` | Fold type surcharge |
| `BindingMethodSurcharge` | Binding method surcharge |
| `QuantityTier` / `SheetQuantityTier` | Volume discount tiers |
| `FinishSetupFee` / `FinishTypeSetupFee` | One-time setup fees (ID overrides type) |
| `ScoringSetupFee` | One-time setup fee for Scoring finish (overrides `FinishTypeSetupFee` for Scoring) |
| `FoldTypeSetupFee` / `BindingMethodSetupFee` | Setup fees for fold/binding |
| `MinimumOrderPrice` | Floor price for orders |

### Calculation Flow

`ProductConfiguration` + `Pricelist` → `PriceCalculator.calculate` → `Validation[PricingError, PriceBreakdown]`

Steps: resolve material unit price (area > sheet > base precedence) → compute sheets used → apply ink factor → finish surcharges → process/category surcharges → fold/binding surcharges → sum subtotal → apply best tier multiplier → collect setup fees → apply minimum order price floor → round total.

Output includes `ComponentBreakdown` per component with optional `inkConfigLine`, `cuttingLine`, `sheetsUsed`, plus `setupFees`, `minimumApplied`, `foldSurcharge`, and `bindingSurcharge`.

See [docs/pricing.md](pricing.md) for detailed explanation with worked examples.

---

## 3. Visual Product Editor

An interactive per-page editor for designing calendars, photo books, and wall pictures.

### Product Types & Formats

| Type | Pages | Available Formats |
|------|-------|-------------------|
| Monthly Calendar | 12 | Wall Calendar, Wall Calendar Large, Desk Calendar, Desk Calendar Small |
| Weekly Calendar | 52 | Wall Calendar, Wall Calendar Large, Desk Calendar, Desk Calendar Small |
| Bi-weekly Calendar | 26 | Wall Calendar, Wall Calendar Large, Desk Calendar, Desk Calendar Small |
| Photo Book | 12 | Square (210×210), Landscape (297×210), Portrait (210×297) |
| Wall Picture | 1 | Small (200×300), Large (400×600), Landscape (600×400) |

### Canvas Elements

`CanvasElement` sealed trait with four variants:
- **PhotoElement** — image upload/replace/clear, drag/resize/rotate
- **TextElement** — rich text with bold, italic, alignment (left/center/right)
- **ShapeElement** — rectangles and lines with stroke and fill colors
- **ClipartElement** — decorative clipart elements

All elements support: selection, drag, resize, rotate, z-ordering, duplication, deletion.

### Page Features

- Per-page backgrounds (solid color or image upload)
- Template text fields (locked month/day labels, separate from user elements)
- Image placeholders as interactive `PhotoElement(imageData = "")`
- Horizontal scrollable page navigation strip (handles 52+ pages)
- Sidebar tabs: Page Elements / Background

See [docs/visual-product-types.md](visual-product-types.md) for all supported types and formats.

---

## 4. Shopping & Checkout

### Shopping Basket

- `Basket` / `BasketItem` with quantity and pre-calculated pricing
- `BasketService` — pure operations: `addItem`, `removeItem`, `updateQuantity`, `calculateTotal`, `clear`
- "Add to Basket" from configuration form with quantity selector
- Reactive basket panel with item list, quantity management, total calculation

### Order via Email

An escape hatch for customers who cannot complete a standard order flow:

- **"Order via Email" collapsible form** — appears in two places in the Product Builder UI:
  1. In the **Validation Status card**, contextually, when there are active validation errors ("Having trouble? Request a quote via email instead.")
  2. At the **bottom of the configuration form**, always visible as a general escape hatch for customers who didn't find all required options.
- **Fields**: customer name, email address, notes/message. Notes are pre-filled with the current (possibly partial) configuration summary (category, printing method, quantity, size, material, estimated price).
- **Delivery**: clicking "Open Email Client" generates a `mailto:` URL with pre-populated subject (`Product Order Request — [category]`) and body, opening the user's default email client. No backend required.
- **Fully bilingual** (EN/CS).

### Checkout & Orders

- `Order` model with customer info, delivery details, basket items
- `DiscountService` with promotional discount codes
- Checkout view with customer details form, delivery options, order summary
- Grand total includes delivery cost and applied discounts

---

## 5. Manufacturing System

A complete production workflow management system for print shops, from order approval through to dispatch. Implemented across 8 phases (see [manufacturing-implementation-plan.md](manufacturing-implementation-plan.md) for full details).

### Phase 1 — Core Domain Model & Workflow Generator

- **14 station types**: Prepress, Digital Printer, Offset Press, Large Format Printer, Letterpress, Cutter, Laminator, UV Coater, Embossing/Foil, Folder, Binder, Large Format Finishing, Quality Control, Packaging
- **DAG-based workflow**: `ManufacturingWorkflow` with `WorkflowStep` nodes, dependency edges, status tracking
- **`WorkflowGenerator`** — derives step sequence and DAG dependencies from `ProductConfiguration` (printing method → station mapping, finish → finishing steps, cross-component binding/QC/packaging)
- Opaque type IDs: `WorkflowId`, `StepId`, `EmployeeId`, `MachineId`

### Phase 2 — Shared UI Framework

- **`SplitTableView`** — domain-agnostic sortable data table with generic type parameter, column definitions, filter chips, search, row selection with side panel
- Reusable across all manufacturing views (and beyond)

### Phase 3 — Manufacturing UI Views

- **Dashboard** — summary cards (Awaiting Approval, In Production, Ready for Dispatch, Overdue, Today's Completions), station status strip (14 tiles), recent orders table
- **Station Queue** — primary operator view with station/status/priority filters, sortable columns, Start/Complete actions, side panel with workflow progress
- **Order Approval** — manager/prepress view with approval queue, Approve/Reject actions, side panel with full order details
- **Order Progress** — fulfilment tracking with workflow status filters, progress bars, deadline urgency coloring, per-item workflow visualization
- **Client-side routing** with sidebar navigation, responsive mobile layout

### Phase 4 — Workflow Engine *(PR #66)*

Pure state machine over `ManufacturingWorkflow` returning `Validation[WorkflowError, ManufacturingWorkflow]`:

- **`startStep`** — Ready → InProgress, assigns employee, enforces DAG dependencies
- **`completeStep`** — InProgress → Completed, auto-promotes downstream Waiting → Ready
- **`failStep`** — InProgress → Failed, workflow → OnHold, appends failure reason
- **`skipStep`** — Waiting/Ready → Skipped (Prepress, QC, Packaging non-skippable)
- **`resetStep`** — Completed/Failed/Skipped → Ready with `isRework` flag, reverts downstream to Waiting

**`QueueScorer`** — advisory priority scoring for pull-model queue ordering:

| Component | Range | Source |
|-----------|-------|--------|
| Deadline urgency | 0–100 | 8 tiers from >72h to overdue |
| Priority boost | -10 to 30 | Rush / Normal / Low |
| Completeness | 0–20 | Completion ratio × 20 |
| Batch affinity | 0–15 | Material ID match with current machine setup |
| Age (tiebreaker) | minutes | FIFO within same total score |

**`WorkflowError`** ADT — 9 variants with English and Czech messages.

### Phase 5 — Employee & Machine Management *(PR #67)*

- **`EmployeeManagementService`** — pure CRUD with `Validation[ManagementError, List[Employee]]`: add, update, toggle active, update capabilities, remove
- **`MachineManagementService`** — pure CRUD with `Validation[ManagementError, List[Machine]]`: add, update, change status (Online/Offline/Maintenance), change station type, remove
- **`EmployeesView`** — SplitTableView with station capability toggle grid, current employee selector
- **`MachinesView`** — SplitTableView with status controls and station type filters
- **Dashboard "My In-Progress Jobs"** section keyed on `currentEmployeeId`

### Phase 6 — Order Approval Enhancements *(PR #67)*

- **`ArtworkCheck`** — per-file validation flags (resolution, bleed, color profile) with `CheckStatus` enum (NotChecked/Passed/Warning/Failed)
- **`PaymentStatus`** enum on `ManufacturingOrder` (Pending/Confirmed/Failed)
- Order-level **`priority`** field drives workflow generation
- **Enhanced approval panel**: per-flag artwork review buttons, payment verification, priority selector, Hold/Request Changes actions

### Phase 7 — Fulfilment Workflow *(PR #67)*

- **`FulfilmentChecklist`** — 4-step dispatch model:
  1. **Collect Items** — per-basket-item collection checkboxes
  2. **Quality Sign-Off** — QC pass/fail with notes
  3. **Packaging** — type selection (Box/Envelope/Roll/Tube/Custom), dimensions, weight
  4. **Dispatch** — confirmation with tracking number (gated: steps 1–3 required first)
- Auto-created on `ManufacturingOrder` when all workflows reach Completed
- Fulfilment progress bar (0/4 → 4/4) in Order Progress view

### Phase 8 — Analytics & Reporting *(PR #67)*

- **`AnalyticsService`** — pure functions: `averageTimePerStation`, `bottleneckStation`, `employeeThroughput`, `onTimeDeliveryRate`
- **KPI cards**: Total Orders, Completed, In Progress, On-Time Rate, Average Step Time
- **Station Performance table** with load indicators (Idle/Light/Moderate/Heavy)
- **Employee Throughput table** with visual bars and station chip lists
- **Bottleneck alert banner** when queue depth exceeds threshold

---

## 6. Internationalization (i18n)

- **`Language` enum** (`En`, `Cs`) with **`LocalizedString`** opaque type (Map-based, English fallback)
- All domain entities use `LocalizedString` for names
- All error ADTs (`ConfigurationError`, `PricingError`, `WorkflowError`, `ManagementError`) have `message(lang)` with Czech translations
- UI language selector with browser language detection and `localStorage` persistence
- All UI components fully localized (form labels, validation messages, price preview, manufacturing views)

---

## 7. UI Framework (`ui-framework` module)

A standalone Scala.js/Laminar component library with no domain dependency:

- **Field components**: `TextField`, `TextAreaField`, `SelectField`, `CheckboxField`, `RadioGroup`
- **Container components**: `Tabs`, `Stepper`, `SplitTableView`
- **Feedback**: `ValidationDisplay`
- **Form abstraction**: Mirror-based compile-time derivation of form state from case classes, composable validators, automatic rendering
- **Utility**: `Visibility.when/unless` CSS toggle modifiers

See [docs/analysis/ui-kit-review.md](analysis/ui-kit-review.md) for component details and code reduction analysis.

---

## 8. Testing

**350 passing tests** across 14 test suites:

| Suite | Tests | Covers |
|-------|-------|--------|
| `ConfigurationBuilderSpec` | 34 | Valid configs, error accumulation, weight rules, finish dependencies |
| `CatalogQueryServiceSpec` | 17 | Material/finish/spec filtering, progressive disclosure |
| `PriceCalculatorSpec` | 23 | Breakdowns, area/sheet pricing, tier discounts, finish precedence, scoring tiers |
| `LocalizationSpec` | 17 | LocalizedString, Czech translations, error messages |
| `BasketServiceSpec` | 17 | Add/remove/update items, quantity validation, totals |
| `WorkflowGeneratorSpec` | 19 | Workflow structure, station mapping, DAG validity |
| `WorkflowEngineSpec` | 27 | Start/complete/fail/skip/reset transitions, DAG enforcement |
| `QueueScorerSpec` | 24 | Urgency tiers, priority boost, batch affinity, sort order |
| `WeightCalculatorSpec` | — | Weight calculation for shipping |
| `EmployeeManagementServiceSpec` | 17 | Employee CRUD, capabilities, activation |
| `MachineManagementServiceSpec` | 16 | Machine CRUD, status transitions |
| `ArtworkCheckSpec` | 15 | Artwork check flags, payment/check status extensions |
| `FulfilmentChecklistSpec` | 17 | Collection, QC, packaging, dispatch, status transitions |
| `AnalyticsServiceSpec` | 13 | Avg time, bottleneck, throughput, on-time rate |

All domain logic is pure (`Validation[E, A]`), never `ZIO` effects — keeping the domain Scala.js-compatible and easily testable.

---

## Documentation Index

| Document | Description |
|----------|-------------|
| [features.md](features.md) | This document — complete feature overview |
| [pricing.md](pricing.md) | Pricing system documentation with worked examples |
| [visual-product-types.md](visual-product-types.md) | Visual product types, formats, and domain mapping |
| [manufacturing-implementation-plan.md](manufacturing-implementation-plan.md) | Manufacturing system implementation plan (Phases 1–8) |
| [ui-guide.md](ui-guide.md) | Build, run, and test instructions |
| **Analysis & Planning** | |
| [analysis/printing-domain-analysis.md](analysis/printing-domain-analysis.md) | Printing industry domain analysis |
| [analysis/domain-model-gap-analysis.md](analysis/domain-model-gap-analysis.md) | Domain model gap analysis |
| [analysis/manufacturing-workflow-analysis.md](analysis/manufacturing-workflow-analysis.md) | Manufacturing workflow design analysis |
| [analysis/roll-up.md](analysis/roll-up.md) | Roll-up banner product specification |
| [analysis/sheet-based-pricing.md](analysis/sheet-based-pricing.md) | Sheet-based material pricing design |
| [analysis/ui-kit-review.md](analysis/ui-kit-review.md) | UI framework component review |
