# 2026-04-16 — Compact ProductBuilder UI Refresh

**PR:** N/A  
**Author:** copilot-swe-agent  
**Type:** refactoring

## Summary

Refined the ProductBuilder configuration UI to a denser, more compact layout with horizontal label/control rows, inline label help actions, slimmer field chrome, horizontal speed-tier cards, and simplified section headings.

## Changes Made

- Modified UI kit field wrappers to support horizontal form-group layout and inline help slots:
  - `modules/ui-framework/src/main/scala/mpbuilder/uikit/fields/FormGroup.scala`
  - `modules/ui-framework/src/main/scala/mpbuilder/uikit/fields/SelectField.scala`
  - `modules/ui-framework/src/main/scala/mpbuilder/uikit/fields/TextField.scala`
  - `modules/ui-framework/src/main/scala/mpbuilder/uikit/fields/TextAreaField.scala`
- Refactored ProductBuilder selectors to use inline help in label rows and horizontal field layout:
  - `modules/ui/src/main/scala/mpbuilder/ui/productbuilder/components/CategorySelector.scala`
  - `modules/ui/src/main/scala/mpbuilder/ui/productbuilder/components/MaterialSelector.scala`
  - `modules/ui/src/main/scala/mpbuilder/ui/productbuilder/components/PrintingMethodSelector.scala`
  - `modules/ui/src/main/scala/mpbuilder/ui/productbuilder/components/InkConfigSelector.scala`
- Updated ProductBuilder form composition and copy:
  - `modules/ui/src/main/scala/mpbuilder/ui/productbuilder/components/ConfigurationForm.scala`
  - `modules/ui/src/main/scala/mpbuilder/ui/productbuilder/components/SpecificationForm.scala`
  - `modules/ui/src/main/scala/mpbuilder/ui/productbuilder/components/FinishSelector.scala`
- Updated compact styling and spacing:
  - `modules/ui/src/main/resources/utilities.css`
  - `modules/ui/src/main/resources/uikit.css`
- Updated specification docs for the new compact configurator behavior:
  - `docs/features.md`
  - `docs/INDEX.md`

## Decisions & Rationale

- Extended `FormGroup` with optional horizontal and help slots instead of replacing default behavior, to remain backward-compatible with non-ProductBuilder screens.
- Moved help buttons into label rows to avoid absolute-positioned selector helpers and improve alignment consistency.
- Kept speed-tier cards as existing semantic controls but switched to horizontal desktop layout for denser scanning.
- Left section numbers in place while shortening titles to preserve workflow orientation with less visual noise.

## Issues Encountered

- Local validation/build commands are blocked in this sandbox by an upstream SSL handshake error while Mill downloads `mill-runner-daemon_3`.
- CI investigation via GitHub Actions logs showed an unrelated deploy-preview failure (`fatal: not in a git directory`) in a separate run.

## Follow-up Items

- [ ] Re-run `./mill ui.compile` and targeted tests in an environment without SSL trust issues.
- [ ] Manually verify final compact layout in a full running app build across desktop/mobile breakpoints.
