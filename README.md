# Material Builder

> ⚠️ **Note**: This project was built with the assistance of an LLM (Large Language Model). 

A custom product builder for the printing industry that lets customers configure products from categories, materials, finishes, and specifications — with declarative compatibility rules ensuring only valid combinations are possible.

## Vision

Material Builder is a domain-driven design (DDD) approach to building a flexible, maintainable product configuration system for print service providers. It enables customers to compose print products (business cards, flyers, banners, etc.) by selecting materials, finishes, and specifications, while automatically enforcing business rules to ensure only producible combinations can be ordered.

## Tech Stack

- **Scala 3.8.1** — Strongly-typed functional programming with DDD principles
- **ZIO 2.x** — Effects management for infrastructure and async operations
- **ZIO Prelude** — Validation with error accumulation (not short-circuiting)
- **ZIO Test** — Property-based and unit testing
- **Scala.js + Laminar** (future) — Interactive web UI

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
src/main/scala/mpbuilder/domain/
├── model/                    # Core domain entities
│   ├── ValueObjects.scala    # CategoryId, MaterialId, FinishId, Quantity, Dimension, etc.
│   ├── Material.scala        # Paper family, weight, properties
│   ├── Finish.scala          # Finish type, side (single/both)
│   ├── ProductCategory.scala # Category constraints & requirements
│   ├── Specification.scala   # 7 spec variants (size, pages, quantity, etc.)
│   └── ProductConfiguration.scala  # The aggregate root
│
├── rules/                    # Compatibility rules & predicates
│   ├── CompatibilityRule.scala     # 5 rule variants + rule engine
│   └── ConfigurationPredicate.scala # Boolean algebra over specs
│
├── service/                  # Domain services
│   ├── ConfigurationBuilder.scala # Resolve + validate + build
│   └── CatalogQueryService.scala  # Filter valid options for UI
│
├── validation/               # Validation logic
│   ├── ValidationError.scala # 11 error types with human-readable messages
│   └── ConfigValidator.scala # Two-layer validation pipeline
│
└── sample/                   # Reference implementation
    └── SampleCatalog.scala   # 5 categories, 5 materials, 7 finishes, 10 rules
```

## Core Components

### 📦 Value Objects
- **Opaque type IDs**: `CategoryId`, `MaterialId`, `FinishId`, `ConfigurationId` with smart constructors
- **Physical properties**: `PaperWeight`, `Quantity`, `Dimension`
- **Enumerations**: `ColorMode`, `PaperFamily`, `FinishType`, `FinishSide`

### 📋 Domain Model
- **`Material`** — Material family, weight, properties (coated, recyclable, etc.)
- **`Finish`** — Finish type, side application (single-sided, both sides)
- **`ProductCategory`** — Defines allowed materials/finishes and required specifications
- **`ProductSpecification`** — 7 variants (Size, Pages, Quantity, BindingType, Lamination, PrintingMethod, ColorMode)
- **`ProductConfiguration`** — Aggregate root combining category, material, finish, specs
- **`ProductCatalog`** — Container for all products, materials, finishes, and rules

### 🔧 Rules Engine
**Compatibility Rules** (sealed ADT with 5 variants):
- `MaterialFinishIncompatible` — Forbids specific material-finish pairs
- `MaterialRequiresFinish` — Material requires a specific finish type
- `FinishRequiresMaterialProperty` — Finish requires material to have a property
- `FinishMutuallyExclusive` — Certain finishes cannot be combined
- `SpecConstraint` — Enforce spec-level constraints (size ranges, page counts, etc.)

**Configuration Predicates** — Boolean algebra (And, Or, Not) for spec validation

### ✅ Validation Pipeline
Two-layer validation:
1. **Structural validation** — Ensure category/material/finish selections are valid
2. **Rule evaluation** — Check all compatibility rules against the configuration
3. **Error accumulation** — Collect all errors, return comprehensive feedback

### 📡 Services
- **`ConfigurationBuilder`** — Orchestrates resolution, validation, and construction
- **`CatalogQueryService`** — Filters valid options based on current state for UI guidance

## Status

### ✅ Phase 1: Domain Model (Complete)
- Core domain entities and value objects
- Sealed ADT for rules and predicates
- Two-layer validation with error accumulation
- Domain services for building & querying
- Sample data: 5 categories, 5 materials, 7 finishes, 10 compatibility rules
- **23 passing tests** covering valid configs, invalid configs, error accumulation, and catalog queries

### 🚧 Roadmap

**Phase 2: Pricing**
- `PriceComponent` model (base price, per-unit, surcharges, discounts)
- `PricingRule` ADT for declarative pricing logic
- `PricingEngine` for price computation and breakdown
- Quantity-based tiered pricing support

**Phase 3: Persistence**
- ZIO-based repository interfaces
- Adapters for JSON/database storage
- Catalog versioning and rule evolution

**Phase 4: UI (Scala.js + Laminar)**
- Cross-compile domain model to JavaScript
- Step-by-step configuration wizard
- Progressive disclosure using `CatalogQueryService`
- Real-time validation feedback
- Live price preview

**Phase 5: Production Readiness**
- ZIO HTTP API for server-side validation & pricing
- Admin interface for catalog/rule management
- Order submission workflow
- PDF proof generation
- Integration with print production systems

## Getting Started

### Prerequisites
- Scala 3.8.1
- JDK 11+
- sbt (Scala Build Tool)

### Build
```bash
sbt compile
```

### Test
```bash
sbt test
```

### Run Tests with Output
```bash
sbt "testOnly * -- -v"
```

## Testing

The project includes comprehensive test coverage:

```bash
sbt test
```

Key test suites:
- **ConfigurationBuilderSpec** — Valid/invalid configurations, error accumulation
- **CatalogQueryServiceSpec** — Material/finish/spec filtering
- **Validation tests** — Error messages and edge cases

## Sample Catalog

The included `SampleCatalog` provides a reference implementation:

**Categories:**
- Business Cards, Flyers, Brochures, Banners, Packaging

**Materials:**
- Uncoated, Gloss, Matte, Kraft, Recycled

**Finishes:**
- Matte, Gloss, Spot UV, Foil, Emboss, Aqueous, Varnish

**Rules:**
- 10 compatibility rules covering material-finish restrictions, property requirements, and spec constraints

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

