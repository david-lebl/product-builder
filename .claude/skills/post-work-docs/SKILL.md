---
name: post-work-docs
description: >
  Post-work documentation procedure. Use after completing any task
  (feature, bugfix, refactoring, analysis) to update changelog,
  troubleshooting, specs, and the docs index.
---

# Post-Work Documentation Steps

After completing any task (feature, bugfix, refactoring, analysis), you **must** perform these documentation steps:

## 1. Work Changelog Entry

Create a file `docs/changelog/YYYY-MM-DD-short-description.md` following the template in `docs/changelog/README.md`. Include:
- Summary of what was accomplished
- List of changes made (files created/modified/deleted)
- Key decisions and rationale
- Issues encountered and how they were resolved
- Follow-up items

## 2. Troubleshooting Updates

If you encountered and resolved any issues during the session, add them to `docs/troubleshooting.md` using the template at the bottom of that file. This helps future sessions avoid the same problems.

## 3. Specification & Analysis Updates

- If the work involved a **new feature**: create or update the relevant specification document in `docs/` and add it to `docs/INDEX.md`.
- If the work involved **analysis or research**: create or update the relevant analysis document in `docs/analysis/` and add it to `docs/INDEX.md`.
- If the work **changed existing behavior**: update the affected specification documents.
- Keep specifications focused on *what* the system does (not implementation details or plans).

## 4. Index Update

After creating or modifying any documentation file, update `docs/INDEX.md` to include the new or changed entry in the appropriate category.
