# 2026-04-27 — Order via Email Feature

**PR:** copilot/add-email-order-option
**Author:** copilot agent
**Type:** feature

## Summary

Added a non-blocking "Order via Email" escape hatch to the Product Builder UI. Customers can now request a quote or place an order via their email client at any point during configuration — whether or not the configuration validates successfully. This addresses two scenarios:

1. **Validation fails** — a customer has selected incompatible or incomplete options and cannot proceed to basket. The email form appears in the Validation Status card as a contextual fallback.
2. **Options not found** — a customer didn't find what they needed in the configurator. The email form is always visible at the bottom of the configuration form.

## Changes Made

- **Created** `modules/ui/src/main/scala/mpbuilder/ui/productbuilder/components/EmailOrderForm.scala` — new collapsible Laminar component with name, email, and notes fields. Notes are pre-filled with a configuration summary on first open. Clicking "Open Email Client" constructs a `mailto:` URL with pre-populated subject and body, then navigates to it.
- **Modified** `modules/ui/src/main/scala/mpbuilder/ui/productbuilder/components/ValidationMessages.scala` — appends `EmailOrderForm` (with contextual hint text) below the validation display when there are active errors.
- **Modified** `modules/ui/src/main/scala/mpbuilder/ui/productbuilder/components/ConfigurationForm.scala` — adds a new `form-section` containing `EmailOrderForm` after the "Add to Basket" section.
- **Modified** `modules/ui/src/main/resources/pricing.css` — added CSS for `.email-order-section`, `.email-order-toggle-btn`, `.email-order-form`, `.email-order-notes`, `.email-order-send-btn`, `.email-order-validation-hint`, `.email-order-validation-msg`.

## Decisions & Rationale

- **`mailto:` only, no backend** — keeps the feature self-contained with zero infrastructure changes. The user's email client handles delivery.
- **Pre-filled notes** — the form reads `ProductBuilderViewModel.stateVar.now()` at mount time to produce a human-readable configuration summary (category, printing method, quantity, size, material, estimated price). This gives the sales team useful context without the customer having to type it all out.
- **Two placements** — contextual (errors) + always-visible (escape hatch) serve different user intents. The contextual placement is conditional on `state.validationErrors.nonEmpty`; the form-level one is unconditional.
- **Local `Var` state** — no changes to `BuilderState`; form field state lives entirely inside the component.
- **Contact email as named constant** (`ContactEmail = "orders@example.com"`) — clearly marked for replacement before production deployment. The code review feedback confirmed this needs to be replaced for production use.
- **Decimal dimension formatting** — changed `.toInt` to `f"%.1f"` for size display in the configuration summary, as millimetre values can have fractional precision.

## Issues Encountered

- None during implementation. Build passed first time. All 179 domain tests passed.

## Follow-up Items

- [ ] Replace `ContactEmail = "orders@example.com"` in `EmailOrderForm.scala` with the real production contact email or read it from a configuration source.
- [ ] Consider making the contact email configurable via a UI settings panel or environment variable rather than a hardcoded constant.
