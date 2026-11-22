# Backend Persistence & Artwork Blob Storage

> Phase 3 and 5.1 of [docs_roadmap/platform_roadmap.md](../docs_roadmap/platform_roadmap.md):
> PostgreSQL persistence (Magnum + Flyway) behind the existing repository ports, and the
> `BlobStorage` port for artwork files. Domain cores, facades' public APIs and Kyo UI views are
> unchanged; the static in-memory demo keeps working exactly as before.

## 1. Module map

| Module | Platform | Purpose |
|---|---|---|
| `backend/json` | JVM + JS | jsoniter-scala codecs for every public domain type (JSONB rows today, HTTP wire in Phase 4). Opaque types decode through their validating smart constructors. |
| `backend/platform` | JVM | `Database` (HikariCP + Magnum `TransactorZIO`), per-context Flyway `Migrations`, `PostgresIdSequence`, `LocalFileBlobStorage`. Test fixture `PostgresTestSupport` (Testcontainers). |
| `backend/<ctx>/02-o-db` | JVM | PostgreSQL repositories for `identity`, `customers`, `catalog`, `pricing`, `ordering`, `artwork` + their migrations (`db/migration/<ctx>/V*.sql`). |
| `backend/app` | JVM | Composition root: per-context Postgres wiring (`mpbuilder.<ctx>.wiring`), `AppRuntime.postgresServices` (all facades on PostgreSQL), idempotent DB seeder. |

Manufacturing stays in-memory (deferred to its own chapter, per the roadmap).

## 2. Running locally

```bash
docker compose up -d postgres          # local PostgreSQL 17 (db/user/password: printcraft)

# Contract tests: the same suite against in-memory and PostgreSQL adapters (Docker required)
./mill 'backend.ordering.02-o-db.test'
./mill backend.platform.test

# Facades on PostgreSQL: seed, place & advance an order, rebuild the runtime, verify persistence
./mill backend.app.test
```

`AppRuntime.postgresServices(dbConfig, blobLayer)` builds every service and both facades on
PostgreSQL; `AppRuntime.seedIfEmpty` loads the sample fixtures into an empty database. The HTTP
server that exposes them is Phase 4.

## 3. Persistence design

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

## 4. Artwork blob storage (Phase 5.1)

* `mpbuilder.shared.BlobStorage` port (`put` / `get` / `delete`) with a cross-platform in-memory
  adapter and the JVM `LocalFileBlobStorage` (atomic writes, `.meta` sidecar, strict key validation
  against path traversal). S3/R2 adapters only need to implement the same trait.
* Both facades route every artwork upload through `BlobIngest`: inline `data:` URLs are decoded and
  stored under the artwork's storage key (preview under `<key>.preview`), `sizeBytes` and the
  **SHA-256 checksum** (pure cross-platform implementation) are recorded on `AssetFileMetadata`.
  When the adapter is configured with a public URL prefix (e.g. `/api/v1/blobs`, served by the Phase 4
  HTTP API), the stored URL becomes `<prefix>/<key>` so large payloads never land in database rows; the
  static browser mode keeps its data URL.
* Discarding a draft artwork also deletes its stored files.

## 5. Known limitations / follow-ups

* Password hashing is salted SHA-256 (unchanged from the identity core) — switch to Argon2/bcrypt
  before production.
* Postgres wiring writes one-time login codes to the log (`[DEV OTP DELIVERY]`) until transactional
  e-mail exists (Phase 6).
* Manufacturing has no tables yet; its in-memory state resets on restart.
