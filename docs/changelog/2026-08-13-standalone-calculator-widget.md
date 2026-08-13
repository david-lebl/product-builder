# 2026-08-13 — Standalone Embeddable Price Calculator

**PR:** N/A
**Author:** agent (Claude Code)
**Type:** feature | refactoring

## Summary

Split the product configurator out of the SPA into a reusable module, and built a second,
much smaller app on top of it: a price calculator a print shop can embed in any website.

The calculator keeps the whole configuration + pricing experience and the basket, but drops
manufacturing, the product catalog, the visual editor, customer management, login and the
5-step checkout wizard. The basket's primary action sends the order by `mailto:` instead of
proceeding to checkout.

Crucially, the widget **no longer promises production dates**. The SPA derives concrete
completion timestamps ("Tomorrow, 11:00") from simulated shop-floor queue data; a widget
running on a customer's website has no connection to a real production queue, so it shows
indicative ranges plus an explicit disclaimer instead.

The configurator is *shared*, not forked — both apps compile the same sources, so they
cannot drift.

## Changes Made

### New modules

- `modules/ui-productbuilder/` — the shared configurator, moved wholesale from
  `modules/ui/src/main/scala/mpbuilder/ui/productbuilder/` (14 files). **The Scala package
  is unchanged** (`mpbuilder.ui.productbuilder`), so every consumer in `ui` — `CheckoutView`,
  `CustomerPortalView`, `LoginWidget`, `OrderHistoryView`, `ProductCatalogApp` and the six
  `visualeditor/*` files — kept compiling untouched.
- `modules/ui-calculator/` — the standalone widget:
  - `CalculatorWidget.scala` — `@JSExportTopLevel("MPCalculator")` with `mount(selector, config)`;
    config keys `lang` and `orderEmail`. Linked with `scalaJSUseMainModuleInitializer = false`,
    so nothing runs until the host page calls `mount`.
  - `CalculatorEnvironment.scala` — the standalone `BuilderEnvironment`.
  - `CalculatorApp.scala` — thin shell (language switch + basket button) over `ProductBuilderApp`.
  - `resources/{index.html, embed-example.html, calculator.css}`, `css/{reset-scoped,shell}.css`,
    `build-css.sh`.

### The `BuilderEnvironment` seam

New `BuilderEnvironment.scala` in `ui-productbuilder` — a case class of host-supplied values
installed once via `BuilderEnvironment.init(...)`, matching the codebase's existing singleton
style (`AppRouter`, `ProductBuilderViewModel`, `ManufacturingViewModel`):

| Field | Full SPA | Calculator |
|---|---|---|
| `pricingContext` | queue surge + busy periods from simulated stations | `PricingContext.default` |
| `completionText` | `CompletionEstimator.formatEarliest` | `None` → indicative ranges |
| `expressAvailable` | `UtilisationCalculator.isExpressAvailable` | `Val(true)` |
| `artwork` | `Some(VisualEditorArtwork)` | `None` → section not rendered |
| `basketOpen` | `AppRouter.basketOpen` | own `Var` |
| `basketPrimaryAction` | "Proceed to Checkout →" | "Send order by e-mail ✉" |
| `orderEmail` | `""` (blank To) | host-configured |

Consumers rewired: `ProductBuilderApp`, `ConfigurationForm`, `BasketView`,
`ProductBuilderViewModel`, `SpecificationForm`.

### Moved out of the shared module into `ui`

- `modules/ui/.../FullAppEnvironment.scala` — simulated station utilisation + queue state,
  `deriveStepTypes`, completion estimation, surge pricing context, Express availability gate.
- `modules/ui/.../VisualEditorArtwork.scala` — the `ArtworkMode` ADT, the artwork form section
  and the per-basket-item artwork display, now behind the `ArtworkIntegration` trait. `artworkMode`
  and `basketItemArtwork` were removed from `BuilderState` and live in this object's own `Var`s.
- `Main.scala` calls `BuilderEnvironment.init(FullAppEnvironment.environment)` before rendering.

### Behaviour changes (both apps)

- Speed-tier price labels (`+35%` / `base price` / `−15%`) are now derived from the active
  pricelist's `ManufacturingSpeedSurcharge` rules instead of being hardcoded, so an embedding
  shop with different multipliers gets truthful labels.
- New `.speed-tier-disclaimer`, shown only when no tier can offer a concrete date.
- `EmailOrderModal` gained a basket mode (`openForBasket()`), a phone field, a configurable
  `mailto:` recipient, and wording stating the message is a request rather than a binding order.
- Ink configuration in the order e-mail now uses `InkConfiguration.notation` ("4/0") instead of
  the raw case-class `toString` ("InkSetup(CMYK,4)+InkSetup(None,0)"). This also fixes the
  pre-existing single-configuration path.
- `formatSpec` in `EmailOrderModal` now handles `BleedSpec` (see Issues below).

### Build

`build.mill` and `build.sbt` both gained `ui-productbuilder` and `ui-calculator`;
`ui` now depends on `ui-productbuilder`.

## Decisions & Rationale

- **Shared module over a fork.** A copied-and-stripped calculator would have been faster but
  would drift from the SPA within a release or two. Keeping the package name identical made
  the extraction nearly free on the consumer side.
- **A mutable global environment, not constructor injection.** Threading a config object through
  all 14 component files would have been a much larger diff for no benefit in a codebase that is
  already built on singletons.
- **Both embedding modes shipped.** The mount-div API is flexible; the iframe is the escape hatch
  when the host page's CSS collides. One JS artifact serves both — `index.html` calls `mount`
  itself.
- **Express kept, dates dropped.** The tier is a real price difference and worth keeping; only
  the *date* was unsupportable standalone. Pricing needed no change at all — `PriceCalculator`
  already took a `PricingContext`, and express pricing and express dates were already decoupled.
- **Tier restrictions kept in the calculator.** Per-category quantity caps and the
  Perfect/Case-binding blocks are product constraints, not queue state, so they still apply.
- **`mailto:` only, contact fields only.** Chosen over a POST endpoint so the widget needs no
  backend. Accepted limitation: some clients truncate long `mailto:` URLs (see Follow-up).

## Issues Encountered

- **Scoped reset ordering.** Putting the `.mp-calculator`-scoped reset at the *end* of the
  bundled stylesheet stripped padding from every control in the widget, because
  `.mp-calculator *` (0,1,0) beats the bare `button`/`input` rules in `utilities.css` (0,0,1).
  Fixed by prepending it, mirroring `reset.css`'s position in the SPA. Added to
  `docs/troubleshooting.md`.
- **`formatSpec` non-exhaustive.** The basket renderer feeds it every spec from a
  `ProductConfiguration`, including `BleedSpec`, which the existing match did not handle — a
  latent `MatchError`. Caught from the pre-existing compiler warning and fixed.

## Verification

- `mill domain.jvm.test` — 117 tests pass, domain untouched.
- `mill __.compile` — all modules.
- Headless-Chrome drive of the **calculator**: configure Business Cards → price 290.00 Kč,
  add two different products → basket total 1423.75 Kč → "Send order by e-mail" renders both
  items, quantities and grand total, addressed to the configured `orderEmail`. Tier cards show
  ranges and the disclaimer, never a timestamp. No artwork section. No console errors.
- Headless-Chrome regression drive of the **SPA**: concrete dates still shown
  ("Tomorrow, 11:00"), disclaimer present but hidden, artwork section with both options,
  basket artwork line, "Proceed to Checkout →" opens the 5-step wizard. No console errors.
- Bundle sizes: calculator `fastLinkJS` 7.3 MB vs SPA 17.3 MB; calculator `fullLinkJS` 1.6 MB.
  Verified `ManufacturingApp`, `CatalogEditorApp`, `CustomerManagementApp`, `VisualEditorApp`,
  `CheckoutView`, `ProductCatalogApp`, `EditorBridge` and `AppRouter` are all absent from the
  calculator bundle.

## Follow-up Items

- [ ] **Bundle slimming.** `loginState` / `checkoutInfo` remain on `BuilderState`, keeping
      `LoginService`, `SampleCustomers` and `CustomerPricelistResolver` reachable in the widget.
      Move them into `ui`-local view models if 1.6 MB proves too large.
- [ ] **CSS namespacing.** Class names (`.card`, `.form-section`, `.price-section`) are generic
      and will collide on some host pages in mount-div mode. A `.mpc-` prefix pass would fix it;
      the iframe is the workaround until then.
- [ ] **POST transport** for orders, replacing `mailto:` and its length limit.
- [ ] **Runtime catalog/pricelist loading** via the existing `codec/DomainCodecs.scala` zio-json
      codecs, so each embedding shop ships its own pricing rather than `SampleCatalog` /
      `SamplePricelist.pricelistCzkSheet`.
- [ ] `calculator.css` is generated by a shell script run by hand — fold it into the Mill build.
- [ ] `docs/ideas/` is still missing from `docs/INDEX.md` (pre-existing gap).
