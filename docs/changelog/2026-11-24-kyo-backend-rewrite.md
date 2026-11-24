# 2026-11-24 — Backend Rewritten on Kyo (effects, kyo-sql, kyo-http, kyo-schema, kyo-test)

**PR:** N/A (branch `t3/rewrite-backend-in-kyo`)
**Author:** Claude (agent session)
**Type:** refactoring

## Summary

The whole backend moved from ZIO / ZIO Prelude / Magnum + HikariCP + Flyway / jsoniter-scala / Tapir +
ZIO HTTP + sttp / ZIO Test + Testcontainers to the Kyo ecosystem (all `1.0.0-RC7`): Kyo effects
(`kyo-core`, `kyo-combinators`), `kyo-schema-json`, `kyo-sql-postgres`, `kyo-http`, `kyo-test` and
`kyo-pod`. Behaviour, URLs, error semantics and module boundaries are unchanged; the frontend no longer
needs `kyo-zio` (facades are Kyo already). All 141 test executions pass (122 cases; contract specs run
twice), including the live-HTTP E2E in memory and PostgreSQL modes, and all three Scala.js bundles link.

## Changes Made

- **Effects** — every service/facade/repository returns `A < Async` / `A < (Async & Abort[E])`; `Ref` →
  `AtomicRef`, `Clock.instant` → `Clock.now`, `Hub` → a small synchronous `EventBus` (`Subscription.drain`).
  `ZLayer`s replaced by constructors (`XServiceLive.make`, `*SampleWiring`, `SampleShowcaseRuntime.make`
  → `ShowcaseRuntime`). New `mpbuilder.shared.{Validation, NonEmptyChunk, Effects}`.
- **JSON** — `backend/json` now defines kyo-schema `Schema`s (opaque types via `JsonSupport.validated`,
  id/enum-keyed maps as objects, generic `NonEmptyChunk` schema, named givens for recursive predicates).
- **Persistence** — `backend/platform`: kyo-sql `Database`, own `Migrations` runner (per-schema
  `schema_migrations`, advisory lock, checksum verification), `KeyedDoc`; all six `02-o-db` adapters use
  typed JSONB columns (`Sql.jsonColumn`). `MigrationSet` now lists its scripts.
- **HTTP** — `01-i-tapir-api` → `01-i-http-api` (DTOs, schemas, `StorefrontApi` / `BackofficeApi` URL
  layout); `02-i-http` are kyo-http `HttpRoute`s (31 + 69) with per-status error envelopes;
  `backend/app` serves them with kyo-http (`ApiHttpServer`, generated `/openapi.json`, Swagger UI page at
  `/docs`, server-wide CORS); `Main` is a `KyoApp`.
- **Client** — `modules/ui-http-client`: `ApiCaller` + `PlatformHttpTransport` (kyo-http client on the
  JVM, `window.fetch` on Scala.js); `StorefrontClient/BackofficeClient.fromZio` → `fromFacade`.
- **Tests** — all specs on kyo-test; `PostgresTestSupport` uses kyo-pod (one container per test JVM,
  fresh database per test) or `TEST_DATABASE_URL`.
- **Build** — Kyo-only dependency sets, `-Wvalue-discard`, `--enable-native-access=ALL-UNNAMED` fork arg,
  JVM/JS-specific source dirs for `api-kernel` and `ui-http-client`.
- **Docs** — `CLAUDE.md`, `README.md`, `docs/backend-persistence-and-http-api.md` (new §8 Kyo
  conventions), `docs/running-the-platform.md`, troubleshooting entries.

## Decisions & Rationale

- **kyo-http replaces Tapir** — it offers typed routes, server, client and OpenAPI generation in one
  library and uses kyo-schema, so the wire format is shared with persistence. Its Scala.js client targets
  Node sockets, so the browser uses a ~20-line `fetch` transport behind the same `ApiCaller`.
- **Constructors instead of `Layer`/`Env`** for production wiring: explicit, fast to compile, no macro
  wiring; `Env` is used where it reads well (`ShowcaseFacade`, sample specs).
- **Own `Validation`** — Kyo's `Abort`/`Result` short-circuit; the configuration engine must report all
  violations at once.
- **Own migration runner** — Flyway needs JDBC; a 100-line runner on kyo-sql keeps per-context schemas,
  ordering, locking and checksum safety.
- **Wire format change accepted** — kyo-schema's defaults (wrapper objects for sum types, BigDecimal as a
  string) differ from jsoniter's; existing dev databases must be recreated.

## Issues Encountered

- Effects silently discarded via value-discarding (`Unit < Async = ref.updateAndGet(f)`) → stateful
  tests failed; fixed with `ref.update`, `-Wvalue-discard` added. See troubleshooting.
- kyo-http `error[E]` requires non-generic error types → one envelope case class per status per BFF.
- `Method too large` for 69 inline-expanded routes → routes grouped into per-area methods.
- Colima on the dev machine stopped forwarding published container ports → `TEST_DATABASE_URL` escape
  hatch (verified with an SSH tunnel into the VM).
- kyo-test group-level `.handle` did not reach nested leaves → services provided per leaf instead.

## Follow-up Items

- [ ] `/openapi.json` is ~9 MB (schemas inlined per route) — check for a component/`$ref` mode in kyo-http.
- [ ] CORS supports only one origin (kyo-http server policy) — add a reflecting filter if multiple origins are needed.
- [x] Production build size check against `d1c43d6` (pre-rewrite), gzip -9 of `fullLinkJS`: unified SPA 3.05 → 3.25 MB (+6.4%),
      admin SPA 2.84 → 2.94 MB (+3.5%), calculator 2.62 → 2.59 MB (−1.2%); raw +7–17%. Not yet attributed — likely the
      kyo-schema codecs for both BFFs linked via `ui-http-client`, to be confirmed.
- [ ] Upstream the Kyo findings in [analysis/kyo-ecosystem-findings.md](../analysis/kyo-ecosystem-findings.md).
- [ ] Manufacturing persistence (still in-memory).
