# 2026-04-16 — Waypoint URL Routing Migration

**PR:** copilot/migrate-to-waypoint-url-routing
**Author:** agent
**Type:** feature

## Summary

Migrated the application from an in-memory routing approach (using `Var[AppRoute]`) to URL-based routing using the Waypoint library, enabling browser history support and deep linking.

## Changes Made

**Created:**
- `modules/ui/src/main/scala/mpbuilder/ui/Router.scala` — New Waypoint router configuration with all route definitions

**Modified:**
- `build.mill` — Added Waypoint 10.0.0-M1 dependency
- `build.sbt` — Added Waypoint 10.0.0-M1 dependency
- `modules/ui/src/main/scala/mpbuilder/ui/AppRouter.scala` — Updated to use Waypoint router instead of in-memory Var
- `modules/ui/src/main/scala/mpbuilder/ui/Main.scala` — Initialize router on startup
- `modules/ui/src/main/scala/mpbuilder/ui/manufacturing/ManufacturingApp.scala` — Use Router for sub-navigation
- `modules/ui/src/main/scala/mpbuilder/ui/catalog/CatalogEditorApp.scala` — Use Router for sub-navigation
- `modules/ui/src/main/scala/mpbuilder/ui/customers/CustomerManagementApp.scala` — Use Router for sub-navigation
- `modules/ui/src/main/scala/mpbuilder/ui/manufacturing/views/DashboardView.scala` — Use Router.pushState for navigation

## Decisions & Rationale

- **Waypoint 10.0.0-M1** — Chosen because it's compatible with Laminar 17.2.0 (the project's existing version) and provides browser History API integration out of the box.

- **Page hierarchy with sealed traits** — Used sealed trait hierarchy (`Page`, `Page.ManufacturingPage`, `Page.CatalogPage`, `Page.CustomerPage`) to enable pattern matching on page groups for navigation highlighting.

- **Anchor elements with href** — Replaced `button` elements with `a` elements for navigation links, providing SEO benefits and allowing middle-click/right-click behavior.

- **Legacy AppRoute compatibility** — Kept the `AppRoute` sealed trait for backwards compatibility with existing code that uses `AppRouter.navigateTo()`.

- **`endOfSegments` in URL patterns** — Waypoint 10.x requires explicit `endOfSegments` at the end of route patterns for exact matching.

## Issues Encountered

- **Build tool availability** — The environment had SSL certificate issues with Mill. Solved by installing and using sbt instead.

- **Waypoint API changes in v10** — The Router class API changed from using `new Router(...)` with implicit parameters to `object Router extends Router[Page](...)`. Adjusted implementation accordingly.

- **Visual editor optional artwork ID** — Required two separate routes (one with segment parameter, one without) since Waypoint doesn't support `Option[String]` segments directly.

## URL Structure

| URL | Page |
|-----|------|
| `/catalog` | Product catalog |
| `/builder` | Product builder |
| `/editor` | Visual editor (no artwork) |
| `/editor/{artworkId}` | Visual editor with artwork |
| `/checkout` | Checkout |
| `/product/{categoryId}` | Product detail |
| `/manufacturing` | Manufacturing dashboard |
| `/manufacturing/station-queue` | Station queue |
| `/manufacturing/order-approval` | Order approval |
| `/manufacturing/order-progress` | Order progress |
| `/manufacturing/employees` | Employees |
| `/manufacturing/machines` | Machines |
| `/manufacturing/analytics` | Analytics |
| `/manufacturing/settings` | Settings |
| `/catalog-editor/categories` | Category editor |
| `/catalog-editor/materials` | Material editor |
| `/catalog-editor/finishes` | Finish editor |
| `/catalog-editor/printing-methods` | Printing method editor |
| `/catalog-editor/rules` | Rules editor |
| `/catalog-editor/pricelist` | Pricelist editor |
| `/catalog-editor/export` | Export/Import |
| `/customers` | Customer list |
| `/customers/pricing` | Customer pricing |
| `/customers/discount-codes` | Discount codes |
| `/my-orders` | Customer portal |
| `/order-history` | Order history |

## Follow-up Items

- [ ] Update existing navigation calls throughout the codebase to use `Router.pushState(Page.X)` instead of `AppRouter.navigateTo(AppRoute.X)` for consistency
- [ ] Add 404 handling for unmatched URLs
- [ ] Consider adding query parameter support for filters (e.g., `/catalog?category=banners`)
- [ ] Test browser back/forward button behavior thoroughly
- [ ] Consider adding route guards for protected pages (e.g., customer portal requires login)
