# New Product Specification: T-Shirts, Eco Bags, Pin Badges, Cups & More

> Specification for extending the product catalog with promotional / merchandise products.
> Each product section defines the domain model entries (category, materials, finishes,
> printing methods), catalog showcase content, and visual editor considerations.

---

## Table of Contents

1. [Overview](#1-overview)
2. [T-Shirts](#2-t-shirts)
3. [Eco Bags (Tote Bags)](#3-eco-bags-tote-bags)
4. [Pin Badges](#4-pin-badges)
5. [Cups & Mugs](#5-cups--mugs)
6. [Suggested Additional Products](#6-suggested-additional-products)
7. [New Materials Summary](#7-new-materials-summary)
8. [New Finishes Summary](#8-new-finishes-summary)
9. [New Printing Methods Summary](#9-new-printing-methods-summary)
10. [Domain Model Changes Required](#10-domain-model-changes-required)

---

## 1. Overview

The current catalog focuses on **paper-based print products** (business cards, flyers, brochures, booklets, calendars, postcards, stickers, banners, roll-ups, packaging). This specification extends the catalog with **promotional / merchandise products** that are common in print shops and online customization platforms.

### New Catalog Group

The existing `CatalogGroup` enum has four variants: `Sheet`, `LargeFormat`, `Bound`, `Specialty`. For promotional products, a new group is needed:

```
CatalogGroup.Promotional  // Branded merchandise (T-shirts, bags, mugs, badges, etc.)
```

This keeps the catalog UI tab-based filtering clean and separates merchandise from traditional print products.

---

## 2. T-Shirts

### Category Definition

| Field | Value |
|-------|-------|
| **Category ID** | `cat-tshirts` |
| **Name (EN/CS)** | T-Shirts / Trička |
| **Components** | Single: `ComponentRole.Main` |
| **Required Specs** | `Size`, `Quantity` |
| **Printing Methods** | Screen Print, DTG (Direct-to-Garment), Sublimation |

### Materials

| Material ID | Name (EN) | Name (CS) | Family | Weight | Properties |
|-------------|-----------|-----------|--------|--------|------------|
| `mat-cotton-tshirt-150` | Cotton T-Shirt 150gsm | Bavlněné tričko 150g | Fabric | 150 gsm | Recyclable |
| `mat-cotton-tshirt-180` | Cotton T-Shirt 180gsm | Bavlněné tričko 180g | Fabric | 180 gsm | Recyclable |
| `mat-polyester-tshirt` | Polyester T-Shirt | Polyesterové tričko | Fabric | 140 gsm | WaterResistant |
| `mat-cotton-poly-blend` | Cotton-Polyester Blend T-Shirt | Směsové tričko bavlna-polyester | Fabric | 160 gsm | Recyclable |
| `mat-organic-cotton-tshirt` | Organic Cotton T-Shirt 180gsm | Bio bavlněné tričko 180g | Fabric | 180 gsm | Recyclable |

### Finishes (Post-Processing)

| Finish | Type | Side | Description |
|--------|------|------|-------------|
| Heat Press Transfer | `Mounting` | Front | Design transferred via heat press onto garment |
| Label / Tag Printing | `Numbering` | Back | Custom labels sewn or printed on collar/hem |
| Fold & Bag Packaging | `Binding` | Both | Individual folding and polybag packaging |

### Printing Methods

| Method ID | Name | Process Type | Max Colors | Notes |
|-----------|------|-------------|------------|-------|
| `pm-screen-print` | Screen Printing | ScreenPrint | 8 | Best for bulk orders, vibrant solid colors |
| `pm-dtg` | Direct-to-Garment (DTG) | Digital | Unlimited | Full-color photo prints, best for small runs |
| `pm-sublimation` | Dye Sublimation | Digital | Unlimited | Polyester only, all-over prints |

### Presets

| Preset | Description | Material | Method | Specs |
|--------|-------------|----------|--------|-------|
| Standard Cotton | White cotton tee, screen print, 50 pcs | Cotton 180gsm | Screen Print | M size, qty 50 |
| Premium DTG | Full-color photo print on organic cotton | Organic Cotton 180gsm | DTG | L size, qty 25 |
| Sublimation All-Over | All-over print on polyester | Polyester | Sublimation | M size, qty 100 |

### Showcase Content

| Field | Value |
|-------|-------|
| **Group** | `CatalogGroup.Promotional` |
| **Tagline (EN)** | "Custom branded T-shirts for every occasion" |
| **Tagline (CS)** | "Trička s vlastním potiskem na každou příležitost" |
| **Popular Finishes** | Screen Printing, DTG Full Color, Sublimation |
| **Turnaround** | 5–7 days |

---

## 3. Eco Bags (Tote Bags)

### Category Definition

| Field | Value |
|-------|-------|
| **Category ID** | `cat-eco-bags` |
| **Name (EN/CS)** | Eco Bags / Eko tašky |
| **Components** | Single: `ComponentRole.Main` |
| **Required Specs** | `Size`, `Quantity` |
| **Printing Methods** | Screen Print, Digital (DTG for fabric) |

### Materials

| Material ID | Name (EN) | Name (CS) | Family | Weight | Properties |
|-------------|-----------|-----------|--------|--------|------------|
| `mat-cotton-canvas-bag` | Cotton Canvas 220gsm | Bavlněné plátno 220g | Fabric | 220 gsm | Recyclable |
| `mat-organic-cotton-bag` | Organic Cotton Bag 180gsm | Bio bavlněná taška 180g | Fabric | 180 gsm | Recyclable |
| `mat-recycled-pet-bag` | Recycled PET Bag | Recyklovaná PET taška | Fabric | 150 gsm | Recyclable, WaterResistant |
| `mat-jute-bag` | Jute / Burlap Bag | Jutová taška | Fabric | 300 gsm | Recyclable, Textured |
| `mat-non-woven-pp-bag` | Non-Woven Polypropylene Bag | Netkaná PP taška | Fabric | 80 gsm | WaterResistant |

### Finishes

| Finish | Type | Side | Description |
|--------|------|------|-------------|
| Heat Press Transfer | `Mounting` | Front | Heat-applied vinyl or transfer print |
| Embroidery | N/A (new type needed — see §10) | Front | Thread-based logo/design application |
| Reinforced Handles | `Binding` | Both | Double-stitched handles for durability |

### Printing Methods

| Method | Process Type | Notes |
|--------|-------------|-------|
| Screen Print | ScreenPrint | Best for 1–3 color logos on canvas |
| DTG | Digital | Full-color photo prints on cotton |

### Presets

| Preset | Description | Material | Method | Specs |
|--------|-------------|----------|--------|-------|
| Standard Canvas | Natural cotton canvas, 1-color screen print, 100 pcs | Cotton Canvas 220gsm | Screen Print | 380×420mm, qty 100 |
| Organic Eco | Organic cotton, full-color DTG print | Organic Cotton 180gsm | DTG | 380×420mm, qty 50 |

### Showcase Content

| Field | Value |
|-------|-------|
| **Group** | `CatalogGroup.Promotional` |
| **Tagline (EN)** | "Sustainable branded bags that carry your message" |
| **Tagline (CS)** | "Ekologické tašky s vlastním potiskem" |
| **Popular Finishes** | Screen Printing, Heat Transfer, Embroidery |
| **Turnaround** | 7–10 days |

---

## 4. Pin Badges

### Category Definition

| Field | Value |
|-------|-------|
| **Category ID** | `cat-pin-badges` |
| **Name (EN/CS)** | Pin Badges / Odznaky |
| **Components** | Single: `ComponentRole.Main` |
| **Required Specs** | `Size`, `Quantity` |
| **Printing Methods** | Digital (printed insert), Offset (for larger runs) |

### Materials

| Material ID | Name (EN) | Name (CS) | Family | Weight | Properties |
|-------------|-----------|-----------|--------|--------|------------|
| `mat-tinplate-badge` | Tinplate Badge Blank | Plechový polotovar na odznak | Hardware | None | SmoothSurface |
| `mat-acrylic-badge` | Acrylic Badge Blank | Akrylátový polotovar na odznak | Hardware | None | SmoothSurface, Transparent |
| `mat-wooden-badge` | Wooden Badge Blank | Dřevěný polotovar na odznak | Hardware | None | Textured, Recyclable |

### Finishes

| Finish | Type | Side | Description |
|--------|------|------|-------------|
| Mylar Film Overlay | `Overlamination` | Front | Protective clear film over printed design |
| Safety Pin Back | `Mounting` | Back | Standard safety pin mechanism |
| Magnet Back | `Mounting` | Back | Magnetic backing instead of pin |
| Bottle Opener Back | `Mounting` | Back | Dual-purpose badge with bottle opener |

### Printing Methods

| Method | Process Type | Notes |
|--------|-------------|-------|
| Digital | Digital | Full-color printed insert under mylar |
| Offset | Offset | Cost-effective for large runs (1000+) |

### Presets

| Preset | Description | Material | Method | Specs |
|--------|-------------|----------|--------|-------|
| Standard Round | 58mm tinplate, digital print, safety pin, 100 pcs | Tinplate | Digital | Ø58mm, qty 100 |
| Small Round | 32mm tinplate, digital print, safety pin, 200 pcs | Tinplate | Digital | Ø32mm, qty 200 |
| Magnet Badge | 58mm tinplate, digital print, magnet back, 50 pcs | Tinplate | Digital | Ø58mm, qty 50 |

### Showcase Content

| Field | Value |
|-------|-------|
| **Group** | `CatalogGroup.Promotional` |
| **Tagline (EN)** | "Custom pin badges for events, brands, and campaigns" |
| **Tagline (CS)** | "Vlastní odznaky pro akce, značky a kampaně" |
| **Popular Finishes** | Mylar Overlay, Safety Pin, Magnet Back |
| **Turnaround** | 3–5 days |

---

## 5. Cups & Mugs

### Category Definition

| Field | Value |
|-------|-------|
| **Category ID** | `cat-cups` |
| **Name (EN/CS)** | Cups & Mugs / Hrnky a šálky |
| **Components** | Single: `ComponentRole.Main` |
| **Required Specs** | `Size`, `Quantity` |
| **Printing Methods** | Sublimation, Screen Print, Digital (UV direct print) |

### Materials

| Material ID | Name (EN) | Name (CS) | Family | Weight | Properties |
|-------------|-----------|-----------|--------|--------|------------|
| `mat-ceramic-mug-white` | White Ceramic Mug 330ml | Bílý keramický hrnek 330ml | Hardware | None | SmoothSurface |
| `mat-ceramic-mug-colored` | Colored Ceramic Mug 330ml | Barevný keramický hrnek 330ml | Hardware | None | SmoothSurface |
| `mat-magic-mug` | Magic Color-Changing Mug 330ml | Magický měnící hrnek 330ml | Hardware | None | SmoothSurface |
| `mat-stainless-travel-mug` | Stainless Steel Travel Mug 450ml | Nerezový cestovní hrnek 450ml | Hardware | None | SmoothSurface, WaterResistant |
| `mat-enamel-mug` | Enamel Mug 350ml | Smaltovaný hrnek 350ml | Hardware | None | SmoothSurface |
| `mat-glass-mug` | Glass Mug 300ml | Skleněný hrnek 300ml | Hardware | None | SmoothSurface, Transparent |

### Finishes

| Finish | Type | Side | Description |
|--------|------|------|-------------|
| Dishwasher-Safe Coating | `Overlamination` | Both | Protective coating for durability in dishwashers |
| Gift Box Packaging | `Binding` | Both | Individual gift box for each mug |
| Glossy Glaze | `UVCoating` | Both | High-gloss ceramic glaze finish |

### Printing Methods

| Method | Process Type | Notes |
|--------|-------------|-------|
| Sublimation | Digital | Standard for white ceramic mugs, wrap-around |
| Screen Print | ScreenPrint | 1–3 color logos, durable |
| UV Direct Print | UVCurableInkjet | Full-color on any surface including colored mugs |

### Presets

| Preset | Description | Material | Method | Specs |
|--------|-------------|----------|--------|-------|
| Standard White Mug | White ceramic 330ml, sublimation, 50 pcs | White Ceramic | Sublimation | 330ml, qty 50 |
| Corporate Gift Set | White ceramic 330ml, sublimation, gift box, 25 pcs | White Ceramic | Sublimation | 330ml, qty 25 |
| Travel Mug | Stainless 450ml, UV print, 20 pcs | Stainless Steel | UV Direct Print | 450ml, qty 20 |

### Showcase Content

| Field | Value |
|-------|-------|
| **Group** | `CatalogGroup.Promotional` |
| **Tagline (EN)** | "Personalized mugs and cups for gifts and branding" |
| **Tagline (CS)** | "Personalizované hrnky a šálky pro dárky a branding" |
| **Popular Finishes** | Sublimation Wrap-Around, UV Direct Print, Gift Box |
| **Turnaround** | 5–7 days |

---

## 6. Suggested Additional Products

Beyond the four requested products, these are popular promotional/merchandise items commonly offered by print shops:

### 6.1 Pens & Writing Instruments

| Field | Value |
|-------|-------|
| **Category ID** | `cat-pens` |
| **Name** | Pens / Propisky |
| **Materials** | Plastic pen body, Metal pen body, Bamboo pen body |
| **Material Family** | Hardware |
| **Printing Methods** | Pad Printing (new process type), UV Direct Print |
| **Finishes** | Engraving (new finish type), Gift box packaging |
| **Components** | Single: `ComponentRole.Main` |
| **Required Specs** | `Quantity` |
| **Typical Sizes** | Standard ballpoint, slim, thick barrel |
| **Typical Quantities** | 100–5000 pcs |

### 6.2 Lanyards & Keychains

| Field | Value |
|-------|-------|
| **Category ID** | `cat-lanyards` |
| **Name** | Lanyards & Keychains / Šňůrky a klíčenky |
| **Materials** | Polyester webbing 20mm, Nylon webbing 15mm, Silicone keychain |
| **Material Family** | Fabric (webbing), Hardware (keychains) |
| **Printing Methods** | Sublimation (full-color lanyards), Screen Print |
| **Finishes** | Safety breakaway clip, Metal hook, Retractable badge reel |
| **Components** | Single: `ComponentRole.Main` |
| **Required Specs** | `Size`, `Quantity` |

### 6.3 Caps & Hats

| Field | Value |
|-------|-------|
| **Category ID** | `cat-caps` |
| **Name** | Caps & Hats / Čepice a kšiltovky |
| **Materials** | Cotton cap, Polyester cap, Trucker mesh cap |
| **Material Family** | Fabric |
| **Printing Methods** | Screen Print, Sublimation, DTG |
| **Finishes** | Embroidery, Heat transfer, Flat/3D embroidery |
| **Components** | Single: `ComponentRole.Main` |
| **Required Specs** | `Size` (One Size / S-M-L), `Quantity` |

### 6.4 USB Flash Drives

| Field | Value |
|-------|-------|
| **Category ID** | `cat-usb-drives` |
| **Name** | USB Flash Drives / USB flash disky |
| **Materials** | Plastic USB body, Wooden USB body, Metal USB body, Leather USB body |
| **Material Family** | Hardware |
| **Printing Methods** | UV Direct Print, Pad Printing |
| **Finishes** | Laser engraving, Gift box, Data preloading |
| **Components** | Single: `ComponentRole.Main` |
| **Required Specs** | `Quantity` |

### 6.5 Notebooks & Notepads

| Field | Value |
|-------|-------|
| **Category ID** | `cat-notebooks` |
| **Name** | Notebooks & Notepads / Zápisníky a bločky |
| **Materials** | Coated cover (300gsm) + Uncoated body (80gsm), Kraft cover + recycled body |
| **Material Family** | Paper |
| **Printing Methods** | Digital, Offset |
| **Finishes** | Matte lamination, Foil stamping, Wire-O binding, Glue binding, Elastic band |
| **Components** | Multi-component: `Cover` + `Body` |
| **Required Specs** | `Size`, `Quantity`, `Pages`, `BindingMethod` |

### 6.6 Mousepads

| Field | Value |
|-------|-------|
| **Category ID** | `cat-mousepads` |
| **Name** | Mousepads / Podložky pod myš |
| **Materials** | Rubber base + fabric top, Rubber base + hard surface |
| **Material Family** | Fabric (top), Hardware (base) |
| **Printing Methods** | Sublimation |
| **Finishes** | Stitched edge, Non-slip base |
| **Components** | Single: `ComponentRole.Main` |
| **Required Specs** | `Size`, `Quantity` |

### 6.7 Phone Cases

| Field | Value |
|-------|-------|
| **Category ID** | `cat-phone-cases` |
| **Name** | Phone Cases / Obaly na telefon |
| **Materials** | Hard plastic case, Soft silicone case, Leather flip case |
| **Material Family** | Hardware |
| **Printing Methods** | UV Direct Print, Sublimation |
| **Finishes** | Matte coating, Glossy coating, Rubber grip |
| **Components** | Single: `ComponentRole.Main` |
| **Required Specs** | `Size` (phone model), `Quantity` |

### 6.8 Umbrellas

| Field | Value |
|-------|-------|
| **Category ID** | `cat-umbrellas` |
| **Name** | Umbrellas / Deštníky |
| **Materials** | Polyester canopy + metal frame, Pongee canopy + fiberglass frame |
| **Material Family** | Fabric |
| **Printing Methods** | Sublimation (all-over), Screen Print (panel print) |
| **Finishes** | UV-resistant coating, Wind-resistant frame |
| **Components** | Single: `ComponentRole.Main` |
| **Required Specs** | `Size`, `Quantity` |

---

## 7. New Materials Summary

All new materials needed across the proposed products:

| ID | Name | Family | Weight | Key Properties |
|----|------|--------|--------|---------------|
| `mat-cotton-tshirt-150` | Cotton T-Shirt 150gsm | Fabric | 150 | Recyclable |
| `mat-cotton-tshirt-180` | Cotton T-Shirt 180gsm | Fabric | 180 | Recyclable |
| `mat-polyester-tshirt` | Polyester T-Shirt | Fabric | 140 | WaterResistant |
| `mat-cotton-poly-blend` | Cotton-Polyester Blend | Fabric | 160 | Recyclable |
| `mat-organic-cotton-tshirt` | Organic Cotton T-Shirt 180gsm | Fabric | 180 | Recyclable |
| `mat-cotton-canvas-bag` | Cotton Canvas 220gsm | Fabric | 220 | Recyclable |
| `mat-organic-cotton-bag` | Organic Cotton Bag 180gsm | Fabric | 180 | Recyclable |
| `mat-recycled-pet-bag` | Recycled PET Bag | Fabric | 150 | Recyclable, WaterResistant |
| `mat-jute-bag` | Jute / Burlap Bag | Fabric | 300 | Recyclable, Textured |
| `mat-non-woven-pp-bag` | Non-Woven PP Bag | Fabric | 80 | WaterResistant |
| `mat-tinplate-badge` | Tinplate Badge Blank | Hardware | — | SmoothSurface |
| `mat-acrylic-badge` | Acrylic Badge Blank | Hardware | — | SmoothSurface, Transparent |
| `mat-wooden-badge` | Wooden Badge Blank | Hardware | — | Textured, Recyclable |
| `mat-ceramic-mug-white` | White Ceramic Mug 330ml | Hardware | — | SmoothSurface |
| `mat-ceramic-mug-colored` | Colored Ceramic Mug 330ml | Hardware | — | SmoothSurface |
| `mat-magic-mug` | Magic Color-Changing Mug | Hardware | — | SmoothSurface |
| `mat-stainless-travel-mug` | Stainless Steel Travel Mug | Hardware | — | SmoothSurface, WaterResistant |
| `mat-enamel-mug` | Enamel Mug 350ml | Hardware | — | SmoothSurface |
| `mat-glass-mug` | Glass Mug 300ml | Hardware | — | SmoothSurface, Transparent |

### MaterialFamily Consideration

The existing `MaterialFamily` enum (`Paper`, `Vinyl`, `Cardboard`, `Fabric`, `Hardware`) already covers the needs:
- **Fabric** — T-shirts, bags, caps, lanyards, umbrellas, mousepads
- **Hardware** — Mugs, badges, pens, USB drives, phone cases

No new `MaterialFamily` variants are required, though `Ceramic`, `Metal`, and `Plastic` sub-families could be useful in the future.

---

## 8. New Finishes Summary

| ID | Name | FinishType | Side | Products |
|----|------|-----------|------|----------|
| `fin-heat-press` | Heat Press Transfer | Mounting | Front | T-shirts, bags |
| `fin-label-print` | Label / Tag Printing | Numbering | Back | T-shirts |
| `fin-fold-bag` | Fold & Bag Packaging | Binding | Both | T-shirts, bags |
| `fin-mylar-overlay` | Mylar Film Overlay | Overlamination | Front | Badges |
| `fin-safety-pin` | Safety Pin Back | Mounting | Back | Badges |
| `fin-magnet-back` | Magnet Back | Mounting | Back | Badges |
| `fin-bottle-opener` | Bottle Opener Back | Mounting | Back | Badges |
| `fin-dishwasher-coat` | Dishwasher-Safe Coating | Overlamination | Both | Mugs |
| `fin-gift-box` | Gift Box Packaging | Binding | Both | Mugs, pens |
| `fin-glossy-glaze` | Glossy Ceramic Glaze | UVCoating | Both | Mugs |

---

## 9. New Printing Methods Summary

| ID | Name | Process Type | Max Colors | Products |
|----|------|-------------|------------|----------|
| `pm-screen-print` | Screen Printing | ScreenPrint | 8 | T-shirts, bags, mugs, caps |
| `pm-dtg` | Direct-to-Garment | Digital | Unlimited (4+) | T-shirts, bags, caps |
| `pm-sublimation` | Dye Sublimation | Digital | Unlimited (4+) | T-shirts (polyester), mugs, mousepads, lanyards |

Note: `ScreenPrint` already exists as `PrintingProcessType.ScreenPrint` in the domain model. `Digital` covers DTG and sublimation. UV Direct Print maps to existing `UVCurableInkjet`.

---

## 10. Domain Model Changes Required

### 10.1 CatalogGroup Extension

```scala
enum CatalogGroup:
  case Sheet
  case LargeFormat
  case Bound
  case Specialty
  case Promotional  // NEW
```

**Impact**: UI `ProductCatalogApp` needs a new tab filter, badge label, and section header for the `Promotional` group.

### 10.2 MaterialFamily — No Changes Needed

`Fabric` and `Hardware` already cover all proposed materials. If finer-grained classification is desired in the future, sub-family discriminators could be added.

### 10.3 FinishType — Potential Extension

The existing `FinishType` enum (23 variants) mostly covers the needs via reuse:
- Heat press → `Mounting`
- Mylar overlay → `Overlamination`
- Safety pin / magnet → `Mounting`

**Optional new variant**: `Embroidery` — if embroidery on garments/bags is a first-class finish. This would require:
```scala
enum FinishType:
  // ... existing 23 variants ...
  case Embroidery  // NEW: thread-based decoration on fabric
```

And updating the `finishCategory` extension method accordingly.

### 10.4 SpecKind — Potential Extension

For garment sizes (S/M/L/XL), a new spec kind may be useful:
```scala
enum SpecKind:
  // ... existing variants ...
  case GarmentSize  // NEW: for T-shirts, caps, etc.
```

Alternatively, garment sizing can be modeled as product variations (presets with size in the name) rather than a new spec kind. This is simpler and avoids domain model changes.

### 10.5 Compatibility Rules

New rules needed for promotional products:

| Rule | Type | Reason |
|------|------|--------|
| Sublimation requires polyester/white ceramic | `MaterialFamilyFinishTypeIncompatible` or custom | Sublimation only works on polymer-coated or polyester surfaces |
| Screen print max 8 colors | Handled by `PrintingMethod.maxColorCount` | Already in domain model |
| Embroidery not on hardware | `MaterialFamilyFinishTypeIncompatible` | Can't embroider on metal/ceramic |
| DTG only on fabric | `FinishRequiresPrintingProcess` or `MaterialFamilyFinishTypeIncompatible` | DTG printers print on textile |

### 10.6 Pricing Rules

Each new product needs pricing rules in all pricelists (USD, CZK, CZK Sheet):

| Rule Type | Example |
|-----------|---------|
| `MaterialBasePrice` | Cotton T-Shirt 180gsm = 85 CZK/unit |
| `MaterialBasePrice` | White Ceramic Mug = 45 CZK/unit |
| `MaterialBasePrice` | Tinplate Badge = 8 CZK/unit |
| `CategorySurcharge` | T-shirts category = 15 CZK/unit (handling) |
| `FinishSetupFee` | Screen print setup = 500 CZK/color |
| `FinishSurcharge` | Gift box = 35 CZK/unit |
| `QuantityTier` | Existing tiers can be reused or new tiers defined per category |
| `MinimumOrderPrice` | Per-category minimums (e.g., T-shirts min 2000 CZK) |
