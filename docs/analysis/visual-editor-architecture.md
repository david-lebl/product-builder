# Visual Editor (Design Studio) — Architecture & Integration Analysis

> Status: implemented 2026-11-25 (editor, document model, local persistence, storefront hand-off).
> Next phases: server sync, asset upload, print-PDF pipeline (see §9).

The Design Studio lets customers design business cards, flyers, posters, postcards, wall/desk calendars and photo
books in the browser. It replaces the pre-Kyo Laminar PoC (`modules/ui/.../visualeditor`, removed in `1e4383e`),
which stored pixel coordinates, inlined photos as data URLs in IndexedDB sessions, hard-coded calendar day labels
as locked text fields and had no link to print geometry. This document describes the redesign and — most
importantly — the decisions that keep the data model ready for **server sync** and **print PDF generation**.

---

## 1. Module layout

| Module | Platform | Contents |
|---|---|---|
| `backend/design/01-c-core` | JVM + JS | Bounded context **design**: document model, pure editing ops, geometry, templates, calendar math, clipart, font catalog, preflight, **SVG renderer**, `DesignService` (+ `private[design] DesignRepository`) |
| `backend/json` → `DesignCodecs` | JVM + JS | kyo-schema codecs for the document & project envelope (IndexedDB, HTTP, JSONB) |
| `backend/sample` → `InMemoryDesignRepository` | JVM + JS | In-memory adapter + `DesignSampleWiring` |
| `modules/ui-editor-core` | JVM + JS | Kyo UI editor: `EditorController` (state, history, gestures, autosave), views, EN/CS bundles, `EditorTheme.css`. `src/main/scala-js` = browser glue (`BrowserEditor`: pointer/keyboard/drop bridge; `IndexedDbStores`) |
| `modules/ui-editor` | Scala.js app | The Design Studio page (`_site/editor/`), standalone **and** embedded by the storefront in an iframe (`EmbedProtocol`) |

`backend/shared` gained the cross-context id `DesignId`.

### 1.1 Why an iframe and not a lazily loaded chunk (measured)

The requirement was: the editor must not grow the storefront, calculator or backoffice bundles.

| Option | Storefront initial JS (gzip) | Notes |
|---|---|---|
| Baseline (before) — `ui` NoModule + Closure | **3.47 MB** (22.0 MB raw) | |
| ES modules + `js.dynamicImport` chunk | **5.98 MB** (99 MB raw) + 0.39 MB editor chunk | Scala.js cannot run Closure on multi-module ES output → everything un-minified. Rejected. |
| Second Scala.js bundle mounted in the same page | — | Kyo UI addresses DOM nodes by document-wide `data-kyo-path` ids; two runtimes (or even two `runMount` roots of one runtime) produce identical paths and patch each other's nodes. Rejected. |
| **Editor page in a full-screen `<iframe>`** (chosen) | **3.48 MB** (22.06 MB raw; +5 KB gzip for the CTA, glue & strings) | Separate document ⇒ separate Kyo runtime, no id collisions; editor bundle 14.8 MB raw / **2.5 MB gzip**, fetched only on "Design online" and cached; same origin ⇒ shared IndexedDB |

The calculator and backoffice bundles do not reference the editor at all.

### 1.2 Embed contract (`modules/ui-editor/.../EmbedProtocol.scala`)

* Host → editor: query string of `editor/index.html` — `embed=1&v=1&lang&theme&category&component&title&w&h&pages&design&customer`.
* Editor → host: `postMessage` (same origin only) `mpbuilder.editor.close` and
  `mpbuilder.editor.complete { design: { id, name, proofDataUrl, minimumDpi, bleedMm, pageCount, trimWidthMm, trimHeightMm, issues } }`.
* The storefront (`modules/ui/Main.openDesignStudio`) attaches the result via
  `ConfiguratorController.attachDesignArtwork` → the existing draft-artwork upload, so the cart, order intake and
  staff artwork review work unchanged. The configurator shows "Design online" / "Edit design" per printable component.

---

### 1.3 Local preview

```bash
./scripts/dev.sh frontend              # storefront at /, Design Studio at /editor/ (plus admin, calculator)
./scripts/dev.sh frontend --app=editor # Design Studio only
```

`--app=ui` builds the editor too, because the storefront's "Design online" opens `editor/index.html` in an iframe.

---

## 2. Document model (`mpbuilder.design.DesignModel`)

```
DesignDocument(formatVersion, product: ProductBinding, format: DesignFormat, pagePolicy: PagePolicy,
               pages: Vector[DesignPage], assets: Vector[ImageAsset])
DesignPage(id, role: PageRole, background: Fill, elements: Vector[DesignElement])   // order = z-order
DesignElement(id, frame: Placement, content: ElementContent, opacity, locked, name)
ElementContent = Text(text, TextStyle) | Image(asset?, ImageCrop, cornerRadius, stroke, filter)
               | Shape(kind, Fill, stroke, cornerRadius) | Clipart(kind, colour) | Calendar(CalendarSpec)
```

**Units are physical, never pixels** — the single most important fix over the PoC:

* geometry in **millimetres**, origin at the **trim box** top-left; the bleed area is `-bleed … trim+bleed`;
* font sizes in **points**; rotation in **degrees clockwise** around the element centre;
* the same numbers drive the canvas (any zoom), thumbnails, proofs and the PDF.

`DesignFormat(trimWidthMm, trimHeightMm, bleedMm, safeMarginMm, binding: Option[BindingGuide])` comes from the
configured product (trim size from the storefront configuration, bleed/safe/binding from the product-kind preset).
`PagePolicy(min, max, step)` encodes product rules (business card 1–2, calendar fixed 13, photo book 12–60 in
steps of 2). `ProductBinding(kind, categoryId, componentKey, label)` links a design back to the configuration.

Design choices worth keeping:

* **Smart elements instead of baked content.** A month grid is `Calendar(CalendarSpec(year, month, language,
  weekStart, holidays, …))`, rendered from parameters (ISO weeks, Czech public holidays incl. Easter). Changing the
  starting month / language re-targets every page (`DesignOps.setCalendarStart`, `updateAllCalendars`). The PoC
  stored 28–31 locked text fields per page.
* **Images are references.** `ImageAsset(id, fileName, mime, pixelWidth, pixelHeight, sizeBytes, sha256, storage)`
  with `storage = DeviceOnly | Uploaded(storageKey) | Builtin(url)`. Ids are content-addressed (`img-<sha256…>`), so
  re-adding a photo is free locally and the server can de-duplicate uploads. The document never contains bytes.
* **Crop is resolution-independent**: `ImageCrop(fit = Cover|Contain, zoom ≥ 1, focusX, focusY)`. Effective DPI is
  computed from pixel size and placed size (`DesignGeometry.effectiveDpi`) — used by preflight and the inspector.
* **Fonts are a curated, embeddable catalog** (`FontCatalog`, OFL Google Fonts with `latin-ext` for Czech). The PDF
  renderer must embed exactly the fonts the browser showed; system fonts are never allowed.
* **Colours are sRGB `#rrggbb`** (`DesignColor`). CMYK conversion belongs to the PDF pipeline (press ICC profile);
  the review dialog warns customers about RGB→CMYK shift.
* **Ids are document-local strings** (`ElementId`, `PageId`) generated deterministically (`DesignIds`) — templates
  and tests are reproducible, and future collaborative/merge features can address elements stably.

### 2.1 Evolution / versioning

`formatVersion` (currently 1) is stored in every document. Rules (enforced by `DesignCodecsSpec`):

1. Additive change → new field **with a default**; old JSON keeps decoding (verified: missing defaulted fields decode).
2. Breaking change → bump `CurrentFormatVersion` and add a JSON-level migration step in `DesignCodecs.decodeDocument`
   before schema decoding (documents live for months in browsers and must stay readable).
3. `DesignValidation.validate` runs after every decode and before every server save (bounds: 200 pages,
   300 elements/page, fonts from the catalog, asset refs resolvable, finite geometry…).

---

## 3. Rendering — one renderer for screen, proof and print

`DesignSvg.renderPage(doc, page, RenderOptions)` is pure Scala (JVM + JS) producing SVG:

* user space = **10 units per mm** (browsers clamp tiny font sizes; text is never laid out below ~6 px);
* viewBox = bleed box (editor/print) or trim box (thumbnails/proofs);
* wrapped text = XHTML inside `<foreignObject>` (exact CSS from `TextStyle`);
* images = `<image>` in a clip path with the computed placement; grayscale/sepia via `feColorMatrix`;
* `RenderMode.Editor` adds `data-ve-el` hit-test attributes and placeholders; `Print` omits placeholders.

The editor canvas injects this markup verbatim (`UI.rawHtml`) and draws its interaction layer (guides, snap lines,
selection, handles) as a separate Kyo SVG overlay in the same coordinate system. Thumbnails, the template gallery,
"My designs" cards and the review proof use the same function — **what the customer approves is what prints**.

### 3.1 Print PDF pipeline (next phase)

Recommended: render each page's `RenderMode.Print` SVG (HTML wrapper with `@page { size: (trim+2·bleed) mm }`,
bundled font files, image URLs pointing to the blob store) in **headless Chromium** (`page.pdf`) → vector PDF with
embedded fonts, foreignObject text identical to the browser → **Ghostscript** (or callas pdfToolbox in production)
to convert to **PDF/X-4** with the press output intent (e.g. FOGRA39/FOGRA51), add trim/bleed boxes and crop marks.
Alternatives considered: Apache Batik/PDFBox (no `foreignObject`, would need our own line breaking ⇒ text drift),
Typst (good typography, but a second layout engine ⇒ drift). Chromium keeps a single layout engine.

Inputs the pipeline needs are already in the model: trim/bleed (`DesignFormat`), font keys (`FontCatalog`), asset
storage keys (`AssetStorage.Uploaded`), exact revision (`DesignProject.revision`) and frozen status
(`DesignStatus.Submitted(artworkId)`).

---

## 4. Editor architecture (`modules/ui-editor-core`)

* `EditorController` — all state as `SignalRef`s (document, page, selection, zoom, history, save status …) and every
  action as a pure `DesignOps` transformation committed via `commit(f, coalesce)`; JVM-tested.
* **Undo/redo** stores immutable document snapshots (structural sharing, 100 steps); typing/sliders coalesce by key
  within 1.5 s; one gesture = one step.
* **Gestures**: Kyo UI has no pointer-move events, so `BrowserEditor` (Scala.js) listens to pointer events, converts
  to mm (`data-ve-page` box), calls `pointerDown/Move/Up` **synchronously**, and during a drag applies an SVG
  `transform` directly to the dragged nodes (60 fps without re-rendering); the document commits once on release.
  Geometry (rotation-aware resize, aspect lock, centre resize, 15° rotation snapping, edge/centre snapping to page,
  safe area and other elements) lives in `DesignGeometry` and is unit-tested.
* **Keyboard**: undo/redo, delete, duplicate, copy/paste, nudge (1 / 10 mm), select all, page up/down, zoom.
* **Photos**: picker or drag-and-drop → SHA-256 → measured → original stored in IndexedDB → placed into the slot under
  the pointer / selected slot / first empty slot; panel photos can be dragged onto frames.
* **Preflight live** (`DesignPreflight`): very low / low DPI (150/250), empty photo slots, text outside safe area,
  content not reaching bleed, binding-zone overlap, tiny text, missing images, not-uploaded images.
* **Review dialog** = customer proof: all pages trimmed, checks, RGB note, proof download, "Use this design".
* UI: home (my designs + template gallery per product), tool rail (templates, text, photos, elements, background,
  layouts), canvas with bleed/safe/binding guides, inspector, page strip; EN/CS; light/dark tokens; responsive
  (≤960 px drawers, ≤640 px bottom rail).

---

## 5. Persistence & sync

### 5.1 Local-first (implemented)

Every edit autosaves (900 ms debounce) to **IndexedDB** (`mpbuilder-design-studio`):

* `designs` — `{ id, json (DesignProject), syncedRevision, pendingPush, updatedAt }`
* `assets` — `{ id, blob (original bytes), mime, name, width, height, size, sha256 }`

IndexedDB (not `localStorage`): photos are megabytes, quotas are hundreds of MB+, works offline. Anonymous designs
are owned by a random device id (`localStorage["mpbuilder.deviceId"]`). Fallback when IndexedDB is unavailable:
in-memory for the session.

### 5.2 Server sync for signed-in customers (designed, service implemented)

`DesignService` (implemented + tested, in-memory adapter) already provides the server semantics:

* `DesignProject(id, name, owner: Device|Customer, document, revision, status: Draft|Submitted|Archived, …)`;
* **optimistic concurrency**: `save(SaveDesignCmd(expectedRevision))` fails with `RevisionConflict(expected, actual)`;
* `claimDeviceDesigns(deviceId, customerId)` re-owns anonymous designs after sign-in;
* `markSubmitted(artworkId)` freezes the exact revision used for production (edits continue on a copy; the editor
  already duplicates submitted designs on open); deleting a submitted design only archives it.

Client algorithm (port `DesignSync` in `EditorModels`): local save always first; if signed in, push when
`pendingPush` with `expectedRevision = syncedRevision`; on `RevisionConflict` pull the server copy and keep **both**
(local copy renamed "… (this device)") — never silently drop work; on sign-in pull the list and merge by id/revision.

HTTP API to add (storefront BFF, bearer session): `GET /api/v1/designs` (summaries), `GET/PUT/DELETE /api/v1/designs/{id}`
(PUT carries `expectedRevision`, 409 on conflict), `POST /api/v1/designs/claim`, `POST /api/v1/designs/assets`
(multipart, sha256 header, idempotent). Postgres adapter `design/02-o-db`: `design.designs(id pk, owner_kind,
owner_id, status, revision, updated_at, doc jsonb)` + index on owner; `design.assets(sha256 pk, storage_key, mime,
width, height, size, created_at)` + `design.design_assets(design_id, sha256)` for GC.

### 5.3 Images ("the problematic part")

* Upload **lazily**: only (a) when a signed-in customer syncs, or (b) when a design is submitted for an order.
  Anonymous browsing never uploads photos.
* Upload **originals** (print needs full resolution), content-addressed by SHA-256 ⇒ idempotent, de-duplicated,
  resumable per asset; after upload set `AssetStorage.Uploaded(storageKey)` and the asset is resolvable from any device.
* Optional optimisation: also upload a ~1600 px preview for fast cross-device editing; the PDF uses the original.
* Limits: per-file cap (e.g. 50 MB), accepted JPEG/PNG/WebP (HEIC converted client-side later), quota per customer,
  GC of assets no live design references (anonymous designs: expire after N days without activity).

---

## 6. Order integration

Today (implemented): confirmed design → proof SVG data URL uploaded as draft artwork (`ColorSpace.RGB`, effective
min DPI, design bleed, page count, trim override) → normal cart/order/artwork-review flow; "Edit design" reopens it.

Target: the cart line carries `DesignId` + revision; on checkout the storefront uploads pending assets, calls
`markSubmitted`, and a production job renders the PDF (§3.1) into a new artwork revision (`PreflightRequirement`
checks still apply). `DesignSubmitted` is published on the `EventBus` for that job.

---

## 7. Testing

* `backend/design/01-c-core` — 25 tests: colours, boxes, rotated hit-testing, resize/rotate/snap geometry, image
  placement & DPI, calendar grids/ISO weeks/Czech holidays, every template valid in EN/CS, ops, layouts, preflight,
  SVG output, service revisions/ownership/claim/freeze.
* `backend/json` — `DesignCodecsSpec`: lossless round-trips, forward-compatible decoding, rejection of bad data.
* `modules/ui-editor-core` — `EditorUiSpec`: EN/CS rendering without missing keys, product launch + autosave,
  coalesced undo/redo, pointer gestures, photo drop targeting, review/complete hand-off, reopen, stylesheet.
* Browser smoke tests (headless Chrome, manual): template → drag → resize, photo drop into calendar cover with DPI
  badge, storefront "Design online" → confirm → artwork attached.

---

## 8. Product kinds & presets

| Kind | Default trim | Bleed / safe | Pages | Templates |
|---|---|---|---|---|
| Business card | 85 × 55 | 3 / 4 | 1–2 | Classic, Bold, Photo |
| Flyer | 148 × 210 | 3 / 6 | 1–2 | Event (+ poster layouts) |
| Poster | 297 × 420 | 3 / 10 | 1 | Typographic, Full photo |
| Postcard | 148 × 105 | 3 / 5 | 2 | Holiday greetings |
| Wall calendar | 210 × 297, binding top 10 mm | 3 / 8 | 13 | Photo calendar, Minimal |
| Desk calendar | 210 × 148, binding top 9 mm | 3 / 6 | 13 | Desk calendar |
| Photo book | 210 × 210 | 3 / 8 | 12–60 step 2 | Classic photo book |
| Custom | configured | 3 / 5 | 1–100 | Blank |

Templates are written as fractions of the format, so they adapt to the configured trim size (e.g. 90 × 50 cards).
The storefront category → kind mapping is `DesignPresets.kindForCategory`.

---

## 9. Next phases (checklist)

1. **Sync**: HTTP routes + `HttpStorefrontFacade` methods, `design/02-o-db` (migration `V1__designs.sql`), wire
   `DesignSync` in the editor for signed-in customers (the embed passes `customer`); conflict UI.
2. **Assets**: upload endpoint into `BlobStorage` (sha256), lazy upload on sync/order, server URL resolution in
   `AssetResolver`.
3. **Order**: `DesignId` on cart lines, `markSubmitted` at checkout, `DesignSubmitted` consumer.
4. **PDF**: headless Chromium renderer service + PDF/X-4 conversion; store as artwork revision; staff preview.
5. **Editor**: inline text editing on canvas, marquee selection, QR codes (vCard for business cards), image
   upload size/HEIC handling, more templates per category, holiday sets for other countries, accessibility pass.
