# CLAUDE.md

This file provides guidance to AI assistants and developers when working with code in this repository.

## Prerequisites & Stack

- **JDK 25** (required by `io.getkyo:kyo-ui:1.0.0-RC7`, which targets JVM class file version 69.0)
- **Scala 3.9.0**, **Scala.js 1.22.0**, **Mill 1.1.3**
- **Kyo 1.0.0-RC7** (`kyo-ui`, `kyo-i18n`, `kyo-zio`) + **ZIO 2.1.26** + **ZIO Prelude 1.0.0-RC39**

## Build & Test Commands

This project uses **Mill** (`build.mill`).

```bash
# Compile everything (JVM + Scala.js)
./mill __.compile

# Run all tests (all backend bounded contexts + Kyo UI JVM tests)
./mill __.test

# Targeted test suites
./mill backend.storefront.jvm.test
./mill ui-core.jvm.test
./mill 'backend.pricing.01-c-core.jvm.test'

# Build Scala.js bundles
./mill ui.fastLinkJS             # Dev Storefront SPA → out/ui/fastLinkJS.dest/main.js
./mill ui.fullLinkJS             # Prod Storefront SPA → out/ui/fullLinkJS.dest/main.js
./mill ui-calculator.fastLinkJS  # Dev Calculator Widget → out/ui-calculator/fastLinkJS.dest/main.js
./mill ui-calculator.fullLinkJS  # Prod Calculator Widget → out/ui-calculator/fullLinkJS.dest/main.js
```

## Architecture Overview

The repository is split into a **cross-compiled (JVM + Scala.js) modular monolith** under `backend/` and a **Kyo UI frontend** under `modules/`:

### Backend Modular Monolith (`backend/`)

All backend modules cross-compile to both `.jvm` and `.js` so the static frontend can execute the exact same domain, pricing, rule, auth, cart, and checkout logic client-side via `backend.sample.js`, while keeping clean boundaries for future REST / GraphQL / Kyo server-push deployment:

- **`backend/shared`** — Top kernel: pure primitives, cross-context opaque IDs, measurements, `Money`, `Language`, `LocalizedString`, `EventBus`.
- **`backend/catalog/01-c-core`** — Product Catalog, Capabilities, Compatibility Rules, Imposition & Configuration.
- **`backend/pricing/01-c-core`** — Commercial Pricing, Customer Agreements, Promo Codes & Margin Analysis.
- **`backend/customers/01-c-core`** — Customer Accounts, Organizations, Tiers & Credit Terms.
- **`backend/artwork/01-c-core`** — Artwork Assets, Revisions & Prepress Preflight.
- **`backend/ordering/01-c-core`** — Shopping Cart, Order Placement & Order Lifecycle.
- **`backend/manufacturing/01-c-core`** — Shop-Floor Manufacturing Workflows, Capacity & Fulfilment.
- **`backend/identity/01-c-core`** — Identity, Authentication (Password + Passwordless OTP) & Sessions.
- **`backend/storefront`** — Storefront BFF / Application Orchestration Facade (`StorefrontFacade`). Depends only on public context APIs.
- **`backend/sample`** — Sample showcase data (15 products, 5 groups, demo B2B accounts, promo codes), in-memory adapters, ACL bridges, and `SampleShowcaseRuntime`.

### Frontend (`modules/`)

- **`modules/ui-core`** (`ui-core.jvm`, `ui-core.js`) — Cross-compiled Kyo UI library with **zero** `org.scalajs.dom` imports and **zero** dependency on `backend.sample`. Contains:
  - `mpbuilder.ui.port.StorefrontClient` — Transport-agnostic pure Kyo async port (plus `StorefrontClient.fromZio` adapter via `kyo-zio` for static/in-memory or embedded JVM usage; future REST/GraphQL/server-push transports simply implement `StorefrontClient`).
  - `mpbuilder.ui.i18n.Translations` — Reactive EN/CS Fluent (`.ftl`) bundles backed by `kyo-i18n`, kept in lockstep with `mpbuilder.shared.Language`.
  - `mpbuilder.ui.theme.AppTheme` — Typed `kyo.Stylesheet` + `Palette` with responsive breakpoints (`960px` tablet, `640px` mobile); injected at runtime via `UI.runStylesheet(...)` (no separate static `.css` files to maintain).
  - `mpbuilder.ui.configurator` — Shared reactive 2-column Product Configurator (`ConfiguratorController` + `ConfiguratorView`) parameterized by `ConfiguratorHost` (`Storefront` vs `Calculator`).
  - `mpbuilder.ui.storefront` — Complete responsive Storefront SPA: Catalog, Product Detail (live-quoted presets), Configurator, Cart Drawer (with promo codes), Checkout (Cart & Quick Order + SVG QR payment visual), Auth Modal (Passwordless OTP via Email/IČO/DIČ + Password login), and Account & Order History.
  - `mpbuilder.ui.calculator` — Embeddable calculator shell (`CalculatorView`) wrapping `ConfiguratorView` + Email Quote Request modal.
  - JVM UI test suite in `modules/ui-core/src/test/scala/mpbuilder/ui/StorefrontUiSpec.scala` using `UI.runRender` + `SampleShowcaseRuntime`.
- **`modules/ui`** (`ui`) — Static Scala.js Storefront SPA (`mpbuilder.ui.Main` extending `KyoApp`) wired to `backend.sample.js` via `StorefrontClient.fromZio`.
- **`modules/ui-calculator`** (`ui-calculator`) — Standalone embeddable Scala.js widget (`mpbuilder.calculator.CalculatorWidget`) exporting global `MPCalculator.mount(selector, config)` without a main initializer.

## Domain & UI Conventions

- **Bounded context isolation** — Every `backend/<context>/01-c-core` depends ONLY on `backend/shared` (zero cross-core dependencies).
- **Opaque types with smart constructors** — IDs and value objects use Scala 3 opaque types validated via ZIO Prelude `Validation[E, A]`. Use `.unsafe(...)` only in tests/sample fixtures.
- **Money** — `Money` is an opaque type over `BigDecimal` (`HALF_UP` rounding). Never use `Double` for monetary calculations.
- **i18n** — UI copy lives in `Translations.scala` (`kyo-i18n` Fluent bundles, EN & CS); domain entities use `LocalizedString`. Every UI change must keep EN & CS complete without missing-key markers (`‹` / `›`).
- **Kyo UI cross-platform purity** — Never import `org.scalajs.dom` or `backend.sample` inside `modules/ui-core`. Browser-only glue (clipboard, `localStorage`, `window.location.hash`, `mailto:`) is injected via callbacks from `modules/ui` and `modules/ui-calculator`.

## Key Paths

| Area | Path |
|---|---|
| Build definition | `build.mill` |
| Backend bounded contexts | `backend/{shared,catalog,pricing,customers,artwork,ordering,manufacturing,identity}/` |
| Storefront BFF facade | `backend/storefront/src/main/scala/mpbuilder/storefront/` |
| Sample showcase runtime | `backend/sample/src/main/scala/mpbuilder/sample/SampleShowcaseRuntime.scala` |
| StorefrontClient port | `modules/ui-core/src/main/scala/mpbuilder/ui/port/StorefrontClient.scala` |
| i18n (EN/CS Fluent) | `modules/ui-core/src/main/scala/mpbuilder/ui/i18n/Translations.scala` |
| Responsive Kyo Stylesheet | `modules/ui-core/src/main/scala/mpbuilder/ui/theme/AppTheme.scala` |
| Shared Configurator | `modules/ui-core/src/main/scala/mpbuilder/ui/configurator/` |
| Storefront Views | `modules/ui-core/src/main/scala/mpbuilder/ui/storefront/` |
| Calculator View | `modules/ui-core/src/main/scala/mpbuilder/ui/calculator/CalculatorView.scala` |
| JVM UI Tests | `modules/ui-core/src/test/scala/mpbuilder/ui/StorefrontUiSpec.scala` |
| Static SPA entrypoint | `modules/ui/src/main/scala/mpbuilder/ui/Main.scala` |
| Calculator JS export | `modules/ui-calculator/src/main/scala/mpbuilder/calculator/CalculatorWidget.scala` |

## Documentation Knowledge Base

Structured documentation lives in `docs/` with a master index at **[docs/INDEX.md](docs/INDEX.md)**.
