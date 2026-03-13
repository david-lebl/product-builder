# Catalog Editor — Architecture Decision Analysis

## Context

Phase 9 adds a Catalog Configuration UI for creating/editing product catalogs, compatibility rules, and pricelists with JSON export/import. This document records the key architecture decisions.

---

## Decision 1: Standalone view vs. integrated in ManufacturingApp

### Options considered

**A) Add as a sub-route inside ManufacturingApp**
- Pros: Single internal-system entry point; shared dark sidebar; fewer top-level routes
- Cons: The manufacturing app is focused on order processing and production workflows — adding catalog configuration dilutes that responsibility. The ManufacturingRoute enum and ManufacturingViewModel already have a clear bounded context (orders, workflows, stations, employees, machines).

**B) Keep as an independent top-level view (chosen)**
- Pros: Clean separation of concerns. Catalog configuration is a setup-time activity (done once, edited infrequently), while manufacturing is a daily operational view. The catalog editor has its own state (CatalogEditorViewModel) that is independent of manufacturing state.
- Cons: Another top-level route in the navigation bar.

### Decision

**Option B — independent view with ManufacturingApp-style layout.** The catalog editor uses the same visual language as ManufacturingApp (dark sidebar, content area) for consistency, but remains a separate route. This fits the customer management plan: Phase 9 (catalog config) is independent/parallel with all other phases, while Phases 6-8 (customer management UI) naturally belong in the manufacturing/operations context.

If in the future we add a broader "Admin" or "Settings" app shell, both the catalog editor and manufacturing could become sub-routes of that shell, but for now keeping them separate is cleaner.

---

## Decision 2: FormComponents location — UI Framework module

### Rationale

The `FormComponents` object (`enumSelect`, `enumCheckboxSet`, `idCheckboxSet`, `localizedStringEditor`, `moneyField`) is domain-agnostic — it works with any Scala 3 enum or opaque type ID. It naturally belongs in the `ui-framework` module alongside `TextField`, `SelectField`, and `SplitTableView`, rather than in the `catalog` package.

Moving it to `mpbuilder.uikit.form.FormComponents` makes it available to all UI modules (calendar, manufacturing, catalog, future customer management UI) without cross-dependencies.

---

## Decision 3: SplitTableView for entity editors

### Rationale

The entity editors (Categories, Materials, Finishes, Printing Methods, Rules, Pricelist) follow a list + detail pattern that maps well to `SplitTableView`:
- **Table**: Entity list with sortable columns
- **Side panel**: Edit form for the selected entity (create/edit)

This replaces the custom `catalog-entity-list` + `catalog-edit-form` pattern with the established manufacturing UI pattern, giving search, sorting, and a consistent split-panel layout.

The side panel for catalog editors is wider than for manufacturing views (480px vs 360px) because the edit forms contain more fields (localized strings, enum checkboxes, component templates). This is achieved via a CSS modifier class `.catalog-editor-app .split-table-content--with-panel`.

---

## Decision 4: Default state — sample catalog + CZK pricelist

### Rationale

Starting with an empty catalog provides a poor first-impression and requires users to manually "Load Sample Data" before they can explore the editor. Loading the sample catalog with CZK pricelist (the primary business currency) by default provides an immediately useful starting state, while the Export/Import view still allows resetting to an empty catalog or importing custom data.

---

## Summary

| Decision | Choice | Rationale |
|----------|--------|-----------|
| Placement | Independent top-level view | Clean separation; catalog config is setup-time, not operational |
| Layout style | ManufacturingApp pattern (dark sidebar) | Visual consistency |
| FormComponents | Moved to `ui-framework` module | Reusable across all UI modules |
| Entity editors | SplitTableView with wider panel | Consistent pattern, search/sort for free |
| Default state | Sample catalog + CZK pricelist loaded | Better first impression |
