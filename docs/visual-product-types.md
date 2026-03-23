# Visual Product Types & Formats

This document defines the supported visual product types, their available formats (sizes),
and the mapping to the product material domain model.

## Implementation Status

All features described in this document are **fully implemented**.

| Feature | Status | Location |
|---------|--------|----------|
| Product types (5) | ✅ Implemented | `domain/model/visualproduct.scala` — `VisualProductType` enum |
| Product formats (10) | ✅ Implemented | `domain/model/visualproduct.scala` — `ProductFormat` case class |
| Format–type applicability | ✅ Implemented | `ProductFormat.formatsFor()` with auto-selection |
| Domain model integration | ✅ Implemented | `service/VisualProductDomainMapping.scala` |
| UI selectors | ✅ Implemented | `ui/calendar/CalendarBuilderApp.scala` |
| Tests | ✅ 29 tests | `VisualProductDomainMappingSpec.scala` |

## Product Types

Defined as `VisualProductType` enum in `mpbuilder.domain.model`:

| Type | Enum Case | Description | Page Count |
|------|-----------|-------------|------------|
| Monthly Calendar | `MonthlyCalendar` | 12-month calendar, one page per month | 12 |
| Weekly Calendar | `WeeklyCalendar` | 52-week calendar, one page per week | 52 |
| Bi-weekly Calendar | `BiweeklyCalendar` | 26 pages, two weeks per page | 26 |
| Photo Book | `PhotoBook` | Customizable photo book | 12 |
| Wall Picture | `WallPicture` | Single image for wall display | 1 |

Each variant provides:
- `defaultPageCount: Int` — number of pages for this product type
- `displayName(lang: Language): String` — localized name (EN/CS)

## Product Formats

Each product type supports a subset of physical formats. Formats define the physical
dimensions (width × height in mm) and the orientation.

Defined as `ProductFormat` case class in `mpbuilder.domain.model` with fields:
`id`, `nameEn`, `nameCs`, `widthMm`, `heightMm`.

Instance methods:
- `displayName(lang)` — localized name
- `isLandscape` / `isPortrait` / `isSquare` — orientation queries
- `orientation` — returns `Orientation.Portrait` or `Orientation.Landscape`
- `toDimension` — converts to domain `Dimension(widthMm, heightMm)`
- `toSizeSpec` — converts to `SpecValue.SizeSpec` for domain configuration

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

Implemented in `ProductFormat.formatsFor(pt: VisualProductType): List[ProductFormat]`.

When a user changes the product type, the format selector is filtered to show
only applicable formats. If the currently selected format is not applicable
to the new type, the first available format for that type is selected automatically
via `ProductFormat.defaultFor(pt)`.

`ProductFormat.findById(id: String): Option[ProductFormat]` allows looking up any
format by its string ID.

## Domain Model Integration

The visual editor product types map to the existing `ProductCategory` in the
material domain. This mapping is implemented in `VisualProductDomainMapping`:

| Visual Product Type | Domain Category | Mapping Method |
|---------------------|-----------------|----------------|
| Monthly Calendar | `cat-calendars` | `toCategoryId(MonthlyCalendar)` |
| Weekly Calendar | `cat-calendars` | `toCategoryId(WeeklyCalendar)` |
| Bi-weekly Calendar | `cat-calendars` | `toCategoryId(BiweeklyCalendar)` |
| Photo Book | `cat-booklets` | `toCategoryId(PhotoBook)` |
| Wall Picture | `cat-banners` (large format) | `toCategoryId(WallPicture)` |

### Specification Building

`VisualProductDomainMapping.toSpecifications(productType, format)` builds a
`ProductSpecifications` containing:

- **`SizeSpec`** — from format dimensions (`ProductFormat.toDimension`)
- **`PagesSpec`** — from product type page count (`VisualProductType.defaultPageCount`)
- **`OrientationSpec`** — derived from format dimensions (portrait/landscape)

These specifications are used for the `ConfigurationRequest` for pricing and validation.

### Format Applicability Check

`VisualProductDomainMapping.isFormatApplicable(productType, format)` validates
whether a given format is valid for a product type, enforcing the applicability rules.

## Key Source Files

| File | Purpose |
|------|---------|
| `modules/domain/src/main/scala/mpbuilder/domain/model/visualproduct.scala` | `VisualProductType` enum and `ProductFormat` case class with all format definitions |
| `modules/domain/src/main/scala/mpbuilder/domain/service/VisualProductDomainMapping.scala` | Domain mapping service (category IDs, specifications) |
| `modules/domain/src/test/scala/mpbuilder/domain/VisualProductDomainMappingSpec.scala` | 29 tests covering all mappings |
| `modules/ui/src/main/scala/mpbuilder/ui/calendar/CalendarModel.scala` | Re-exports domain types for UI usage |
| `modules/ui/src/main/scala/mpbuilder/ui/calendar/CalendarBuilderApp.scala` | Product type and format selectors UI |
| `modules/ui/src/main/scala/mpbuilder/ui/calendar/CalendarViewModel.scala` | Reactive state management for product type/format changes |
