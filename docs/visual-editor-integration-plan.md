# Visual Editor Integration & Improvements — Implementation Plan

> Analysis and phased implementation plan for integrating the product builder with the visual editor, adding session persistence, and building an image gallery.
>
> **Status:** Phases 1-3 implemented, Phase 4 partial (CSS polish done, thumbnails/export deferred).

---

## Current State

### Visual Editor (`modules/ui/.../calendar/`)
- Standalone canvas editor with support for 5 product types (Monthly/Weekly/Bi-weekly Calendar, Photo Book, Wall Picture) and 11 formats
- Canvas elements: `PhotoElement`, `TextElement`, `ShapeElement`, `ClipartElement` — all as a sealed ADT
- Template system with locked fields (month/day labels for calendars)
- Page navigation with thumbnail strip
- **No session persistence** — all state lost on browser refresh
- **No image gallery** — photos are uploaded per-element as base64 data URLs

### Product Builder (`modules/ui/.../productbuilder/`)
- Full product configuration: category, material, printing, finishes, ink, specifications
- `ArtworkMode` enum: `UploadArtwork` or `DesignInEditor`
- Basket with `BasketItem` (configuration + quantity + price breakdown)
- `basketItemArtwork: Map[ConfigurationId, ArtworkMode]` tracks artwork mode per item
- **No data flows between editor and builder** — they are independent tabs with separate view models

### Missing
- No session/state persistence for editor work (only language preference in localStorage)
- No link between a configured product and a visual editor session
- No shared image gallery across sessions
- No mechanism to associate finished artwork with a basket item

---

## Phase 1 — Session Persistence (IndexedDB + Auto-Save) ✅

### Goal
Auto-save the user's in-progress editor work to the browser's IndexedDB so it survives page refreshes. Let users manage multiple named sessions.

### 1.1 Session Data Model

Define the session model in the UI module (not domain — this is browser-local state):

```
EditorSession:
  id: String (UUID)
  name: String (user-editable, default "Untitled")
  productType: VisualProductType
  productFormat: ProductFormat
  pages: List[CalendarPage]  (full serialized state)
  imageReferences: Set[String]  (keys into gallery, not raw base64)
  linkedConfigurationId: Option[String]  (Phase 3)
  createdAt: Long (epoch ms)
  updatedAt: Long (epoch ms)
```

A `SessionSummary` projection for listing (no heavy page data):

```
SessionSummary:
  id, name, productType, productFormat, pageCount, updatedAt
```

### 1.2 IndexedDB Storage Layer

Create `EditorSessionStore` — a Scala.js facade over IndexedDB:
- DB name: `mpbuilder-editor`, version `1`
- Object store: `sessions` (keyPath: `id`)
- Indexes: `by-updated` on `updatedAt` (for sorted listing)
- Operations: `save(session)`, `load(id)`, `delete(id)`, `listSummaries()`, `getLatest()`
- Use `scala.scalajs.js.Promise` / callback wrappers for async IndexedDB API
- JSON serialization of `CalendarPage` and elements (derive or manual codec)

### 1.3 Auto-Save

- Debounced auto-save (e.g. 3 seconds after last change) triggered by `CalendarViewModel` state updates
- Save indicator in the toolbar: "Saved · 14:32" or "Saving..."
- On unload (`beforeunload`), trigger a final save

### 1.4 Session Resume Dialog

On editor entry (when `CalendarBuilderApp` mounts):
1. Query `EditorSessionStore.listSummaries()`
2. If sessions exist → show modal with two options:
   - **Continue previous session** — list of `SessionSummary` rows (name, product type, dimensions, page count, last updated)
   - **Start new session** — proceeds with fresh state (as today)
3. If no sessions → go directly to fresh state

### 1.5 Session Management Panel

Add a collapsible "Sessions" side panel or toolbar dropdown:
- **Current session**: editable name field, status ("Saved · 14:32")
- **Rename**: inline edit of session name
- **Session list**: all sessions with name, product description, dimensions (`WxH mm`), page count, last edit time
- **Actions per session**: Load, Delete (without confirmation)
- Loading a session replaces current editor state (prompt to save current first if dirty)

### Key Files to Create/Modify
| File | Action |
|------|--------|
| `calendar/EditorSession.scala` | **New** — session model + summary |
| `calendar/EditorSessionStore.scala` | **New** — IndexedDB facade |
| `calendar/CalendarViewModel.scala` | Modify — integrate auto-save, session load/save |
| `calendar/components/SessionPanel.scala` | **New** — session list & management UI |
| `calendar/components/SessionResumeDialog.scala` | **New** — startup modal |
| `calendar/CalendarBuilderApp.scala` | Modify — mount resume dialog, session panel |
| `calendar.css` | Modify — styles for dialog, panel, save indicator |

---

## Phase 2 — Image Gallery ✅

### Goal
Provide a shared image gallery across all editor sessions. Images are stored as references (URLs or IndexedDB blob keys), not inline base64 in the canvas state.

### 2.1 Gallery Data Model

```
GalleryImage:
  id: String (UUID)
  name: String (original filename)
  blobKey: String (IndexedDB blob store key)
  thumbnailDataUrl: String (small base64 thumbnail for fast listing)
  width: Int (px)
  height: Int (px)
  addedAt: Long (epoch ms)
  sizeBytes: Long
```

### 2.2 IndexedDB Image Store

Extend the `mpbuilder-editor` database (version bump):
- Object store: `images` (keyPath: `id`) — stores `GalleryImage` metadata
- Object store: `image-blobs` (keyPath: `id`) — stores raw `Blob` data
- Operations: `addImage(file)`, `addImages(files)`, `removeImage(id)`, `getBlob(id)`, `listImages()`, `getThumbnail(id)`
- On upload: generate a downscaled thumbnail (canvas resize), store blob + metadata

### 2.3 Image Reference in Canvas

Refactor `PhotoElement` and `ClipartElement`:
- Replace `imageData: String` (base64) with `imageRef: ImageRef`
- `ImageRef` is either `GalleryRef(galleryImageId)` or `InlineData(base64)` (backward compat)
- On render: if `GalleryRef`, load blob from store → create `URL.createObjectURL` (cached)
- On session save: only the `galleryImageId` is persisted (not the blob)

### 2.4 Gallery Panel UI

Add a "Gallery" tab/panel in the editor sidebar:
- Grid of image thumbnails with filename and dimensions
- **Upload**: "Add Images" button — multi-file select (`input[multiple]`), drag-and-drop zone
- **Use on page**: click an image → adds `PhotoElement` with `GalleryRef` to current page center
- **Remove**: delete from gallery (with warning if used in any session)
- **Broken references**: if a gallery image is missing when loading a session, show a warning banner: "N image(s) could not be loaded" with list of affected elements highlighted

### 2.5 Session Image Tracking

Each `EditorSession.imageReferences` tracks which gallery image IDs are used. This allows:
- Showing which sessions reference a given image before deletion
- Detecting broken references on session load

### Key Files to Create/Modify
| File | Action |
|------|--------|
| `calendar/GalleryImage.scala` | **New** — gallery model |
| `calendar/ImageStore.scala` | **New** — IndexedDB image blob store |
| `calendar/components/GalleryPanel.scala` | **New** — gallery grid UI |
| `calendar/CalendarModel.scala` | Modify — add `ImageRef` ADT, refactor `PhotoElement`/`ClipartElement` |
| `calendar/CalendarViewModel.scala` | Modify — gallery integration, image loading |
| `calendar/components/CalendarPageCanvas.scala` | Modify — render from `ImageRef` |
| `calendar/components/ElementListEditor.scala` | Modify — photo upload → gallery flow |
| `calendar/EditorSessionStore.scala` | Modify — DB version bump, image stores |

---

## Phase 3 — Product Builder Integration ✅

### Goal
Connect the visual editor to the product builder so a configured product can be designed in the editor, and the resulting artwork is linked back to the basket.

### 3.1 Shared Session Reference

Introduce a lightweight link between systems:
- `EditorSession.linkedConfigurationId: Option[String]` — points to a `ConfigurationId` from the product builder
- `BasketItem` gets `editorSessionId: Option[String]` — points back to the editor session
- This is a soft link (string ID) — no deep coupling between view models

### 3.2 Launch Editor from Product Builder

When `ArtworkMode.DesignInEditor` is selected for a basket item:
1. Product builder passes product context to the editor:
   - Dimensions from `SpecValue.SizeSpec` → set `ProductFormat` (or custom dimensions)
   - Page count from `SpecValue.PagesSpec` → set page count
   - Product category → suggest `VisualProductType`
2. If a linked session already exists → load it
3. If not → create new session with product context pre-filled, link it
4. Navigate to the visual editor tab

### 3.3 Product Context Display in Editor

When a session is linked to a product configuration:
- Show a read-only info bar: "Business Card · 90×50mm · 2 pages · Matte Lamination"
- Display product-specific visual aids (background overlays):
  - **Calendar with wiring**: show wire-binding zone at top edge
  - **Roll-up with platform**: show platform base area at bottom
  - **Picture with frame**: show frame border overlay
  - **Booklet with binding**: show spine/binding margin
- These are non-printable template overlays, not canvas elements

### 3.4 Sync Status Back to Basket

- In `BasketView`, for items with `DesignInEditor`:
  - Show "Artwork: In Progress" / "Artwork: Ready" status
  - "Edit in Visual Editor" button → navigates to linked session
- Status is derived from session existence + a user-set "mark as ready" flag
- The actual PDF generation from the editor canvas is out of scope (backend concern) — the link via session ID is sufficient for now

### 3.5 Open Product Types (Beyond Pre-configured)

The editor already supports custom dimensions via `ProductFormat`. For arbitrary product configurations:
- If the product category doesn't map to a known `VisualProductType`, use a generic "Custom Product" type
- Dimensions and page count come from the product builder specifications
- No calendar template fields — just blank pages at the correct dimensions
- This makes the editor usable for any product, not just calendars and photo books

### Key Files to Create/Modify
| File | Action |
|------|--------|
| `calendar/EditorSession.scala` | Modify — add `linkedConfigurationId` |
| `calendar/CalendarModel.scala` | Modify — add generic `CustomProduct` type, template overlays |
| `calendar/CalendarViewModel.scala` | Modify — accept product context, link management |
| `calendar/components/ProductContextBar.scala` | **New** — linked product info display |
| `calendar/components/TemplateOverlay.scala` | **New** — visual aids (wiring, frame, platform) |
| `productbuilder/ProductBuilderViewModel.scala` | Modify — launch editor with context, store session link |
| `productbuilder/components/BasketView.scala` | Modify — artwork status, "Edit" button |
| `domain/model/basket.scala` | Modify — add `editorSessionId` to `BasketItem` |
| `AppRouter.scala` | Modify — support navigation with session context |

---

## Phase 4 — Polish & UX Refinements

### 4.1 Session Thumbnails
- Generate a thumbnail preview of the first page on save (canvas → `toDataURL`)
- Show in session list and resume dialog

### 4.2 Gallery Drag-and-Drop
- Drag images from gallery panel directly onto the canvas
- Drop zone highlighting on the canvas area

### 4.3 Broken Image Recovery
- When images fail to load, show placeholder with "Image not found" overlay
- Let user re-link: click broken image → file picker → replace the gallery entry (same ID, new blob)

### 4.4 Session Export/Import
- Export a session as JSON (for backup or sharing between browsers)
- Import from JSON file

---

## Far Future

- **PDF artwork generation** from editor canvas (backend service, likely using headless browser or PDF library)
- **Collaborative editing** — multiple users working on the same session (WebSocket sync)
- **Template marketplace** — pre-designed layouts users can start from (calendar themes, photo book layouts)
- **Layer system** — explicit layer management (background, content, overlay) with lock/visibility toggles
- **Advanced typography** — text on path, text effects, font upload
- **Vector graphics support** — import/edit SVG elements natively
- **Undo/redo history** — command pattern with full state timeline
- **Cloud storage** for sessions and images (replace IndexedDB with server-side persistence when user accounts exist)
- **AI-assisted layout** — auto-arrange photos, suggest layouts based on image count and aspect ratios
- **Print preview mode** — show bleed, trim, and safe zones; CMYK color preview
- **Batch operations** — apply the same layout/style across multiple pages
- **Re-linking broken gallery images** to new files on the user's machine