# 2026-11-25 — CI Workflow Review: Triggers, Mill Launcher, Caching, scalafmt

**PR:** N/A (branch `t3/ci-workflow-review-lint`)
**Author:** Claude (agent session)
**Type:** chore / CI

## Summary

The GitHub Actions setup was reviewed. CI never ran on feature PRs because it was limited to `main` while
every PR targets `develop`, and the PR-preview cleanup failed on every PR close. Both are fixed. Workflows
now use the repo's `./mill` (now the official Mill launcher), cache dependencies, and share one
site-assembly script. A report-only scalafmt check was added. The remaining optimisation backlog is in
[analysis/ci-optimization-plan.md](../analysis/ci-optimization-plan.md).

## Changes Made

- `ci.yml`: runs on `main` + `develop` (push & PR) and `workflow_dispatch`, with a per-PR concurrency
  cancel. Separate report-only `format` job. `coursier/cache-action@v9`. Single parallel `fastLinkJS`
  call. Test reports uploaded on failure.
- `deploy-preview.yml`: checkout always runs (it was skipped on `closed`, so removal failed with
  `fatal: not in a git directory`); fork PRs skipped; caching added.
- `deploy-pages.yml` / `deploy-preview.yml`: use the new `scripts/assemble-site.sh`.
- `copilot-setup-steps.yml`: actually warms Mill + deps (`./mill __.prepareOffline`).
- `./mill`: replaced the custom wrapper with the official `mill-dist-1.1.3-mill.sh` launcher. The wrapper's
  cache path collided with the launcher's JVM-fallback executable path.
- `.scalafmt.conf` added (3.11.5, Scala 3 dialect, 120 columns). The code base is not reformatted yet (251 files).
- Removed the dead sbt `project/` directory and the sbt commands in `.github/copilot-instructions.md`.
- Actions bumped to `checkout@v7`, `setup-java@v6`, `upload-artifact@v7` (off the deprecated Node 20).

## Follow-ups

See [analysis/ci-optimization-plan.md](../analysis/ci-optimization-plan.md) Part 2: one-shot reformat and
making scalafmt blocking, Scalafix, staged/parallel jobs, `out/` caching, Mill-managed JDK, Scala Steward
+ Dependabot for actions, and Pages race hardening.
