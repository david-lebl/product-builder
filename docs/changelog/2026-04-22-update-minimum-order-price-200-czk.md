# 2026-04-22 — Update Minimum Order Price to 200 CZK

**PR:** N/A
**Author:** copilot agent
**Type:** bugfix

## Summary

Updated the sample pricelist minimum order price from 500 CZK to 200 CZK as requested. Both CZK pricelists (`pricelistCzk` and `pricelistCzkSheet`) were updated, along with the related test assertions.

## Changes Made

- `modules/domain/src/main/scala/mpbuilder/domain/sample/SamplePricelist.scala` — changed `MinimumOrderPrice(Money("500"))` to `MinimumOrderPrice(Money("200"))` in two places (one per CZK pricelist variant)
- `modules/domain/src/test/scala/mpbuilder/domain/PriceCalculatorSpec.scala` — updated three tests that asserted `breakdown.total == Money("500.00")` to `Money("200.00")` and updated the corresponding comments

## Decisions & Rationale

- Only the `MinimumOrderPrice` rules were changed; other occurrences of `500` in the file (e.g. area tier prices, grommet spacing, quantity tiers) are unrelated pricing values and were left intact.
- The promotional pricelist entry `MinimumOrderPrice(Money("2000"))` was intentionally left unchanged as it is a separate promotional variant.

## Issues Encountered

None.

## Follow-up Items

- [ ] None identified.
