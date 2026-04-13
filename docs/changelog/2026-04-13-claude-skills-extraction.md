# 2026-04-13 — Extract Post-Work Docs into Claude Code Skill

**PR:** copilot/add-documentation-skills
**Author:** agent
**Type:** documentation

## Summary

Extracted the 28-line post-work documentation procedure from `CLAUDE.md` into a Claude Code skill at `.claude/skills/post-work-docs/SKILL.md`. This reduces always-loaded context tokens while keeping the procedure available on demand.

## Changes Made

- Created `.claude/skills/post-work-docs/SKILL.md` — the 4-step post-work documentation procedure as a Claude Code skill with YAML frontmatter
- Modified `CLAUDE.md` — replaced the full 28-line procedure (§121-148) with a 3-line pointer to the skill
- Kept `.github/copilot-instructions.md` unchanged — GitHub Copilot agent doesn't support `.claude/skills/`, so it needs the procedure inline
- Created `docs/analysis/claude-skills-vs-claudemd.md` — analysis document comparing skills vs CLAUDE.md approaches
- Created this changelog entry

## Decisions & Rationale

- **Why extract to skill?** The post-work procedure is a multi-step workflow only needed at session end, not during coding. Skills load on-demand, keeping the always-loaded CLAUDE.md lean.
- **Why keep copilot-instructions.md inline?** GitHub Copilot coding agent doesn't support `.claude/skills/` — it can only read `.github/copilot-instructions.md`.
- **Why not extract more sections?** Other CLAUDE.md sections (build commands, architecture rules, key paths) are compact and always-relevant. Only the post-work procedure was a clear candidate.

## Issues Encountered

- None

## Follow-up Items

- [ ] Consider adding more skills as workflows grow (e.g., `add-pricing-rule`, `create-domain-service`, `run-full-validation`)
- [ ] Monitor whether Claude Code correctly auto-triggers the post-work-docs skill at session end
