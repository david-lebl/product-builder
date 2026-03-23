# Visual Product Types — Implementation Features

This document describes the new features added to implement the visual product types
domain integration as specified in [visual-product-types.md](visual-product-types.md).

---

## 1. Domain-Level Product Types (`VisualProductType`)

**File**: `modules/domain/src/main/scala/mpbuilder/domain/model/visualproduct.scala`

The `VisualProductType` enum was moved from the UI layer to the domain module,
making it cross-compiled (JVM + JS) and available for domain-level testing and
integration.

### Enum Cases

| Case | Page Count | English Name | Czech Name |
|------|------------|--------------|------------|
| `MonthlyCalendar` | 12 | Monthly Calendar | Měsíční kalendář |
| `WeeklyCalendar` | 52 | Weekly Calendar | Týdenní kalendář |
| `BiweeklyCalendar` | 26 | Bi-weekly Calendar | Dvoutýdenní kalendář |
| `PhotoBook` | 12 | Photo Book | Fotokniha |
| `WallPicture` | 1 | Wall Picture | Obraz na zeď |

### Methods

- **`defaultPageCount: Int`** — Returns the default page count for the product type.
- **`displayName(lang: Language): String`** — Returns the localized display name (EN/CS).

---

## 2. Domain-Level Product Formats (`ProductFormat`)

**File**: `modules/domain/src/main/scala/mpbuilder/domain/model/visualproduct.scala`

The `ProductFormat` case class was moved from the UI layer to the domain module with
additional methods for domain integration.

### Case Class Fields

```scala
case class ProductFormat(
  id: String,       // Unique format identifier (e.g., "wall-calendar")
  nameEn: String,   // English display name
  nameCs: String,   // Czech display name
  widthMm: Int,     // Width in millimeters
  heightMm: Int,    // Height in millimeters
)
```

### Instance Methods

| Method | Return Type | Description |
|--------|-------------|-------------|
| `displayName(lang)` | `String` | Localized name |
| `isLandscape` | `Boolean` | Width > height |
| `isPortrait` | `Boolean` | Height > width |
| `isSquare` | `Boolean` | Width == height |
| `orientation` | `Orientation` | Derived from dimensions |
| `toDimension` | `Dimension` | Domain dimension value |
| `toSizeSpec` | `SpecValue.SizeSpec` | Domain specification value |

### Companion Object Methods

| Method | Description |
|--------|-------------|
| `formatsFor(pt)` | Returns applicable formats for a product type |
| `defaultFor(pt)` | Returns the first applicable format (auto-selected on type change) |
| `findById(id)` | Looks up a format by its string ID |
| `isLandscape(fmt)` | Static orientation check |
| `all` | List of all 10 defined formats |

### Predefined Formats (10 total)

**Calendar formats (4):**
`WallCalendar`, `WallCalendarLarge`, `DeskCalendar`, `DeskCalendarSmall`

**Photo Book formats (3):**
`PhotoBookSquare`, `PhotoBookLandscape`, `PhotoBookPortrait`

**Wall Picture formats (3):**
`WallPictureSmall`, `WallPictureLarge`, `WallPictureLandscape`

---

## 3. Visual Product Domain Mapping Service

**File**: `modules/domain/src/main/scala/mpbuilder/domain/service/VisualProductDomainMapping.scala`

A pure mapping service that bridges visual product types to the domain model,
enabling pricing and validation of visual products using the existing domain infrastructure.

### Methods

#### `toCategoryId(productType: VisualProductType): CategoryId`

Maps visual product types to domain category IDs:

| Visual Product Type | Domain Category ID |
|---------------------|-------------------|
| `MonthlyCalendar` | `cat-calendars` |
| `WeeklyCalendar` | `cat-calendars` |
| `BiweeklyCalendar` | `cat-calendars` |
| `PhotoBook` | `cat-booklets` |
| `WallPicture` | `cat-banners` |

#### `toSpecifications(productType, format): ProductSpecifications`

Builds a complete `ProductSpecifications` from a visual product type and format:

- **`SizeSpec`** — from format dimensions (e.g., 210×297 mm for Wall Calendar)
- **`PagesSpec`** — from product type page count (e.g., 12 for Monthly Calendar)
- **`OrientationSpec`** — derived from format dimensions (portrait/landscape)

#### `isFormatApplicable(productType, format): Boolean`

Validates whether a format is valid for a product type, enforcing the applicability
rules defined in the spec.

#### `categoryIdValue(productType): String`

Convenience method returning the string value of the mapped category ID.

---

## 4. UI Integration

**File**: `modules/ui/src/main/scala/mpbuilder/ui/calendar/CalendarModel.scala`

The UI module now imports `VisualProductType` and `ProductFormat` from the domain
module using Scala 3 `export` statements, maintaining backward compatibility with
all existing UI code:

```scala
import mpbuilder.domain.model.{VisualProductType, ProductFormat}
export mpbuilder.domain.model.VisualProductType
export mpbuilder.domain.model.ProductFormat
```

The `CalendarState.defaultPageCount()` method now delegates to the domain-level
`VisualProductType.defaultPageCount` instead of duplicating the logic.

---

## 5. Test Coverage

**File**: `modules/domain/src/test/scala/mpbuilder/domain/VisualProductDomainMappingSpec.scala`

29 new tests covering all new functionality:

### Category Mapping Tests (7)
- Each product type maps to the correct category ID
- All calendar types map to the same category
- Category IDs match `SampleCatalog` IDs

### Specification Building Tests (6)
- Monthly/Weekly/Bi-weekly calendar page counts
- Photo Book square format dimensions
- Wall Picture page count
- Landscape format produces correct orientation

### Format Applicability Tests (4)
- Calendar formats applicable to calendar types
- Photo book formats not applicable to calendar types
- Calendar formats not applicable to photo book
- Wall picture formats only applicable to wall picture

### ProductFormat Tests (8)
- `formatsFor` returns correct count per type
- `defaultFor` returns first format
- `findById` lookup
- `all` contains 10 formats
- Orientation methods
- `toDimension` conversion
- `toSizeSpec` conversion

### VisualProductType Tests (4)
- `defaultPageCount` for all types
- `displayName` in English
- `displayName` in Czech

---

## Architecture Decision

The types were placed in the **domain module** (not the UI module) because:

1. **Cross-compilation**: The domain module compiles to both JVM and JS, making the types
   available for server-side use when a backend is added.
2. **Testability**: Domain tests can exercise the mapping logic without depending on Scala.js/Laminar.
3. **Separation of concerns**: Product types and formats are domain concepts, not UI concepts.
   They define the business rules for what products can be created and how they relate to
   pricing categories.
4. **Consistency**: Follows the existing pattern where all domain concepts (categories, materials,
   specifications) live in `mpbuilder.domain.model`.

The UI module re-exports the domain types using Scala 3 `export` declarations, so existing
UI code continues to work unchanged via `import mpbuilder.ui.calendar.*`.
