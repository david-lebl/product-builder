---
name: scala-scaffold-service
description: >
  Scaffold a new service in a bounded context — public trait + ZIO accessor
  companion, private[<ctx>] Live implementation, repository port wiring, and
  the ZLayer. Use when the user asks to add a service or a new operation
  area to an existing context. Trigger phrases: "new service", "add a
  service", "scaffold a service for X", "create an OrderService",
  "service trait + Live".
---

# Scaffold a Service

Primary references:
[`04-service-design-capabilities.md`](../../../docs/dev_guides/04-service-design-capabilities.md) ·
[`03-services-capabilities-style.md`](../../../docs/dev_guides/03-services-capabilities-style.md) ·
[`07-dependency-injection.md`](../../../docs/dev_guides/07-dependency-injection.md) ·
[`05-persistence.md`](../../../docs/dev_guides/05-persistence.md) (for repository ports).

Existing template in this repo (pure-Validation style — no ZIO effects): [`modules/domain/src/main/scala/mpbuilder/domain/service/BasketService.scala`](../../../modules/domain/src/main/scala/mpbuilder/domain/service/BasketService.scala). For the target architecture with ZIO effects + ZLayer, follow §1 of the capabilities guide.

## 1. Gather Inputs

- **Service name** (`OrderService`, `ShippingService`).
- **Context** it belongs to (determines package and module).
- **Operations** the user wants — verb-first names, one `def` each.
- **Dependencies**: which repos/ports/configs does it need?
- **Error type**: usually `<Ctx>Error`. If the enum doesn't exist yet, invoke `scala-scaffold-error` first.

## 2. Anatomy (Three Parts)

Every service has three parts — generate them together:

### Part A — Public trait (root package of `*-core`)

```scala
package com.myco.<ctx>

import zio.*

trait <Svc>:
  def <op1>(input: <Op1>Input): IO[<Ctx>Error, <Op1>View]
  def <op2>(id: <SharedId>): IO[<Ctx>Error, Unit]

object <Svc>:
  def <op1>(input: <Op1>Input)(using s: <Svc>): IO[<Ctx>Error, <Op1>View] = s.<op1>(input)
  def <op2>(id: <SharedId>)(using s: <Svc>): IO[<Ctx>Error, Unit] = s.<op2>(id)
```

The accessor companion (`using s: <Svc>`) is the capability-style pattern from [`03-services-capabilities-style.md`](../../../docs/dev_guides/03-services-capabilities-style.md). If the codebase already uses `ZIO.serviceWithZIO[Svc](_.op(x))` accessors, match that style instead.

### Part B — Live implementation (`impl` package of `*-core`)

```scala
package com.myco.<ctx>
package impl

import zio.*

private[<ctx>] final case class <Svc>Live(
  <dep1>: <Dep1>,          // e.g. repository port
  <dep2>: <Dep2>,          // e.g. idGen
  config: <Ctx>Config      // optional — see `scala-scaffold-di-config`
) extends <Svc>:

  override def <op1>(input: <Op1>Input): IO[<Ctx>Error, <Op1>View] =
    for
      parsed <- ZIO.fromEither(parse(input))           // DTO → domain
      _      <- ZIO.cond(…, (), <Ctx>Error.<Guard>)    // domain rule
      entity  = build(parsed)
      _      <- <dep1>.save(entity)                    // UIO — infra failures already .orDie'd in adapter
    yield toView(entity)
```

Data flow is always: **public DTO → parse → domain → logic → persist → view out**. All `parse`/`toView` helpers are `private` to the class.

### Part C — Layer (companion of the Live)

```scala
object <Svc>Live:
  val layer: URLayer[<Dep1> & <Dep2> & <Ctx>Config, <Svc>] =
    ZLayer.fromFunction(<Svc>Live.apply)
```

(If the project adopts `ZLayer.derive`, use that instead — it's the TODO noted in `07-dependency-injection.md` §2.)

## 3. Method Conventions

- **Verb-first names**: `checkout`, `cancelOrder`, `calculatePrice`. Never `process`, `handle`, `execute`, `doAction`.
- **Public DTOs in signatures**, not domain entities. Keep the domain model behind the `impl` boundary.
- **Error type is the context's enum**. Always `IO[<Ctx>Error, A]`, never `Task`, never `IO[String, A]`.
- **`UIO[List[…]]`** for read-only operations that genuinely can't produce a domain error.
- **Stateless**: no `var`, no mutable fields. Config and deps come in through the constructor.

## 4. Repository Port (If Needed)

If the service needs persistence, define the port in the **same `impl` package** as the Live:

```scala
private[<ctx>] trait <Agg>Repository:
  def save(e: <Agg>): UIO[Unit]
  def findById(id: <Agg>.Id): UIO[Option[<Agg>]]
```

Port speaks **domain entities**, not DAO records. See `05-persistence.md`. Also create the `InMemory<Agg>Repository` in the same package so tests can wire a `ULayer[<Agg>Repository]`.

The Postgres adapter lives in `<ctx>-infra/.../impl/postgres/` and is created separately (not part of this skill — follow-up task).

## 5. ZIO Accessor Style: Which One?

Two styles exist in the guides:

- **`using`-based accessors** (`03-services-capabilities-style.md`): `<Svc>.op(input)(using s: <Svc>)`. Requires the caller to have `<Svc>` in implicit scope.
- **`ZIO.serviceWithZIO` accessors** (`04-service-design-capabilities.md`): `<Svc>.op(input)` returns a `ZIO[<Svc>, ...]`.

Ask the user which style the project uses, or grep an existing service and match it. Don't invent a third style.

## 6. Minimal Test

Create a smoke test next to the skeleton:

```scala
suite("<Svc>Live")(
  test("<op1> on <happy-path>"):
    for result <- <Svc>.<op1>(goodInput)
    yield assertTrue(result.<field> == <expected>),

  test("<op1> fails with <error> on <bad-input>"):
    for result <- <Svc>.<op1>(badInput).exit
    yield assert(result)(fails(equalTo(<Ctx>Error.<Case>)))
).provide(
  <Svc>Live.layer,
  InMemory<Agg>Repository.layer,
  <Ctx>Config.test
)
```

## 7. Wire Into the App

Add the new service's layer to the app's `provide` block (or the per-context layer bundle). See [`scala-scaffold-di-config`](../scala-scaffold-di-config/SKILL.md) for composition strategies.

## 8. Verification Checklist

- [ ] `<Svc>` trait in the public package; `<Svc>Live` in `impl` with `private[<ctx>]`.
- [ ] Signatures use public DTOs, not domain entities.
- [ ] All domain errors flow through `IO[<Ctx>Error, A]`.
- [ ] No `throw`, no `Task` leaking.
- [ ] `val layer` exists on the Live companion.
- [ ] Smoke test compiles and passes with in-memory repo.
- [ ] `mill <module>.compile` green.
- [ ] Run `scala-code-review` against the diff before committing.

## 9. Out of Scope

- HTTP routes for the new service (see `10-api-design.md` and future `scala-scaffold-http-route` skill if needed).
- Postgres adapter implementation — create a stubbed port + in-memory impl; real adapter is a follow-up.
- Event publication — if the service emits domain events, the event ADT and consumer wiring are separate tasks.