# 2026-04-28 — Email Order Option in Product Builder UI

**PR:** copilot/add-email-order-option-ui
**Author:** copilot agent
**Type:** feature

## Summary

Added an "Order via Email" button to the product builder UI so customers can submit an enquiry even when the online configuration is incomplete or fails validation. A popup modal pre-fills name, email (from logged-in customer), and a message textarea with the full current configuration details. Clicking "Open Email Client" opens the default mail app via a `mailto:` URI.

## Changes Made

- **Created** `modules/ui/src/main/scala/mpbuilder/ui/productbuilder/components/EmailOrderModal.scala`
  - Singleton object with `Var`-based state (`isOpen`, `nameVar`, `emailVar`, `textVar`)
  - `open()` reads current `BuilderState` and pre-fills fields; name/email taken from logged-in `Customer.contactInfo` when available
  - `buildEmailText()` formats category, preset, printing method, specs, components/materials/ink/finishes, calculated price, and any validation errors into readable plain text
  - `triggerButton()` renders the styled "✉ Order via Email" button
  - `apply()` renders the full modal overlay (backdrop + dialog with header, form fields, info note, footer buttons)
  - "Open Email Client" button constructs `mailto:?subject=...&body=...` URI using `scala.scalajs.js.URIUtils.encodeURIComponent` and sets `dom.window.location.href`
- **Modified** `modules/ui/src/main/scala/mpbuilder/ui/productbuilder/components/ValidationMessages.scala`
  - Added `email-order-hint` section below validation status: an informational note and the `EmailOrderModal.triggerButton()`
- **Modified** `modules/ui/src/main/scala/mpbuilder/ui/productbuilder/ProductBuilderApp.scala`
  - Added `EmailOrderModal()` at the bottom of the app element so the modal is rendered exactly once as a global singleton
- **Modified** `modules/ui/src/main/resources/basket.css`
  - Added styles for `.email-modal-backdrop`, `.email-modal-dialog`, `.email-modal-header`, `.email-modal-title`, `.email-modal-close`, `.email-modal-body`, `.email-modal-textarea`, `.email-modal-footer`, `.email-modal-send-btn`, `.email-order-hint`, `.email-order-btn`

## Decisions & Rationale

- **Singleton modal at app level** — the modal is rendered once in `ProductBuilderApp` and the trigger button can be placed anywhere via `EmailOrderModal.triggerButton()`. This follows the existing pattern used for the basket drawer.
- **`Var`-based pre-fill, not reactive** — name, email, and text are written into `Var`s when `open()` is called rather than being derived as `Signal`s. This allows the user to freely edit them before sending without re-derived signals overwriting their changes on each keystroke.
- **`mailto:` URI with no hardcoded recipient** — there is no shop email configured in the codebase. The `mailto:` link is generated as `mailto:?subject=…&body=…` so the user's email client opens with all fields except To pre-filled; the UI note instructs users to add the recipient. This avoids hardcoding a placeholder address that would appear in code.
- **Subject uses ASCII hyphen** — em dash (–) was replaced with hyphen (-) in the email subject to avoid rendering issues in older email clients.
- **Validation hint always visible** — the email order hint is shown in the Validation Status panel regardless of whether validation passes or fails, to cover both the "can't complete configuration" and "valid but want to confirm manually" scenarios.

## Issues Encountered

- **Incorrect enum variants** — initial code used `FoldType.Z`, `FoldType.Barrel`, `BindingMethod.HardcoverBinding`, `BindingMethod.WireO`, `BindingMethod.CopticStitch`, and `ManufacturingSpeed.Overnight` which do not exist. Corrected to `FoldType.ZFold`, `FoldType.RollFold`, `FoldType.FrenchFold`, `FoldType.CrossFold`, `BindingMethod.WireOBinding`, `BindingMethod.CaseBinding`, and `ManufacturingSpeed.Economy` after checking `specification.scala` and `manufacturing.scala`. See troubleshooting entry.
- **`dom.window.encodeURIComponent` not available** — `org.scalajs.dom.Window` does not expose `encodeURIComponent`. Used `scala.scalajs.js.URIUtils.encodeURIComponent` instead, while still importing `org.scalajs.dom` for `dom.window.location`.
- **Duplicate import removed** — `ProductBuilderApp.scala` initially added an explicit `import mpbuilder.ui.productbuilder.components.EmailOrderModal` that was redundant with the wildcard `components.*` import already present.

## Follow-up Items

- [ ] Add a configurable shop contact email address so the `mailto:` link can pre-fill the To field — e.g., via a `BuilderConfig` object or environment-injected JS global.
- [ ] Consider placing a second "Order via Email" button at the bottom of `ConfigurationForm.scala` as a footer below the "Add to Basket" section for even more visibility.
- [ ] Consider i18n for the email subject line (currently ASCII-only to avoid encoding edge cases).
