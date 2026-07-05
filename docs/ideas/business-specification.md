# Product Builder — Business Specification

> A description of what the system does, for whom, and how it behaves — written for a business audience. No technology, programming language, or software-architecture detail is included here; this document describes capabilities and rules, not implementation. Where a described capability is only partially built or not yet connected to the rest of the system, that is called out explicitly rather than glossed over — see Section 15.

---

## 1. Purpose

The system is an online product configurator, marketing catalog, and print-shop management platform for a printing business. It serves four groups of people:

1. **Prospective customers browsing**, who explore a marketing-style product gallery to see what's on offer, with photography, feature highlights, and starting prices, before committing to configure anything.
2. **Customers configuring and ordering**, who design and order printed products — business cards, flyers, brochures, banners, booklets, calendars, packaging, stickers, promotional merchandise, and more — choosing materials, finishes, and sizes, and seeing an accurate, itemized price before they buy.
3. **Print shop staff and managers**, who receive approved orders and move them through production — printing, cutting, laminating, folding, binding, quality control, packaging, and dispatch — with visibility into workload, deadlines, and bottlenecks.
4. **Catalog administrators**, who maintain the product range, materials, finishes, prices, and business rules that govern what can be ordered — without needing a software update to change them.

The guiding idea is that a customer should never be able to configure something that cannot actually be produced, and should always see a correct, fully itemized price for what they've configured — including when production is running at rush speed or the shop is unusually busy.

---

## 2. Product Catalog

### 2.1 Product Categories

The catalog is organized into **15 product categories**, split into a core print range and a promotional-merchandise range:

**Core print products:** Business Cards, Postcards, Flyers, Brochures, Booklets, Calendars, Banners, Packaging, Stickers & Labels, Roll-Up Banners, and a general-purpose "Free Configuration" category that allows any material and any finish in the whole catalog, intended for custom/one-off requests that don't fit a standard category.

**Promotional merchandise:** T-Shirts, Eco Bags (tote bags), Pin Badges, and Cups & Mugs — branded giveaway items produced with garment/object-printing methods (screen printing, direct-to-garment, dye sublimation) rather than paper printing.

Each category defines:
- which materials, finishes, and printing methods are allowed for it,
- which product details (size, quantity, orientation, etc.) the customer must provide,
- for products made of multiple physical parts (e.g. a booklet's cover and inner pages, a roll-up banner's fabric plus its stand), a separate material/finish choice for each part — parts are called "components" and are labeled by role: Main, Cover, Body, or Stand. A component can be marked optional (e.g. the Roll-Up Banner's Stand — a customer can order just the printed banner film without a stand).
- a set of ready-made "quick order" preset configurations — e.g. Business Cards offers a "Basic" preset (standard paper, no finishes, common size/quantity) and a "Premium" preset (heavier paper, lamination, rounded corners) so a customer can start from a sensible default and adjust from there instead of building from a blank form.

### 2.2 Materials

The catalog includes a wide range of materials:
- **Papers** — coated glossy and coated matte art paper across nine weight grades (90 through 350 gsm), plus uncoated bond paper, kraft paper, coated silk paper, cotton (textured) paper, and synthetic waterproof paper.
- **Large-format materials** — adhesive vinyl, clear adhesive vinyl, and heavy-duty PVC banner material.
- **Cardboard** — corrugated cardboard for packaging.
- **Adhesive stock** — for stickers and labels.
- **Roll-up hardware** — banner fabric plus economy or premium retractable stands.
- **Promotional fabrics and blanks** — cotton, polyester, and blended T-shirt fabric in multiple weights; canvas, organic cotton, recycled, and jute tote bag fabric; tinplate, acrylic, and wooden badge blanks; ceramic, stainless steel, enamel, and glass mug blanks.

Each material carries a weight/thickness where applicable, and a set of properties (e.g. glossy, matte, textured, water-resistant, recyclable, transparent) that compatibility rules and descriptions can refer to.

### 2.3 Finishes

Finishing options include lamination (matte/gloss), UV coating, soft-touch coating, aqueous coating, embossing/debossing, foil stamping, spot varnish, die-cutting, kiss-cutting (for peel-off sticker sheets), scoring/creasing, perforation, rounded corners, grommets (eyelets), overlamination (extra protective layer for outdoor graphics), and a gum-rope tensioning accessory for banners. Promotional-merchandise finishes include heat-press transfer, embroidery, custom label/tag printing, fold-and-bag packaging, mylar overlay, safety-pin/magnet/bottle-opener attachments for badges, dishwasher-safe coating, glossy ceramic glaze, gift-box packaging, and reinforced handles for bags.

Each finish belongs to a finish type (e.g. many different lamination products all belong to the "Lamination" type, so a rule or price can apply to "any lamination" without listing every one), and applies to a particular side of the product (front, back, or both).

Some finishes take an extra customer-chosen parameter, priced independently rather than as a flat add-on:
- **Scoring/creasing** — the customer specifies how many creases they need; each crease count has its own price (not simply "price × number of creases").
- **Grommets** — the customer specifies the spacing between eyelets; wider spacing means fewer grommets and a lower price per square meter.
- **Round corners** — the customer specifies how many corners (1–4) and the corner radius.

### 2.4 Printing Methods

Products can be produced using: **Digital printing** (no plate setup, cost-effective for shorter runs, unlimited colors), **Offset printing** (traditional plate-based printing for large runs, up to 6 colors, supports precise Pantone spot-color matching), **Letterpress** (artisan relief printing that presses ink into thick paper for a tactile debossed look, limited to 1–2 colors, a premium choice for luxury cards), and **UV Curable Inkjet** (large-format printing with weather-resistant UV-cured ink, used for banners and outdoor graphics, unlimited colors). Promotional merchandise additionally supports **Screen Printing** (best for bulk orders of vibrant solid colors, up to 8 colors), **Direct-to-Garment (DTG)** (full-color photo-quality prints, best for small runs), and **Dye Sublimation** (all-over prints on polyester/coated surfaces).

### 2.5 Ink Configuration

Customers choose an ink setup describing how many colors print on the front and back — common presets include full-color both sides, full-color front only, full-color front with black-only back, black-only one side, and black-only both sides. The system checks that the chosen setup is actually achievable with the selected printing method (for example, it won't allow more ink colors than a method supports).

### 2.6 Product Details (Specifications)

Depending on the category, a customer is asked for some combination of: size, quantity, page orientation, bleed (extra printable margin for trimming), number of pages, fold type (half, tri-fold, gate fold, accordion, Z-fold, roll fold, French fold, cross fold), binding method (saddle-stitch, perfect binding, spiral binding, wire-o binding, case binding), and production speed (see Section 8 for how speed affects price and availability).

### 2.7 Multi-Currency Pricing

Price lists are currency-specific — the sample data includes both a US-dollar list and Czech-koruna lists — so the same catalog of products, materials, and finishes can be priced independently for different markets without changing the product definitions themselves.

---

## 3. Product Showcase — Browse Before You Configure

Before jumping into the step-by-step configurator, customers can browse a marketing-style product gallery organized into five groups: **Sheet** products (business cards, flyers, brochures, postcards), **Bound** products (booklets, calendars), **Large Format** (banners, roll-ups), **Specialty** (packaging, stickers & labels), and **Promotional** (branded merchandise).

Each showcased product includes: a tagline and a longer bilingual description, a hero image and a photo gallery, a list of named variations (e.g. "Basic" vs. "Premium" business cards) that link straight into the configurator pre-loaded with that variation's preset, a list of feature highlights (e.g. "Custom Sizes," "Premium Finishes" — each with an icon, title, and short description), optional step-by-step ordering instructions, a "popular finishes" tag list, an estimated turnaround range shown as marketing copy (e.g. "3–5 days"), and a display icon.

The gallery view shows a hero banner, filter tabs by product group, and a grid of product cards (image, group badge, name, tagline, turnaround estimate, number of variations available). Clicking through to a product's detail page shows the full gallery and description, a prominent "Configure & Order" call to action, clickable variation cards that show a live computed "from [price]" for each variation, feature highlights, ordering instructions, and the list of materials actually available for that category (pulled live from the real catalog, so the marketing page never advertises a material that isn't really orderable).

---

## 4. Compatibility Rules — What Can Be Combined

Rather than being hard-coded per product, compatibility is governed by a library of business rules that can each be added, removed, or adjusted independently. In plain terms, the kinds of rules the system supports are:

- **Material/finish conflicts** — a specific material and a specific finish cannot be combined (e.g. a very thin paper can't take embossing).
- **Required finishes** — a material requires a certain finish to be usable at all.
- **Property requirements** — a finish can only be applied to materials that have a certain property (e.g. a coating that only works on coated stock).
- **Mutually exclusive finishes** — certain specific finishes, or certain whole finish *types*, cannot be selected together on the same product (e.g. two competing coatings).
- **Weight requirements** — a finish type requires the material to be at least a certain weight/thickness (e.g. embossing needs sufficiently thick stock).
- **One finish per category** — only one finish from a given group may be selected (e.g. only one type of surface coating at a time).
- **Finish dependencies** — a finish requires another finish type to also be present, or requires a specific printing process to have been chosen.
- **Size and quantity limits** — a category can restrict the allowed size range or order quantity.
- **Configuration-wide constraints** — restrictions that look at the whole order together (e.g. certain ink type and finish combinations aren't allowed).
- **Cross-category technical limits** — global constraints that apply everywhere, such as a maximum page count for a given binding method, or weight-based limits that apply regardless of product category.
- **Crease-count limits for scoring** — the maximum number of creases (folds/scores) allowed can be capped by product category, by material, or by printing method, since some machines or materials can't handle too many creases.

When a customer's configuration violates a rule, or is missing required information, the system reports **every** problem at once (not just the first one it finds), so a customer with three issues in their configuration sees all three in a single pass rather than fixing them one at a time.

To keep the experience smooth, the ordering interface only ever shows options that are still valid given what's already been chosen — for example, once an incompatible material is picked, finishes that don't work with it are hidden or disabled rather than being offered and then rejected.

There is a further, separate layer of restriction specifically for **rush (Express) production speed**, covered in Section 8.2, since some finishing choices simply cannot be rushed regardless of the standard compatibility rules.

---

## 5. How Pricing Is Calculated

Pricing is entirely rule-driven: a price list is a collection of independent pricing rules (currency, base prices, surcharges, discounts, fees), and the calculator combines whichever rules apply to a given order. This section explains the calculation from raw materials cost through to the final total, in the exact order it is actually performed.

### 5.1 Step 1 — Quantity

The calculation starts from the quantity the customer has ordered. If no quantity has been specified, pricing cannot proceed.

### 5.2 Step 2 — Base Material Cost

The cost of the material is worked out using whichever pricing method applies to that material, checked in this order of priority:

1. **Tiered area pricing** — for large-format materials priced per square meter, where the per-square-meter rate itself changes at certain size thresholds (e.g. bigger banners get a lower rate per square meter).
2. **Flat area pricing** — a simple price per square meter, multiplied by the item's actual area.
3. **Sheet pricing** — for materials cut from standard press sheets, the system works out how many physical sheets are needed to produce the order (accounting for how many copies fit per sheet, including bleed and gutter margins) and prices per sheet.
4. **Flat per-unit pricing** — a simple price per finished item, used as the fallback when none of the above apply (this is how promotional merchandise like T-shirts, bags, badges, and mugs are priced — a flat cost per item, since they aren't cut from a press sheet).

If a material has no price configured, or a size-dependent pricing method is used but no size was provided, the order cannot be priced and the customer is told exactly what's missing.

### 5.3 Step 3 — Ink Cost

If the price list defines an additional cost for the chosen printing method and ink setup (i.e. how many colors print on front/back), that cost is added — priced per sheet for sheet-based materials, or per square meter for area-based materials. If no such rule exists for the chosen combination (this is normal for the promotional printing methods — screen printing, DTG, sublimation), no additional ink charge is added; the assumption is that ink is already included in the base price or category surcharge.

### 5.4 Step 4 — Finish Charges

Each finish selected is priced using whichever of the following applies, in this order:

1. **Crease-count pricing** — for scoring/creasing, the price is looked up by the *exact* number of creases chosen. If the exact crease count requested has no price defined, the system raises an error rather than silently charging nothing.
2. **Grommet spacing pricing** — for grommets (eyelets), the price is based on spacing between grommets and the item's area, with tiers similar to the area pricing described above.
3. **Linear-length pricing** — for accessories like ropes on banners, priced per meter of length.
4. **Standard finish surcharge** — a straightforward surcharge for the finish, applied per finished item, per sheet, or per square meter depending on how the underlying material is priced. A surcharge tied to a specific finish product takes precedence over a more general surcharge that applies to its whole finish type.

If none of the above pricing methods apply to a given finish, it is treated as included at no extra charge.

### 5.5 Step 5 — Process, Category, Fold, and Binding Charges

Additional flat, per-item surcharges are added if configured for: the chosen printing process, the product category itself (used, for example, to add a flat surcharge to T-shirts, eco bags, or mugs to cover handling), the chosen fold type, and the chosen binding method.

### 5.6 Step 6 — Subtotal

All of the per-item charges above (material, ink, finishes, process, category, fold, binding) are added together into a subtotal.

### 5.7 Step 7 — Volume Discount

A volume discount is applied to the subtotal based on how much is being ordered. There are two possible ways this is measured, and the system picks whichever is more appropriate for the order:

- **By physical sheets** — if the product uses sheet-based materials, the discount tier is based on the total number of physical press sheets the order requires (a bigger, more efficient press run earns a bigger discount).
- **By quantity** — otherwise, the discount tier is based simply on the number of finished items ordered.

In both cases, the applicable discount tier is the most generous one the order actually qualifies for (e.g. "1,000 units or more" beats "500 units or more" once the order reaches 1,000). If neither kind of tier is configured for a given price list, no volume discount is applied at all.

### 5.8 Step 8 — Production Speed Surcharge

If the customer selected a non-default production speed (see Section 8 for the full rush-order system), a surcharge or discount multiplier for that speed tier is applied to the discounted subtotal at this point — **after** the volume discount but **before** one-time setup fees are added. Choosing a slower, cheaper tier (Economy) reduces the subtotal; choosing rush (Express) increases it, and the increase can grow further if the shop's production queue is currently busy (Section 8.2).

### 5.9 Step 9 — One-Time Setup Fees

Certain finishes, fold types, and binding methods carry a one-time setup fee — representing real costs like machine changeovers that don't scale with order size. These fees are:

- charged once per distinct finish/fold/binding method even if it's used on multiple parts of a multi-component product (e.g. lamination on both a booklet's cover and its inner pages is still only one setup fee),
- **added after** the volume discount and the production-speed adjustment — setup fees are never discounted or rush-surcharged, since they represent a fixed real-world cost regardless of order size or speed,
- for creasing/scoring specifically, governed by its own dedicated one-time fee, which — if configured — always takes priority over a more generic finish-type setup fee.

### 5.10 Step 10 — Minimum Order Price

If, after everything else, the total falls below a configured minimum order value, the price is raised to that minimum. The customer is shown that the minimum was applied, rather than the calculation simply looking like a flat minimum charge with no explanation.

### 5.11 Step 11 — Final Rounding

The final total, and every intermediate subtotal along the way, is rounded to the nearest cent (two decimal places) as it is produced, so no rounding error can silently accumulate across steps.

### 5.12 What the Customer Sees

The price breakdown shown to the customer itemizes: the material cost, the ink cost (if any), each finish charge, process/category/fold/binding surcharges, the subtotal, the volume discount applied, the production-speed adjustment (if any), any one-time setup fees, whether a minimum order price kicked in, and the final total — broken down separately for each physical component of a multi-part product (e.g. cover vs. body of a booklet).

### 5.13 Worked Examples

**Business cards** — 500 cards, coated art paper, matte lamination, offset printing, full color both sides:

```
Material                              $0.08 × 500 =  $40.00
Ink (offset, full color both sides)   $0.04 × 500 =  $20.00
Matte lamination                      $0.03 × 500 =  $15.00
                                                    ─────────
Subtotal                                           =  $75.00
Volume discount (250–999 units)                    ×    0.90
                                                    ─────────
Total                                              =  $67.50
```

**Tri-fold brochure with setup fee** — 100 brochures, sheet-priced paper, matte lamination, tri-fold, priced in CZK:

```
Material (sheet-based)                    2,000.00 CZK
Matte lamination surcharge                  200.00 CZK
Tri-fold surcharge                          150.00 CZK
                                          ────────────
Subtotal                                  2,350.00 CZK
Sheet-volume discount                     ×     0.90
                                          ────────────
Discounted subtotal                       2,115.00 CZK
One-time: matte lamination setup             +50.00 CZK
One-time: tri-fold setup                     +80.00 CZK
                                          ────────────
Total                                     2,245.00 CZK
```

**Creased brochure (scoring)** — 500 brochures, digital printing, 2 creases:

```
Material                                  6,000.00 CZK
Ink (digital, full color both sides)      1,500.00 CZK
Creasing (2 creases, exact-match price)     500.00 CZK
                                          ────────────
Subtotal                                  8,000.00 CZK
Volume discount (500–999 units)           ×     0.85
                                          ────────────
Discounted subtotal                       6,800.00 CZK
One-time: creasing setup fee                 +60.00 CZK
                                          ────────────
Total                                     6,860.00 CZK
```

Choosing 3 creases instead of 2 does not simply scale this price proportionally — it looks up its own independently-configured price, since a pricing rule exists for each specific crease count offered.

**Large-format banner (area-based)** — 10 banners, 1m × 0.5m each, adhesive vinyl, UV coating, UV inkjet printing, full color both sides:

```
Area per banner: 0.5 m²
Material:  $16.20/m² × 0.5 m²  = $8.10 per banner
Ink:        $1.80/m² × 0.5 m²  = $0.90 per banner
UV coating: $0.04/m² × 0.5 m²  = $0.02 per banner

Material line (×10 banners)                 $81.00
Ink line      (×10 banners)                  $9.00
Finish line   (×10 banners)                  $0.20
                                            ────────
Subtotal                                    $90.20
Volume discount (1–249 units, none)         ×  1.00
                                            ────────
Total                                       $90.20
```

**Rush order with a busy shop** — same brochure order as above, but with Express speed selected while the busiest production station is running at 75% of its capacity:

```
Discounted subtotal (after volume discount)     6,800.00 CZK
Express base surcharge                          × 1.35
Extra surcharge (queue ≥70% busy)               + 0.15
                                                ────────────
Effective speed multiplier                      × 1.50
Subtotal after speed adjustment                10,200.00 CZK
One-time: creasing setup fee                       +60.00 CZK
                                                ────────────
Total                                           10,260.00 CZK
```

The extra rush surcharge for a busy shop is capped — the combined speed multiplier can never exceed a configured ceiling (2.0× in the sample data), so a customer paying for rush service is protected from an unbounded surge price even if the shop is completely saturated. See Section 8.2 for the full rush-pricing mechanism, including when Express is refused outright.

### 5.14 Customer-Specific Pricing

Individual customer accounts can be given their own pricing overlay on top of the standard price list, applied in order of specificity (the most specific override wins):

1. A fixed override price on one specific material (replaces the standard price entirely).
2. A percentage discount on one specific material.
3. A percentage discount on an entire product category.
4. A blanket percentage discount across the customer's whole account.

On top of this, a customer can separately have: a percentage discount on a specific finish, an entirely custom set of volume-discount tiers (replacing the standard ones), and a custom minimum-order override. This lets the shop show a corporate customer both the standard price and "your price" side by side, and negotiate pricing at whatever level of granularity makes sense for that account (see Section 15 for a caveat about the "customer tier" label, which is separate from this overlay system and currently has no automatic effect).

---

## 6. Visual Product Editor

For products where the design itself is part of what's being ordered — calendars, photo books, and wall pictures — customers use a page-by-page visual design tool rather than just choosing options from a form.

### 6.1 Product Types and Sizes

| Product | Number of Pages | Available Sizes |
|---|---|---|
| Monthly Calendar | 12 | Wall (standard or large), Desk (standard or small) |
| Weekly Calendar | 52 | Wall (standard or large), Desk (standard or small) |
| Bi-weekly Calendar | 26 | Wall (standard or large), Desk (standard or small) |
| Photo Book | 12 | Square, Landscape, Portrait |
| Wall Picture | 1 | Small, Large, Landscape |

Only the sizes that make sense for a given product type are offered.

### 6.2 What Can Be Placed on a Page

Customers can add and freely arrange:
- **Photos** — upload, replace, or remove an image; an empty photo slot appears as a placeholder the customer can click to fill.
- **Text** — with bold, italic, and left/center/right alignment.
- **Shapes** — rectangles and lines, with configurable outline and fill colors.
- **Decorative clipart.**

Every element on a page can be selected, moved, resized, rotated, layered in front of or behind other elements, duplicated, or deleted.

### 6.3 Page Features

Each page can have its own background — a solid color or an uploaded image. Calendar pages include fixed template text (such as the month name and day labels) that is kept separate from the customer's own elements, so it can't accidentally be deleted while editing. A scrollable strip at the bottom of the screen lets customers jump between pages, which matters for products with up to 52 pages.

If a customer leaves in the middle of designing, their progress is saved and they are offered the chance to resume where they left off next time.

---

## 7. Shopping Basket and Checkout

Configured products (whether built through the step-by-step form or the visual editor) are added to a shopping basket with a chosen quantity. The basket shows each item's price exactly as calculated at the time it was added, so the total shown to the customer doesn't shift unexpectedly if prices change elsewhere. Customers can update quantities, remove items, or clear the basket entirely, with the total recalculating live.

### 7.1 Checkout Flow

Checkout is a five-step process: **sign-in/identification**, **contact details**, **delivery**, **payment**, and a final **summary** for review before the order is placed.

- **Sign-in** — see Section 9.4 for how business customers can identify themselves to unlock their own pricing.
- **Contact details** — name, email, phone, and (for business customers) company name, business ID, and VAT ID, plus an invoicing address and an optional separate delivery address.
- **Delivery** — pickup from a named shop location, or courier delivery at Standard, Express, or Economy service level.
- **Payment** — bank transfer with a scannable payment QR code (the default, and the only option for guests), or "invoice on account" for approved business customers. A card-payment option is visible in the interface but is not yet functional ("coming soon").
- **Summary** — a full itemized review of the order (items, delivery cost, any discount code applied, grand total), an optional order note, and the final place-order action.

### 7.2 Discount Codes at Checkout

A customer can enter a promotional discount code on the order summary screen. In the current system, the codes that actually apply at checkout are a fixed set of simple percentage-off codes (5% to 50% off, plus a few named seasonal/welcome codes) with no expiry date, no usage limit, and no minimum-order requirement — any of them can be typed in at any time. See Section 15 for an important caveat: a separate, more sophisticated discount-code management screen exists for staff (Section 9.3) but is not currently connected to what checkout actually honors.

### 7.3 Order by Email

As an alternative to placing an order directly, a customer can request a quote by email instead: a pop-up form pre-filled with a full plain-text summary of their current configuration (category, preset, printing method, all chosen specifications, every component's material/ink/finishes, and the calculated price) opens in the customer's own email application, ready to send to the shop — the customer only needs to add the shop's email address, review the message, and hit send. If the configuration currently has unresolved validation problems, the email explicitly notes that "the online configurator reported issues with this selection," so the shop knows to double-check before quoting.

---

## 8. Rush Orders and Production-Speed Pricing

Beyond the standard price, a customer can choose how fast they want production to run, and the system prices — and sometimes restricts — that choice based on real shop capacity.

### 8.1 Speed Tiers

Three tiers are available: **Express** (rush, priced at a premium), **Standard** (the default), and **Economy** (slower, priced at a discount). In the sample pricing, Express costs 35% more than standard, Economy costs 15% less — these percentages are configurable, not fixed rules of the system.

### 8.2 Dynamic Pricing Based on Shop Business

Express and Standard speed can each carry an **extra** surcharge on top of their base price, driven by how busy the shop's production floor currently is:

- **Queue-based surcharge** — the busier the shop's single most-saturated production station is (its combined queue depth and in-progress workload relative to how many machines it has), the higher the extra surcharge, in escalating steps (e.g. an Express order gets +10% once the busiest station is about half-full, +15% once it's more like 70% full, and +25% once it's 85%+ full — all applicable steps stack). Standard speed has its own, milder version of the same escalation.
- **Seasonal/calendar surcharge** — independent of current queue load, an extra surcharge can also apply on specific days of the week, specific months (e.g. a pre-Christmas surcharge in November–December, a back-to-school-season surcharge in September), or after a certain time of day.
- **A price ceiling always applies** — no matter how many of the above stack up, the combined rush price can never exceed a configured multiple of the base price (2× in the sample data), so a customer is never charged an unbounded amount just because the shop happens to be extremely busy.
- Economy speed is unaffected by any of this — it is always a flat, predictable discount.

### 8.3 When Rush Isn't Available

- **Automatic cutoff when the shop is saturated** — once the busiest station's load crosses a critical threshold (95% in the sample data), Express is automatically taken off the table for new orders, rather than being sold at an ever-escalating price.
- **Per-category quantity caps** — Express has its own maximum order quantity per category (e.g. up to 2,000 units for business cards/flyers/brochures/etc., but only up to 500 units for bulkier items like banners, roll-ups, and packaging) — ordering more than that cap simply isn't offered at rush speed.
- **Certain finishing choices always block rush** — Perfect Binding and Case Binding both require glue-curing time that is fundamentally incompatible with a rush turnaround, so Express is never offered for a product using either of those binding methods, regardless of quantity or category.
- **Materials can be excluded per tier** — a shop can also flag specific materials as unavailable for a given speed tier.

### 8.4 Estimating When an Order Will Be Ready

Customers and staff see an estimated completion window (an earliest and latest date/time, shown as a friendly relative description like "Tomorrow, 9:00" or a weekday name), built from real shop conditions rather than a flat promise:

- the actual sequence of production stations the order must pass through, each with its own realistic setup time and per-item processing time,
- how long the order will likely sit in queue at each of those stations, which depends on the chosen speed tier — Express jumps to the front of the queue, Standard waits behind roughly half the existing queue, Economy waits behind the whole existing queue,
- an approval-queue delay before production even starts, again tier-dependent (roughly next business day for Express, about two business days for Standard, about four business days for Economy, in the sample calibration),
- a same-day order cutoff time — orders placed after the cutoff (or outside working hours, or on a non-working day) are treated as starting the next working day,
- the shop's actual working hours, working days, and any configured holiday closures — all of the "how long will this take" math correctly skips nights, weekends, and holidays.

### 8.5 Estimated Shipment Weight

While configuring a product, and again at the order summary, customers see an estimated shipment weight (grams or kilograms). This is calculated from each component's material weight and the sheet area used, scaled up by the order quantity — with a special adjustment for saddle-stitched booklets/calendars, where the cover and body sheets are folded double. This figure is informational only: it is not currently used to calculate the delivery/shipping cost shown at checkout (that comes from a flat courier-service surcharge instead), and it is not tied into packaging logic.

---

## 9. Customer Accounts

### 9.1 Customer Types and Status

Every customer account has a **type** — Guest, Registered, Registered Corporate, or Agency — and a **status** — Active, Inactive, Suspended, or Pending Approval. Only Active accounts can sign in; Inactive and Suspended accounts are explicitly refused at login, and a Pending Approval account is treated the same as "no such account" at login (so a not-yet-approved applicant isn't tipped off that their application even exists yet, until it's approved). Only Registered Corporate customers are offered "invoice on account" as a payment method at checkout — everyone else pays by bank transfer.

A customer record also carries a business classification tier (Standard, Silver, Gold, Platinum) — see Section 15 for a note that this tier is currently a label only and does not yet automatically adjust pricing.

### 9.2 Business Customer Sign-In

Business ("Agency") customers can sign in using their Business ID, VAT ID, or email address, via a one-time code sent to them (valid for 5 minutes) that starts a 24-hour session. Signing in during checkout skips the identification step entirely, pre-fills the customer's name and address, and automatically applies that customer's negotiated pricing (Section 5.14) to whatever is in their basket.

### 9.3 Customer & Discount Code Administration

Staff have a dedicated management area to maintain customer records (contact info, company details, address, pricing overlay, and free-text internal notes with a timestamp and author — a lightweight CRM function), and a discount-code management screen supporting rich rules: percentage discount, fixed-amount discount, or free delivery; a valid-from/valid-until date range; a maximum number of uses; a minimum order value; and restriction to specific categories, specific customer types, or specific named customers. See Section 15 for an important caveat about this screen.

### 9.4 Internal Notes

Staff can attach free-text notes to a customer record, each stamped with the date and (optionally) which staff member wrote it — useful for recording things like special handling instructions or account history that shouldn't live in the customer's own visible order history.

---

## 10. Contextual Help

Next to configurator fields, customers can get two kinds of on-demand explanation:

- A **general help button ("?")** — always available on Category, Material, Printing Method, Ink Configuration, Fold Type, Binding Method, and Finishes fields — explains what that kind of choice means in general terms.
- A **specific info button ("i")** — appears only once the customer has actually selected a specific material, finish, or other catalog option that has its own description written for it, and shows that option's own bilingual description (e.g. "why you'd choose this particular paper"). It simply doesn't appear if the selected option has no description written for it yet.

On a touch device the explanation opens on tap; on a mouse/trackpad device it opens on hover; clicking anywhere else, or the trigger again, closes it.

---

## 11. Production / Manufacturing Management

Once an order is placed, it enters production tracking — a complete system for running the physical print-shop workflow from approval through to dispatch.

### 11.1 Production Stations

Production is organized around the physical stations a shop has: prepress (file preparation), digital printer, offset press, large-format printer, letterpress, cutter, laminator, UV coater, embossing/foil stamping, folder, binder, large-format finishing, quality control, and packaging — 14 station types in total.

### 11.2 Automatic Workflow Generation

When an order is approved, the system automatically works out the exact sequence of production steps it needs to go through, based on what was ordered — which printing method was used (routing to the matching press/printer station), which finishes were selected (routing to the matching finishing stations), and, for multi-part products, how the individual parts come back together for shared binding, quality control, and packaging steps at the end. Steps that depend on earlier steps are only made available to start once their prerequisites are complete.

### 11.3 Order Approval

Before entering production, an order goes through an approval step where staff can:
- review uploaded artwork files against three independent checks — **resolution**, **bleed**, and **color profile** — each individually markable as Not Checked, Passed, Warning, or Failed, with free-text notes; a shorthand marks the whole file as "fully passed" only once all three checks pass, "has issues" if any check failed, or "has warnings" if none failed but at least one is flagged,
- confirm payment status (Pending, Confirmed, or Failed),
- set or adjust the order's priority (which later affects queue ordering — see 11.4),
- move the order to one of five approval states: Placed, Approved, Rejected, Pending Changes (sent back to the customer), or On Hold.

### 11.4 Working the Production Queue

Staff working at a given station see a queue of the jobs waiting for them, which they can filter and sort by station, status, and priority, and can mark as started or completed. Each job in the queue can be opened to see its full order details and overall progress through the workflow. Staff can always freely re-sort the queue themselves — the priority score described below is advisory guidance, not an enforced order.

To help staff decide what to work on next, each waiting job is given an advisory priority score, made up of:
- **how close the deadline is** — jobs that are overdue or due very soon score highest,
- **the order's priority level** — rush orders score a bonus, low-priority orders score a penalty, and an order running on Express production speed gets an extra large bonus so it reliably outranks Standard/Economy work regardless of deadline,
- **how far along the whole order already is** — jobs that are part of a nearly-finished order score slightly higher, to help clear things through and out the door,
- **whether it matches what the station is already set up for** — e.g. a job using the same material the machine is currently loaded with scores a bonus, to reduce changeover time; Economy-speed jobs get an extra flat bonus here too, since batching slower, non-urgent work together is efficient,
- as a tie-breaker between equally-scored jobs, **the older job goes first**.

If a step can't be completed as planned, staff can mark it as failed (putting the whole order on hold with a recorded reason), or — for steps where this makes sense — skip it. Some critical steps (prepress, quality control, packaging) can never be skipped. A step can also be reset and reworked if needed, which automatically reverts any downstream steps that had already started back to a waiting state.

### 11.5 Staff and Equipment Management

Staff members can be registered with the station types they're capable of working, activated or deactivated, and assigned to jobs; a "my jobs in progress" view lets each staff member see what they currently have underway. Machines can be registered by station type and marked online, offline, or in maintenance.

### 11.6 Fulfilment and Dispatch

Once every production step for an order is finished, a dispatch checklist is automatically created, requiring these steps in order:
1. **Collect items** — confirm each item in the order has been gathered, with a record of which staff member verified each one.
2. **Quality sign-off** — a final pass/fail check, with the signing staff member and notes recorded.
3. **Packaging** — choose the packaging type (box, envelope, roll, tube, or custom) and record its dimensions and weight.
4. **Dispatch** — confirm shipment with a tracking number. This step is only available once the previous three are complete.

### 11.7 Tracking Order Progress

Staff and managers can see every order's progress through production, with visual progress bars, coloring that highlights orders getting close to or past their deadline, and a full breakdown of each item's production status.

### 11.8 Analytics and Reporting

Management has access to reporting on:
- average time spent at each production station,
- which station is currently the bottleneck slowing everything else down,
- how much work each staff member is completing,
- what percentage of orders are being delivered on time.

A dashboard summarizes the shop's current state at a glance: how many orders are awaiting approval, currently in production, ready to dispatch, overdue, or completed today; a live view of how busy each station is; and an alert banner if any station's queue backs up beyond a healthy level.

### 11.9 Shop Configuration (Prototype)

A settings screen exists for configuring working hours, order cutoff times, rush-pricing multipliers and caps, the queue-saturation threshold that disables Express, busy-period rules, per-station time estimates, and a holiday calendar. See Section 15 — this screen is currently a prototype: it does not save changes, and none of its fields actually feed into the real completion-time or pricing calculations described in Sections 5 and 8, which instead run on their own built-in sample configuration.

---

## 12. Production Cost & Margin Analysis (Internal)

Separately from the price a customer is charged, the shop can maintain an internal, simplified production-cost model for each material, printing process, and finish — deliberately kept simpler than the customer price list (no volume tiers, no setup fees, no minimum-order floor), plus a single overhead percentage applied on top of raw costs. This lets management compare a given order's actual selling price against its estimated production cost to see the margin (both in money and as a percentage), and flags two warning conditions: the order is being sold at an outright loss (below production cost), or the margin percentage has dropped below a configurable healthy threshold (15% by default). This is a backend calculation capability — see Section 15 for the caveat that it does not yet have a dedicated staff-facing report screen.

---

## 13. Multi-Language Support

The entire customer- and staff-facing experience — product names, form labels, validation messages, error explanations, contextual help text, and all screens described above — is available in English and Czech, with the interface automatically detecting the visitor's browser language and remembering their preference for future visits, plus a manual language switcher.

---

## 14. Catalog Administration

Non-technical staff can maintain the entire catalog and rule set themselves, without needing a software change:
- add, edit, and remove product categories, materials, finishes, and printing methods,
- edit the compatibility rules described in Section 4,
- edit price lists, surcharges, discount tiers, and setup fees described in Section 5,
- export the entire catalog for backup, and import it back (or into another environment), as a single file.

---

## 15. Known Gaps and Partially-Implemented Features

In the interest of giving an accurate picture rather than an aspirational one, the following capabilities exist in some form today but are not fully wired end-to-end. Anyone scoping future work from this specification should treat these as "needs finishing," not "already delivered":

- **Two separate discount-code systems that don't talk to each other.** The staff-facing discount-code management screen (Section 9.3) supports rich rules — expiry dates, usage limits, minimum order values, category/customer restrictions, percentage/fixed/free-delivery types — but the actual checkout flow (Section 7.2) only ever checks a small fixed list of hardcoded percentage codes with none of those rules applied. Codes created or edited in the management screen currently have **no effect** on real customer orders.
- **Customer Tier is a label, not a pricing mechanism.** The Standard/Silver/Gold/Platinum classification on a customer record is intended to "drive default discount levels" but nothing in the system currently reads it to actually adjust a price — any tier-based pricing today has to be set up manually via the customer-specific pricing overlay (Section 5.14).
- **Card payment is not functional.** It appears as a checkout option but is marked "coming soon" and cannot currently be used to complete a purchase; only bank transfer and (for approved corporate accounts) invoice-on-account actually work.
- **Some rush-order restriction fields are defined but not enforced.** The per-category, per-speed-tier restriction data model includes fields for capping the number of components or finishes, and for restricting which finish types are allowed at a given speed — but the actual rush-eligibility check today only enforces the quantity cap, the blocked-materials list, and the fixed Perfect/Case Binding rule (Section 8.3). The extra fields are effectively placeholders for now.
- **The shop settings screen (Section 11.9) doesn't persist and isn't connected.** Changes made there are not saved, and none of the values shown (working hours, cutoff times, rush multipliers, the Express-disable threshold, etc.) actually affect the real pricing or completion-estimate calculations, which run on their own built-in configuration instead.
- **Production cost / margin analysis has no dedicated report screen yet** (Section 12) — the calculation exists and is usable, but there isn't yet a staff-facing view built specifically to surface it.
- **Estimated shipment weight is display-only** (Section 8.5) — it is shown to the customer but does not currently affect the delivery cost calculation or any packaging logic.

---

## 16. Summary of Capabilities

| Area | Capability |
|---|---|
| Catalog | 15 categories (11 core print + Free Configuration + 4 promotional-merchandise), 30 materials, 28 finishes, 7 printing methods, per-category preset "quick order" configurations, multi-currency price lists |
| Showcase | Marketing gallery of 14 showcased products across 5 groups, with variations linking directly into the configurator |
| Compatibility | 17 kinds of business rule governing valid combinations, all customer-editable via the catalog admin tools, plus a separate rush-speed eligibility layer |
| Pricing | Full itemized pricing engine covering flat, area-based, tiered-area, and sheet-based material pricing; ink costs; per-finish surcharges (flat, crease-count, spacing-tiered, or per-length); process/category/fold/binding surcharges; volume discounts; rush/economy speed pricing with queue- and calendar-driven dynamic surcharges and a price ceiling; one-time setup fees; a minimum order floor; and customer-specific pricing overlays |
| Visual design | Page-by-page editor for calendars (12/26/52 pages), photo books, and wall pictures, with photos, text, shapes, and clipart, and resumable in-progress sessions |
| Basket & checkout | Full basket management, a five-step checkout (identification, contact, delivery, payment, summary), discount codes, and an email-quote alternative to self-checkout |
| Rush orders | Three speed tiers with dynamic queue- and calendar-based pricing, an automatic Express cutoff when the shop is saturated, per-category quantity caps, binding-method exclusions, and realistic completion-date estimation accounting for shop hours/holidays |
| Customers | Four customer types and four account statuses, OTP-based business sign-in, per-account negotiated pricing overlays, and a lightweight CRM with internal notes |
| Help | Contextual general and option-specific help buttons throughout the configurator |
| Production | 14 production station types, automatic workflow generation, priority-scored (advisory) work queues, staff/equipment management, artwork/payment approval gating, 4-step fulfilment/dispatch, and shop-wide analytics |
| Cost analysis | Internal production-cost modeling and margin/below-cost warnings, separate from customer pricing |
| Language | Full English/Czech localization throughout |
| Administration | Non-technical catalog, rule, and pricing management with export/import |
| Known gaps | See Section 15 for discount-code duality, unused customer-tier field, non-functional card payment, partially-enforced rush restrictions, a non-persistent shop settings screen, and other loose ends worth closing before relying on this as a finished system |
