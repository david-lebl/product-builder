# 2026-05-03 — Ink Config Single-Sided Constraint & Sticker Pricing Fix

**PR:** copilot/check-prices-for-print-methods
**Author:** copilot agent
**Type:** bugfix / feature

## Summary

Fixed three related issues with printing-method-dependent ink configuration and pricing:

1. **Missing ink prices for solvent and Epson 8-color in `pricelistCzkSheet`** — the UI-facing pricelist was missing `InkConfigurationAreaPrice` rules for `solventInkjetId` and `epson8ColorId`. Only UV inkjet had area ink prices; solvent and Epson produced no ink line in the price breakdown and caused zero ink cost for sticker products. Also updated UV inkjet prices from the stale placeholder values (22/30/45 CZK/m²) to the correct production values (360/480/720 CZK/m²) matching `pricelistCzk`.

2. **Large-format inkjet single-sided constraint** — large-format inkjet methods (UV curable, solvent, extended-gamut/Epson) are inherently single-sided (one face per pass). Added a `IsSingleSided` predicate to `ConfigurationPredicate` and a `TechnologyConstraint` in `SampleRules` that rejects double-sided ink configs (4/4, 4/1, 1/1) when a large-format inkjet method is selected.

3. **UI ink config selector filtering** — `InkConfigSelector` now shows only the valid subset of ink config options based on the selected method: UV inkjet → 4/0, 1/0, 4/0+W; Solvent/Latex → 4/0, 1/0; sheet-fed methods → all options. When switching to a large-format method, `selectPrintingMethod` automatically resets any double-sided config to 4/0.

## Changes Made

### Domain
- `modules/domain/src/main/scala/mpbuilder/domain/model/printingmethod.scala` — Added `isLargeFormatInkjet` extension method on `PrintingProcessType` companion object; returns `true` for UVCurableInkjet, SolventInkjet, LatexInkjet.
- `modules/domain/src/main/scala/mpbuilder/domain/rules/predicates.scala` — Added `IsSingleSided` case to `ConfigurationPredicate` enum; evaluates to true when all components have `isSingleSided == true`.
- `modules/domain/src/main/scala/mpbuilder/domain/validation/RuleEvaluator.scala` — Added handler for `ConfigurationPredicate.IsSingleSided`.
- `modules/domain/src/main/scala/mpbuilder/domain/sample/SamplePricelist.scala` — `pricelistCzkSheet`: updated UV inkjet area prices (22→360, 30→480, 45→720, 6→100, 10→160 CZK/m²); added solvent inkjet area prices (300/400/600/80/140 CZK/m²); added Epson 8-color area prices (420/560/840/110/190 CZK/m²).
- `modules/domain/src/main/scala/mpbuilder/domain/sample/SampleRules.scala` — Added private `isLargeFormatInkjet: ConfigurationPredicate` constant to eliminate nested `Or` duplication; refactored two existing sticker constraints to use it; added new `TechnologyConstraint` enforcing single-sided ink config for large-format inkjet methods.

### UI
- `modules/ui/src/main/scala/mpbuilder/ui/productbuilder/ProductBuilderViewModel.scala` — Added `selectedPrintingMethod: Signal[Option[PrintingMethod]]`; updated `selectPrintingMethod` to use `isLargeFormatInkjet` extension method and auto-sanitise ink configs when switching to a large-format method.
- `modules/ui/src/main/scala/mpbuilder/ui/productbuilder/components/InkConfigSelector.scala` — Renamed `presets` to `allPresets`; added `presetsForMethod()` filtering function; wired `selectedPrintingMethod` signal into the children signal so the dropdown updates reactively.

### Tests
- `modules/domain/src/test/scala/mpbuilder/domain/ConfigurationBuilderSpec.scala` — Fixed two banner tests that used `cmyk4_4` with UV inkjet (changed to `cmyk4_0`); added 8 new tests in the "large-format inkjet single-sided constraint" suite.
- `modules/domain/src/test/scala/mpbuilder/domain/PriceCalculatorSpec.scala` — Added 3 new tests in "pricelistCzkSheet area pricing for large-format inkjet" suite: UV inkjet (360 CZK/m²), solvent inkjet (300 CZK/m²), Epson 8-color (420 CZK/m²).

## Decisions & Rationale

- **No new `HasAnyPrintingProcess` predicate**: Instead of extending the predicate enum with a set-accepting variant (which would require codec changes and more test coverage), a private `isLargeFormatInkjet: ConfigurationPredicate` constant was defined in `SampleRules` and reused across all three constraints. This achieves readability without new public API surface.

- **Extension method on `PrintingProcessType`**: Placing `isLargeFormatInkjet` as an extension method on the domain type (not a UI utility) ensures consistent classification is shared between domain rules and UI code without duplication.

- **UV inkjet price update in `pricelistCzkSheet`**: The 22/30/45 CZK/m² values were placeholder values set when the CZK sheet pricelist was first introduced for sheet-fed products. They were not updated when large-format inkjet methods were added for stickers/banners. The correct values (360/480/720) match `pricelistCzk`.

- **Auto-reset on method switch**: When switching from a sheet-fed method (digital, offset) to a large-format inkjet method, any double-sided ink configuration (4/4, 4/1, 1/1) is automatically reset to 4/0, and any white-ink configuration (4/0+W) is reset to 4/0 if the new method doesn't support white ink (i.e., not UV inkjet). This prevents the user from reaching an invalid state.

- **`4/0+W` remains available for UV inkjet only**: The existing white-ink constraint (requires transparent material OR UV inkjet) combined with the new UI filtering correctly restricts the `4/0+W` option.

## Issues Encountered

- Two existing banner tests used `InkConfiguration.cmyk4_4` (double-sided) with `uvInkjetId`, which is now correctly rejected. Updated both to `cmyk4_0`. These tests were using an unrealistic ink config for a single-sided banner product and were masked by the missing constraint.

- A new test for "digital + 4/4 double-sided → valid" initially failed with `MissingRequiredSpec(Orientation)` because the flyers category requires an orientation spec. Added `SpecValue.OrientationSpec(Orientation.Portrait)` to fix.

## Follow-up Items

- [ ] Consider `4/0+W` pricing in `pricelistCzkSheet` — currently it matches the `4/1` area price rule (frontColorCount=4, backColorCount=1) because white underlay has `colorCount=1`. This may need a dedicated price rule if the cost structure differs from grayscale-back printing.
- [ ] Banner category may benefit from explicitly listing only single-sided methods in its `allowedPrintingMethodIds` to prevent future confusion.
- [ ] The `4/0+W` UV inkjet pricing at 480 CZK/m² (pricelistCzkSheet) is charged as `4/1` — verify this is the intended pricing for white-underlay stickers.
