# 2026-11-23 — Quick Order Re-priced on the Server

**PR:** N/A (branch `rewrite/backend-core`)
**Author:** Claude Code (agent session)
**Type:** bugfix

## Summary

"Buy now" (quick order) checkout used to accept a client-built `CartLine`, including its `LinePriceSnapshot`
and `ProductSnapshot`. Over the HTTP API, a client could edit the JSON and place an order at any price.
Quick orders now send only the product configuration. The storefront facade configures and prices it
server-side on every checkout call.

## Changes Made

- `backend/storefront/.../StorefrontModels.scala` — new `QuickOrderItem(cmd: ConfigureProductCmd, currency, speed, copies, artworkIds)`;
  `CheckoutSource.QuickSingleLine(item: QuickOrderItem)` replaces `QuickSingleLine(line: CartLine)`.
- `backend/storefront/.../StorefrontFacade.scala` — `loadCheckoutLines(source, sessionToken, quickLineId)` runs
  `configureAndQuote` for quick items (customer-specific pricing via the session token) and builds the line with
  `StorefrontTranslators.toCartLine`. `getCheckoutOptions` / `previewCheckout` use the fixed line ID `quick-preview`;
  `submitCheckout` draws a real line ID from `IdSequence`.
- `modules/ui-core/.../AppState.scala` — `startQuickOrder(draft: DraftConfig)` builds a `QuickOrderItem` from the draft;
  new `startPresetQuickOrder(categoryId, presetId)` for the product-detail page.
- `modules/ui-core/.../StorefrontApp.scala`, `ProductDetailView.scala` — call the new methods.
- Tests: `StorefrontFacadeSpec` (new assertion: the placed quick-order line price equals the server's quote),
  `BackofficeFacadeSpec`, `PlatformE2ESpec`, `PostgresRuntimeSpec` now send `QuickOrderItem`.

## Decisions & Rationale

- **Send the configuration, not a price.** The `QuickOrderItem` type cannot carry a price, so the client has nothing to tamper with.
  This matches how `addConfiguredProductToCart` already works.
- **Fixed preview line ID.** Options and preview are called repeatedly while the customer fills in checkout. Using `IdSequence`
  there would use up persistent sequence values, so only `submitCheckout` allocates a real line ID.
- **`currency` lives on the item.** `SubmitCheckoutCmd` has no currency field, and the quick line has to be priced in one.
- **Breaking wire change.** The JSON shape of `CheckoutSource.QuickSingleLine` changed. The UI and HTTP client are in this repo
  and share the codecs, so no compatibility shim was added.

## Issues Encountered

- `./mill __.compile` failed with `UnsupportedClassVersionError` (class file version 69.0) because the Mill daemon ran on JDK 17.
  Resolved by switching to JDK 25 and restarting the daemon. See troubleshooting.md.
- The first version of the new test assertion compared the whole `LinePriceSnapshot` and failed on `quoteId`, which is unique per quote.
  The assertion now compares everything except `quoteId`.

## Follow-up Items

- [ ] Cart lines keep the price snapshot from add-to-cart time and are not re-priced at checkout. Decide whether a pricelist change
      should re-price open carts or reject checkout.
- [ ] Configurator `recompute` makes 1 quote + 2 calls per component on every change with no debounce, which costs several
      round trips per click in HTTP mode.
