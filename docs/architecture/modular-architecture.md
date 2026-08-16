# Modular Architecture — Feature Modules, DDD, Hexagonal

> **Status:** proposed. This document defines the target architecture for decomposing the monolithic
> `domain` module into feature-aligned bounded contexts. The order-intake context is specified
> separately in [order-intake.md](order-intake.md).

## 1. Why

Today the repo is a **single 14.5k-LOC `domain` module** plus four Scala.js UI modules. There is
**no backend at all**: no HTTP, no database, no persistence beyond `localStorage`.
`mpbuilder.domain.sample.*` is the de-facto database, imported by 12 of 23 test specs. "Place Order"
in `modules/ui/src/main/scala/mpbuilder/ui/components/CheckoutView.scala` clears the basket, resets
the form and navigates away — **no order is ever created, stored, or transmitted**.

The domain itself is good (rules-as-data, `Validation` error accumulation, opaque IDs, effect-free so
it cross-compiles to Scala.js) but it has **no internal boundaries**:

- flat `mpbuilder.domain.{model, pricing, rules, validation, weight, manufacturing, service, codec, sample}`
- `service/` is a 24-file bag mixing five prospective bounded contexts
- `model ⇄ pricing` is a genuine **cycle** — `model/{order,basket,customer,discount}.scala` import
  `pricing`, while all of `pricing` imports `model`. It only compiles because it is one compile unit.
- `model/manufacturing.scala:370` — `ManufacturingOrder` **embeds the whole `Order`**, so
  manufacturing hard-depends on the order aggregate and, through it, on live catalog entities
- `model/ids.scala` holds 10 opaque IDs spanning every future module in one file
- two competing discount systems: the rich, rules-aware `DiscountCodeService` versus the hardcoded
  `DiscountService.lookupPercent` that the checkout UI actually honours
  (see [`ideas/from-scratch-specification.md`](../ideas/from-scratch-specification.md) §15)

**Goal.** Decompose into feature-aligned modules, each hexagonal with `01-core` (pure, ports) and
`02-infra` (adapters), deployed as **one JVM process**, with cross-module calls restricted to
published port interfaces — so any module can later be lifted into its own service without rewriting
its core.

## 2. Decisions

| Decision | Choice | Rationale |
|---|---|---|
| Deployment | **Modular monolith**, one JVM process | Boundaries strict enough to extract later, without paying operational cost now |
| Backend stack | **ZIO 2 + tapir + Postgres** | tapir gives OpenAPI *and* a typed Scala.js client for the existing Laminar frontend |
| Identity | **Own `identity` module**, password + JWT | Full control; existing OTP business sign-in kept as a second method |
| Sequencing | **Thin slice first**, extractions back-filled | Order intake ships early; extractions land behind ports that are already proven |
| Basket | **Server-side**, owned by order-intake | Cross-device basket, authoritative re-pricing, abandoned-cart data |
| `ui-calculator` | **Stays fully offline** | It embeds on third-party sites. Constrains `catalog`/`pricing` `01-core` to stay JVM+JS and effect-free |
| `build.sbt` | **Deleted**, along with `project/` and stale `target/` | Mill is already the only build in CI and docs; mirroring ~20 new sub-modules is pure cost |

## 3. Sub-module convention

Each feature gets three numbered sub-modules. The third one is what makes the monolith actually
decomposable — without it, every module ends up depending on every other module's core.

```
modules/<feature>/
  00-contract/   Published language. DTOs, domain events, and the port traits other
                 modules are allowed to call. Tiny, stable, dependency-light. JVM+JS.
  01-core/       Aggregates, value objects, pure domain services, driven-port traits.
                 Effect-free where it must cross-compile; ZIO only in the app-service layer.
  02-infra/      Adapters. Postgres repositories, tapir endpoints + server logic,
                 in-process implementations of *other* modules' ports, integrations. JVM only.
```

### The one dependency rule

> A module may depend on another module's `00-contract`, and on `00-kernel`.
> It may **never** depend on another module's `01-core` or `02-infra`.
> Wiring lives only in the top-level `app` composition root.

This is what makes "extract to a microservice" a mechanical operation later: swap the in-process
adapter behind a `00-contract` port for an HTTP client, and nothing in any `01-core` changes.

The rule is machine-checkable — a CI step asserting no `moduleDeps` entry references another
feature's `01-core`/`02-infra`. Add it while there are only three modules to police.

## 4. Module map

```
modules/
  00-kernel/                      shared kernel — JVM+JS, zero feature deps
  catalog/         00-contract 01-core 02-infra
  pricing/         00-contract 01-core 02-infra
  customers/       00-contract 01-core 02-infra
  identity/        00-contract 01-core 02-infra
  order-intake/    00-contract 01-core 02-infra
  manufacturing/   00-contract 01-core 02-infra
  artwork/         00-contract 01-core 02-infra      (later)
  notifications/   00-contract 01-core 02-infra      (later)
  app/                            composition root: ZIO layers, one tapir server, Flyway
  legacy-domain/                  temporary: today's `domain`, shrinking each phase
  ui-framework/ ui-productbuilder/ ui-calculator/ ui/ ui-showcase/   (unchanged)
```

### Dependency graph

```
                                00-kernel
                                    ▲
        ┌──────────┬────────────┬───┴────┬──────────────┬───────────────┐
    catalog     pricing     customers  identity    order-intake    manufacturing
      ▲            ▲            ▲          ▲             │                │
      │            │            │          │             │                │
      └── order-intake depends on all four 00-contracts ─┘                │
                          manufacturing consumes order-intake events ─────┘
                                        │
                                    app (wires every 02-infra)
```

Only `app` sees any `02-infra`. Every arrow points into a `00-contract`.

### 4.1 `00-kernel` — shared kernel (JVM + JS)

Everything genuinely shared, owned by nobody, depended on by everybody. Extracting this is what
**breaks the `model ⇄ pricing` cycle**.

| Type | Comes from |
|---|---|
| `Money`, `Currency`, `Price` | `pricing/Money.scala` — already has zero domain imports, just misplaced |
| `Percentage` | `pricing/CustomerPricing.scala:7` |
| `Language`, `LocalizedString` | `model/language.scala` |
| `Dimension`, `Quantity` | `model/specification.scala` |
| `ValidatedId` idiom | the `apply → Validation` / `unsafe` / `.value` pattern, copy-pasted ~16× |
| `DomainError` trait | `def message: String` + `def message(lang: Language)` — repeated in 9 error ADTs |
| `Timestamp` | replaces the raw `Long` epoch millis used throughout |

### 4.2 `catalog` — what can be built

**Owns:** `ProductCatalog`, `ProductCategory`, `Material`, `Finish`, `PrintingMethod`,
`ComponentTemplate`, `CategoryPreset`, `ShowcaseProduct`, `ProductSpecifications`,
`ProductConfiguration`, `CompatibilityRule`/`Ruleset`/predicates, `RuleEvaluator`,
`ConfigurationValidator`, `ConfigurationBuilder`, `CatalogQueryService`, `WeightCalculator`,
`DomainCodecs`.
**IDs:** `CategoryId`, `MaterialId`, `FinishId`, `PrintingMethodId`, `ConfigurationId`, `PresetId`.

**Publishes:** `CatalogPort`, `ConfigurationSnapshot`, `CatalogVersion`.
**`02-infra` later:** catalog persistence plus the admin/import-export API replacing
`modules/ui/catalog/ExportImportView.scala`'s client-side JSON.

> **Constraint:** `01-core` must stay JVM+JS and effect-free — `ui-calculator` runs it in the browser.

### 4.3 `pricing` — what it costs

**Owns:** `Pricelist`, `PricingRule` (~28 cases), `PriceCalculator`, `PriceBreakdown`,
`PricingContext`, `BusyPeriodMultiplier`, `QueueThreshold`, `CustomerPricing` +
`CustomerPricelistResolver`, `ProductionCost*`, `DiscountCode` + `DiscountCodeService`.
**ID:** `DiscountCodeId`.

Discount codes live here, not in order-intake: they are price-affecting rules, and putting them here
is what lets us **delete `DiscountService.lookupPercent`** and close the §15 dual-discount gap.
Order-intake only ever asks `PricingPort` for a quote, and the quote already has the discount applied.

**Publishes:** `PricingPort`, `PriceQuote`, `DiscountPort`, `CustomerPricingProfile`.

`PricingContext` carries queue utilisation, so pricing is manufacturing-aware. Keep the direction
one-way: pricing's contract exposes `PricingContext` as plain data; `manufacturing/02-infra` supplies
it — today's `UtilisationCalculator.buildPricingContext` becomes exactly that adapter.

> Same JVM+JS, effect-free constraint as catalog, for the same calculator reason.

### 4.4 `customers` — who we sell to

**Owns:** `Customer`, `CompanyInfo`, `CustomerNote`, `CustomerStatus`, `CustomerTier`,
`CustomerManagementService`. **ID:** `CustomerId`.

`Customer` currently embeds `pricing.CustomerPricing`. Pragmatic fix: `CustomerPricing` moves to
`pricing/00-contract` and `customers/01-core` depends on that contract — legal under the dependency
rule, near-zero churn. (A purer alternative — `customers` stores only a `PricingProfileId` and
pricing owns the profile store — remains available if the coupling ever bites.)

### 4.5 `identity` — who is signed in *(mostly greenfield)*

**Owns:** `User` (credentials), password hashing, JWT issue/verify/refresh, `Role`, `AuthSession`,
and the existing OTP flow (`OtpRequest`, `OtpToken`, `LoginSession`, `LoginService` — identify by
IČO / DIČ / e-mail, 5-minute OTP, 24-hour session), reframed as one of several authentication methods.

> **Critical separation:** `User` = credentials + roles (identity). `Customer` = business profile,
> company data, pricing, CRM notes (customers). Linked by `User.customerId: Option[CustomerId]`.
> Today there is no `User` type at all, and `CheckoutInfo.loginPassword` is a vestigial field that
> nothing consumes — it is deleted.

**Roles:** `Customer`, `StaffOperator`, `StaffManager`, `Admin`. `Employee` (manufacturing) gains a
`userId` so staff can actually sign in — today `Employee` has station capabilities but no login.

**Publishes:** `IdentityPort` (`verifyToken`, `principalOf`), `Principal`, `Role`, `UserId`.

### 4.6 `order-intake`

Basket, checkout, the `Order` aggregate and its lifecycle, order numbers, order history.
Specified in full in **[order-intake.md](order-intake.md)**.

### 4.7 `manufacturing` — how it gets made

**Owns:** everything in `model/manufacturing.scala` (workflows, steps, stations, `Employee`,
`Machine`, `ArtworkCheck`, `FulfilmentChecklist`), the `manufacturing/` config package
(`ShopSchedule`, `WorkingHours`, `StationTimeEstimate`, `StationUtilisation`, `TierRestriction`,
`CategoryTierConfig`), and the services `WorkflowGenerator`, `WorkflowEngine`, `QueueScorer`,
`CompletionEstimator`, `UtilisationCalculator`, `TierRestrictionValidator`, `AnalyticsService`,
`Employee`/`MachineManagementService`.

**The key change:** `ManufacturingOrder` stops embedding `Order`. It holds `orderId: OrderId` plus a
`ProductionOrderSnapshot` built from the `OrderPlaced` event (customer display name, line items with
their configuration snapshots, deadline, priority). Approval, payment and fulfilment status stay
here — that is genuinely manufacturing's lifecycle, distinct from the order's commercial lifecycle.

### 4.8 `artwork`, `notifications` *(later)*

- **`artwork`** — visual-editor sessions, canvas elements, uploaded assets; today entirely
  client-side in `modules/ui/visualeditor/` + `modules/ui/persistence/` (localStorage). Order lines
  reference `ArtworkId`; order-intake never sees canvas data.
- **`notifications`** — order confirmation, OTP delivery, status e-mails; replaces the `mailto:`
  approach in `EmailOrderModal.scala`.

## 5. Build wiring

Matching the existing `build.mill` style — explicit `Task.Sources` overrides, backticked names,
shared version vals. New dependency vals:

```scala
val zioVersion    = "2.1.16"
val tapirVersion  = "1.11.13"
val quillVersion  = "4.8.6"       // quill-jdbc-zio
val flywayVersion = "11.3.0"

val serverMvnDeps = Seq(
  mvn"com.softwaremill.sttp.tapir::tapir-zio-http-server::${tapirVersion}",
  mvn"com.softwaremill.sttp.tapir::tapir-json-zio::${tapirVersion}",
  mvn"com.softwaremill.sttp.tapir::tapir-swagger-ui-bundle::${tapirVersion}",
  mvn"io.getquill::quill-jdbc-zio::${quillVersion}",
  mvn"org.postgresql:postgresql:42.7.5",
  mvn"org.flywaydb:flyway-core:${flywayVersion}",
  mvn"org.flywaydb:flyway-database-postgresql:${flywayVersion}",
  mvn"com.github.jwt-scala::jwt-zio-json::10.0.1",
  mvn"org.bouncycastle:bcprov-jdk18on:1.80"
)
```

Define three reusable templates (`CrossSubModule`, `CoreSubModule`, `InfraSubModule`) rather than
copy-pasting ~20 near-identical blocks. One object per feature, nesting its sub-modules:

```scala
object `order-intake` extends Module {
  private val base = rootDir / "modules" / "order-intake"

  object `00-contract` extends Module {
    object jvm extends ScalaModule {
      def scalaVersion  = scala3Version
      def scalacOptions = commonScalacOptions
      def sources       = Task.Sources(base / "00-contract" / "src" / "main" / "scala")
      def moduleDeps    = Seq(`00-kernel`.jvm, catalog.`00-contract`.jvm,
                              pricing.`00-contract`.jvm, customers.`00-contract`.jvm,
                              identity.`00-contract`.jvm)
      def mvnDeps       = Seq(mvn"dev.zio::zio-json::0.7.3",
                              mvn"com.softwaremill.sttp.tapir::tapir-core::${tapirVersion}")
    }
    object js extends ScalaJSModule { /* same sources, .js deps — for the Laminar client */ }
  }

  object `01-core` extends ScalaModule {
    def scalaVersion  = scala3Version
    def scalacOptions = commonScalacOptions
    def sources       = Task.Sources(base / "01-core" / "src" / "main" / "scala")
    def moduleDeps    = Seq(`00-kernel`.jvm, `order-intake`.`00-contract`.jvm)
    def mvnDeps       = Seq(mvn"dev.zio::zio::${zioVersion}", mvn"dev.zio::zio-prelude::1.0.0-RC39")
    object test extends ScalaTests { /* zio-test, as domain.jvm.test does today */ }
  }

  object `02-infra` extends ScalaModule {
    def scalaVersion  = scala3Version
    def scalacOptions = commonScalacOptions
    def sources       = Task.Sources(base / "02-infra" / "src" / "main" / "scala")
    def resources     = Task.Sources(base / "02-infra" / "src" / "main" / "resources")  // db/migration
    def moduleDeps    = Seq(`01-core`)
    def mvnDeps       = serverMvnDeps
    object test extends ScalaTests { /* + testcontainers-backed Postgres */ }
  }
}
```

`app` depends on every `02-infra`, has a `ZIOAppDefault` main, one `ZLayer` graph, and one zio-http
server serving tapir routes + Swagger UI + the built SPA assets.

## 6. Implementation order

### Track A — foundations (first; unblocks everything)

**Phase 0 — `00-kernel`.** Extract `Money` / `Currency` / `Price` / `Percentage` / `Language` /
`LocalizedString` / `Dimension` / `Quantity`; add `ValidatedId` and `DomainError`; split
`model/ids.scala` per owner. **This breaks the `model ⇄ pricing` cycle.**
*Verify:* `mill __.compile && mill __.test` — all 23 specs green; `mill ui.fastLinkJS` still links.

**Phase 1 — `legacy-domain` + port façades.** Rename `domain` → `legacy-domain` (same sources, still
cross-compiled). Create `catalog/00-contract`, `pricing/00-contract`, `customers/00-contract` with
the port traits and DTOs, plus `02-infra` implementations that **delegate straight into
`legacy-domain`**. Nothing else moves yet. Add the CI dependency-rule check.
*Verify:* everything compiles; port-contract tests exercise the façades against `SampleCatalog` /
`SamplePricelist`.

**Phase 2 — `app` skeleton + `identity`.** Stand up `app` (ZIOAppDefault, zio-http, Flyway, Postgres
via docker-compose, Swagger UI), then build `identity` end to end: `User`, Argon2 hashing, JWT
issue/verify/refresh, roles, the auth endpoints; port today's `LoginService` OTP flow in as a second
method.
*Verify:* register → login → `/auth/me` against a real Postgres (testcontainers); the SPA's
`LoginWidget` signs in for real.

### Track B — order intake (the goal)

**Phase 3 — basket, server-side.** *Verify:* basket survives a page reload and a device switch;
`ui-calculator` still works with the network disabled.
**Phase 4 — checkout & quoting.** Deletes `DiscountService.lookupPercent`.
*Verify:* an expired / exhausted / min-order code from `SampleDiscountCodes` is correctly refused.
**Phase 5 — the `Order` aggregate.** *Verify:* placing an order in the browser writes `orders`,
`order_lines` and an `OrderPlaced` outbox row; the customer portal shows it.
**Phase 6 — events → manufacturing.** `ManufacturingOrder` stops embedding `Order`.
*Verify:* a placed order appears in the manufacturing approval queue with correct workflows;
approving it moves the order to `InProduction` in the customer portal.

Full detail for phases 3–6 is in [order-intake.md](order-intake.md) §8.

### Track C — back-fill extractions (behind ports already in use)

| Phase | Work |
|---|---|
| **7 — `catalog`** | Move catalog model + rules + validation + weight + codecs out of `legacy-domain`; keep JVM+JS. Split `SampleCatalog`/`SampleRules` into `catalog/testkit`. |
| **8 — `pricing`** | Same for pricing + discount codes + production cost. |
| **9 — `customers`** | Customer entity + CRM; `CustomerPricing` lands in `pricing/00-contract`. |
| **10 — `manufacturing`** | Split `model/manufacturing.scala` out of `model/`; move the six `manufacturing/` config files and the eight manufacturing services; persist workflows. |
| **11 — remainder** | `artwork`, `notifications`, analytics read-models. `legacy-domain` is now empty — delete it. |

Each of these is mechanical, because the port contracts and their tests already exist and don't change.

## 7. Risks and handling

| Risk | Handling |
|---|---|
| **`model ⇄ pricing` cycle** | Phase 0 kernel extraction. `Money`/`Language` move out, `Basket → PriceBreakdown` becomes a legal one-way dependency, `CustomerPricing` moves to `pricing/00-contract`. |
| **`ManufacturingOrder` embeds `Order`** | Phase 6 replaces it with `orderId` + `ProductionOrderSnapshot` built from the event. `orderItemIndex` → `OrderLineId`. |
| **Two discount systems** | Discounts live only in `pricing`; order-intake only ever reads a quote. `DiscountService.lookupPercent` deleted in Phase 4. |
| **`sample/` is the database, and 12 of 23 specs import it** | Untouched until Phase 7, by which time everything real reads Postgres. Then split per feature into `<feature>/testkit`, one module at a time, fixing that feature's specs alongside it. |
| **Scala.js constraint** | `00-kernel`, all `00-contract`s, and `catalog`/`pricing` `01-core` stay cross-compiled and effect-free (the `ui-calculator` offline guarantee). `order-intake`/`identity`/`manufacturing` `01-core` are JVM-only; their contracts are still JS so the UI shares DTOs. Every phase's verification includes `mill ui-calculator.fullLinkJS`. |
| **`Order` has zero tests today** | Phase 5 is the first thing with a real state machine — property-test the transition table, and table-test every `OrderIntakeError`'s EN/CS message following the existing `LocalizationSpec` pattern. |
| **Surge pricing makes quotes go stale** | Explicit `PriceChanged` error plus confirm-and-retry, rather than silently repricing or silently honouring a stale quote. |
| **~20 new Mill modules** | Three shared templates instead of copy-paste; `build.sbt` deleted so there is only one build to maintain. |
| **Modular monolith drifting back into a ball of mud** | The dependency rule is machine-checkable — a CI step asserting no `moduleDeps` references another feature's `01-core`/`02-infra`, added in Phase 1. |

## 8. Verification baseline

These gates must stay green at **every** phase boundary:

```bash
mill __.compile
mill __.test                     # 23 specs / ~600 tests today — must never regress
mill ui.fullLinkJS               # the SPA still links
mill ui-calculator.fullLinkJS    # the offline widget still links
```

From Phase 2 onward, additionally:

```bash
docker compose up -d postgres
mill app.run                          # Swagger UI at /docs, SPA at /
mill 'order-intake.02-infra.test'     # testcontainers-backed repository + endpoint tests
```

**End-to-end smoke, from Phase 5:** register → configure a product → add to basket → reload the page
(basket survives) → apply a discount code → place the order → see it in the customer portal → see it
in the manufacturing approval queue.