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
