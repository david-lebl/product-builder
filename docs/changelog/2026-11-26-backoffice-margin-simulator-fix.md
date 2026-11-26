# 2026-11-26 — Backoffice Margin Simulator Inputs Fixed

**PR:** t3/fix-backoffice-margin-simulator
**Author:** Claude (agent)
**Type:** bugfix

## Summary

The Backoffice Pricing → Margin Simulator was unusable: picking a category, preset, currency, speed or tier snapped the dropdown back to its first option, and the quantity field lost typed digits. Selects now keep their value, the quantity field is a digit-filtered text input, the simulation reads the current inputs when it runs, and changing an input re-runs the simulation once a result is shown.

## Changes Made

- `modules/ui-admin-core/.../views/PricingManagerView.scala` — simulator: every `<option>` marks `.selected(...)`; quantity uses `input.inputFilter(InputFilter.Digits)` instead of `numberInput`; `runSim` reads all refs at run time; select/input changes re-run the simulation when a result is already displayed; `renderSimulator` is `private[admin]` for tests. Promo-kind and agreement-form selects also mark `.selected`.
- `CustomerManagerView.scala`, `OrderDetailView.scala`, `StaffSettingsView.scala` — same `.selected(...)` fix on all remaining admin selects (segment, line speed, staff role, courier/bank currency).
- `modules/ui-admin-core/src/test/.../BackofficeUiSpec.scala` — new test rendering the simulator with non-default selections and asserting the matching options are `selected` and quantity is digit-filtered.

## Decisions & Rationale

- Kyo UI re-renders regions as HTML strings; browsers ignore `value` on `<select>`, and kyo-ui only uses `select.value` when emitting the attribute (no JS property patch). Marking `option selected` is the pattern already used by the storefront configurator.
- Quantity: same caret problem as #179 (number inputs don't expose `selectionStart`).

## Issues Encountered

- Mill used the system JDK 17 here, so tests were run with `JAVA_HOME` pointed at JDK 25; #183 (pinned `mill-jvm-version`) has since made that unnecessary.

## Follow-up Items

- [ ] Other admin numeric fields (promo value/max uses, agreement discount, credit terms) still use `numberInput`; convert to filtered text inputs if they show the caret problem.
