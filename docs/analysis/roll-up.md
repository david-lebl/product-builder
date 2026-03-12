# Roll-Up Banners

A roll-up (retractable banner stand) is a self-standing large-format display product commonly used at trade shows, retail environments, and exhibitions. The printed graphic retracts into a compact cassette housing at the base, making it easy to transport and set up.

---

## Product Components

A roll-up order has one required and one optional component:

| Component | Role | Required | Description |
|-----------|------|----------|-------------|
| **Printed Banner** | `Main` | Yes | The large-format graphic printed on polyester banner film |
| **Standing Platform** | `Stand` | No (optional) | The retractable aluminium cassette and telescoping support pole |

The stand component is **optional**: customers can order banner printing only (e.g., as a replacement for an existing stand), or a complete set with a new stand.

---

## Allowed Materials

### Banner Film (Main component)

| Material ID | Name | Description |
|-------------|------|-------------|
| `mat-rollup-banner-film` | Polyester Banner Film 510gsm | Durable water-resistant polyester film, optimised for UV inkjet printing. The standard substrate for retractable roll-up banners. |

### Stand Types (Stand component — optional)

| Material ID | Name | Description |
|-------------|------|-------------|
| `mat-rollup-stand-economy` | Roll-Up Stand Economy | Aluminium retractable cassette with single-rail support pole. Suitable for short-term campaigns and occasional use. |
| `mat-rollup-stand-premium` | Roll-Up Stand Premium | Heavy-duty aluminium cassette with double-rail support and carry bag. Suitable for frequent use and professional exhibitions. |

---

## Available Finishes

| Finish ID | Name | Type | Description |
|-----------|------|------|-------------|
| `fin-overlamination` | Overlamination | `Overlamination` (Surface) | Optional protective matte overlamination film applied to the printed surface. Increases durability and reduces glare from ambient lighting. Applied to the front side only. |

---

## Printing Methods

| Method ID | Name | Notes |
|-----------|------|-------|
| `pm-uv-inkjet` | UV Curable Inkjet | Required. UV inkjet ensures vibrant, durable large-format print on polyester banner film. |

---

## Specifications

| Spec | Required | Description |
|------|----------|-------------|
| `Size` | Yes | Banner dimensions in mm (width × height) |
| `Quantity` | Yes | Number of banners (and stands, if selected) |

---

## Size Constraints

Standard roll-up sizes follow industry norms:

| Dimension | Min | Max |
|-----------|-----|-----|
| Width | 600 mm | 1 200 mm |
| Height | 1 600 mm | 2 400 mm |

Typical standard sizes: **850 × 2 000 mm**, **1 000 × 2 000 mm**, **1 200 × 2 000 mm**.

---

## Ink Configuration

- Only **CMYK** ink type is supported for the banner (Main) component.
- The stand (Stand) component uses `InkConfiguration.noInk` (`0/0`) — it is a physical hardware item and carries no printed ink.

---

## Pricing

Pricing is defined in `SamplePricelist` for USD and CZK pricelists.

### Banner Film

The banner film is priced by **area** (`MaterialAreaPrice`), based on the configured size spec (width × height in m²):

| Pricelist | Price per m² |
|-----------|-------------|
| USD | $12.00 |
| CZK | 280 Kč |

### Stand Types

Stands are priced by **unit** (`MaterialBasePrice`), one per ordered quantity:

| Stand | USD | CZK |
|-------|-----|-----|
| Economy | $25.00 | 590 Kč |
| Premium | $55.00 | 1 290 Kč |

### Overlamination

| Pricelist | Surcharge (per unit) |
|-----------|---------------------|
| USD | $2.50 (type-level `Overlamination`) |
| CZK | 60 Kč (type-level `Overlamination`) |

---

## Compatibility Rules

| Rule Type | Description |
|-----------|-------------|
| `SpecConstraint` — MinDimension(600, 1) | Banner width must be at least 600 mm |
| `SpecConstraint` — MaxDimension(1200, ∞) | Banner width must not exceed 1 200 mm |
| `SpecConstraint` — MinDimension(1, 1600) | Banner height must be at least 1 600 mm |
| `SpecConstraint` — MaxDimension(∞, 2400) | Banner height must not exceed 2 400 mm |
| `ConfigurationConstraint` — AllowedInkTypes(CMYK) | Only CMYK ink type is allowed (`InkType.None` on the stand component passes this check) |

---

## Category Definition Summary

```scala
val rollUps: ProductCategory = ProductCategory(
  id = rollUpsId,                                // "cat-roll-ups"
  name = LocalizedString("Roll-Up Banners", "Roll-up bannery"),
  components = List(
    ComponentTemplate(                            // Required banner component
      ComponentRole.Main,
      allowedMaterialIds = Set(rollUpBannerFilmId),
      allowedFinishIds = Set(overlaminationId),
    ),
    ComponentTemplate(                            // Optional stand component
      ComponentRole.Stand,
      allowedMaterialIds = Set(rollUpStandEconomyId, rollUpStandPremiumId),
      allowedFinishIds = Set.empty,
      optional = true,
    ),
  ),
  requiredSpecKinds = Set(SpecKind.Size, SpecKind.Quantity),
  allowedPrintingMethodIds = Set(uvInkjetId),
)
```

---

## Example Configurations

### Banner only (replacement print)

```
Category:        Roll-Up Banners
Printing method: UV Curable Inkjet
Banner:          Polyester Banner Film 510gsm, 4/0 CMYK
Finishes:        (none)
Size:            850 × 2000 mm
Quantity:        1
```

### Complete set with Economy stand

```
Category:        Roll-Up Banners
Printing method: UV Curable Inkjet
Banner:          Polyester Banner Film 510gsm, 4/0 CMYK, + Overlamination
Stand:           Roll-Up Stand Economy
Size:            850 × 2000 mm
Quantity:        3
```

### Premium exhibition set

```
Category:        Roll-Up Banners
Printing method: UV Curable Inkjet
Banner:          Polyester Banner Film 510gsm, 4/0 CMYK, + Overlamination
Stand:           Roll-Up Stand Premium
Size:            1000 × 2000 mm
Quantity:        5
```
