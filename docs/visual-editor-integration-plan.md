# Visual Editor Integration & Improvements — Implementation Plan

> Analysis and phased implementation plan for integrating the product builder with the visual editor, adding session persistence, and building an image gallery.

---

## Status Summary

| Phase | Feature | Status |
|-------|---------|--------|
| Phase 1 | Session Persistence | ✅ Implemented |
| Phase 2 | Image Gallery | ✅ Implemented |
| Phase 3 | Product Builder Integration | ✅ Implemented |
| Phase 4 | Polish & UX Refinements | ✅ Partially Implemented |

---

## Current State (After Implementation)

### Visual Editor (`modules/ui/.../calendar/`)
- Standalone canvas editor with support for 6 product types (Monthly/Weekly/Bi-weekly Calendar, Photo Book, Wall Picture, **Custom Product**) and 11+ formats
- Canvas elements: `PhotoElement`, `TextElement`, `ShapeElement`, `ClipartElement` — all as a sealed ADT
- Template system with locked fields (month/day labels for calendars)
- Page navigation with thumbnail strip
- **✅ Session persistence** — auto-save to localStorage with debounced 3-second interval
- **✅ Image gallery** — gallery panel with thumbnail uploads, reusable across sessions
- **✅ Session management** — resume dialog, session panel, rename, delete, load, export/import

### Product Builder (`modules/ui/.../productbuilder/`)
- Full product configuration: category, material, printing, finishes, ink, specifications
- `ArtworkMode` enum: `UploadArtwork(fileName)` or `DesignInEditor(sessionId)`
- Basket with `BasketItem` (configuration + quantity + price breakdown + **editorSessionId**)
- `basketItemArtwork: Map[ConfigurationId, ArtworkMode]` tracks artwork mode per item
- **✅ Editor launch with context** — "Open Visual Editor" passes product specs to create a linked session
- **✅ Artwork status in basket** — shows "In Progress" with "Edit in Editor" button

---

## Phase 1 — Session Persistence (localStorage + Auto-Save) ✅

### 1.1 Session Data Model — ✅ Implemented

Defined in `calendar/EditorSession.scala`:

```
EditorSession:
  id: String (UUID)
  name: String (user-editable, default "Untitled")
  productType: VisualProductType
  productFormat: ProductFormat
  pages: List[CalendarPage]  (full serialized state)
  imageReferences: Set[String]  (hash-based tracking)
  linkedConfigurationId: Option[String]  (Phase 3)
  createdAt: Double (epoch ms)
  updatedAt: Double (epoch ms)
```

A `SessionSummary` projection for listing (no heavy page data):

```
SessionSummary:
  id, name, productType, productFormat, pageCount, elementCount, linkedConfigurationId, createdAt, updatedAt
```

### 1.2 localStorage Storage Layer — ✅ Implemented

**Implementation note:** The plan originally specified IndexedDB. We used **localStorage** instead for simplicity and synchronous access. The `EditorSessionStore` object provides the same API:

- **Keys**: `editor-sessions-index`, `editor-session-{id}`, `editor-pending-session`, `editor-image-gallery`
- **Operations**: `save(session)`, `load(id)`, `delete(id)`, `listSummaries()`, `getLatest()`
- **JSON codec**: Manual `js.Dynamic` ↔ Scala type conversion in `SessionCodec` (private to calendar package)
- **Serialization**: Full calendar state including all page elements, template fields, and backgrounds
- **Limitation**: localStorage has ~5MB limit per origin. Large sessions with many images may exceed this.

### 1.3 Auto-Save — ✅ Implemented

- Debounced auto-save (3 seconds after last change) triggered by all state mutation methods in `CalendarViewModel`
- Save indicator in the session panel: "Saved · 14:32" or "Saving..."
- Auto-save triggers on: element add/remove/update, background change, page navigation, product type/format change

### 1.4 Session Resume Dialog — ✅ Implemented

`SessionResumeDialog.scala`: On editor entry, if previous sessions exist:
1. Shows modal overlay listing up to 5 recent sessions
2. Each row shows: name, product type, format, page count, element count, last updated time
3. Click a session → loads it
4. "Start New Session" → creates fresh state
5. "Close" → dismisses without loading

### 1.5 Session Management Panel — ✅ Implemented

`SessionPanel.scala`: Always-visible bar at top of editor:
- **Current session**: editable name field, save status ("Saved · 14:32")
- **Expand/collapse**: toggle to show session list and actions
- **Actions**: New Session, Save Now, Export (downloads JSON), Import (uploads JSON)
- **Session list**: all saved sessions with name, product type, page count
- **Per-session actions**: Load, Delete
- **Linked product indicator**: shows when session is linked to a product configuration

### Key Files Created/Modified
| File | Action |
|------|--------|
| `calendar/EditorSession.scala` | **New** — session model + summary |
| `calendar/EditorSessionStore.scala` | **New** — localStorage facade + SessionCodec |
| `calendar/CalendarViewModel.scala` | **Modified** — auto-save, session CRUD, gallery ops |
| `calendar/components/SessionPanel.scala` | **New** — session list & management UI |
| `calendar/components/SessionResumeDialog.scala` | **New** — startup modal |
| `calendar/CalendarBuilderApp.scala` | **Modified** — mount resume dialog, session panel |
| `calendar.css` | **Modified** — styles for dialog, panel, save indicator |

---

## Phase 2 — Image Gallery ✅

### 2.1 Gallery Data Model — ✅ Implemented

Defined in `calendar/GalleryImage.scala`:

```
GalleryImage:
  id: String (UUID)
  name: String (original filename)
  thumbnailDataUrl: String (small base64 thumbnail for fast listing)
  width: Int (px)
  height: Int (px)
  addedAt: Double (epoch ms)
  sizeBytes: Long
```

### 2.2 Gallery Storage — ✅ Implemented (localStorage)

**Implementation note:** The plan specified a separate IndexedDB blob store. We store gallery metadata (including thumbnails) in localStorage under the `editor-image-gallery` key. Full-resolution images remain as inline base64 in canvas elements (as they were originally). This is a pragmatic trade-off:
- ✅ Gallery thumbnails persist across sessions
- ✅ Thumbnails can be added to new pages
- ⚠️ Full-resolution originals are not stored separately (localStorage size constraint)

### 2.3 Image Reference — Partial

The plan proposed an `ImageRef` ADT (`GalleryRef` | `InlineData`). We kept the existing inline base64 approach in `PhotoElement.imageData` and `ClipartElement.imageData`. Gallery images are added to pages via their thumbnail data URL. A full `ImageRef` refactor is deferred to when IndexedDB or server-side storage is implemented.

### 2.4 Gallery Panel UI — ✅ Implemented

`GalleryPanel.scala` — new sidebar tab "Image Gallery":
- **Upload**: "Add Images" button with multi-file select
- **Thumbnail generation**: Creates downscaled 150px thumbnails via canvas resize
- **Grid display**: 2-column grid of image thumbnails with filename and dimensions
- **Use on page**: "➕" button adds the image as a new `PhotoElement` on the current page
- **Remove**: "×" button removes from gallery

### 2.5 Session Image Tracking — ✅ Implemented

`EditorSession.imageReferences` tracks hash-based references of images used in session pages. This allows detecting which sessions reference images.

### Key Files Created/Modified
| File | Action |
|------|--------|
| `calendar/GalleryImage.scala` | **New** — gallery model |
| `calendar/EditorSessionStore.scala` | **Modified** — gallery storage methods |
| `calendar/components/GalleryPanel.scala` | **New** — gallery grid UI |
| `calendar/CalendarViewModel.scala` | **Modified** — gallery operations |
| `calendar/CalendarBuilderApp.scala` | **Modified** — added Gallery tab in sidebar |

---

## Phase 3 — Product Builder Integration ✅

### 3.1 Shared Session Reference — ✅ Implemented

- `EditorSession.linkedConfigurationId: Option[String]` — points to a `ConfigurationId` from the product builder
- `BasketItem.editorSessionId: Option[String]` — points back to the editor session
- `ArtworkMode.DesignInEditor(sessionId: Option[String])` — changed from case object to case class carrying the session ID
- Soft string-based linking — no deep coupling between view models

### 3.2 Launch Editor from Product Builder — ✅ Implemented

When "Open Visual Editor" is clicked in the configuration form:
1. `ProductBuilderViewModel.openInEditor()` generates a session ID
2. Extracts product category name and page count from current configuration
3. Stores a `PendingEditorSession` in localStorage (`editor-pending-session` key)
4. Navigates to the Calendar Builder tab
5. `CalendarBuilderApp` checks for pending session on mount and calls `CalendarViewModel.initFromProductConfig()`
6. If a linked session already exists for that configuration → loads it
7. If not → creates a new session with the product context pre-filled

### 3.3 Product Context Display in Editor — ✅ Implemented

`ProductContextBar.scala` — when a session is linked to a product configuration:
- Shows a styled info bar: "Product Name · Format (WxH mm) · N pages"
- "Linked to Product Builder" badge
- Also shown in the session panel as a linked product indicator

### 3.4 Sync Status Back to Basket — ✅ Implemented

In `BasketView.scala`, for items with `DesignInEditor(sessionId)`:
- Shows "🎨 Design: In Progress" when sessionId is present
- Displays "Edit in Editor →" button that navigates to the visual editor
- The actual "Ready" status and PDF generation are deferred to server-side implementation

### 3.5 Open Product Types — ✅ Implemented

- Added `VisualProductType.CustomProduct` to the enum
- `CalendarState.createCustomProductPages()` creates blank pages (default 4) at correct dimensions
- Custom Product is available in the product type dropdown
- When launched from the product builder, Custom Product type is used for arbitrary product configurations

### Key Files Created/Modified
| File | Action |
|------|--------|
| `calendar/EditorSession.scala` | **New** — includes `linkedConfigurationId` |
| `calendar/CalendarModel.scala` | **Modified** — added `CustomProduct` type with factory methods |
| `calendar/CalendarViewModel.scala` | **Modified** — `initFromProductConfig()`, pending session handling |
| `calendar/components/ProductContextBar.scala` | **New** — linked product info display |
| `productbuilder/ProductBuilderViewModel.scala` | **Modified** — `openInEditor()`, `DesignInEditor(sessionId)` |
| `productbuilder/components/ConfigurationForm.scala` | **Modified** — calls `openInEditor()` |
| `productbuilder/components/BasketView.scala` | **Modified** — artwork status, "Edit" button |
| `domain/model/basket.scala` | **Modified** — added `editorSessionId` to `BasketItem` |

---

## Phase 4 — Polish & UX Refinements ✅ (Partial)

### 4.1 Session Thumbnails — Deferred
Generating canvas → `toDataURL` thumbnails requires rendering the full page to an offscreen canvas, which is complex. Deferred to future work.

### 4.2 Gallery Drag-and-Drop — Deferred
Drag-and-drop from gallery to canvas requires complex event handling. Currently, gallery images are added via button click.

### 4.3 Broken Image Recovery — Deferred
Since images are stored inline (not as gallery references), broken images are not an issue with the current implementation. This becomes relevant when the `ImageRef` refactor is completed.

### 4.4 Session Export/Import — ✅ Implemented
- **Export**: Downloads the current session as a JSON file (`editor-session.json`)
- **Import**: File picker to upload a JSON session file, assigns a new ID to avoid conflicts

---

## Far Future

- **PDF artwork generation** from editor canvas (backend service)
- **Collaborative editing** — multiple users working on the same session (WebSocket sync)
- **Template marketplace** — pre-designed layouts
- **Layer system** — explicit layer management with lock/visibility toggles
- **Advanced typography** — text on path, text effects, font upload
- **Vector graphics support** — import/edit SVG elements natively
- **Undo/redo history** — command pattern with full state timeline
- **Cloud storage** for sessions and images (replace localStorage with server-side persistence)
- **AI-assisted layout** — auto-arrange photos, suggest layouts
- **Print preview mode** — show bleed, trim, and safe zones; CMYK color preview
- **Batch operations** — apply the same layout/style across multiple pages
- **IndexedDB migration** — replace localStorage with IndexedDB for larger storage capacity
- **Full ImageRef refactor** — separate image blobs from canvas element data
