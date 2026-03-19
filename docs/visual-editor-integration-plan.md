# Visual Editor Integration Plan

This document describes the analysis and implementation plan for connecting the Product Builder
(product parameters view) with the Visual Editor (originally the calendar builder), adding
auto-save, session history, image gallery, and artwork synchronization with the shopping basket.

## 1. Overview

### Current State

- **Product Builder** (`ProductBuilderApp`) — configures product category, material, finish,
  printing method, and specifications (size, quantity, pages, etc.). Supports two artwork modes:
  `UploadArtwork` (attach file) and `DesignInEditor` (placeholder — not yet connected).
- **Visual Editor** (`CalendarBuilderApp`) — a standalone page editor supporting five visual
  product types (Monthly Calendar, Weekly Calendar, Bi-weekly Calendar, Photo Book, Wall Picture)
  with photo/text/shape/clipart elements on a per-page canvas.
- **Shopping Basket** — stores `BasketItem`s keyed by `ConfigurationId`. The `basketItemArtwork`
  map in `BuilderState` already tracks `ArtworkMode` per basket item, but the editor state is
  not referenced.

### Goals

| # | Goal | Scope |
|---|------|-------|
| 1 | **Connect product builder ↔ visual editor** — pass dimensions, pages, and product type from the product builder to the editor, and accept custom product configurations | Implementation |
| 2 | **Artwork ↔ basket synchronization** — link visual editor sessions to basket items via an artwork session ID | Implementation |
| 3 | **Auto-save** — persist editor state in browser local storage automatically | Implementation |
| 4 | **Session history** — list saved sessions with preview, title, timestamp; load or delete | Implementation |
| 5 | **Resume prompt** — on editor open, ask user to continue or start fresh | Implementation |
| 6 | **Image gallery** — collect all images across sessions; warn about broken references | Implementation |
| 7 | **Product overlays** — display product-specific visual hints (wire binding, roll-up stand, picture frame) to help customers visualize the final product | Future |
| 8 | **Templates & clipart gallery** — pre-made layouts and art clips | Future |

---

## 2. Architecture

### 2.1 Product Builder → Visual Editor Connection

When the user selects **"Design in Editor"** artwork mode in the product builder and clicks
"Open Editor", the system:

1. Reads the current product specifications (dimensions from `SizeSpec`, page count from
   `PagesSpec`, product type inferred from category).
2. Creates or reuses an `EditorSession` (see §2.3) and assigns it an `artworkSessionId`.
3. Stores `artworkSessionId` in `BuilderState.artworkMode` as
   `ArtworkMode.DesignInEditor(sessionId)`.
4. Navigates to `AppRoute.CalendarBuilder` with the session pre-configured.
5. The visual editor initializes its state from the session, using the product parameters as
   canvas dimensions and page count.

For pre-configured products (e.g. calendars with exact dimensions), the editor still selects the
matching `VisualProductType` and `ProductFormat`. For arbitrary custom products, the editor uses
a `CustomProduct` type with user-provided dimensions and page count.

### 2.2 Visual Editor → Basket Synchronization

When adding a configured product to the basket:

- If `artworkMode` is `DesignInEditor(sessionId)`, the `sessionId` is stored alongside the
  `BasketItem` in `basketItemArtwork`.
- This `sessionId` is a reference that a backend service would later use to fetch the generated
  PDF artwork. For now, the ID is stored as a string identifier.
- The basket view shows the artwork mode (upload vs. editor-designed) and allows re-opening the
  editor session.

### 2.3 Auto-Save & Session Model

```
EditorSession
├── id: String              // unique session ID (timestamp-based)
├── title: String           // user-editable title (default: product type + date)
├── lastUpdated: Double     // epoch millis
├── productType: String     // serialized VisualProductType
├── productFormat: String   // format ID
├── calendarStateJson: String  // JSON-serialized CalendarState
├── sourceConfigId: Option[String]  // link to product builder ConfigurationId
└── thumbnailDataUrl: Option[String] // tiny preview image
```

**Storage**: Browser `localStorage` with key prefix `editor-session-`.

- **Auto-save** triggers on every state change with a 2-second debounce.
- **Session list** stored under key `editor-sessions-index` as a JSON array of metadata
  (id, title, lastUpdated, productType, thumbnailDataUrl).
- **Session data** stored under key `editor-session-{id}` as a full JSON blob.

### 2.4 Session History UI

A panel (accessible from the visual editor header) showing all saved sessions:

- **Preview thumbnail** — small canvas snapshot
- **Title** — editable inline
- **Last updated** — relative time ("2 hours ago")
- **Actions**: Load, Delete
- Sorted by `lastUpdated` descending

### 2.5 Resume Prompt

When navigating to the visual editor:

1. Check localStorage for any sessions matching the current `artworkSessionId` (if coming from
   product builder) or any recent session (if opening editor directly).
2. If a matching session exists, show a modal dialog:
   - "You have unsaved work from {date}. Continue where you left off?"
   - Buttons: **Continue** | **Start New**
3. If no session exists, start with a fresh editor state.

### 2.6 Image Gallery

Collects all unique image references (base64 data URLs) from all saved sessions:

- Scans all session data for `PhotoElement.imageData` and `ClipartElement.imageData` values.
- Displays as a thumbnail grid.
- Images that fail to render show a warning badge with option to re-link or clear.
- Clicking an image inserts it into the current editor page.

---

## 3. Implementation Details

### 3.1 Model Changes

**`ArtworkMode`** — extend `DesignInEditor` to carry a session ID:

```scala
// Before
case object DesignInEditor extends ArtworkMode

// After  [x] DONE
case class DesignInEditor(sessionId: Option[String] = None) extends ArtworkMode
```

**`CalendarModel`** — add `CustomProduct` to `VisualProductType`:

```scala
// Added  [x] DONE
case class CustomProduct(pages: Int, widthMm: Int, heightMm: Int) extends VisualProductType
```

### 3.2 New Files

| File | Purpose | Status |
|------|---------|--------|
| `calendar/EditorSessionStore.scala` | localStorage read/write for sessions | [x] DONE |
| `calendar/components/SessionHistoryPanel.scala` | Session list UI | [x] DONE |
| `calendar/components/ResumeDialog.scala` | Modal resume prompt | [x] DONE |
| `calendar/components/ImageGalleryPanel.scala` | Cross-session image gallery | [x] DONE |

### 3.3 Modified Files

| File | Change | Status |
|------|--------|--------|
| `ProductBuilderViewModel.scala` | `DesignInEditor` now carries sessionId; `openInEditor()` method; navigation helper | [x] DONE |
| `CalendarViewModel.scala` | Session load/save integration; `initFromProductConfig()` method | [x] DONE |
| `CalendarBuilderApp.scala` | Session history tab; resume dialog on mount; image gallery tab | [x] DONE |
| `CalendarModel.scala` | `CustomProduct` variant; JSON serialization helpers | [x] DONE |
| `AppRouter.scala` | Navigation with session context | [x] DONE |
| `calendar.css` | Styles for session history, resume dialog, image gallery | [x] DONE |

### 3.4 Auto-Save Flow

```
User edits canvas
  → CalendarViewModel.stateVar changes
  → 2-second debounce timer
  → EditorSessionStore.save(currentSessionId, calendarState)
  → Update session index in localStorage
```

### 3.5 Product Builder → Editor Navigation Flow

```
User selects "Design in Editor" in product builder
  → User clicks "Open in Visual Editor" button
  → ProductBuilderViewModel.openInEditor():
      1. Read current specs (size, pages, category)
      2. Generate artworkSessionId
      3. Update artworkMode = DesignInEditor(Some(sessionId))
      4. Store product config reference in session
      5. AppRouter.navigateTo(CalendarBuilder)
  → CalendarBuilderApp mounts:
      1. Check for pending session from product builder
      2. Initialize editor with correct dimensions/pages
      3. Show resume dialog if existing session found
```

---

## 4. Data Flow Diagram

```
┌──────────────────┐     artworkSessionId      ┌──────────────────┐
│  Product Builder  │ ───────────────────────── │  Visual Editor   │
│  (parameters)     │                           │  (canvas)        │
│                   │  dimensions, pages,       │                  │
│  ArtworkMode:     │  product type             │  CalendarState   │
│  DesignInEditor   │ ─────────────────────── → │                  │
│  (sessionId)      │                           │  EditorSession   │
└────────┬─────────┘                           └────────┬─────────┘
         │                                              │
         │ addToBasket()                                │ auto-save
         ▼                                              ▼
┌──────────────────┐                           ┌──────────────────┐
│  Shopping Basket  │                           │  localStorage    │
│                   │                           │                  │
│  basketItemArtwork│                           │  editor-session- │
│  [configId →      │                           │  {id}            │
│   sessionId]      │                           │                  │
└──────────────────┘                           │  editor-sessions │
                                               │  -index          │
                                               └──────────────────┘
```

---

## 5. Future Enhancements

### 5.1 Product Overlays (Visual Hints)

Display product-specific background elements to help customers visualize the final product:

- **Calendar with wire binding** — show wire spiral at the top edge
- **Roll-up banner with stand** — show the stand/base below the canvas
- **Picture with frame** — show frame border around the canvas
- **Booklet with binding** — show spine/staple indicators

These overlays are non-editable decorative layers rendered behind/around the canvas.

### 5.2 Templates Gallery

Pre-designed page layouts that users can apply:

- Holiday themes, seasonal designs
- Business templates (corporate calendars, product catalogs)
- Photo arrangement templates (grid, collage, full-bleed)

### 5.3 Clipart Gallery

Searchable collection of decorative elements:

- Icons, borders, frames, stickers
- Categorized by theme (nature, business, holidays)
- Drag-and-drop onto canvas

### 5.4 Backend Integration

When backend is implemented:

- Sessions sync to server via API instead of localStorage
- PDF generation from `CalendarState` on the backend
- Image upload to CDN instead of base64 in localStorage
- Collaborative editing support

---

## 6. Implementation Phases

### Phase 1: Core Connection (this PR) [x] DONE

- [x] Extend `ArtworkMode.DesignInEditor` with session ID
- [x] Add `CustomProduct` visual product type
- [x] Create `EditorSessionStore` for localStorage persistence
- [x] Implement auto-save with debouncing in `CalendarViewModel`
- [x] Add session history panel with load/delete
- [x] Add resume dialog on editor open
- [x] Add image gallery panel
- [x] Connect product builder navigation to editor with parameters
- [x] Add CSS for new components

### Phase 2: Enhanced Overlays (future)

- [ ] Product-specific visual overlays (wire binding, frames, stands)
- [ ] Overlay configuration based on product category properties
- [ ] Print bleed/safe area indicators

### Phase 3: Templates & Clipart (future)

- [ ] Template gallery with predefined layouts
- [ ] Clipart library with categories
- [ ] Drag-and-drop from gallery to canvas
- [ ] User-uploaded clipart management

### Phase 4: Backend Sync (future)

- [ ] Server-side session storage API
- [ ] PDF artwork generation service
- [ ] Image CDN upload
- [ ] Session sharing and collaboration
