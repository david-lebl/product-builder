# UI Kit Showcase

Interactive demo of all components in the `ui-framework` module. Runs as a standalone Scala.js app with its own `index.html` and complete CSS — no dependency on the main app or the domain module.

## What it demonstrates

| Section | Components |
|---------|-----------|
| **Field Components** | `TextField`, `TextAreaField`, `SelectField`, `CheckboxField`, `RadioGroup` — including live validation with `FormFieldState` |
| **Containers** | `Tabs`, `Stepper` |
| **Validation & Visibility** | `ValidationDisplay`, `Visibility.when` / `Visibility.unless` |
| **Form Derivation** | `FormState.create[T]` — Mirror-based compile-time derivation from a case class, with `.withValidators`, `.touchAll`, `.allErrors` |

## Running locally

### 1. Build the JavaScript

```bash
# Development build (fast, unoptimised)
sbt uiShowcase/fastLinkJS

# Production build (fully optimised)
sbt uiShowcase/fullLinkJS
```

Output lands at:
```
modules/ui-showcase/target/scala-3.3.3/material-builder-ui-showcase-fastopt/main.js   # dev
modules/ui-showcase/target/scala-3.3.3/material-builder-ui-showcase-opt/main.js       # prod
```

### 2. Open in a browser

The `index.html` expects `main.js` in the same directory. The easiest way is to serve the output folder with any static file server:

```bash
# Using Python (no install needed on macOS)
cd modules/ui-showcase/target/scala-3.3.3/material-builder-ui-showcase-fastopt
python3 -m http.server 8080
# → open http://localhost:8080
```

Or copy `src/main/resources/index.html` next to the built `main.js` and open it directly in a browser (works without a server for local files on most browsers).

```bash
cp src/main/resources/index.html \
   target/scala-3.3.3/material-builder-ui-showcase-fastopt/index.html
open target/scala-3.3.3/material-builder-ui-showcase-fastopt/index.html
```

## Module layout

```
modules/ui-showcase/
  src/main/scala/mpbuilder/uikit/showcase/
    Main.scala            ← entry point: renderOnDomContentLoaded → UiKitShowcase()
  src/main/resources/
    index.html            ← standalone page with full ui-kit CSS
  README.md               ← this file
```

## Dependencies

```
uiShowcase
  └── uiFramework   (Laminar components, no domain)
        └── Laminar 17.2.0
```

The showcase has **no dependency on the domain module** — it only imports from `mpbuilder.uikit.*`.
