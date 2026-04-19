# Extended Binding and Cover Configuration

> Specification for the binding element, cover role split, binding edge/pitch, and related model extensions.

## Binding Methods

The `BindingMethod` enum represents how pages are held together:

| Value | Description | Compatible Binding Edge |
|-------|-------------|------------------------|
| `SaddleStitch` | Stapled through the spine fold | Left, Right |
| `PerfectBinding` | Glued spine | Left, Right |
| `PlasticCoilBinding` | Plastic O-ring coil (formerly SpiralBinding) | Top, Left, Right |
| `MetalWireBinding` | Metal double-loop wire (formerly WireOBinding) | Top, Left, Right |
| `CaseBinding` | Hardcover with case boards | Left, Right |

## Component Roles

### Cover Roles

| Role | Purpose | Example |
|------|---------|---------|
| `Cover` | Single undifferentiated cover (front/back same sheet) | Booklets with saddle stitch |
| `FrontCover` | Transparent protective front cover | Calendar clear PVC/PET sheet |
| `BackCover` | Structural back cover | Calendar 350gsm cardboard back |

Booklets use `Cover` (the front and back are part of the same sheet in saddle stitch). Calendars use `Cover` for the printed cover page, and optionally `FrontCover`/`BackCover` for protective/structural elements.

### Binding Component

`ComponentRole.Binding` represents the physical binding element (wire, coil) as a configurable component with material selection. This allows users to choose binding color/type independently from the binding method.

**Binding hardware materials (all `MaterialFamily.Hardware`):**

| Material | ID | Notes |
|----------|----|-------|
| Black Metal Wire (3:1) | `mat-black-metal-wire` | Standard black double-loop |
| Silver Metal Wire (3:1) | `mat-silver-metal-wire` | Chrome/silver finish |
| White Metal Wire (3:1) | `mat-white-metal-wire` | White-coated |
| Black Plastic Coil | `mat-black-plastic-coil` | Standard black O-ring |
| White Plastic Coil | `mat-white-plastic-coil` | Clean appearance |
| Clear Plastic Coil | `mat-clear-plastic-coil` | Transparent |
| Silver Plastic Coil | `mat-silver-plastic-coil` | Metallic silver |

### Other Component Roles

| Role | Purpose | Status |
|------|---------|--------|
| `HangingStrip` | Wall calendar hanging strip/eyelet | Defined, not yet templated |
| `CaseBoard` | Rigid boards for case binding | Defined, not yet templated |
| `Endpaper` | Decorative paper inside case binding covers | Defined, not yet templated |
| `Packaging` | Shrink wrap, poly bag, wrapping | Defined, not yet templated |

## Specification Values

### Binding Edge (`SpecKind.BindingEdge`)

Which edge of the page the binding runs along:

| Value | Use Case |
|-------|----------|
| `BindingEdge.Top` | Wall calendars (wire across the top) |
| `BindingEdge.Left` | Standard portrait booklets |
| `BindingEdge.Right` | Right-to-left language booklets |

Required for `calendars` and `booklets` categories.

### Binding Pitch (`SpecKind.BindingPitch`)

Hole spacing for coil/wire binding:

| Value | Description |
|-------|-------------|
| `BindingPitch.ThreeToOne` | 3:1 pitch — 34 holes per A4 (standard) |
| `BindingPitch.FourToOne` | 4:1 pitch — 52 holes per A4 (thinner documents) |

Defined but not yet required by any category.

## Compatibility Rules

| Rule | Constraint |
|------|-----------|
| Plastic coil binding → material | Binding component must use plastic coil material IDs |
| Metal wire binding → material | Binding component must use metal wire material IDs |
| Top edge → method | Top binding edge only with PlasticCoilBinding or MetalWireBinding |
| Calendar binding edge | Top or Left only |
| Booklet binding edge | Left or Right only |

## Finish Extensions

### New Finish Types

| Type | Category | Description |
|------|----------|-------------|
| `HangingStrip` | Structural | Calendar hanging strip/eyelet |
| `IndexTab` | Structural | Thumb index tabs for catalogs |
| `ShrinkWrap` | Structural | Protective shrink wrapping |

### New Finish Parameters

| Parameter | Used With | Fields |
|-----------|-----------|--------|
| `SaddleStitchParams` | Binding | `stapleCount: Int` (1–6) |
| `DrillingParams` | Drilling | `holeCount: Int` (1–10), `positionMm: List[Double]` |
| `IndexTabParams` | IndexTab | `tabCount: Int` (1–31), `tabWidthMm: Int` (5–30) |

## Calendar Category Configuration

The `calendars` category supports these components:

| Component | Optional | Materials |
|-----------|----------|-----------|
| Cover | no | Medium-heavy coated papers |
| Body | no | Medium-heavy coated/uncoated papers |
| FrontCover | yes | Clear PVC, Clear PET |
| BackCover | yes | Cardboard 350gsm, heavy coated papers |
| Binding | yes | All wire and coil materials |

Required specs: `Size`, `Quantity`, `Pages`, `BindingMethod`, `BindingEdge`

### Presets

| Preset | Cover | Body | Front | Back | Binding | Edge |
|--------|-------|------|-------|------|---------|------|
| Wall Calendar | Glossy 250g | Glossy 170g | — | — | — | Top |
| Desk Calendar | Matte 300g | Matte 200g | — | — | — | Top |
| Premium Wall | Matte 250g | Matte 200g | PVC | Cardboard 350g | Silver wire | Top |

## Booklet Category Configuration

The `booklets` category supports these components:

| Component | Optional | Materials |
|-----------|----------|-----------|
| Cover | no | All coated papers |
| Body | no | All coated/uncoated papers |
| Binding | yes | All wire and coil materials |

Required specs: `Size`, `Quantity`, `Pages`, `BindingMethod`, `BindingEdge`

### Presets

| Preset | Cover | Body | Binding | Method | Edge |
|--------|-------|------|---------|--------|------|
| Saddle Stitch | Glossy 250g | Glossy 130g | — | Saddle Stitch | Left |
| Perfect Binding | Matte 300g | Matte 130g | — | Perfect Binding | Left |
| Metal Wire | Matte 250g | Matte 130g | Black wire | Metal Wire | Left |
