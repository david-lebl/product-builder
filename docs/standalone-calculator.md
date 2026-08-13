# Standalone Price Calculator (Embeddable Widget)

> A second, much smaller application built on the same product configurator as the full SPA.
> A print shop drops it into its own website; a visitor configures a product, sees a fully
> itemized price, collects items in a basket, and sends the order by e-mail.

## 1. Purpose and scope

The full application is an internal-and-customer system: product catalog, visual editor,
shop-floor manufacturing views, customer management, and a five-step checkout. The calculator
is the outward-facing slice of that — *configure and price a product* — packaged so it can live
on any website.

**Included:** category and preset selection, all product specifications, printing method,
per-component material / ink / finishes, manufacturing speed tiers, the full price and weight
breakdown, validation messages, the basket, and ordering by e-mail.

**Excluded:** manufacturing and shop-floor views, the product catalog / showcase pages, the
visual editor, customer accounts and login, customer-specific pricing, discount codes, and the
checkout wizard (delivery options, payment methods, invoice addresses).

## 2. Production dates are not promised

This is the single most important behavioural difference from the SPA.

The SPA estimates a concrete completion timestamp for each speed tier — "Tomorrow, 11:00",
"Monday, 14:00" — by running `CompletionEstimator` over the shop's station queues, working
hours and same-day cutoffs. That is only honest when the estimate is backed by real
production-queue data.

The widget runs on a customer's website with no connection to the shop's production system.
It therefore shows **indicative ranges** and says so:

| Tier | Price | Lead time shown |
|---|---|---|
| Express | from the pricelist (e.g. +35%) | Same day / next business day |
| Standard | base price | 2–5 business days |
| Economy | from the pricelist (e.g. −15%) | 5–10 business days |

> *Lead times are indicative only — the final production date is confirmed by e-mail once we
> accept your order.*

Two related gates also change:

- **No queue surge pricing.** The SPA raises the Express multiplier as the busiest station
  fills up (+10% at 50% load, +15% at 70%, +25% at 85%) and adds busy-period multipliers. The
  widget prices with base multipliers only, since it has no utilisation figure to react to.
- **No shop-load cutoff.** The SPA withdraws Express once the busiest station passes 95%.
  The widget always offers Express.

**Unchanged:** per-category Express quantity caps and the Perfect Binding / Case Binding blocks
still apply. Those are product constraints — glue needs time to cure regardless of how busy the
shop is — not queue state.

The speed-tier price labels are read from the active pricelist's `ManufacturingSpeedSurcharge`
rules rather than hardcoded, so a shop that configures different multipliers sees its own numbers.

## 3. Ordering by e-mail

The basket works exactly as in the SPA — add configured products, change quantities, remove
items, see a running total. Only its primary action differs: instead of *Proceed to Checkout*
it offers **Send order by e-mail**.

That opens a modal with:

- **Name**, **e-mail**, **phone** (phone optional)
- an editable **message** pre-filled with every basket item — category, quantity, line total,
  printing method, specifications, and per-component material / ink notation / finishes — plus
  the basket grand total
- a note stating the message is a price request, not a binding order

Sending opens the visitor's own mail client through a `mailto:` link, addressed to the shop
address the host page configured. The message closes by asking the shop to confirm the price
and production date and to say where artwork should be sent.

There is no artwork step. The widget has no visual editor, and a `mailto:` link cannot carry a
file, so artwork is arranged in the shop's reply rather than pretended at in the form.

The single-configuration **✉ Order via Email** button in the Validation Status panel is retained
in both apps, for asking about one configuration without using the basket.

**Known limitation:** some mail clients truncate long `mailto:` URLs (Outlook at roughly 2000
characters), so a large multi-item basket may arrive clipped. A POST endpoint is the planned
upgrade.

## 4. Embedding

Two integration modes, from one JavaScript artifact.

### A. Mount into your own element

```html
<link rel="stylesheet" href="https://cdn.example.com/calc/calculator.css">
<div id="mp-calculator"></div>
<script src="https://cdn.example.com/calc/main.js"></script>
<script>
  MPCalculator.mount('#mp-calculator', {
    lang: 'cs',
    orderEmail: 'orders@shop.cz'
  });
</script>
```

| Config key | Default | Meaning |
|---|---|---|
| `lang` | stored preference, then browser language, then `en` | `'en'` or `'cs'` |
| `orderEmail` | `''` | Recipient of the order e-mail. Blank leaves the To field empty and the customer fills it in. |

Both keys are optional; `mount` logs an error and does nothing if the selector matches nothing.

This mode shares the host page's CSS cascade. The widget ships a reset scoped to its
`.mp-calculator` root rather than the SPA's global `* { }` and `body { }` rules, so it will not
restyle the surrounding page — but its own class names (`.card`, `.form-section`,
`.price-section`) are generic and may collide with a host site's styles.

### B. Iframe

```html
<iframe src="https://calc.example.com/" style="width:100%;height:1400px;border:0"></iframe>
```

The bundled `index.html` mounts the widget itself. This is the recommended mode when style
isolation matters. Set `orderEmail` in that file before deploying.

`embed-example.html` ships alongside as a working reference for mode A, rendered inside a
deliberately different-looking host page.

## 5. Module layout

```
modules/
  domain/             pure core, cross-compiled (unchanged)
  ui-framework/       Laminar components, no domain dependency (unchanged)
  ui-productbuilder/  the shared configurator ← consumed by both apps
  ui-calculator/      standalone widget  (deps: domain.js, ui-framework, ui-productbuilder)
  ui/                 full SPA           (deps: domain.js, ui-framework, ui-productbuilder)
  ui-showcase/        UI kit demo (unchanged)
```

`ui-productbuilder` holds the package `mpbuilder.ui.productbuilder` — the same package name it
had inside `ui`, so consumers were unaffected by the move.

### The host seam

`ui-productbuilder` must not reference the SPA's routing, visual editor or shop-floor
simulation, so those are supplied by the host through `BuilderEnvironment`, installed once
before the first render:

| Field | Full SPA | Calculator |
|---|---|---|
| `pricingContext` | queue surge + busy periods | `PricingContext.default` |
| `completionText` | concrete date per tier | `None` → indicative range |
| `expressAvailable` | shop-load gate | always `true` |
| `artwork` | visual editor integration | `None` → section hidden |
| `basketOpen` | owned by `AppRouter` | owned by the widget |
| `basketPrimaryAction` | Proceed to Checkout | Send order by e-mail |
| `orderEmail` | `""` | host-configured |

Returning `None` from `completionText` is what makes lead times indicative — the tier card falls
back to its static range and the disclaimer appears.

## 6. Building

```bash
mill ui-calculator.fastLinkJS     # dev  → out/ui-calculator/fastLinkJS.dest/main.js
mill ui-calculator.fullLinkJS     # prod → out/ui-calculator/fullLinkJS.dest/main.js  (~1.6 MB)

./modules/ui-calculator/build-css.sh   # regenerate calculator.css
```

Deploy `main.js` together with `calculator.css`, and `index.html` if using the iframe mode.

`calculator.css` is generated, not hand-edited. It concatenates the widget's scoped reset, the
six SPA stylesheets the configurator actually uses (`tokens, layout, utilities, uikit,
pricing, basket`) and the widget's shell styles. The SPA's `reset.css` is deliberately excluded —
its `*` and `body` rules would restyle the host page. Rerun `build-css.sh` after editing any of
the source stylesheets; CI fails the build if the committed file has drifted.

## 7. Deployment & previews

Both `deploy-pages.yml` (on `main`) and `deploy-preview.yml` (per PR) build the widget alongside
the SPA and publish it under a `calculator/` subdirectory — the two apps cannot share a directory
because both emit `main.js`.

```
_site/
├── index.html, *.css, main.js        the full SPA
└── calculator/
    ├── index.html                    the widget, standalone (iframe this)
    ├── embed-example.html            mount-div integration in a mock host site
    ├── calculator.css
    └── main.js
```

So on any PR preview:

| URL | Shows |
|---|---|
| `<preview-url>/` | the full SPA |
| `<preview-url>/calculator/` | the widget on its own |
| `<preview-url>/calculator/embed-example.html` | the widget embedded in someone else's page |

The same paths apply to the GitHub Pages deployment of `main`.

## 8. Related documents

- [manufacturing-speed-pipeline.md](manufacturing-speed-pipeline.md) — how the SPA derives the
  concrete dates this widget deliberately omits
- [analysis/express-manufacturing-analysis.md](analysis/express-manufacturing-analysis.md) —
  the full Express design, including surge pricing and the availability cutoff
- [email-order-option.md](email-order-option.md) — the original single-configuration e-mail order
- [pricing.md](pricing.md) — pricing rules and the calculation order
