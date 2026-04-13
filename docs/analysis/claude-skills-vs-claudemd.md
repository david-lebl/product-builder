# Claude Code Skills vs CLAUDE.md — Analysis

## Overview

This document compares two mechanisms for providing instructions to Claude Code and recommends when to use each.

## CLAUDE.md — Always-Loaded Context

**Loaded:** Every session, every task — automatically.

**Best for:**
- Build & test commands
- Architecture rules and coding conventions
- Key file paths and navigation
- Short, always-relevant facts (i18n, Money type, Laminar gotchas)

**Keep it lean:** Every line consumes context tokens on every interaction. Target <120 lines.

## .claude/skills/ — On-Demand Procedures

**Loaded:** Only when the skill's `description` matches the current request, or when explicitly invoked via `/skill-name`.

**Best for:**
- Multi-step workflows (post-work documentation, deployment, code review checklists)
- Procedures only needed at specific points in a session
- Large reference material that isn't always relevant
- Scriptable or step-by-step actions

**Structure:**
```
.claude/skills/<skill-name>/SKILL.md   # Required — YAML frontmatter + instructions
.claude/skills/<skill-name>/reference.md  # Optional — deep context
.claude/skills/<skill-name>/scripts/      # Optional — executable scripts
```

## Decision Matrix

| Content Type | CLAUDE.md | .claude/skills/ |
|---|---|---|
| Build commands | ✅ | |
| Architecture rules | ✅ | |
| Key paths & navigation | ✅ | |
| Coding conventions (short) | ✅ | |
| Multi-step procedures | | ✅ |
| End-of-session checklists | | ✅ |
| Rarely-needed reference | | ✅ |
| Template-based workflows | | ✅ |

## Current State (this repo)

| Item | Location | Status |
|---|---|---|
| Build & Test Commands | CLAUDE.md | ✅ Correct |
| Architecture Overview | CLAUDE.md | ✅ Correct |
| Domain Layer Principles | CLAUDE.md | ✅ Correct |
| Key Paths | CLAUDE.md | ✅ Correct |
| Post-Work Documentation | `.claude/skills/post-work-docs/` | ✅ Extracted |

## Future Skill Candidates

| Potential Skill | Trigger Description |
|---|---|
| `add-pricing-rule` | Adding or modifying a pricing rule in the catalog |
| `create-domain-service` | Creating a new domain service with opaque types and Validation |
| `add-new-product` | Adding a new product category/material/finish to the catalog |
| `run-full-validation` | Pre-PR compile + test + lint checklist |

## Note on GitHub Copilot

GitHub Copilot coding agent does **not** support `.claude/skills/`. It reads only `.github/copilot-instructions.md`. Therefore, any procedure that must also work for Copilot agents needs to remain inline in that file.
