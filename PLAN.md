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
- **Compatibility rules** — Sealed ADT with 13 rule variants, spec predicates, configuration predicates with boolean algebra (And/Or/Not)
- **Validation** — Two-layer pipeline (structural checks then rule evaluation), rich error ADT with 22 error types and human-readable messages
- **Services** — `ConfigurationBuilder` (resolve + validate + build), `CatalogQueryService` (filtered queries for UI guidance)
- **Sample data** — 6 categories (Business Cards, Flyers, Brochures, Banners, Packaging, Booklets), 5 materials, 14 finishes, 4 printing methods, 22 compatibility rules
- **Tests** — 44 passing tests covering valid configs, error accumulation, catalog queries, weight rules, finish dependencies, and printing process requirements

## Completed — Phase 2: Pricing

Declarative pricing layer following the same rules-as-data pattern:

- **Money** — Opaque type over `BigDecimal` (never `Double`), rounding to 2dp with `HALF_UP`
- **PricingRule** — Sealed enum with 7 variants: `MaterialBasePrice`, `MaterialAreaPrice`, `FinishSurcharge`, `FinishTypeSurcharge`, `PrintingProcessSurcharge`, `CategorySurcharge`, `QuantityTier`
- **PriceCalculator** — Pure interpreter: config + pricelist → `Validation[PricingError, PriceBreakdown]`. Supports area-based pricing (large-format), ID-over-type finish surcharge precedence, and quantity tier discounts
- **PriceBreakdown** — Detailed output with line items for material, finishes, process/category surcharges, subtotal, multiplier, and rounded total
- **Sample data** — Prices for all 5 materials (flat + area-based for vinyl), surcharges for key finishes, letterpress process surcharge, 4 quantity tiers (1.0×/0.90×/0.80×/0.70×)
- **Tests** — 11 pricing tests (55 total): valid breakdowns, area-based calculation, tier discounts, multiple finishes, precedence, graceful skip, and all error cases
- **Documentation** — See `docs/pricing.md` for detailed explanation with worked examples

---

## Roadmap

### Phase 3: UI with Scala.js + Laminar

- Cross-compile domain model to Scala.js
- Build a step-by-step product configuration wizard using Laminar
- Use `CatalogQueryService` for progressive disclosure — each step shows only valid options
- Real-time validation feedback as the user builds a configuration
- Price preview updating live as options are selected

### Phase 4: Persistence

- Add ZIO-based repository interfaces in the domain layer
- Implement persistence adapters (e.g. JSON file store or database)
- Store and retrieve product catalogs, rulesets, pricelists, and saved configurations
- Support catalog versioning for rule/data evolution

### Phase 5: Production Readiness

- API layer (ZIO HTTP or similar) for server-side validation and pricing
- Admin interface for managing catalogs, rules, and pricelists without code changes
- Order submission workflow
- PDF proof generation
- Integration with print production systems
