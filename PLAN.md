# Product Builder — Goals & Roadmap

## Vision

A custom product builder for the printing industry that lets customers configure products from categories, materials, finishes, and specifications — with declarative compatibility rules ensuring only valid combinations are possible. Includes a photo calendar editor for designing custom calendar pages.

## Tech Stack

- **Scala 3.3.3** with DDD principles
- **ZIO 2.x** for effects (infrastructure layer)
- **ZIO Prelude** for validation with error accumulation
- **Scala.js + Laminar** for the interactive web UI (cross-compiled domain)

## Key Design Decisions

1. **Rules are data, not code** — `CompatibilityRule` is a sealed ADT interpreted by a rule engine. New materials/finishes can be added without changing business logic.
2. **ZIO Prelude `Validation`** for error accumulation — all errors are collected, not short-circuited.
3. **No effects in domain layer** — all domain functions return `Validation[...]`, never `ZIO[...]`. This keeps the domain Scala.js-compatible.
4. **Specification pattern as ADT** — `ConfigurationPredicate` with And/Or/Not is inspectable and serializable, unlike `A => Boolean`.
5. **Progressive disclosure** — `CatalogQueryService` pre-filters valid options so the UI only shows compatible choices.

---

## Completed — Phase 1: Domain Model

The core domain layer is fully implemented and tested:

- **Value objects** — Opaque type IDs (`CategoryId`, `MaterialId`, `FinishId`, `PrintingMethodId`, `ConfigurationId`, `BasketId`) with smart constructors, `PaperWeight`, `Quantity`, `Dimension`
- **Domain model** — `Material` (family, weight, properties), `Finish` (type, side), `ProductCategory` (allowed materials/finishes/printing methods, required specs), `ProductSpecification` (8 spec kinds), `ProductConfiguration` (aggregate root), `ProductCatalog`, `PrintingMethod`
- **Compatibility rules** — Sealed ADT with 12 rule variants, spec predicates, configuration predicates with boolean algebra (And/Or/Not)
- **Validation** — Two-layer pipeline (structural checks then rule evaluation), rich error ADT with 19 error types and localized messages
- **Services** — `ConfigurationBuilder` (resolve + validate + build), `CatalogQueryService` (filtered queries for UI guidance)
- **Sample data** — 7 categories (Business Cards, Flyers, Brochures, Banners, Packaging, Booklets, Calendars), 9 materials, 14 finishes, 4 printing methods, 24 compatibility rules

## Completed — Phase 2: Pricing

Declarative pricing layer following the same rules-as-data pattern:

- **Money** — Opaque type over `BigDecimal` (never `Double`), rounding to 2dp with `HALF_UP`
- **PricingRule** — Sealed enum with 7 variants: `MaterialBasePrice`, `MaterialAreaPrice`, `FinishSurcharge`, `FinishTypeSurcharge`, `PrintingProcessSurcharge`, `CategorySurcharge`, `QuantityTier`
- **PriceCalculator** — Pure interpreter: config + pricelist → `Validation[PricingError, PriceBreakdown]`. Supports area-based pricing (large-format), ID-over-type finish surcharge precedence, and quantity tier discounts
- **PriceBreakdown** — Detailed output with line items for material, finishes, process/category surcharges, subtotal, multiplier, and rounded total
- **Sample data** — Prices for all 9 materials (flat + area-based for vinyl), surcharges for key finishes, letterpress process surcharge, 4 quantity tiers (1.0×/0.90×/0.80×/0.70×)
- **Documentation** — See `docs/pricing.md` for detailed explanation with worked examples

---

## Completed — Phase 3: UI with Scala.js + Laminar

Interactive web UI for configuring print products:

- **Cross-compilation** — Domain model compiled to JavaScript with Scala.js (Scala 3.3.3 for compatibility)
- **Laminar framework** — Reactive UI with Signal/Var primitives for state management
- **Step-by-step wizard** — 5-step configuration flow: category → material → printing method → finishes → specifications
- **Progressive disclosure** — `CatalogQueryService` filters options in real-time; disabled states guide users through valid paths only
- **Live validation** — Immediate feedback on incompatible selections with detailed error messages
- **Price calculation** — Real-time pricing with full breakdown (material, finishes, surcharges, quantity discounts)
- **Modern design** — Responsive layout with gradient purple theme, sticky price preview, info boxes, and visual feedback
- **Sample catalog** — Pre-loaded with 7 categories, 9 materials, 14 finishes, 4 printing methods, 24 compatibility rules, and full pricelist
- **Deployment** — GitHub Actions workflow builds Scala.js and deploys to GitHub Pages

See `docs/ui-guide.md` for build and run instructions.

---

## Completed — Phase 4: Internationalization (i18n)

Multi-language support across domain model and UI:

- **`Language` enum** (`En`, `Cs`) and **`LocalizedString` opaque type** over `Map[Language, String]` with English fallback
- `name` field on `Material`, `Finish`, `ProductCategory`, `PrintingMethod` uses `LocalizedString`
- `ConfigurationError.message(lang)` and `PricingError.message(lang)` with Czech translations for all variants
- `PriceCalculator.calculate` accepts optional `lang` parameter for localized line item labels
- UI language selector with browser language detection and `localStorage` persistence
- All UI components localized: form headings, labels, placeholders, validation messages, price preview

---

## Completed — Phase 5: Extended Catalog

Expanded sample data based on printing domain analysis:

- **Calendars** category — Wire-bound/spiral-only with 12–28 pages constraint
- **New materials** — Yupo Synthetic 200μm, Cotton Paper 300gsm, Coated Silk 250gsm, Adhesive Stock 100gsm
- **New finishes** — Soft Touch Coating, Aqueous Coating, Debossing, Scoring, Perforation, Round Corners, Grommets
- **Rules** expanded from 10 → 24 with material-finish incompatibilities, weight constraints, and calendar-specific rules
- **Pricing** updated for all new materials and finishes

---

## Completed — Phase 6: Shopping Basket

Complete shopping basket functionality:

- **Domain model** — `BasketId` opaque type, `Basket` containing `BasketItem`s (configuration + quantity + pre-calculated pricing)
- **`BasketService`** — Pure functional operations via `Validation[BasketError, _]`: `addItem`, `removeItem`, `updateQuantity`, `calculateTotal`, `clear`
- **`BasketError`** ADT with localized messages (EN/CS): `InvalidQuantity`, `ConfigurationNotFound`, `PricingFailed`
- **UI** — Basket item list with quantity management, remove/clear buttons, total calculation display, success/error messaging
- **Integration** — "Add to Basket" from configuration form, reactive basket state in ViewModel

---

## Completed — Phase 7: Photo Calendar Builder

Interactive calendar page editor:

- **`CanvasElement` sealed trait** with `PhotoElement`, `TextElement`, `ShapeElement`, `ClipartElement` variants
- **12-page editor** — One page per month, each with its own elements and background
- **Element manipulation** — Drag, resize, rotate for all element types; z-ordering, duplication, deletion
- **Unified element list** — `ElementListEditor` replaces separate photo/text panels; type-dispatched form editors
- **Text formatting** — Bold, italic, alignment (left/center/right)
- **Shape support** — Lines and rectangles with stroke and fill colors
- **Page backgrounds** — Solid color picker or image upload
- **Template text fields** — Locked month/day labels separate from user elements
- **Client-side routing** — `AppRouter` with navigation between Product Builder and Calendar Builder views
- **Global language selector** — Positioned at root level, applies to both views

---

## Testing

**99 passing tests** across 5 test suites:

- **ConfigurationBuilderSpec** — 34 tests: valid configs, error accumulation, weight rules, finish dependencies, printing process requirements
- **CatalogQueryServiceSpec** — 17 tests: material/finish/spec filtering, progressive disclosure
- **PriceCalculatorSpec** — 14 tests: valid breakdowns, area-based calculation, tier discounts, multiple finishes, precedence, error cases
- **LocalizationSpec** — 17 tests: `LocalizedString` behavior, Czech translations, error message localization, backward compatibility
- **BasketServiceSpec** — 17 tests: add/remove/update items, quantity validation, total calculation

---

## Roadmap

### Persistence

- Add ZIO-based repository interfaces in the domain layer
- Implement persistence adapters (e.g. JSON file store or database)
- Store and retrieve product catalogs, rulesets, pricelists, and saved configurations
- Support catalog versioning for rule/data evolution

### Production Readiness

- API layer (ZIO HTTP or similar) for server-side validation and pricing
- Admin interface for managing catalogs, rules, and pricelists without code changes
- Order submission workflow
- PDF proof generation
- Integration with print production systems
