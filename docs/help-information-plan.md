# Help Information for Product Configuration — Analysis & Implementation Plan

## Problem Statement

Customers configuring print products may not understand the differences between
configuration options such as paper weights, lamination types, binding methods,
printing technologies, and finishing techniques. The product builder needs an
in-context help system that explains each option directly where it is selected.

Two levels of help are needed:

1. **Section overview help** — placed next to the section label (e.g. "Category",
   "Material", "Printing Method", "Finishes"). Explains the section purpose and
   the range of choices available.
2. **Selected-item detail help** — shown when an item is selected in a dropdown or
   checkbox. Explains the specific material, finish, or method that was chosen.

All descriptions must be bilingual (EN / CS) and part of the catalog so
administrators can manage them through the Catalog Editor.

---

## Architecture Decisions

### Domain Model

Each catalog entity gains an optional `description` field:

```scala
final case class Material(
    id: MaterialId,
    name: LocalizedString,
    family: MaterialFamily,
    weight: Option[PaperWeight],
    properties: Set[MaterialProperty],
    description: Option[LocalizedString] = None,   // ← new
)

final case class Finish(
    id: FinishId,
    name: LocalizedString,
    finishType: FinishType,
    side: FinishSide,
    description: Option[LocalizedString] = None,   // ← new
)

final case class PrintingMethod(
    id: PrintingMethodId,
    name: LocalizedString,
    processType: PrintingProcessType,
    maxColorCount: Option[Int],
    description: Option[LocalizedString] = None,   // ← new
)

final case class ProductCategory(
    id: CategoryId,
    name: LocalizedString,
    components: List[ComponentTemplate],
    requiredSpecKinds: Set[SpecKind],
    allowedPrintingMethodIds: Set[PrintingMethodId],
    description: Option[LocalizedString] = None,   // ← new
)
```

**Why `Option[LocalizedString]`?**
- Backward compatible — existing data keeps working (`= None`)
- Not every item needs a description (e.g. `Coated Art Paper Matte 115gsm` is
  self-explanatory for common weights)
- Follows the existing pattern of optional fields in the domain model

### UI Component — `HelpInfo`

A reusable Laminar component at `modules/ui/src/main/scala/mpbuilder/ui/components/HelpInfo.scala` with two variants:

| Variant | Use Case | API |
|---------|----------|-----|
| `HelpInfo.apply(desc, lang)` | Static popup next to a section label | Takes a `LocalizedString` and `Language` |
| `HelpInfo.fromSignal(descSignal, langSignal)` | Reactive popup that changes with selection | Takes `Signal[Option[LocalizedString]]` and `Signal[Language]` |

**Behavior:**
- Renders a small circular "?" (or "ℹ") trigger button
- On click, shows a popup overlay with the description text
- Popup closes on clicking the "×" button or clicking outside (backdrop)
- Trigger button is only visible when a description exists (for `fromSignal`)

### CSS Classes

All styles live in `utilities.css` following the existing CSS architecture:

| Class | Purpose |
|-------|---------|
| `.help-info-wrapper` | Inline flex container around trigger + popup |
| `.help-info-trigger` | The "?" circle button |
| `.help-info-trigger--small` | Smaller variant for inline item help |
| `.help-info-popup` | Positioned popup panel (hidden by default) |
| `.help-info-popup--visible` | Displayed state |
| `.help-info-popup-content` | Text content area |
| `.help-info-popup-close` | Close "×" button |
| `.help-info-backdrop` | Full-screen click-outside catcher |
| `.label-with-help` | Flex row: label + help trigger (for section labels) |
| `.item-with-help` | Flex row: item name + help trigger (for finish items) |

### Integration Points

| Component | Help Type | Signal / Source |
|-----------|-----------|-----------------|
| `CategorySelector` | Section overview | `selectedCategoryDescription` from ViewModel |
| `MaterialSelector` | Selected-item detail | Derived from `availableMaterials` + `selectedMaterialId` |
| `PrintingMethodSelector` | Selected-item detail | Derived from `availablePrintingMethods` + `selectedMethodId` |
| `FinishSelector` | Per-item detail | Each finish's `description` field |
| `SpecificationForm` | (Future) Fold type / Binding method help | Enum-level descriptions |

### Catalog Editor

All four editor views gain "Description (EN)" and "Description (CS)" text fields
below the name fields:

- `MaterialEditorView` — Description (EN/CS) fields
- `FinishEditorView` — Description (EN/CS) fields
- `PrintingMethodEditorView` — Description (EN/CS) fields
- `CategoryEditorView` — Description (EN/CS) fields

Empty description fields result in `None` (no popup shown to customers).

---

## Sample Descriptions Added

### Materials (key items)
- **Coated Art Paper 300gsm** — Premium glossy paper for business cards
- **Uncoated Bond 120gsm** — Natural matte paper for letterheads
- **Kraft Paper 250gsm** — Rustic eco-friendly for packaging
- **Adhesive Vinyl** — Durable waterproof for outdoor use
- **Corrugated Cardboard** — Lightweight yet sturdy for packaging
- **Coated Silk 250gsm** — Semi-matte combining readability with sheen
- **Yupo Synthetic** — Tear-resistant waterproof synthetic
- **Cotton Paper 300gsm** — Luxurious cotton-fiber for premium cards
- **Roll-Up Stand Economy** vs **Premium** — Budget vs professional grade

### Printing Methods (all)
- **Offset** — High-volume, plate-based, 500+ copies
- **Digital** — Cost-effective, no plates, short runs
- **UV Curable Inkjet** — Wide-format, UV-cured, outdoor durability
- **Letterpress** — Classic relief, tactile impression, 1–2 colors

### Finishes (all 16)
Descriptions for every finish type covering what it does, when to use it,
and what makes it unique.

### Categories (key items)
- Business Cards, Flyers, Brochures, Banners, Packaging, Booklets

---

## Future Development

### Rich Text / Markdown Descriptions
The current implementation uses plain text `LocalizedString` for descriptions.
For future development, rich text with images could be supported by:

1. **Markdown rendering** — Store descriptions as markdown strings, render with
   a lightweight JS markdown library (e.g. `marked.js` or `snarkdown`).
   Would require adding a JS dependency to the Scala.js build.

2. **Structured rich content** — Define a sealed ADT for content blocks:
   ```scala
   sealed trait ContentBlock
   object ContentBlock:
     case class Text(content: LocalizedString) extends ContentBlock
     case class Image(url: String, alt: LocalizedString) extends ContentBlock
     case class Heading(text: LocalizedString) extends ContentBlock
   ```
   More type-safe but more complex to author.

3. **External CMS** — Store descriptions in an external CMS/database and
   fetch them via API. Best for non-technical content editors.

**Recommendation:** Start with plain text (current implementation), then
evolve to markdown when rich formatting is needed. The `Option[LocalizedString]`
field is forward-compatible with all approaches.

### Additional Help Targets
- **Fold type descriptions** with visual diagrams
- **Binding method descriptions** with visual diagrams
- **Paper weight guide** ("What does gsm mean?")
- **Color mode explanations** (CMYK vs PMS vs Grayscale)
- **Section-level overview tooltips** for each step of the wizard
