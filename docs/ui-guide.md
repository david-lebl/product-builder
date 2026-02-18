# Running the Product Builder UI

This guide explains how to build, run, and test the Product Builder locally. The app includes two main views: the **Product Parameters** (print product configurator) and the **Visual Editor** (visual product editor for calendars, photo books, and wall pictures).

## Prerequisites

- Java 11+ (Java 17 recommended)
- sbt 1.12.3+ (Scala Build Tool)
- A modern web browser

## Quick Start

### 1. Build the UI

From the project root directory, run:

```bash
sbt ui/fastLinkJS
```

This will:
- Compile the domain model for Scala.js
- Compile the UI code to JavaScript
- Generate `main.js` in `modules/ui/target/scala-3.3.3/material-builder-ui-fastopt/`

The first build may take a while as it downloads dependencies.

### 2. Prepare the distribution

Copy the compiled files to a distribution directory:

```bash
mkdir -p dist
cp modules/ui/src/main/resources/index.html dist/
cp modules/ui/target/scala-3.3.3/material-builder-ui-fastopt/main.js dist/
```

### 3. Serve the UI

Start a local HTTP server in the `dist` directory:

```bash
cd dist
python3 -m http.server 8080
```

Or use any other static file server of your choice.

### 4. Open in browser

Navigate to: http://localhost:8080/index.html

## Development Workflow

For faster development iterations:

1. Keep sbt running in watch mode:
   ```bash
   sbt ~ui/fastLinkJS
   ```
   This will automatically recompile when you change files.

2. After each compilation, copy the new `main.js` to `dist/`:
   ```bash
   cp modules/ui/target/scala-3.3.3/material-builder-ui-fastopt/main.js dist/
   ```

3. Refresh your browser to see changes.

## Production Build

For production deployment, use fullOptimization:

```bash
sbt ui/fullLinkJS
```

Then copy from `modules/ui/target/scala-3.3.3/material-builder-ui-opt/` instead.

## Testing

### Running Tests

```bash
# Run all tests (99 tests across 5 suites)
sbt test

# Run only domain JVM tests (faster, no Scala.js compilation)
sbt domainJVM/test

# Run a single test suite
sbt "testOnly mpbuilder.domain.ConfigurationBuilderSpec"
sbt "testOnly mpbuilder.domain.CatalogQueryServiceSpec"
sbt "testOnly mpbuilder.domain.PriceCalculatorSpec"
sbt "testOnly mpbuilder.domain.LocalizationSpec"
sbt "testOnly mpbuilder.domain.BasketServiceSpec"

# Verbose test output (shows individual test names)
sbt "testOnly * -- -v"
```

### Test Suites

| Suite | Tests | Description |
|-------|-------|-------------|
| `ConfigurationBuilderSpec` | 34 | Valid/invalid configurations, error accumulation, weight rules, finish dependencies |
| `CatalogQueryServiceSpec` | 17 | Material/finish/spec filtering, progressive disclosure |
| `PriceCalculatorSpec` | 14 | Pricing calculations, breakdowns, area-based pricing, quantity tiers |
| `LocalizationSpec` | 17 | i18n, localized strings, error message translations |
| `BasketServiceSpec` | 17 | Basket operations, quantity management, totals |

### Tips for Faster Iteration

- Use `sbt domainJVM/test` instead of `sbt test` when you only changed domain code — it skips Scala.js compilation and is significantly faster.
- Use `sbt ~domainJVM/test` for continuous testing during domain development.
- The UI module has no tests; compile it with `sbt ui/compile` to check for errors.

## Using the UI

The app has two main views accessible via navigation buttons at the top: **Product Parameters** and **Visual Editor**. A language selector (English / Čeština) is available at the top level and applies to both views.

### Product Parameters (Product Configuration)

#### Step 1: Select Product Category
Choose from available categories (Business Cards, Flyers, Brochures, Banners, Packaging, Booklets, Calendars).

#### Step 2: Select Material
The dropdown shows only materials compatible with your selected category.

#### Step 3: Select Printing Method
Choose a printing method suitable for your category.

#### Step 4: Select Finishes (Optional)
The UI shows only finishes compatible with your material and category. You can select multiple finishes.

#### Step 5: Product Specifications
Enter specifications:
- **Quantity**: Number of items (e.g., 1000)
- **Size**: Width and height in millimeters (e.g., 90mm × 50mm for business cards)
- **Pages**: Optional, for multi-page products
- **Color Mode**: CMYK or Grayscale
- **Binding Method**: Required for booklets/calendars

#### Step 6: Calculate Price & Add to Basket
Click "Calculate Price" to validate your configuration and see the pricing breakdown. You can then add the configured product to the shopping basket with a specified quantity.

### Shopping Basket
- View all added items with product details (category, material, printing method, finishes)
- Update quantities for individual items
- Remove items or clear the entire basket
- See basket total calculation

### Visual Editor

The Visual Editor is a page-based designer for visual products:

#### Product Type & Format Selection
- **Product Type**: Monthly Calendar (12p), Weekly Calendar (52p), Bi-weekly Calendar (26p), Photo Book (12p), Wall Picture (1p)
- **Format**: Physical size in mm — varies by product type (e.g., Wall Calendar 210×297mm, Desk Calendar 297×170mm, Photo Book Square 210×210mm)

#### Page Canvas
- Drag, resize, and rotate elements on the canvas
- Canvas aspect ratio matches the physical format
- Template text fields (month names, day numbers) are locked and non-editable
- Click empty area to deselect all elements

#### Page Elements (left sidebar, first tab)
- **Photo upload**: Upload images from local machine; empty placeholders shown as dashed-border areas
- **Text elements**: Add text with bold/italic/alignment formatting
- **Shapes**: Lines and rectangles with configurable stroke/fill colors
- **Element list**: All elements with z-ordering (↑↓), duplicate (⎘), and delete (×) buttons
- **Property editors**: Type-specific forms for each selected element

#### Background (left sidebar, second tab)
- **Template selector**: Grid template
- **Background color**: Color picker
- **Background image**: Upload custom background
- **Apply to all**: Apply background or template to all pages

#### Page Navigation (bottom strip)
- Horizontal scrollable strip with page thumbnails
- Previous/Next buttons
- Click any thumbnail to jump to that page
- Works with any number of pages (1–52+)

## Key Features

- **Progressive Disclosure**: Options are enabled/disabled based on your selections, preventing invalid combinations
- **Real-time Compatibility**: The UI dynamically filters available materials, finishes, and methods based on business rules
- **Price Calculation**: Instant pricing with detailed breakdown including material costs, finish surcharges, and quantity discounts
- **Validation Feedback**: Clear error messages if configuration is invalid
- **Internationalization**: English and Czech language support with browser detection
- **Shopping Basket**: Manage multiple product configurations with quantities and totals
- **Visual Editor**: Multi-product visual design with interactive elements

## Architecture

The UI is built with:
- **Scala.js**: Compiles Scala code to JavaScript
- **Laminar**: Reactive UI framework with Signal/Var reactive primitives
- **Domain Model**: Shared pure functional domain logic (cross-compiled from JVM)
- **Sample Catalog**: Pre-loaded with materials, finishes, rules, and pricing
- **AppRouter**: Client-side routing between Product Parameters and Visual Editor views

## Tips for AI Assistants / MCP Server Integration

When working with this codebase through an MCP server (e.g., GitHub Copilot Coding Agent or Claude Code):

### Build & Compile

- **sbt may not be on PATH** — If you get `sbt: command not found`, install it:
  ```bash
  curl -sL "https://github.com/sbt/sbt/releases/download/v1.12.3/sbt-1.12.3.tgz" -o /tmp/sbt.tgz
  tar -xzf /tmp/sbt.tgz -C /tmp
  export PATH="/tmp/sbt/bin:$PATH"
  ```
- **First sbt invocation is slow** — It downloads dependencies and compiles everything. Allow 2–3 minutes for the first `sbt compile`. Subsequent runs use the incremental compiler and are much faster.
- **Use `sbt domainJVM/test` for fast domain testing** — Avoids Scala.js compilation overhead. Only use `sbt test` when you need to verify JS compatibility.
- **Chain commands** — Use `sbt "domainJVM/test" "ui/compile"` to run domain tests and compile UI in one sbt session, avoiding startup overhead.

### Code Navigation

- **Domain model is in `modules/domain/`** — Cross-compiled for JVM and JS. All domain logic is pure (no IO effects).
- **UI is in `modules/ui/`** — Scala.js only. Uses Laminar for reactive rendering.
- **Visual editor model is in `CalendarModel.scala`** — Contains all types: `VisualProductType`, `ProductFormat`, `CanvasElement` ADT, `CalendarTemplate`, `CalendarPage`, `CalendarState`.
- **No tests for UI module** — Verify UI changes by compiling with `sbt ui/compile`.

### Common Gotchas

- **Laminar's `child <-- signal.map { ... }`** recreates the DOM element on every signal emission. Set `selected` on `<option>` elements explicitly rather than relying on `value :=` on the `<select>`.
- **Exhaustive matching is mandatory** — All `enum` matches in domain code must cover every case without wildcards. The compiler enforces this.
- **`Money` is opaque** — Use `Money(bigDecimal)` to create, `.value` to extract. Never use `Double`.
- **Cross-compilation** — Domain code must work on both JVM and JS. Avoid JVM-only APIs (e.g., `java.io`, `java.sql`).

### Verifying UI Changes

Since there are no UI tests, verify UI changes by:
1. Running `sbt ui/compile` to check for compilation errors
2. Building the JS with `sbt ui/fastLinkJS`
3. Copying `main.js` to `dist/` and opening in a browser
4. Taking screenshots for review

Note: In sandboxed MCP environments, localhost servers may not be accessible from the browser. You can create static HTML mockups with inline styles to preview layout changes.

## Troubleshooting

### JavaScript Errors in Browser Console
Check that `main.js` is properly copied to the `dist/` directory and accessible at `/main.js`.

### "Cannot find module" Errors
Make sure you're serving from the `dist/` directory and that both `index.html` and `main.js` are present.

### Compilation Errors
Ensure you're using Scala 3.3.3 (specified in `build.sbt`). Scala 3.8.x has compatibility issues with Scala.js.

### Changes Not Reflecting
Clear your browser cache or do a hard refresh (Ctrl+F5 or Cmd+Shift+R).

### sbt Out of Memory
If sbt runs out of memory, set the JVM heap:
```bash
export SBT_OPTS="-Xmx2G"
sbt compile
```

## Next Steps

- Persistence layer for saving configurations and calendar/visual editor state
- Server-side API for validation and pricing
- Admin interface for managing catalogs and rules
- PDF generation for visual product pages and order proofs
