# Kyo Ecosystem Findings — Bugs, Gaps & Upstream Suggestions

> Gathered while porting the whole backend (≈23k lines, 7 bounded contexts, 2 BFFs, ~100 HTTP routes,
> PostgreSQL persistence, browser SPAs) from ZIO / Tapir / Magnum / jsoniter to Kyo. See
> [changelog/2026-11-24-kyo-backend-rewrite.md](../changelog/2026-11-24-kyo-backend-rewrite.md).
> Purpose: material for upstream issues and PRs to [getkyo/kyo](https://github.com/getkyo/kyo).

**Environment:** Kyo `1.0.0-RC7` (`kyo-core`, `kyo-combinators`, `kyo-schema-json`, `kyo-sql-postgres`,
`kyo-http`, `kyo-test-runner`, `kyo-pod`, `kyo-ui`), Scala 3.9.0, Scala.js 1.22.0, JDK 25.0.1 (Temurin),
Mill 1.1.3, macOS arm64, PostgreSQL 17.

Each finding is tagged **Bug** (wrong behaviour), **Footgun** (correct by the rules but easy to get
silently wrong), **Gap** (missing capability we had to work around) or **DX** (friction / messages / docs),
and rated by how much it cost us. "Verified" means we reproduced it in isolation or read the cause in the
Kyo sources.

## Summary

| # | Module | Kind | Finding | Impact |
|---|---|---|---|---|
| 1 | kyo-test | **Bug** | Group-level `.handle` does not reach nested leaves → `KyoBugException: unhandled suspension` | Medium |
| 2 | kernel / core | **Footgun** | An effect returned where `Unit < S` is expected compiles and **never runs** | High |
| 3 | kyo-http | **Bug?** | `HttpResponse.halt` drops the response body and sends a generic `{status, error}` JSON | Medium |
| 4 | kyo-http | **Gap** | Scala.js client is Node-only (`node:net`); no browser `fetch` backend | High |
| 5 | kyo-http | **Gap** | `.error[E]` needs a non-generic `ConcreteTag`; one error type ↔ one fixed status | Medium |
| 6 | kyo-http | **Gap** | OpenAPI generator never emits `components` — every schema inlined (9 MB spec) | Medium |
| 7 | kyo-http | **Gap** | Server CORS takes a single origin; `HttpHandler.withFilter` is `private[kyo]` | Low |
| 8 | kyo-http | **DX** | Many routes in one expression → `Method too large` (inline expansion) | Low |
| 9 | kyo-http | **DX** | Conservative defaults: `maxContentLength` 64 KiB, host `127.0.0.1`, port `0` | Low |
| 10 | core | **Gap** | `AtomicRef` has no `update` / `modify` (only `updateAndGet` / `getAndUpdate`) | Medium |
| 11 | prelude / data | **Gap** | No error-accumulating `Validation` / `NonEmptyChunk` | Medium |
| 12 | kernel | **DX** | `if`/`match` mixing a pure value and an effect has no `flatMap` (union type) | Low |
| 13 | kyo-schema | **DX** | Missing schema for an opaque or recursive type reports "given instance derived … does not match" | Low |
| 14 | kyo-schema | **Gap / DX** | Non-`String` map keys always encode as `[{key, value}]`; no key-codec typeclass | Low |
| 15 | kyo-schema | **DX** | `BigDecimal` encodes as a JSON *string* (sensible, but surprising and undocumented) | Low |
| 16 | kyo-system | **Footgun** | `import kyo.*` shadows `java.lang.System` once `kyo-system` is on the classpath | Low |
| 17 | kyo-net / ffi | **DX** | Noisy startup: JDK restricted-method warnings + a WARN about missing native kqueue artifact | Low |
| 18 | combinators / core | **DX** | Small API gaps vs. ZIO users' expectations (`.either`, `Kyo.foreach(Option)`, multi-arg `assert`) | Low |
| 19 | core | **DX** | `Clock.TimeControl.set` sleeps 100 ms of wall-clock time per call by default | Low |
| 20 | kyo-kernel | **Bug?** | JMH (`jmh-core`, generators → `asm`, `commons-math3`, `jopt-simple`) is a compile-scope dependency, so it ships in every Kyo app | Low |
| 21 | kyo-ui | **Bug + Gap** | `maxHeight(Length.Auto)` emits invalid `max-height: auto` (no way to lift a max size); no inset `shadow`; no `dvh` / value fallbacks | Low |

What worked very well is listed at the end — worth saying upstream too.

---

## 1. kyo-test: group-level `.handle` is not applied to nested leaves — **Bug** (verified)

The API docs say: *"when applied to a group node, the composed transform is invoked freshly around each
descended leaf body"*. In practice the leaf runs without the handler and the runner crashes with a kernel bug report.

```scala
import kyo.*
import kyo.test.*

class KyoReproSpec extends Test[Env[Int]]:
  private val provide = [A] => (v: A < (Env[Int] & Async & Abort[Any] & Scope)) => Env.run(42)(v)

  "group-level handle".handle[Env[Int]](provide) - {        // ✗ fails
    "leaf reads Env" in { Env.get[Int].map(i => assert(i == 42)) }
  }

  "leaf-level handle".handle[Env[Int]](provide) in {         // ✓ passes
    Env.get[Int].map(i => assert(i == 42))
  }
```

```
[FAIL] group-level handle › leaf reads Env
kyo.bug$KyoBugException: BUG unhandled suspension: Kyo(kyo.Env[+scala.Any], $anonfun.get(KyoReproSpec.scala:11:19))
Please open an issue 🥹 https://github.com/getkyo/kyo/issues
```

**Workaround:** discharge the effect inside each leaf (we used `Env.runAll` via small extension methods).
**Suggestion:** apply the group transform to descendants as documented, or reject `.handle` + `-` at compile
time; in any case the typed leaf body should not be able to reach the runner with an unhandled `Env`.

## 2. Effects silently dropped through value discarding — **Footgun** (verified, highest cost)

A Kyo computation is a value. When the expected type is `Unit < S` and the expression is `X < S`, Scala's
value discarding turns the *pending computation* into `()` — it type-checks and never executes:

```scala
private def increment(ref: AtomicRef[Int]): Unit < Sync = ref.updateAndGet(_ + 1)

for
  ref <- AtomicRef.init(0)
  _   <- increment(ref)
  n   <- ref.get
yield assert(n == 0)   // passes — the update never ran
```

This hit every in-memory repository in our port (`save(x): Unit < Async = ref.updateAndGet(...)`): code
compiled, unit tests of the pure domain passed, and only cross-context tests revealed that carts and orders
"disappeared". Kyo already has a dedicated check for a related case (*"Cannot lift `Unit < …` to the expected
type"*, issue #903) but not this one.

**Workaround:** `-Wvalue-discard` (reports `discarded non-Unit value of type Int < kyo.Sync`) plus a
unit-returning `ref.update`. **Suggestions:**
- Detect `A < S` → `Unit < S` value discarding (macro check like #903, or document `-Wvalue-discard` /
  `-Wnonunit-statement` as *required* compiler flags in the Kyo README / getting-started).
- Add `AtomicRef.update` (see #10) so the natural spelling is the safe one.

## 3. kyo-http: `HttpResponse.halt` discards the response body — **Bug?** (verified in source)

`Halt` is documented as *"the framework … sends the wrapped response directly"*, but
`RouteUtil.encodeHalt` replaces any body with `{"status": <code>, "error": "<status name>"}`
(`kyo/internal/server/RouteUtil.scala`, `encodeHalt` → `encodeErrorBody(status)`). Only status and headers
survive. A handler therefore cannot short-circuit with a custom JSON error body — e.g. an API-specific error
envelope whose status depends on the error value.

**Suggestion:** serialize the halted response's body field when present (or add `halt(status, json)`), and
document the current behaviour either way.

## 4. kyo-http on Scala.js only works on Node — **Gap** (verified)

`kyo-http_sjs1` (and `kyo-net_sjs1`) import `node:net`, `node:tls`, `node:fs` via `@JSImport`; there is no
`fetch`/`XMLHttpRequest` backend. A browser SPA cannot use `HttpClient` (it would not even link under
`ModuleKind.NoModule`), so we wrote a ~20-line `window.fetch` transport and kept routes JVM-only.

**Suggestion:** a browser backend for `HttpClient` (fetch, with `AbortController` for interruption), selected
automatically when `node:*` is unavailable. That would let one `HttpRoute` definition drive server, JVM client
**and** browser client — the strongest selling point of typed routes. At minimum, document "Node only" in the
module README.

## 5. kyo-http typed errors: non-generic types only, one fixed status per type — **Gap** (verified)

```scala
route.error[NotFound[StorefrontError]](HttpStatus.NotFound)
// error: This method requires a ConcreteTag, but the type NotFound[StorefrontError] has type parameters.
```

Combined with #3, an API whose status depends on the error *value* (our facades return one ADT; status is
derived from the case: 401/403/404/409/400) needs one concrete envelope case class per status per error type.

**Suggestions:** `error[E](statusOf: E => HttpStatus)`; and/or allow generic error types whose tag is erased
(match by class).

## 6. kyo-http OpenAPI: no `components` / `$ref` — **Gap** (verified in source)

`OpenApiGenerator.generate` always sets `components = None`; every request/response/error schema is inlined
per operation. With ~100 routes over a rich domain (and our BFF error ADT on each of 5 statuses) the generated
`/openapi.json` is **≈9 MB** — Swagger UI loads it, slowly. Recursive types also inline.

**Suggestion:** emit named schemas into `components.schemas` (keyed by type name / tag) and reference them with
`$ref`; recursion then becomes representable instead of expanded.

## 7. kyo-http: CORS is single-origin; no server-wide custom filters — **Gap** (verified)

`HttpServerConfig.Cors(allowOrigin: String, …)` sends one fixed value. Supporting an allow-list requires
reflecting the request `Origin`, which needs a server-wide filter — but `HttpHandler.withFilter` is
`private[kyo]` and `HttpServerConfig` accepts no custom filters. Per-route `.filter(...)` works but must be added
to every route definition.

**Suggestions:** `Cors(allowOrigins: Seq[String] | predicate)` with origin reflection + `Vary: Origin`; and a
public way to add server-wide filters.

## 8. kyo-http: `Method too large` with many routes in one expression — **DX** (verified)

69 routes built in a single `Seq(...)` (each with inline `bodyJson[T]` / `.error[E]` expansions) type-checked
and then failed with `Method too large: …BackofficeRoutes.<init>`. Splitting into one method per area fixed it.
**Suggestion:** a note in the docs, or less inline expansion per route (e.g. hoist the JSON-schema computation
out of the call site).

## 9. kyo-http: conservative server defaults — **DX**

`HttpServerConfig.default` uses `maxContentLength = 65536`, `host = "127.0.0.1"`, `port = 0`. All reasonable
for tests, but JSON uploads above 64 KiB fail and the server is unreachable from containers until overridden.
**Suggestion:** document them prominently next to `HttpServer.init`.

## 10. `AtomicRef` has no `update` / `modify` — **Gap** (verified)

The safe API has `get`, `set`, `getAndSet`, `compareAndSet`, `getAndUpdate`, `updateAndGet` — no unit-returning
`update` and no `modify(f: A => (B, A))`. Users coming from ZIO `Ref` reach for `updateAndGet` in `Unit`
positions, which is exactly the trigger for #2. We added both as extensions.
**Suggestion:** add `update` and `modify` to `AtomicRef` (and `AtomicLong/Int`).

## 11. No error-accumulating validation — **Gap**

`Abort` / `Result` short-circuit. Smart constructors and rule engines need *all* errors (our product
configuration engine reports every violated rule). We wrote `Validation[E, A]` + `NonEmptyChunk[A]`
(≈150 lines). **Suggestion:** a `Validated`/`Validation` in `kyo-data` (applicative zip, `validateAll`,
`toResult`), plus a `NonEmptyChunk`.

## 12. Pure/effect branches don't compose — **DX**

```scala
for
  x <- if cond then (a, None) else load(key)   // value flatMap is not a member of (A, B) | (A, B) < (Async & Abort[E])
```
The `if` infers a union of a pure value and a pending computation. An ascription
(`(…): (A, B) < (Async & Abort[E])`) fixes it. **Suggestion:** mention in the docs; a better error message
("ascribe the branch type …") would save time.

## 13. kyo-schema: misleading message for missing / recursive schemas — **DX** (verified)

For an opaque type without a given (`WorkflowId`) and for a recursive enum (`ConfigPredicate`) the error is:

```
No given instance of type kyo.Schema[ConfigPredicate] was found.
I found: kyo.Schema.derived[ConfigPredicate]
But given instance derived in object Schema does not match type kyo.Schema[ConfigPredicate].
```
Fix for recursion: declare a *named* given (`given configPredicateSchema: Schema[ConfigPredicate] = Schema.derived`)
so self-references resolve to it. **Suggestion:** have the macro report the real cause ("opaque type — provide
a Schema" / "recursive type — declare a named given").

## 14. kyo-schema: non-`String` map keys encode as entry arrays — **Gap / DX**

`Map[K, V]` with `K != String` always becomes `[{"key": …, "value": …}]`, also for opaque ids or enums that
have an obvious string form. We added `keyedMap(decodeKey)(encodeKey)` built on `stringMapSchema.transform`.
**Suggestion:** a `SchemaKey[K]` (key codec) typeclass with instances for enums and `transform`-ed strings.

## 15. kyo-schema: `BigDecimal` is a JSON string — **DX**

`bigDecimalSchema` writes `"12.50"` (quoted). Good for money (no precision loss), but it differs from most JSON
libraries and is not documented. **Suggestion:** document it; optionally offer a numeric variant
(the writer already has `bigNumber`).

## 16. `import kyo.*` shadows `java.lang.System` — **Footgun** (verified)

`kyo-schema-json` depends on `kyo-system`, which exports `kyo.System`. With `import kyo.*`,
`System.nanoTime()` fails: *"value nanoTime is not a member of object kyo.System"*. Other common names in the
`kyo` package (`Result`, `Duration`, `Instant`, `Clock`, `Random`, `Log`, `Span`, `Record`) are also easy
to collide with in user code. **Suggestion:** document the collisions; consider not exporting `System` at the
`kyo` top level.

## 17. Noisy startup on the JVM — **DX**

Every JVM using kyo-net prints JDK warnings from `kyo.ffi.internal.NativeLoader` (*"restricted method in
java.lang.foreign.SymbolLookup … use --enable-native-access=ALL-UNNAMED"*) and a `WARN` that the kqueue
native library is missing on macOS (*"add kyo-net's darwin-aarch64 classifier artifact"*). Neither indicates a
problem (NIO is used). **Suggestion:** log the backend selection at `DEBUG`/`INFO`; document the JVM flag.

## 18. Small API gaps for ZIO migrants — **DX**

Things we re-implemented in a few lines each: `.either` (only `result`/`resultPartial` exist), `.orNone`,
`.ignore`, `.as(x)`, `someOrFail` on `Option[A] < S`, `Kyo.foreach` over `Option` (only `Iterable`), and
kyo-test's single-condition `assert` (ZIO's `assertTrue(a, b, c)` became one `assert` per line).
None is a bug; a short "Coming from ZIO" cheat sheet would cover most of them.

## 19. `TimeControl.set` sleeps real time — **DX** (verified in source)

`Clock.withTimeControl(_.set(instant))` uses a default `wallClockDelay` of 100 ms, so each call blocks the test
for 100 ms. **Suggestion:** document it next to `set(now)` (the `set(now, wallClockDelay)` overload exists).

## 20. kyo-kernel pulls a benchmarking harness into production — **Bug?** (verified in the POM)

`kyo-kernel_3-1.0.0-RC7.pom` declares `jmh-core`, `jmh-generator-bytecode` and `jmh-generator-reflection` 1.37
without `test`/`provided` scope. Every Kyo application therefore ships JMH plus its transitive `asm`,
`commons-math3` and `jopt-simple` (`kyo-data` likewise brings `pprint`/`fansi`). In our server they are 7 of the
28 runtime jars. **Suggestion:** move JMH to a separate benchmark module or `provided` scope.

## 21. kyo-ui `Style`: no "unset" / `none` sizes, no inset shadow, no value fallbacks — **Bug + Gap** (verified in source)

Found while pinning the backoffice DataTable views to the viewport (sticky table header, internally scrolling
table and detail pane). The layout is in `AdminTheme.documentCss`. It is raw CSS because three things cannot be
written with typed `kyo.Style` (checked in `kyo-ui_3-1.0.0-RC7-sources.jar`: `Style.scala`, `Length.scala`,
`internal/CssStyleRenderer.scala`):

1. **`maxHeight(Length.Auto)` emits invalid CSS (Bug).** `Length.scala` documents `Auto` as accepted by
   `maxHeight`, and `CssStyleRenderer` renders it as `max-height: auto;`. `auto` is not a valid `max-height`
   value, so browsers drop the declaration and the base rule's cap stays in force. `Length` has no `None`, and
   `Length.Calc("none")` renders `calc(none)`, so there is no typed way to **lift** a `max-height` / `max-width`
   set by a base rule. Our own `AppTheme` tablet rule
   `.rule("mp-dt-detail-pane", … .maxHeight(Length.Auto))` has therefore never done anything: the
   `max-height: 78vh` from the base rule still applies at ≤ 960px. The same applies to `minWidth` / `minHeight`
   (`auto` is valid there, but there is no `none` for the max variants).
2. **No `inset` box-shadow (Gap).** `Style.shadow(x, y, blur, spread, color)` has no inset flag. With
   `border-collapse: collapse` (which `mp-dt-table` uses), a `position: sticky` `<th>`'s `border-bottom` stays
   behind on scroll; the usual fix is `box-shadow: inset 0 -1px 0 <border>`, which cannot be expressed.
3. **No value fallbacks (Gap).** `Style` is last-write-wins per prop kind (`Style.++` / `appendProp`), so the
   progressive-enhancement pair `height: 100vh; height: 100dvh` cannot be emitted. `Length.Calc("100dvh")`
   gives `calc(100dvh)` (valid), but without the `vh` fallback for older browsers. `Length` has only
   `Px | Pct | Em | Vh | Calc | Auto`: no `dvh` / `svh` / `rem` / `fr`.

What *does* work, contrary to our first assumption: `Selector` has `descendant`, `child`, `pseudo(name)` and a
`Stylesheet.MediaQuery.minWidth` / `raw`, so `:has()` rules like
`Selector.cls("mp-admin-content").pseudo("has(.mp-dt-root)")` and
`Selector.cls("mp-admin-content").descendant(Selector.tag("*").pseudo("has(.mp-dt-root)"))` are expressible.
(Not tried: whether `Stylesheet.media` accepts `Selector` rules is unchecked; the raw string keeps the whole
layout in one readable block anyway.)

**Workaround:** `AdminTheme.documentCss`, a raw CSS string injected next to `AppTheme.documentCss` by
`modules/ui` and `modules/ui-admin`. It is not part of the typed `Stylesheet`, so the JVM test checks the string
itself (`BackofficeUiSpec`).
**Suggestion:** reject or render `Length.Auto` correctly for `max*` (emit `none`), add a `Length.None` for
`maxWidth` / `maxHeight`, add an `inset` parameter to `shadow`, add `dvh` / `svh` / `rem` units, and offer a
`Style.raw(prop, value)` escape hatch that is appended (not merged), which would also cover fallbacks.

---

## What worked well (worth telling upstream)

- **kyo-sql**: the native PostgreSQL driver + pool, `sql"…"` fragments with typed binds,
  `Sql.jsonColumn` (typed JSONB from a kyo-schema `Schema`), `DB.transaction`, `DB.withAdvisoryLock` and
  `executeRaw` for migrations — all six repository adapters and the migration runner worked first time
  against PostgreSQL 17.
- **kyo-http**: typed `HttpRoute`s with path captures, header/body fields and record access
  (`req.fields.cartId`) are pleasant; server-wide CORS and OpenAPI generation work out of the box; the same
  routes power a JVM client.
- **kyo-schema**: derivation handled the entire domain (nested ADTs, defaults, `Option`, `Set`, recursive
  rule trees with named givens); `transform` made validated opaque types easy.
- **Effects**: porting ~20k lines of ZIO code to `A < (Async & Abort[E])` was largely mechanical; Scala.js
  support across `kyo-core`, `kyo-schema` and `kyo-sql` let the full domain still run in the browser.
- **kyo-test** integrates with Mill's test runner with no extra setup; **kyo-pod** started containers reliably.

## Next steps

- [ ] File issues for #1, #3, #5, #6 (each has a reproduction or a source pointer above).
- [ ] Propose a browser `fetch` backend for `kyo-http` (#4).
- [ ] Propose `AtomicRef.update` / `modify` (#10) and documenting `-Wvalue-discard` (#2).
- [ ] Contribute a "Coming from ZIO" page (#12, #16, #18).
- [ ] File a kyo-ui issue for `max-height: auto` + missing `Length.None` / inset shadow / `dvh` (#21).
- [ ] Measure the Scala.js footprint of derived kyo-schema codecs before reporting anything: after the rewrite the
      production SPA grew +6.4% gzipped (3.05 → 3.25 MB) while the storefront-only calculator shrank −1.2%, which
      points at codec size but is not yet attributed.
