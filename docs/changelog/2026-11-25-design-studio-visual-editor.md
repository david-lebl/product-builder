# 2026-11-25 — Design Studio (Visual Editor) rewrite in Kyo UI

**PR:** N/A (branch `t3/visual-editor-kyo-module`)
**Author:** agent (Claude)
**Type:** feature

## Summary

Rebuilt the pre-Kyo visual editor PoC as a print-grade Design Studio: a new `design` bounded context with a
physical-unit document model and shared SVG renderer, a Kyo UI editor module with local-first IndexedDB storage,
and a separate editor page that the storefront opens in an iframe ("Design online" per printable component). The
confirmed design is attached to the configured product as draft artwork. Full analysis (sync, image upload, PDF
pipeline, bundle measurements): [visual-editor-architecture.md](../analysis/visual-editor-architecture.md).

## Changes Made

- **New** `backend/design/01-c-core` — `DesignModel` (mm/pt, trim-relative; elements: text, image slot, shape,
  clipart, live calendar grid), `DesignGeometry` (rotation-aware resize/rotate/snap, image placement & DPI),
  `CalendarMath` (ISO weeks, Czech holidays), `DesignOps`, `DesignTemplates` / `DesignPresets` / `PageLayouts`
  (11 templates, 8 product kinds), `DesignPreflight`, `DesignValidation`, `FontCatalog`, `Clipart`, `DesignSvg`,
  `DesignService` + `DesignServiceLive` (revisions, owners, claim, submit-freeze) — 25 tests.
- `backend/shared` — `DesignId`. `backend/json` — `DesignCodecs` + `DesignCodecsSpec`. `backend/sample` —
  `InMemoryDesignRepository`, `DesignSampleWiring`.
- **New** `modules/ui-editor-core` — `EditorController`, views (home, workspace, canvas + overlay, panels,
  inspector, page strip, review/proof dialog), `EditorTranslations` (EN/CS), `EditorTheme`; Scala.js glue
  `BrowserEditor` and `IndexedDbStores` — 9 tests.
- **New** `modules/ui-editor` — `EditorMain` page + `EmbedProtocol` (query params in, `postMessage` out).
- `ui-core` — `DesignOnlineRequest`, `DesignArtwork`, `ConfiguratorController.attachDesignArtwork`,
  `DraftConfig.designs`, "Design online / Edit design" card in the artwork step, `cfg-design-*` strings.
- `modules/ui/Main.scala` — iframe opener + message handling.
- `build.mill`, CI and deploy workflows (`_site/editor/`), `scripts/dev.sh` (`--app=editor`; `ui`/`all` build the editor), HOW_TO_RUN.md, CLAUDE.md, roadmap, docs index.

## Decisions & Rationale

- **Iframe over lazy ES-module chunk**: measured — ES modules disable Closure (storefront initial JS 3.47 → 5.98 MB
  gzip); a second Kyo runtime/root in one document collides on `data-kyo-path`. The iframe keeps the storefront
  bundle unchanged and loads the 2.5 MB-gzip editor only on demand.
- **Physical units + one renderer** so canvas, thumbnails, proof and the future Chromium-based PDF agree.
- **Local-first** IndexedDB (photos are large, offline-capable); server sync is designed around the already
  implemented revision/ownership semantics of `DesignService`.

## Issues Encountered

- Kyo UI's base CSS makes `div`/`header` flex columns → rows need explicit `flex-direction:row`.
- `\p{L}` regexes are unsupported by the ES2015 Scala.js target (found via browser smoke test).
- `kyo.Frame` clashes with a geometry type named `Frame` → named `Placement`.
