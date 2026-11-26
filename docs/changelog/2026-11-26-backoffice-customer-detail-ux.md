# 2026-11-26 — Backoffice Customer Management UX Redesign

**PR:** t3/backoffice-customer-detail-ux
**Author:** Claude (agent)
**Type:** feature

## Summary

Redesigned how staff create and manage customers in the Backoffice:
- Status-aware header buttons, a status banner and KPI tiles.
- New tabs: Profile, Pricing & Terms, Orders, Artworks, Notes & Activity.
- Each profile section is a card with its own Edit button.
- A payment-terms editor that starts from the current terms and checks eligibility, plus a credit-usage meter.
- Tier changes need a confirm step and show a preview of the tier's agreement.
- A complete, validated registration form that generates the customer ID.
- A backend guard against registering a duplicate customer ID.

Spec: [backoffice-customer-management.md](../backoffice-customer-management.md).

## Changes Made

- **Backend**:
  - `CustomerError.CustomerIdAlreadyExists` was added.
  - `CustomerServiceLive.register` now rejects an existing ID. Previously it silently overwrote the existing account.
  - `CustomerDomainSpec` covers the new check.
- **State** (`BackofficeState`):
  - Customer mutations now return `Boolean < Async`, so editors close only when a save succeeds.
  - `registerCustomer` returns `Option[CustomerId]`.
  - New `openCustomer(id)`, used by the dashboard's "Open" button.
- **Views**:
  - `CustomerManagerView` was rewritten.
  - New package `mpbuilder.admin.views.customers`:
    - `CustomerUi`: formatting, order stats, form fields, cards.
    - `CustomerDrafts`: drafts, validation, command builders, ID generation.
    - `CustomerProfileTab`, `CustomerCommercialTab`, `CustomerCreateForm`.
- **Theme**: `mp-cust-*` rules in `AdminTheme`, plus `mp-field-error`, `mp-field-required` and `mp-input-invalid`.
- **i18n**: about 50 new `adm-cust-*` keys (EN + CS). The register button now reads "+ New Customer" / "+ Nový zákazník".
- **Tests**: new `CustomerManagerSpec` with 7 tests:
  - Draft rules: IDs, validation, contact promotion, terms.
  - Register and duplicate ID.
  - EN/CS rendering of the detail, the Pricing & Terms tab and the profile editor.

## Decisions & Rationale

- **Draft refs separate from the parent render.**
  - The editing section (`editingRef`) is part of the detail's combined signal.
  - The keystroke-level draft is rendered locally (`draftRef.render`), so typing only re-renders the open form.
- **One profile draft per customer, with the section shown chosen by `Section`.** Saving any section sends a full
  `UpdateCustomerProfileCmd`. Hidden fields keep the current values.
- **Common edits without opening a form:** *Make primary*, *Remove contact*, *Set default address* and *Remove address*
  are buttons on the card. Removals ask for confirmation.
- **Agreement CRUD stays in Pricing.** It deep-links there, so the agreement editor isn't duplicated.

## Issues Encountered

- The Mill daemon was running on JDK 17. Fixed per the existing troubleshooting entry (`UnsupportedClassVersionError`).
- Kyo UI varargs children cannot take a list built as `head +: list` or `list :+ x` (see troubleshooting).

## Follow-up Items

- [ ] Show staff display names instead of employee IDs on account notes.
- [ ] Optionally offer to tighten payment terms on suspension.
