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

## Phase 4 ✅ — Workflow Engine State Transitions

**Status: Complete**

### Implemented

1. **`WorkflowError.scala`** — Error ADT in `mpbuilder.domain.service`:
   - 9 error variants: `StepNotFound`, `StepNotReady`, `StepNotInProgress`, `StepAlreadyCompleted`, `StepAlreadySkipped`, `DependenciesNotMet`, `WorkflowNotActive`, `StepCannotBeSkipped`, `StepCannotBeReset`
   - Exhaustive `message` match for both English and Czech

2. **`WorkflowEngine.scala`** — Pure state transition service in `mpbuilder.domain.service`:
   - `startStep(wf, stepId, employeeId)` — validates step is Ready + dependencies met, sets InProgress, assigns employee
   - `completeStep(wf, stepId)` — validates step is InProgress, promotes downstream steps via `promoteReadySteps`, derives workflow status
   - `failStep(wf, stepId, reason)` — marks as Failed, sets workflow OnHold, appends failure reason to notes
   - `skipStep(wf, stepId)` — marks Waiting/Ready as Skipped for optional stations, promotes downstream steps; required stations (Prepress, QC, Packaging) cannot be skipped
   - `resetStep(wf, stepId)` — resets Completed/Failed/Skipped to Ready for rework, sets `isRework` flag, reverts downstream steps to Waiting, transitions workflow from OnHold/Completed back to InProgress
   - All operations return `Validation[WorkflowError, ManufacturingWorkflow]`
   - DAG constraint enforcement (dependencies must be Completed/Skipped before starting)
   - Automatic downstream step promotion after completion/skip
   - Automatic workflow status derivation (Pending → InProgress → Completed / OnHold)

3. **`QueueScorer.scala`** — Pure priority calculation service in `mpbuilder.domain.service`:
   - `QueueScore` case class: `deadlineUrgency` (0–100), `priorityBoost` (Rush=30, Normal=0, Low=-10), `completenessBoost` (0–20), `batchAffinity` (0–15), `ageMinutes` (FIFO tiebreaker)
   - `score(workflow, now)` — calculates composite score for a workflow
   - `scoreWithAffinity(workflow, step, now, currentMaterialId, stepMaterialId)` — adds batch affinity scoring
   - `sortByScore` — sorts scored items by descending total, then descending age
   - Deadline urgency tiers: overdue=100, ≤2h=95, ≤8h=80, ≤24h=60, ≤48h=40, ≤72h=20, >72h=5

4. **`WorkflowEngineSpec.scala`** — 27 tests covering:
   - `startStep`: Ready→InProgress, Pending→InProgress workflow promotion, step-not-found/not-ready/completed/cancelled validation
   - `completeStep`: InProgress→Completed, downstream promotion, workflow Completed detection
   - `failStep`: InProgress→Failed, workflow→OnHold, failure notes
   - `skipStep`: Ready→Skipped, downstream promotion, required station rejection, InProgress rejection
   - `resetStep`: Completed/Failed→Ready with rework flag, downstream reversion, OnHold→InProgress
   - DAG enforcement: unmet dependencies prevent start
   - Multi-component workflow: binding step readiness after component completion
   - Error messages: all 9 error variants have En/Cs translations

5. **`QueueScorerSpec.scala`** — 24 tests covering:
   - Deadline urgency tiers (all 8 levels)
   - Priority boost (Rush/Normal/Low)
   - Completeness boost (0%/80%/100%)
   - Batch affinity (match/mismatch/none)
   - Age calculation (normal/negative)
   - Composite scoring (Rush+overdue > Normal+comfortable, sort order, FIFO tiebreaker)

---

## Phase 5 ✅ — Employee & Machine Management

**Status: Complete**

### Implemented

1. **`ManagementError.scala`** — Error ADT in `mpbuilder.domain.service`:
   - 7 error variants: `EmployeeNotFound`, `EmployeeNameEmpty`, `EmployeeAlreadyExists`, `EmployeeNoCapabilities`, `MachineNotFound`, `MachineNameEmpty`, `MachineAlreadyExists`
   - Exhaustive `message` match for both English and Czech

2. **`EmployeeManagementService.scala`** — Pure service in `mpbuilder.domain.service`:
   - `addEmployee` — validates ID uniqueness, name non-empty, capabilities non-empty
   - `updateEmployee` — updates name and active status with validation
   - `updateCapabilities` — changes station capabilities with non-empty validation
   - `toggleActive` — toggles active/inactive status
   - `removeEmployee` — removes employee with existence check
   - All operations return `Validation[ManagementError, List[Employee]]`

3. **`MachineManagementService.scala`** — Pure service in `mpbuilder.domain.service`:
   - `addMachine` — validates ID uniqueness and name non-empty, trims inputs
   - `updateMachine` — updates name and notes with validation
   - `changeStatus` — transitions machine between Online/Offline/Maintenance
   - `changeStationType` — reassigns machine to a different station type
   - `removeMachine` — removes machine with existence check
   - All operations return `Validation[ManagementError, List[Machine]]`

4. **`MachineStatus` extensions** — Added `displayName` and `icon` to `MachineStatus` enum (Online 🟢, Offline 🔴, Maintenance 🟡)

5. **`EmployeeManagementServiceSpec.scala`** — 17 tests covering:
   - `addEmployee`: success, duplicate ID, empty name, empty capabilities, name trimming
   - `updateEmployee`: success, not found, empty name
   - `updateCapabilities`: success, empty set, not found
   - `toggleActive`: active→inactive, inactive→active, not found
   - `removeEmployee`: success, not found
   - Error messages: all 7 error variants have En/Cs translations

6. **`MachineManagementServiceSpec.scala`** — 16 tests covering:
   - `addMachine`: success, duplicate ID, empty name, trimming
   - `updateMachine`: success, not found, empty name
   - `changeStatus`: to Offline, to Maintenance, not found
   - `changeStationType`: success, not found
   - `removeMachine`: success, not found
   - `MachineStatus` extensions: display names, icons

7. **`ManufacturingRoute` update** — Enabled `Employees` route (was "coming soon"), added `Machines` route

8. **`ManufacturingViewModel` updates**:
   - Employee/machine state: `employees`, `machines`, `currentEmployeeId`, `selectedEmployeeId`, `selectedMachineId` Vars
   - `currentEmployee` derived signal (resolves ID to Employee)
   - `myInProgressJobs` signal — filters steps assigned to current employee
   - `startStep` now assigns current employee to step via `assignedTo`
   - Employee actions: `addEmployee`, `toggleEmployeeActive`, `updateEmployeeCapabilities`, `setCurrentEmployee`
   - Machine actions: `addMachine`, `changeMachineStatus`, `updateMachineNotes`
   - Sample data: 5 employees with different capabilities, 8 machines with different statuses

9. **`EmployeesView.scala`** — Employee management view:
   - Uses `SplitTableView` with active/inactive filter
   - Current employee selector (dropdown) at the top
   - Columns: Name, Status, Stations (chip list), Actions
   - Activate/Deactivate inline actions
   - Side panel with employee detail and station capability toggles (click to add/remove)

10. **`MachinesView.scala`** — Machine registry view:
    - Uses `SplitTableView` with status and station type filters
    - Columns: Machine name, Station Type, Status (with icon), Notes, Actions
    - Status change actions: Online/Maintenance/Offline buttons
    - Side panel with machine detail, notes, and status controls

11. **`DashboardView` update** — "My In-Progress Jobs" section:
    - Shown when logged in as an employee
    - Lists steps the current employee has claimed (InProgress + assignedTo match)
    - Each job shows order, station, product, and a [Complete] button
    - Empty state message when no jobs in progress

12. **CSS** — Added styles for:
    - `btn-warning` (amber, for maintenance/deactivation actions)
    - Employee selector, station capability chips, capability toggle grid
    - Machine name, notes preview, station type display, status controls
    - "My In-Progress Jobs" section with empty state

---

## Phase 6 ✅ — Order Approval Enhancements

**Status: Complete**

### Implemented

1. **`PaymentStatus` enum** in `manufacturing.scala`:
   - Variants: `Pending`, `Confirmed`, `Failed`
   - `displayName` and `icon` extensions

2. **`CheckStatus` enum** in `manufacturing.scala`:
   - Variants: `NotChecked`, `Passed`, `Warning`, `Failed`
   - `displayName` and `icon` extensions

3. **`ArtworkCheck` case class** in `manufacturing.scala`:
   - Fields: `resolution`, `bleed`, `colorProfile` (all `CheckStatus`), `notes`
   - `ArtworkCheck.unchecked` factory for initial state
   - `isFullyPassed`, `hasIssues`, `hasWarnings` extension methods

4. **`ManufacturingOrder` enhanced** — added fields (with defaults for backward compatibility):
   - `priority: Priority` — order-level priority for workflow generation
   - `paymentStatus: PaymentStatus` — payment verification status
   - `artworkCheck: ArtworkCheck` — prepress file validation flags

5. **`ArtworkCheckSpec.scala`** — 15 tests covering:
   - Unchecked defaults
   - `isFullyPassed` (all passed, partial, unchecked)
   - `hasIssues` (resolution/bleed/colorProfile failures, no failures)
   - `hasWarnings` (warnings without failures, with failures, all passed)
   - `CheckStatus` and `PaymentStatus` extension display names and icons

6. **`ManufacturingViewModel` enhanced**:
   - New actions: `holdOrder`, `requestChanges`, `setOrderPriority`, `setOrderDeadline`, `setPaymentStatus`, `updateArtworkCheck`, `setApprovalNotes`
   - `approveOrder` now uses order-level `priority` (instead of workflow-level)
   - Sample data includes varied payment statuses (Confirmed/Pending) and artwork checks (Passed/Failed/Warning/NotChecked)

7. **`OrderApprovalView` enhanced**:
   - New table columns: Payment (status badge), Artwork (summary indicator)
   - Payment filter (Pending/Confirmed/Failed) added to filter bar
   - Side panel — Priority & Deadline section: clickable priority buttons (Rush/Normal/Low) with visual state
   - Side panel — Payment section: clickable payment status buttons with verification flow
   - Side panel — Artwork Review section: per-flag check buttons (Resolution/Bleed/Color Profile × NotChecked/Passed/Warning/Failed)
   - Side panel — Internal Notes section with approval notes display
   - Enhanced approval actions: Hold, Request Changes buttons alongside Approve/Reject
   - On Hold orders can be approved or sent back to PendingChanges

8. **CSS** — Added styles for:
   - Priority buttons (Rush=red, Normal=blue, Low=green active states)
   - Payment status buttons
   - Artwork check grid with flag buttons (color-coded by status)
   - Artwork notes display (yellow sidebar)
   - Approval notes section

---

## Phase 7 ✅ — Fulfilment Workflow

**Status: Complete**

### Implemented

1. **`PackagingType` enum** in `manufacturing.scala`:
   - Variants: `Box`, `Envelope`, `Roll`, `Tube`, `Custom`
   - `displayName` extension

2. **`FulfilmentStatus` enum** — `NotStarted`, `InProgress`, `Completed`

3. **Fulfilment data types** in `manufacturing.scala`:
   - `CollectedItem` — per-basket-item collection record (index, collected, verifiedBy)
   - `QualitySignOff` — QC sign-off (passed, signedBy, notes) with `empty` factory
   - `PackagingInfo` — packaging details (type, dimensions, weight) with `empty` factory
   - `DispatchInfo` — dispatch confirmation (dispatched, trackingNumber, timestamp, employee) with `empty` factory
   - `FulfilmentChecklist` — complete checklist combining all four steps

4. **`FulfilmentChecklist` extensions**:
   - `allItemsCollected`, `isQualityPassed`, `isPackaged`, `isDispatched` — step completion checks
   - `status` — derives `FulfilmentStatus` from step states
   - `completedStepsCount` / `totalStepsCount` — progress tracking (4 steps total)
   - `FulfilmentChecklist.create(itemCount)` — factory for new checklists

5. **`ManufacturingOrder` enhanced**:
   - `fulfilment: Option[FulfilmentChecklist]` field (defaults to None)
   - `isReadyForDispatch` extension — true when all workflows completed
   - `isDispatched` extension — true when fulfilment dispatch confirmed

6. **`FulfilmentChecklistSpec.scala`** — 17 tests covering:
   - `create`: correct item count, empty QC, empty packaging/dispatch
   - `allItemsCollected`: none/some/all collected
   - `status`: NotStarted/InProgress/Completed transitions
   - `completedStepsCount`: 0/1/2/4 (full completion)
   - `PackagingType` display names
   - `ManufacturingOrder.isReadyForDispatch` and `isReadyForDispatch` false

7. **`ManufacturingViewModel` enhanced**:
   - `completeStep` auto-creates fulfilment checklist when all workflows complete
   - `toggleItemCollected` — per-item collection toggle (with employee assignment)
   - `signOffQuality` — QC pass/fail with employee and notes
   - `setPackaging` — packaging type, dimensions, weight
   - `confirmDispatch` — dispatch confirmation with tracking number and timestamp

8. **`OrderProgressView` enhanced**:
   - Status column now shows "Ready for Dispatch" and "Dispatched" states
   - Fulfilment checklist section in side panel (appears when workflows complete)
   - Step 1 — Collect Items: per-item checkboxes with visual collection state
   - Step 2 — Quality Check: Pass QC button, shows notes when passed
   - Step 3 — Package: packaging type selection buttons (Box/Envelope/Roll/Tube/Custom)
   - Step 4 — Dispatch: Confirm Dispatch button (disabled until steps 1-3 complete), tracking display
   - Fulfilment progress bar showing 0/4 → 4/4 step completion

9. **CSS** — Added styles for:
   - Fulfilment section with blue top border
   - Collect items checklist with collected/uncollected states
   - QC sign-off display
   - Packaging type buttons and info display
   - Dispatch confirmation with disabled state hint

---

## Phase 8 ✅ — Analytics & Reporting

**Status: Complete**

### Implemented

1. **`AnalyticsService.scala`** — Pure analytics service in `mpbuilder.domain.service`:
   - `averageTimePerStation` — computes average step duration per station type from completed steps
   - `bottleneckStation` — identifies station with most Ready steps (queue depth)
   - `employeeThroughput` — counts completed steps per employee
   - `onTimeDeliveryRate` — fraction of completed orders that met their deadline
   - `computeSummary` — aggregates all metrics into `AnalyticsSummary`

2. **Analytics data types**:
   - `AnalyticsSummary` — total/completed/inProgress orders, avg time, on-time rate, bottleneck, station metrics, employee metrics
   - `StationMetric` — per-station: completed steps, avg time, queue depth, in progress
   - `EmployeeMetric` — per-employee: completed steps, stations worked

3. **`AnalyticsServiceSpec.scala`** — 13 tests covering:
   - `averageTimePerStation`: avg calculation, non-completed ignored, empty result
   - `bottleneckStation`: most ready steps, no ready steps
   - `employeeThroughput`: per-employee counts, unassigned ignored
   - `onTimeDeliveryRate`: no completed, no deadline, on-time, late, 50/50 mix
   - `computeSummary`: full summary computation with station and employee metrics

4. **`AnalyticsView.scala`** — Analytics dashboard view:
   - KPI cards row: Total Orders, Completed, In Progress, On-Time Rate, Avg Step Time
   - Bottleneck alert banner (amber, shown when queue depth > 1)
   - Station Performance table: station, completed, avg time, queue, in progress, load indicator
   - Employee Throughput table: name, completed steps (with visual bar), stations worked (chip list)
   - Load indicators: Idle/Light/Moderate/Heavy with color coding

5. **`ManufacturingRoute.Analytics`** — Added to sidebar navigation

6. **CSS** — Added styles for:
   - KPI cards grid with responsive layout
   - Bottleneck alert banner (amber/gold)
   - Analytics tables with station cells and bar indicators
   - Load indicator badges (idle/light/moderate/heavy color coding)

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
