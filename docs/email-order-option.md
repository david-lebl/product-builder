# Email Order Option — Feature Specification

> Allows customers to request a quote or order via email when the online configuration is incomplete, fails validation, or the customer simply prefers email communication.

## Overview

An "✉ Order via Email" button is shown in the **Validation Status** panel of the product builder. Clicking it opens a popup modal pre-filled with the customer's name, email, and a structured message summarising the current configuration. The customer can edit the message and then open their default email client with a single click.

## Trigger Points

| Location | Always Shown | Condition |
|----------|-------------|-----------|
| Validation Status panel (below validation result) | ✅ | Always — shown for both valid and invalid configurations so customers can also use it as a "confirm via email" shortcut |

## Modal Dialog

### Fields

| Field | Pre-filled From | Editable |
|-------|----------------|----------|
| Your name | `Customer.contactInfo.firstName + lastName` (when logged in) | ✅ |
| Your email | `Customer.contactInfo.email` (when logged in) | ✅ |
| Message | Auto-generated configuration summary (see below) | ✅ |

All three fields can be edited by the customer before sending.

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
10. **Signature** — name and email appended at send time

### Send Action

Clicking **Open Email Client** constructs a `mailto:` URI:

```
mailto:?subject=<encoded subject>&body=<encoded body>
```

- **To:** empty — the customer must enter the shop's email address in their email client. A UI note instructs them to do so.
- **Subject:** `Product Order Inquiry - <category name>` (EN) / `Poptavka objednavky - <category name>` (CS); ASCII only to avoid encoding edge cases.
- **Body:** the full message text from the textarea, followed by the name/email signature.
- The URI is set via `dom.window.location.href`.

## Localisation

All UI labels and auto-generated message text support **English** and **Czech** (`Language.En` / `Language.Cs`).

## Technical Notes

- The modal is a global singleton rendered once in `ProductBuilderApp` to avoid duplicate DOM nodes. The trigger button can be placed anywhere without instantiating another modal.
- State (`isOpen`, `nameVar`, `emailVar`, `textVar`) is held in `Var`s inside `EmailOrderModal` so the user's edits are not overwritten reactively while typing.
- URL encoding uses `scala.scalajs.js.URIUtils.encodeURIComponent` (not `dom.window.encodeURIComponent` which is unavailable).

## Future Enhancements

- Add a configurable shop contact email so the `mailto:` To field can be pre-filled.
- Surface the button in more locations (e.g., as a footer below the "Add to Basket" section in `ConfigurationForm`).
