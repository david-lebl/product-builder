# Running the Product Builder UI

This guide explains how to build and run the Product Builder UI locally. The app includes two main views: the **Product Builder** (print product configurator) and the **Calendar Builder** (photo calendar editor).

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

The app has two main views accessible via navigation buttons at the top: **Product Builder** and **Calendar Builder**. A language selector (English / Čeština) is available at the top level and applies to both views.

### Product Builder

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

### Calendar Builder

The Calendar Builder is a visual editor for designing 12-page photo calendars:

- **Page Navigation**: Move between 12 monthly pages
- **Element Types**: Add photos (upload from local machine), text fields, and shapes (lines, rectangles)
- **Element Editing**: Select any element to resize, rotate, reposition, or modify properties
- **Text Formatting**: Bold, italic, text alignment (left/center/right)
- **Shapes**: Configurable stroke and fill colors
- **Z-Ordering**: Bring elements to front or send to back
- **Duplicate & Delete**: Clone or remove elements
- **Page Backgrounds**: Set solid colors or upload background images
- **Template Fields**: Pre-set month and day labels (locked, non-editable)

## Key Features

- **Progressive Disclosure**: Options are enabled/disabled based on your selections, preventing invalid combinations
- **Real-time Compatibility**: The UI dynamically filters available materials, finishes, and methods based on business rules
- **Price Calculation**: Instant pricing with detailed breakdown including material costs, finish surcharges, and quantity discounts
- **Validation Feedback**: Clear error messages if configuration is invalid
- **Internationalization**: English and Czech language support with browser detection
- **Shopping Basket**: Manage multiple product configurations with quantities and totals
- **Calendar Editor**: Visual drag-and-drop calendar page design

## Architecture

The UI is built with:
- **Scala.js**: Compiles Scala code to JavaScript
- **Laminar**: Reactive UI framework with Signal/Var reactive primitives
- **Domain Model**: Shared pure functional domain logic (cross-compiled from JVM)
- **Sample Catalog**: Pre-loaded with materials, finishes, rules, and pricing
- **AppRouter**: Client-side routing between Product Builder and Calendar Builder views

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

- Persistence layer for saving configurations and calendar state
- Server-side API for validation and pricing
- Admin interface for managing catalogs and rules
- PDF generation for calendar pages and order proofs
