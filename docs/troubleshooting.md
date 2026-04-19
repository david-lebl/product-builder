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

### Global TechnologyConstraint rules fail on unrelated product categories

**Symptom:** Adding a `TechnologyConstraint` rule (e.g., "top edge binding requires coil/wire") causes validation failures for completely unrelated products like business cards and banners.

**Cause:** `TechnologyConstraint` rules are evaluated globally — they are not scoped to a specific category (unlike `SpecConstraint`). If the rule's guard predicate passes when a spec is absent (e.g., `AllowedBindingEdges` returns success when no binding edge is set), the `Not(...)` wrapper inverts it, and the overall `Or(Not(...), ...)` fails.

**Solution:** Use predicates that return `false` when the relevant spec is absent. For example, use `BindingEdgeIs(edges)` (which returns `false` when no edge is set) instead of `Spec(AllowedBindingEdges(edges))` (which returns success/unit when no spec exists). The pattern is:
```scala
// ✅ Correct — fires only when binding edge IS set to Top
ConfigurationPredicate.Or(
  ConfigurationPredicate.Not(ConfigurationPredicate.BindingEdgeIs(Set(BindingEdge.Top))),
  ConfigurationPredicate.BindingMethodIs(Set(...)),
)

// ❌ Wrong — fires for ALL products because Not(AllowedBindingEdges) is false when no edge exists
ConfigurationPredicate.Or(
  ConfigurationPredicate.Not(ConfigurationPredicate.Spec(SpecPredicate.AllowedBindingEdges(Set(BindingEdge.Top)))),
  ConfigurationPredicate.BindingMethodIs(Set(...)),
)
```

**Files:** `modules/domain/src/main/scala/mpbuilder/domain/rules/predicates.scala`, `modules/domain/src/main/scala/mpbuilder/domain/sample/SampleRules.scala`

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

*To add a new entry, use this template:*

```markdown
### Short descriptive title

**Symptom:** What you observe (error message, unexpected behavior).

**Cause:** Why it happens (if known).

**Solution:** Step-by-step fix.

**Files:** Relevant file paths (optional).
```
