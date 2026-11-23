# Backend Persistence, HTTP API & Artwork Blob Storage

> Phases 3, 4 and 5.1 of [docs_roadmap/platform_roadmap.md](../docs_roadmap/platform_roadmap.md):
> PostgreSQL persistence (Magnum + Flyway), the Tapir HTTP API with frontend network wiring,
> and the `BlobStorage` port for artwork files.

## 1. Two runtime modes, zero UI changes

```
              Kyo UI (ui / ui-admin / ui-calculator)
                         │  StorefrontClient / BackofficeClient (unchanged ports)
          ┌──────────────┴───────────────┐
 Mode A: static (default)        Mode B: window.__API_BASE_URL__ set
 fromZio(SampleShowcaseRuntime)  fromZio(HttpStorefrontFacade / HttpBackofficeFacade)
                                         │  Tapir contracts (<bff>/01-i-tapir-api) over sttp4 Fetch
                                         ▼
                                 <bff>/02-i-http routes, mounted by backend/app (ZIO HTTP, Swagger UI at /docs)
                                         │
                                 backend/app   --storage=memory | --storage=postgres
                                         │
                     StorefrontFacade & BackofficeFacade (unchanged)
                                         │
                    context services ── *Repository ports (private[ctx])
                          ├─ in-memory adapters (backend/sample)
                          └─ PostgreSQL adapters (backend/<ctx>/02-o-db, Magnum)
```

The HTTP clients **implement the existing ZIO facade traits**, so the existing
`StorefrontClient.fromZio` / `BackofficeClient.fromZio` lifts are reused verbatim and the
JVM test-suite drives the real server through the very same client the browser uses.

## 2. Module map

| Module | Platform | Purpose |
|---|---|---|
| `backend/json` | JVM + JS | jsoniter-scala codecs for every public domain type (shared by JSONB rows and the HTTP wire). Opaque types decode through their validating smart constructors. |
| `backend/platform` | JVM | `Database` (HikariCP + Magnum `TransactorZIO`), per-context Flyway `Migrations`, `PostgresIdSequence`, `LocalFileBlobStorage`. Test fixture `PostgresTestSupport` (Testcontainers). |
| `backend/<ctx>/02-o-db` | JVM | PostgreSQL repositories for `identity`, `customers`, `catalog`, `pricing`, `ordering`, `artwork` + their migrations (`db/migration/<ctx>/V*.sql`). |
| `backend/api-kernel` | JVM + JS | Transport kernel shared by both BFF contracts (`mpbuilder.api`): bearer auth inputs, validated opaque path codecs, typed error envelope + status mapping, OpenAPI schemas, shared collection codecs, `SystemEndpoints` (health, blobs). |
| `backend/storefront/01-i-tapir-api` | JVM + JS | `mpbuilder.storefront.http.api`: `StorefrontEndpoints`, request DTOs, codecs. Depends only on the kernel + the storefront facade models. |
| `backend/storefront/02-i-http` | JVM | `mpbuilder.storefront.http.StorefrontRoutes` — server logic binding the contracts to `StorefrontFacade`. |
| `backend/backoffice/01-i-tapir-api` | JVM + JS | `mpbuilder.backoffice.http.api`: `BackofficeEndpoints`, request DTOs, codecs. |
| `backend/backoffice/02-i-http` | JVM | `mpbuilder.backoffice.http.BackofficeRoutes` — server logic binding the contracts to `BackofficeFacade`. |
| `backend/app` | JVM | Composition root: config, per-context Postgres wiring (`mpbuilder.<ctx>.wiring`), DB seeder, HTTP server assembly (`mpbuilder.app.http`: mounts both BFF routes + system routes, Swagger, CORS), `Main`. |
| `modules/ui-http-client` | JVM + JS | `HttpStorefrontFacade`, `HttpBackofficeFacade`, `HttpClients` factory. |

Manufacturing stays in-memory in both modes (deferred to its own chapter, per the roadmap).

## 3. Running

> Step-by-step instructions: [running-the-platform.md](running-the-platform.md).

```bash
# Mode B with in-memory storage (no database needed)
./mill backend.app.run --storage=memory --port=8080

# Mode B with PostgreSQL
docker compose up -d postgres
DATABASE_URL=jdbc:postgresql://localhost:5432/printcraft ./mill backend.app.run --storage=postgres

# Point a static SPA at the API: set this before loading main.js
<script>window.__API_BASE_URL__ = "http://localhost:8080";</script>
# …or for the calculator widget: MPCalculator.mount("#calc", { apiBaseUrl: "http://localhost:8080" })
```

| Setting | Flag | Env | Default |
|---|---|---|---|
| Storage | `--storage=memory\|postgres` | `STORAGE` | `memory` |
| Bind | `--host`, `--port` | `HOST`, `PORT` | `0.0.0.0`, `8080` |
| CORS origins | `--cors=a,b` | `CORS_ORIGINS` | `*` |
| Blob directory | `--blob-dir` | `BLOB_DIR` | `./data/blobs` |
| Blob URL prefix | `--blob-public-base-url` | `BLOB_PUBLIC_BASE_URL` | `/api/v1/blobs` |
| Skip seeding | `--no-seed` | — | seeding on |
| Database | — | `DATABASE_URL`, `DATABASE_USER`, `DATABASE_PASSWORD`, `DATABASE_POOL_SIZE` | user/pass `printcraft` |

Endpoints: `GET /healthz`, `GET /readyz` (checks the DB), `GET /docs` (Swagger UI),
`/api/v1/storefront/**` (31 endpoints), `/api/v1/admin/**` (69 endpoints, bearer token),
`GET /api/v1/blobs/<key>`.

Demo logins are the same as the static demo (`admin@printshop.cz` / `staff1234`,
`jan@printshoppro.cz` / `demo1234`). In server mode OTP codes are written to the server log
(`[DEV OTP DELIVERY]`) until transactional e-mail exists.

## 4. Persistence design

* **Schema per bounded context.** Each context owns a PostgreSQL schema (`identity`, `customers`,
  `catalog`, `pricing`, `ordering`, `artwork`, plus `platform`) with its **own Flyway history
  table**, so contexts migrate independently — modular-monolith isolation at the data layer.
* **Relational key columns + JSONB aggregate.** Lookup/filter columns (e-mail, normalized IČO/DIČ,
  status, tier, customer id, placed_at, promo code …) are real indexed columns; the full aggregate is a
  `doc JSONB` column decoded with the shared codecs. Nested ADTs (rules, price entries, product
  snapshots, preflight reports) evolve without migrations; new fields with defaults decode from old rows.
* **Normalized, append-only order audit trail.** `ordering.order_status_events` is the source of truth
  for `Order.statusHistory`; `save` appends only unseen events (`ON CONFLICT DO NOTHING`).
* **Catalog versioning.** `catalog.documents` holds the active catalog/ruleset;
  `catalog.document_history` keeps every published version.
* **Atomic promo redemption.** `incrementUsage` is a single `jsonb_set` `UPDATE` (no read-modify-write).
* **Restart-safe IDs.** Facades draw `ord-N`, `art-N`, `line-N`, `ORD-YYYY-N` numbers from the
  `IdSequence` port: in-memory in the static demo, `platform.id_sequence` (starts at 10000) in Postgres.
* **Secure tokens.** Postgres mode wires a `SecureRandom` token generator (the sample runtime keeps
  its deterministic demo tokens).
* **DB seeder.** On an empty database the app seeds the `backend/sample` fixtures (catalog, rules,
  pricelists, agreements, promo codes, cost sheets, customers, delivery settings, demo orders &
  artwork, demo credentials). Every step is idempotent.

### Contract tests

Each `02-o-db` module runs **the same behavioural suite against the in-memory adapter and
PostgreSQL** (Testcontainers, `postgres:17-alpine`), e.g.
`./mill 'backend.ordering.02-o-db.test'`. Docker must be running.

## 5. HTTP API design

* **Each BFF owns its API** (boilerplate `01-i-tapir-api` / `02-i-http` tiers): the storefront and
  backoffice contracts compile independently and cannot import each other; route constructors are
  `private[<bff>]` with a public `apply` for the composition root. Only transport-neutral helpers live
  in `backend/api-kernel`.

* **Wire format** = jsoniter codecs from `backend/json`; ADTs use a `"type"` discriminator.
* **Errors** are `<status> {"message": "...", "error": {<typed StorefrontError|BackofficeError>}}`.
  Status is derived from the error case: 401 (unauthenticated / invalid credentials), 403
  (forbidden / not owned / wrong principal kind), 404 (`*NotFound`), 409 (already exists / in use),
  else 400. Clients decode the typed error back, so UI error handling is identical in both modes.
* **Auth** is `Authorization: Bearer <SessionToken>`; optional on storefront pricing/cart/checkout
  endpoints (enables B2B pricing & prefill), required on `/me/**` and all `/admin/**` except login.
* **Prices are never accepted from the client.** Requests carry configurations (`ConfigureProductCmd`)
  and IDs; the server prices them. Cart lines are priced at add-to-cart time; a quick-order checkout
  (`CheckoutSource.QuickSingleLine(QuickOrderItem)`) is re-configured and re-priced on every
  options / preview / submit call.
* **OpenAPI** documents every path, parameter, tag and summary; request/response bodies are
  documented by domain type name. Fully-derived field schemas for the catalog/pricing/order trees
  exceed JVM class-size limits and would bloat the Scala.js bundle — the codecs are the source of
  truth for body structure.

## 6. Artwork blob storage (Phase 5.1)

* `mpbuilder.shared.BlobStorage` port (`put` / `get` / `delete`) with a cross-platform in-memory
  adapter and the JVM `LocalFileBlobStorage` (atomic writes, `.meta` sidecar, strict key validation
  against path traversal). S3/R2 adapters only need to implement the same trait.
* Both facades route every artwork upload through `BlobIngest`: inline `data:` URLs are decoded and
  stored under the artwork's storage key (preview under `<key>.preview`), `sizeBytes` and the
  **SHA-256 checksum** (pure cross-platform implementation) are recorded on `AssetFileMetadata`.
  When the adapter serves files (server mode), the stored URL becomes `/api/v1/blobs/<key>` so large
  payloads never land in database rows; the static browser mode keeps its data URL.
* Discarding a draft artwork also deletes its stored files.

## 7. Known limitations / follow-ups

* Password hashing is salted SHA-256 (unchanged from the identity core) — switch to Argon2/bcrypt
  before production.
* Blob downloads are not access-controlled (keys are unguessable-ish but not secret), and
  `DELETE /artwork/drafts/{id}` is unauthenticated like the existing facade contract — add ownership
  checks (draft session / order owner / staff role) before production.
* Uploads travel as base64 inside JSON (default limit 80 MB per request); a multipart or presigned
  upload endpoint is the next step for large print files.
* Transport failures in the HTTP client surface as defects (UI panic), not typed errors.
* Manufacturing has no tables yet; its in-memory state resets on restart.
