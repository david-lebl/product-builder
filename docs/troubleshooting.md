# Troubleshooting

> Known issues, common problems from agent sessions, and their solutions. This document is a living knowledge base — agents and developers should add entries when encountering and resolving issues.

---

## Build & Compilation

### Mill StackOverflow on `DeriveJsonCodec.gen[PricingRule]`

**Symptom:** JVM StackOverflow error when compiling with Mill, specifically when the Scala 3 compiler expands macros for `PricingRule` (19+ enum variants).

**Solution:** The file `.mill-jvm-opts` must contain `-Xss8m` to increase the JVM stack size. This file is committed to the repo and must not be removed or added to `.gitignore`.

**Files:** `.mill-jvm-opts`, `.gitignore`

---

### sbt not pre-installed in CI/sandbox environments

**Symptom:** `sbt: command not found` when trying to build with sbt in a fresh environment.

**Solution:**
```bash
curl -sL https://github.com/sbt/sbt/releases/download/v1.12.3/sbt-1.12.3.tgz -o /tmp/sbt.tgz
tar xzf /tmp/sbt.tgz -C /tmp
export PATH="/tmp/sbt/bin:$PATH"
```

---

### Mill not pre-installed in CI/sandbox environments

**Symptom:** `mill: command not found` in CI or sandbox.

**Solution:** CI installs Mill 1.1.3 via:
```bash
curl -fL "https://repo1.maven.org/maven2/com/lihaoyi/mill-dist/1.1.3/mill-dist-1.1.3-mill.sh" -o /usr/local/bin/mill
chmod +x /usr/local/bin/mill
```
Note: Use the `-mill.sh` suffix for Linux (not `.exe`).

**Files:** `.github/workflows/ci.yml`

---

### Mill SSL issues in Copilot Agent environment

**Symptom:** `javax.net.ssl.SSLHandshakeException: PKIX path building failed` when Mill (via Coursier) tries to download `mill-runner-daemon` or other JARs from Maven Central. The native Mill binary downloads fine, but dependency resolution fails.

**Cause:** The default JVM in the Copilot Agent runner lacks the root CA certificates required to validate Maven Central's TLS certificate. Installing Temurin JDK 17 (via `actions/setup-java`) provides an up-to-date CA bundle that resolves this.

**Solution:** The repository ships `.github/workflows/copilot-setup-steps.yml` which the Copilot Agent runs automatically before starting work. It installs Temurin JDK 17 and pre-warms the Coursier cache so all subsequent `./mill` commands work without network access.

If you need to fix this manually in a fresh shell (e.g. outside the Copilot Agent):
```bash
# Option A — pass the JVM flag that disables SSL verification (dev-only, not for CI)
JAVA_OPTS="-Djavax.net.ssl.trustStore=/etc/ssl/certs/java/cacerts" ./mill __.compile

# Option B — install Temurin JDK and re-run
sudo apt-get install -y temurin-17-jdk   # or use SDKMAN / jabba
./mill __.compile
```

**Files:** `.github/workflows/copilot-setup-steps.yml`

---

## Domain & Pricing

### Price doesn't change when selecting manufacturing speed

**Symptom:** Selecting different manufacturing speeds in the UI has no effect on the displayed price.

**Cause:** `ManufacturingSpeedSurcharge` pricing rules must be present in ALL pricelists (USD, CZK, CZK Sheet). The UI uses `SamplePricelist.pricelistCzkSheet`. Without these rules, `PriceCalculator` returns `None` for speed surcharge.

**Solution:** Ensure `ManufacturingSpeedSurcharge` rules are added to all three pricelists in `SamplePricelist.scala`.

**Files:** `modules/domain/src/main/scala/mpbuilder/domain/sample/SamplePricelist.scala`

---

### Sticker ink cost is silently zero for certain material/method combinations

**Symptom:** A sticker configuration calculates correctly for material cost but produces no ink line and an understated total price. No error is returned — the calculation simply omits the ink component.

**Cause:** `PriceCalculator` selects the ink-pricing strategy based on the material pricing rule:
- **Base-priced material** (`MaterialBasePrice`) → `InkPricingBasis.SheetOrUnit` → looks for `InkConfigurationSheetPrice`
- **Area-priced material** (`MaterialAreaPrice` / `MaterialAreaTier`) → `InkPricingBasis.Area` → looks for `InkConfigurationAreaPrice`

Large-format inkjet methods (UV inkjet, solvent inkjet, Epson 8-color) only have `InkConfigurationAreaPrice` rules. Digital and offset only have `InkConfigurationSheetPrice` rules. When a base-priced paper material (adhesiveStock, Yupo) is combined with a large-format inkjet method, or when an area-priced vinyl material is combined with digital/offset, the ink rule is not found and `inkConfigLine` is `None` (ink cost = 0, silently).

**Solution:** `SampleRules` now contains two `ConfigurationConstraint` rules for the stickers category that prevent these mismatched combinations at validation time:
1. Vinyl-family materials require a large-format inkjet process (UV / solvent / extended gamut).
2. Large-format inkjet processes require a vinyl-family material.

If validation is bypassed and `PriceCalculator` is called directly, the bug still exists — the ink line will be absent. See also the BUG-documenting tests in `PriceCalculatorSpec.scala` (`"BUG: adhesiveStock sticker + UV inkjet…"` and `"BUG: vinyl sticker + digital…"`).

A more robust fix would be for `PriceCalculator` to return a `PricingError` when the ink-pricing basis and the print method's available rules are incompatible (e.g. no `InkConfigurationAreaPrice` found for an area-priced material), rather than silently returning `None`. This is tracked as a follow-up item.

**Files:**
- `modules/domain/src/main/scala/mpbuilder/domain/sample/SampleRules.scala` (fix)
- `modules/domain/src/test/scala/mpbuilder/domain/PriceCalculatorSpec.scala` (bug documentation tests)
- `modules/domain/src/test/scala/mpbuilder/domain/ConfigurationBuilderSpec.scala` (constraint tests)

---

## UI & Scala.js

### Laminar `combineWith` tuple flattening issues

**Symptom:** Compilation errors or runtime issues when combining multiple signals with `combineWith`.

**Cause:** Tuples are flattened via `tuplez`. Using `case (a, b) =>` pattern matching can fail.

**Solution:** Always use explicit argument types in the handler:
```scala
// ✅ Correct
signal1.combineWith(signal2).map((a: TypeA, b: TypeB) => ...)

// ❌ Incorrect
signal1.combineWith(signal2).map { case (a, b) => ... }
```

---

### Laminar `Element` helper cannot return `emptyNode`

**Symptom:** Scala type mismatch when a helper method is declared to return `Element` but uses `emptyNode` in one branch.

**Cause:** `emptyNode` is a `CommentNode`, not an `Element`, so it does not satisfy a strict `Element` return type.

**Solution:** Return `Option[Element]` from optional helper methods and render with `.getOrElse(emptyNode)` (or adjust the helper return type to a compatible node/modifier abstraction).

**Files:** `modules/ui-productbuilder/src/main/scala/mpbuilder/ui/productbuilder/components/PricePreview.scala`

---

## Agent Session Issues

### Agent cannot push to repository

**Symptom:** Git push commands fail in agent sandbox environments.

**Solution:** Use `report_progress` tool to push changes. Direct `git push` is not available in sandboxed agent environments.

---

### Agent cannot access .github/agents/ directory

**Symptom:** Security restriction prevents reading files in `.github/agents/`.

**Solution:** These files contain instructions for other agents and are not relevant. Use `.github/copilot-instructions.md` and `CLAUDE.md` for agent configuration instead.

---

*To add a new entry, use this template:*

```markdown
### Short descriptive title

**Symptom:** What you observe (error message, unexpected behavior).

**Cause:** Why it happens (if known).

**Solution:** Step-by-step fix.

**Files:** Relevant file paths (optional).
```

---

### Wrong `FoldType` / `BindingMethod` / `ManufacturingSpeed` enum variant names

**Symptom:** Compilation error `value X is not a member of object mpbuilder.domain.model.FoldType` (or `BindingMethod` / `ManufacturingSpeed`).

**Cause:** The actual enum cases differ from commonly assumed names.

**Correct values:**
- `FoldType`: `Half, Tri, Gate, Accordion, ZFold, RollFold, FrenchFold, CrossFold`
- `BindingMethod`: `SaddleStitch, PerfectBinding, SpiralBinding, WireOBinding, CaseBinding`
- `ManufacturingSpeed`: `Express, Standard, Economy`

**Files:** `modules/domain/src/main/scala/mpbuilder/domain/model/specification.scala`, `modules/domain/src/main/scala/mpbuilder/domain/model/manufacturing.scala`

---

### `dom.window.encodeURIComponent` not available in Scala.js

**Symptom:** Compilation error `value encodeURIComponent is not a member of org.scalajs.dom.Window`.

**Cause:** `org.scalajs.dom.Window` does not directly expose the global `encodeURIComponent` function.

**Solution:** Use `scala.scalajs.js.URIUtils.encodeURIComponent(str)` instead. Keep `import org.scalajs.dom` for other `dom.window` usage (e.g. `dom.window.location.href`).

```scala
import scala.scalajs.js.URIUtils
val encoded = URIUtils.encodeURIComponent(str)
```

**Files:** `modules/ui-productbuilder/src/main/scala/mpbuilder/ui/productbuilder/components/EmailOrderModal.scala`

---

### `PriceCalculator` match structure produces unreachable case for sheet/base pricing

**Symptom:** After adding a new priority-based match at the top of `calculateComponentBreakdown` (e.g. `areaTierRule match`), sheet-priced and base-priced materials silently fall through to `NoSizeForAreaPricing` / `NoBasePriceForMaterial` errors instead of computing correctly.

**Cause:** The pattern `someOption.orElse(anotherOption) match { case Some(x) => …; case _ => …; case None => … }` has an unreachable `case None =>` because `case _` is exhaustive. The sheet/base fallback was placed in the dead `case None` branch.

**Solution:** Use a strictly nested match structure with no wildcard `case _` at the outer level:
```scala
areaTierRule match
  case Some(tierRule) => // tier area pricing
  case None =>
    areaRule match
      case Some(areaPrice) => // flat area pricing
      case None =>
        sheetRule match
          case Some(sp) => // sheet pricing
          case None =>
            baseRule match
              case Some(bp) => // base pricing
              case None => Validation.fail(…)
```

**Files:** `modules/domain/src/main/scala/mpbuilder/domain/pricing/PriceCalculator.scala`

---

### Scoring finish causes `MissingScoringPrice` pricing error in `pricelistCzkSheet`

**Symptom:** Selecting the Scoring finish in the product builder shows a pricing error "No scoring price rule found for 1 crease(s)" even though the product and pricelist appear correct.

**Cause:** `pricelistCzkSheet` (the CZK sheet-based pricelist used in the UI) was missing `ScoringCountSurcharge` rules. When a user selects the Scoring finish, the UI sets `ScoringParams(creaseCount=1)` as the default. `PriceCalculator.computeSingleFinishLine` then looks for a `ScoringCountSurcharge(1, …)` rule — and fails because the sheet pricelist only had a generic `FinishTypeSurcharge(Scoring)`, not the crease-specific rules.

**Solution:** Add `ScoringCountSurcharge(1..N, …)` and `ScoringSetupFee(…)` to `pricelistCzkSheet` in `SamplePricelist.scala`. The generic `FinishTypeSurcharge(Scoring)` can remain as a fallback for Scoring finishes without `ScoringParams` (backward compat), but must NOT be the sole rule when crease-parameterised scoring is used.

**Files:** `modules/domain/src/main/scala/mpbuilder/domain/sample/SamplePricelist.scala`

---

## UI / Reactive State

### Manufacturing UI shows nothing (blank page) after catalog changes

**Symptom:** The entire Manufacturing tab is blank — no sidebar, no content, nothing rendered.

**Cause:** `ManufacturingViewModel` is a Scala.js singleton object. Its initializer calls `generateSampleOrders()`, which calls `ConfigurationBuilder.build(...).toEither.toOption.get`. If the build fails (e.g. because a sample config references a material that is no longer in `allowedMaterialIds` for that category), `.get` on `None` throws `NoSuchElementException` and the object initialization crashes. In Scala.js, a crashed singleton makes all code that references it fail silently, leaving the tab blank.

**Solution:** Keep the material IDs in `ManufacturingViewModel.generateSampleOrders()` in sync with `SampleCatalog`. When a category's `allowedMaterialIds` changes, update the corresponding `buildConfig` call in the VM. In this case, the Banners category was changed from `vinylId` to `pvc510gId`, so the banner sample order had to switch to `pvc510gId`.

**Files:** `modules/ui/src/main/scala/mpbuilder/ui/manufacturing/ManufacturingViewModel.scala`

---

### Pricelist editor table is empty after adding new `PricingRule` variants

**Symptom:** The Pricing Rules table in the Catalog Editor / Pricelist view shows no rows, even though rules exist in the pricelist.

**Cause:** `PricelistEditorView.pricingRuleSummary` and `pricingRuleTypeName` are non-exhaustive `match` expressions over `PricingRule`. When the pricelist contains a rule variant that has no match arm, Laminar's reactive signal throws a `MatchError` inside `.map`, which stops signal propagation and leaves `children` empty.

**Solution:** Whenever a new `PricingRule` variant is added, add a corresponding case to **both** `pricingRuleSummary` (returns a display string) **and** `pricingRuleTypeName` (returns the type name string) in `PricelistEditorView`. The Scala compiler emits a non-exhaustive match warning for these matches; treat those warnings as errors.

**Files:** `modules/ui/src/main/scala/mpbuilder/ui/catalog/views/PricelistEditorView.scala`


---

### Widget controls lose their padding — scoping a reset raises its specificity

**Symptom:** In the standalone calculator, inputs have no padding (text sits flush against the left edge) and buttons like *Add to Basket* and *Validate price* collapse to a thin bar. The same components look correct in the SPA. Confusingly, some controls are fine — anything styled through its own class, e.g. `.email-order-btn`.

**Cause:** The widget replaces the SPA's global `reset.css` with a `.mp-calculator`-scoped equivalent, because `* { margin: 0; padding: 0 }` and `body { … }` would restyle the host page. But scoping changes the weight: `.mp-calculator *` is specificity **(0,1,0)**, while the base control styles in `utilities.css` — `button { padding: 8px 20px }`, `select, input { padding: 7px 10px }` — are bare element selectors at **(0,0,1)**. The reset therefore wins and zeroes their padding.

Specificity beats source order, so **reordering the concatenation does not fix this.** Class-styled controls survive because their rules are also (0,1,0) and come later in the file, which is why the breakage looks arbitrary.

**Solution:** Wrap the scoped selectors in `:where()`, which contributes zero specificity:

```css
:where(.mp-calculator),
:where(.mp-calculator *) { margin: 0; padding: 0; box-sizing: border-box; }
```

That weighs (0,0,0) — exactly like the `*` selector it replaces — so it behaves as a true reset while staying confined to the widget. Keep the file prepended (the position `reset.css` has in the SPA), and keep the typography rule on a plain `.mp-calculator` selector so it still wins against the host page's `body`.

**Verify with computed styles, not screenshots.** Inputs carry `height: 36px` independently of padding, so a zero-padding input still looks roughly right in a screenshot. Compare `getComputedStyle(el).padding` for the same elements in both apps.

**Files:** `modules/ui-calculator/css/reset-scoped.css`, `modules/ui-calculator/build-css.sh`

---

### `MatchError` from `EmailOrderModal.formatSpec` on a full configuration

**Symptom:** Building the order e-mail for a basket throws a `MatchError`, or a Laminar signal silently stops updating.

**Cause:** `formatSpec` matches on `SpecValue`. The single-configuration path only ever fed it the specs the form had produced, so a missing `BleedSpec` arm went unnoticed. The basket path iterates `ProductConfiguration.specifications.specs.values`, which contains every spec the configuration carries — including `BleedSpec`.

**Solution:** Keep `formatSpec` exhaustive over `SpecValue`. The compiler emits a non-exhaustive match warning for it; treat that warning as an error, as with `PricelistEditorView`'s `PricingRule` matches.

**Files:** `modules/ui-productbuilder/src/main/scala/mpbuilder/ui/productbuilder/components/EmailOrderModal.scala`

---

### Product builder renders with SPA behaviour (or crashes) in a new host app

**Symptom:** A new app embedding `ProductBuilderApp` shows concrete completion dates, an artwork section, or a "Proceed to Checkout" button that goes nowhere.

**Cause:** `mpbuilder.ui.productbuilder` reads its host-specific behaviour from the `BuilderEnvironment` singleton. If the host never calls `BuilderEnvironment.init(...)`, the defaults apply (no queue data, no artwork, a no-op basket action) — and if it installs the wrong environment, it gets the other app's behaviour.

**Solution:** Call `BuilderEnvironment.init(...)` once, before the first render. The SPA does this in `Main.main` with `FullAppEnvironment.environment`; the calculator does it in `CalculatorWidget.mount` with `CalculatorEnvironment.environment(config)`.

**Files:** `modules/ui-productbuilder/src/main/scala/mpbuilder/ui/productbuilder/BuilderEnvironment.scala`, `modules/ui/src/main/scala/mpbuilder/ui/Main.scala`, `modules/ui-calculator/src/main/scala/mpbuilder/calculator/CalculatorWidget.scala`

---

### `JsonReaderException: expected key: "type"` when reading a JSONB row

**Symptom:** A value written with the `backend/json` codecs round-trips in memory but fails to decode after a trip through a PostgreSQL `JSONB` column, with `expected key: "type"`.

**Cause:** `JSONB` does not preserve object key order (it sorts keys by length, then bytes). jsoniter-scala, by default, requires an ADT's discriminator field to be the **first** key of the object.

**Solution:** The shared `JsonSupport.config` sets `.withRequireDiscriminatorFirst(false)`. Always derive persisted codecs with `JsonCodecMaker.make(config)` from `mpbuilder.json.JsonSupport`, never with the library default config.

**Files:** `backend/json/src/main/scala/mpbuilder/json/JsonSupport.scala`

---

### `Conflicting definitions: given_JsonValueCodec_List` (or `_NonEmptyChunk`)

**Symptom:** Several `given JsonValueCodec[List[X]] = …` / `given JsonValueCodec[NonEmptyChunk[X]] = …` in one object fail to compile.

**Cause:** Anonymous givens are named after the outer type constructor only, so `List[A]` and `List[B]` both become `given_JsonValueCodec_List`.

**Solution:** Name them: `given listOrderView: JsonValueCodec[List[OrderView]] = …`, `given necOrderLine: JsonValueCodec[NonEmptyChunk[OrderLine]] = …`.

**Files:** `backend/json/src/main/scala/mpbuilder/json/*Codecs.scala`, `backend/{storefront,backoffice}/01-i-tapir-api/…/http/api/*ApiCodecs.scala`, `backend/api-kernel/src/main/scala/mpbuilder/api/ApiKernelCodecs.scala`

---

### Tapir schema derivation: "Could not find Schema", then "Class too large"

**Symptom:** `sttp.tapir.generic.auto` derivation of domain bodies reports `Could not find Schema for type …` deep inside the catalog tree; raising `-Xmax-inlines` makes it compile in ~2 minutes and then fails emitting with `Class too large` / `Method too large`.

**Cause:** Magnolia inlines the entire catalog/pricing/order ADT tree per endpoint. It hits the inline limit first, then JVM class-file limits; the generated code would also bloat the Scala.js client bundle.

**Solution:** Bodies use lightweight named schemas (`ApiSupport.body[T]`, `jsonList[T]`, `ApiSchemas.named`). The jsoniter codecs are the source of truth for body structure. Opaque types still need explicit schemas (`ApiSchemas`), including ones from other contexts, e.g. `manufacturing.WorkflowId` / `StepId` reached via `ManufacturingError`.

**Files:** `backend/api-kernel/src/main/scala/mpbuilder/api/ApiSchemas.scala`, `backend/api-kernel/src/main/scala/mpbuilder/api/ApiSupport.scala`

---

### Facade-generated IDs collide after a server restart

**Symptom:** With PostgreSQL storage, the first order after a restart fails with a unique violation or overwrites `ord-1`.

**Cause:** `StorefrontFacade` used to number orders, lines and artwork from an in-memory counter that restarted at 0.

**Solution:** Facades take an `IdSequence` port: `IdSequence.inMemoryLayer()` in the static demo, `PostgresIdSequence.layer` (`platform.id_sequence`, starting at 10000) in persistent mode.

**Files:** `backend/shared/src/main/scala/mpbuilder/shared/BlobStorage.scala` (`IdSequence`), `backend/platform/src/main/scala/mpbuilder/platform/db/PostgresIdSequence.scala`

---

### Frontend on another origin can't load data from the API (CORS preflight 405)

**Symptom:** The SPA served from e.g. `http://localhost:3000` with `window.__API_BASE_URL__ = "http://localhost:8080"` renders the catalog, but quotes, cart, login and all admin calls fail. Chrome reports *"Response to preflight request doesn't pass access control check: It does not have HTTP ok status"*, and `OPTIONS` requests return **405**.

**Cause:** Browsers send an `OPTIONS` preflight before every cross-origin JSON `POST`/`PUT`/`DELETE` and every request with an `Authorization` header. zio-http's `Middleware.cors` added CORS headers but did not answer the preflight; Tapir saw no `OPTIONS` endpoint and rejected it with 405. Plain `GET`s (catalog, showcases) skip the preflight, so curl and the first screen looked fine. The JVM E2E client never sends preflights either.

**Solution:** CORS is handled by Tapir's `CORSInterceptor` (`HttpServer.corsConfig`), which answers preflights for every endpoint with 204. `PlatformE2ESpec` now asserts a 2xx preflight for storefront and admin endpoints. If you change CORS handling, verify with:

```bash
curl -i -X OPTIONS localhost:8080/api/v1/storefront/builder/quote \
  -H "Origin: http://localhost:3000" -H "Access-Control-Request-Method: POST" \
  -H "Access-Control-Request-Headers: content-type,authorization"
```

**Files:** `backend/app/src/main/scala/mpbuilder/app/http/HttpServer.scala`, `backend/app/src/test/scala/mpbuilder/app/PlatformE2ESpec.scala`
