# 2026-11-27 — Full Product Configuration on Cart & Order Lines

**PR:** N/A (branch `t3/store-full-product-config-on-order-lines`)
**Author:** agent (Claude)
**Type:** feature

## Summary

Cart and order lines kept only a product summary, so orientation, bleed, binding, delivery format and finish
sides/settings were lost. Lines now freeze the full configuration (see
[order-line-product-configuration.md](../order-line-product-configuration.md)).

## Changes Made

- `ordering` (`OrderingApi.scala`): new `FinishSnapshot` (side, params, foil color), `ProductSpecSnapshot`
  (dimensions, orientation, bleed, binding, delivery format); required `ProductSnapshot.specs`;
  `ComponentSnapshot.finishes` replaces `finishIds`; helpers `component`, `bleed`, `expectedArtworkPages`.
- `storefront` (`StorefrontTranslators`): `toFinishSnapshot`, `toProductSpecSnapshot` with exhaustive enum
  mappings; `toProductSnapshot` fills them; `defaultBleed` (5 mm large-format inkjet, else 3 mm) also used by
  `DomainBridges.toPreflightRequirement`. `DomainBridges.toProductSnapshot` now delegates (removed duplicate).
- Storefront & backoffice facades: order-line artwork preflight uses the ordered bleed and binding page count;
  removed the two duplicated `defaultPageCountFor` helpers.
- UI: new `LineSpecFormat` (ui-core) — EN/CS labels reusing the catalog label helpers; spec chips + finish
  details in the backoffice order detail; summary line in the cart drawer and My orders.
- Sample: seed lines carry a spec; ORD-2026-0901 has matte lamination (both sides) + rounded corners (4, r 3 mm).
- Tests: `OrderingDomainSpec` (page counts), `OrderingCodecsSpec` (full round-trip),
  `StorefrontFacadeSpec` (booklet cart → order), `BackofficeUiSpec` (order detail shows spec and finishes).

## Decisions & Rationale

- Mirror types in `ordering` instead of importing catalog types, to keep cores dependent only on `shared`
  (same approach as `manufacturing`). Explicit `match` mappings so a new catalog case fails compilation.
- No backward compatibility (early development): the new fields are required, `finishIds` was dropped, and
  bleed is resolved to a concrete value when the line is frozen instead of being optional. Existing stored
  carts/orders from before this change will not decode — reset local databases.
- Orientation, binding and finish side/params stay `Option`: they genuinely don't apply to every product.
- Booklet body artwork now expects the bound page count (e.g. 12 for 16 pp + cover) instead of 1–2 pages.

## Issues Encountered

- kyo-pod Postgres containers were unreachable (existing troubleshooting entry); ran the DB/E2E suites with
  `TEST_DATABASE_URL` against a local Postgres. `./mill __.test` passed fully.

## Follow-up Items

- [ ] Pass the order-line snapshot (not only the quote-time `ConfiguredProduct`) into manufacturing `ProductionSpec`.
- [ ] Consider storing full ink configuration (process vs. spot channels) instead of only the notation.
