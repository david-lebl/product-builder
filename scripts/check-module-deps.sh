#!/usr/bin/env bash
#
# Enforces the bounded-context dependency rules from docs/architecture/modular-architecture.md:
#
#   01-core   -> commons only. Never another context's core, never any infra.
#   02-infra  -> its own 01-core, plus other contexts' 01-core. Never another 02-infra.
#   commons   -> nothing.
#
# These rules are what make a context extractable into its own service later. They are cheap to
# hold now and expensive to recover once violated, so this runs in CI.
#
# During the migration, `domain` (the shrinking legacy module) is an allowed dependency of any
# 02-infra. That exemption disappears with the module itself in Phase 7.

set -euo pipefail
cd "$(dirname "$0")/.."

BUILD_FILE="build.mill"
violations=0

fail() {
  echo "  ✗ $1" >&2
  violations=$((violations + 1))
}

# Extract "<module path>|<moduleDeps contents>" for every module that declares moduleDeps.
# Tracks the nearest enclosing `object <name>` chain to build a qualified path.
deps=$(awk '
  /^object [`a-zA-Z0-9_-]+ extends/ { top = $2; gsub(/`/, "", top); depth1 = top; sub1 = ""; sub2 = "" }
  /^  object [`a-zA-Z0-9_-]+ extends/ { sub1 = $2; gsub(/`/, "", sub1); sub2 = "" }
  /^    object [`a-zA-Z0-9_-]+ extends/ { sub2 = $2; gsub(/`/, "", sub2) }
  /def moduleDeps/ {
    path = depth1
    if (sub1 != "") path = path "." sub1
    if (sub2 != "") path = path "." sub2
    line = $0
    sub(/.*Seq\(/, "", line)
    sub(/\).*/, "", line)
    gsub(/`/, "", line)
    gsub(/ /, "", line)
    print path "|" line
  }
' "$BUILD_FILE")

echo "Checking module dependency rules..."
echo

while IFS='|' read -r module depslist; do
  [ -z "$module" ] && continue
  echo "  $module -> ${depslist:-(none)}"

  IFS=',' read -ra items <<< "$depslist"
  for dep in "${items[@]}"; do
    [ -z "$dep" ] && continue

    # A context core may depend on commons and on its own sibling targets only.
    if [[ "$module" == *"01-core"* ]]; then
      case "$dep" in
        commons.jvm|commons.js) ;;
        *) fail "$module depends on '$dep'; a 01-core may depend on commons only" ;;
      esac
    fi

    # No module may ever depend on another module's infra.
    if [[ "$dep" == *"02-infra"* && "$module" != *"02-infra"* ]]; then
      fail "$module depends on infra '$dep'"
    fi
    if [[ "$module" == *"02-infra"* && "$dep" == *"02-infra"* ]]; then
      fail "$module depends on another context's infra '$dep'"
    fi
  done
done <<< "$deps"

# commons is the kernel: it must stay dependency-free.
if grep -A12 '^object commons extends' "$BUILD_FILE" | grep -q 'def moduleDeps'; then
  fail "commons declares moduleDeps; the shared kernel must depend on nothing"
fi

echo
if [ "$violations" -gt 0 ]; then
  echo "FAILED: $violations dependency-rule violation(s)." >&2
  echo "See docs/architecture/modular-architecture.md §3.2" >&2
  exit 1
fi

echo "OK: all module dependencies respect the context boundaries."
