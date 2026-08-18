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
mill 'pricing.01-core.jvm.compile'  # Pricing public contract
mill 'customers.01-core.compile'    # Customers public contract
mill 'identity.01-core.compile'     # Identity public contract
mill 'order-intake.01-core.compile' # Order-intake public contract
mill app.compile                    # Composition root
mill ui.compile              # Full SPA (Scala.js)
mill ui-productbuilder.compile  # Shared product configurator
mill ui-calculator.compile   # Standalone embeddable calculator
mill ui-framework.compile    # UI framework only

# Test
mill commons.jvm.test                               # Shared kernel tests
mill domain.jvm.test                                # All domain tests
mill 'domain.jvm.test.testOnly *PriceCalculatorSpec' # Single suite (pattern)
mill 'catalog.02-infra.test'                        # Catalog contract tests
mill 'pricing.02-infra.test'                        # Pricing contract tests
mill 'customers.02-infra.test'                      # Customers contract tests
mill 'identity.02-infra.test'                       # Auth contract tests
mill 'order-intake.01-core.test'                    # Pure checkout and order policy tests
mill 'order-intake.02-infra.test'                   # Basket, checkout and order contract tests
mill 'manufacturing.02-infra.test'                  # Production contract tests, incl. the event translation

# Run the backend (Swagger UI at /docs, health at /health).
# Also serves the built SPA from ./dist (override with ASSET_ROOT), so the app and the API share
# an origin — the server-side basket needs that.
# STAFF_EMAILS provisions staff accounts: those addresses register with the operator role, which is
# what lets anyone approve production or confirm a payment. Registration never grants it by itself.
JWT_SECRET=dev-secret PORT=8080 STAFF_EMAILS=floor@shop.example mill app.run

# Refresh ./dist after a UI change
mill ui.fullLinkJS && cp out/ui/fullLinkJS.dest/main.js dist/ \
  && cp modules/ui/src/main/resources/*.css modules/ui/src/main/resources/index.html dist/

# Architecture guard — must pass before compile; CI runs it first
./scripts/check-module-deps.sh

# JS build
mill ui.test                 # SPA translation tests (Scala.js, runs on Node)
mill ui-productbuilder.test  # Offline basket tests (Scala.js, runs on Node)
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
the two shared kernels — `commons` and `product-kernel` — **only**; never another context's core,
never any infra. A `02-infra` may depend on its
own core plus other contexts' cores (that is where anti-corruption adapters live), never another
context's infra.

- **`product-kernel/`** — Cross-compiled shared kernel for the *product*: `ProductConfiguration`, `ProductCatalog`, `Material`, `Finish`, `PrintingMethod`, `ProductSpecifications`, `SpecValue`, `ManufacturingSpeed`, catalog's ids, and their JSON codecs. Owned by no context, depended on by catalog and pricing — you cannot price a product without knowing what a product is. Depends on `commons` only.
- **`commons/`** — Cross-compiled shared kernel: `Money`, `Currency`, `Price`, `Percentage`, `Language`, `LocalizedString`, `Dimension`, `Quantity`, `Timestamp`, `DomainError`, `Estimated[+A]`. Depends on nothing. Business concepts (`CategoryId`, `CustomerType`, …) do **not** belong here.
- **`catalog/01-core`** — Catalog's public contract *and* its model: `CatalogService`, `CatalogError`, `ConfigurationSnapshot`, request DTOs, plus `model/` (entities and catalog's five ids), `rules/`, `validation/`, `weight/`, `codec/` and `service/` (`ConfigurationBuilder`, `CatalogQueryService`). Cross-compiled, `commons`-only. The model is public rather than behind `impl` because pricing's engine, manufacturing's generator and the SPA all operate on `ProductConfiguration` directly; narrowing it belongs with the pricing extraction.
- **`catalog/sample`** — The bundled product catalog (`SampleCatalog`, `SampleRules`, `SampleShowcase`). Fixture data standing in for a store that does not exist yet — and, until it does, what the running SPA sells from. Cross-compiled.
- **`catalog/02-infra`** — Adapters. `LegacyCatalogService` delegates to `domain/` until the model is extracted (Phase 7); `Mapping` is the whole DTO↔domain anti-corruption layer.
- **`pricing/01-core` + `02-infra`** — `PricingService`: quotes, speed offers, discount codes. `01-core/engine` holds the calculation engine itself (`PriceCalculator`, `PricingRule`, `PriceBreakdown`, `PricingContext`, production costs, the customer pricing overlay) and the whole discount system — pricing **owns** discounts, so there is only one place a discount can be computed. Cross-compiled, because the offline calculator prices in the browser. It refers to a customer by its own `CustomerRef` and to a customer type by a plain label, never by the customers context's types. The engine's error is `CalculationError`; `PricingError` is the service contract's.
- **`pricing/sample`** — The bundled pricelists, discount codes and production costs.
- **`customers/01-core` + `02-infra`** — `CustomerService`: lookup, registration. Deliberately exposes **no** negotiated pricing — that is pricing's, resolved from a customer id.
- **`identity/01-core` + `02-infra`** — `AuthService`: registration, password login, JWT issue/verify/refresh, roles, OTP sign-in. Owns credentials only — the business profile is `customers`, linked by `customerId`. Stores are `Ref`-backed until Phase 3.
- **`order-intake/01-core` + `02-infra`** — `BasketService` (server-side basket), `CheckoutService` (delivery and payment options, whole-basket quote, discount codes, per-line speed offers) and `OrderService` (place, read, cancel, confirm payment, reject). `01-core/impl` holds the `Basket` and `Order` aggregates, the pure `BasketPolicy`, `CheckoutPolicy` and `OrderPolicy`, and the shared `Baskets` loader; `02-infra/impl/adapters` holds one adapter per foreign context — catalog, pricing, customers — and they are the only files that know those contexts exist. Stores are `Ref`-backed until Postgres lands.
- **`manufacturing/01-core` + `02-infra`** — `ProductionService`: takes work from order-intake's `OrderPlaced` events, plans it into workflow steps, and reports approval and dispatch back as its own events. Depends on **no** other context's core from `01-core` — it is told what to make and cannot reach back, which is what replaces `ManufacturingOrder` embedding `Order`. `02-infra/impl/adapters/OrderIntakeAdapter` is the only file that knows order-intake exists; `impl/legacy` plans steps with the legacy `WorkflowGenerator` until Phase 10.
- **`app/`** — Composition root. The **only** module that may depend on an `02-infra`: it wires the ZLayer graph, mounts every context's tapir routes on one zio-http server, serves the built SPA and the OpenAPI docs, and runs `EventRelay` — the only thing with both order-intake and manufacturing in scope. No business logic.
- **`domain/`** — Cross-compiled (JVM + JS). Pure functional core: no ZIO effects, only `Validation[E, A]` from ZIO Prelude. **Legacy and shrinking** — catalog left in Phase 7; what remains is the pricing engine, the manufacturing workflow model, customers, orders and the rest of `service/`. Now depends on `catalog/01-core`.
- **`ui-productbuilder/`** — The shared product configurator (form, pricing preview, validation, basket, e-mail order), package `mpbuilder.ui.productbuilder`. Depends on `domainJS` and `uiFramework`. Consumed by both apps below, so it must not reference anything in `mpbuilder.ui.*`. `basket/` holds the `BasketBackend` seam and the offline `LocalBasketBackend`.
- **`ui/`** — Scala.js + Laminar SPA. Depends on `domainJS`, `catalog/01-core` (JS), `uiFramework`, `ui-productbuilder`. `basket/HttpBasketBackend` puts the basket on the server; served from the same origin as the API by `app`.
- **`ui-calculator/`** — Standalone embeddable price calculator (`mpbuilder.calculator`). Linked without a main initializer; exports a global `MPCalculator` with `mount(selector, config)`. See [docs/standalone-calculator.md](docs/standalone-calculator.md).
- **`ui-framework/`** — Reusable Laminar components with no domain dependency (`mpbuilder.uikit` package).
- **`ui-showcase/`** — Demo for `ui-framework` components.

### The `BuilderEnvironment` seam

`ui-productbuilder` gets everything host-specific — pricing context, completion dates, Express
availability, the artwork step, **where the basket lives**, the basket's primary action, the order
e-mail recipient — from `BuilderEnvironment`, a singleton the host installs once before the first
render:

- `ui` → `FullAppEnvironment.environment` (from `Main.main`): simulated shop-floor queue, concrete
  completion timestamps, surge pricing, visual-editor artwork, checkout wizard.
- `ui-calculator` → `CalculatorEnvironment.environment(config)` (from `CalculatorWidget.mount`):
  `PricingContext.default`, **no** completion dates (indicative ranges + disclaimer instead),
  no artwork step, basket → e-mail order, and `LocalBasketBackend` — the widget must keep working
  with the network off.

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
| Configuration snapshot codecs | `modules/catalog/01-core/.../codec/ConfigurationCodecs.scala` |
| Architecture docs | `docs/architecture/` |
| Catalog rules / validation / weight | `modules/catalog/01-core/src/main/scala/mpbuilder/catalog/` |
| Bundled catalog data | `modules/catalog/sample/src/main/scala/mpbuilder/catalog/sample/` |
| Pricing rules/engine | `modules/pricing/01-core/src/main/scala/mpbuilder/pricing/engine/` |
| Product kernel | `modules/product-kernel/src/main/scala/mpbuilder/product/` |
| Domain services | `modules/domain/src/main/scala/mpbuilder/domain/service/` |
| Sample data | `modules/domain/src/main/scala/mpbuilder/domain/sample/` |
| Manufacturing domain | `modules/domain/src/main/scala/mpbuilder/domain/manufacturing/` |
| Manufacturing UI | `modules/ui/src/main/scala/mpbuilder/ui/manufacturing/` |
| Product configurator (shared) | `modules/ui-productbuilder/src/main/scala/mpbuilder/ui/productbuilder/` |
| Host seam | `modules/ui-productbuilder/.../BuilderEnvironment.scala`, `modules/ui/.../FullAppEnvironment.scala`, `modules/ui-calculator/.../CalculatorEnvironment.scala` |
| Standalone calculator | `modules/ui-calculator/` (sources, `css/`, `build-css.sh`) |
| Pricing tests | `modules/domain/src/test/scala/mpbuilder/domain/PriceCalculatorSpec.scala` (moves with Phase 11) |
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
