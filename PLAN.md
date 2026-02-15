# Product Builder — Goals & Roadmap

## Vision

A custom product builder for the printing industry that lets customers configure products from categories, materials, finishes, and specifications — with declarative compatibility rules ensuring only valid combinations are possible.

## Tech Stack

- **Scala 3.8.1** with DDD principles
- **ZIO 2.x** for effects (infrastructure layer)
- **ZIO Prelude** for validation with error accumulation
- **Scala.js + Laminar** for the UI (future)

## Key Design Decisions

1. **Rules are data, not code** — `CompatibilityRule` is a sealed ADT interpreted by a rule engine. New materials/finishes can be added without changing business logic.
2. **ZIO Prelude `Validation`** for error accumulation — all errors are collected, not short-circuited.
3. **No effects in domain layer** — all domain functions return `Validation[...]`, never `ZIO[...]`. This keeps the domain Scala.js-compatible.
4. **Specification pattern as ADT** — `ConfigurationPredicate` with And/Or/Not is inspectable and serializable, unlike `A => Boolean`.
5. **Progressive disclosure** — `CatalogQueryService` pre-filters valid options so the UI only shows compatible choices.

---

## Completed — Phase 1: Domain Model

The core domain layer is fully implemented and tested:

- **Value objects** — Opaque type IDs (`CategoryId`, `MaterialId`, `FinishId`, `ConfigurationId`) with smart constructors, `PaperWeight`, `Quantity`, `Dimension`
- **Domain model** — `Material` (family, weight, properties), `Finish` (type, side), `ProductCategory` (allowed materials/finishes, required specs), `ProductSpecifications` (7 spec variants), `ProductConfiguration` (aggregate root), `ProductCatalog`
- **Compatibility rules** — Sealed ADT with 5 rule variants (`MaterialFinishIncompatible`, `MaterialRequiresFinish`, `FinishRequiresMaterialProperty`, `FinishMutuallyExclusive`, `SpecConstraint`), spec predicates, configuration predicates with boolean algebra
- **Validation** — Two-layer pipeline (structural checks then rule evaluation), rich error ADT with 11 error types and human-readable messages
- **Services** — `ConfigurationBuilder` (resolve + validate + build), `CatalogQueryService` (filtered queries for UI guidance)
- **Sample data** — 5 categories (Business Cards, Flyers, Brochures, Banners, Packaging), 5 materials, 7 finishes, 10 compatibility rules
- **Tests** — 23 passing tests covering valid configs, error accumulation, and catalog queries

---

## Roadmap

### Phase 2: Pricing

- Define a `PriceComponent` model (base price, per-unit cost, finish surcharges, quantity discounts)
- Add `PricingRule` ADT — declarative pricing rules tied to materials, finishes, specs
- Implement `PricingEngine` that computes a price breakdown from a valid `ProductConfiguration`
- Add quantity-based tiered pricing (e.g. 100 units = $X, 500 units = $Y)
- Consider currency handling and rounding strategies

### Phase 3: Persistence

- Add ZIO-based repository interfaces in the domain layer
- Implement persistence adapters (e.g. JSON file store or database)
- Store and retrieve product catalogs, rulesets, and saved configurations
- Support catalog versioning for rule/data evolution

### Phase 4: UI with Scala.js + Laminar

- Cross-compile domain model to Scala.js
- Build a step-by-step product configuration wizard using Laminar
- Use `CatalogQueryService` for progressive disclosure — each step shows only valid options
- Real-time validation feedback as the user builds a configuration
- Price preview updating live as options are selected

### Phase 5: Production Readiness

- API layer (ZIO HTTP or similar) for server-side validation and pricing
- Admin interface for managing catalogs and rules without code changes
- Order submission workflow
- PDF proof generation
- Integration with print production systems
