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

**Files:** `modules/ui/src/main/scala/mpbuilder/ui/productbuilder/components/PricePreview.scala`

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

### Missing binding/cover material prices cause CategoryPresetSpec pricing failure

**Symptom:** `CategoryPresetSpec - every preset produces a priced configuration` fails with messages like `cat-calendars/preset-calendars-wall: pricing failed`.

**Cause:** When binding materials (coil, wire-O) or transparent cover plastics are added to a category's component templates and presets, their pricing rules (`MaterialLinearPrice`, `MaterialFixedPrice`, `MaterialBasePrice`) must be present in **all** pricelists — `pricelist` (USD), `pricelistCzk`, and `pricelistCzkSheet`. The `CategoryPresetSpec` uses `pricelistCzkSheet` by default.

**Solution:** Add the corresponding `MaterialLinearPrice` / `MaterialFixedPrice` / `MaterialBasePrice` entries to all three pricelists in `SamplePricelist.scala` whenever a new binding or cover material is added.

**Files:** `modules/domain/src/main/scala/mpbuilder/domain/sample/SamplePricelist.scala`

---

### `sed` multi-line insertion corrupts Scala helper method order

**Symptom:** After using `sed` to add a new helper function before an existing one, Scala compiler reports "Not found" for the existing function (e.g. `generate`) that appears correctly in the file visually.

**Cause:** `sed` line-end substitution may merge the new function body with the next line if the pattern does not include a newline. The result is the new function's last line and the existing function's signature appear on the same line, removing the `def` keyword from scope.

**Solution:** Use the `edit` tool (which does literal string replacement) instead of `sed` for inserting Scala definitions. Always verify the surrounding context with `view` after an edit.

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
