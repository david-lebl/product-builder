#!/usr/bin/env bash
#
# Enforces the bounded-context dependency rules from docs/architecture/modular-architecture.md §3.2:
#
#   commons   -> nothing. It is the shared kernel.
#   01-core   -> commons only. Never another context's core, never any infra.
#   02-infra  -> its own 01-core, plus other contexts' 01-core (that is where anti-corruption
#                adapters live). Never another context's infra.
#   app       -> the composition root, and the only module allowed to see an 02-infra.
#
# These rules are what make a context extractable into its own service later. They are cheap to
# hold now and expensive to recover once violated, so this runs in CI ahead of compilation.
#
# During the migration, `domain` (the shrinking legacy module) is an allowed dependency of any
# 02-infra. That exemption disappears with the module itself in Phase 7.

set -euo pipefail
cd "$(dirname "$0")/.."

exec python3 - "build.mill" <<'PYTHON'
import re, sys

build = open(sys.argv[1]).read()

# Track the `object` nesting by indentation so a module's qualified path is known, then capture
# each `def moduleDeps = Seq(...)` including multi-line ones. An earlier awk version only matched
# single-line Seq(...) and silently reported multi-line modules as having no dependencies at all —
# which meant it was not checking the one module (`app`) whose dependencies matter most.
OBJECT = re.compile(r'^(\s*)(?:private\s+)?object\s+`?([A-Za-z0-9_-]+)`?\s+extends')
# The `Seq(` may sit on the next line — a long dependency list gets wrapped by any formatter — so
# the declaration and the opening of the sequence are matched separately.
DEPS = re.compile(r'^\s*def\s+moduleDeps\s*=', re.M)

lines = build.split("\n")
stack = []          # (indent, name)
modules = {}        # qualified path -> list of deps
owner_of_line = {}

for i, line in enumerate(lines):
    m = OBJECT.match(line)
    if m:
        indent = len(m.group(1))
        while stack and stack[-1][0] >= indent:
            stack.pop()
        stack.append((indent, m.group(2)))
    owner_of_line[i] = ".".join(n for _, n in stack)

for i, line in enumerate(lines):
    if not DEPS.match(line):
        continue
    # Gather until parens balance, so multi-line Seq(...) is captured whole.
    buf, depth, started = "", 0, False
    for j in range(i, len(lines)):
        buf += lines[j]
        if "Seq(" in buf:
            started = True
        if started:
            depth += lines[j].count("(") - lines[j].count(")")
            if depth <= 0:
                break
    if "Seq(" not in buf:
        continue
    inner = buf[buf.index("Seq(") + 4 : buf.rindex(")")]
    deps = [d.strip().strip("`").rstrip(",").strip() for d in inner.split(",")]
    deps = [d.replace("`", "") for d in deps if d]
    modules[owner_of_line[i]] = deps

violations = []
print("Checking module dependency rules...\n")

for module, deps in sorted(modules.items()):
    context = module.split(".")[0]
    # A bare `01-core.jvm` inside `object catalog` means `catalog.01-core.jvm`.
    resolved = [d if d.split(".")[0] in modules or "." not in d or d.startswith(("commons", "domain", "ui"))
                else d for d in deps]
    qualified = [
        d if d.startswith(("commons", "domain", "ui")) or d.split(".")[0] in
             {m.split(".")[0] for m in modules} and not d.startswith(("01-", "02-"))
        else f"{context}.{d}"
        for d in resolved
    ]
    print(f"  {module:28s} -> {', '.join(qualified) if qualified else '(none)'}")

    for dep in qualified:
        if "01-core" in module:
            # `commons` and `product-kernel` are shared kernels: owned by no context, depended on
            # by several, and named so the sharing is a decision rather than a leak. Everything
            # else is still forbidden — a core may not see another context's core.
            if dep not in ("commons.jvm", "commons.js", "product-kernel.jvm", "product-kernel.js"):
                violations.append(
                    f"{module} depends on '{dep}'; a 01-core may depend on commons and "
                    "product-kernel only"
                )
        if "02-infra" in dep and module != "app":
            violations.append(f"{module} depends on infra '{dep}'; only the app composition root may")
        if "02-infra" in module and "02-infra" in dep:
            violations.append(f"{module} depends on another context's infra '{dep}'")

if "commons" in modules:
    violations.append("commons declares moduleDeps; the shared kernel must depend on nothing")

# The product kernel is shared, so it must stay small and dependency-free apart from commons.
for kernel in ("product-kernel.jvm", "product-kernel.js"):
    for dep in modules.get(kernel, []):
        if not dep.startswith("commons"):
            violations.append(
                f"{kernel} depends on '{dep}'; the product kernel may depend on commons only"
            )

# `app` must actually be checked — if it parsed as dependency-free, the parser is broken.
if "app" in modules and not modules["app"]:
    violations.append("app parsed with no moduleDeps; the parser is not seeing them")

# Every declaration must have been parsed. A module the parser silently skips is a module the
# guard is not guarding — which has already happened twice, once to `app` and once to `ui`.
declared = len(DEPS.findall(build))
if declared != len(modules):
    violations.append(
        f"{declared} moduleDeps declarations in the build, but {len(modules)} parsed — "
        "the parser is missing one, and whatever it missed is unchecked"
    )

print()
if violations:
    for v in violations:
        print(f"  ✗ {v}", file=sys.stderr)
    print(f"\nFAILED: {len(violations)} dependency-rule violation(s).", file=sys.stderr)
    print("See docs/architecture/modular-architecture.md §3.2", file=sys.stderr)
    sys.exit(1)

print("OK: all module dependencies respect the context boundaries.")
PYTHON
