# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Test Commands

```bash
sbt compile          # Compile all sources
sbt test             # Run all tests
sbt "testOnly mpbuilder.domain.ConfigurationBuilderSpec"  # Run a single test suite
sbt "testOnly * -- -v"  # Verbose test output
```

- Scala 3.3.3, JDK 11+, sbt
- Dependencies: ZIO 2.1.16, ZIO Prelude 1.0.0-RC39, ZIO Test

## Architecture

This is a DDD (Domain-Driven Design) product configuration system for the printing industry. All domain logic is **pure** — uses `Validation[E, A]` (ZIO Prelude), never `ZIO` effects. This keeps the domain Scala.js-compatible.

### Package layout: `mpbuilder.domain`

- **`model/`** — Value objects (opaque type IDs with smart constructors), entities, enums. `ProductConfiguration` is the aggregate root.
- **`rules/`** — `CompatibilityRule` sealed ADT (12 rule variants), `SpecPredicate` and `ConfigurationPredicate` (boolean algebra with And/Or/Not). Rules are data, not code.
- **`validation/`** — `ConfigurationError` ADT with exhaustive `message` match. `RuleEvaluator` interprets rules. `ConfigurationValidator` runs two-layer validation: structural checks first, then rule evaluation.
- **`service/`** — `ConfigurationBuilder` resolves IDs from catalog and orchestrates validation. `CatalogQueryService` pre-filters compatible options for UI progressive disclosure.
- **`pricing/`** — `Money` opaque type (BigDecimal, never Double). `PricingRule` sealed enum (7 variants: base price, area price, finish/type/process/category surcharges, quantity tiers). `PriceCalculator` interprets rules purely. `PricingError` ADT with exhaustive `message` match. `PriceBreakdown` output with line items.
- **`sample/`** — `SampleCatalog` (7 categories, 9 materials, 14 finishes, 4 printing methods), `SampleRules` (24 rules), and `SamplePricelist` (pricing for all materials, key finishes, 4 quantity tiers). Used by tests.

### Key data flow

`ConfigurationRequest` → resolve IDs from `ProductCatalog` → `ConfigurationValidator.validate` (structural + rules) → `ProductConfiguration` → `PriceCalculator.calculate` (config + pricelist) → `PriceBreakdown`

### Validation approach

ZIO Prelude `Validation` accumulates all errors (not short-circuit). Structural validation (material allowed for category, required specs present) runs first via `flatMap`, then rule evaluation runs all rules and collects errors via `zipRight`.

### Adding new rule types

1. Add variant to `CompatibilityRule` enum
2. Add corresponding error to `ConfigurationError` enum (with `message` match arm)
3. Add evaluation arm in `RuleEvaluator.evaluate`
4. Handle in `CatalogQueryService.compatibleFinishes` (explicit match — no wildcard)
5. Add sample rules in `SampleRules` and tests

### Pricing approach

`PriceCalculator.calculate` is pure — returns `Validation[PricingError, PriceBreakdown]`. Steps: extract quantity → resolve material unit price (area-based first, then flat) → finish surcharges (ID-level overrides type-level) → process/category surcharges → sum subtotal → apply best quantity tier multiplier → round total to 2dp.

### Adding new pricing rule types

1. Add variant to `PricingRule` enum
2. Add corresponding error to `PricingError` enum (with `message` match arm) if needed
3. Add handling in `PriceCalculator.calculate`
4. Add sample rules in `SamplePricelist` and tests

### Important conventions

- All enum matches must be exhaustive — no wildcards in `CompatibilityRule`, `ConfigurationError`, or `PricingError` pattern matches
- `Money` is an opaque type over `BigDecimal` — never use `Double` for monetary values
- `ProductCategory.allowedPrintingMethodIds` empty set means "no restriction" (all methods allowed)
- `FinishCategory` is derived from `FinishType` via extension method `finishCategory`, not stored on `Finish`
- `CatalogQueryService.compatibleFinishes` takes `printingMethodId: Option[PrintingMethodId]` — `None` means "not yet selected, don't filter by method"
- Finish pricing precedence: `FinishSurcharge` (by ID) overrides `FinishTypeSurcharge` (by type) for the same finish
