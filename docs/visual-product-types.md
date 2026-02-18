# Visual Product Types & Formats

This document defines the supported visual product types, their available formats (sizes),
and the mapping to the product material domain model.

## Product Types

| Type | Description | Page Count |
|------|-------------|------------|
| Monthly Calendar | 12-month calendar, one page per month | 12 |
| Weekly Calendar | 52-week calendar, one page per week | 52 |
| Bi-weekly Calendar | 26 pages, two weeks per page | 26 |
| Photo Book | Customizable photo book | 12 |
| Wall Picture | Single image for wall display | 1 |

## Product Formats

Each product type supports a subset of physical formats. Formats define the physical
dimensions (width × height in mm) and the orientation.

### Calendar Formats

| Format ID | Name (EN) | Name (CS) | Size (mm) | Orientation | Applicable Types |
|-----------|-----------|-----------|-----------|-------------|------------------|
| `wall-calendar` | Wall Calendar | Nástěnný kalendář | 210 × 297 | Portrait | Monthly, Weekly, Bi-weekly |
| `wall-calendar-large` | Wall Calendar Large | Nástěnný kalendář velký | 297 × 420 | Portrait | Monthly, Weekly, Bi-weekly |
| `desk-calendar` | Desk Calendar | Stolní kalendář | 297 × 170 | Landscape | Monthly, Weekly, Bi-weekly |
| `desk-calendar-small` | Desk Calendar Small | Stolní kalendář malý | 210 × 110 | Landscape | Monthly, Weekly, Bi-weekly |

### Photo Book Formats

| Format ID | Name (EN) | Name (CS) | Size (mm) | Orientation | Applicable Types |
|-----------|-----------|-----------|-----------|-------------|------------------|
| `photobook-square` | Photo Book Square | Fotokniha čtvercová | 210 × 210 | Square | Photo Book |
| `photobook-landscape` | Photo Book Landscape | Fotokniha na šířku | 297 × 210 | Landscape | Photo Book |
| `photobook-portrait` | Photo Book Portrait | Fotokniha na výšku | 210 × 297 | Portrait | Photo Book |

### Wall Picture Formats

| Format ID | Name (EN) | Name (CS) | Size (mm) | Orientation | Applicable Types |
|-----------|-----------|-----------|-----------|-------------|------------------|
| `wall-picture-small` | Wall Picture Small | Obraz malý | 210 × 297 | Portrait | Wall Picture |
| `wall-picture-large` | Wall Picture Large | Obraz velký | 297 × 420 | Portrait | Wall Picture |
| `wall-picture-landscape` | Wall Picture Landscape | Obraz na šířku | 420 × 297 | Landscape | Wall Picture |

## Format–Type Applicability Rules

- **Calendar types** (Monthly, Weekly, Bi-weekly) → Calendar formats only
- **Photo Book** → Photo Book formats only
- **Wall Picture** → Wall Picture formats only

When a user changes the product type, the format selector is filtered to show
only applicable formats. If the currently selected format is not applicable
to the new type, the first available format for that type is selected automatically.

## Domain Model Integration

The visual editor product types map to the existing `ProductCategory` in the
material domain:

| Visual Product Type | Domain Category |
|---------------------|-----------------|
| Monthly Calendar | `cat-calendars` |
| Weekly Calendar | `cat-calendars` |
| Bi-weekly Calendar | `cat-calendars` |
| Photo Book | `cat-booklets` |
| Wall Picture | `cat-banners` (large format) |

The format dimensions are used for the `SpecValue.SizeSpec` when building
the `ConfigurationRequest` for pricing and validation.
