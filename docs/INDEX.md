# Documentation Index

> Master table of contents for all project documentation. This is the entry point for finding analysis, specifications, guides, and knowledge base articles.

## 📋 Specifications

Core feature specifications — stable documents describing *what* the system does.

| Document | Description |
|----------|-------------|
| [features.md](features.md) | Comprehensive feature overview of the entire system |
| [pricing.md](pricing.md) | Pricing engine specification with 17 rule types and worked examples |
| [visual-product-types.md](visual-product-types.md) | Visual editor product types (calendars, photo books, wall pictures) and formats |
| [manufacturing-speed-pipeline.md](manufacturing-speed-pipeline.md) | Order completion time estimation — 8-stage pipeline specification |
| [new-product-specification.md](new-product-specification.md) | Specification for adding new product types to the system |
| [help-information.md](help-information.md) | Contextual help system specification (field-level `?` and `i` buttons) |

## 🔬 Analysis & Research

Research documents, gap analyses, and architecture decisions — context for *why* things are the way they are.

| Document | Description |
|----------|-------------|
| [analysis/printing-domain-analysis.md](analysis/printing-domain-analysis.md) | Comprehensive printing industry domain analysis (50+ product categories, materials, methods) |
| [analysis/domain-model-gap-analysis.md](analysis/domain-model-gap-analysis.md) | Comparison of domain model vs real-world printing requirements (8 priority gaps) |
| [analysis/sheet-based-pricing.md](analysis/sheet-based-pricing.md) | Technical design for sheet-based material pricing with nesting algorithm |
| [analysis/manufacturing-workflow-analysis.md](analysis/manufacturing-workflow-analysis.md) | Manufacturing workflow design and state management analysis |
| [analysis/express-manufacturing-analysis.md](analysis/express-manufacturing-analysis.md) | Express/fast manufacturing workflows and timeline analysis |
| [analysis/catalog-editor-architecture.md](analysis/catalog-editor-architecture.md) | Architecture decisions for Phase 9 catalog configuration UI |
| [analysis/ui-kit-review.md](analysis/ui-kit-review.md) | Review of ui-framework components and styling gap analysis |
| [analysis/roll-up.md](analysis/roll-up.md) | Roll-up/retractable banner product specification (2 components, area-based pricing) |
| [analysis/claude-skills-vs-claudemd.md](analysis/claude-skills-vs-claudemd.md) | Analysis of Claude Code skills vs CLAUDE.md — when to use each, decision matrix |

## 📐 Plans & Roadmaps

Implementation plans and future feature designs.

| Document | Description |
|----------|-------------|
| [../PLAN.md](../PLAN.md) | Project goals, completed phases, and roadmap |
| [manufacturing-implementation-plan.md](manufacturing-implementation-plan.md) | Manufacturing system phases 1–8 implementation plan |
| [customer-portal-plan.md](customer-portal-plan.md) | Customer-facing order tracking portal (5 use cases, 12 future features) |
| [customer-management-plan.md](customer-management-plan.md) | Customer account management feature plan |

## 🛠 Developer Guides

How-to guides for developers working with the codebase.

| Document | Description |
|----------|-------------|
| [ui-guide.md](ui-guide.md) | Build, run, and test instructions (Mill) |
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
| [changelog/README.md](changelog/README.md) | Changelog format, template, and log entries index |
| [changelog/2026-04-18-calendar-covers-binding-colors.md](changelog/2026-04-18-calendar-covers-binding-colors.md) | Calendar protective covers, binding color selection, and binding method rename |
| [changelog/2026-04-16-remove-sbt-from-docs-and-pipeline.md](changelog/2026-04-16-remove-sbt-from-docs-and-pipeline.md) | Session log for removing sbt from root docs and CI pipeline |
| [changelog/2026-04-15-price-preview-validate-button-layout.md](changelog/2026-04-15-price-preview-validate-button-layout.md) | Session log for moving validate action/per-item price into the price preview card |
