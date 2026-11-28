#!/usr/bin/env bash
# Builds the four production Scala.js bundles and assembles the static site
# deployed to GitHub Pages (used by .github/workflows/deploy-pages.yml and deploy-preview.yml).
#
#   scripts/assemble-site.sh [output-dir]   # default: _site
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
SITE_DIR="${1:-_site}"
cd "$ROOT_DIR"

# Compile all four apps first (parallel, cheap on memory), then link each production bundle
# on its own. fullLinkJS (optimizer + Closure) needs several GB of heap per bundle; running the
# four in parallel in one Mill daemon overflows the -Xmx in .mill-jvm-opts on a 16 GB CI runner
# (java.lang.OutOfMemoryError: Java heap space). The daemon is shut down after each link so the
# Scala.js linker caches it keeps per module are released before the next bundle.
./mill '{ui,ui-admin,ui-calculator,ui-editor}.compile'
./mill shutdown
for app in ui ui-admin ui-calculator ui-editor; do
  ./mill "$app.fullLinkJS"
  ./mill shutdown
done

rm -rf "$SITE_DIR"
mkdir -p "$SITE_DIR/admin" "$SITE_DIR/calculator" "$SITE_DIR/editor"

cp modules/ui/src/main/resources/index.html "$SITE_DIR/"
cp out/ui/fullLinkJS.dest/main.js "$SITE_DIR/"
# Terms & Conditions / privacy notice linked from the checkout consent checkbox (mpbuilder.storefront.LegalTerms).
cp -R modules/ui/src/main/resources/legal "$SITE_DIR/"

cp modules/ui-admin/src/main/resources/index.html "$SITE_DIR/admin/"
cp out/ui-admin/fullLinkJS.dest/main.js "$SITE_DIR/admin/"

# The calculator goes in its own directory — every app emits main.js.
# index.html is the iframe-able standalone page, embed-example.html shows the
# mount-div integration inside a mock host site.
cp modules/ui-calculator/src/main/resources/* "$SITE_DIR/calculator/"
cp out/ui-calculator/fullLinkJS.dest/main.js "$SITE_DIR/calculator/"

# The Design Studio is a separate page; the storefront opens it in an iframe (editor/index.html).
cp modules/ui-editor/src/main/resources/index.html "$SITE_DIR/editor/"
cp out/ui-editor/fullLinkJS.dest/main.js "$SITE_DIR/editor/"

echo "Site assembled in $SITE_DIR"
