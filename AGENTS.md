# AGENTS.md

Instructions for AI coding agents (Codex, Copilot, Cursor, Claude, …) working in this repository.

**The full guide is [CLAUDE.md](CLAUDE.md)** — build/test commands, architecture, domain & UI conventions, key paths and the documentation workflow. Read it before making changes.

## Prefer breaking changes

The platform is under active development and **not deployed** — no external API consumers, production databases or persisted user data exist. Favour the cleanest design over compatibility: change types, APIs, routes, DTOs and schemas at the root and update all call sites; prefer a generic rewrite over yet another special case; delete obsolete code; never add deprecated aliases, shims, adapters or `v2` duplicates. SQL migrations and design-document formats may be rewritten instead of migrated. Everything must still compile, tests must stay green, and EN/CS translations must stay complete. See "Development Stage" in [CLAUDE.md](CLAUDE.md) for details.
