# Product Builder — Goals & Roadmap

## Vision

A domain-driven product configuration and manufacturing management system for the printing industry. Customers configure products from categories, materials, finishes, and specifications — with declarative compatibility rules ensuring only valid combinations are possible. Includes a visual product editor for designing calendars, photo books, and wall pictures, a shopping basket with checkout, and a full manufacturing workflow management system for print shop operators.

## Tech Stack

- **Scala 3.3.3** with DDD principles
- **ZIO 2.x** for effects (infrastructure layer)
- **ZIO Prelude** for validation with error accumulation
- **Scala.js + Laminar** for the interactive web UI (cross-compiled domain)

## Key Design Decisions

1. **Rules are data, not code** — `CompatibilityRule` is a sealed ADT interpreted by a rule engine. New materials/finishes can be added without changing business logic.
2. **ZIO Prelude `Validation`** for error accumulation — all errors are collected, not short-circuited.
3. **No effects in domain layer** — all domain functions return `Validation[...]`, never `ZIO[...]`. This keeps the domain Scala.js-compatible.
4. **Specification pattern as ADT** — `ConfigurationPredicate` with And/Or/Not is inspectable and serializable, unlike `A => Boolean`.
5. **Progressive disclosure** — `CatalogQueryService` pre-filters valid options so the UI only shows compatible choices.

---

## Completed — Phase 1: Domain Model

The core domain layer is fully implemented and tested:

- **Value objects** — Opaque type IDs (`CategoryId`, `MaterialId`, `FinishId`, `PrintingMethodId`, `ConfigurationId`, `BasketId`) with smart constructors, `PaperWeight`, `Quantity`, `Dimension`
- **Domain model** — `Material` (family, weight, properties), `Finish` (type, side), `ProductCategory` (allowed materials/finishes/printing methods, required specs), `ProductSpecification` (8 spec kinds), `ProductConfiguration` (aggregate root), `ProductCatalog`, `PrintingMethod`
- **Compatibility rules** — Sealed ADT with 12 rule variants, spec predicates, configuration predicates with boolean algebra (And/Or/Not)
- **Validation** — Two-layer pipeline (structural checks then rule evaluation), rich error ADT with 19 error types and localized messages
- **Services** — `ConfigurationBuilder` (resolve + validate + build), `CatalogQueryService` (filtered queries for UI guidance)
- **Sample data** — 7 categories (Business Cards, Flyers, Brochures, Banners, Packaging, Booklets, Calendars), 9 materials, 14 finishes, 4 printing methods, 24 compatibility rules

## Completed — Phase 2: Pricing

Declarative pricing layer following the same rules-as-data pattern:

- **Money** — Opaque type over `BigDecimal` (never `Double`), rounding to 2dp with `HALF_UP`
- **PricingRule** — Sealed enum with 17 variants covering material (base/area/sheet), finish surcharges, process/category surcharges, ink configuration factor, cutting, fold/binding surcharges, quantity/sheet tiers, setup fees, and minimum order price
- **PriceCalculator** — Pure interpreter: config + pricelist → `Validation[PricingError, PriceBreakdown]`. Supports area-based pricing (large-format), sheet-based pricing with nesting, ID-over-type finish surcharge precedence, and quantity tier discounts
- **PriceBreakdown** — Detailed output with per-component line items, setup fees, and rounded total
- **Sample data** — USD + CZK pricelists with full finish/fold/binding surcharges and setup fees
- **Documentation** — See `docs/pricing.md` for detailed explanation with worked examples

---

## Completed — Phase 3: UI with Scala.js + Laminar

Interactive web UI for configuring print products:

- **Cross-compilation** — Domain model compiled to JavaScript with Scala.js (Scala 3.3.3 for compatibility)
- **Laminar framework** — Reactive UI with Signal/Var primitives for state management
- **Step-by-step wizard** — 5-step configuration flow: category → material → printing method → finishes → specifications
- **Progressive disclosure** — `CatalogQueryService` filters options in real-time; disabled states guide users through valid paths only
- **Live validation** — Immediate feedback on incompatible selections with detailed error messages
- **Price calculation** — Real-time pricing with full breakdown (material, finishes, surcharges, quantity discounts)
- **Modern design** — Responsive layout with gradient purple theme, sticky price preview, info boxes, and visual feedback
- **Sample catalog** — Pre-loaded with 11 categories, 13 materials, 16 finishes, 4 printing methods, 29 compatibility rules, and full pricelists
- **Deployment** — GitHub Actions workflow builds Scala.js and deploys to GitHub Pages

See `docs/ui-guide.md` for build and run instructions.

---

## Completed — Phase 4: Internationalization (i18n)

Multi-language support across domain model and UI:

- **`Language` enum** (`En`, `Cs`) and **`LocalizedString` opaque type** over `Map[Language, String]` with English fallback
- `name` field on `Material`, `Finish`, `ProductCategory`, `PrintingMethod` uses `LocalizedString`
- `ConfigurationError.message(lang)` and `PricingError.message(lang)` with Czech translations for all variants
- `PriceCalculator.calculate` accepts optional `lang` parameter for localized line item labels
- UI language selector with browser language detection and `localStorage` persistence
- All UI components localized: form headings, labels, placeholders, validation messages, price preview

---

## Completed — Phase 5: Extended Catalog

Expanded sample data based on printing domain analysis:

- **Calendars** category — Wire-bound/spiral-only with 12–28 pages constraint
- **New materials** — Yupo Synthetic 200μm, Cotton Paper 300gsm, Coated Silk 250gsm, Adhesive Stock 100gsm
- **New finishes** — Soft Touch Coating, Aqueous Coating, Debossing, Scoring, Perforation, Round Corners, Grommets
- **Rules** expanded from 10 → 29 with material-finish incompatibilities, weight constraints, and calendar-specific rules
- **Pricing** updated for all new materials and finishes

---

## Completed — Phase 6: Shopping Basket

Complete shopping basket functionality:

- **Domain model** — `BasketId` opaque type, `Basket` containing `BasketItem`s (configuration + quantity + pre-calculated pricing)
- **`BasketService`** — Pure functional operations via `Validation[BasketError, _]`: `addItem`, `removeItem`, `updateQuantity`, `calculateTotal`, `clear`
- **`BasketError`** ADT with localized messages (EN/CS): `InvalidQuantity`, `ConfigurationNotFound`, `PricingFailed`
- **UI** — Basket item list with quantity management, remove/clear buttons, total calculation display, success/error messaging
- **Integration** — "Add to Basket" from configuration form, reactive basket state in ViewModel

---

## Completed — Phase 7: Photo Calendar Builder

Interactive calendar page editor:

- **`CanvasElement` sealed trait** with `PhotoElement`, `TextElement`, `ShapeElement`, `ClipartElement` variants
- **12-page editor** — One page per month, each with its own elements and background
- **Element manipulation** — Drag, resize, rotate for all element types; z-ordering, duplication, deletion
- **Unified element list** — `ElementListEditor` replaces separate photo/text panels; type-dispatched form editors
- **Text formatting** — Bold, italic, alignment (left/center/right)
- **Shape support** — Lines and rectangles with stroke and fill colors
- **Page backgrounds** — Solid color picker or image upload
- **Template text fields** — Locked month/day labels separate from user elements
- **Client-side routing** — `AppRouter` with navigation between Product Builder and Calendar Builder views
- **Global language selector** — Positioned at root level, applies to both views

---

## Completed — Phase 8: Visual Product Editor Generalization

Extended the calendar builder into a general visual product editor:

- **Product types** — `VisualProductType` enum: Monthly Calendar (12p), Weekly Calendar (52p), Bi-weekly Calendar (26p), Photo Book (12p), Wall Picture (1p)
- **Domain-driven formats** — `ProductFormat` case class with physical dimensions (mm); 10 formats: 4 calendar (wall/desk, normal/large/small), 3 photo book (square/landscape/portrait), 3 wall picture (small/large/landscape)
- **Format–type rules** — `ProductFormat.formatsFor(pt)` constrains which formats are available per product type; selector dynamically filtered
- **Interactive image placeholders** — Replaced static `TemplateImagePlaceholder` with `PhotoElement(imageData = "")` in page elements list — fully interactive: selectable, draggable, resizable, with upload/replace/clear
- **Horizontal page navigation** — Scrollable page strip in footer (replaces right sidebar); handles 52+ pages
- **Sidebar tabs** — "Page Elements" / "Background" tabs to reduce visual noise
- **UI naming** — Tabs renamed: "Product Parameters" / "Visual Editor"; page title/header: "Product Builder"
- **Canvas aspect ratio** — Derived from physical format dimensions
- **Documentation** — `docs/visual-product-types.md` for all supported types and formats

---

## Completed — Manufacturing Phase 1: Core Domain Model & Workflow Generator

Full manufacturing domain model and automatic workflow generation:

- **14 station types** — Prepress, Digital Printer, Offset Press, Large Format Printer, Letterpress, Cutter, Laminator, UV Coater, Embossing/Foil, Folder, Binder, Large Format Finishing, Quality Control, Packaging
- **DAG-based `ManufacturingWorkflow`** — `WorkflowStep` nodes with dependency edges, status tracking, employee/machine assignment
- **`WorkflowGenerator`** — Derives step sequence and DAG from `ProductConfiguration` (printing method → station mapping, finishes → finishing steps, cross-component binding/QC/packaging)
- Opaque type IDs: `WorkflowId`, `StepId`, `EmployeeId`, `MachineId`

## Completed — Manufacturing Phase 2: Shared UI Framework

- **`SplitTableView`** — Domain-agnostic sortable data table with generic type parameter, column definitions, filter chips, search, row selection with side panel
- Reusable across all manufacturing views

## Completed — Manufacturing Phase 3: Manufacturing UI Views

7 operational views for print shop management:

- **Dashboard** — Summary cards, station status strip (14 tiles), recent orders table, "My In-Progress Jobs"
- **Station Queue** — Primary operator view with station/status/priority filters, Start/Complete actions, side panel with workflow progress
- **Order Approval** — Manager/prepress view with approval queue, side panel with full order details
- **Order Progress** — Fulfilment tracking with progress bars, deadline urgency coloring, workflow visualization
- **Employees** — Employee profiles with station capability toggles
- **Machines** — Machine registry with Online/Offline/Maintenance status controls
- **Analytics** — KPI cards, station performance, employee throughput, bottleneck alerts
- Sidebar navigation, client-side routing, responsive mobile layout

## Completed — Manufacturing Phase 4: Workflow Engine

Pure state machine over `ManufacturingWorkflow` returning `Validation[WorkflowError, ManufacturingWorkflow]`:

- **`startStep`** — Ready → InProgress, assigns employee, enforces DAG dependencies
- **`completeStep`** — InProgress → Completed, auto-promotes downstream Waiting → Ready
- **`failStep`** — InProgress → Failed, workflow → OnHold, appends failure reason
- **`skipStep`** — Waiting/Ready → Skipped (Prepress, QC, Packaging non-skippable)
- **`resetStep`** — Completed/Failed/Skipped → Ready with `isRework` flag, reverts downstream to Waiting
- **`QueueScorer`** — Advisory priority scoring: deadline urgency, priority boost, completeness, batch affinity, age tiebreaker
- **`WorkflowError`** ADT — 9 variants with English and Czech messages

## Completed — Manufacturing Phase 5: Employee & Machine Management

- **`EmployeeManagementService`** — Pure CRUD: add, update, toggle active, update capabilities, remove
- **`MachineManagementService`** — Pure CRUD: add, update, change status, change station type, remove
- **`ManagementError`** ADT — 7 variants with English and Czech messages
- **UI** — `EmployeesView` with capability toggle grid, `MachinesView` with status controls, Dashboard "My In-Progress Jobs"

## Completed — Manufacturing Phase 6: Order Approval Enhancements

- **`ArtworkCheck`** — Per-file validation flags (resolution, bleed, color profile) with `CheckStatus` enum
- **`PaymentStatus`** enum — Pending/Confirmed/Failed
- Order-level **priority** drives workflow generation
- **Enhanced approval panel** — artwork review buttons, payment verification, priority selector, Hold/Request Changes

## Completed — Manufacturing Phase 7: Fulfilment Workflow

- **`FulfilmentChecklist`** — 4-step dispatch model: collect items → quality sign-off → packaging → dispatch (gated)
- Auto-created when all workflows reach Completed
- Progress bar (0/4 → 4/4) in Order Progress view

## Completed — Manufacturing Phase 8: Analytics & Reporting

- **`AnalyticsService`** — Pure: `averageTimePerStation`, `bottleneckStation`, `employeeThroughput`, `onTimeDeliveryRate`
- KPI cards, station performance table with load indicators, employee throughput with bars, bottleneck alert banner

---

## Testing

**341 passing tests** across 14 test suites:

- **ConfigurationBuilderSpec** — 34 tests
- **CatalogQueryServiceSpec** — 17 tests
- **PriceCalculatorSpec** — 14 tests
- **LocalizationSpec** — 17 tests
- **BasketServiceSpec** — 17 tests
- **WorkflowGeneratorSpec** — 19 tests
- **WorkflowEngineSpec** — 27 tests
- **QueueScorerSpec** — 24 tests
- **EmployeeManagementServiceSpec** — 17 tests
- **MachineManagementServiceSpec** — 16 tests
- **ArtworkCheckSpec** — 15 tests
- **FulfilmentChecklistSpec** — 17 tests
- **AnalyticsServiceSpec** — 13 tests
- **WeightCalculatorSpec** — weight calculation

---

## Roadmap

### Persistence

- Add ZIO-based repository interfaces in the domain layer
- Implement persistence adapters (e.g. JSON file store or database)
- Store and retrieve product catalogs, rulesets, pricelists, and saved configurations
- Support catalog versioning for rule/data evolution

### Production Readiness

- API layer (ZIO HTTP or similar) for server-side validation and pricing
- Admin interface for managing catalogs, rules, and pricelists without code changes
- PDF proof generation
- Integration with print production systems
- Invoice management and customer management views
