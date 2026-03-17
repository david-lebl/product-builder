# Help Information for Product Configuration Options

## Problem Statement

Customers configuring print products may not understand the differences between various configuration options such as:
- **Materials**: Paper weight (e.g., 90gsm vs 350gsm), coated vs uncoated, kraft vs vinyl
- **Finishes**: Matte vs gloss lamination, UV coating vs aqueous coating, embossing vs debossing
- **Printing Methods**: Digital vs offset, letterpress vs UV inkjet
- **Binding Methods**: Saddle stitch vs perfect binding vs spiral vs wire-o
- **Fold Types**: Half fold vs tri-fold vs gate fold vs accordion
- **Product Categories**: What each category includes and its typical use cases

Without help descriptions, customers must rely on prior printing knowledge or external research, leading to potential order errors and customer support overhead.

## Analysis

### Current State

The domain model has four main catalog entity types, each with only a `name: LocalizedString`:
- `Material(id, name, family, weight, properties)`
- `Finish(id, name, finishType, side)`
- `PrintingMethod(id, name, processType, maxColorCount)`
- `ProductCategory(id, name, components, requiredSpecKinds, allowedPrintingMethodIds)`

There is **no description or help text** anywhere in the catalog model. The UI shows names only.

### Requirements

1. Each catalog item (material, finish, printing method, category) should support an optional localized description
2. Descriptions should be part of the catalog data (not hardcoded in the UI) so they can be edited by catalog administrators
3. The product builder UI should display help buttons (ⓘ) next to configuration options
4. When clicked, help buttons show a popup with the description in the current language
5. The catalog editor should allow editing descriptions for each item
6. Rich text / markdown / images can be deferred to future development — plain text is the initial implementation

### Design Decisions

- **`Option[LocalizedString]` with default `None`**: Keeps backward compatibility. Existing code constructing entities without descriptions continues to work. JSON codec auto-derivation handles the optional field gracefully.
- **Descriptions in catalog, not hardcoded**: Aligns with the catalog-driven architecture. Print shop operators can customize descriptions to match their specific materials and capabilities.
- **Plain text popup**: Simple, no external dependencies. Rich text support (markdown rendering, images) is a natural future extension.
- **Click-to-show popup vs hover tooltip**: Click-based interaction works better on mobile/touch devices and for longer text content.

## Implementation

### Domain Model Changes

Added `description: Option[LocalizedString] = None` to:
- `Material` — Describes paper type, weight characteristics, best use cases
- `Finish` — Explains the finishing technique, visual/tactile result, when to use it
- `PrintingMethod` — Describes the printing process, advantages, ideal use cases
- `ProductCategory` — Explains what the category includes, typical specifications, tips

### Sample Data

All items in `SampleCatalog` received bilingual (EN/CS) descriptions:
- **30+ materials** with descriptions covering paper type, weight, surface, and best applications
- **16 finishes** with descriptions of the technique, visual result, and use cases
- **4 printing methods** with descriptions of the process, advantages, and ideal scenarios
- **11 product categories** with descriptions of what they include and configuration tips

### UI Components

#### HelpInfo Component (`HelpInfo.scala`)
- `HelpInfo.apply(description, lang)` — For static descriptions (e.g., finish items)
- `HelpInfo.fromSignal(description, lang)` — For reactive descriptions that change with selection (e.g., material selector)
- Renders a small "?" button that opens a popup with the description text
- Click-outside-to-close behavior
- CSS animation for smooth appearance

#### Updated Selectors
- **CategorySelector** — Shows help popup after selecting a category
- **MaterialSelector** — Shows help popup for the currently selected material
- **PrintingMethodSelector** — Shows help popup for the selected printing method
- **FinishSelector** — Shows help button next to each finish checkbox

#### Updated Catalog Editors
- **MaterialEditorView** — Added "Help Description" section with EN/CS text areas
- **FinishEditorView** — Added "Help Description" section with EN/CS text areas
- **PrintingMethodEditorView** — Added "Help Description" section with EN/CS text areas
- **CategoryEditorView** — Added "Help Description" section with EN/CS text areas

### Infrastructure
- Added `textAreaField` to `FormComponents` for multi-line text editing
- Added CSS classes: `.help-info-trigger`, `.help-info-wrapper`, `.help-info-popup`
- JSON codecs auto-derive via `DeriveJsonCodec.gen[T]` — no manual codec changes needed

## Future Development

### Rich Text Support
The current implementation uses plain text. Future enhancements could include:
- **Markdown rendering**: Parse description text as Markdown, render as HTML in the popup
- **Image support**: Allow image URLs or embedded images in descriptions (e.g., showing fold type diagrams, material texture photos, lamination comparison images)
- **Structured help content**: Separate short description (tooltip) from detailed description (popup modal)
- A dedicated `HelpContent` type replacing `Option[LocalizedString]` with support for markdown, images, and links

### Additional Enhancements
- **Fold type visualizations**: SVG diagrams showing how each fold type looks
- **Binding method diagrams**: Visual comparison of binding techniques
- **Material comparison table**: Side-by-side comparison of material properties
- **Contextual help**: Help text that adapts based on the selected category
- **Admin preview**: Preview help popups in the catalog editor before publishing
