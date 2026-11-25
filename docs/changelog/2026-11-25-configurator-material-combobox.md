# 2026-11-25 — Searchable Material Combo Box in the Configurator

**PR:** N/A (branch `t3/configurator-material-searchable-combobox`)
**Author:** Claude (agent)
**Type:** feature

## Summary

Replaced the configurator's wrap-grid of material cards with a rich, searchable single-select combo box (Vaadin-style), plus one-click "weight variant" pills for the selected paper line. Material, surface type, properties and paper weight are all searchable from one input.

## Changes Made

- **New** `modules/ui-core/.../components/ComboBox.scala` — generic rich combo box: rich trigger (swatch, label, meta line, weight badge), search input, grouped listbox, diacritic/case-insensitive match highlighting, keyboard navigation (↑/↓, PgUp/PgDn, Enter, Esc, Tab), result counter, empty state with "Clear search". State lives in a `ComboBox.Handle` so typing never re-creates the input.
- **New** `modules/ui-core/.../configurator/MaterialSearch.scala` — pure search: AND across whitespace terms; text terms match EN + CS name/description/family/surface/properties/weight plus synonyms; weight terms `200-300`, `>=250`, `<200` (optional `g`/`gsm`/`g/m2` suffix) filter by grammage; display order family → surface → weight → name; `weightVariants` finds the same paper line in other grammages.
- `ConfiguratorView.scala` — material section now `renderMaterialPicker` (combo box + "Also available in" weight pills).
- `ConfiguratorState.scala` — `ConfiguratorController.materialPicker: ComboBox.Handle`.
- `CommonComponents.scala` — localized `materialFamilyLabel`, `surfaceCoatingLabel`, `materialPropertyLabel` (previously raw enum `toString`, untranslated in CS).
- `AppTheme.scala` — `mp-combo-*`, `mp-swatch-<surface>`, `mp-weight-variants` rules.
- `Translations.scala` — `cfg-material-*` keys (EN & CS).
- `StorefrontUiSpec.scala` — 2 tests: search semantics (surface, CS diacritics, ranges, comparators, variants, highlight) and closed/open render in EN & CS.

## Decisions & Rationale

- **Weight stays inside the combo box, not a separate input.** Materials are discrete SKUs (a weight that exists for gloss may not exist for kraft), so a separate weight field would create invalid combinations. Instead weight is searchable (`300`, `250-350`, `>=300`) and the most common tweak — same paper, different weight — gets quick pills under the box.
- Filtering logic is passed into `ComboBox` as `search: String => List[Item]`, keeping the component generic for reuse (e.g. backoffice pickers).
- Diacritic folding is char-for-char so highlight indices map back to the original text (no `java.text.Normalizer`, which Scala.js lacks).

## Issues Encountered

- First diacritics table zipped two strings of unequal length → off-by-one mapping (`ř`→`o`); caught by the CS search test, replaced with explicit groups.
- Local Mill picked JDK 17 → `UnsupportedClassVersionError`; see existing troubleshooting entry (use JDK 25).

## Follow-up Items

- [ ] Scroll the keyboard-active option into view (kyo-ui exposes no scroll-into-view API yet; long lists rely on typing to narrow).
- [ ] Consider reusing `ComboBox` in backoffice catalog/pricing editors.
