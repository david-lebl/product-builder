# 2026-11-26 — Backoffice B2B Agreement Editor

**PR:** N/A (branch `t3/backoffice-b2b-agreement-config`)
**Author:** agent (Claude)
**Type:** feature

## Summary

The backoffice can now configure every part of a B2B price agreement. Previously only the global
discount and minimum order could be edited; category/material/finish overrides were view-only. A new
editor offers a structured form plus a JSON mode covering the full aggregate.

## Changes Made

- `modules/ui-admin-core/.../admin/pricing/AgreementDraft.scala` (new) — string-backed form model,
  `from` / `toAgreement` with accumulated EN/CS validation issues, preserved advanced rate models.
- `modules/ui-admin-core/.../admin/pricing/AgreementJson.scala` (new) — encode (pretty-printed) /
  decode via the shared kyo-schema `PricingCodecs`.
- `modules/ui-admin-core/.../admin/views/AgreementEditorView.scala` (new) — Form/JSON editor and the
  read-only rule summary (catalog names resolved, volume curves shown).
- `PricingManagerView.scala` — create panel and detail panel use the editor; actions "Edit Agreement",
  "Edit as JSON", "Delete"; scoped-rule count includes fixed rates and volume curves.
- `BackofficeState.replaceAgreement` — save, delete the previous key when re-targeted, report success.
- `AdminTranslations.scala` — `adm-pri-agr-edit`, `adm-pri-agr-edit-json`, `adm-pri-agr-rules`
  (replacing `adm-pri-agr-edit-global`, `adm-pri-agr-scoped-readonly`).
- `build.mill` — `ui-admin-core` now depends on `backend.json` (+ `jsonDeps`).
- Tests: `modules/ui-admin-core/src/test/scala/mpbuilder/admin/AgreementEditorSpec.scala`.
- Docs: `docs/b2b-agreements.md`.

## Decisions & Rationale

- No backend change needed: the save endpoint already persists the whole aggregate.
- Volume curves and tiered/discrete fixed rates are JSON-only to keep the form simple; the form never
  drops them.
- JSON mode uses the exact wire format so it doubles as API documentation and is validated by the
  same smart constructors as HTTP/DB.
- Numeric inputs are filtered text inputs (`InputFilter.Decimal`), per the caret fix in #179.

## Issues Encountered

- Default shell JDK 17 → `UnsupportedClassVersionError`; build with JDK 25 (see troubleshooting).
- Kyo UI children need `UI.fragment(xs*)` rather than splatting a `List[UI]` alongside other children.
- Review (#186): editor dropdowns snapped back to their first option after a pick — Kyo UI re-renders
  regions as HTML and browsers ignore `value` on `<select>`. Every editor `<option>` now sets
  `.selected(...)`; `AgreementEditorSpec` asserts the selected options.

## Follow-up Items

- [ ] Structured editor for custom volume curves (scope, basis, tier rows).
- [ ] "Duplicate to another currency" action.
