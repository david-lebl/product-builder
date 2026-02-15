# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Test Commands

```bash
sbt compile          # Compile all sources
sbt test             # Run all tests
sbt "testOnly mpbuilder.domain.ConfigurationBuilderSpec"  # Run a single test suite
sbt "testOnly * -- -v"  # Verbose test output
```

- Scala 3.8.1, JDK 11+, sbt
- Dependencies: ZIO 2.1.16, ZIO Prelude 1.0.0-RC39, ZIO Test

## Architecture

This is a DDD (Domain-Driven Design) product configuration system for the printing industry. All domain logic is **pure** — uses `Validation[E, A]` (ZIO Prelude), never `ZIO` effects. This keeps the domain Scala.js-compatible.

### Package layout: `mpbuilder.domain`

- **`model/`** — Value objects (opaque type IDs with smart constructors), entities, enums. `ProductConfiguration` is the aggregate root.
- **`rules/`** — `CompatibilityRule` sealed ADT (13 rule variants), `SpecPredicate` and `ConfigurationPredicate` (boolean algebra with And/Or/Not). Rules are data, not code.
- **`validation/`** — `ConfigurationError` ADT with exhaustive `message` match. `RuleEvaluator` interprets rules. `ConfigurationValidator` runs two-layer validation: structural checks first, then rule evaluation.
- **`service/`** — `ConfigurationBuilder` resolves IDs from catalog and orchestrates validation. `CatalogQueryService` pre-filters compatible options for UI progressive disclosure.
- **`sample/`** — `SampleCatalog` (6 categories, 5 materials, 14 finishes, 4 printing methods) and `SampleRules` (22 rules). Used by tests.

### Key data flow

`ConfigurationRequest` → resolve IDs from `ProductCatalog` → `ConfigurationValidator.validate` (structural + rules) → `ProductConfiguration`

### Validation approach

ZIO Prelude `Validation` accumulates all errors (not short-circuit). Structural validation (material allowed for category, required specs present) runs first via `flatMap`, then rule evaluation runs all rules and collects errors via `zipRight`.

### Adding new rule types

1. Add variant to `CompatibilityRule` enum
2. Add corresponding error to `ConfigurationError` enum (with `message` match arm)
3. Add evaluation arm in `RuleEvaluator.evaluate`
4. Handle in `CatalogQueryService.compatibleFinishes` (explicit match — no wildcard)
5. Add sample rules in `SampleRules` and tests

### Important conventions

- All enum matches must be exhaustive — no wildcards in `CompatibilityRule` or `ConfigurationError` pattern matches
- `ProductCategory.allowedPrintingMethodIds` empty set means "no restriction" (all methods allowed)
- `FinishCategory` is derived from `FinishType` via extension method `finishCategory`, not stored on `Finish`
- `CatalogQueryService.compatibleFinishes` takes `printingMethodId: Option[PrintingMethodId]` — `None` means "not yet selected, don't filter by method"
