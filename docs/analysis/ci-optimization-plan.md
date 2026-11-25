# CI / Build Pipeline — Review & Optimisation Plan

Status as of 2026-11-25. Part 1 records what was wrong and what has already been fixed
(no new infrastructure needed). Part 2 is the backlog for next time.

## Part 1 — Findings & fixes already applied

| # | Problem found | Impact | Fix applied |
|---|---|---|---|
| 1 | `ci.yml` triggered only on `main`, but every feature PR targets `develop`. | **CI never ran on PRs or on `develop` pushes** (last PR CI run: 2026-11-22). Broken builds/tests were only caught by the preview build, which runs no tests. | CI now runs on PRs and pushes to `main` **and** `develop` (+ `workflow_dispatch`), with per-PR `concurrency` that cancels superseded runs. |
| 2 | `deploy-preview.yml` skipped `actions/checkout` on `closed` events. | Every PR close/merge failed the preview cleanup with `fatal: not in a git directory`; stale previews accumulated under `gh-pages/pr-preview/`. | Checkout now always runs. Fork PRs (read-only token) are skipped. Concurrency keyed per PR number, queued (not cancelled) so a `gh-pages` push is never aborted midway. |
| 3 | Workflows installed Mill with `curl … > /usr/local/bin/mill`, hardcoding `1.1.3` in three places, instead of using the repo's `./mill`. | Version drift between CI and `.mill-version`; three places to bump. | All workflows call `./mill`, so `.mill-version` is the single source of truth. |
| 4 | `./mill` was a custom wrapper that downloaded the official launcher script to `~/.cache/mill/download/<version>`. The official launcher stores the **JVM Mill executable at that same path** whenever it can't use the native binary (glibc < 2.39, e.g. Ubuntu 22.04 / Debian 12, or a `1.1.3-jvm` version). | The two files overwrite each other: launcher recursion or a jar being `exec`'d as a shell script. It also ignored `MILL_VERSION`/`.config/mill-version` conventions. | `./mill` is now the official `mill-dist-1.1.3-mill.sh` launcher, committed as-is (the standard Mill setup). Bump with `echo X > .mill-version`; refresh the launcher from `https://repo1.maven.org/maven2/com/lihaoyi/mill-dist/X/mill-dist-X-mill.sh`. |
| 5 | Only `ci.yml` cached anything (Coursier only); the deploy workflows downloaded all dependencies on every run. The Copilot setup job cached `~/.cache/mill`, but nothing ever filled it. | Slow, network-bound deploys and agent setup. | `coursier/cache-action@v9` (Coursier + Mill caches, keyed on `build.mill`/`.mill-version`) in every workflow. Copilot setup runs `./mill __.prepareOffline` to pre-fetch Mill and all dependencies. |
| 6 | Site assembly copied and pasted between `deploy-pages.yml` and `deploy-preview.yml`; three separate Mill invocations ran one after another. | Duplication, and no parallel linking. | `scripts/assemble-site.sh` (also usable locally) with one Mill call `'{ui,ui-admin,ui-calculator,ui-editor}.fullLinkJS'`, so Mill links all four bundles (incl. the `ui-editor` Design Studio) in parallel. CI does the same with `fastLinkJS`. |
| 7 | Old action majors (`checkout@v4`, `setup-java@v4`) on the deprecated Node 20 runtime (runner warnings). | Future breakage. | `checkout@v7`, `setup-java@v6`, `upload-artifact@v7`. |
| 8 | No formatting/lint checks. | Inconsistent style; noisy diffs. | `.scalafmt.conf` (scalafmt 3.11.5, Scala 3 dialect, 120 cols) and a **report-only** `format` CI job (`./mill mill.scalalib.scalafmt/checkFormatAll`). It currently reports 251 misformatted files, so it can't be blocking yet (see 2.1). |
| 9 | Leftover sbt build (`project/build.properties`, `project/plugins.sbt`, sbt 1.12 / Scala.js 1.18) and sbt commands in `.github/copilot-instructions.md`. | Misleads agents/IDEs; Scala Steward and Metals would detect a second (dead) build. | Removed `project/`; Copilot instructions now list `./mill` commands. |
| 10 | Failed test runs left no reports. | Diagnosing failures meant reading raw logs. | CI uploads `out/**/test-report.xml` as an artifact on failure (kept 7 days). |

Useful local commands:

```bash
./mill mill.scalalib.scalafmt/checkFormatAll   # what CI checks
./mill mill.scalalib.scalafmt/reformatAll      # fix everything
scripts/assemble-site.sh _site                 # reproduce the Pages build locally
```

## Part 2 — Next steps (backlog)

Ordered roughly by value ÷ effort.

### 2.1 Make scalafmt blocking (no infra)

1. Merge or rebase the open PRs first: a full reformat conflicts with every open branch.
2. Run `./mill mill.scalalib.scalafmt/reformatAll` in a dedicated commit containing only that change.
3. Add that commit's SHA to `.git-blame-ignore-revs` so `git blame` and GitHub skip it.
4. Delete `continue-on-error: true` from the `format` job in `ci.yml`, then make **Format** a required check in branch protection.
5. Optional: `ScalafmtModule` mixed into the base traits gives per-module `__.checkFormat` / `__.reformat`.

### 2.2 Scalafix (no infra, needs build changes)

- Plugin: `com.goyeau::mill-scalafix::0.6.2` (Mill 1.x artifact `mill-scalafix_mill1_3`), loaded through a `//| mvnDeps:` header in `build.mill`; mix `ScalafixModule` into `BackendJvmModule`, `BackendJsModule` and the UI module traits.
- Semantic rules need SemanticDB (`-Xsemanticdb`, added by the plugin) and, for `RemoveUnused`, `-Wunused:all` in `commonScalacOptions`. Check first that Scala 3.9.0 works with the Scalafix release in use, and how much `-Wunused:all` slows compilation.
- Starting `.scalafix.conf`: `rules = [RemoveUnused, OrganizeImports, DisableSyntax, LeakingImplicitClassVal, NoValInForComprehension]`. Start with `OrganizeImports` (with `removeUnused = true`), then add `DisableSyntax` (`noNulls`, `noReturns`, `noFinalize`).
- CI: `./mill __.fix --check` in the `format` job, report-only first, same rollout as 2.1.
- Cheaper interim step: add `-Wunused:imports` (+ later `-Wunused:all`) to `commonScalacOptions` and fix the warnings, without Scalafix.

### 2.3 Split CI into stages and parallel jobs

The current `build` job compiles once and then runs everything serially. Once its runtime matters:

```
format ─┐
        ├─► test-core (shared, *-c-core, json, storefront, backoffice, sample) ─┐
compile ┤                                                                        ├─► link-js (fullLinkJS + size report)
        ├─► test-db   (*/02-o-db, platform, app — needs Docker)                  ┤
        └─► test-ui   (ui-core.jvm, ui-admin-core.jvm)                           ┘
```

- Hand compiled output to later jobs by uploading/downloading `out/` as an artifact, or with `actions/cache` keyed on `${{ github.sha }}`. Mill re-validates by input hashes, so a restored `out/` skips recompilation **if paths and JDK match** (same runner image).
- Choose suites with selectors: `./mill 'backend.__.02-o-db.test'`, `'backend.{shared.jvm,catalog.01-c-core.jvm,…}.test'`, `'{ui-core,ui-admin-core}.jvm.test'`.
- Matrix variant: `strategy.matrix.suite: [core, db, ui]` with one selector per entry.
- Measure first. On a 4-core runner, Mill already runs independent modules in parallel, so splitting mostly helps with fast feedback and clearer failure reports, not total minutes.

### 2.4 Incremental `out/` caching between runs

- Cache `out/` with `actions/cache`, key `mill-out-${{ runner.os }}-${{ hashFiles('build.mill', '.mill-version') }}-${{ github.sha }}`, restore-keys falling back to the same prefix without the SHA. This allows incremental Zinc compilation on PRs.
- Risks: `out/` can grow to gigabytes (each repo has a 10 GB cache limit), and a stale cache can occasionally poison it. Prune `out/**/*.dest` for link/assembly tasks before saving, and restore only on PRs while saving only from `develop`/`main` (cache-action `save-always: false`, or the split `actions/cache/restore` + `actions/cache/save`).

### 2.5 Mill-managed JDK (no infra)

- Switch `//| mill-jvm-version: system` to `//| mill-jvm-version: temurin:25` (or `25`). Mill then fetches JDK 25 through Coursier, so `actions/setup-java`, the JDK-detection logic in `scripts/dev.sh` and `.sdkmanrc` all become optional. Devs whose default JDK is 17 (the Mill daemon currently starts on whatever `java` is on PATH) stop hitting "class file version 69" errors.
- The JDK lands in the Coursier cache, so `coursier/cache-action` already covers it.

### 2.6 Dependency updates — Scala Steward (needs a token or GitHub App)

- Dependabot does not understand `build.mill`, so Scala Steward is the option. Run it as a scheduled workflow (`scala-steward-org/scala-steward-action@v2`, weekly cron). It needs a PAT or a GitHub App secret so its PRs trigger CI (PRs opened with the default `GITHUB_TOKEN` don't run workflows).
- Mill 1.x support: Steward detects Mill through `.mill-version` / `build.mill`. Check that it parses the `mvn"…"` interpolator and the `val kyoVersion = …` indirection, or use the Steward Mill plugin. It does pick up `.scalafmt.conf` `version`, and can bump `.mill-version`.
- Add `.scala-steward.conf`: group all `io.getkyo` artifacts into one PR (`pullRequests.grouping`), set `updates.pin` for Scala 3.9.x if needed, and point `pullRequests.frequency` / `updatePullRequests` at `develop` (`branches` input of the action).
- Dependabot **can** still update GitHub Actions versions: add `.github/dependabot.yml` with `package-ecosystem: github-actions`. Zero infra; good next step.

### 2.7 Pages / preview hardening

- Both deploy workflows push to `gh-pages` independently. A `main` deploy and a preview deploy can race (JamesIves retries, but not forever). Consider a shared job-level `concurrency: gh-pages-push` on the deploy step only, or migrate `main` to the official Pages artifact flow (`actions/upload-pages-artifact` + `actions/deploy-pages`). Previews would then need their own branch or an external host.
- Gate preview deploys on CI success (`workflow_run` on CI, or `needs:` when merged into one workflow) so broken PRs don't publish previews and the bundles aren't linked twice.
- Report bundle sizes (`du -h _site/**/main.js`) in the job summary (`$GITHUB_STEP_SUMMARY`) to catch Scala.js size regressions.

### 2.8 Other cheap wins

- Use a `pull_request` `paths-ignore` filter (`docs/**`, `*.md`, `docs_roadmap/**`, `guides/**`) to skip CI for docs-only PRs. Mind required checks: skipped workflows block merging unless a stub job reports success.
- Pin third-party actions (`rossjrw/*`, `JamesIves/*`, `coursier/*`) to commit SHAs for supply-chain safety, and let Dependabot bump them.
- Add `./mill __.compile` with `-Werror` (or `-Xfatal-warnings`) after the warnings are cleaned up.
- Publish JUnit results as PR annotations (e.g. `mikepenz/action-junit-report`) from the uploaded `test-report.xml` files.
- Add a `docker compose`-free smoke test: start `backend.app.run --storage=memory` in the background and `curl /health` plus `/openapi.json`.
