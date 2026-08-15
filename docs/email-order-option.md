# Email Order Option — Feature Specification

> Allows customers to request a quote or order via email when the online configuration is incomplete, fails validation, or the customer simply prefers email communication.

## Overview

An "✉ Order via Email" button is shown in the **Validation Status** panel of the product builder. Clicking it opens a popup modal pre-filled with the customer's name, email, and a structured message summarising the current configuration. The customer can edit the message and then open their default email client with a single click.

## Trigger Points

| Location | Always Shown | Condition |
|----------|-------------|-----------|
| Validation Status panel (below validation result) | ✅ | Always — shown for both valid and invalid configurations so customers can also use it as a "confirm via email" shortcut |
| Next to **Add to Basket** in the configuration form ("✉ Order this by e-mail") | ✅ | Always, and deliberately **not** disabled when *Add to Basket* is — the point of ordering by e-mail is to cover what the configurator cannot, and the message carries any validation issues with it. Skips the basket: orders just the configuration on screen, at the quantity in the adjacent *Quantity to add* field. |

## Modal Dialog

### Fields

| Field | Pre-filled From | Editable |
|-------|----------------|----------|
| Your name | `Customer.contactInfo.firstName + lastName` (when logged in) | ✅ |
| Your email | `Customer.contactInfo.email` (when logged in) | ✅ |
| Your phone | `Customer.contactInfo.phone` (when logged in); optional | ✅ |
| Message | Auto-generated summary — of the current configuration, or of the whole basket (see Modes below) | ✅ |

All fields can be edited by the customer before sending.

## Modes

The modal has two modes, sharing the same fields and send action.

| Mode | Opened by | Message describes |
|------|-----------|-------------------|
| Single configuration | `open(copies)` — the ✉ Order via Email button in the Validation Status panel (`copies = 1`), or ✉ Order this by e-mail next to *Add to Basket* (`copies` = the *Quantity to add* field) | the configuration currently being built |
| Basket | `openForBasket()` — the basket's primary button in the standalone calculator | every basket item plus the grand total |

When `copies > 1` the price line shows the unit price and the line total —
`Calculated Price: 290.00 Kč each × 3 = 870.00 Kč` — rather than dropping the count.

Basket mode is the standalone calculator's replacement for the checkout wizard; see
[standalone-calculator.md](standalone-calculator.md). The full SPA's basket still leads to
checkout, and only uses single-configuration mode.

### Message Pre-fill Content

The auto-generated message includes, in order:

1. **Greeting line** (localised EN/CS)
2. **Category name**
3. **Preset/variant name** (if a preset is selected)
4. **Printing method**
5. **Specifications** — quantity, size, orientation, fold type, binding method, pages, manufacturing speed
6. **Components** — for each role (Main / Cover / Body / Stand): material name, ink notation, list of applied finishes
7. **Calculated price** (if validation succeeded and price was computed)
8. **Validation issues** — if any errors exist, they are listed as a note so the shop can see what combination the customer was trying to achieve
9. **Call to action** (localised)
10. **Signature** — name, email and phone appended at send time

In **basket mode** the body instead lists each item as `N) <category> — <qty>× — <line total>`,
each followed by its printing method, specifications and per-component material / ink notation /
finishes, then the basket grand total. It closes by stating that the prices and lead times come
from the online calculator and are indicative, and asks the shop to confirm the price and
production date and to say where artwork should be sent.

### Send Action

Clicking **Open Email Client** constructs a `mailto:` URI:

```
mailto:<recipient>?subject=<encoded subject>&body=<encoded body>
```

- **To:** `BuilderEnvironment.orderEmail`. Blank in the full SPA, so the customer enters the shop's address themselves; the standalone calculator fills it from its `orderEmail` mount option. The UI note adapts to say which of the two applies.
- **Subject:** `Product Order Inquiry - <category name>` (EN) / `Poptavka objednavky - <category name>` (CS) in single-configuration mode; `Order Request (N items)` / `Objednavka (N polozek)` in basket mode. ASCII only to avoid encoding edge cases.
- **Body:** the full message text from the textarea, followed by the name/email/phone signature.
- The URI is set via `dom.window.location.href`.

> **Length limit.** Some mail clients truncate long `mailto:` URLs — Outlook at roughly 2000 characters. A large multi-item basket can exceed that. Replacing `mailto:` with a POST endpoint is a tracked follow-up.

## Localisation

All UI labels and auto-generated message text support **English** and **Czech** (`Language.En` / `Language.Cs`).

## Technical Notes

- The modal is a global singleton rendered once in `ProductBuilderApp` to avoid duplicate DOM nodes. The trigger button can be placed anywhere without instantiating another modal.
- State (`isOpen`, `modeVar`, `nameVar`, `emailVar`, `phoneVar`, `textVar`) is held in `Var`s inside `EmailOrderModal` so the user's edits are not overwritten reactively while typing.
- `formatSpec` must stay exhaustive over `SpecValue`: basket mode iterates every spec a `ProductConfiguration` carries, so a missing arm is a `MatchError` rather than a warning.
- Ink is rendered with `InkConfiguration.notation` ("4/0"), never the case class's `toString`.
- URL encoding uses `scala.scalajs.js.URIUtils.encodeURIComponent` (not `dom.window.encodeURIComponent` which is unavailable).

## Future Enhancements

- Replace `mailto:` with a POST endpoint so long baskets cannot be truncated and the shop receives structured data.
- Surface the button in more locations (e.g., as a footer below the "Add to Basket" section in `ConfigurationForm`).
