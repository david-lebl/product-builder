# New Domain Type

Scaffold a new opaque ID or value type following project conventions.

## Opaque ID (string-based)

```scala
opaque type <TypeName>Id = String
object <TypeName>Id:
  def apply(value: String): Validation[String, <TypeName>Id] =
    if value.nonEmpty then Validation.succeed(value)
    else Validation.fail("<TypeName>Id must not be empty")

  def unsafe(value: String): <TypeName>Id = value   // tests / sample data only

  extension (id: <TypeName>Id) def value: String = id
```

Add to: `modules/domain/src/main/scala/mpbuilder/domain/model/ids.scala`

## Value type (e.g. Money-like)

```scala
opaque type <ValueType> = BigDecimal
object <ValueType>:
  def apply(value: BigDecimal): <ValueType> = value
  def apply(value: String): <ValueType>    = BigDecimal(value)

  val zero: <ValueType> = BigDecimal(0)

  extension (v: <ValueType>)
    def value: BigDecimal = v
    def +(other: <ValueType>): <ValueType> = v + other
    def rounded: <ValueType> = v.setScale(2, BigDecimal.RoundingMode.HALF_UP)
```

## Rules

- Smart constructors always return `Validation[String, T]` — never `Either` or raw `T`.
- `.unsafe(...)` is for tests and `sample/` data only.
- Extension methods go inside the companion object.
- Import `zio.prelude.*` to bring `Validation` into scope.

