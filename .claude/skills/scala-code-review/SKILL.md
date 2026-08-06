---
name: scala-code-review
description: >
  Structured review of Scala code against the project's development guidelines
  in docs/dev_guides/. Use this skill when the user asks to review a diff, a
  branch, a file, or a proposed change for conformance. Trigger phrases:
  "review this code", "guideline review", "does this follow our conventions",
  "check before merging", "is this idiomatic", "review against the guides".
---

# Scala Code Review

Guides live in [`docs/dev_guides/`](../../../docs/dev_guides/) and are the source of truth. This skill is a checklist that maps intent → guide section, so reviews stay consistent.

## 1. Scope the Review

Before reading code, establish:

- **What changed?** Use `git diff` / `git log` on the target branch. Don't review files the user didn't touch.
- **What layer?** Core domain? Infra adapter? HTTP? Tests? The applicable checklist depends on this.
- **What was the intent?** PR description, commit messages, or ask the user if unclear.

## 2. Checklist by Concern

Walk the diff once per concern. Not every concern applies to every change — skip ones that don't.

### Domain modeling — see [`02-domain-modeling.md`](../../../docs/dev_guides/02-domain-modeling.md)

- No primitives crossing a domain boundary (String/Int/Boolean). Every concept has its own type.
- IDs are opaque types with `apply`, `.value` extension, and a smart constructor returning `Either[ValidationError, T]` (or `Validation` in pure-domain modules).
- Entities split as `Entity(id, data)`; values are `final case class`.
- No `Option` + `assert` combinations — use sum types to make illegal states unrepresentable.
- Collections that must be non-empty use `NonEmptyChunk`, not `List` with runtime checks.
- Types owned by one aggregate live in its companion; flat under `impl` only when shared.

### Error model — see [`03-error-model.md`](../../../docs/dev_guides/03-error-model.md)

- One `enum` per bounded context, in the public package, with nested `Code` enum carrying `value: String` and `httpStatus: Int`.
- Code prefix matches context (`ORD-`, `SHP-`, …). Numbers not reused.
- Domain errors flow through ZIO's typed channel (`IO[ContextError, A]`). Infra failures use `.orDie` at the adapter boundary.
- No `IO[Throwable, A]`, no `IO[String, A]`, no `throw` in domain code.
- No foreign context's error type exposed — translate in adapters.
- Logging is at the HTTP/event boundary, not inside the domain.

### Service design — see [`04-service-design-capabilities.md`](../../../docs/dev_guides/04-service-design-capabilities.md) and [`03-services-capabilities-style.md`](../../../docs/dev_guides/03-services-capabilities-style.md)

- Trait in the public package; `Live` class `private[<context>]` in `impl`.
- Companion object exposes ZIO accessors (`OrderService.checkout(...)(using s)`), so callers never summon the trait manually.
- Service method signatures use **public DTOs**, not domain entities.
- Parse in the `Live`: `DTO → domain → logic → persist → view out`.
- Verb-first names (`checkout`, `cancelOrder`). No `process`, `handle`, `execute`.
- Stateless — no `var`, no mutable fields. All state in repos or config.
- `val layer: URLayer[Deps, Service] = ZLayer.fromFunction(Live.apply)` in the Live's companion.

### Persistence — see [`05-persistence.md`](../../../docs/dev_guides/05-persistence.md)

- Repository port speaks the domain: `UIO[Option[Order]]`, not `Task[Option[OrderRecord]]`.
- No intermediate `OrderRecord`. Domain entity in, domain entity out.
- DAO lives in `impl.<tech>/`, is `private[<tech>]`, has `fromDomain`/`toDomain`.
- Adapter calls `.orDie` at its own boundary, so `Throwable` never leaks upward.
- Domain-meaningful DB errors (e.g. duplicate key) are caught specifically and translated to the domain error enum; everything else is `.orDie`.
- In-memory implementation exists in `*-core` for tests, trivial `Ref[Map[Id, Entity]]`.

### Bounded contexts — see [`06-bounded-contexts.md`](../../../docs/dev_guides/06-bounded-contexts.md) and [`12-cross-context-coupling.md`](../../../docs/dev_guides/12-cross-context-coupling.md)

- Dependency rules: `*-core` depends only on `commons`; `*-infra` depends on its own `*-core` plus other `*-core` for adapters; no core → core; no infra → infra.
- Anti-corruption layer (port + snapshot + adapter in `impl.adapters/`) when calling another context's service.
- The consumer never imports another context's domain entity or error enum directly.
- Cross-context events are fire-and-forget — async, not request/response.

### API design — see [`10-api-design.md`](../../../docs/dev_guides/10-api-design.md)

- Routes live in `impl.http/` inside `*-infra`, not in `*-core`.
- Error → HTTP mapping at the route boundary, driven by `error.code.httpStatus`. Never in the service.
- Response shape is `{ "code": "...", "message": "..." }`.
- JSON codecs on public DTOs, not on domain entities.

### Testing — see [`09-testing.md`](../../../docs/dev_guides/09-testing.md)

- In-memory repos/stubs, no mocks.
- Error cases asserted with `.exit` + `fails(equalTo(ErrorEnum.Case))`.
- One test per error enum case.
- Test `.provide(...)` block uses `ULayer` test doubles only.
- Fresh state per test via ZLayer reconstruction, not shared mutable fixtures.

### DI & config — see [`07-dependency-injection.md`](../../../docs/dev_guides/07-dependency-injection.md), [`08-configuration.md`](../../../docs/dev_guides/08-configuration.md)

- Every implementation exposes `val layer`. No ad-hoc construction in `Main`.
- Config is a `ZLayer`, declared as a dependency like any other.
- Secrets use the `Secret` opaque type — no raw `String` password fields.

## 3. How to Write the Review

- Walk the diff concern-by-concern; when you find something to flag, cite the exact `file:line` and the guide section it violates.
- Distinguish **must fix** (breaks a guideline) from **consider** (style/ergonomic). Call this out explicitly.
- If something looks novel but justified, note why. Don't flag conformance for its own sake.
- End with a short summary: green / yellow / red, and the top 1–3 items the user should address first.

## 4. Out of Scope for This Skill

- Running the build/tests — defer to `scala-build`.
- Scaladoc drift — defer to `scala-scaladoc`.
- Security review — defer to the `security-review` plugin skill.
- Writing the fix — the review *finds* the issues. Implementation is a follow-up task the user explicitly approves.