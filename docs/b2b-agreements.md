# B2B Price Agreements

Customer price agreements (`CustomerPriceAgreement`, owned by the `pricing` context) adjust the base
pricelist for a specific customer or for every customer of a tier. They are managed in the backoffice
under **Pricing → B2B Agreements** (Manager / Admin roles; other staff see them read-only).

## Identity & Resolution

- An agreement is identified by **target + currency**: `SpecificCustomer(customerId)` or
  `TierDefault(tier)` (Standard, Silver, Gold, Platinum), in one currency (CZK, EUR, USD, GBP).
- When quoting, a customer-specific agreement wins over the tier default; only an agreement in the
  quote's currency applies.

## Rules

| Rule | Effect |
|---|---|
| Global discount (%) | Discounts every variable charge not covered by a more specific rule |
| Category discounts (%) | Discount every variable charge of products in that category |
| Material discounts (%) | Discount the material charge for that material |
| Finish discounts (%) | Discount the finish charge for that finish |
| Fixed material rates | Replace the pricelist material rate (any `RateModel`) |
| Custom volume curves | Fully replace the pricelist volume curves |
| Minimum order override | Replaces the pricelist minimum order net |

Precedence for a charge: **fixed material rate > material / finish discount > category discount >
global discount**. One-time setup fees are never discounted. Percentages must be within 0–100.

## Backoffice Editor

Creating or editing an agreement opens an editor with two modes:

- **Form** — target (kind, customer or tier, currency), global terms, add/remove rows for category,
  material and finish discounts (items picked from the live catalog), and fixed material rates with
  simple rate models (per unit, per sheet, per m², per linear m, per perimeter m, flat fee). Advanced
  rate models (tiered area, discrete lookup) and custom volume curves are shown and preserved; curves
  can be reverted to the pricelist defaults.
- **JSON** — the whole agreement in its API wire format (`/api/v1/backoffice/pricing/agreements`),
  covering every field including volume curves and advanced rate models. Values are validated by
  the same codecs as the server.

Switching modes carries the current values across. Saving validates everything at once and lists
every problem (missing items, duplicates, out-of-range percentages, negative amounts, invalid JSON,
unknown customer, or a clash with an existing agreement for the same target and currency). Changing
the target or currency of an existing agreement moves it: the new one is saved and the old one removed.
