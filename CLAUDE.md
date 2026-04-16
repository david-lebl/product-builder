# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Test Commands

This project uses **Mill** as the build tool. sbt is also available in the project but not covered here.

### Mill

```bash
# Compile
mill domain.jvm.compile      # Domain (JVM target)
mill domain.js.compile       # Domain (Scala.js target)
mill ui.compile              # UI (Scala.js)
mill ui-framework.compile    # UI framework only

# Test
mill domain.jvm.test                                # All domain tests
mill 'domain.jvm.test.testOnly *PriceCalculatorSpec' # Single suite (pattern)

# JS build
mill ui.fastLinkJS           # Dev build → out/ui/fastLinkJS.dest/main.js
mill ui.fullLinkJS           # Production build → out/ui/fullLinkJS.dest/main.js
```

## Architecture Overview

**Scala 3.3.3** monorepo with 4 modules:

- **`domain/`** — Cross-compiled (JVM + JS). Pure functional core: no ZIO effects, only `Validation[E, A]` from ZIO Prelude. Contains pricing engine, compatibility rules, manufacturing workflow, and all services.
- **`ui/`** — Scala.js + Laminar SPA. Depends on `domainJS` and `uiFramework`.
- **`ui-framework/`** — Reusable Laminar components with no domain dependency (`mpbuilder.uikit` package).
- **`ui-showcase/`** — Demo for `ui-framework` components.

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
| Pricing rules/engine | `modules/domain/src/main/scala/mpbuilder/domain/pricing/` |
| Compatibility rules | `modules/domain/src/main/scala/mpbuilder/domain/rules/` |
| Domain services | `modules/domain/src/main/scala/mpbuilder/domain/service/` |
| Sample data | `modules/domain/src/main/scala/mpbuilder/domain/sample/` |
| Manufacturing domain | `modules/domain/src/main/scala/mpbuilder/domain/manufacturing/` |
| Manufacturing UI | `modules/ui/src/main/scala/mpbuilder/ui/manufacturing/` |
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
