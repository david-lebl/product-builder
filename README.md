# Product Builder

> ⚠️ **Note**: This project was built with the assistance of an LLM (Large Language Model).

A domain-driven product configuration and manufacturing management system for the printing industry. Customers configure print products, design visual layouts, place orders — and shop operators manage the full production lifecycle from approval through dispatch.

**🌐 Live Demo:** [https://david-lebl.github.io/product-builder/](https://david-lebl.github.io/product-builder/)

| Product Configurator | Manufacturing Dashboard |
|:---:|:---:|
| ![Product Configurator](https://github.com/user-attachments/assets/3673282f-ccc0-4976-bbce-72a8d453521b) | ![Manufacturing Dashboard](https://github.com/user-attachments/assets/998f0cce-9f0b-49b3-8712-af5dee5a88e5) |

| Visual Editor |
|:---:|
| ![Visual Editor](https://github.com/user-attachments/assets/b063a3ab-d73c-4c33-ac19-65d5ba4c0425) |

## What It Does

1. **Configure** — Select a product category, material, printing method, finishes, and specifications. A rules engine enforces only valid, producible combinations.
2. **Design** — Use the visual editor to layout calendars, photo books, or wall pictures with photos, text, shapes, and clipart on a per-page canvas.
3. **Price** — See real-time pricing with detailed breakdowns including material costs, finish surcharges, setup fees, and volume discounts.
4. **Order** — Add products to a shopping basket, apply discount codes, and check out with delivery options.
5. **Manufacture** — Operators manage production through a dashboard with station queues, order approval, fulfilment tracking, employee/machine management, and analytics.

## Tech Stack

| Technology | Role |
|------------|------|
| **Scala 3.3.3** | Strongly-typed functional programming with DDD principles |
| **ZIO Prelude** | `Validation` with error accumulation (not short-circuiting) |
| **ZIO Test** | Unit testing framework (341 tests) |
| **Scala.js** | Cross-compilation of domain model to JavaScript |
| **Laminar 17.2.0** | Reactive web UI framework |
| **Mill 1.1.3** | Primary build tool (sbt also supported) |
| **GitHub Actions** | CI/CD with deployment to GitHub Pages |

## Key Design Principles

- **Rules as data, not code** — Compatibility rules (`CompatibilityRule`, 12 variants) and pricing rules (`PricingRule`, 17 variants) are sealed ADTs interpreted by engines. New materials or finishes require only rule data updates, not code changes.
- **Error accumulation** — ZIO Prelude `Validation` collects all errors at once, giving users comprehensive feedback instead of failing on the first problem.
- **Pure domain layer** — All domain logic returns `Validation[E, A]`, never `ZIO[...]` effects. This keeps the domain testable and Scala.js-compatible.
- **Composable predicates** — `ConfigurationPredicate` uses And/Or/Not ADT instead of opaque `A => Boolean`, making rules inspectable, serializable, and debuggable.
- **Progressive disclosure** — `CatalogQueryService` pre-filters valid options at each step, so the UI shows only compatible choices.

## Project Structure

```
modules/
├── domain/                          # Core domain (cross-compiled JVM + JS)
│   ├── model/                       # Entities, value objects, enums
│   │   ├── ids.scala                #   Opaque type IDs with smart constructors
│   │   ├── material.scala           #   Material family, weight, properties
│   │   ├── finish.scala             #   Finish type, side application
│   │   ├── category.scala           #   Category constraints & component templates
│   │   ├── specification.scala      #   8 spec kinds (size, pages, quantity, etc.)
│   │   ├── configuration.scala      #   ProductConfiguration — the aggregate root
│   │   ├── catalog.scala            #   ProductCatalog container
│   │   ├── printingmethod.scala     #   Printing methods & process types
│   │   ├── language.scala           #   Language enum, LocalizedString
│   │   ├── basket.scala             #   Shopping basket model
│   │   ├── order.scala              #   Order, customer, delivery
│   │   ├── component.scala          #   Multi-component product support
│   │   └── manufacturing.scala      #   Workflow, steps, stations, fulfilment
│   ├── rules/                       # Compatibility rules (12-variant ADT)
│   ├── validation/                  # Two-layer validation pipeline
│   ├── pricing/                     # Pricing engine (17-variant ADT)
│   ├── service/                     # Domain services
│   │   ├── ConfigurationBuilder     #   Resolve + validate + build
│   │   ├── CatalogQueryService      #   Progressive disclosure filtering
│   │   ├── BasketService            #   Shopping basket operations
│   │   ├── DiscountService          #   Promotional discount codes
│   │   ├── WorkflowGenerator        #   Config → manufacturing workflow DAG
│   │   ├── WorkflowEngine           #   Step state machine (start/complete/fail/skip/reset)
│   │   ├── QueueScorer              #   Priority scoring for operator queues
│   │   ├── EmployeeManagementService #  Employee CRUD
│   │   ├── MachineManagementService #   Machine registry CRUD
│   │   └── AnalyticsService         #   Production analytics & KPIs
│   ├── weight/                      # Weight calculation for shipping
│   └── sample/                      # Reference data (11 categories, 13 materials, etc.)
│
├── ui/                              # Scala.js web application
│   ├── AppRouter                    #   Client-side routing (3 views)
│   ├── ProductBuilderApp            #   Product configuration wizard
│   ├── calendar/                    #   Visual product editor
│   │   ├── CalendarModel            #     5 product types, 10 formats, canvas elements
│   │   ├── CalendarBuilderApp       #     Editor view with canvas, sidebar, page nav
│   │   └── components/              #     Canvas, element list, background, page strip
│   ├── manufacturing/               #   Manufacturing management
│   │   ├── ManufacturingApp         #     Sidebar navigation + route switching
│   │   ├── ManufacturingViewModel   #     Reactive state management
│   │   └── views/                   #     7 views (see below)
│   └── components/                  #   Configurator UI components
│
├── ui-framework/                    # Reusable Laminar component library (no domain dep)
│   ├── fields/                      #   TextField, SelectField, CheckboxField, RadioGroup
│   ├── containers/                  #   Tabs, Stepper, SplitTableView
│   ├── form/                        #   Mirror-based form state derivation
│   └── feedback/                    #   ValidationDisplay
│
└── ui-showcase/                     # Standalone UI kit demo app

docs/
├── features.md                      # Comprehensive feature overview
├── pricing.md                       # Pricing system with worked examples
├── visual-product-types.md          # Visual editor types & formats
├── manufacturing-implementation-plan.md  # Manufacturing phases 1–8
├── ui-guide.md                      # Build, run & test instructions
└── analysis/                        # Research & planning documents
    ├── printing-domain-analysis.md
    ├── domain-model-gap-analysis.md
    ├── manufacturing-workflow-analysis.md
    ├── sheet-based-pricing.md
    ├── roll-up.md
    └── ui-kit-review.md
```

## Features

### 🎨 Product Configuration

- **11 categories** — Business Cards, Postcards, Flyers, Brochures, Booklets, Calendars, Banners, Packaging, Stickers & Labels, Roll-Up Banners, Free Configuration
- **13 materials** with paper families, weights, and properties
- **16 finishes** — lamination, UV coating, embossing, foil stamping, die-cut, and more
- **4 printing methods** — Digital, Offset, Letterpress, UV Inkjet
- **Multi-component products** (e.g. booklet cover + body, roll-up banner + stand)
- **Per-side ink configuration** — CMYK 4/4, 4/0, 4/1, mono 1/0, 1/1
- **12 compatibility rules** with composable predicates (And/Or/Not)
- Step-by-step wizard with progressive disclosure and real-time validation

### 💰 Pricing Engine

- **17 pricing rule types** — material (base/area/sheet), finish surcharges (ID overrides type), process/category surcharges, ink configuration factor, cutting, fold/binding surcharges, quantity/sheet tiers, setup fees, minimum order price
- Sheet-based pricing with nesting/imposition calculation
- Real-time price breakdown with per-component detail
- Multiple currency support (USD, CZK)
- See [docs/pricing.md](docs/pricing.md) for worked examples

### 📅 Visual Product Editor

- **5 product types**: Monthly Calendar (12p), Weekly Calendar (52p), Bi-weekly Calendar (26p), Photo Book (12p), Wall Picture (1p)
- **10 physical formats** with precise mm dimensions
- Canvas elements: photos, text (bold/italic/alignment), shapes (rectangles/lines), clipart
- Drag, resize, rotate, z-order, duplicate, delete for all elements
- Per-page backgrounds (solid color or image)
- Horizontal scrollable page navigation (handles 52+ pages)
- See [docs/visual-product-types.md](docs/visual-product-types.md) for all types and formats

### 🛒 Shopping & Checkout

- Shopping basket with quantity management and real-time totals
- Discount codes via `DiscountService`
- Checkout with customer details, delivery options, and order summary

### 🏭 Manufacturing System

A complete production management suite for print shops with 7 operational views:

| View | Purpose |
|------|---------|
| **Dashboard** | Summary cards, station status strip, recent orders, "My In-Progress Jobs" |
| **Station Queue** | Pull-model operator view with priority scoring, Start/Complete actions |
| **Order Approval** | Artwork checks, payment verification, priority & deadline management |
| **Order Progress** | Workflow visualization, deadline urgency, fulfilment tracking |
| **Employees** | Employee profiles, station capabilities, current employee selector |
| **Machines** | Machine registry with Online/Offline/Maintenance status controls |
| **Analytics** | KPI cards, station performance, employee throughput, bottleneck alerts |

Key capabilities:
- **DAG-based workflows** — `WorkflowGenerator` derives step sequence from product configuration, mapping printing methods and finishes to 14 station types
- **Workflow engine** — Pure state machine: `startStep`, `completeStep`, `failStep`, `skipStep`, `resetStep` with DAG dependency enforcement
- **Queue priority scoring** — Deadline urgency (0–100), priority boost, completeness, batch affinity, age tiebreaker
- **Artwork checks** — Per-file resolution/bleed/color profile validation flags
- **Fulfilment workflow** — 4-step dispatch: collect items → quality sign-off → packaging → dispatch (gated)
- **Analytics** — Average time per station, bottleneck detection, employee throughput, on-time delivery rate

See [docs/manufacturing-implementation-plan.md](docs/manufacturing-implementation-plan.md) for full implementation details.

### 🌐 Internationalization

- English and Czech (`En`, `Cs`) with `LocalizedString` opaque type
- All entities, error messages, and UI components fully localized
- Browser language detection with `localStorage` persistence

## Getting Started

### Prerequisites

- Java 11+ (Java 17 recommended)
- Mill 1.1.3+ (included as bootstrap script) or sbt (Scala Build Tool)
- A modern web browser

### Build & Test

#### Using Mill (recommended)

```bash
./mill domain.jvm.compile          # Compile domain module (JVM)
./mill domain.jvm.test             # Run all domain tests
./mill ui.compile                  # Compile UI module (Scala.js)
./mill ui.fastLinkJS               # Build UI JavaScript (development)
./mill ui.fullLinkJS               # Build UI JavaScript (production, optimized)
```

#### Using sbt (legacy)

```bash
sbt compile                    # Compile all modules (domain JVM + JS, UI)
sbt test                       # Run all 341 tests (14 suites)
sbt domainJVM/test             # Run domain tests only (faster)
sbt ui/fastLinkJS              # Build UI JavaScript (development)
sbt ui/fullLinkJS              # Build UI JavaScript (production, optimized)
```

### Run Locally

#### Using Mill

```bash
./mill ui.fastLinkJS

mkdir -p dist
cp modules/ui/src/main/resources/index.html dist/
cp modules/ui/src/main/resources/*.css dist/
cp out/ui/fastLinkJS.dest/main.js dist/

cd dist && python3 -m http.server 8080
# Open http://localhost:8080
```

#### Using sbt

```bash
sbt ui/fastLinkJS

mkdir -p dist
cp modules/ui/src/main/resources/index.html dist/
cp modules/ui/src/main/resources/*.css dist/
cp modules/ui/target/scala-3.3.3/material-builder-ui-fastopt/main.js dist/

cd dist && python3 -m http.server 8080
# Open http://localhost:8080
```

For development with auto-recompilation: `sbt ~ui/fastLinkJS`

See [docs/ui-guide.md](docs/ui-guide.md) for detailed instructions.

## Testing

**341 passing tests** across 14 test suites:

| Suite | Tests | Area |
|-------|------:|------|
| ConfigurationBuilderSpec | 34 | Product configuration & validation |
| CatalogQueryServiceSpec | 17 | Progressive disclosure filtering |
| PriceCalculatorSpec | 14 | Pricing calculations & breakdowns |
| LocalizationSpec | 17 | i18n, LocalizedString, error messages |
| BasketServiceSpec | 17 | Basket operations & totals |
| WorkflowGeneratorSpec | 19 | Workflow DAG generation |
| WorkflowEngineSpec | 27 | State transitions & DAG enforcement |
| QueueScorerSpec | 24 | Priority scoring & sorting |
| EmployeeManagementServiceSpec | 17 | Employee CRUD & capabilities |
| MachineManagementServiceSpec | 16 | Machine CRUD & status transitions |
| ArtworkCheckSpec | 15 | Artwork validation flags |
| FulfilmentChecklistSpec | 17 | Dispatch checklist & gating |
| AnalyticsServiceSpec | 13 | Production metrics & KPIs |
| WeightCalculatorSpec | — | Weight calculation |

## Documentation

- **[docs/features.md](docs/features.md)** — Comprehensive feature overview
- **[docs/pricing.md](docs/pricing.md)** — Pricing system with worked examples
- **[docs/visual-product-types.md](docs/visual-product-types.md)** — Visual editor types & formats
- **[docs/manufacturing-implementation-plan.md](docs/manufacturing-implementation-plan.md)** — Manufacturing phases 1–8
- **[docs/ui-guide.md](docs/ui-guide.md)** — Build, run & test instructions
- **[docs/analysis/](docs/analysis/)** — Research & planning documents

## License

MIT — see [LICENSE](LICENSE) for details.

