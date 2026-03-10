# Manufacturing UI — Implementation Plan

> Tracks what has been implemented, what's in progress, and what remains for future phases.

---

## Phase 1 ✅ — Core Domain Model & Workflow Engine

**Status: Complete**

### Implemented

1. **`manufacturing.scala`** — Full domain model in `mpbuilder.domain.model`:
   - `StationType` enum (14 station types: Prepress, DigitalPrinter, OffsetPress, LargeFormatPrinter, Letterpress, Cutter, Laminator, UVCoater, EmbossingFoil, Folder, Binder, LargeFormatFinishing, QualityControl, Packaging) with `displayName` and `icon` extensions
   - `StepStatus` enum (Waiting, Ready, InProgress, Completed, Skipped, Failed)
   - `WorkflowStatus` enum (Pending, InProgress, Completed, OnHold, Cancelled)
   - `Priority` enum (Rush, Normal, Low) with `sortWeight` for queue ordering
   - `ApprovalStatus` enum (Placed, Approved, Rejected, PendingChanges, OnHold)
   - `WorkflowStep` — individual manufacturing step with DAG dependencies (`dependsOn: Set[StepId]`), employee/machine assignment, timestamps, notes
   - `ManufacturingWorkflow` — workflow per order item with step list, status, priority, deadline; extensions for `readySteps`, `completionRatio`, `evaluateReadiness`
   - `ManufacturingOrder` — combines `Order` with workflows and approval state; extensions for `overallStatus`, `overallCompletionRatio`, `customerName`, `itemSummary`
   - `Employee` and `Machine` models for future use
   - Opaque type IDs: `WorkflowId`, `StepId`, `EmployeeId`, `MachineId` (consistent with existing ID pattern)

2. **`WorkflowGenerator.scala`** — Pure service in `mpbuilder.domain.service`:
   - `generate(ProductConfiguration, ...) => ManufacturingWorkflow`
   - Derives step sequence and DAG dependencies from product configuration
   - Maps `PrintingProcessType` → `StationType` (Digital→DigitalPrinter, Offset→OffsetPress, etc.)
   - Per-component step generation: printing → surface finishing (lamination) → UV coating → decorative finishing (embossing/foil) → cutting → large format finishing → folding
   - Cross-component steps: binding (depends on all component steps), QC (depends on binding or last steps), packaging (depends on QC)
   - Prepress always first (Ready status), all other steps start as Waiting

3. **`WorkflowGeneratorSpec.scala`** — 19 tests covering:
   - Basic workflow structure (Prepress → ... → QC → Packaging)
   - Printing station mapping (Digital, Letterpress, UV Inkjet)
   - Finishing steps (Laminator, UVCoater, EmbossingFoil, Cutter)
   - Multi-component products (booklet cover + body, binding step)
   - Workflow state management (`evaluateReadiness`, `completionRatio`)
   - Folding step generation
   - DAG validity (no self-references, all dependency IDs exist)

---

## Phase 2 ✅ — Shared UI Framework Component

**Status: Complete**

### Implemented

1. **`SplitTableView.scala`** in `mpbuilder.uikit.containers` (ui-framework module):
   - Domain-agnostic sortable data table with generic type parameter `[T]`
   - `ColumnDef[T]` — column header, accessor (renders HtmlElement), optional sort key, optional width
   - `FilterDef` — pluggable filter chips with multi-select support
   - `SplitTableConfig[T]` — table configuration with columns, filters, search, row selection, empty message
   - Search field with placeholder text
   - Filter bar with clickable filter chips (active/inactive toggle)
   - Multi-column client-side sorting (ascending/descending toggle, ▲/▼/⇅ indicators)
   - Side panel that opens when a row is selected (grid layout: table + panel)
   - Row selection highlighting
   - Empty state message
   - Used by all four manufacturing table views

---

## Phase 3 ✅ — Manufacturing UI Views

**Status: Complete**

### Implemented

1. **`ManufacturingModel.scala`** — Route and state type definitions:
   - `ManufacturingRoute` enum (Dashboard, StationQueue, OrderApproval, OrderProgress, Employees)
   - `DashboardSummary`, `StationStatus`, `QueueItem` data types
   - Filter state types: `StationQueueFilters`, `ApprovalFilters`, `ProgressFilters`

2. **`ManufacturingViewModel.scala`** — Reactive state management:
   - Sample data generation from existing `SampleCatalog` configurations (6 orders with realistic products)
   - Derived signals: `dashboardSummary`, `stationStatuses`, `queueItems`, `approvalOrders`, `progressOrders`
   - Actions: `approveOrder`, `rejectOrder`, `startStep`, `completeStep` with proper state transitions
   - `evaluateReadiness` called after step completion to promote Waiting → Ready steps

3. **`ManufacturingApp.scala`** — Main app shell:
   - Sidebar navigation with icon + label buttons
   - Route-based content switching
   - Dark sidebar (#1e293b) with active state highlighting

4. **`DashboardView.scala`** — Dashboard landing page:
   - Summary cards (Awaiting Approval, In Production, Ready for Dispatch, Overdue, Today's Completions)
   - Station status strip (14 station tiles with queue depth and status indicators)
   - Recent orders table with status badges and progress bars
   - Click-to-navigate to Order Progress

5. **`StationQueueView.scala`** — Primary operator view:
   - Uses `SplitTableView` with filterable queue items
   - Station type filter (14 station chips), status filter, priority filter
   - Sortable columns: Priority, Order, Customer, Product, Station, Status
   - Action buttons: Start (Ready steps) / Complete (InProgress steps)
   - Side panel with order header, current step info, workflow progress visualization, order items

6. **`OrderApprovalView.scala`** — Manager/prepress view:
   - Uses `SplitTableView` for approval queue
   - Status filter (Placed, Approved, Rejected, PendingChanges, OnHold)
   - Approve/Reject action buttons with workflow generation on approval
   - Side panel with full order detail: customer info, delivery, item specs with materials/finishes, pricing

7. **`OrderProgressView.scala`** — Fulfilment tracking view:
   - Uses `SplitTableView` for active orders
   - Workflow status and priority filters
   - Progress bar with completed/total step counts
   - Deadline display with urgency coloring (Overdue, Today, Tomorrow, Xd left)
   - Side panel with per-item workflow visualization (step chain with status dots)

### AppRouter Integration

- Added `AppRoute.Manufacturing` route
- Added "Manufacturing" / "Výroba" navigation link
- Navigation bar hidden on manufacturing route (uses its own sidebar)
- Basket button hidden on manufacturing route

### CSS Styles

- Full manufacturing CSS in `index.html` (~600 lines):
  - Sidebar layout (240px dark sidebar + content area)
  - Dashboard cards, station tiles, tables
  - SplitTableView styles (search, filters, table, side panel)
  - Badge system (pending, active, completed, error, warning, info, muted, ready)
  - Progress bars, deadline indicators
  - Detail panel sections, workflow step chain visualization
  - Responsive mobile layout (bottom navigation bar at ≤900px)

---

## Phase 4 — Workflow Engine State Transitions (Future)

**Not yet implemented**

1. **`WorkflowEngine`** — Formal state transition service with `Validation[WorkflowError, ManufacturingWorkflow]`:
   - `startStep(wf, stepId, employeeId)` — validates step is Ready, sets InProgress
   - `completeStep(wf, stepId)` — validates step is InProgress, promotes downstream steps
   - `failStep(wf, stepId, reason)` — marks as Failed, sets workflow OnHold
   - `skipStep(wf, stepId)` — marks as Skipped for optional steps
   - `resetStep(wf, stepId)` — resets to Ready for rework scenarios
   - Enforces DAG constraints (can't start a step whose dependencies aren't met)
   - Returns accumulated validation errors (not short-circuit)

2. **`WorkflowError`** ADT with exhaustive `message` match

3. **`QueueScorer`** — Priority calculation:
   - Deadline urgency (0–100 based on hours until deadline)
   - Priority boost (Rush=30, Normal=0, Low=-10)
   - Completeness boost (0–20 based on % steps done)
   - Batch affinity (0–15 if matches current machine setup)
   - FIFO tiebreaker (age in minutes)

---

## Phase 5 — Employee & Machine Management (Future)

**Not yet implemented**

1. Employee profiles with station capabilities
2. Machine registry with status tracking (Online/Offline/Maintenance)
3. Machine setup tracking for batch affinity scoring
4. Employee login/selection in the UI
5. "My In-Progress Jobs" section on dashboard (filtered by current employee)
6. Employee settings view with station capability toggles

---

## Phase 6 — Order Approval Enhancements (Future)

**Not yet implemented**

1. Artwork review workflow (file check flags: resolution, bleed, color profile)
2. Payment verification integration
3. Priority and deadline assignment at approval time
4. In-house order creation modal (embedding the product builder)
5. Customer contact workflow for artwork issues

---

## Phase 7 — Fulfilment Workflow (Future)

**Not yet implemented**

1. Structured fulfilment checklist in Order Progress side panel:
   - Collect items (per basket item checkbox)
   - Quality check sign-off
   - Package (type selection, dimensions/weight)
   - Delivery (service selection, address, shipping label)
   - Invoice generation
   - Dispatch confirmation (tracking number)
2. Partial order dispatch support
3. Customer notifications at key points

---

## Phase 8 — Analytics & Reporting (Future)

**Not yet implemented**

1. Analytics dashboard view:
   - Average time per station type
   - Bottleneck identification
   - Employee throughput
   - On-time delivery rate
2. Estimated completion time calculation
3. Batch affinity suggestions

---

## Phase 9 — Future Views (Future)

**Not yet implemented**

1. Invoice management view (table, PDF generation, payment tracking)
2. Customer management view (contacts, order history, discount tiers)
3. Station configuration view (machine registry, employee-station assignments)
4. Material/stock tracking integration

---

## Architecture Decisions

| Decision | Choice | Rationale |
|---|---|---|
| Domain purity | Pure functions, no ZIO effects | Consistent with existing architecture, Scala.js compatible |
| Workflow model | DAG with linear UI presentation | Supports parallel component paths; simplified for operator view |
| Queue model | Pull (employee picks from queue) | Operator expertise, material awareness, multi-station flexibility |
| UI framework | `SplitTableView` in ui-framework | Reusable across all manufacturing views; domain-agnostic |
| State management | Laminar `Var`/`Signal` with in-memory data | Consistent with existing ProductBuilderViewModel pattern |
| Sample data | Generated from `SampleCatalog` configurations | Realistic test data using existing domain model |
