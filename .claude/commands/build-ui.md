# Build UI (Scala.js)

Compile the Laminar SPA to JavaScript.

## Dev build (fast link)

```bash
mill ui.fastLinkJS
# Output: out/ui/fastLinkJS.dest/main.js
```

## Production build (full link, optimised)

```bash
mill ui.fullLinkJS
# Output: out/ui/fullLinkJS.dest/main.js
```

## sbt (legacy)

```bash
sbt ui/fastLinkJS         # Dev build
sbt ui/fullLinkJS         # Production build
sbt ~ui/fastLinkJS        # Watch mode
```

## Notes

- The UI module depends on `domain.js` and `ui-framework`.
- The compiled JS goes into the `dist/` directory when served via the deploy workflow.
- Use `fastLinkJS` during development; `fullLinkJS` for deployments.

