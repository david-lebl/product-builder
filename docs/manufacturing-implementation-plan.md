# Manufacturing UI Implementation Plan

## Status: Phases 1–4 + CSS + Filter Bar complete (2026-03-10)

---

## Phase 1 — Domain Model Enhancement ✅

### 1a. `StationType` enum extended (7 → 14 variants)

**File:** `modules/domain/src/main/scala/mpbuilder/domain/manufacturing/ManufacturingModel.scala`

```
Prepress, DigitalPrinter, OffsetPress, LargeFormatPrinter, Letterpress,
Cutter, Laminator, UVCoater, EmbossingFoil, Folder, Binder,
LargeFormatFinishing, QualityControl, Packaging
```

`WorkflowStatus` enum added: `Pending, InProgress, Completed, OnHold, Cancelled`
(distinct from `OrderStatus` used in `ProductionStep`; available for higher-level workflow state if needed).

### 1b. `SampleStations.scala` updated

- Added `prepress` station (StationType.Prepress, sortOrder 1)
- Renamed `printing` → `StationType.DigitalPrinter`
- Renamed `cutting` → `StationType.Cutter`
- Renamed `lamination` → `StationType.Laminator`
- Renamed `folding` → `StationType.Folder`
- Renamed `binding` → `StationType.Binder`
- Renamed `qualityCheck` → `qualityControl` (StationType.QualityControl)
- `allStations` now has 8 stations (prepress first, packaging last)

### 1c. `WorkflowGenerator.scala` created

**File:** `modules/domain/src/main/scala/mpbuilder/domain/manufacturing/WorkflowGenerator.scala`

- Pure function: `(ProductConfiguration, List[Station], now: Long) => List[ProductionStep]`
- Exhaustive match on `PrintingProcessType` → printer station
- Finish-type → Laminator / UVCoater / EmbossingFoil / Binder / LargeFormatFinishing
- SpecKind.FoldType → Folder; SpecKind.BindingMethod → Binder
- Always: Prepress (first) + Cutter + QualityControl + Packaging (last)
- Deduplicates station types preserving order

---

## Phase 2 — UI Framework: `SplitTableView` ✅

**File:** `modules/ui-framework/src/main/scala/mpbuilder/uikit/containers/SplitTableView.scala`

Generic sortable table + side panel component. No domain dependency.

Key types:
- `ColumnDef[A]` — column definition with optional sort key and width class
- `RowAction[A]` — inline row action with destructive flag
- `SplitTableView.apply[A](...)` — renders table with search, sort, and sliding panel

Implementation notes:
- Uses `Var[String]` for search, `Var[Option[String]]` for selected key, `Var[(String, Boolean)]` for sort
- Laminar `combineWith` flattens tuples via `tuplez` — use explicit arg types in `.map` lambdas
- Panel opens when a row is selected; close button clears selection

---

## Phase 3 — Manufacturing UI Views ✅

### 3a. `ManufacturingViewModel.scala`

**File:** `modules/ui/src/main/scala/mpbuilder/ui/manufacturing/ManufacturingViewModel.scala`

- `ManufacturingUiState` case class + `ManufacturingView` enum
- `stateVar: Var[ManufacturingUiState]` + `state: Signal[ManufacturingUiState]`
- Action methods: `pullOrder`, `startOrder`, `completeOrder`, `holdOrder`, `resumeOrder`, `approveOrder`
- Derived signals: `queuedOrders`, `inProgressOrders`, `completedOrders`, `pendingApprovalOrders`, `onHoldOrders`
- 5 seeded `ManufacturingOrder`s using `SampleCatalog` + `PriceCalculator`
- `approveOrder`: generates workflow steps via `WorkflowGenerator` then pulls into Prepress

### 3b. `ManufacturingApp.scala`

**File:** `modules/ui/src/main/scala/mpbuilder/ui/manufacturing/ManufacturingApp.scala`

Tab navigation (Dashboard / Station Queue / Order Approval / Order Progress) with `.mfg-tab--active` CSS class.

### 3c. `DashboardView.scala`

**File:** `modules/ui/src/main/scala/mpbuilder/ui/manufacturing/views/DashboardView.scala`

- Summary cards: Awaiting Approval, In Production, Queued, Completed, On Hold
- Station status strip (all stations with active/queued/idle badges via `ManufacturingService.stationSummary`)
- Recent orders table (last 10)

### 3d. `StationQueueView.scala`

**File:** `modules/ui/src/main/scala/mpbuilder/ui/manufacturing/views/StationQueueView.scala`

- `SplitTableView` with columns: Priority, Order ID, Customer, Product, Current Station, Status, Deadline
- Row actions: Start / Complete / Hold / Resume (context-sensitive)
- Side panel: workflow step chain, notes, attachments

### 3e. `OrderApprovalView.scala`

**File:** `modules/ui/src/main/scala/mpbuilder/ui/manufacturing/views/OrderApprovalView.scala`

- Shows `pendingApprovalOrders` (no current station, not completed)
- Row actions: Approve (→ starts workflow via `approveOrder`) / Reject (→ hold)
- Side panel: product specs, pricing summary, file list

### 3f. `OrderProgressView.scala`

**File:** `modules/ui/src/main/scala/mpbuilder/ui/manufacturing/views/OrderProgressView.scala`

- Shows orders in workflow or completed
- Progress bar column (completed steps / total steps)
- Row actions: Start / Complete / Resume
- Side panel: per-step vertical chain, fulfilment checklist

---

## Phase 4 — Route Integration ✅

### 4a. `AppRouter.scala`

- Added `AppRoute.Manufacturing`
- Added "Manufacturing / Výroba" nav link
- Added `ManufacturingApp()` to route render switch

---

## Design Decisions

| Decision | Rationale |
|---|---|
| `pendingApprovalOrders` = orders with no `currentStationId` and not `isFullyCompleted` | Avoids adding a new `AwaitingApproval` variant to `OrderStatus` — keeps domain model lean |
| `approveOrder` generates steps on-the-fly via `WorkflowGenerator` | Orders created without steps; steps are derived from configuration when approved |
| `completeAndAdvance` used for "Complete" button | Auto-advances to next station queue (push); pull model available via `pullOrder` |
| `SplitTableView` search is caller-provided | Keeps the component generic — each view provides its own filter logic |
| CSS classes follow BEM-like naming: `mfg-tab`, `status-badge`, `step-chip`, etc. | Consistent with existing UI conventions |
| `WorkflowStatus` added but not yet used in `ProductionStep` | Reserved for higher-level workflow state (e.g., awaiting artwork, on customer hold) |

---

## Phase 5 — CSS Styling ✅

All manufacturing CSS added to `modules/ui/src/main/resources/index.html`.

Styled classes:
- Manufacturing app shell: `.manufacturing-app`, `.manufacturing-tabs`, `.mfg-tab`, `.mfg-tab--active`
- Dashboard: `.dashboard-cards`, `.summary-card`, card color variants, `.station-strip`, `.station-card`, `.station-badge`
- Table: `.data-table`, `.data-table-th/td`, `.data-table-row--selected`, `.sort-indicator`
- Row actions: `.row-action`, `.row-action--destructive`
- Badges: `.priority-badge--*`, `.status-badge--*`, `.deadline-overdue/urgent`
- SplitTableView: `.split-table-view`, `.split-table-toolbar`, `.split-table-panel`, panel open/close animation
- Detail panel: heading/customer/product labels, `.detail-notes`, step chain (horizontal + vertical)
- Progress bar: `.progress-bar-container`, `.progress-bar-track`, `.progress-bar-fill` (gradient fill)
- Checklist: `.checklist-item`, `.checklist-item--done`, check icon colour
- Filter bar: `.filter-bar`, `.filter-chip`, `.filter-chip--active`, `.filter-divider`

## Phase 6 — Filter Bar ✅

**StationQueueView.scala**: Added local `Var[Set[OrderStatus]]` + `Var[Set[OrderPriority]]` with toggle chips:
- Status chips: Queued / InProgress / OnHold
- Priority chips: Urgent / High / Normal / Low
- Multi-select (empty = show all); chips are wired directly into the `activeOrders` signal via `combineWith`

## Future Work

- [ ] Add `WorkflowStatus` to `ManufacturingOrder` for approval/rejection tracking
- [ ] Filter bar in OrderProgressView and OrderApprovalView
- [ ] Station type filter in StationQueueView
- [ ] Deadline date picker / range filter
- [ ] Real-time updates (WebSocket or polling integration)
- [ ] Print job notes editing inline
- [ ] Attachment upload / file viewer
- [ ] Export to PDF / print docket
