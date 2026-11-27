---
name: scala-scaffold-bounded-context
description: >
  Create a new bounded context in this Scala monorepo — a *-core + *-infra
  module pair, canonical package layout, build wiring, and the initial
  skeleton (entity, error enum, service trait, repo port, Live, layer,
  config). Use when the user asks to spin up a new domain area. Trigger
  phrases: "new bounded context", "create a new module for X", "scaffold a
  new context", "set up a Shipping context", "add a new *-core / *-infra".
---

# Scaffold a Bounded Context

Primary references:
[`01-project-structure.md`](../../../docs/dev_guides/01-project-structure.md) ·
[`06-bounded-contexts.md`](../../../docs/dev_guides/06-bounded-contexts.md) ·
[`07-dependency-injection.md`](../../../docs/dev_guides/07-dependency-injection.md) ·
[`08-configuration.md`](../../../docs/dev_guides/08-configuration.md) ·
[`12-cross-context-coupling.md`](../../../docs/dev_guides/12-cross-context-coupling.md)

## 1. Before Scaffolding: Merge Check

Ask: **should this be a new context at all?** Per `06-bounded-contexts.md` §4, if the proposed scope shares more than ~50% of its vocabulary with an existing context, merge instead. Only proceed if:

- The new area has a distinct ubiquitous language.
- Its data model changes independently of any existing context.
- Merging it would bloat an existing `*-core` past comprehensibility.

If you proceed, capture the decision (one line in the PR / commit) so the reviewer understands the choice.

## 2. Confirm Inputs

Ask the user (or confirm from context):

- **Context name** in PascalCase (`Ordering`) and kebab-case (`ordering`).
- **Root package** (`com.myco.ordering` or match the project's pattern — check an existing `*-core`).
- **Other contexts it needs to call** (drives which anti-corruption adapters to stub).

## 3. Create the Core Module

```
modules/<ctx>-core/
└── src/main/scala/<pkg-path>/
    ├── <Ctx>Service.scala              ← trait + accessor object (public)
    ├── <Ctx>Error.scala                ← enum + Code enum (public)
    ├── <Ctx>Id.scala                   ← shared opaque ID (public)
    ├── <verb>Input.scala               ← public DTO(s)
    ├── <Noun>View.scala                ← public read-model(s)
    └── impl/
        ├── <Aggregate>.scala           ← private[<ctx>] entity
        ├── <Aggregate>Repository.scala ← private[<ctx>] port
        ├── <Ctx>ServiceLive.scala      ← private[<ctx>] implementation + val layer
        ├── InMemory<Aggregate>Repository.scala
        └── <Ctx>Config.scala           ← optional, if there's domain-level config
```

Every file under `impl/` uses the **chained package clause**:

```scala
package com.myco.<ctx>
package impl
```

Types in `impl/` are `private[<ctx>]`. See §1 of [01-project-structure.md](../../../docs/dev_guides/01-project-structure.md).

## 4. Create the Infra Module

```
modules/<ctx>-infra/
└── src/main/scala/<pkg-path>/
    └── impl/
        ├── postgres/                 ← or whatever persistence tech
        │   ├── Postgres<Aggregate>Repository.scala
        │   ├── <Aggregate>DAO.scala
        │   └── PostgresConfig.scala
        ├── http/
        │   ├── <Ctx>Routes.scala
        │   ├── ErrorMapper.scala
        │   └── Codecs.scala
        └── adapters/                 ← one adapter per consumed context
            └── <OtherCtx>Adapter.scala
```

Chained package clause applies here too (`package impl`, `package postgres`, etc.). DAOs are `private[<tech>]`.

## 5. Wire the Build

**Mill** (`build.mill`):

```scala
object `<ctx>-core` extends CommonScalaModule {
  def moduleDeps = Seq(commons)
}
object `<ctx>-infra` extends CommonScalaModule {
  def moduleDeps = Seq(`<ctx>-core`) // + other-ctx-core for adapters
}
```

**sbt** (`build.sbt`):

```scala
lazy val <ctx>Core  = project.in(file("modules/<ctx>-core")).dependsOn(commons)
lazy val <ctx>Infra = project.in(file("modules/<ctx>-infra")).dependsOn(<ctx>Core /*, otherCoreForAdapters*/)
lazy val app        = (existing).dependsOn(<ctx>Infra)
```

**Dependency rules** (hard constraints — verify before committing):

| Module | May depend on | Must NOT depend on |
|---|---|---|
| `*-core` | `commons` only | Other `*-core`, any `*-infra` |
| `*-infra` | Own `*-core`, other `*-core` (adapters only) | Other `*-infra` |
| `app` | All `*-infra` | — |

## 6. Initial Skeleton Content

Delegate the individual files — don't inline everything in this skill:

- Error enum + `Code` → invoke `scala-scaffold-error` with the context name.
- Service trait + `Live` + layer → invoke `scala-scaffold-service`.
- Domain entity + opaque ID → invoke `scala-scaffold-domain-model`.
- `ZLayer` + optional config → invoke `scala-scaffold-di-config`.

When running end-to-end, walk in this order: **error → domain model → service → di-config**. Each step has enough to compile independently.

## 7. Main Wiring

Add the new context to the app's provide block:

```scala
program.provide(
  <Ctx>ServiceLive.layer,
  Postgres<Aggregate>Repository.layer,        // or InMemory in dev
  <Ctx>Config.layer,
  // + adapter layers for consumed contexts
)
```

Prefer a per-context layer bundle (`<Ctx>Layers.live`, `<Ctx>InfraLayers.postgres`) so `Main.provide(...)` stays readable. See `07-dependency-injection.md` §5.

## 8. Verification

1. `mill <ctx>-core.compile` + `mill <ctx>-infra.compile` — both green.
2. `mill <ctx>-core.test` — a smoke test of the Live service with in-memory repo passes.
3. Re-read the dependency-rules table (§5) against your diff — no core→core, no infra→infra.
4. Run `scala-code-review` on the new files before committing.

## 9. What This Skill Does Not Do

- Implement actual domain logic. It produces a compiling skeleton with `???` in the Live.
- Set up HTTP routes beyond an `ErrorMapper` stub — defer to follow-up work.
- Generate a real persistence adapter — the DAO/SQL is left as a placeholder.
- Commit the changes. Hand off to the user when the skeleton compiles.