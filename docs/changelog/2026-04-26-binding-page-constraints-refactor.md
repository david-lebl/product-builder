# 2026-04-26 — Binding Page Constraints Refactor

**PR:** N/A
**Author:** David Lebl / Claude
**Type:** refactoring

## Summary

Moved booklet page-count rules out of category-level `SpecConstraint` entries into `TechnologyConstraint` rules tied to binding method. Replaced the single saddle stitch + heavy paper rule with a three-tier weight-based page limit system, and added flat max-page limits for perfect binding, spiral binding, and wire-o binding.

## Changes Made

- **`modules/domain/src/main/scala/mpbuilder/domain/sample/SampleRules.scala`**
  - Removed `SpecConstraint(bookletsId, MinPages(8), ...)` (was booklet-category-only)
  - Removed `SpecConstraint(bookletsId, MaxPages(96), ...)` (was booklet-category-only)
  - Removed FIXME comment that flagged these as needing to move
  - Removed single `TechnologyConstraint` for saddle stitch ≥300gsm → max 80 pages
  - Added `TechnologyConstraint`: any multi-page binding method → min 8 pages
  - Added `TechnologyConstraint`: saddle stitch → max 96 pages (any paper)
  - Added `TechnologyConstraint`: saddle stitch + ≥170gsm → max 80 pages
  - Added `TechnologyConstraint`: saddle stitch + ≥250gsm → max 52 pages
  - Added `TechnologyConstraint`: perfect binding → max 400 pages
  - Added `TechnologyConstraint`: spiral binding → max 300 pages
  - Added `TechnologyConstraint`: wire-o binding → max 160 pages

- **`modules/domain/src/test/scala/mpbuilder/domain/ConfigurationBuilderSpec.scala`**
  - Updated "booklet with too few pages" assertion: `SpecConstraintViolation` → `TechnologyConstraintViolation`
  - Updated "saddle stitch on heavy paper" tests: renamed (≥300gsm → ≥250gsm), changed success case from 80 pages to 52 pages
  - Added 5 tests for the saddle stitch weight tiers (170gsm/250gsm/130gsm boundaries)
  - Added 6 tests for perfect/spiral/wire-o max-page limits (boundary success + over-limit rejection each)

- **`docs/features.md`** — updated rule variant count and table (added missing `ConfigurationConstraint` and `TechnologyConstraint`)

## Decisions & Rationale

- **Category → Technology scope**: Page limits aren't booklet-specific — they derive from the physics of each binding method. Moving them to `TechnologyConstraint` means they apply in any category (e.g. a future "magazines" category gets them for free).
- **Overlapping saddle stitch tiers**: Three independent constraints (any, ≥170gsm, ≥250gsm) overlap intentionally — the tightest applicable constraint always wins, no need for a `HasMaxWeight` predicate that doesn't exist.
- **GSM thresholds**: ≥170gsm = normal paper, ≥250gsm = heavy paper (confirmed with user).
- **New method limits** (suggested and accepted):
  - Perfect binding 400 pages — glue spine structural limit for in-house production
  - Spiral binding 300 pages — largest standard plastic coil diameter
  - Wire-O binding 160 pages — largest twin-loop wire (≈1.25″)

## Issues Encountered

None.

## Follow-up Items

- [ ] Consider adding paper-weight tiers for perfect binding (thick paper + many pages can stress the spine)
- [ ] Wire-O and spiral limits currently don't distinguish paper weight; could add ≥250gsm variants if needed
