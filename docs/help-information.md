# Help Information for Configuration Options

This document describes the help information feature in the product builder, which provides contextual explanations for configuration options to help customers understand what they are selecting.

## Overview

The product builder displays two types of help information:

1. **Field-level help (`?` button)** — A static explanation of what a configuration field means and how it affects the product. Shown next to the label of each selector.
2. **Item description (`ⓘ` button)** — A detailed description of the currently selected option, pulled from the catalog data. Only appears when the selected item has a description defined.

## Supported Fields

| Field | `?` Help Text | `ⓘ` Item Description |
|-------|--------------|---------------------|
| Category | What categories are and how they constrain options | Description of the selected category |
| Material | What material/paper weight means | Description of the selected material |
| Printing Method | How different printing methods work | Description of the selected method |
| Finishes | What finishing options do | Per-finish description (inline) |

## Domain Model

The `description` field is an optional localized string on each catalog entity:

```scala
final case class Material(
    // ... other fields ...
    description: Option[LocalizedString] = None,
)

final case class Finish(
    // ... other fields ...
    description: Option[LocalizedString] = None,
)

final case class PrintingMethod(
    // ... other fields ...
    description: Option[LocalizedString] = None,
)

final case class ProductCategory(
    // ... other fields ...
    description: Option[LocalizedString] = None,
)
```

Descriptions are **bilingual** (English + Czech) using `LocalizedString`. They are defined in the catalog and can be set per item. The field uses a default value of `None`, so existing catalog data without descriptions remains fully compatible.

## UI Component: `HelpInfo`

Located at `modules/ui/src/main/scala/mpbuilder/ui/components/HelpInfo.scala`.

### API

```scala
// Static field-level help (? button)
HelpInfo(text: Signal[String]): Element

// Reactive item description (ⓘ button, only visible when description exists)
HelpInfo.fromSignal(description: Signal[Option[String]]): Element
```

### Behavior

- **`?` button**: Clicking opens a centered modal popup with the help text. Clicking the backdrop or the button again closes it.
- **`ⓘ` button**: Only renders when the signal contains `Some(text)`. Same popup behavior. Disappears when no description is available (e.g., no item selected).

### CSS Classes

| Class | Purpose |
|-------|---------|
| `.help-info-wrapper` | Inline wrapper for the button + popup |
| `.help-info-trigger` | The circular `?` button |
| `.help-info-trigger--detail` | Variant styling for the `ⓘ` button |
| `.help-info-popup` | The popup container |
| `.help-info-popup--visible` | Makes the popup visible |
| `.help-info-backdrop` | Full-screen backdrop for click-to-close |
| `.help-info-backdrop--visible` | Makes the backdrop visible |
| `.selector-with-help` | Wrapper for selectors with positioned help buttons |
| `.selector-help-buttons` | Absolutely-positioned container for help buttons |
| `.label-with-help` | Flex container for label + inline help button |
| `.finish-label-with-help` | Inline-flex container for finish checkbox labels |

## Example Descriptions

### Roll-Up Economy vs Premium (material descriptions)

**Economy Stand:**
> Budget-friendly retractable banner stand. Lightweight aluminium construction (~2 kg), basic snap-rail top bar. Suitable for indoor use, short-term events, and single-use promotions. Typically lasts 10–20 setups.

**Premium Stand:**
> Professional-grade retractable banner stand. Wide base with adjustable feet for stability, tensioned top rail for a flat banner surface. Built for frequent use at trade shows and permanent displays. Lasts 100+ setups with interchangeable cassettes.

### Matte vs Gloss Lamination (finish descriptions)

**Matte Lamination:**
> Protective matte film applied to the surface. Provides a smooth, non-reflective finish with a velvety feel. Reduces glare and fingerprints.

**Gloss Lamination:**
> Shiny protective film that enhances color vibrancy and contrast. Makes images pop with a high-gloss mirror-like finish. Adds durability and water resistance.

## Future Development

- **Rich text / Markdown support**: Currently descriptions are plain text. A future enhancement could support Markdown rendering or rich HTML for descriptions with images, links, and formatting.
- **Catalog Editor integration**: The catalog editor UI should include a `localizedStringEditor` for the description field when editing materials, finishes, etc.
- **Image attachments**: For materials and finishes, sample images or swatches could be attached to descriptions to give customers a visual reference.
