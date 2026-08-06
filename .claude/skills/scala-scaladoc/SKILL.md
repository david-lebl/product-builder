---
name: scala-scaladoc
description: >
  Verify that Scaladoc on public Scala APIs is present, accurate, and in sync
  with the code. Use this skill when the user asks to check, update, or write
  Scaladoc, or as a follow-up to API-level changes. Trigger phrases:
  "check scaladoc", "update scaladoc", "are docs current", "scaladoc review",
  "document this service", "docs drift".
---

# Scaladoc Maintenance

Keep Scaladoc on **public** surface area accurate. Private internals are self-documenting through naming and tests.

## 1. What Requires Scaladoc

| Artefact | Scaladoc expected? |
|---|---|
| Public service trait (`trait OrderService`) | Yes — one-line summary per method |
| Public DTOs (`CheckoutInput`, `OrderView`) | Class-level summary; individual fields only if non-obvious |
| Public error enum + `Code` enum | Class-level summary; per-case doc when the triggering condition isn't obvious from the name |
| Public opaque IDs + smart constructors | `apply` doc stating what it validates |
| Published events (`OrderEvent`) | Class-level + per-case, since consumers in other contexts read these |
| `ZLayer` values (`val layer`) | One line: what it builds, what it requires |
| `impl`-scoped types (`private[ordering]`) | Usually **no** — the name + signature is enough |
| Obvious case class fields | **No** — `customerId: CustomerId` doesn't need "the customer ID" |

## 2. Conventions

- Use `/** … */` doc comments, not `//` line comments.
- Summary sentence first. Then blank line. Then `@param`, `@return`, `@throws` (rare — defect channel), and for services `@note` on error cases worth surfacing.
- Document **what the method does**, not how. Implementation details belong in code.
- For ZIO methods, don't re-explain `IO`/`UIO`. Do name the error type if it's informative: "Fails with [[OrderError.EmptyItemList]] when the input has no items."
- `Validation[E, A]` in pure-domain modules: note whether errors accumulate (they do — state it once at the trait level, not on every method).

## 3. Drift Detection

When you review an API file, look for these drift signals:

1. **Renamed symbol still referenced in doc.** `@param customerId` when the parameter is now `buyerId`.
2. **New parameter, undocumented.** The signature has four params, the doc has three `@param` lines.
3. **Removed parameter still documented.**
4. **Changed return type, stale description.** "Returns the saved order" when the method now returns a view.
5. **Error case added to the enum, not mentioned in service doc.** New `OrderError.TooManyItems` exists, but `checkout`'s doc says it fails with `EmptyItemList` or `InvalidAddress`.
6. **Behaviour inverted.** The doc says "throws if empty" but the code returns `ZIO.fail(…)`.

## 4. Procedure

1. **Find the surface.** Glob the public package of the changed context:
   `modules/<context>-core/src/main/scala/<root-pkg>/*.scala` — everything outside `impl/`.
2. **For each file, read the doc and signature side-by-side.** Flag drift from the list above.
3. **For new public APIs without any Scaladoc**, write a draft following §1–2. Keep it short — one sentence is often enough.
4. **When updating, don't rewrite the whole comment.** Change only what's wrong; preserve the author's voice.
5. **Don't add docs to private/impl types** unless the user asks. Scaladoc on `private[ordering]` helpers adds noise.

## 5. When to Invoke This Skill

- After scaffolding a new service, error, or domain type (follows `scala-scaffold-*`).
- After refactoring that changes signatures, parameter names, or error channels.
- As part of `post-work-docs` when API-level changes were made.
- When the user says the docs "feel stale" or before publishing a public release.

## 6. What Not to Do

- Don't generate Scaladoc by paraphrasing the method name (`def checkout` → "Checks out."). That adds no information and rots immediately.
- Don't document field-by-field unless a field has a non-obvious unit, format, or invariant.
- Don't update Scaladoc in files the user did not change — scope each pass to the current task.
- Don't put example code blocks in Scaladoc unless the API is genuinely tricky; prefer a pointer to a test file.

See [`04-service-design-capabilities.md`](../../../docs/dev_guides/04-service-design-capabilities.md) for the public/internal boundary that defines what "public" means here.