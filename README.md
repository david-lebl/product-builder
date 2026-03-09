# Product Builder

> ⚠️ **Note**: This project was built with the assistance of an LLM (Large Language Model). 

A custom product builder for the printing industry that lets customers configure products from categories, materials, finishes, and specifications — with declarative compatibility rules ensuring only valid combinations are possible. Includes a visual product editor for designing calendars, photo books, and wall pictures with per-page customization.

## Vision

Product Builder is a domain-driven design (DDD) approach to building a flexible, maintainable product configuration system for print service providers. It enables customers to compose print products (business cards, flyers, banners, calendars, photo books, etc.) by selecting materials, finishes, and specifications, while automatically enforcing business rules to ensure only producible combinations can be ordered. The visual editor allows customers to design multi-page products with photos, text, and shapes.

**🌐 Live Demo:** [https://david-lebl.github.io/product-builder/](https://david-lebl.github.io/product-builder/)

![Calendar Builder UI](https://github.com/user-attachments/assets/c5f6eb00-698b-4f32-abf7-f4034b2f9982)

## Tech Stack

- **Scala 3.3.3** — Strongly-typed functional programming with DDD principles
- **ZIO 2.x** — Effects management for infrastructure and async operations
- **ZIO Prelude** — Validation with error accumulation (not short-circuiting)
- **ZIO Test** — Property-based and unit testing
- **Scala.js + Laminar** — Interactive reactive web UI (cross-compiled domain model)

## Key Design Principles

### 1. Rules as Data, Not Code
Compatibility rules are modeled as a sealed ADT (`CompatibilityRule`), interpreted by a rule engine. Adding new materials or finishes doesn't require code changes—just update the rule definitions.

### 2. Error Accumulation Over Short-Circuiting
Uses `ZIO Prelude Validation` to collect all validation errors at once, providing comprehensive feedback instead of failing on the first error.

### 3. Domain Logic Stays Pure
The domain layer contains only `Validation[...]` computations, never `ZIO[...]` effects. This keeps the domain layer pure, testable, and Scala.js-compatible.

### 4. Specification Pattern as Composable ADT
`ConfigurationPredicate` uses algebraic data types with And/Or/Not instead of opaque predicates (`A => Boolean`), making rules inspectable, serializable, and debuggable.

### 5. Progressive Disclosure
`CatalogQueryService` pre-filters valid options based on the current configuration, so the UI shows only compatible choices at each step.

## Project Structure

```
modules/
├── domain/src/main/scala/mpbuilder/domain/
│   ├── model/                    # Core domain entities
│   │   ├── ids.scala             # CategoryId, MaterialId, FinishId, etc.
│   │   ├── material.scala        # Paper family, weight, properties
│   │   ├── finish.scala          # Finish type, side (single/both)
│   │   ├── category.scala        # Category constraints & requirements
│   │   ├── specification.scala   # 8 spec kinds (size, pages, quantity, binding, etc.)
│   │   ├── configuration.scala   # The aggregate root
│   │   ├── catalog.scala         # Product catalog container
│   │   ├── printingmethod.scala  # Printing methods (offset, digital, etc.)
│   │   ├── language.scala        # i18n: Language enum, LocalizedString
│   │   └── basket.scala          # Shopping basket model
│   │
│   ├── rules/                    # Compatibility rules & predicates
│   │   ├── CompatibilityRule.scala     # 12 rule variants
│   │   ├── CompatibilityRuleset.scala  # Versioned rule container
│   │   └── predicates.scala            # SpecPredicate & ConfigurationPredicate
│   │
│   ├── service/                  # Domain services
│   │   ├── ConfigurationBuilder.scala  # Resolve + validate + build
│   │   ├── CatalogQueryService.scala   # Filter valid options for UI
│   │   ├── BasketService.scala         # Shopping basket operations
│   │   └── BasketError.scala           # Basket error ADT
│   │
│   ├── validation/               # Validation logic
│   │   ├── ConfigurationError.scala    # 19 error types with localized messages
│   │   ├── ConfigurationValidator.scala # Two-layer validation pipeline
│   │   └── RuleEvaluator.scala         # Rule interpretation engine
│   │
│   ├── pricing/                  # Pricing system
│   │   ├── Money.scala           # Money opaque type (BigDecimal, never Double)
│   │   ├── PricingRule.scala     # 7-variant pricing rule ADT
│   │   ├── Pricelist.scala       # Versioned pricelist container
│   │   ├── PricingError.scala    # Pricing error ADT
│   │   ├── PriceBreakdown.scala  # Line items + breakdown output
│   │   └── PriceCalculator.scala # Pure pricing interpreter
│   │
│   └── sample/                   # Reference implementation
│       ├── SampleCatalog.scala   # 11 categories, 13 materials, 16 finishes, 4 methods
│       ├── SampleRules.scala     # 29 compatibility rules
│       └── SamplePricelist.scala # Pricing for all materials + key finishes
│
├── ui/src/main/scala/mpbuilder/ui/
│   ├── Main.scala                # Application entry point
│   ├── AppRouter.scala           # Client-side routing (Product Parameters / Visual Editor)
│   ├── ProductBuilderApp.scala   # Product configuration view
│   ├── ProductBuilderViewModel.scala # Reactive state management
│   ├── components/               # Product configuration UI components
│   │   ├── CategorySelector.scala
│   │   ├── MaterialSelector.scala
│   │   ├── PrintingMethodSelector.scala
│   │   ├── FinishSelector.scala
│   │   ├── SpecificationForm.scala
│   │   ├── ConfigurationForm.scala
│   │   ├── PricePreview.scala
│   │   ├── ValidationMessages.scala
│   │   └── BasketView.scala
│   └── calendar/                 # Visual product editor
│       ├── CalendarBuilderApp.scala    # Main visual editor view
│       ├── CalendarModel.scala         # Visual editor model (product types, formats,
│       │                               #   canvas elements, templates, pages)
│       ├── CalendarViewModel.scala     # Reactive state for the visual editor
│       └── components/
│           ├── CalendarPageCanvas.scala # Interactive canvas with drag/resize/rotate
│           ├── ElementListEditor.scala  # Element management & property editors
│           ├── BackgroundEditor.scala   # Page background & template settings
│           └── PageNavigation.scala     # Horizontal page strip with thumbnails

docs/
├── pricing.md                    # Pricing system documentation with worked examples
├── ui-guide.md                   # Running and testing guide
├── visual-product-types.md       # Visual product types, formats, and domain mapping
├── printing-domain-analysis.md   # Printing industry domain analysis
└── domain-model-gap-analysis.md  # Domain model gap analysis
```

## Core Components

### 📦 Value Objects
- **Opaque type IDs**: `CategoryId`, `MaterialId`, `FinishId`, `PrintingMethodId`, `ConfigurationId`, `BasketId` with smart constructors
- **Physical properties**: `PaperWeight`, `Quantity`, `Dimension`
- **Enumerations**: `InkType`, `PaperFamily`, `FinishType`, `FinishSide`, `MaterialFamily`, `MaterialProperty`
- **Ink configuration**: `InkSetup` (ink type + color count), `InkConfiguration` (front + optional back setup)
- **i18n**: `Language` enum (`En`, `Cs`), `LocalizedString` opaque type with fallback

### 📋 Domain Model
- **`Material`** — Material family, weight, properties, localized name
- **`Finish`** — Finish type, side application, localized name
- **`PrintingMethod`** — Printing process type, localized name
- **`ProductCategory`** — Allowed materials/finishes/printing methods and required specifications
- **`ProductSpecification`** — 8 spec kinds (Size, Pages, Quantity, BindingMethod, ColorMode, Orientation, Bleed, FoldType)
- **`ProductConfiguration`** — Aggregate root combining category, material, printing method, finishes, specs
- **`ProductCatalog`** — Container for all categories, materials, finishes, printing methods, and rules
- **`Basket`** / **`BasketItem`** — Shopping basket with quantity and pre-calculated pricing

### 🔧 Rules Engine
**Compatibility Rules** (sealed ADT with 12 variants):
- `MaterialFinishIncompatible` — Forbids specific material-finish pairs
- `MaterialRequiresFinish` — Material requires a specific finish type
- `FinishRequiresMaterialProperty` — Finish requires material to have a property
- `FinishMutuallyExclusive` — Certain finishes cannot be combined
- `SpecConstraint` — Enforce spec-level constraints (size ranges, page counts, etc.)
- `MaterialPropertyFinishTypeIncompatible` — Material property conflicts with finish type
- `MaterialFamilyFinishTypeIncompatible` — Material family conflicts with finish type
- `MaterialWeightFinishType` — Finish type requires minimum material weight
- `FinishTypeMutuallyExclusive` — Finish types that cannot be combined
- `FinishCategoryExclusive` — Only one finish per category allowed
- `FinishRequiresFinishType` — Finish depends on another finish type
- `FinishRequiresPrintingProcess` — Finish requires a specific printing process

**Predicates** — `SpecPredicate` and `ConfigurationPredicate` with boolean algebra (And, Or, Not) for composable rule conditions

### 💰 Pricing
- **`Money`** — Opaque type over `BigDecimal` (never `Double`), rounded to 2dp
- **`PricingRule`** — 7 variants: base price, area price, finish/type/process/category surcharges, quantity tiers
- **`PriceCalculator`** — Pure interpreter: config + pricelist → `Validation[PricingError, PriceBreakdown]`
- **`PriceBreakdown`** — Detailed output with line items, subtotal, multiplier, and total

### ✅ Validation Pipeline
Two-layer validation:
1. **Structural validation** — Ensure category/material/finish selections are valid
2. **Rule evaluation** — Check all compatibility rules against the configuration
3. **Error accumulation** — Collect all errors, return comprehensive localized feedback

### 📡 Services
- **`ConfigurationBuilder`** — Orchestrates resolution, validation, and construction
- **`CatalogQueryService`** — Filters valid options based on current state for UI guidance
- **`BasketService`** — Shopping basket operations (add, remove, update quantity, calculate totals)

## Features

### 🌐 Internationalization (i18n)
- `Language` enum (`En`, `Cs`) with `LocalizedString` opaque type
- All domain entities (materials, finishes, categories, printing methods) have localized names
- All validation and pricing error messages support Czech and English
- UI language selector with browser language detection and localStorage persistence

### 🛒 Shopping Basket
- Add configured products with quantity to basket
- Update quantities, remove items, clear basket
- Real-time total calculation with currency support
- Pure functional `BasketService` with `Validation`-based error handling

### 📅 Visual Product Editor
- **Product types**: Monthly Calendar (12p), Weekly Calendar (52p), Bi-weekly Calendar (26p), Photo Book (12p), Wall Picture (1p)
- **Domain-driven formats**: 10 physical formats with precise mm dimensions (wall/desk calendars, photo books, wall pictures)
- ADT-based canvas element model: `PhotoElement`, `TextElement`, `ShapeElement`, `ClipartElement`
- Photo upload, resize, rotate, and position on canvas — including template image placeholders as interactive `PhotoElement`s
- Text elements with bold/italic/alignment formatting
- Shape elements (lines, rectangles) with stroke and fill colors
- Page backgrounds (solid color or image)
- Unified element list with z-ordering (bring to front / send to back), duplication, and deletion
- Horizontal scrollable page navigation strip — handles 52+ pages
- Sidebar tabs: "Page Elements" / "Background" for reduced visual noise
- Format selector dynamically filtered by product type

### 🎨 Product Configuration UI
- Step-by-step wizard: category → material → printing method → finishes → specifications
- Progressive disclosure with real-time compatibility filtering
- Live validation feedback with localized error messages
- Real-time price calculation with detailed breakdown
- Client-side routing between Product Parameters and Visual Editor views
- Modern responsive design with purple gradient theme

## Status

### ✅ Phase 1: Domain Model (Complete)
- Core domain entities and value objects with opaque type IDs
- Sealed ADT for rules (12 variants) and predicates
- Two-layer validation with error accumulation (19 error types)
- Domain services for building & querying
- Sample data: 7 categories, 9 materials, 14 finishes, 4 printing methods, 24 compatibility rules

### ✅ Phase 2: Pricing (Complete)
- `Money` opaque type over `BigDecimal` with 2dp rounding
- `PricingRule` sealed enum with 7 variants
- `PriceCalculator` pure interpreter with area-based pricing, finish surcharge precedence, and quantity tier discounts
- `PriceBreakdown` with detailed line items
- See [docs/pricing.md](docs/pricing.md) for worked examples

### ✅ Phase 3: UI with Scala.js + Laminar (Complete)
- Cross-compiled domain model to JavaScript (Scala 3.3.3 for Scala.js compatibility)
- Interactive web UI using Laminar reactive framework
- Step-by-step configuration wizard with progressive disclosure
- Real-time validation feedback and compatibility filtering
- Live price calculation with detailed breakdown
- Modern responsive design with purple gradient theme
- Deployed to GitHub Pages via GitHub Actions

### ✅ Phase 4: Internationalization (Complete)
- `Language` enum and `LocalizedString` opaque type with English fallback
- All domain entities and error messages localized (English + Czech)
- UI language selector with browser language detection and localStorage persistence
- Localized pricing line item labels

### ✅ Phase 5: Extended Catalog (Complete)
- Added Calendars category with binding/page constraints
- Added 4 materials: Yupo Synthetic, Cotton Paper, Coated Silk, Adhesive Stock
- Added 7 finishes: Soft Touch, Aqueous, Debossing, Scoring, Perforation, Round Corners, Grommets
- Expanded compatibility rules to 24

### ✅ Phase 6: Shopping Basket (Complete)
- `Basket` / `BasketItem` domain model with quantity and pricing
- `BasketService` with pure functional operations (add, remove, update, calculate total)
- `BasketError` ADT with localized messages
- Full basket UI with item management, quantity updates, and total calculation

### ✅ Phase 7: Photo Calendar Builder (Complete)
- ADT-based `CanvasElement` model (Photo, Text, Shape, Clipart)
- 12-page editor with per-page backgrounds and element management
- Canvas with drag, resize, rotate for all element types
- Unified element list with z-ordering, duplication, and type-specific editors
- Client-side routing between Product Builder and Calendar Builder

### ✅ Phase 8: Visual Product Editor Generalization (Complete)
- Extended calendar builder into a general visual product editor
- 5 product types: Monthly Calendar (12p), Weekly Calendar (52p), Bi-weekly Calendar (26p), Photo Book (12p), Wall Picture (1p)
- Domain-driven `ProductFormat` with physical dimensions: 10 formats across calendar (wall/desk), photo book (square/landscape/portrait), and wall picture
- Template image placeholders replaced with interactive `PhotoElement` — selectable, draggable, resizable, with upload/replace/clear
- Horizontal scrollable page navigation strip (handles 52+ pages)
- Sidebar tab view ("Page Elements" / "Background") to reduce visual noise
- Format selector dynamically filtered by product type
- See [docs/visual-product-types.md](docs/visual-product-types.md) for supported types and formats

**99 passing tests** across 5 test suites.

**Try it locally:** See [docs/ui-guide.md](docs/ui-guide.md) for instructions on running the UI locally.

### 🚧 Roadmap

**Persistence**
- ZIO-based repository interfaces
- Adapters for JSON/database storage
- Catalog versioning and rule evolution

**Production Readiness**
- ZIO HTTP API for server-side validation & pricing
- Admin interface for catalog/rule management
- Order submission workflow
- PDF proof generation
- Integration with print production systems

## Getting Started

### Prerequisites
- Java 11+ (Java 17 recommended)
- sbt 1.12.3+ (Scala Build Tool)
- A modern web browser

### Build & Test

```bash
# Compile all modules (domain JVM + JS, UI)
sbt compile

# Run all tests (99 tests across 5 suites)
sbt test

# Run a single test suite
sbt "testOnly mpbuilder.domain.ConfigurationBuilderSpec"

# Run tests with verbose output
sbt "testOnly * -- -v"

# Compile only the UI (Scala.js)
sbt ui/fastLinkJS

# Production-optimized JS build
sbt ui/fullLinkJS
```

### Run the UI Locally

```bash
# 1. Build the UI JavaScript
sbt ui/fastLinkJS

# 2. Copy files to a dist directory
mkdir -p dist
cp modules/ui/src/main/resources/index.html dist/
cp modules/ui/target/scala-3.3.3/material-builder-ui-fastopt/main.js dist/

# 3. Serve with any HTTP server
cd dist && python3 -m http.server 8080

# 4. Open http://localhost:8080/index.html
```

For development with auto-recompilation:
```bash
sbt ~ui/fastLinkJS
```

See [docs/ui-guide.md](docs/ui-guide.md) for detailed instructions including development workflow, production builds, and troubleshooting.

## Testing

The project includes **99 passing tests** across 5 test suites:

```bash
sbt test
```

Key test suites:
- **ConfigurationBuilderSpec** — Valid/invalid configurations, error accumulation (34 tests)
- **CatalogQueryServiceSpec** — Material/finish/spec filtering (17 tests)
- **PriceCalculatorSpec** — Pricing calculations, breakdowns, edge cases (14 tests)
- **LocalizationSpec** — i18n, localized strings, error messages (17 tests)
- **BasketServiceSpec** — Basket operations, quantity management, totals (17 tests)

## Sample Catalog

The included `SampleCatalog` provides a reference implementation:

**Categories:**
- Business Cards, Flyers, Brochures, Banners, Packaging, Booklets, Calendars, Postcards, Stickers, **Roll-Up Banners**

**Materials:**
- Coated Art Paper 300gsm, Uncoated Bond 120gsm, Kraft Card 280gsm, Adhesive Vinyl, Corrugated Board, Coated Silk 250gsm, Yupo Synthetic 200μm, Adhesive Stock 100gsm, Cotton Paper 300gsm
- Coated Art Paper Glossy (9 weights: 90–350gsm), Coated Art Paper Matte (9 weights: 90–350gsm)
- **Polyester Banner Film 510gsm** (roll-up printing substrate), **Roll-Up Stand Economy**, **Roll-Up Stand Premium**

**Finishes:**
- Matte Lamination, Gloss Lamination, UV Coating, Embossing, Foil Stamping, Die Cut, Varnish, Soft Touch Coating, Aqueous Coating, Debossing, Scoring, Perforation, Round Corners, Grommets, Kiss Cut, **Overlamination**

**Printing Methods:**
- Offset, Digital, UV Curable Inkjet, Letterpress

**Rules:**
- 29 compatibility rules covering material-finish restrictions, weight requirements, finish dependencies, property constraints, spec constraints, and roll-up size constraints

## Design Highlights

### Why ZIO Prelude Validation?
Short-circuiting error handling (like `Either`) provides only the first error. `Validation` collects all errors, giving users complete feedback on what went wrong.

Example:
```scala
// With Either: one error, then fail
// With Validation: all errors at once
val config = ConfigurationBuilder.build(
  categoryId = BusinessCards,
  materialId = Kraft,
  finishId = Foil,
  specs = Seq(...)
)
// Returns: Chunk(Error1, Error2, Error3) — user knows all issues
```

### Why ADT Rules Over Functions?
Rules are data, not logic. This enables:
- **Serialization** — Save rules to JSON/database
- **Introspection** — Analyze which rules apply
- **Dynamic loading** — Update rules without redeployment
- **Admin UI** — Non-technical users configure rules

### Why `ConfigurationPredicate` Over `A => Boolean`?
```scala
// ❌ Opaque — can't inspect or serialize
val pred: ProductSpecification => Boolean = ???

// ✅ Transparent — inspectable, serializable, composable
sealed trait ConfigurationPredicate
case class SizeInRange(min: Dimension, max: Dimension) extends ConfigurationPredicate
case class And(left: ConfigurationPredicate, right: ConfigurationPredicate) extends ConfigurationPredicate
case class Or(left: ConfigurationPredicate, right: ConfigurationPredicate) extends ConfigurationPredicate
case class Not(pred: ConfigurationPredicate) extends ConfigurationPredicate
```

## Contributing

This is a reference implementation of DDD principles applied to a print-industry domain. Feel free to adapt it to your domain model.

## License

[Specify your license here]

