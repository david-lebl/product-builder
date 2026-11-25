# 2026-11-25 — IndexedDB Persistence for the Static Demo

**PR:** t3/indexeddb-persistent-ui-storage
**Author:** agent (Claude)
**Type:** feature

## Summary

The backend-less demo SPAs now keep the sample runtime's state in the browser's IndexedDB: orders, carts,
customers, logins/sessions, artwork, catalog/pricing edits, promo usage, delivery & bank settings and the ID
sequence survive a page reload. A "Reset demo data" footer button returns to the sample data.

## Changes Made

- `backend/shared/.../StateRef.scala` (new): `StateRef` cell (in-memory + write-through persistent map/value
  variants), `DocStore` port, `DocOp`, `InMemoryDocStore`. `IdSequence.fromState` in `BlobStorage.scala`.
- `backend/sample/.../sample/SampleStorage.scala` (new): `memory` / `load(docStore)` / `reset`, kyo-schema
  decoding with per-collection fallback to seeds, `FormatVersion`.
- In-memory repositories (catalog, pricing, customers, identity, artwork, ordering) take `StateRef` instead of
  `AtomicRef`; every `*SampleWiring` function and `SampleShowcaseRuntime.make` accept `storage: SampleStorage`
  (default `SampleStorage.memory`, so tests and the JVM server are unchanged).
- `backend/sample/src/main/scala-js/.../browser/IndexedDbDocStore.scala`, `BrowserShowcaseRuntime.scala` (new).
- `build.mill`: `backend.sample` depends on `backend.json` (+ kyo-schema-json); `sample.js` adds `src/main/scala-js`.
- `modules/ui/Main.scala`, `modules/ui-admin/AdminMain.scala`: boot through `BrowserShowcaseRuntime`.
- `StorefrontApp.render(onResetDemoData = …)` footer button; EN/CS keys `footer-reset-demo`, `footer-reset-demo-confirm`.
- Tests: `backend/sample/src/test/.../PersistentShowcaseSpec.scala` (reload survival, ID continuity, sessions,
  promo usage, lossless JSON round-trip of every collection, reset, unreadable data, format version).
  `OrderingRepositoryContractSpec` builds its in-memory delivery repo with `StateRef.init`.
- Docs: `docs/backend-persistence-and-http-api.md` §6a, `CLAUDE.md`.

## Decisions & Rationale

- **Write-through cache, not IndexedDB-backed repositories**: state is loaded once at boot, reads stay
  synchronous in memory and the existing repository code is reused unchanged apart from the cell type.
- **Entity-level diffs** (one document per map entry) keep writes small (e.g. one order) even with large
  artwork data URLs in other entities.
- **Seeds are not written** until a collection first changes, so seed updates in new deployments still show up
  for untouched collections; incompatible changes are handled by bumping `FormatVersion`.
- Same JSON codecs as PostgreSQL (`backend/json`), so no second serialization format.
- Manufacturing jobs / OTP challenges are not persisted (matching the Postgres mode scope / transient by nature).

## Issues Encountered

- `req.result` on a `js.Dynamic` resolved to Kyo's `.result` extension — use `req.selectDynamic("result")`
  (see troubleshooting).
- Headless Chrome `--dump-dom` did not let the fastLinkJS bundle finish booting, so the IndexedDB adapter was
  not exercised end-to-end in a browser in this session; the persistence logic is covered on the JVM via
  `DocStore.inMemory`.
- Postgres-backed suites fail locally with `Connection refused` (container port forwarding; pre-existing,
  documented in troubleshooting) — all in-memory variants pass.

## Follow-up Items

- [ ] Manually verify in a browser: place an order, reload, see it in Orders and in `#/admin` intake; Reset demo data.
- [ ] Optional cross-tab sync (BroadcastChannel) to avoid two tabs drawing the same next ID.
- [ ] Optional "Reset demo data" entry in the standalone backoffice.
