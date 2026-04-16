# 2026-04-16 — Remove sbt from root docs and GitHub pipeline

**PR:** #—
**Author:** agent
**Type:** documentation

## Summary

Removed sbt references from all root-facing documentation and the GitHub CI pipeline while keeping the sbt build files (`build.sbt`, `project/`) intact in the repository. Verified Mill is fully configured and working.

## Changes Made

- **`.github/workflows/ci.yml`** — Removed the `test-sbt` job; CI now runs Mill-only (`mill __.compile`, `mill __.test`)
- **`README.md`** — Removed sbt from Tech Stack table, Prerequisites, Build & Test section, and Run Locally section
- **`CLAUDE.md`** — Removed the sbt legacy section; kept Mill commands only; noted sbt is still available in the project
- **`docs/ui-guide.md`** — Removed Quick Start (sbt), Development Workflow (sbt), Production Build (sbt), Running Tests (sbt), AI/MCP sbt tips, and sbt Out of Memory troubleshooting entry; updated all remaining sbt references to Mill equivalents
- **`docs/INDEX.md`** — Updated ui-guide.md description to remove "(Mill/sbt)" label
- **`docs/changelog/README.md`** — Added this entry

## Decisions & Rationale

- **Keep sbt files in repo** — `build.sbt` and `project/` are retained for developers who prefer sbt. The goal was only to remove sbt as a promoted/documented build path, not to delete it.
- **Mill completeness check** — The existing `build.mill` correctly defines all four modules (`domain.jvm`, `domain.js`, `ui-framework`, `ui-showcase`, `ui`) with proper deps, test frameworks, and JS targets. No changes needed to `build.mill`.
- **CI simplification** — The `test-sbt` CI job was redundant given that Mill runs the same tests. Removing it halves CI time and removes the sbt dependency from the pipeline.

## Issues Encountered

None.

## Follow-up Items

- [ ] Consider adding Mill watch mode notes once a file-watcher workflow is established
