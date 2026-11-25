# 2026-11-25 — Fix Scala.js Link OutOfMemoryError in CI

**PR:** N/A (branch `t3/fix-scalajs-link-oom`)
**Author:** Claude (agent session)
**Type:** bugfix / CI

## Summary

The CI step "Link Scala.js bundles" failed with `java.lang.OutOfMemoryError: Java heap space` inside the
Scala.js linker's `SourceMapWriter`. The linker runs in the Mill daemon JVM, which had no `-Xmx`. The JVM
therefore capped its heap at ¼ of RAM (4 GB on `ubuntu-latest`). That was not enough to link four large
bundles in parallel after `__.compile` and `__.test` had already run in the same daemon.

## Changes Made

- `.mill-jvm-opts`: added `-Xmx6g` (next to `-Xss8m`). This also covers the `fullLinkJS` builds in
  `scripts/assemble-site.sh` (Pages and preview deploys).
- `docs/troubleshooting.md`: new entry for this failure.

## Decisions & Rationale

- Raising the daemon heap keeps the bundles linking in parallel. Linking them one at a time (`-j 1`) would
  have been the alternative, but it makes the step slower and only delays the problem as the bundles grow.
- Checked locally on a 16 GB machine (same RAM as the runner): all four `fastLinkJS` bundles link in about
  2.5 minutes. fastLink output is 60–107 MB of JS plus 38–57 MB of source map per bundle.
