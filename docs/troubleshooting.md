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

### Mill SSL issues in sandbox environments

**Symptom:** Mill fails to download dependencies due to SSL certificate issues in sandboxed environments.

**Solution:** Fall back to sbt for building in sandboxed environments. See sbt installation instructions above.

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

*To add a new entry, use this template:*

```markdown
### Short descriptive title

**Symptom:** What you observe (error message, unexpected behavior).

**Cause:** Why it happens (if known).

**Solution:** Step-by-step fix.

**Files:** Relevant file paths (optional).
```
