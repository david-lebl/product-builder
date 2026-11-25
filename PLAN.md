# Product Builder — Goals & Roadmap

> Short status overview. The detailed analysis, target architecture and per-phase scope live in
> **[docs_roadmap/platform_roadmap.md](docs_roadmap/platform_roadmap.md)**; per-session details are in
> [docs/changelog/](docs/changelog/).

## Vision

One platform for a print shop: customers configure, price and order print products (with artwork
upload and automated preflight, or a visual editor), and staff take those orders through intake and
production while managing catalog, pricing, customers and store settings. The platform runs both as a
**zero-backend static demo** and as a **client-server deployment on PostgreSQL**, from the same code.

## Guiding Decisions

1. **Cross-compiled modular monolith.** Each bounded context (`catalog`, `pricing`, `customers`, `identity`, `ordering`, `artwork`, `manufacturing`) has a `01-c-core` that depends only on `backend/shared` and compiles to JVM and Scala.js. See [guides/scala_3_modulith_boilerplate.md](guides/scala_3_modulith_boilerplate.md).
2. **Rules and pricing as data.** Compatibility rules and pricing rules are ADTs interpreted by engines, so new products need data, not code.
3. **Validated domain types.** Opaque types with ZIO Prelude `Validation` smart constructors; errors accumulate rather than short-circuit; `Money` is never a `Double`.
4. **Transport-agnostic UI.** Kyo UI views talk to the `StorefrontClient` / `BackofficeClient` ports; the same views run in-memory or over HTTP.
5. **In-memory first, then persistence, then network.** Workflows are proven in-memory before schemas and wire formats are frozen.
6. **Each BFF owns its API**, and **each context owns its schema and migrations.**

## Status

| Phase | Scope | Status |
|---|---|---|
| Foundations | Modulith rewrite: bounded contexts, rule & pricing engines, Kyo UI storefront, configurator, embeddable calculator | ✅ Done |
| 1 | Backoffice domain & order intake core (RBAC facade, manual lifecycle, audit trail, admin CRUD) | ✅ Done |
| 2 | Backoffice & order intake Kyo UI (unified `#/admin` demo + standalone SPA), refinements, in-memory artwork management | ✅ Done |
| 3 | PostgreSQL persistence: Magnum + Flyway, schema per context, DB seeder, in-memory ⇄ Postgres contract tests | ✅ Done |
| 4 | HTTP API per BFF (Tapir + ZIO HTTP, OpenAPI), `--storage=memory\|postgres` server, HTTP clients & frontend mode switch | ✅ Done |
| 5.1 | Artwork blob storage (`BlobStorage` port, local filesystem adapter, SHA-256), download endpoint | ✅ Done |
| 5.2 | Visual editor PoC in isolated `modules/ui-editor` (SVG canvas, `ArtworkDraftBridge` into the artwork context) | ⏭ Next |
| 6+ | Manufacturing & shop-floor UI (incl. persistence), payment / e-mail / courier integrations, S3/R2 storage, print-ready PDF engine | 🗓 Later |

## Before Production

Known gaps, tracked in [docs/backend-persistence-and-http-api.md](docs/backend-persistence-and-http-api.md#7-known-limitations--follow-ups):

- Stronger password hashing (Argon2 / bcrypt) and real OTP e-mail delivery (codes are logged in dev).
- Ownership and role checks on blob downloads and draft-artwork deletion.
- Multipart or presigned uploads for large print files (currently base64 in JSON, 80 MB limit).
- Typed transport errors in the HTTP client.
- Manufacturing persistence (still in-memory).
