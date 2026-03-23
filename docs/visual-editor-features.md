# Visual Editor — New Features Documentation

This document describes all features added as part of the visual editor integration implementation.

---

## 1. Session Persistence

### Overview

Editor sessions are now persisted to browser `localStorage`, so users can resume their work after closing and reopening the browser. Sessions are auto-saved with a 3-second debounce and can be managed through the session panel.

### How It Works

**Auto-Save**: Every state change (adding/moving/editing elements, changing backgrounds, navigating pages) triggers a debounced auto-save. The save status is displayed in the session panel bar ("Saving..." → "Saved · 14:32").

**Session Data**: Each session stores the complete editor state:
- Session metadata (ID, name, product type, format, timestamps)
- All pages with their template fields, backgrounds, and canvas elements
- Image references tracking which images are used
- Optional link to a product builder configuration

**Storage Keys** (in `localStorage`):
| Key | Content |
|-----|---------|
| `editor-sessions-index` | JSON array of session metadata (for fast listing) |
| `editor-session-{id}` | Full JSON session state |
| `editor-pending-session` | Handoff data from product builder |
| `editor-image-gallery` | Image gallery metadata |

### Session Resume Dialog

When navigating to the Visual Editor with existing saved sessions, a modal dialog appears:
- Lists up to 5 recent sessions with name, product type, dimensions, page/element counts
- Click a session row to load and continue editing
- "Start New Session" to begin fresh
- "Close" to dismiss the dialog

### Session Management Panel

A collapsible panel at the top of the editor provides:
- **Session name**: Editable text field (changes trigger auto-save)
- **Save status**: Shows "Saving..." during debounce, "Saved · HH:MM" after save
- **Expand button**: Reveals the full session management UI

Expanded panel features:
- **New Session**: Creates a new empty session (saves current first)
- **Save Now**: Forces an immediate save
- **Export**: Downloads the current session as `editor-session.json`
- **Import**: File picker to load a previously exported session
- **Session list**: All saved sessions with Load/Delete actions
- Current session is highlighted with a bullet (●)

### Key Files

| File | Description |
|------|-------------|
| `calendar/EditorSession.scala` | `EditorSession` and `SessionSummary` case classes |
| `calendar/EditorSessionStore.scala` | localStorage facade with `save`, `load`, `delete`, `listSummaries` |
| `calendar/EditorSessionStore.scala` | `SessionCodec` — JSON codec for all calendar types via `js.Dynamic` |
| `calendar/components/SessionPanel.scala` | Session management panel UI |
| `calendar/components/SessionResumeDialog.scala` | Startup session resume dialog |

---

## 2. Image Gallery

### Overview

A cross-session image gallery allows users to upload images once and reuse them across multiple editor sessions. Gallery images are stored as small thumbnails in `localStorage`.

### Gallery Panel

The gallery is accessible as a new sidebar tab ("Image Gallery") alongside "Page Elements" and "Background":

- **Upload**: "📁 Add Images" button supports multi-file image selection
- **Thumbnails**: Uploaded images are downscaled to 150px thumbnails using an offscreen canvas
- **Grid**: Images displayed in a 2-column grid with filename and dimensions
- **Add to page**: "➕" button creates a new `PhotoElement` on the current page using the thumbnail
- **Delete**: "×" button removes the image from the gallery

### Storage

Gallery metadata (including thumbnail data URLs) is stored in `localStorage` under `editor-image-gallery`. Each `GalleryImage` record contains:
- `id` — UUID
- `name` — original filename
- `thumbnailDataUrl` — base64-encoded downscaled thumbnail
- `width`, `height` — original image dimensions in pixels
- `addedAt` — timestamp
- `sizeBytes` — original file size

### Key Files

| File | Description |
|------|-------------|
| `calendar/GalleryImage.scala` | `GalleryImage` case class |
| `calendar/components/GalleryPanel.scala` | Gallery panel UI with upload, grid, and actions |
| `calendar/CalendarViewModel.scala` | `addToGallery`, `removeFromGallery` methods |

---

## 3. Product Builder Integration

### Overview

The visual editor is now connected to the product builder. When a user chooses "Design in Visual Editor" for a configured product, the editor opens with the product context pre-filled and the session linked back to the basket item.

### Launch Flow

1. User configures a product in the Product Builder
2. Selects "Design in Visual Editor" as the artwork mode
3. Clicks "Open Visual Editor →"
4. `ProductBuilderViewModel.openInEditor()`:
   - Generates a session ID
   - Extracts product category name and page count from the configuration
   - Stores a `PendingEditorSession` in localStorage
   - Navigates to the Visual Editor tab
5. `CalendarBuilderApp` on mount:
   - Checks for a pending session via `EditorSessionStore.consumePendingSession()`
   - If found, calls `CalendarViewModel.initFromProductConfig()`
   - Looks for an existing session linked to the configuration ID
   - If found → loads it; if not → creates a new linked session

### Product Context Display

When a session is linked to a product configuration:
- **Product Context Bar**: Blue info bar showing "Product Name · Format (WxH mm) · N pages" with a "Linked to Product Builder" badge
- **Session Panel**: Shows a 🔗 linked product indicator with the product description

### Basket Artwork Status

In the basket, items with `ArtworkMode.DesignInEditor(sessionId)` show:
- "🎨 Design: In Progress" when a session ID is present
- "Edit in Editor →" button that navigates directly to the visual editor

### Data Model Changes

**`ArtworkMode.DesignInEditor`** changed from `case object` to `case class DesignInEditor(sessionId: Option[String] = None)` to carry the linked session ID.

**`BasketItem`** gained `editorSessionId: Option[String] = None` for tracking the linked editor session.

**`PendingEditorSession`** — new case class for the PB → editor handoff:
```
PendingEditorSession:
  configurationId: String
  productType: VisualProductType
  format: ProductFormat
  pageCount: Int
  productDescription: String
```

### Key Files

| File | Description |
|------|-------------|
| `productbuilder/ProductBuilderViewModel.scala` | `openInEditor()` method |
| `productbuilder/components/ConfigurationForm.scala` | Updated "Open Visual Editor" button |
| `productbuilder/components/BasketView.scala` | Artwork status with "Edit in Editor" button |
| `domain/model/basket.scala` | Added `editorSessionId` to `BasketItem` |
| `calendar/components/ProductContextBar.scala` | Linked product info bar |
| `calendar/EditorSessionStore.scala` | `setPendingSession`, `consumePendingSession`, `findByConfigurationId` |

---

## 4. Custom Product Type

### Overview

A new `CustomProduct` type has been added to the visual editor, enabling it to be used for arbitrary product configurations that don't map to a specific visual product type.

### Details

- `VisualProductType.CustomProduct` — new enum variant
- Default page count: 4 (configurable from product builder specs)
- Blank pages with page numbers and a photo placeholder
- Available formats: Portrait, Landscape, Square, Wall Calendar, Wall Calendar Large
- Appears in the product type dropdown as "Custom Product (4 pages)" / "Vlastní produkt (4 stránky)"
- Used automatically when launching from the product builder

### Key Changes

| File | Change |
|------|--------|
| `calendar/CalendarModel.scala` | Added `CustomProduct` to `VisualProductType`, `ProductFormat.formatsFor`, `CalendarState.defaultPageCount`, `createCustomProductPages` factory |
| `calendar/EditorSessionStore.scala` | Added "custom" ↔ `CustomProduct` codec |
| `calendar/CalendarBuilderApp.scala` | Added "Custom Product" option to product type dropdown |

---

## 5. Session Export/Import

### Export

Downloads the current session as a JSON file:
1. Click "📤 Export" in the session panel
2. The session (including all pages and elements) is serialized to JSON
3. Browser downloads the file as `editor-session.json`

### Import

Loads a previously exported session:
1. Click "📥 Import" in the session panel
2. Select a `.json` file from the file picker
3. The session is deserialized and assigned a new ID (to avoid conflicts)
4. The imported session becomes the current session

---

## 6. Internationalization (i18n)

All new features are fully localized in English (EN) and Czech (CS):
- Session resume dialog titles, labels, and buttons
- Session panel labels, status messages, and action buttons
- Gallery panel labels and empty state messages
- Product context bar text and badges
- Basket artwork status messages and buttons
- Custom Product type name in the dropdown

---

## 7. CSS Additions

New CSS classes added to `calendar.css`:

| Category | Classes |
|----------|---------|
| Session Resume Dialog | `.session-resume-overlay`, `.session-resume-dialog`, `.session-resume-item`, `.session-resume-actions` |
| Session Panel | `.session-panel`, `.session-info-bar`, `.session-name-input`, `.session-save-status`, `.session-list-*` |
| Gallery Panel | `.gallery-panel`, `.gallery-upload-*`, `.gallery-grid`, `.gallery-item-*` |
| Product Context Bar | `.product-context-bar`, `.product-context-content`, `.product-context-badge` |
| Basket Status | `.basket-item-editor-status`, `.basket-edit-in-editor-btn` |

All styles follow existing conventions:
- CSS variables from `tokens.css` (`--color-primary`, `--color-border`, `--shadow-card`)
- Consistent border-radius (6px–12px), padding, and spacing
- Responsive-friendly (no fixed widths on key containers)
- Hover states for all interactive elements
