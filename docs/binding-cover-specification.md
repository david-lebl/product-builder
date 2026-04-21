# Binding & Cover Domain Specification

> Describes how bound products (calendars, booklets, case-bound books) model their physical components — covers, bodies, and binding hardware — and how pricing and validation work.

---

## Component Roles

Products with binding use these component roles (in addition to `Main` for single-component products):

| Role | Description | typical `optional` |
|------|-------------|---------|
| `FrontCover` | Front/top cover sheet | `true` (e.g. calendar: transparent plastic, optional) |
| `BackCover` | Back/bottom cover sheet | `false` (booklets always have a back cover) |
| `Body` | Interior pages/sheets | `false` |
| `Binding` | Binding hardware (coil, wire, board) | `false` for loop/case binding |

Each role has its own `ComponentTemplate` in `ProductCategory`, defining `allowedMaterialIds` and `optional`.

---

## Binding Method

`BindingMethod` enum in `specification.scala`:

| Value | Description |
|-------|-------------|
| `SaddleStitch` | Stapled through the spine; pages must be divisible by 4 |
| `PerfectBinding` | Glued spine; pages divisible by 2 |
| `LoopBinding` | Punched-edge coil or wire-O; pages divisible by 2. Material family (Plastic vs Metal) picks the physical variant |
| `CaseBinding` | Hard case cover glued to spine; no page-count divisibility constraint |

`LoopBinding` replaces the former `SpiralBinding` + `WireOBinding` split. The binding *material* (family `Plastic` or `Metal`) now distinguishes plastic coil from metal double-loop wire.

---

## Binding Material

A `Binding` component carries a `Material` with:

- `family: MaterialFamily` — must be `Plastic`, `Metal`, or `Cardboard` (for case-binding board)
- `attributes: Option[Set[MaterialAttribute]]` — physical specs (see below)
- `inkConfiguration = InkConfiguration.noInk`
- `finishes = Nil`
- `sheetCount = 1`

### MaterialAttribute

```scala
sealed trait MaterialAttribute
object MaterialAttribute:
  final case class MaxBoundEdgeLengthMm(value: Double)  // binding max length
  final case class MaxBoundThicknessMm(value: Double)   // binding max stack thickness
  final case class Color(hex: HexColor)                 // swatch color (#RRGGBB)
  final case class CoilPitchMm(value: Double)           // coil pitch (future)
```

`HexColor` is an opaque type over `String` validated as `#RRGGBB`.

---

## Material Family Extensions

Two new families in `MaterialFamily`:

| Family | Usage |
|--------|-------|
| `Plastic` | Transparent/tinted cover sheets; plastic coil binding |
| `Metal` | Metal wire-O binding materials |

Combined with `MaterialProperty.Transparent`, a front-cover plastic sheet is modelled as `family = Plastic, properties = Set(Transparent)`.

---

## Product Category — Bound Edge

`ProductCategory` carries `boundEdge: Option[BoundEdge]`. Set on categories whose `requiredSpecKinds` includes `BindingMethod`:

```scala
enum BoundEdge:
  case LongEdge, ShortEdge, Width, Height
```

| Category | `boundEdge` |
|----------|------------|
| Wall calendar (A4 portrait) | `Some(ShortEdge)` — bound across the top (210mm) |
| Desk calendar (A5 landscape) | `Some(LongEdge)` — bound across the long side |
| Booklets | `Some(Height)` — bound on the left/short spine |

`LongEdge` = `max(widthMm, heightMm)`, `ShortEdge` = `min(widthMm, heightMm)`.

---

## Compatibility Rules

| Rule | Trigger | Effect |
|------|---------|--------|
| `SpecConstraint(AllowedBindingMethods)` | Per-category | Validates `BindingMethodSpec` is in allowed set |
| `TechnologyConstraint(PagesDivisibleBy(2))` | Loop/PerfectBinding | Rejects odd page counts |
| `TechnologyConstraint(PagesDivisibleBy(4))` | SaddleStitch | Rejects page counts not divisible by 4 |
| `BindingMaterialConstrainsSize` _(planned)_ | Binding component present | Fails if `BoundEdge` dimension > `MaxBoundEdgeLengthMm` |
| `ComponentRequired` _(planned)_ | `LoopBinding`/`CaseBinding` chosen | Requires a `Binding` component to be present |

---

## Pricing — Binding

Three layers compose the binding price:

1. **`BindingMethodSurcharge(method, pricePerUnit)`** — variable cost per copy, discountable.
2. **`BindingMethodSetupFee(method, amount)`** — fixed machine-setup fee, not discounted.
3. **Binding component material price** — one of:
   - `MaterialLinearPrice(materialId, pricePerMeter)` — for coils/wires. The meter length is computed from `category.boundEdge` + `SizeSpec`.
   - `MaterialFixedPrice(materialId, pricePerUnit)` — for case-binding boards.
   - `MaterialBasePrice(materialId, unitPrice)` — for transparent cover sheets (FrontCover/BackCover).

The material price for the Binding component is included in `componentBreakdowns` (same as any other component); only the binding surcharge and setup fee appear in `bindingSurcharge` / `setupFees`.

### Linear price computation

```
boundEdgeMm = category.boundEdge match
  LongEdge  → max(widthMm, heightMm)
  ShortEdge → min(widthMm, heightMm)
  Width     → widthMm
  Height    → heightMm
  None      → max(widthMm, heightMm)   // fallback

pricePerUnit = pricePerMeter × (boundEdgeMm / 1000.0)
lineTotal    = pricePerUnit × quantity
```

---

## Manufacturing Workflow

`WorkflowGenerator` selects the binder station from the Binding component's material family:

| `material.family` | Station |
|---|---|
| `Plastic` | Loop-coil binder |
| `Metal` | Wire-O binder |
| `Cardboard` | Case-binding station |
| (none / SaddleStitch) | Saddle-stitch binder |

---

## Sample Materials

### Binding materials (partial)

| ID | Name | Family | `MaxBoundEdgeLengthMm` | Pricing |
|----|------|--------|------------------------|---------|
| `plastic-coil-a4-black` | Plastic Coil A4 Black | Plastic | 297 | `MaterialLinearPrice` |
| `plastic-coil-a4-white` | Plastic Coil A4 White | Plastic | 297 | `MaterialLinearPrice` |
| `plastic-coil-a3-black` | Plastic Coil A3 Black | Plastic | 420 | `MaterialLinearPrice` |
| `metal-wire-o-a4-silver` | Metal Wire-O A4 Silver | Metal | 297 | `MaterialLinearPrice` |
| `metal-wire-o-a4-black` | Metal Wire-O A4 Black | Metal | 297 | `MaterialLinearPrice` |
| `case-binding-board-black-350gsm` | Case Binding Board Black | Cardboard | — | `MaterialFixedPrice` |

### Front-cover transparent plastics

| ID | Name | Family | Properties |
|----|------|--------|-----------|
| `plastic-clear-200mic` | Transparent Plastic 200mic | Plastic | `Transparent` |
| `plastic-clear-300mic` | Transparent Plastic 300mic | Plastic | `Transparent` |

---

## See Also

- [`features.md`](features.md) — system overview
- [`pricing.md`](pricing.md) — full pricing rule reference
- [`analysis/domain-model-gap-analysis.md`](analysis/domain-model-gap-analysis.md) — original gap analysis motivating these changes
