# 2026-11-23 — PostgreSQL Persistence & Artwork Blob Storage (Roadmap Phase 3 + 5.1 storage)

**PR:** N/A
**Author:** Claude Code (agent session)
**Type:** feature

## Summary

Implemented Phase 3: every bounded context except manufacturing persists to PostgreSQL through
**Magnum** + HikariCP + Flyway, behind the existing `private[context]` repository ports. Also added
the storage side of Phase 5.1: a `BlobStorage` port wired into both facades. Domain cores, the
facades' public APIs and Kyo UI views are unchanged; the static in-memory demo works as before.

## Changes Made

- **build.mill** — new modules `backend.json` (JVM+JS), `backend.platform`, `backend.<ctx>.02-o-db` ×6
  (identity, customers, catalog, pricing, ordering, artwork) and `backend.app`.
- **backend/shared** — `BlobStorage` port + `InMemoryBlobStorage`, `DataUrl`, `BlobIngest`, `IdSequence`,
  pure cross-platform `Sha256`; `BlobStorageSpec`.
- **backend/storefront, backend/backoffice** — artwork uploads go through `BlobIngest` (stored bytes,
  real size and SHA-256 on `AssetFileMetadata`); `IdSequence` replaces the restart-unsafe ID counter;
  new `BlobFailure` error cases; discarding a draft deletes its stored files.
- **backend/sample** — `SampleShowcaseRuntime.layer(blobStorage = …)` provides `BlobStorage` & `IdSequence`;
  `SampleIdentity.demoCustomerLogins` / `demoStaff`; `seedArtworks` is `private[artwork]`.
- **backend/json** — validated codecs for every opaque type, per-context codec objects, `DomainCodecs`;
  `DomainCodecsSpec` (lossless round-trip of the full sample catalog, ruleset and pricing data).
- **backend/platform** — `Database` (HikariCP + `TransactorZIO`), per-context Flyway `Migrations`,
  `PostgresIdSequence`, `LocalFileBlobStorage`; `PostgresTestSupport` (Testcontainers 2).
- **backend/<ctx>/02-o-db** — Postgres repositories + `V1__<ctx>.sql`; contract specs running the same
  suite against the in-memory and the PostgreSQL adapters.
- **backend/app** — per-context Postgres wiring in `mpbuilder.<ctx>.wiring` (secure token generator,
  logging OTP delivery), `AppRuntime.postgresServices`, idempotent DB seeder; `PostgresRuntimeSpec`
  (facades on PostgreSQL incl. a full runtime rebuild).
- **docker-compose.yml**, `.gitignore` (`/data/`); docs: `backend-persistence.md`, troubleshooting
  entries, INDEX, CLAUDE.md, roadmap status.

## Decisions & Rationale

- **Magnum** (per request) via `magnumzio`'s `TransactorZIO`; hand-written SQL, no repo macros.
- **Schema per bounded context** with its own Flyway history → contexts migrate independently.
- **Indexed key columns + JSONB aggregate**; orders keep a normalized append-only status-event table;
  the catalog keeps an append-only version history; promo redemption is a single atomic `UPDATE`.
- **jsoniter-scala** (cross-compiled, outside the domain cores); opaque types decode through their
  smart constructors so persisted rows can't bypass invariants. The same codecs serve the Phase 4 API.
- **Blob storage belongs with persistence**: the port, adapters and facade ingest land here; the HTTP
  download endpoint ships with Phase 4.
- Manufacturing stays in-memory (deferred, as the roadmap specifies).

## Issues Encountered

- JSONB key reordering broke jsoniter discriminator decoding → `withRequireDiscriminatorFirst(false)`.
- Anonymous given name clashes for `NonEmptyChunk[_]` codecs → named givens.
- Restart-unsafe facade ID counter → `IdSequence` port backed by `platform.id_sequence`.
- `TransactorZIO` has a private constructor → built via `TransactorZIO.layer`.

## Follow-up Items

- [ ] Phase 4: HTTP API exposing the facades (incl. blob downloads) and frontend network wiring.
- [ ] Argon2/bcrypt password hashing; real OTP e-mail delivery (Phase 6).
- [ ] Manufacturing persistence.
