---
name: scala-scaffold-di-config
description: >
  Wire a new ZLayer or add a configuration class following this project's
  per-module DI/config conventions. Use when the user asks to add DI plumbing
  or a new config. Trigger phrases: "add a ZLayer", "wire DI for X", "add
  config for X", "new configuration class", "layer graph", "hook this into
  Main", "fix the missing layer".
---

# Scaffold DI Layer or Configuration

Primary references:
[`07-dependency-injection.md`](../../../docs/dev_guides/07-dependency-injection.md) ·
[`08-configuration.md`](../../../docs/dev_guides/08-configuration.md).

## 1. ZLayer — Add a Layer for an Implementation

Every service, repository, adapter, and config class exposes a `val layer` in its companion object. The pattern:

```scala
// Constructor-based wiring (no initialisation side effects)
object <Impl>:
  val layer: URLayer[<Dep1> & <Dep2>, <Port>] =
    ZLayer.fromFunction(<Impl>.apply)
```

When construction needs side effects (DB connection, file load, schema check), use `ZLayer { ... }`:

```scala
object PostgresOrderRepository:
  val layer: RLayer[javax.sql.DataSource, OrderRepository] =
    ZLayer:
      for
        ds <- ZIO.service[javax.sql.DataSource]
        _  <- ZIO.attemptBlocking(ds.getConnection.close()) // fail-fast
      yield PostgresOrderRepository(ds)
```

**Layer type choice:**

| Type | When |
|---|---|
| `ULayer[A]` | No dependencies, no failure. In-memory impls, stubs, fixed values. |
| `URLayer[D, A]` | Dependencies `D`, no failure. The common case. |
| `TaskLayer[A]` | No dependencies, may fail (e.g. config load). |
| `RLayer[D, A]` | Dependencies `D`, may fail. |

Let the compiler infer when possible — only write the type explicitly on `val layer` where the module's public surface needs it.

## 2. Wiring a Missing Dependency

If the user says "missing layer for X":

1. Identify the service that needs X (the failing `.provide(...)` will name it).
2. Does X have a `val layer`? If not, add one per §1.
3. Add X's layer to the relevant `.provide` block (test setup, `Main`, or a per-context bundle).
4. Re-run the build — ZIO's compile-time DI checker will confirm the graph is now complete.

`ZLayer.Debug.mermaid` (drop into any `.provide` block temporarily) prints the layer graph as Mermaid — useful when the error is confusing.

## 3. Test Doubles

For every port, add an in-memory/stub `ULayer` so tests never need a real dependency:

```scala
object InMemory<Agg>Repository:
  val layer: ULayer[<Agg>Repository] =
    ZLayer(Ref.make(Map.empty[<Agg>.Id, <Agg>]).map(InMemory<Agg>Repository(_)))
```

```scala
object Stub<Ext>Port:
  def layer(fixed: Map[String, <Snapshot>]): ULayer[<Ext>Port] =
    ZLayer.succeed(new <Ext>Port:
      def get(id: String): Task[Option[<Snapshot>]] = ZIO.succeed(fixed.get(id)))

  val empty: ULayer[<Ext>Port] = layer(Map.empty)
```

## 4. Per-Module Layer Bundles

As the app grows, avoid a giant `.provide(...)` in `Main`. Each `*-core` and `*-infra` can expose a bundle:

```scala
// <ctx>-core
object <Ctx>Layers:
  val live: URLayer[<AllCoreDeps>, <Svc>] = <Svc>Live.layer
  val test: ULayer[<Svc>] =
    InMemory<Agg>Repository.layer ++ UUIDIdGenerator.layer >>> <Svc>Live.layer

// <ctx>-infra
object <Ctx>InfraLayers:
  val postgres: RLayer[javax.sql.DataSource, <Agg>Repository] = Postgres<Agg>Repository.layer
```

`Main` then composes bundles, not individual leaves. See §5 of `07-dependency-injection.md`.

## 5. Config — Add a New Config Class

### 5.1 Where does it live?

| Kind of config | Location |
|---|---|
| Domain-level (feature flags, thresholds, limits) | `impl` package of `*-core` |
| Infrastructure (DB URL, HTTP port, pool size) | `impl.<tech>` sub-package of `*-infra` |
| Cross-cutting (app name, logging level) | Closest containing module, or `commons` only if truly universal |

### 5.2 Template

```scala
package com.myco.<ctx>
package impl

private[<ctx>] final case class <Ctx>Config(
  maxItemsPerOrder: Int,
  defaultCurrency:  String,
  featureFoo:       Boolean
)

object <Ctx>Config:
  val layer: TaskLayer[<Ctx>Config] =
    ZLayer(ZIO.config(deriveConfig[<Ctx>Config].nested("<ctx>")))

  /** Test fixture — use in unit tests to avoid reading real config. */
  val test: ULayer[<Ctx>Config] =
    ZLayer.succeed(<Ctx>Config(maxItemsPerOrder = 100, defaultCurrency = "USD", featureFoo = false))
```

Services declare the config as a constructor dependency; `ZLayer.fromFunction(Live.apply)` wires it automatically.

### 5.3 Secrets

Sensitive fields (passwords, API tokens) must use the `Secret` opaque type:

```scala
final case class PostgresConfig(
  url:      String,
  username: String,
  password: Secret,   // redacts in toString and logs
  poolSize: Int
)
```

If `Secret` isn't defined in `commons` yet, add it:

```scala
// modules/commons/.../Secret.scala
opaque type Secret = String
object Secret:
  def apply(v: String): Secret = v
  extension (s: Secret)
    def value: String    = s
    def redacted: String = "***"
```

Override the containing case class's `toString` if defense-in-depth in logs matters.

### 5.4 Loading

This project uses `zio-config`. The `deriveConfig[T]` macro reads from the ZIO `Config` source (HOCON, env, or wherever the `ZLayer` provides it). Nest under a context-scoped path (`<ctx>`, or `<ctx>.db` for infra) so multiple contexts don't collide.

Never commit secrets — use `${?ENV_VAR}` substitution in HOCON or a secrets manager layer.

## 6. Wire Config into Main

```scala
program.provide(
  // Configs first
  <Ctx>Config.layer,
  PostgresConfig.layer,
  ServerConfig.layer,
  // Services/adapters
  <Svc>Live.layer,
  Postgres<Agg>Repository.layer,
  HikariDataSource.layer,
  Server.default
)
```

Invalid config → the layer fails → the app won't start. That's the desired behaviour.

## 7. Avoiding "God Wiring"

Symptoms and fixes in `07-dependency-injection.md` §6:

- `.provide(...)` spans 40+ lines → group by context (§4 here).
- Unclear dependency chain → drop `ZLayer.Debug.mermaid` in, inspect, remove.
- Circular reference complaint → usually means two contexts are peering without an ACL. See `12-cross-context-coupling.md`.

## 8. Verification

1. `mill <module>.compile` — compiler errors catch missing or wrong layers.
2. Check the layer type annotation matches the constructor signature (if you add a param, the layer's required environment changes — update it).
3. In tests, use the `test` layer for configs; do not rely on a real HOCON file.
4. If you added a new config path, document it briefly (one line in the module's README or a `application.conf` comment) so ops knows it exists.

## 9. Out of Scope

- Writing the HOCON / env files themselves — this skill scaffolds the Scala side.
- Secrets-manager integration (Vault, AWS Secrets Manager) — needs its own adapter.
- Runtime config reload — zio-config supports it; not the default in this project.
