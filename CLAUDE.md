# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Test Commands

```bash
sbt compile          # Compile all sources (domain JVM + JS, UI)
sbt test             # Run all tests (186 tests, 5 suites)
sbt domainJVM/test   # Run domain tests only (faster)
sbt ui/compile       # Compile UI module only
sbt ui/fastLinkJS    # Build UI JavaScript (development)
sbt ui/fullLinkJS    # Build UI JavaScript (production, optimized)
sbt "testOnly mpbuilder.domain.ConfigurationBuilderSpec"  # Run a single test suite
sbt "testOnly * -- -v"  # Verbose test output
```

- Scala 3.3.3, JDK 11+ (17 recommended), sbt
- Dependencies: ZIO 2.1.16, ZIO Prelude 1.0.0-RC39, ZIO Test, Laminar 17.2.0

## Architecture

This is a DDD (Domain-Driven Design) product configuration system for the printing industry. All domain logic is **pure** — uses `Validation[E, A]` (ZIO Prelude), never `ZIO` effects. This keeps the domain Scala.js-compatible.

### Package layout: `mpbuilder.domain`

- **`model/`** — Value objects (opaque type IDs with smart constructors), entities, enums. `ProductConfiguration` is the aggregate root. `InkType`/`InkSetup`/`InkConfiguration` model per-side ink setup (e.g., 4/0, 4/4). `SpecKind.InkConfig` + `SpecValue.InkConfigSpec` replace the former `ColorMode`.
- **`rules/`** — `CompatibilityRule` sealed ADT (12 rule variants), `SpecPredicate` and `ConfigurationPredicate` (boolean algebra with And/Or/Not). Rules are data, not code.
- **`validation/`** — `ConfigurationError` ADT with exhaustive `message` match. `RuleEvaluator` interprets rules. `ConfigurationValidator` runs two-layer validation: structural checks first, then rule evaluation.
- **`service/`** — `ConfigurationBuilder` resolves IDs from catalog and orchestrates validation. `CatalogQueryService` pre-filters compatible options for UI progressive disclosure.
- **`pricing/`** — `Money` opaque type (BigDecimal, never Double). `PricingRule` sealed enum (17 variants: base/area/sheet material prices, finish/type/process/category/fold/binding surcharges, quantity/sheet tiers, `InkConfigurationFactor`, cutting surcharge, finish/type/fold/binding setup fees, minimum order price). `PriceCalculator` interprets rules purely. `PricingError` ADT with exhaustive `message` match. `PriceBreakdown` output with `ComponentBreakdown` per component (including optional `inkConfigLine`, `cuttingLine`, `sheetsUsed`), plus `setupFees`, `minimumApplied`, `foldSurcharge`, and `bindingSurcharge` fields.
- **`sample/`** — `SampleCatalog` (11 categories, 13 materials, 16 finishes, 4 printing methods), `SampleRules` (29 rules), and `SamplePricelist` (USD + CZK base + CZK sheet pricelists; full finish/fold/binding surcharges, setup fees, and minimum order price on CZK pricelists). Used by tests.

### Package layout: `mpbuilder.ui.calendar` (Visual Editor)

- **`CalendarModel.scala`** — All visual editor types: `VisualProductType` enum (5 product types), `ProductFormat` case class with physical mm dimensions (10 formats), `CanvasElement` sealed trait with `PhotoElement`/`TextElement`/`ShapeElement`/`ClipartElement`, `CalendarTemplate` with `TemplateTextField` and `CalendarPage`, `CalendarState` aggregate.
- **`CalendarViewModel.scala`** — Reactive state management with `Var[CalendarState]`. Element CRUD, selection, z-ordering, duplication. Product type/format switching. Background/template operations.
- **`CalendarBuilderApp.scala`** — Main visual editor view with product type/format selectors, sidebar tabs, and layout.
- **`components/`** — `CalendarPageCanvas` (interactive canvas with drag/resize/rotate), `ElementListEditor` (element management with type-specific form editors), `BackgroundEditor` (page background settings), `PageNavigation` (horizontal scrollable page strip).

### Package layout: `mpbuilder.uikit.form` (UI Framework — Form Components)

- **`FormComponents.scala`** — Generic ADT-derived form components: `textField`, `numberField`, `optionalNumberField`, `enumSelect`, `enumSelectRequired`, `enumCheckboxSet`, `idCheckboxSet`, `actionButton`, `dangerButton`, `sectionHeader`. Domain-agnostic — works with any Scala 3 enum or opaque type.

### Package layout: `mpbuilder.ui.catalog` (Catalog Editor)

- **`CatalogEditorApp.scala`** — Main editor view with ManufacturingApp-style dark sidebar navigation (7 sections). Sample catalog + CZK pricelist loaded by default.
- **`CatalogEditorModel.scala`** — `CatalogSection` enum, `EditState` ADT, `CatalogEditorState` aggregate.
- **`CatalogEditorViewModel.scala`** — Reactive state management with CRUD for all catalog entities, JSON import/export via `DomainCodecs`.
- **`FormComponents.scala`** — Re-exports generic components from ui-framework and adds domain-specific: `localizedStringEditor`, `moneyField`.
- **`views/`** — Per-entity editor views using `SplitTableView`: `CategoryEditorView`, `MaterialEditorView`, `FinishEditorView`, `PrintingMethodEditorView`, `RulesEditorView`, `PricelistEditorView`, `ExportImportView`.

### Package layout: `mpbuilder.domain.codec`

- **`DomainCodecs.scala`** — JSON codecs (zio-json) for all domain types: IDs, enums, value objects, entities, rules (14 variants), predicates (recursive ADT), pricing rules (18 variants), catalog, pricelist. Includes `CatalogExport` container type.

### Key data flow

`ConfigurationRequest` → resolve IDs from `ProductCatalog` → `ConfigurationValidator.validate` (structural + rules) → `ProductConfiguration` → `PriceCalculator.calculate` (config + pricelist) → `PriceBreakdown`

### Validation approach

ZIO Prelude `Validation` accumulates all errors (not short-circuit). Structural validation (material allowed for category, required specs present) runs first via `flatMap`, then rule evaluation runs all rules and collects errors via `zipRight`.

### Adding new rule types

1. Add variant to `CompatibilityRule` enum
2. Add corresponding error to `ConfigurationError` enum (with `message` match arm)
3. Add evaluation arm in `RuleEvaluator.evaluate`
4. Handle in `CatalogQueryService.compatibleFinishes` (explicit match — no wildcard)
5. Add sample rules in `SampleRules` and tests

### Pricing approach

`PriceCalculator.calculate` is pure — returns `Validation[PricingError, PriceBreakdown]`. Steps: extract quantity → resolve material unit price (area > sheet > base precedence) → compute `sheetsUsed` per component (for sheet-priced materials) → apply `InkConfigurationFactor` (multiplier on material cost by front/back color counts; 1.0 = no line item) → finish surcharges (ID-level overrides type-level) → process/category surcharges → fold type surcharge → binding method surcharge → sum subtotal → apply best tier multiplier (`SheetQuantityTier` if totalSheets > 0 and rules exist, else `QuantityTier` fallback) → collect setup fees (finish/fold/binding; ID-level overrides type-level; same finish ID on multiple components charged once) → add setup fees to discounted subtotal → apply `MinimumOrderPrice` floor if configured → round total to 2dp.

### Adding new pricing rule types

1. Add variant to `PricingRule` enum
2. Add corresponding error to `PricingError` enum (with `message` match arm) if needed
3. Add handling in `PriceCalculator.calculate`
4. Add sample rules in `SamplePricelist` and tests

### Adding new visual product types

1. Add variant to `VisualProductType` enum in `CalendarModel.scala`
2. Add `ProductFormat` definitions and update `formatsFor` / `defaultFor`
3. Add factory method in `CalendarState` companion (`createXxxPages`)
4. Update `CalendarState.create` match and `CalendarState.updateLanguage` match
5. Update `defaultPageCount` match
6. Update product type selector options in `CalendarBuilderApp.scala`
7. Update `docs/visual-product-types.md`

### Important conventions

- All enum matches must be exhaustive — no wildcards in `CompatibilityRule`, `ConfigurationError`, or `PricingError` pattern matches
- `Money` is an opaque type over `BigDecimal` — never use `Double` for monetary values
- `ProductCategory.allowedPrintingMethodIds` empty set means "no restriction" (all methods allowed)
- `FinishCategory` is derived from `FinishType` via extension method `finishCategory`, not stored on `Finish`
- `InkConfiguration` models per-side ink setup: `InkSetup(inkType, colorCount)` for front and back. Presets: `cmyk4_4`, `cmyk4_0`, `cmyk4_1`, `mono1_0`, `mono1_1`. Structural validation checks `maxColorCount` against `PrintingMethod.maxColorCount`
- `CatalogQueryService.compatibleFinishes` takes `printingMethodId: Option[PrintingMethodId]` — `None` means "not yet selected, don't filter by method"
- Finish pricing precedence: `FinishSurcharge` (by ID) overrides `FinishTypeSurcharge` (by type); same for setup fees: `FinishSetupFee` overrides `FinishTypeSetupFee`
- Setup fees are added after the quantity tier multiplier — they are never volume-discounted. Same finish ID on multiple components is charged once.
- `MinimumOrderPrice` is applied last (after setup fees), raising the total to the floor only when needed. `minimumApplied` stores the pre-floor amount for UI display.
- Template image placeholders use `PhotoElement(imageData = "")` — they're fully interactive `CanvasElement`s, not static template fields
- The format `<select>` in `CalendarBuilderApp` is re-created on each state change (`child <-- ...`), so options must have `selected` attribute set explicitly
