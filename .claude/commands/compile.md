# Compile

Compile all modules using Mill (preferred) or sbt.

## Mill (preferred)

```bash
# Compile everything
mill __.compile

# Compile individual modules
mill domain.jvm.compile       # Domain (JVM)
mill domain.js.compile        # Domain (Scala.js)
mill ui-framework.compile     # UI kit
mill ui.compile               # Full UI app (Scala.js)
```

## sbt (legacy)

```bash
sbt compile
# or per-module:
sbt domainJVM/compile
sbt uiFramework/compile
sbt ui/compile
```

Run the appropriate command based on which module you are currently editing. Prefer Mill.

