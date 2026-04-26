# Work Changelog

> Session-by-session work logs documenting what was done, decisions made, and issues encountered. Each entry corresponds to a PR, feature, or agent session.

## Format

Each changelog entry is a markdown file named `YYYY-MM-DD-short-description.md` (e.g., `2026-04-13-documentation-knowledge-base.md`).

### Template

```markdown
# [Date] — [Short Title]

**PR:** #[number] or N/A
**Author:** [human / agent name]
**Type:** feature | bugfix | refactoring | documentation | analysis

## Summary

Brief description of what was accomplished in this session.

## Changes Made

- List of concrete changes (files created, modified, deleted)
- ...

## Decisions & Rationale

- Key decisions made and why
- Alternatives considered (if any)

## Issues Encountered

- Problems hit during the session and how they were resolved
- Reference troubleshooting.md entries if applicable

## Follow-up Items

- [ ] Things left to do or discovered during this session
- [ ] ...
```

---

## Log Entries

| Date | Title | PR | Type |
|------|-------|----|------|
| 2026-04-26 | [Binding Page Constraints Refactor](2026-04-26-binding-page-constraints-refactor.md) | — | refactoring |
| 2026-04-25 | [Fix Lamination Pricing: Per Sheet and Per Area](2026-04-25-fix-lamination-pricing-per-sheet-area.md) | — | bugfix |
| 2026-04-22 | [Update minimum order price to 200 CZK](2026-04-22-update-minimum-order-price-200-czk.md) | — | bugfix |
| 2026-04-22 | [Additive Ink Configuration Pricing](2026-04-22-ink-configuration-additive-pricing.md) | #135 | feature |
| 2026-04-22 | [Ink Configuration Pricing Analysis](2026-04-22-ink-configuration-pricing-analysis.md) | — | analysis |
| 2026-04-21 | [Fix Manufacturing UI crash and Pricelist empty table](2026-04-21-fix-manufacturing-ui-and-pricelist-table.md) | #134 | bugfix |
| 2026-04-21 | [Scoring: stepper input and pricing fix](2026-04-21-scoring-stepper-and-pricing-fix.md) | — | bugfix |
| 2026-04-20 | [Creasing/Scoring feature](2026-04-20-scoring-creasing-feature.md) | — | feature |
| 2026-04-20 | [Banner Product Overhaul](2026-04-20-banner-product-overhaul.md) | — | feature |
| 2026-04-18 | [Fix Mill SSL failure in Copilot Agent environment](2026-04-18-fix-mill-ssl-copilot-agent.md) | — | bugfix |
| 2026-04-16 | [Compact UI Redesign for ProductBuilderApp](2026-04-16-compact-ui-redesign.md) | — | refactoring |
| 2026-04-16 | [Remove sbt from root docs and GitHub pipeline](2026-04-16-remove-sbt-from-docs-and-pipeline.md) | — | documentation |
| 2026-04-15 | [Price preview validate button & per-item display layout](2026-04-15-price-preview-validate-button-layout.md) | — | bugfix |
| 2026-04-13 | [Documentation knowledge base & agent skills](2026-04-13-documentation-knowledge-base.md) | — | documentation |
| 2026-04-13 | [Extract post-work docs into Claude Code skill](2026-04-13-claude-skills-extraction.md) | — | documentation |
