# GitHub Copilot Instructions

This is a **Scala 3.3.3** monorepo for a material/print product configurator SPA. Follow the conventions below precisely when generating or modifying code.

---

## Build Tools

This project supports both **Mill** (primary) and **sbt** (legacy).

### Mill (preferred)
```bash
mill domain.jvm.compile      # Domain JVM
mill domain.js.compile       # Domain Scala.js
mill ui.compile              # UI (Scala.js SPA)
mill ui-framework.compile    # UI kit only

mill domain.jvm.test                                  # All tests
mill 'domain.jvm.test.testOnly *PriceCalculatorSpec'  # Single suite

mill ui.fastLinkJS           # Dev JS build  → out/ui/fastLinkJS.dest/main.js
mill ui.fullLinkJS           # Prod JS build → out/ui/fullLinkJS.dest/main.js
```

### sbt (legacy)
```bash
sbt domainJVM/compile
sbt domainJVM/test
sbt "domainJVM/testOnly *PriceCalculatorSpec"
sbt ui/fastLinkJS
```

---

## Module Structure

| Module | Target | Description |
|---|---|---|
| `domain/` | JVM + JS | Pure functional core — no ZIO effects, no Scala.js APIs |
| `ui/` | Scala.js | Laminar SPA; depends on `domain.js` + `ui-framework` |
| `ui-framework/` | Scala.js | Reusable Laminar components (`mpbuilder.uikit`) — no domain dependency |
| `ui-showcase/` | Scala.js | Demo app for `ui-framework` |

---

## Domain Layer Rules

### Error handling — always use `Validation`, never `Either`
```scala
import zio.prelude.*

// ✅ Correct — accumulates ALL errors
def validate(x: Int): Validation[String, Int] =
  Validation.validateWith(
    Validation.fromPredicateWith("must be positive")(x)(_ > 0),
    Validation.fromPredicateWith("must be < 1000")(x)(_ < 1000)
  )((a, _) => a)

// ❌ Wrong — do not use Either in domain
def validate(x: Int): Either[String, Int] = ???
```

### Opaque types with smart constructors
Every ID and value type is an opaque type. Smart constructors return `Validation`. Use `.unsafe(...)` **only** in tests and sample data.

```scala
opaque type MaterialId = String
object MaterialId:
  def apply(value: String): Validation[String, MaterialId] =
    if value.nonEmpty then Validation.succeed(value)
    else Validation.fail("MaterialId must not be empty")

  def unsafe(value: String): MaterialId = value           // tests / sample data only

  extension (id: MaterialId) def value: String = id
```

### Rules as sealed ADTs (data, not functions)
`PricingRule` and `CompatibilityRule` are `enum`/sealed types — they represent *data* interpreted by a pure engine. Never embed logic inside a rule case.

```scala
// ✅ Data
enum PricingRule:
  case MaterialBasePrice(materialId: MaterialId, unitPrice: Money)
  case QuantityTier(minQuantity: Int, maxQuantity: Option[Int], multiplier: BigDecimal)
  // ...

// ❌ Wrong — logic in the rule itself
case class MaterialBasePrice(materialId: MaterialId, unitPrice: Money):
  def apply(ctx: PricingContext): Money = ???
```

### Pure domain — no effects, no Scala.js APIs
The `domain/` module cross-compiles to both JVM and JS. Never import:
- `zio.ZIO` or any ZIO effect type
- `scala.scalajs.*`
- Any JVM-only library

---

## Money

`Money` is an opaque type over `BigDecimal`. Always use `HALF_UP` rounding. Never use `Double` for monetary values.

```scala
// ✅
val price: Money = Money("12.50")
val rounded: Money = price.rounded   // .setScale(2, HALF_UP)

// ❌
val price: Double = 12.5
```

---

## Localization

`LocalizedString` is an opaque type over `Map[Language, String]`. Always provide both EN and CS for user-facing strings.

```scala
LocalizedString("Material not found", "Materiál nebyl nalezen")
```

`Language` enum: `Language.En`, `Language.Cs`.

---

## Laminar / Scala.js (UI)

### `combineWith` tuple handlers
When combining multiple Signals/Streams, tuples are flattened via `tuplez`. Always use **explicit argument types** in the handler — never pattern-match on a tuple directly:

```scala
// ✅
signal1.combineWith(signal2).map((a: String, b: Int) => s"$a $b")

// ❌ — compile error with tuplez
signal1.combineWith(signal2).map { case (a, b) => s"$a $b" }
```

### `Signal[String]` labels in `uikit` components
All `ui-framework` field components receive `Signal[String]` for labels, not `Language` or `LocalizedString`. Translate before passing in:

```scala
TextField(label = language.signal.map(lang => label(lang)), ...)
```

### UI kit components (`mpbuilder.uikit`)
- **fields**: `TextField`, `TextAreaField`, `SelectField`, `CheckboxField`, `RadioGroup`
- **containers**: `Tabs`, `Stepper`, `SplitTableView`
- **feedback**: `ValidationDisplay`
- **form**: `FormState`, `FormRenderer`, `FormFieldState`, `FieldValidator`
- **util**: `Visibility.when` / `Visibility.unless`

---

## Key Source Paths

| Area | Path |
|---|---|
| Domain model | `modules/domain/src/main/scala/mpbuilder/domain/model/` |
| Pricing rules & engine | `modules/domain/src/main/scala/mpbuilder/domain/pricing/` |
| Compatibility rules | `modules/domain/src/main/scala/mpbuilder/domain/rules/` |
| Domain services | `modules/domain/src/main/scala/mpbuilder/domain/service/` |
| Sample / test data | `modules/domain/src/main/scala/mpbuilder/domain/sample/` |
| Manufacturing domain | `modules/domain/src/main/scala/mpbuilder/domain/manufacturing/` |
| Manufacturing UI | `modules/ui/src/main/scala/mpbuilder/ui/manufacturing/` |
| UI kit components | `modules/ui-framework/src/main/scala/mpbuilder/uikit/` |
| Pricing tests | `modules/domain/src/test/scala/mpbuilder/domain/PriceCalculatorSpec.scala` |

---

## Testing

Tests use **ZIO Test** (`ZIOSpecDefault`).

```scala
object MySpec extends ZIOSpecDefault:
  def spec = suite("MyFeature")(
    test("does something") {
      val result = myFunction(input)
      assertTrue(result == expected)
    }
  )
```

Run a single suite:
```bash
mill 'domain.jvm.test.testOnly *MySpec'
```

---

## Pricing Calculation Flow

```
subtotal
  → × quantity multiplier         (discountedSubtotal)
  → × manufacturing speed factor
  → + setupFees                   (one-time, NOT discounted)
  → billable
  → max(billable, minimumOrderPrice)
```

Setup fees (`FinishTypeSetupFee`, `FoldTypeSetupFee`, etc.) are added **after** all multipliers.

---

## Scala 3 Style

- Prefer `enum` over `sealed trait` + `case class` hierarchies for ADTs.
- Use `extension` methods inside companion objects of opaque types.
- Use `given`/`using` for type class instances.
- Indentation syntax (no braces) is used throughout — stay consistent.
- `-Xmax-inlines:64` is set; keep inlining reasonable.

