# 2026-11-25 — Fix fullLinkJS OutOfMemoryError in Pages / preview deploys

**PR:** N/A (branch `t3/fix-fulllinkjs-oom-deploy`)
**Author:** Claude (agent session)
**Type:** bugfix / CI

## Summary

After `-Xmx6g` (see `2026-11-25-scalajs-link-oom.md`), the deploy workflows still failed:
`scripts/assemble-site.sh` ran `./mill '{ui,ui-admin,ui-calculator,ui-editor}.fullLinkJS'` in one
invocation. Four parallel production links (optimizer + Closure Compiler) used more than 6 GB and failed with
`java.lang.OutOfMemoryError: Java heap space` after ~8 minutes.

## Changes Made

- `scripts/assemble-site.sh`: compile the four apps in parallel, then link each bundle in its own
  `./mill <app>.fullLinkJS` with `./mill shutdown` after each one. Every link gets a fresh daemon with the
  full heap.
- `docs/troubleshooting.md`: extended the Scala.js link OOM entry with the `fullLinkJS` case.

## Decisions & Rationale

- Running the links one after another costs little time: in CI the parallel run had been going for ~490 s
  before it failed. Locally (JDK 25), the sequential links took 150 s (ui), 175 s (ui-admin),
  78 s (ui-calculator) and 59 s (ui-editor), ~9 min for the whole script including compilation.
- Raising `-Xmx` further was rejected: four Closure runs plus the daemon would need most of the runner's
  16 GB, which leaves too little for the OS and the GC.
