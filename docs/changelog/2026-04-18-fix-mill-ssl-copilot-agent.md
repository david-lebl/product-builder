# 2026-04-18 — Fix Mill SSL failure in Copilot Agent environment

**PR:** —
**Author:** copilot agent
**Type:** bugfix

## Summary

Added `.github/workflows/copilot-setup-steps.yml` to fix a recurring `SSLHandshakeException` that prevented the Copilot Agent from using Mill to compile the project. Updated the troubleshooting guide with the root cause and proper solution.

## Changes Made

- **Created** `.github/workflows/copilot-setup-steps.yml` — Copilot Agent setup workflow: installs Temurin JDK 17, caches Coursier and Mill downloads, pre-warms the Mill build.
- **Modified** `docs/troubleshooting.md` — replaced the outdated "fall back to sbt" workaround with a proper explanation of the root cause and the `copilot-setup-steps.yml` fix.
- **Modified** `docs/changelog/README.md` — added this session to the log table.

## Decisions & Rationale

- **Root cause:** The default JVM available in the Copilot Agent runner environment does not include the root CA certificates trusted by Maven Central's TLS endpoint. Coursier (used internally by Mill to resolve `mill-runner-daemon`) therefore throws a PKIX validation error. GitHub Actions CI avoids this because `actions/setup-java` with the Temurin distribution ships an up-to-date CA bundle.
- **Fix chosen:** `copilot-setup-steps.yml` is the canonical mechanism for pre-configuring the Copilot Agent environment. It mirrors the JDK setup already used in `ci.yml` and adds Coursier/Mill cache steps so the agent never needs to re-download JARs on every task.
- **Alternative considered:** Disabling SSL verification via JVM flags — rejected because it is a security anti-pattern and does not survive agent restarts.

## Issues Encountered

- The `mill` bootstrap wrapper in the repo already existed and downloads the correct Mill version. The failure only happens in the second phase (Coursier resolving Mill's own runtime JARs) because that phase runs on the system JVM, not a fresh download.

## Follow-up Items

- [ ] Verify the `copilot-setup-steps.yml` workflow passes on the default branch once this PR is merged (it must be on the default branch to be picked up by Copilot).
