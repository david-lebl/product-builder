# Running the Material Builder UI

This guide explains how to build and run the Material Builder UI locally.

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

## Using the UI

### Step 1: Select Product Category
Choose from available categories (Business Cards, Flyers, Brochures, Banners, Packaging, Booklets).

### Step 2: Select Material
The dropdown will show only materials compatible with your selected category.

### Step 3: Select Printing Method
Choose a printing method suitable for your category.

### Step 4: Select Finishes (Optional)
The UI shows only finishes compatible with your material and category. You can select multiple finishes.

### Step 5: Product Specifications
Enter specifications:
- **Quantity**: Number of items (e.g., 1000)
- **Size**: Width and height in millimeters (e.g., 90mm × 50mm for business cards)
- **Pages**: Optional, for multi-page products
- **Color Mode**: CMYK or Grayscale

### Step 6: Calculate Price
Click "Calculate Price" to validate your configuration and see the pricing breakdown.

## Key Features

- **Progressive Disclosure**: Options are enabled/disabled based on your selections, preventing invalid combinations
- **Real-time Compatibility**: The UI dynamically filters available materials, finishes, and methods based on business rules
- **Price Calculation**: Instant pricing with detailed breakdown including material costs, finish surcharges, and quantity discounts
- **Validation Feedback**: Clear error messages if configuration is invalid

## Architecture

The UI is built with:
- **Scala.js**: Compiles Scala code to JavaScript
- **Laminar**: Reactive UI framework with Signal/Var reactive primitives
- **Domain Model**: Shared pure functional domain logic (cross-compiled from JVM)
- **Sample Catalog**: Pre-loaded with materials, finishes, rules, and pricing

## Troubleshooting

### JavaScript Errors in Browser Console
Check that `main.js` is properly copied to the `dist/` directory and accessible at `/main.js`.

### "Cannot find module" Errors
Make sure you're serving from the `dist/` directory and that both `index.html` and `main.js` are present.

### Compilation Errors
Ensure you're using Scala 3.3.3 (specified in `build.sbt`). Scala 3.8.x has compatibility issues with Scala.js.

### Changes Not Reflecting
Clear your browser cache or do a hard refresh (Ctrl+F5 or Cmd+Shift+R).

## Next Steps

- Persistence layer for saving configurations
- Server-side API for validation and pricing
- Admin interface for managing catalogs and rules
- PDF generation for order proofs
