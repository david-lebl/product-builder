# 2026-11-28 — Session & Cart Persistence Across Reloads

**PR:** N/A (branch `t3/session-cart-persistence`)
**Author:** agent (Claude)
**Type:** feature

## Summary

In network mode (`window.__API_BASE_URL__`) the unified SPA now keeps the signed-in session and the active cart
across page reloads and new tabs. Only the handles (session token, cart id) are stored in `localStorage`; the
backend re-validates them on boot.

## Changes Made

- `StorefrontFacade.restoreSession(token): AuthResult` (validate session + load customer), exposed as
  `GET /api/v1/auth/session` (`StorefrontRoutes`), `HttpStorefrontFacade` and the `StorefrontClient` port.
- New port `mpbuilder.ui.port.SessionStore` / `PersistedSession` (ui-core, DOM-free) with `none` and `inMemory`.
- `StorefrontState` (`AppState.scala`): takes a `sessionStore`; restores session + cart on `init` (before the
  configurator computes quotes), persists on sign-in, sign-out, cart creation, currency switch and cart checkout;
  sign-out drops a customer-bound cart; cart ids are now random instead of a per-page counter.
- `modules/ui/.../Main.scala`: `localStorage`-backed store keyed by API base URL (Mode B only).
- Tests: two new `StorefrontUiSpec` cases (reload restores session & cart; sign-out / stale handles are dropped);
  `PlatformE2ESpec` asserts `restoreSession` over HTTP (valid and bogus token).
- Docs: `backend-persistence-and-http-api.md` (session & cart persistence paragraph).

## Decisions & Rationale

- Persist the token only and re-validate server-side (new endpoint) rather than serialising the whole
  `AuthSession` client-side — expiry/revocation stay authoritative and no stale principal is shown.
- Mode A persists nothing: the in-memory runtime is rebuilt each load, so restored handles could never resolve.
- Restored carts belonging to another customer are discarded (shared-computer privacy); sign-out does the same
  so behaviour is identical with and without a reload.
- Random cart ids: persisted ids outlive the page, so `cart-ui-1` from every fresh page load would collide and
  `createCart` overwrites an existing cart.

## Issues Encountered

- `AtomicRef` has no `update` in the effectful sense used by `Effects` for `SignalRef`; used `updateAndGet(...).unit`.
- `backend.app.test` Postgres variants fail locally with `Connection refused` to the kyo-pod container (see the
  existing troubleshooting entry); memory-mode E2E, including the new endpoint, passes.

## Follow-up Items

- [ ] Live cross-tab sync via the `storage` event (sign-in/out or cart change in one tab reflected in open tabs).
- [ ] Persist the staff backoffice session the same way.
- [ ] Persist the selected currency.
