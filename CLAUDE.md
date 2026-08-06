# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Test Commands

This project uses **Mill** as the build tool (the legacy sbt build was removed).

### Mill

```bash
# Compile
mill __.compile              # Everything
mill pricing.core.jvm.compile   # One context (JVM target)
mill pricing.core.js.compile    # One context (Scala.js target)
mill ui.compile              # UI (Scala.js)
mill ui-framework.compile    # UI framework only

# Test
mill __.test                                            # All tests
mill pricing.core.jvm.test                              # One context's tests
mill 'pricing.core.jvm.test.testOnly *PriceCalculatorSpec' # Single suite (pattern)

# JS build
mill ui.fastLinkJS           # Dev build → out/ui/fastLinkJS.dest/main.js
mill ui.fullLinkJS           # Production build → out/ui/fullLinkJS.dest/main.js
```

## Architecture Overview

**Scala 3.3.3** monorepo organized by bounded context (diamond architecture). Each context is a cross-compiled (JVM + JS) Mill module defined via the `ContextModule` trait in `build.mill`, with pure sources in one **flat package** `mpbuilder.<context>` at `modules/<context>/core/src`. Future persistence/IO adapters will live in JVM-only `modules/<context>/infra` siblings (package `mpbuilder.<context>.adapter`).

Context dependency DAG (compiler-enforced via `moduleDeps`, no cycles):

```
kernel ← catalog ← pricing ← { customer ← ordering, manufacturing } ← samples ← ui
```

- **`kernel/`** — Shared kernel: cross-context IDs (`CategoryId`, `OrderId`, `EmployeeId`, …), `Language`/`LocalizedString`, `Money`/`Currency`/`Price`, `Percentage`, `KernelCodecs`.
- **`catalog/`** — Product catalog & configuration: categories, components, materials, finishes, printing methods, specifications, presets, showcase, compatibility rules + validation, weight calculation, `CatalogQueryService`, `ConfigurationBuilder`, `CatalogCodecs`.
- **`pricing/`** — Pricelists, `PricingRule`, `PriceCalculator`, `PriceBreakdown`, customer pricing overlay + resolver, production cost, busy-period/queue types, `PresetPriceService`, `CatalogExport`, `PricingCodecs`.
- **`customer/`** — Customer & identity: `Customer`, `CustomerType`, `ContactInfo`, `Address`, OTP login, management services.
- **`ordering/`** — Basket, checkout, `Order`, discount codes, `BasketService`, `DiscountService`, `DiscountCodeService`.
- **`manufacturing/`** — Workflow aggregate (`ManufacturingOrder` wraps `Order`), stations, schedule/capacity, workflow engine/generator, queue scoring, utilisation, employee/machine management, analytics.
- **`samples/`** — All `Sample*` seed data (runtime module; also the test-fixture dependency via `testContextDeps`).
- **`ui/`** — Scala.js + Laminar SPA. Depends on the context `.core.js` targets and `ui-framework`.
- **`ui-framework/`** — Reusable Laminar components with no domain dependency (`mpbuilder.uikit` package).
- **`ui-showcase/`** — Demo for `ui-framework` components.

All context modules are pure functional: no ZIO effects, only `Validation[E, A]` from ZIO Prelude. JSON codecs live next to their types (`KernelCodecs` ⊂ `CatalogCodecs` ⊂ `PricingCodecs` via `export … given` re-exports — import the highest one you need).

### Domain Layer Principles

**Rules as sealed ADTs** — Both `CompatibilityRule` (12 variants) and `PricingRule` (17 variants) are data, not functions. They are serializable and interpreted by a pure engine.

**Error accumulation** — All domain functions return `Validation[E, A]` which collects all errors (not short-circuiting). Never use `Either` for domain results.

**Opaque types with smart constructors** — Every ID (`CategoryId`, `MaterialId`, etc.) and value object uses Scala 3 opaque types. Smart constructors return `Validation`. Use `.unsafe(...)` only in tests/sample data.

**Pure core, no effects** — Context `core` modules must remain effect-free so they cross-compile to Scala.js. ZIO effects only appear in future `infra` adapter modules (not yet implemented).

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
| Shared kernel (IDs, i18n, Money) | `modules/kernel/core/src/main/scala/mpbuilder/kernel/` |
| Catalog, rules, validation, weight | `modules/catalog/core/src/main/scala/mpbuilder/catalog/` |
| Pricing rules/engine | `modules/pricing/core/src/main/scala/mpbuilder/pricing/` |
| Customer & login | `modules/customer/core/src/main/scala/mpbuilder/customer/` |
| Basket, order, discounts | `modules/ordering/core/src/main/scala/mpbuilder/ordering/` |
| Manufacturing workflow & services | `modules/manufacturing/core/src/main/scala/mpbuilder/manufacturing/` |
| Sample data | `modules/samples/core/src/main/scala/mpbuilder/samples/` |
| Manufacturing UI | `modules/ui/src/main/scala/mpbuilder/ui/manufacturing/` |
| Pricing tests | `modules/pricing/core/src/test/scala/mpbuilder/pricing/PriceCalculatorSpec.scala` |
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
