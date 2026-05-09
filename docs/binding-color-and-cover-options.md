# Binding Color and Cover Options — Feature Specification

> Extends the binding method specification for booklets, calendars, and any product requiring binding, with two additional configurable options: binding color and (for spiral/wire-O bindings) separate front and back cover material choices.

## Overview

When a customer selects a binding method that supports colored hardware (Spiral Binding, Wire-O Binding, or Case Binding), a **Binding Color** dropdown appears in the specification form. For spiral and wire-O bindings, two additional dropdowns allow independent selection of a **Front Cover** and **Back Cover** (each can be Transparent or Carton).

Binding color materials are managed in the material catalog under the `BindingMaterial` family, making them configurable in the same stocks interface as paper, vinyl, and other materials.

## Binding Methods and Their Options

| Binding Method | Binding Color | Front Cover | Back Cover |
|---------------|:---:|:---:|:---:|
| Saddle Stitch | — | — | — |
| Perfect Binding | — | — | — |
| Spiral Binding | ✅ Ring color | ✅ | ✅ |
| Wire-O Binding | ✅ Ring color | ✅ | ✅ |
| Case Binding | ✅ Desk color | — | — |

## Binding Color

### What It Is

The physical color of the binding element:
- **Spiral / Wire-O**: the plastic or metal ring/coil that holds the pages together
- **Case Binding**: the fabric or leatherette color of the hardcover desk

### Material Configuration

Binding colors are first-class catalog materials with `MaterialFamily.BindingMaterial`. They can be created, renamed, or removed in the catalog editor without code changes.

**Sample colors shipped with the system:**

| ID | Name (EN) | For |
|----|-----------|-----|
| `mat-binding-black` | Binding – Black | Spiral / Wire-O |
| `mat-binding-silver` | Binding – Silver | Spiral / Wire-O |
| `mat-binding-red` | Binding – Red | Spiral / Wire-O |
| `mat-binding-blue` | Binding – Blue | Spiral / Wire-O |
| `mat-binding-white` | Binding – White | Spiral / Wire-O |
| `mat-binding-gold` | Binding – Gold | Spiral / Wire-O |
| `mat-desk-black` | Desk Cover – Black | Case Binding |
| `mat-desk-bordeaux` | Desk Cover – Bordeaux | Case Binding |
| `mat-desk-navy` | Desk Cover – Navy | Case Binding |
| `mat-desk-brown` | Desk Cover – Brown | Case Binding |

### Filtering by Binding Method

The dropdown in the product builder shows only materials relevant to the selected method:
- Spiral / Wire-O → materials with IDs starting `mat-binding-`
- Case Binding → materials with IDs starting `mat-desk-`

### Clearing on Method Change

Selecting a different binding method automatically clears any previously selected binding color, front cover, and back cover selections.

## Spiral / Wire-O Cover Options

For Spiral Binding and Wire-O Binding only, two independent cover dropdowns appear:

| Option | Description |
|--------|-------------|
| **Transparent** | Clear polypropylene or PVC cover — shows the first page through the cover |
| **Carton** | Opaque cardboard cover — provides a solid, printable front or back |

Front and back covers are selected independently; a customer may choose Transparent front + Carton back (or any combination).

## Domain Model

### New Types

```scala
// specification.scala
enum SpiralCoverType:
  case Transparent, Carton

// category.scala
enum SpecKind:
  case ..., BindingColor, SpiralFrontCover, SpiralBackCover

// specification.scala
enum SpecValue:
  case BindingColorSpec(materialId: MaterialId)
  case SpiralFrontCoverSpec(coverType: SpiralCoverType)
  case SpiralBackCoverSpec(coverType: SpiralCoverType)

// material.scala
enum MaterialFamily:
  case ..., BindingMaterial
```

### Spec Persistence

All three specs are persisted in `ProductSpecifications` alongside the other specs (size, quantity, binding method, etc.) and round-trip correctly through JSON via `DomainCodecs`.

## UI Behavior

1. Customer selects a product category that requires `SpecKind.BindingMethod` (e.g., Booklets, Calendars)
2. Customer selects a binding method from the dropdown
3. **If Spiral, Wire-O, or Case Binding is selected**: a "Binding Color" dropdown appears with relevant color options
4. **If Spiral or Wire-O is selected**: "Front Cover" and "Back Cover" dropdowns also appear
5. Customer's choices are included in the email order summary (via `EmailOrderModal`) and in the validated `ProductConfiguration`
