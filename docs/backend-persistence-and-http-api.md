# Backend Persistence, HTTP API & Artwork Blob Storage

> Phases 3, 4 and 5.1 of [docs_roadmap/platform_roadmap.md](../docs_roadmap/platform_roadmap.md):
> PostgreSQL persistence (**kyo-sql**), the HTTP API (**kyo-http**) with frontend network wiring,
> and the `BlobStorage` port for artwork files. The whole backend runs on **Kyo** — see
> [§8 Kyo conventions](#8-kyo-conventions).

## 1. Two runtime modes, zero UI changes

```
              Kyo UI (ui / ui-admin / ui-calculator)
                         │  StorefrontClient / BackofficeClient (unchanged ports)
          ┌──────────────┴───────────────┐
 Mode A: static (default)        Mode B: window.__API_BASE_URL__ set
 fromFacade(SampleShowcaseRuntime)  fromFacade(HttpStorefrontFacade / HttpBackofficeFacade)
                                         │  JSON over HTTP (kyo-schema); fetch in the browser,
                                         │  kyo-http client on the JVM
                                         ▼
                                 <bff>/02-i-http kyo-http routes, mounted by backend/app
                                 (kyo-http server, OpenAPI at /openapi.json, Swagger UI at /docs)
                                         │
                                 backend/app   --storage=memory | --storage=postgres
                                         │
                     StorefrontFacade & BackofficeFacade (Kyo effects)
                                         │
                    context services ── *Repository ports (private[ctx])
                          ├─ in-memory adapters (backend/sample, AtomicRef)
                          └─ PostgreSQL adapters (backend/<ctx>/02-o-db, kyo-sql)
```

The facades already speak Kyo (`A < (Async & Abort[E])`), so `StorefrontClient.fromFacade` /
`BackofficeClient.fromFacade` are plain pass-throughs. The HTTP clients **implement the same facade
traits**, so the JVM test-suite drives the real server through the very same client code the browser
uses (only the raw transport differs).

## 2. Module map

| Module | Platform | Purpose |
|---|---|---|
| `backend/json` | JVM + JS | kyo-schema `Schema`s for every public domain type (shared by JSONB rows and the HTTP wire). Opaque types decode through their validating smart constructors (`JsonSupport.validated`); id/enum-keyed maps encode as JSON objects. |
| `backend/platform` | JVM | `Database` (kyo-sql `SqlClient` pool, native PostgreSQL wire driver), `Migrations` (per-schema, advisory-locked runner), `PostgresIdSequence`, `LocalFileBlobStorage`. Test fixture `PostgresTestSupport` (kyo-pod container or `TEST_DATABASE_URL`). |
| `backend/<ctx>/02-o-db` | JVM | PostgreSQL repositories for `identity`, `customers`, `catalog`, `pricing`, `ordering`, `artwork` + their migrations (`db/migration/<ctx>/V*.sql`). |
| `backend/api-kernel` | JVM + JS | `mpbuilder.api`: error envelope `ApiError`, status mapping (`ApiConventions.statusOf`), health payload, shared schemas. JVM-only `src/main/scala-jvm`: kyo-http helpers (`ApiServer`: bearer extraction, opaque path codecs, OpenAPI docs helper). |
| `backend/storefront/01-i-http-api` | JVM + JS | `mpbuilder.storefront.http.api`: request DTOs, `StorefrontApiCodecs`, URL layout `StorefrontApi`. |
| `backend/storefront/02-i-http` | JVM | `StorefrontRoutes` — kyo-http `HttpRoute`s + handlers bound to `StorefrontFacade` (31 routes). |
| `backend/backoffice/01-i-http-api` | JVM + JS | `mpbuilder.backoffice.http.api`: request DTOs, `BackofficeApiCodecs`, URL layout `BackofficeApi`. |
| `backend/backoffice/02-i-http` | JVM | `BackofficeRoutes` — kyo-http routes bound to `BackofficeFacade` (69 routes, grouped per area). |
| `backend/app` | JVM | Composition root (`KyoApp`): config, per-context Postgres wiring (`mpbuilder.<ctx>.wiring`), DB seeder, server assembly (`mpbuilder.app.http.ApiHttpServer`: both BFFs + system routes, OpenAPI, CORS). |
| `modules/ui-http-client` | JVM + JS | `ApiCaller`, `HttpStorefrontFacade`, `HttpBackofficeFacade`, `HttpClients`; transport `PlatformHttpTransport` = kyo-http client (`scala-jvm`) / `window.fetch` (`scala-js`). |

Manufacturing stays in-memory in both modes (deferred to its own chapter, per the roadmap).

## 3. Running

> Step-by-step instructions: [running-the-platform.md](running-the-platform.md).

```bash
# Mode B with in-memory storage (no database needed)
./mill backend.app.run --storage=memory --port=8080

# Mode B with PostgreSQL
docker compose up -d postgres
DATABASE_URL=postgresql://localhost:5432/printcraft ./mill backend.app.run --storage=postgres

# Point a static SPA at the API: set this before loading main.js
<script>window.__API_BASE_URL__ = "http://localhost:8080";</script>
# …or for the calculator widget: MPCalculator.mount("#calc", { apiBaseUrl: "http://localhost:8080" })
```

| Setting | Flag | Env | Default |
|---|---|---|---|
| Storage | `--storage=memory\|postgres` | `STORAGE` | `memory` |
| Bind | `--host`, `--port` | `HOST`, `PORT` | `0.0.0.0`, `8080` |
| CORS origin | `--cors=https://shop.example` | `CORS_ORIGINS` | `*` (or exactly one origin) |
| Blob backend | `--blob-storage=memory\|local\|gcs` | `BLOB_STORAGE` | `memory` (in memory mode) / `local` (in postgres mode) |
| Blob directory | `--blob-dir` | `BLOB_DIR` | `./data/blobs` |
| Blob URL prefix | `--blob-public-base-url` | `BLOB_PUBLIC_BASE_URL` | `/api/v1/blobs` |
| GCS bucket / prefix / endpoint / creds | `--gcs-bucket`, `--gcs-prefix`, `--gcs-endpoint`, `--gcs-credentials` | `GCS_BUCKET`, `GCS_KEY_PREFIX`, `GCS_ENDPOINT`, `GCS_CREDENTIALS_FILE` / `GOOGLE_APPLICATION_CREDENTIALS`, `GCS_ACCESS_TOKEN` | endpoint `https://storage.googleapis.com`; ADC (or anonymous for custom emulator endpoint) |
| Skip seeding | `--no-seed` | — | seeding on |
| Database | — | `DATABASE_URL` (`postgresql://host:port/db`; a legacy `jdbc:postgresql://…` URL is accepted), `DATABASE_USER`, `DATABASE_PASSWORD`, `DATABASE_POOL_SIZE` | user/pass `printcraft` |

Endpoints: `GET /healthz`, `GET /readyz` (checks the DB), `GET /openapi.json` (generated by kyo-http
from the routes), `GET /docs` (Swagger UI), `/api/v1/storefront/**` (31 routes),
`/api/v1/admin/**` (69 routes, bearer token), `GET /api/v1/blobs/<key>`.

Demo logins are the same as the static demo (`admin@printshop.cz` / `staff1234`,
`jan@printshoppro.cz` / `demo1234`). In server mode OTP codes are written to the server log
(`[DEV OTP DELIVERY]`) until transactional e-mail exists.

## 4. Persistence design (kyo-sql)

* **Native driver, no JDBC.** `kyo-sql-postgres` speaks the PostgreSQL wire protocol on kyo-net with its
  own pool; `Database.run` / `tx` / `attempt` wrap `DB.run(client)` (+ `DB.transaction`). Repository
  ports are `A < Async`, so unexpected SQL errors become panics; `attempt` surfaces typed `DbError`s.
* **Schema per bounded context.** Each context owns a PostgreSQL schema (`identity`, `customers`,
  `catalog`, `pricing`, `ordering`, `artwork`, plus `platform`) with its **own history table**
  (`<schema>.schema_migrations`), so contexts migrate independently. `Migrations` applies the scripts a
  `MigrationSet` lists (in order, each in its own transaction, under a PostgreSQL advisory lock) and
  refuses to start if an applied script's SHA-256 changed. Never edit an applied script — add `V<n+1>__*.sql`
  and append it to the context's `MigrationSet`.
* **Relational key columns + typed JSONB aggregate.** Lookup/filter columns (e-mail, normalized IČO/DIČ,
  status, tier, customer id, placed_at, promo code …) are real indexed columns; the aggregate is a
  `doc JSONB` column bound and read as a typed value through `given SqlSchema.Column[X] = Sql.jsonColumn`
  (kyo-schema). Keyed reads use `KeyedDoc[A](key, doc)` rows (`SELECT order_id AS key, doc …`).
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

> **Upgrading an existing dev database:** the Kyo rewrite changed both the migration history table
> (Flyway → `schema_migrations`) and the JSONB document format (jsoniter → kyo-schema). Recreate local
> databases created before it: `docker compose down -v && docker compose up -d postgres`.

### Contract tests

Each `02-o-db` module runs **the same behavioural suite against the in-memory adapter and
PostgreSQL**, e.g. `./mill 'backend.ordering.02-o-db.test'`. By default `PostgresTestSupport` starts one
`postgres:17-alpine` container per test JVM with **kyo-pod** (Docker or Podman must be running) and creates
a fresh, migrated database per test (dropped on exit). Set `TEST_DATABASE_URL`
(`postgresql://user:pw@host:port/postgres`, a role allowed to `CREATE DATABASE`) to use an existing
server instead (CI, or a Docker VM that cannot forward published ports).

## 5. HTTP API design (kyo-http)

* **Each BFF owns its API**: `01-i-http-api` (cross-compiled DTOs, schemas, URL layout) and `02-i-http`
  (JVM kyo-http routes). The two BFFs compile independently and cannot import each other; route
  constructors are `private[<bff>]` with a public `apply` for the composition root.
* **Routes are typed kyo-http `HttpRoute`s**: path captures (`Capture[CartId]("cartId")`, decoded through
  the smart constructor), `headerOpt("authorization")`, `bodyJson[T]` / `.response(_.bodyJson[T])`,
  `.metadata(tag, summary)`; the server generates `/openapi.json` from them.
* **Wire format** = kyo-schema JSON from `backend/json`: case classes as objects, sum types as
  `{"Variant": {...}}`, `BigDecimal`/`Money` as JSON strings (no precision loss), id/enum-keyed maps as objects.
* **Errors** are `<status> {"message": "...", "error": {<typed StorefrontError|BackofficeError>}}`.
  Status is derived from the error case (`ApiConventions.statusOf`): 401 (unauthenticated / invalid
  credentials), 403 (forbidden / not owned / wrong principal kind), 404 (`*NotFound`), 409 (already exists /
  in use), else 400. kyo-http maps one concrete error type to one status, so each route module declares one
  envelope case class per status (`BadRequest`, `Unauthorized`, …) with that same wire shape. Clients decode
  the typed error back, so UI error handling is identical in both modes.
* **Auth** is `Authorization: Bearer <SessionToken>`; optional on storefront pricing/cart/checkout
  endpoints (enables B2B pricing & prefill), required on `/me/**` and all `/admin/**` except login.
* **CORS** is kyo-http's server-wide policy (preflights answered for every route); one origin or `*`.
* **Prices are never accepted from the client.** Requests carry configurations (`ConfigureProductCmd`)
  and IDs; the server prices them. Cart lines are priced at add-to-cart time; a quick-order checkout
  (`CheckoutSource.QuickSingleLine(QuickOrderItem)`) is re-configured and re-priced on every
  options / preview / submit call.
* **Browser transport.** kyo-http's Scala.js client targets Node sockets, so the SPAs use a tiny
  `window.fetch` transport (`modules/ui-http-client/src/main/scala-js`); request building, error decoding
  and JSON are shared (`ApiCaller`).

## 6. Artwork blob storage (Phase 5.1)

* `mpbuilder.shared.BlobStorage` port (`put` / `get` / `delete`, `A < (Async & Abort[BlobError])`) with a
  cross-platform in-memory adapter, the JVM `LocalFileBlobStorage` (atomic writes, `.meta` sidecar,
  strict key validation against path traversal), and `GcsBlobStorage` (Google Cloud Storage JSON API v1
  over non-blocking `java.net.http.HttpClient`, with ADC / service-account RS256 JWT / authorized-user /
  metadata-server OAuth2 auth, token caching + 401 refresh, transient retries, and optional `keyPrefix`).
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
* Uploads travel as base64 inside JSON (limit 80 MB per request); a multipart or presigned upload
  endpoint is the next step for large print files.
* The generated `/openapi.json` inlines every body/error schema per route (~9 MB); Swagger UI loads it,
  but slowly.
* CORS allows a single origin (or `*`): kyo-http's server-wide policy has no allow-list.
* Transport failures in the HTTP client surface as panics (UI "unexpected error"), not typed errors.
* Manufacturing has no tables yet; its in-memory state resets on restart.

## 8. Kyo conventions

* **Effect rows.** Services return `A < Async` (infallible) or `A < (Async & Abort[E])`; in-memory
  adapters (`AtomicRef`) are `Sync`, which widens to `Async`. No `Env`/`Layer` in production wiring —
  constructors (`XServiceLive.make`, `XSampleWiring.service`, `SampleShowcaseRuntime.make`) wire services
  explicitly; specs may provide services through `Env` (`ShowcaseTestEnv`).
* **Shared helpers** (`mpbuilder.shared.Effects`): `opt.orFail(e)`, `either.orAbort`, `effect.someOrFail(e)`,
  `.either`, `.orNone`, `.ignore`, `.as(x)`, `ref.update(f)` / `ref.modify(f)`, `Effects.traverse`,
  `Effects.failWhen/failUnless`, `Effects.now`. kyo-combinators adds `mapAbort`, `recover`, `orPanic`, `*>`.
* **Validation.** Kyo has no accumulating validation, so `mpbuilder.shared.Validation[E, A]` /
  `NonEmptyChunk` keep the smart-constructor & configuration-engine semantics (all errors at once);
  bridge with `.toAbort` / `.toResult`.
* **Never discard an effect.** A Kyo computation is a value: `def save(...): Unit < Async = ref.updateAndGet(f)`
  compiles via value-discarding and **never runs**. The build enables `-Wvalue-discard`; use `.unit` /
  `ref.update`.
* **Branches.** An `if`/`match` mixing a pure value and an effect needs an ascription
  (`(… ): A < (Async & Abort[E])`) before `.map`/`<-`.
* **Tests** use kyo-test (`class XSpec extends Test[Any]`, `"group" - { "case" in { … } }`, `assert(…)`);
  time-dependent specs use `Clock.withTimeControl`.
