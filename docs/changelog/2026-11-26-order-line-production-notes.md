# 2026-11-26 — Order-Line Production Instructions

**PR:** N/A (branch `t3/order-line-manufacturing-note`)
**Author:** agent (Claude)
**Type:** feature + analysis

## Summary

Customers can attach optional production instructions to one cart/order line. Analysis of whether to build it
as a customer ↔ backoffice thread instead is in
[analysis/order-line-production-notes.md](../analysis/order-line-production-notes.md) — decided on a dedicated
field now, a single order-level conversation later.

## Changes Made

- `ordering`: `productionNote` on `CartLine` / `OrderLine`, `ProductionNote.normalize`,
  `CartService.updateLineProductionNote`, `OrderService.updateLineProductionNote` (audit event, locked after
  acceptance via `OrderError.ProductionNoteLocked`).
- `storefront`: `addConfiguredProductToCart(productionNote)`, `QuickOrderItem.productionNote`,
  `updateCartLineProductionNote`, `updateMyOrderLineProductionNote`.
- HTTP: `AddToCartRequest.productionNote`, `ProductionNoteRequest`,
  `PUT /storefront/carts/{cartId}/lines/{lineId}/production-note`,
  `PUT /storefront/me/orders/{orderId}/lines/{lineId}/production-note`; `HttpStorefrontFacade`, `StorefrontClient`.
- `manufacturing`: `ProductionSpec.customerInstructions` shown on the Prepress step; `DomainBridges.toProductionSpec` param.
- UI: configurator artwork step textarea, `ProductionNoteEditor` (cart drawer + My orders), checkout summary
  badge, backoffice order detail box; order intake queue badge ("Customer instructions · N"), "Customer instructions" filter and note-text search; EN/CS strings.
- Sample: seed order ORD-2026-0902 has instructions.
- Tests: `StorefrontFacadeSpec`, `ManufacturingDomainSpec`, new `OrderingCodecsSpec`, `BackofficeUiSpec`, `StorefrontUiSpec`.

## Decisions & Rationale

- Separate from staff `specNote` so the customer's wording is never overwritten.
- Editable in the same window as free artwork upload (before acceptance).
- No DB migration: JSONB docs decode the missing field as `None` (covered by `OrderingCodecsSpec`).

## Issues Encountered

- Mill needed JDK 25 (`JAVA_HOME`), see troubleshooting.
- Postgres contract / HTTP E2E suites could not run locally: Colima did not forward new container ports
  (existing troubleshooting entry). Everything else in `./mill __.test` passed.

## Follow-up Items

- [ ] Run the Postgres suites in CI.
- [ ] Order-level customer ↔ backoffice conversation (see analysis sketch).
