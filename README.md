# Product Builder

> ⚠️ **Note**: This project was built with the assistance of an LLM (Large Language Model).

A domain-driven print-shop platform: customers configure and price print products, upload artwork
with automated preflight and place orders; staff accept, review and progress those orders and manage
the catalog, pricing, customers and store settings in a role-based backoffice.

**🌐 Live demo (runs fully in the browser, no backend):**

| App | URL |
|---|---|
| Storefront + Staff Backoffice (`#/admin`) | [david-lebl.github.io/product-builder](https://david-lebl.github.io/product-builder/) |
| Standalone Backoffice | [david-lebl.github.io/product-builder/admin/](https://david-lebl.github.io/product-builder/admin/) |
| Embeddable price calculator | [david-lebl.github.io/product-builder/calculator/](https://david-lebl.github.io/product-builder/calculator/) |

Orders placed in the storefront land immediately in Staff Order Intake: both run on one shared
in-memory runtime. Demo staff login: `admin@printshop.cz` / `staff1234`; demo customer:
`jan@printshoppro.cz` / `demo1234`.

## What It Does

**Storefront**
- **Product configurator** for 15 products in 5 groups (sheet print, large format, bound, specialty & packaging, promotional): materials, printing methods, ink configuration, finishes and specs, guided by a declarative compatibility rule engine with press-sheet imposition.
- **Live pricing** with a detailed breakdown, speed tiers (express / standard / economy) driven by shop-floor capacity, promo codes and **B2B dual pricing** (list vs. agreement price) for signed-in business customers.
- **Artwork upload** per product component with automated **prepress preflight** (resolution, bleed, trim size, colour space, page count, fonts, transparency), plus revisions after ordering.
- **Cart and checkout** with shop pickup or courier delivery; payment by QR bank transfer, card, or invoice on account (net terms for approved B2B accounts); password or passwordless sign-in (OTP by e-mail, IČO or DIČ).
- **My account**: order history, addresses, payment instructions, artwork status.

**Staff backoffice** (RBAC: Operator, Prepress, Manager, Admin)
- **Order intake**: queue, accept / hold / resume / cancel, manual stage progression (`Accepted → InProduction → ReadyForDispatch → Dispatched → Completed`), payment confirmation, line edits, internal notes, and an immutable audit timeline.
- **Artwork review**: approve, request changes or reject, upload corrected revisions, re-run preflight.
- **Catalog manager** for categories, presets, materials, finishes, printing methods, rules and showcases, with in-use protection and catalog versioning.
- **Pricing manager** for pricelists, B2B agreements, promo codes, cost sheets and a live margin simulator.
- **Customers** (approvals, tiers, credit terms, notes, audit trail), **staff directory**, and **store settings** (pickup locations, couriers, bank accounts).

**Embeddable calculator**: a standalone widget (`MPCalculator.mount(selector, config)`) that prices products on any website, with e-mail ordering.

All UI copy is available in **English and Czech**.

## Tech Stack

| Area | Technology |
|---|---|
| Language & build | Scala 3.9.0, Scala.js 1.22.0, Mill 1.1.3, JDK 25 |
| Domain & effects | Kyo 1.0.0-RC7 (`kyo-core`, `kyo-combinators`: `Async`, `Abort`, `AtomicRef`, `Clock`), error-accumulating `Validation`, opaque types with smart constructors |
| UI | Kyo UI 1.0.0-RC7 (`kyo-ui`, `kyo-i18n` Fluent bundles), cross-compiled to JVM (tests) and Scala.js |
| Persistence | PostgreSQL 17 via `kyo-sql` (native wire driver + pool, typed JSONB columns), schema-per-context migrations |
| HTTP API | `kyo-http` typed routes + server (generated OpenAPI / Swagger UI), kyo-http client (JVM) / `fetch` (browser), `kyo-schema` JSON |
| Testing | `kyo-test`, Kyo UI JVM render tests, `kyo-pod` throwaway PostgreSQL containers |
| CI/CD | GitHub Actions → GitHub Pages (static demo) |

## Architecture

A **cross-compiled modular monolith** (hexagonal / DDD): every bounded context compiles to both the
JVM and Scala.js, so the browser demo runs the exact same domain, pricing, rules and checkout logic as
the server.

```
backend/
├── shared/                      Kernel: opaque IDs, Money, LocalizedString, EventBus, BlobStorage, IdSequence
├── catalog|pricing|customers|identity|ordering|artwork|manufacturing/
│   ├── 01-c-core/               Domain + public API; depends ONLY on shared (JVM + JS)
│   └── 02-o-db/                 kyo-sql PostgreSQL repository adapters + migrations (JVM; not manufacturing yet)
├── storefront/                  Customer BFF facade
│   ├── 01-i-http-api/           Wire DTOs, schemas & URL layout of /api/v1/storefront (JVM + JS)
│   └── 02-i-http/               kyo-http routes
├── backoffice/                  Staff RBAC BFF facade (+ 01-i-http-api /api/v1/admin, 02-i-http)
├── api-kernel/                  Shared transport helpers: error envelope, status mapping, health; kyo-http helpers (JVM)
├── json/                        kyo-schema codecs (JSONB rows + HTTP wire)
├── platform/                    kyo-sql pool, migration runner, local blob storage (JVM)
├── sample/                      Showcase data, in-memory adapters, SampleShowcaseRuntime
└── app/                         Composition root: --storage=memory|postgres, seeder, HTTP server

modules/
├── ui-core/                     Storefront, configurator & calculator views (Kyo UI, JVM + JS)
├── ui-admin-core/               Backoffice views (Kyo UI, JVM + JS)
├── ui-http-client/              HTTP implementations of the facades (JVM + JS)
├── ui/                          Unified SPA: storefront + #/admin
├── ui-admin/                    Standalone backoffice SPA
└── ui-calculator/               Embeddable calculator widget
```

The UI talks to transport-agnostic ports (`StorefrontClient`, `BackofficeClient`) and boots in one of
two modes **with no view changes**:

- **Static mode** (default, GitHub Pages): the backend runs inside the browser on the in-memory sample runtime.
- **Network mode**: when the page sets `window.__API_BASE_URL__`, the same ports call the HTTP API backed by PostgreSQL (or in-memory) storage.

Key principles: **rules and pricing as data** (inspectable ADTs interpreted by engines), **error
accumulation** with `Validation`, **bounded-context isolation** enforced by the build, and **one
PostgreSQL schema per context**, each with its own migrations.

## Getting Started

**Prerequisites:** JDK 25 (e.g. `sdk env` with the bundled `.sdkmanrc`), Mill 1.1.3 (`./mill` wrapper
included), and Docker (or Podman) for PostgreSQL and the database tests.

### Static demo (no backend)

```bash
./mill ui.fastLinkJS
mkdir -p dist && cp modules/ui/src/main/resources/index.html out/ui/fastLinkJS.dest/main.js dist/
cd dist && python3 -m http.server 8000      # http://localhost:8000 (storefront), /#/admin (backoffice)
```

### API server

```bash
./mill backend.app.run --storage=memory     # http://localhost:8080/docs (Swagger UI), /openapi.json

docker compose up -d postgres
DATABASE_URL=postgresql://localhost:5432/printcraft ./mill backend.app.run --storage=postgres
```

On first boot the database is migrated and seeded with the showcase data. To point the SPA at the
server, see **[docs/running-the-platform.md](docs/running-the-platform.md)**, which also covers all
server options and demo logins.

### Tests

```bash
./mill __.test                               # everything (~140 tests; DB suites need Docker)
./mill backend.sample.jvm.test               # cross-context domain & facade tests
./mill ui-admin-core.jvm.test                # Kyo UI render tests (RBAC, i18n, storefront → intake E2E)
./mill 'backend.ordering.02-o-db.test'       # same contract against in-memory and PostgreSQL
./mill backend.app.test                      # live HTTP end-to-end, memory and PostgreSQL modes
```

## Documentation

- **[docs/INDEX.md](docs/INDEX.md)**: master index of all documentation (start here)
- **[PLAN.md](PLAN.md)**: goals and roadmap status; details in [docs_roadmap/platform_roadmap.md](docs_roadmap/platform_roadmap.md)
- **[docs/backend-persistence-and-http-api.md](docs/backend-persistence-and-http-api.md)**: persistence, HTTP API and blob storage design
- **[docs/running-the-platform.md](docs/running-the-platform.md)**: running the server and frontends
- **[guides/scala_3_modulith_boilerplate.md](guides/scala_3_modulith_boilerplate.md)**: the module and layer conventions
- **[CLAUDE.md](CLAUDE.md)**: build commands and coding conventions for contributors and AI assistants
- **[docs/troubleshooting.md](docs/troubleshooting.md)** and **[docs/changelog/](docs/changelog/)**: known issues and work history

## License

MIT, see [LICENSE](LICENSE).
