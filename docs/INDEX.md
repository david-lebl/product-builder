# Documentation Index

> Master table of contents for all project documentation. This is the entry point for finding analysis, specifications, guides, and knowledge base articles.

## 📋 Specifications

Core feature specifications — stable documents describing *what* the system does.

| Document | Description |
|----------|-------------|
| [features.md](features.md) | Comprehensive feature overview of the entire system |
| [pricing.md](pricing.md) | Pricing engine specification with 24 rule types and worked examples |
| [visual-product-types.md](visual-product-types.md) | Legacy (pre-Kyo PoC) visual editor product types — superseded by the Design Studio architecture doc |
| [manufacturing-speed-pipeline.md](manufacturing-speed-pipeline.md) | Order completion time estimation — 8-stage pipeline specification |
| [new-product-specification.md](new-product-specification.md) | Specification for adding new product types to the system |
| [help-information.md](help-information.md) | Contextual help system specification (field-level `?` and `i` buttons) |
| [email-order-option.md](email-order-option.md) | Email order option — pre-filled modal popup for requesting orders via email |
| [standalone-calculator.md](standalone-calculator.md) | Embeddable price calculator widget — scope, e-mail ordering, indicative (non-promised) lead times, and the two embedding modes |

## 🔬 Analysis & Research

Research documents, gap analyses, and architecture decisions — context for *why* things are the way they are.

| Document | Description |
|----------|-------------|
| [analysis/visual-editor-architecture.md](analysis/visual-editor-architecture.md) | Design Studio (visual editor): document model in mm/pt, shared SVG renderer, local-first IndexedDB storage, server sync & asset upload design, print-PDF pipeline, iframe embedding & bundle-size measurements |
| [analysis/ci-optimization-plan.md](analysis/ci-optimization-plan.md) | GitHub Actions / Mill CI review: fixed issues (CI skipped `develop` PRs, broken preview cleanup, custom Mill wrapper) and backlog (scalafmt enforcement, scalafix, staged jobs, `out/` caching, Scala Steward) |
| [analysis/kyo-ecosystem-findings.md](analysis/kyo-ecosystem-findings.md) | Kyo 1.0.0-RC7 bugs, gaps & footguns found during the backend rewrite (with reproductions) — input for upstream issues |
| [analysis/printing-domain-analysis.md](analysis/printing-domain-analysis.md) | Comprehensive printing industry domain analysis (50+ product categories, materials, methods) |
| [analysis/domain-model-gap-analysis.md](analysis/domain-model-gap-analysis.md) | Comparison of domain model vs real-world printing requirements (8 priority gaps) |
| [analysis/sheet-based-pricing.md](analysis/sheet-based-pricing.md) | Technical design for sheet-based material pricing with nesting algorithm |
| [analysis/manufacturing-workflow-analysis.md](analysis/manufacturing-workflow-analysis.md) | Manufacturing workflow design and state management analysis |
| [analysis/express-manufacturing-analysis.md](analysis/express-manufacturing-analysis.md) | Express/fast manufacturing workflows and timeline analysis |
| [analysis/catalog-editor-architecture.md](analysis/catalog-editor-architecture.md) | Architecture decisions for Phase 9 catalog configuration UI |
| [analysis/ui-kit-review.md](analysis/ui-kit-review.md) | Review of ui-framework components and styling gap analysis |
| [analysis/roll-up.md](analysis/roll-up.md) | Roll-up/retractable banner product specification (2 components, area-based pricing) |
| [analysis/claude-skills-vs-claudemd.md](analysis/claude-skills-vs-claudemd.md) | Analysis of Claude Code skills vs CLAUDE.md — when to use each, decision matrix |
| [analysis/ink-configuration-pricing.md](analysis/ink-configuration-pricing.md) | Analysis of the old `InkConfigurationFactor` model, its structural problems, and the implemented additive `InkConfigurationSheetPrice` + `InkConfigurationAreaPrice` rules |

## 📐 Plans & Roadmaps

Implementation plans and future feature designs.

| Document | Description |
|----------|-------------|
| [../PLAN.md](../PLAN.md) | Goals, guiding decisions, and phase status overview (details in the platform roadmap) |
| [manufacturing-implementation-plan.md](manufacturing-implementation-plan.md) | Manufacturing system phases 1–8 implementation plan |
| [customer-portal-plan.md](customer-portal-plan.md) | Customer-facing order tracking portal (5 use cases, 12 future features) |
| [customer-management-plan.md](customer-management-plan.md) | Customer account management feature plan |
| [../docs_roadmap/platform_roadmap.md](../docs_roadmap/platform_roadmap.md) | Platform roadmap — Phases 1–6 (backoffice, persistence, HTTP API, artwork & editor) with implementation status |

## 🛠 Developer Guides

How-to guides for developers working with the codebase.

| Document | Description |
|----------|-------------|
| [ui-guide.md](ui-guide.md) | Build, run, and test instructions (Mill) |
| [../HOW_TO_RUN.md](../HOW_TO_RUN.md) | Local development script (`scripts/dev.sh`) for frontend, backend, and PostgreSQL combinations |
| [ui-design-system.md](ui-design-system.md) | UI design system — theme tokens, light/dark/system mode, icons, shared controls, responsive rules |
| [running-the-platform.md](running-the-platform.md) | How to run the API server (`--storage=memory\|postgres`), demo logins, pointing the SPAs/calculator at it, server options, and tests |
| [backend-persistence-and-http-api.md](backend-persistence-and-http-api.md) | PostgreSQL persistence (kyo-sql, schema per context, migration runner), kyo-http API (OpenAPI & Swagger), `--storage=memory\|postgres`, HTTP client wiring (`window.__API_BASE_URL__`), artwork `BlobStorage`, and Kyo coding conventions |
| [adding-new-product-guide.md](adding-new-product-guide.md) | Step-by-step guide for adding new products to the catalog |
| [../CLAUDE.md](../CLAUDE.md) | AI assistant guidance — architecture, build commands, coding conventions |

## 🏗 Refactoring

Technical debt and improvement plans.

| Document | Description |
|----------|-------------|
| [refactoring/css-architecture.md](refactoring/css-architecture.md) | CSS modularization plan — split monolithic 4,875-line CSS into 12 modules |

## 🔧 Troubleshooting

| Document | Description |
|----------|-------------|
| [troubleshooting.md](troubleshooting.md) | Known issues, agent session problems, and solutions |

## 📝 Work Changelog

Session-by-session work logs documenting what was done, decisions made, and issues encountered.

| Document | Description |
|----------|-------------|
| [changelog/2026-11-26-backoffice-margin-simulator-fix.md](changelog/2026-11-26-backoffice-margin-simulator-fix.md) | Bugfix — Backoffice Margin Simulator selects no longer reset (option `selected`), digit-filtered quantity, inputs applied on run |
| [changelog/2026-11-25-ci-workflow-review.md](changelog/2026-11-25-ci-workflow-review.md) | Session log for the CI workflow fixes, official Mill launcher, caching and report-only scalafmt check |
| [changelog/2026-11-25-scalajs-link-oom.md](changelog/2026-11-25-scalajs-link-oom.md) | Session log for the Scala.js link OutOfMemoryError fix (Mill daemon `-Xmx6g`) |
| [changelog/2026-11-25-fulllinkjs-oom-deploy.md](changelog/2026-11-25-fulllinkjs-oom-deploy.md) | Session log for the deploy `fullLinkJS` OutOfMemoryError fix (link bundles one at a time) |
| [changelog/README.md](changelog/README.md) | Changelog format, template, and log entries index |
| [changelog/2026-11-25-mobile-configurator-sticky-dock.md](changelog/2026-11-25-mobile-configurator-sticky-dock.md) | Feature — sticky bottom price dock on mobile/tablet with expandable price-detail sheet, add-to-cart feedback and bottom-sheet cart |
| [changelog/2026-11-25-configurator-material-combobox.md](changelog/2026-11-25-configurator-material-combobox.md) | Feature — searchable rich material combo box (name/surface/gsm ranges, EN+CS) with weight-variant pills in the configurator |
| [changelog/2026-11-24-ui-visual-ux-modernization.md](changelog/2026-11-24-ui-visual-ux-modernization.md) | UI modernization — CSS-variable theme with dark mode, icon set, shared controls, storefront & backoffice redesign, layout bug fixes |
| [changelog/2026-11-24-kyo-backend-rewrite.md](changelog/2026-11-24-kyo-backend-rewrite.md) | Refactoring — whole backend on Kyo: effects, kyo-schema JSON, kyo-sql persistence, kyo-http API/server/client, kyo-test & kyo-pod |
| [changelog/2026-11-23-quick-order-server-pricing.md](changelog/2026-11-23-quick-order-server-pricing.md) | Bugfix — quick-order checkout sends a configuration (`QuickOrderItem`) and is re-priced server-side instead of trusting a client price |
| [changelog/2026-11-23-phase4-http-api.md](changelog/2026-11-23-phase4-http-api.md) | Roadmap Phase 4 — per-BFF Tapir HTTP API, ZIO HTTP server (`--storage=memory\|postgres`), HTTP clients & frontend mode switch |
| [changelog/2026-11-23-phase3-postgres-persistence.md](changelog/2026-11-23-phase3-postgres-persistence.md) | Roadmap Phase 3 + 5.1 storage — PostgreSQL persistence (Magnum + Flyway), DB seeder, contract tests, artwork BlobStorage |
| [changelog/2026-05-03-printing-method-setup-fee.md](changelog/2026-05-03-printing-method-setup-fee.md) | Add PrintingMethodSetupFee pricing rule; update all three sample pricelists and affected tests |
| [changelog/2026-05-03-dtg-sublimation-uv-mug-pricing-fix.md](changelog/2026-05-03-dtg-sublimation-uv-mug-pricing-fix.md) | Fix missing ink config prices for DTG, sublimation, and UV flatbed on mugs in pricelistCzkSheet |
| [changelog/2026-05-03-ink-config-single-sided-constraint.md](changelog/2026-05-03-ink-config-single-sided-constraint.md) | IsSingleSided predicate, large-format inkjet single-sided constraint, pricelistCzkSheet solvent/Epson pricing fix, UI ink config selector filtering |
| [changelog/2026-04-28-print-method-pricing-gaps.md](changelog/2026-04-28-print-method-pricing-gaps.md) | Print method pricing gap analysis, new solvent/Epson 8-color methods, sticker category update; sticker material/method constraints and silent ink-cost bug fix |
| [changelog/2026-04-28-email-order-option.md](changelog/2026-04-28-email-order-option.md) | Add "Order via Email" button and pre-filled modal popup to the product builder UI |
| [changelog/2026-04-26-binding-page-constraints-refactor.md](changelog/2026-04-26-binding-page-constraints-refactor.md) | Refactor booklet page rules into TechnologyConstraints; add weight-based saddle stitch tiers and binding method max-page limits |
| [changelog/2026-04-25-fix-lamination-pricing-per-sheet-area.md](changelog/2026-04-25-fix-lamination-pricing-per-sheet-area.md) | Fix finish surcharges: sheet-priced → per press sheet; area-priced → per m² |
| [changelog/2026-04-22-update-minimum-order-price-200-czk.md](changelog/2026-04-22-update-minimum-order-price-200-czk.md) | Update minimum order price from 500 CZK to 200 CZK in sample pricelists |
| [changelog/2026-04-22-ink-configuration-pricing-analysis.md](changelog/2026-04-22-ink-configuration-pricing-analysis.md) | Analysis document for ink configuration pricing — additive per-press model proposal |
| [changelog/2026-04-21-fix-manufacturing-ui-and-pricelist-table.md](changelog/2026-04-21-fix-manufacturing-ui-and-pricelist-table.md) | Fix Manufacturing UI blank page and Pricelist empty table (regressions from PR #133) |
| [changelog/2026-04-21-scoring-stepper-and-pricing-fix.md](changelog/2026-04-21-scoring-stepper-and-pricing-fix.md) | Scoring: replace radio buttons with stepper input; fix MissingScoringPrice error in pricelistCzkSheet |
| [changelog/2026-04-20-scoring-creasing-feature.md](changelog/2026-04-20-scoring-creasing-feature.md) | Session log for Creasing/Scoring feature (ScoringParams, ScoringCountSurcharge, ScoringSetupFee, max-crease caps) |
| [changelog/2026-04-20-banner-product-overhaul.md](changelog/2026-04-20-banner-product-overhaul.md) | Session log for Banner Product Overhaul (PVC 510g, area tiers, gum rope, max-dimension rule) |
| [changelog/2026-04-16-remove-sbt-from-docs-and-pipeline.md](changelog/2026-04-16-remove-sbt-from-docs-and-pipeline.md) | Session log for removing sbt from root docs and CI pipeline |
| [changelog/2026-04-15-price-preview-validate-button-layout.md](changelog/2026-04-15-price-preview-validate-button-layout.md) | Session log for moving validate action/per-item price into the price preview card |
