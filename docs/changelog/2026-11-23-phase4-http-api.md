# 2026-11-23 — HTTP API, Server & Frontend Network Wiring (Roadmap Phase 4)

**PR:** N/A
**Author:** Claude Code (agent session)
**Type:** feature

Builds on [Phase 3 — PostgreSQL persistence](2026-11-23-phase3-postgres-persistence.md).

## Summary

Exposed the Storefront and Backoffice BFFs over HTTP/JSON with Tapir + ZIO HTTP, added a runnable
server with `--storage=memory|postgres`, and connected the Kyo UI frontends through HTTP
implementations of the facade traits. The frontends pick their mode at boot, so the static
zero-backend demo keeps working.

## Changes Made

- **build.mill** — new modules `backend.api-kernel` (JVM+JS), `backend.{storefront,backoffice}.01-i-tapir-api`
  (JVM+JS) and `.02-i-http` (JVM), `ui-http-client` (JVM+JS); `backend.app` gains the HTTP server and
  `mainClass`; the SPAs and the calculator depend on `ui-http-client`.
- **backend/{storefront,backoffice}/01-i-tapir-api** — each BFF owns its contracts: `StorefrontEndpoints` (31),
  `BackofficeEndpoints` (69), request DTOs and codecs.
- **backend/api-kernel** — shared transport helpers: bearer auth inputs, validated opaque path codecs,
  typed error envelope with HTTP status mapping, named OpenAPI schemas, `SystemEndpoints`
  (`/healthz`, `/readyz`, `GET /api/v1/blobs/**`).
- **backend/{storefront,backoffice}/02-i-http** — Tapir server routes (`private[<bff>]` constructors).
- **backend/app** — `AppConfig` (flags/env), `AppRuntime` (memory | postgres modes), HTTP server assembly
  (`mpbuilder.app.http`: Swagger UI at `/docs`, Tapir CORS interceptor, 80 MB request limit), `Main`;
  `PlatformE2ESpec` (live HTTP in memory and PostgreSQL mode, incl. server restart & CORS preflight).
- **modules/ui-http-client** — `HttpStorefrontFacade`, `HttpBackofficeFacade`, `HttpTransport`, `HttpClients`.
- **modules/ui, ui-admin, ui-calculator** — `window.__API_BASE_URL__` (calculator: also an `apiBaseUrl`
  mount option) switches to the HTTP clients.
- **docs** — `backend-persistence.md` renamed to `backend-persistence-and-http-api.md` and extended;
  new `running-the-platform.md`; troubleshooting entries; INDEX; CLAUDE.md; roadmap status.

## Decisions & Rationale

- **Each BFF owns its HTTP API** (`01-i-tapir-api` / `02-i-http` per the modulith boilerplate), so storefront
  and backoffice contracts compile and evolve independently; only transport-neutral helpers are shared.
- **HTTP clients implement the ZIO facade traits** → the existing `fromZio` lifts are reused unchanged, and the
  JVM E2E test drives the server through the same client the browser uses.
- **Wire format = the Phase 3 jsoniter codecs**; errors decode back to the exact typed facade error.
- **Named (not fully derived) OpenAPI body schemas** — full Magnolia derivation exceeded JVM class limits
  and would bloat the JS bundle. The production calculator bundle grew only 14.68 MB → 14.81 MB.
- **CORS via Tapir's interceptor**, which answers browser preflights for every endpoint.

## Issues Encountered

- Anonymous given name clashes for `List[_]` codecs → named givens.
- Tapir auto-derivation: missing schemas, inline limit, then "Class too large" → named schemas.
- CORS preflight answered with 405 by zio-http's middleware + Tapir → Tapir `CORSInterceptor`, with a regression test.

## Follow-up Items

- [ ] Ownership/role checks for blob downloads and draft-artwork deletion.
- [ ] Dev-only way to see OTP codes in HTTP mode (memory mode currently has none).
- [ ] Multipart / presigned uploads instead of base64-in-JSON for large print files.
- [ ] Typed transport errors in the HTTP client (currently defects).
