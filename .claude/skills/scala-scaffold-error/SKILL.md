---
name: scala-scaffold-error
description: >
  Create a new error enum for a bounded context, or add a new variant to an
  existing one, following the Code + httpStatus pattern. Use when the user
  asks to add an error case, define errors for a new context, or wire up
  HTTP status mapping. Trigger phrases: "new error", "add an error case",
  "create an error enum", "add an error for X context", "what error should
  I use for …".
---

# Scaffold an Error Enum

Authoritative guide: [`03-error-model.md`](../../../docs/dev_guides/03-error-model.md). HTTP implications: [`10-api-design.md`](../../../docs/dev_guides/10-api-design.md).

Existing template to mirror: [`modules/domain/src/main/scala/mpbuilder/domain/service/BasketError.scala`](../../../modules/domain/src/main/scala/mpbuilder/domain/service/BasketError.scala).

## 1. Decide: New Enum or New Variant?

- **New variant** in an existing context's error enum when a service in that context can fail in a new way. Don't create a second enum for the same context.
- **New enum** when you are introducing a new bounded context. One enum per context — there is no global `AppError` (see §1 of the guide).
- If the failure is **infrastructure** (DB down, timeout, network), it is **not** a domain error — it travels through ZIO's defect channel via `.orDie`. Don't add it to the enum.

Ask: can the caller do something meaningful in response (show a specific message, retry with different input, take an alternative path)? If yes → domain error. If no → defect.

## 2. Template (new enum in a new context)

Place in the **public package** of the context's `*-core`:

```scala
package com.myco.<ctx>

enum <Ctx>Error(val code: <Ctx>Error.Code, val details: String):

  /** Human-readable rendered message. */
  def message: String = s"[${code.value}] $details"

  case <CaseA>
    extends <Ctx>Error(Code.<CaseA>, "<describe what went wrong>")

  case <CaseB>(field: Type)
    extends <Ctx>Error(Code.<CaseB>, s"<…includes $field>")

end <Ctx>Error

object <Ctx>Error:

  enum Code(val value: String, val httpStatus: Int):
    case <CaseA> extends Code("<PFX>-001", 422)
    case <CaseB> extends Code("<PFX>-002", 404)
```

**Conventions:**

- **Code prefix** = short context abbreviation in upper-case (`ORD-`, `SHP-`, `USR-`). Pick a 3-letter abbreviation that isn't already used.
- **Numbers** start at 001 and increment. Never reuse a retired number — add a new one.
- **httpStatus** is a hint. The HTTP mapper can override in edge cases, but default to:
  - `400 Bad Request` — malformed input you couldn't parse at all (rare — parse earlier instead).
  - `404 Not Found` — entity lookup failed.
  - `409 Conflict` — operation incompatible with current state (`OrderAlreadyCancelled`, `InsufficientStock`).
  - `422 Unprocessable Entity` — input was well-formed but semantically invalid (`EmptyItemList`, `InvalidAddress`).
  - `402 Payment Required` — payment-specific refusal.
- **Payload in the variant** should be enough to render a precise UI message or dashboard entry without parsing strings (`InsufficientStock(sku, requested, available)`, not `InsufficientStock(msg: String)`).

## 3. Localized Error Messages (this repo's style)

The existing `BasketError.scala` uses `message(lang: Language)` returning `LocalizedString` (EN/CS). If you're adding to a context that already localizes, match that pattern. If the new context is English-only or uses the `details: String` style from the guide, say so explicitly in the Scaladoc so future variants are consistent.

## 4. Adding a New Variant to an Existing Enum

1. Open the enum file.
2. Add the `case …` line following the existing style (keep the order — usually logical grouping, then append-only for new cases).
3. Add the matching entry to the `Code` enum with the next unused number.
4. If the variant appears in a public `*-core` service signature, grep for callers that pattern-match exhaustively — the compiler will fail them, which is the point. Fix the matches.
5. If the HTTP mapper has bespoke handling, extend it.

## 5. HTTP Mapping

One place, one time: the HTTP layer's error mapper. Default shape:

```scala
def map<Ctx>Error(e: <Ctx>Error): (Int, ErrorResponse) =
  (e.code.httpStatus, ErrorResponse(code = e.code.value, message = e.message))

final case class ErrorResponse(code: String, message: String)
```

Lives in `<ctx>-infra/.../impl/http/ErrorMapper.scala`. Never in the service, never in the domain.

## 6. Testing the New Error

For every new case, add at least one test in the relevant `*Spec` file:

```scala
test("fails with <CaseA> when <condition>"):
  for result <- <Ctx>Service.<op>(badInput).exit
  yield assert(result)(fails(equalTo(<Ctx>Error.<CaseA>)))
```

For parameterised variants, use `isSubtype[<Ctx>Error.<CaseB>](hasField("field", _.field, equalTo(expected)))`.

Also assert the code + httpStatus pair for any new entry, so frontend consumers know their contract is stable:

```scala
test("<CaseA> carries stable code and status"):
  val e = <Ctx>Error.<CaseA>
  assertTrue(e.code.value == "<PFX>-001", e.code.httpStatus == 422)
```

## 7. Anti-Patterns (Reject These)

- `throw new IllegalArgumentException("...")` in domain code → fail with the enum instead.
- `IO[Throwable, A]` on a service method → use `IO[<Ctx>Error, A]`.
- String-only errors (`IO[String, A]`) → always an enum.
- Global `AppError` shared across contexts → one enum per context.
- Exposing another context's error type upstream → translate in the anti-corruption adapter.

## 8. Cross-References

- When a consuming context receives another context's error through a port, the adapter translates (see `03-error-model.md` §4.2 and `12-cross-context-coupling.md`).
- For accumulated validation errors, model the specific validator to return `IO[NonEmptyChunk[<Ctx>Error], A]` with `ZIO.validate`.