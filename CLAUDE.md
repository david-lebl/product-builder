# CLAUDE.md

This file provides guidance to AI assistants and developers when working with code in this repository.

## Prerequisites & Stack

- **JDK 25** (required by `io.getkyo:kyo-ui:1.0.0-RC7`, which targets JVM class file version 69.0)
- **Scala 3.9.0**, **Scala.js 1.22.0**, **Mill 1.1.3**
- **Kyo 1.0.0-RC7** (`kyo-ui`, `kyo-i18n`, `kyo-zio`) + **ZIO 2.1.26** + **ZIO Prelude 1.0.0-RC39**
- **Persistence**: Magnum 2.0.0 (`magnumzio`) + HikariCP + Flyway + PostgreSQL 17, jsoniter-scala codecs
- **Docker** must be running for the `02-o-db`, `platform` and `app` test suites (Testcontainers)

## Build & Test Commands

This project uses **Mill** (`build.mill`).

```bash
# Compile everything (JVM + Scala.js)
./mill __.compile

# Run all tests (all backend bounded contexts + Kyo UI JVM tests)
./mill __.test

# Targeted test suites
./mill backend.storefront.jvm.test
./mill backend.sample.jvm.test
./mill ui-core.jvm.test
./mill ui-admin-core.jvm.test
./mill 'backend.pricing.01-c-core.jvm.test'

# Persistence contract tests (in-memory vs PostgreSQL via Testcontainers) & facades-on-PostgreSQL test
./mill 'backend.ordering.02-o-db.test'
./mill backend.app.test
docker compose up -d postgres   # local PostgreSQL 17

# Build Scala.js bundles
./mill ui.fastLinkJS             # Dev Unified Storefront + #/admin SPA → out/ui/fastLinkJS.dest/main.js
./mill ui.fullLinkJS             # Prod Unified Storefront + #/admin SPA → out/ui/fullLinkJS.dest/main.js
./mill ui-admin.fastLinkJS       # Dev Standalone Backoffice SPA → out/ui-admin/fastLinkJS.dest/main.js
./mill ui-admin.fullLinkJS       # Prod Standalone Backoffice SPA → out/ui-admin/fullLinkJS.dest/main.js
./mill ui-calculator.fastLinkJS  # Dev Calculator Widget → out/ui-calculator/fastLinkJS.dest/main.js
./mill ui-calculator.fullLinkJS  # Prod Calculator Widget → out/ui-calculator/fullLinkJS.dest/main.js
```

## Architecture Overview

The repository is split into a **cross-compiled (JVM + Scala.js) modular monolith** under `backend/` and a **Kyo UI frontend** under `modules/`:

### Backend Modular Monolith (`backend/`)

All backend modules cross-compile to both `.jvm` and `.js` so the static frontend can execute the exact same domain, pricing, rule, auth, cart, checkout, and backoffice order-intake logic client-side via `backend.sample.js`, while keeping clean boundaries for future persistence and REST / GraphQL / Kyo server-push deployment:

- **`backend/shared`** — Top kernel: pure primitives, cross-context opaque IDs, measurements, `Money`, `Language`, `LocalizedString`, `EventBus`.
- **`backend/catalog/01-c-core`** — Product Catalog, Capabilities, Compatibility Rules, Imposition, Configuration & Admin CRUD with `EntityInUse` guards.
- **`backend/pricing/01-c-core`** — Commercial Pricing, Customer Agreements, Promo Codes, Production Cost Sheets & Margin Analysis.
- **`backend/customers/01-c-core`** — Customer Accounts, Organizations, B2B Approvals, Tiers, Credit Terms & Internal Notes.
- **`backend/artwork/01-c-core`** — Artwork Assets, Revisions & Prepress Preflight.
- **`backend/ordering/01-c-core`** — Shopping Cart, Order Placement, Manual Order Intake Lifecycle (`Accepted`, `OnHold`/`Resume`, `InProduction`, `ReadyForDispatch`, `Dispatched`, `Completed`, `Cancelled`), `OrderStatusEvent` Audit Trail & Delivery/Bank Settings.
- **`backend/manufacturing/01-c-core`** — Shop-Floor Manufacturing Workflows, Capacity & Fulfilment.
- **`backend/identity/01-c-core`** — Identity, Authentication (Password + Passwordless OTP), Sessions & Staff Directory Management.
- **`backend/storefront`** — Storefront BFF / Application Orchestration Facade (`StorefrontFacade`). Depends only on public context APIs.
- **`backend/backoffice`** — Role-based Staff Backoffice & Order Intake BFF (`BackofficeFacade`). Enforces `Operator` / `Prepress` / `Manager` / `Admin` RBAC across all contexts. Depends only on public context APIs.
- **`backend/json`** (JVM + JS) — jsoniter-scala codecs for all public domain types (JSONB rows); opaque types decode through smart constructors.
- **`backend/platform`** (JVM) — Magnum `Database` (HikariCP), per-context Flyway migrations (schema per context), `PostgresIdSequence`, `LocalFileBlobStorage`.
- **`backend/<context>/02-o-db`** (JVM) — PostgreSQL repository adapters + migrations for identity, customers, catalog, pricing, ordering, artwork (manufacturing stays in-memory).
- **`backend/app`** (JVM) — Composition root: per-context Postgres wiring in `mpbuilder.<ctx>.wiring`, `AppRuntime.postgresServices`, idempotent DB seeder.
- **`backend/sample`** — Sample showcase data (15 products, 5 groups, demo B2B accounts, 4 seeded demo orders, demo staff accounts, promo codes), in-memory adapters, ACL bridges, and `SampleShowcaseRuntime`.

### Frontend (`modules/`)

- **`modules/ui-core`** (`ui-core.jvm`, `ui-core.js`) — Cross-compiled Kyo UI library with **zero** `org.scalajs.dom` imports and **zero** dependency on `backend.sample`. Contains `StorefrontClient`, `Translations` (EN/CS Fluent), `AppTheme`, shared `ConfiguratorView`, `StorefrontApp`, and `CalculatorView`, plus JVM UI tests in `StorefrontUiSpec.scala`.
- **`modules/ui-admin-core`** (`ui-admin-core.jvm`, `ui-admin-core.js`) — Cross-compiled Kyo UI Backoffice library with **zero** `org.scalajs.dom` imports and **zero** dependency on `backend.sample`. Contains:
  - `mpbuilder.admin.port.BackofficeClient` — Pure Kyo async port (`BackofficeClient.fromZio` via `kyo-zio`).
  - `mpbuilder.admin.i18n.AdminTranslations` — EN/CS Fluent bundles composed with `Translations`.
  - `mpbuilder.admin.theme.AdminTheme` — Responsive sidebar/topbar/KPI/table/timeline `kyo.Stylesheet`.
  - `mpbuilder.admin.views` — Staff Login, Operations Dashboard, Order Intake Queue, Order Detail (manual stage controls, artwork review, staff notes, audit timeline), Catalog Manager, Pricing & Live Margin Simulator, Customer Manager & B2B Approvals, and Staff Directory & Store Delivery/Bank Settings.
  - JVM UI test suite in `modules/ui-admin-core/src/test/scala/mpbuilder/admin/BackofficeUiSpec.scala` covering RBAC, EN/CS i18n, and unified Storefront → Backoffice E2E order flow.
- **`modules/ui`** (`ui`) — Unified Static Scala.js Demo SPA (`mpbuilder.ui.Main`) hosting BOTH Storefront (`#/catalog`, `#/builder`, ...) and Staff Backoffice (`#/admin/...`) over ONE shared in-memory `SampleShowcaseRuntime` so Storefront orders land immediately in Staff Order Intake.
- **`modules/ui-admin`** (`ui-admin`) — Standalone deployable Scala.js Backoffice SPA (`mpbuilder.admin.AdminMain`, deployed to `_site/admin/`).
- **`modules/ui-calculator`** (`ui-calculator`) — Standalone embeddable Scala.js widget (`mpbuilder.calculator.CalculatorWidget`) exporting global `MPCalculator.mount(selector, config)` without a main initializer.

## Domain & UI Conventions

- **Bounded context isolation** — Every `backend/<context>/01-c-core` depends ONLY on `backend/shared` (zero cross-core dependencies).
- **Opaque types with smart constructors** — IDs and value objects use Scala 3 opaque types validated via ZIO Prelude `Validation[E, A]`. Use `.unsafe(...)` only in tests/sample fixtures.
- **Money** — `Money` is an opaque type over `BigDecimal` (`HALF_UP` rounding). Never use `Double` for monetary calculations.
- **i18n** — UI copy lives in `Translations.scala` & `AdminTranslations.scala` (`kyo-i18n` Fluent bundles, EN & CS); domain entities use `LocalizedString`. Every UI change must keep EN & CS complete without missing-key markers (`‹` / `›`).
- **Persistence** — Repository ports stay `private[context]`; Postgres adapters live in `mpbuilder.<ctx>.db`. Persist via indexed key columns + a `doc JSONB` aggregate, always derived with `mpbuilder.json.JsonSupport.config`. New tables go in a new `V<n>__*.sql` under `db/migration/<ctx>/` (never edit applied migrations). Every repository change must keep its in-memory/Postgres contract spec green.
- **Facade IDs & files** — Facades draw IDs from `IdSequence` and store artwork bytes via `BlobStorage` (`BlobIngest`), never in aggregates.
- **Kyo UI cross-platform purity** — Never import `org.scalajs.dom` or `backend.sample` inside `modules/ui-core` or `modules/ui-admin-core`. Browser-only glue (clipboard, `localStorage`, `window.location.hash`, `mailto:`) is injected via callbacks from `modules/ui`, `modules/ui-admin`, and `modules/ui-calculator`.

## Key Paths

| Area | Path |
|---|---|
| Build definition | `build.mill` |
| Backend bounded contexts | `backend/{shared,catalog,pricing,customers,artwork,ordering,manufacturing,identity}/` |
| Storefront BFF facade | `backend/storefront/src/main/scala/mpbuilder/storefront/` |
| Backoffice BFF facade | `backend/backoffice/src/main/scala/mpbuilder/backoffice/` |
| Sample showcase runtime | `backend/sample/src/main/scala/mpbuilder/sample/SampleShowcaseRuntime.scala` |
| StorefrontClient port | `modules/ui-core/src/main/scala/mpbuilder/ui/port/StorefrontClient.scala` |
| BackofficeClient port | `modules/ui-admin-core/src/main/scala/mpbuilder/admin/port/BackofficeClient.scala` |
| i18n (EN/CS Fluent) | `modules/ui-core/src/main/scala/mpbuilder/ui/i18n/Translations.scala`, `modules/ui-admin-core/src/main/scala/mpbuilder/admin/i18n/AdminTranslations.scala` |
| Responsive Kyo Stylesheets | `modules/ui-core/src/main/scala/mpbuilder/ui/theme/AppTheme.scala`, `modules/ui-admin-core/src/main/scala/mpbuilder/admin/theme/AdminTheme.scala` |
| Shared Configurator | `modules/ui-core/src/main/scala/mpbuilder/ui/configurator/` |
| Storefront Views | `modules/ui-core/src/main/scala/mpbuilder/ui/storefront/` |
| Backoffice Views | `modules/ui-admin-core/src/main/scala/mpbuilder/admin/views/` |
| Calculator View | `modules/ui-core/src/main/scala/mpbuilder/ui/calculator/CalculatorView.scala` |
| JVM UI Tests | `modules/ui-core/src/test/scala/mpbuilder/ui/StorefrontUiSpec.scala`, `modules/ui-admin-core/src/test/scala/mpbuilder/admin/BackofficeUiSpec.scala` |
| Unified SPA entrypoint | `modules/ui/src/main/scala/mpbuilder/ui/Main.scala` |
| Standalone Admin SPA | `modules/ui-admin/src/main/scala/mpbuilder/admin/AdminMain.scala` |
| JSON codecs | `backend/json/src/main/scala/mpbuilder/json/` |
| DB platform & migrations | `backend/platform/src/main/scala/mpbuilder/platform/`, `backend/*/02-o-db/src/main/resources/db/migration/` |
| Calculator JS export | `modules/ui-calculator/src/main/scala/mpbuilder/calculator/CalculatorWidget.scala` |

## Documentation Knowledge Base

Structured documentation lives in `docs/` with a master index at **[docs/INDEX.md](docs/INDEX.md)**.
