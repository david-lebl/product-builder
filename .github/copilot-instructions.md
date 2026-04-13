# Copilot Instructions

This file provides guidance to GitHub Copilot coding agent when working with code in this repository.

## Build & Test

See [CLAUDE.md](../CLAUDE.md) for detailed build commands. Quick reference:

```bash
# Mill (recommended)
mill __.compile          # Compile all
mill __.test             # Test all
mill ui.fastLinkJS       # Dev JS build

# sbt (legacy)
sbt compile              # Compile all
sbt test                 # Test all
sbt ui/fastLinkJS        # Dev JS build
```

## Key Architecture Rules

- **Pure domain, no effects** — Domain module uses `Validation[E, A]` from ZIO Prelude only. No ZIO effects.
- **Rules as sealed ADTs** — Compatibility and pricing rules are data, not functions.
- **Opaque types everywhere** — All IDs and value objects use Scala 3 opaque types with smart constructors.
- **Error accumulation** — Always use `Validation`, never `Either`, for domain results.
- **Money = BigDecimal** — Never use `Double` for monetary values. Always `HALF_UP` rounding.

See [CLAUDE.md](../CLAUDE.md) for full architecture details.

## Documentation Knowledge Base

This project maintains a structured documentation system. After completing any task, you **must** follow the post-work documentation steps below.

**Entry points:**

| Resource | Purpose |
|----------|---------|
| [docs/INDEX.md](../docs/INDEX.md) | Master table of contents — find any document by category |
| [docs/troubleshooting.md](../docs/troubleshooting.md) | Known issues & solutions (build, domain, UI, agent sessions) |
| [docs/changelog/](../docs/changelog/) | Per-session work logs (what was done, decisions, issues) |

## Post-Work Documentation Requirements

After completing any task (feature, bugfix, refactoring, analysis), perform these documentation steps before finalizing:

### 1. Work Changelog Entry

Create a file `docs/changelog/YYYY-MM-DD-short-description.md` following the template in [docs/changelog/README.md](../docs/changelog/README.md). Include:
- Summary of what was accomplished
- List of changes made (files created/modified/deleted)
- Key decisions and rationale
- Issues encountered and how they were resolved
- Follow-up items

### 2. Troubleshooting Updates

If you encountered and resolved any issues during the session, add them to [docs/troubleshooting.md](../docs/troubleshooting.md) using the template at the bottom of that file. This helps future sessions avoid the same problems.

### 3. Specification & Analysis Updates

- **New feature** → create or update the relevant specification in `docs/` and add it to `docs/INDEX.md`
- **Analysis or research** → create or update the document in `docs/analysis/` and add it to `docs/INDEX.md`
- **Changed existing behavior** → update the affected specification documents
- Keep specifications focused on *what* the system does, not implementation details

### 4. Index Update

After creating or modifying any documentation file, update [docs/INDEX.md](../docs/INDEX.md) to include the new or changed entry in the appropriate category.
