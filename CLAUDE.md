# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Test Commands

This project uses **Mill** as the build tool. sbt is also available in the project but not covered here.

### Mill

```bash
# Compile
mill commons.jvm.compile     # Shared kernel (JVM target)
mill domain.jvm.compile      # Domain (JVM target)
mill domain.js.compile       # Domain (Scala.js target)
mill 'catalog.01-core.jvm.compile'  # Catalog public contract
mill 'catalog.02-infra.compile'     # Catalog adapters
mill ui.compile              # Full SPA (Scala.js)
mill ui-productbuilder.compile  # Shared product configurator
mill ui-calculator.compile   # Standalone embeddable calculator
mill ui-framework.compile    # UI framework only

# Test
mill commons.jvm.test                               # Shared kernel tests
mill domain.jvm.test                                # All domain tests
mill 'domain.jvm.test.testOnly *PriceCalculatorSpec' # Single suite (pattern)
mill 'catalog.02-infra.test'                        # Catalog contract tests

# Architecture guard — must pass before compile; CI runs it first
./scripts/check-module-deps.sh

# JS build
mill ui.fastLinkJS           # Dev build → out/ui/fastLinkJS.dest/main.js
mill ui.fullLinkJS           # Production build → out/ui/fullLinkJS.dest/main.js
mill ui-calculator.fullLinkJS   # Widget build → out/ui-calculator/fullLinkJS.dest/main.js

./modules/ui-calculator/build-css.sh   # Regenerate the widget's bundled calculator.css
```

## Architecture Overview

**Scala 3.3.3** monorepo, mid-migration to feature-aligned bounded contexts — see
[docs/architecture/modular-architecture.md](docs/architecture/modular-architecture.md). New work goes
into a context (`<context>/01-core` + `<context>/02-infra`); `domain/` is the shrinking legacy module.

**The dependency rule, enforced by `scripts/check-module-deps.sh` in CI:** a `01-core` may depend on
`commons` **only** — never another context's core, never any infra. A `02-infra` may depend on its
own core plus other contexts' cores (that is where anti-corruption adapters live), never another
context's infra.

- **`commons/`** — Cross-compiled shared kernel: `Money`, `Currency`, `Price`, `Percentage`, `Language`, `LocalizedString`, `Dimension`, `Quantity`, `Timestamp`, `DomainError`, `Estimated[+A]`. Depends on nothing. Business concepts (`CategoryId`, `CustomerType`, …) do **not** belong here.
- **`catalog/01-core`** — Catalog's public contract: `CatalogService`, `CatalogError`, `ConfigurationSnapshot`, request DTOs. Cross-compiled, `commons`-only.
- **`catalog/02-infra`** — Adapters. `LegacyCatalogService` delegates to `domain/` until the model is extracted (Phase 7); `Mapping` is the whole DTO↔domain anti-corruption layer.
- **`domain/`** — Cross-compiled (JVM + JS). Pure functional core: no ZIO effects, only `Validation[E, A]` from ZIO Prelude. Contains pricing engine, compatibility rules, manufacturing workflow, and all services. **Legacy** — being split into contexts.
- **`ui-productbuilder/`** — The shared product configurator (form, pricing preview, validation, basket, e-mail order), package `mpbuilder.ui.productbuilder`. Depends on `domainJS` and `uiFramework`. Consumed by both apps below, so it must not reference anything in `mpbuilder.ui.*`.
- **`ui/`** — Scala.js + Laminar SPA. Depends on `domainJS`, `uiFramework`, `ui-productbuilder`.
- **`ui-calculator/`** — Standalone embeddable price calculator (`mpbuilder.calculator`). Linked without a main initializer; exports a global `MPCalculator` with `mount(selector, config)`. See [docs/standalone-calculator.md](docs/standalone-calculator.md).
- **`ui-framework/`** — Reusable Laminar components with no domain dependency (`mpbuilder.uikit` package).
- **`ui-showcase/`** — Demo for `ui-framework` components.

### The `BuilderEnvironment` seam

`ui-productbuilder` gets everything host-specific — pricing context, completion dates, Express
availability, the artwork step, the basket's primary action, the order e-mail recipient — from
`BuilderEnvironment`, a singleton the host installs once before the first render:

- `ui` → `FullAppEnvironment.environment` (from `Main.main`): simulated shop-floor queue, concrete
  completion timestamps, surge pricing, visual-editor artwork, checkout wizard.
- `ui-calculator` → `CalculatorEnvironment.environment(config)` (from `CalculatorWidget.mount`):
  `PricingContext.default`, **no** completion dates (indicative ranges + disclaimer instead),
  no artwork step, basket → e-mail order.

Never reach for `AppRouter`, `EditorBridge` or shop-floor data from inside `ui-productbuilder`;
add a field to `BuilderEnvironment` instead.

### Domain Layer Principles

**Rules as sealed ADTs** — Both `CompatibilityRule` (12 variants) and `PricingRule` (17 variants) are data, not functions. They are serializable and interpreted by a pure engine.

**Error accumulation** — All domain functions return `Validation[E, A]` which collects all errors (not short-circuiting). Never use `Either` for domain results.

**Opaque types with smart constructors** — Every ID (`CategoryId`, `MaterialId`, etc.) and value object uses Scala 3 opaque types. Smart constructors return `Validation`. Use `.unsafe(...)` only in tests/sample data.

**Pure domain, no effects** — The domain module must remain effect-free so it cross-compiles to Scala.js. ZIO effects only appear in infrastructure layers (not yet implemented).

### Pricing Calculation Flow

```
subtotal → discountedSubtotal (× quantity multiplier) → + setupFees → billable → max(billable, minimumOrderPrice)
```

`PriceBreakdown` has `setupFees: List[LineItem]` and `minimumApplied: Option[Money]`. Setup fees are NOT multiplied by the quantity discount multiplier.

### Manufacturing Workflow

`WorkflowGenerator.generate(config, stations, now)` derives `List[ProductionStep]` from a `ProductConfiguration`. Steps form a DAG enforced by `WorkflowEngine`. `SampleStations.allStations` has 8 stations covering the full lifecycle.

### Laminar Notes

When using `combineWith` on multiple signals/streams, tuples are flattened via `tuplez`. Always use explicit argument types in the handler: `(a: A, b: B) =>` not `case (a, b) =>`.

### Key Paths

| Area | Path |
|---|---|
| Shared kernel | `modules/commons/src/main/scala/mpbuilder/commons/` |
| Catalog contract / adapters | `modules/catalog/01-core/`, `modules/catalog/02-infra/` |
| Configuration snapshot codecs | `modules/domain/.../codec/ConfigurationCodecs.scala` |
| Architecture docs | `docs/architecture/` |
| Pricing rules/engine | `modules/domain/src/main/scala/mpbuilder/domain/pricing/` |
| Compatibility rules | `modules/domain/src/main/scala/mpbuilder/domain/rules/` |
| Domain services | `modules/domain/src/main/scala/mpbuilder/domain/service/` |
| Sample data | `modules/domain/src/main/scala/mpbuilder/domain/sample/` |
| Manufacturing domain | `modules/domain/src/main/scala/mpbuilder/domain/manufacturing/` |
| Manufacturing UI | `modules/ui/src/main/scala/mpbuilder/ui/manufacturing/` |
| Product configurator (shared) | `modules/ui-productbuilder/src/main/scala/mpbuilder/ui/productbuilder/` |
| Host seam | `modules/ui-productbuilder/.../BuilderEnvironment.scala`, `modules/ui/.../FullAppEnvironment.scala`, `modules/ui-calculator/.../CalculatorEnvironment.scala` |
| Standalone calculator | `modules/ui-calculator/` (sources, `css/`, `build-css.sh`) |
| Pricing tests | `modules/domain/src/test/scala/mpbuilder/domain/PriceCalculatorSpec.scala` |
| UI kit components | `modules/ui-framework/src/main/scala/mpbuilder/uikit/` |

### UI Framework Components (`mpbuilder.uikit`)

- **fields/**: `TextField`, `TextAreaField`, `SelectField` (+ `SelectOption`), `CheckboxField`, `RadioGroup` (+ `RadioOption`)
- **containers/**: `Tabs` (+ `TabDef`), `Stepper` (+ `StepDef`), `SplitTableView` (+ `ColumnDef[A]`, `RowAction[A]`)
- **feedback/**: `ValidationDisplay`
- **form/**: `FormState` (Mirror-based derivation), `FormRenderer`, `FormFieldState`, `FieldValidator`
- **util/**: `Visibility.when` / `Visibility.unless`

All field components take `Signal[String]` labels (not `Language`) to keep the framework domain-independent.

### i18n

`LocalizedString` is an opaque type. All user-facing error messages are localized to EN/CS. `Language` enum has `En` and `Cs` variants.

### Money

`Money` is an opaque type over `BigDecimal`. Always use `HALF_UP` rounding. Never use `Double` for monetary values.

## Documentation Knowledge Base

This project maintains a structured documentation system. **All documentation lives in `docs/`** with a master index at **[docs/INDEX.md](docs/INDEX.md)**.

| Resource | Purpose |
|----------|---------|
| [docs/INDEX.md](docs/INDEX.md) | Master table of contents — find any document by category |
| [docs/troubleshooting.md](docs/troubleshooting.md) | Known issues & solutions (build, domain, UI, agent sessions) |
| [docs/changelog/](docs/changelog/) | Per-session work logs (what was done, decisions, issues) |

## Post-Work Documentation

After completing any task, run the **post-work-docs** skill to update changelog, troubleshooting, specs, and the docs index. See [`.claude/skills/post-work-docs/SKILL.md`](.claude/skills/post-work-docs/SKILL.md) for the full procedure.
