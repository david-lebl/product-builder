---
name: scala-scaffold-domain-model
description: >
  Scaffold a domain model element — an entity, value object, or opaque ID
  with a smart constructor — following this project's Scala 3 domain
  modeling conventions. Use when the user asks to add a new domain type.
  Trigger phrases: "add a domain model", "new entity", "create an ID type",
  "scaffold a value object", "add an opaque type for X", "smart constructor
  for Y".
---

# Scaffold a Domain Model Element

Authoritative guide: [`02-domain-modeling.md`](../../../docs/dev_guides/02-domain-modeling.md). This skill is a procedural summary — read the guide when a decision is ambiguous.

Existing templates in this repo (read these before writing — they show the exact style):

- Opaque IDs: [`modules/domain/src/main/scala/mpbuilder/domain/model/ids.scala`](../../../modules/domain/src/main/scala/mpbuilder/domain/model/ids.scala)
- Entity with identity/data split and extensions: [`modules/domain/src/main/scala/mpbuilder/domain/model/category.scala`](../../../modules/domain/src/main/scala/mpbuilder/domain/model/category.scala)
- Plain entity: [`modules/domain/src/main/scala/mpbuilder/domain/model/basket.scala`](../../../modules/domain/src/main/scala/mpbuilder/domain/model/basket.scala)

## 1. Pick the Shape

Ask one question first: **what kind of thing is this?**

| Thing | Shape |
|---|---|
| An identifier (no invariants) | Opaque type wrapping `UUID` / `String` |
| An identifier with format rules (e.g. SKU pattern) | Opaque type + smart constructor |
| A single primitive with invariants (`Ratio`, `Quantity`) | Opaque type + smart constructor, or `case class private` |
| Two-or-more fields, no lifecycle | `final case class` (value object) |
| Identity + state that evolves | `case class Entity(id: Id, data: Data)` (entity) |
| Closed set of alternatives | `enum` (sum type) |

If the type has a closed set of shapes with different fields, use `enum` with parameterised cases — don't squash into a case class with `Option` fields. See `02-domain-modeling.md` §4.

## 2. Placement

- **Shared across a bounded context**: flat in the `impl` package of `*-core`. Chained package clause brings it into scope for all sibling files.
- **Owned by one aggregate**: nested in the aggregate's companion object.
- **Public identifier / DTO**: root package of `*-core` (visible outside the context).

In this project (single `domain` module), place under `modules/domain/src/main/scala/mpbuilder/domain/model/`.

## 3. Opaque ID — Template

```scala
opaque type <Name>Id = <Underlying> // e.g. UUID, String, Long
object <Name>Id:
  def apply(value: <Underlying>): Validation[String, <Name>Id] =
    // … validate: non-empty, matches regex, within range, etc.
    if valid then Validation.succeed(value) else Validation.fail("<reason>")

  /** Bypasses validation — use only in tests and static sample data. */
  def unsafe(value: <Underlying>): <Name>Id = value

  extension (id: <Name>Id) def value: <Underlying> = id
```

Mirror the exact style of `ids.scala`. In ZIO-effect modules (per the guides' target architecture), use `Either[ValidationError, T]` instead of `Validation`.

## 4. Value Object — Template

```scala
final case class <Name>(
  field1: Type1,
  field2: Type2
)
```

No invariants beyond field types → nothing else needed. Invariants that span fields → make the primary constructor `private` and expose a smart constructor:

```scala
final case class <Name> private (field1: Type1, field2: Type2)
object <Name>:
  def apply(field1: Type1, field2: Type2): Validation[String, <Name>] = …
```

In Scala 3, `private` on the primary constructor also makes `.copy` and `new` private, so the smart constructor is the only path in.

## 5. Entity — Template

```scala
final case class <Name>(id: <Name>.Id, data: <Name>.Data)
object <Name>:
  opaque type Id = UUID
  object Id:
    def apply(v: UUID): Id = v
    def unsafe(v: UUID): Id = v
    extension (id: Id) def value: UUID = id

  final case class Data(
    // mutable-feeling fields go here; identity is stable, data evolves
  )
```

Gives you `order1.id == order2.id` (identity compare) and `order1.data == order2.data` (state compare) for free.

## 6. Illegal-States Check

Before writing, answer these out loud. Fix any that come back wrong:

- Can two values of this type be constructed that represent contradictory state? If yes, redesign — use a sum type or narrow the type.
- Is there an `Option[A]` + a boolean flag / another `Option[B]` that are mutually exclusive? Replace with an `enum`.
- Is there a collection that must be non-empty? Use `NonEmptyChunk`.
- Is there a primitive parameter that's swappable with a sibling parameter of the same primitive type? Introduce distinct opaque types.

## 7. Deliverable

1. Create the file under the correct package.
2. Use chained package clauses if inside `impl`.
3. Add Scaladoc only where the name+signature don't explain the invariant (see `scala-scaladoc` skill).
4. If the type is part of a service's signature, re-check that `*-core` still depends only on `commons`.
5. Compile: `mill domain.jvm.compile` (or the containing module's compile target).
6. If adding a new public ID type, mention it in the module's README / INDEX if it's load-bearing; otherwise skip — the code is the doc.

## 8. Out of Scope

- Writing tests for the smart constructor — that belongs in the test suite, not this skill. (Though a property-based round-trip test is a good follow-up.)
- JSON codecs — add in the HTTP layer (`impl.http/`), not next to the domain type.
- DAO / persistence mapping — see `05-persistence.md` and the `scala-scaffold-service` / persistence pieces.