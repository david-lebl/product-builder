# 2026-11-26 — Pin JDK 25 for Mill (no more manual JDK switching)

**PR:** N/A (branch `t3/jdk25-default-setup`)
**Author:** Claude (agent session)
**Type:** build / developer experience

## Summary

Kyo 1.0.0-RC7 needs JDK 25, but `build.mill` declared `//| mill-jvm-version: system`, so Mill ran on whatever
`java` the shell had (often 17). Every fresh shell or AI agent session failed with
`UnsupportedClassVersionError` (class file version 69.0) until someone exported `JAVA_HOME`.

## Changes Made

- `build.mill`: `//| mill-jvm-version: temurin:25.0.1` (matches `.sdkmanrc`). The `./mill` launcher downloads
  that JDK via Coursier once, caches it, and runs the daemon, compiler and forked tests on it.
- `CLAUDE.md`, `README.md`, `HOW_TO_RUN.md`, `docs/troubleshooting.md`: JDK 25 is now automatic; dropped the
  manual `JAVA_HOME` / `sdk use` steps.

## Decisions & Rationale

- Pinning in the build is better than shell setup (direnv, `.zshrc`, agent instructions): it works the same
  for every shell, IDE terminal, worktree, CI runner and AI agent with nothing to remember.
- Pinned an exact version (not just `25`) so local and CI builds use the same JDK.
- Verified with the shell on JDK 17: `backend.pricing.01-c-core.jvm.test` compiles and passes; the daemon
  runs on Coursier's Temurin 25.0.1.
- CI's `actions/setup-java` (25) is now redundant for Mill but kept; removing it is optional.
