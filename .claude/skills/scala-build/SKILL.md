---
name: scala-build
description: >
  Build and workflow commands for this Scala 3 monorepo. Use this skill when
  the user asks to compile, run tests (all or a single suite), build the JS
  bundle, check formatting, or run a pre-commit sanity pass. Trigger phrases
  include: "compile", "build", "run tests", "test this", "testOnly",
  "fastLinkJS", "fullLinkJS", "watch", "format", "is the project green",
  "pre-commit check".
---

# Scala Build & Workflow

Authoritative command list is in [`CLAUDE.md`](../../../CLAUDE.md). Always prefer **Mill** (primary) and fall back to **sbt** only if the user explicitly asks.

## 1. Compile

| Target | Mill | sbt |
|---|---|---|
| Domain (JVM) | `mill domain.jvm.compile` | `sbt domainJVM/compile` |
| Domain (Scala.js) | `mill domain.js.compile` | (cross-compile via domainJVM/compile not supported) |
| UI (Scala.js) | `mill ui.compile` | `sbt ui/compile` |
| UI framework only | `mill ui-framework.compile` | `sbt uiFramework/compile` |

Run in parallel if the user asks to verify multiple targets — they are independent.

## 2. Test

| Intent | Mill | sbt |
|---|---|---|
| All domain tests | `mill domain.jvm.test` | `sbt domainJVM/test` |
| Single suite (pattern) | `mill 'domain.jvm.test.testOnly *PriceCalculatorSpec'` | `sbt "domainJVM/testOnly *PriceCalculatorSpec"` |

Pattern matching uses `*` on suite name. Always quote the Mill command because of the `*`. If the user gives an FQN, use it; otherwise infer the shortest unique suffix.

See [`docs/dev_guides/09-testing.md`](../../../docs/dev_guides/09-testing.md) for authoring conventions (in-memory layers, `.exit`+`fails(…)` for error assertions, no mocks). When the user asks "do the tests pass?" just run them — don't rewrite anything first.

## 3. JS Bundle

| Mode | Mill | sbt |
|---|---|---|
| Dev (fast, sourcemapped) | `mill ui.fastLinkJS` | `sbt ui/fastLinkJS` |
| Production (optimised) | `mill ui.fullLinkJS` | `sbt ui/fullLinkJS` |
| Watch mode | (re-run manually, or a Mill watcher) | `sbt ~ui/fastLinkJS` |

Output: `out/ui/fastLinkJS.dest/main.js` (Mill) or the sbt equivalent under `target/`.

## 4. Format

There is currently **no `.scalafmt.conf`** in this repo. If the user asks to format:

1. Tell them no formatter is configured.
2. Ask whether they want to (a) set one up (separate task), (b) rely on the compiler's style (`-feature`, `-deprecation`), or (c) skip.

Do not silently run `scalafmt` — it will pick up an upstream default config and reformat the whole tree.

## 5. Pre-Commit Sanity

Before the user commits, if they ask for a sanity check:

1. `mill domain.jvm.compile` + `mill ui.compile` — both must succeed.
2. `mill domain.jvm.test` — must be green.
3. Report the total test count (compare against baseline in `CLAUDE.md` or memory if known).
4. If changes touch UI, also `mill ui.fastLinkJS` to catch Scala.js-only failures.

Do not attempt to commit from this skill. Hand off to the `commit-commands:commit` skill (or whatever commit tooling the user invokes) once the sanity pass is green.

## 6. What to Report

- Exit code + the last 20–40 lines of output on failure. On success, a one-line confirmation plus test count if tests ran.
- If a compile fails, cite the file:line from the error and stop — do not "fix" on a build-skill invocation.