# Order-Line Product Configuration

Every cart line and order line stores the complete product configuration the customer priced, so an order is
fully specified for prepress and production without going back to the catalog.

## What a line stores (`ProductSnapshot`)

| Part | Content |
|------|---------|
| Category | id, localized name, catalog version |
| Components (`ComponentSnapshot`) | material (id + name), printing method, ink notation (e.g. `4/0`, `4+W/0`), finished & flat size, sheets used |
| Finishes (`FinishSnapshot`, per component) | finish id + name, finish type key, **applied side** (front / back / both), **settings** (corner count & radius, foil color, grommet spacing, perforation pitch, length, crease count) |
| Physical spec (`ProductSpecSnapshot`) | dimensions (flat, folded + fold type, box, circular, print area), **orientation** (if any), **bleed** (always set — see below), **binding** (if any) (method, total pages, self-cover), **delivery format** (cut singles, kiss-cut sheets, roll labels with core & unwind direction, packed units) |

Quantity, artwork versions, speed and copies stay on the line itself.

The snapshot is frozen when the product is added to the cart (or re-priced for a quick order) and copied
unchanged onto the order line at checkout. Later catalog changes never change a placed order.

`ordering` defines these types itself (no dependency on `catalog`); the storefront ACL
(`StorefrontTranslators.toProductSnapshot`) translates the catalog's `ConfiguredProduct`.

## Uses

- **Backoffice order detail** — each line lists the physical spec (Format, Orientation, Bleed, Binding,
  Delivery) and each component's finishes with side and settings, e.g. "Matte Lamination (Both sides)",
  "Rounded Corners (4 corners, r 3 mm)".
- **Cart drawer and My orders** — a one-line summary (orientation, bleed, binding, non-default delivery
  format, finishes).
- **Artwork preflight for order lines** (customer and staff uploads, re-uploads, re-run preflight):
  - required bleed = the line's bleed;
  - expected pages = for a bound booklet's body: total pages minus 4 for a separate cover (all pages when
    self-cover); otherwise 1 page per printed side.

## Bleed

The bleed is resolved when the line is frozen: the customer's choice, otherwise 5 mm for large-format inkjet
and 3 mm for everything else. So every line states its bleed explicitly.
