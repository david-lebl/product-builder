# Product Builder (material-builder)

Full-stack product configurator and price calculator for a printing business —
Scala 3 everywhere. Stage 1 covers the **product calculator** (configure a
product with live, itemized pricing) and **order submission** into a
new-orders queue awaiting approval.

Specification: [`docs/ideas/business-specification.md`](docs/ideas/business-specification.md)
· Sample data: [`docs/ideas/full-catalog-czk-price-list.md`](docs/ideas/full-catalog-czk-price-list.md)

## Architecture

| Module | Platform | What it is |
|---|---|---|
| `modules/shared` | JVM + JS (crossproject) | Domain model, compatibility rules, validation (error-accumulating), the full 11-step pricing engine, sample catalog + CZK pricelist, API DTOs |
| `modules/backend` | JVM | ZIO 2 + zio-http server, zio-quill (protoquill) Postgres persistence, Flyway migrations |
| `modules/frontend` | Scala.js | Laminar SPA — the configurator UI |
| `frontend/` | Node | Vite host (dev server + production bundling) |

The browser computes live prices locally with the **same shared code** the
server uses to re-price authoritatively on order submission — one source of
truth for catalog, rules, and pricing.

## Development

Prerequisites: JDK 17+, sbt, Node 18+, Docker.

```bash
docker compose up -d               # Postgres 16 on localhost:5433
sbt backend/run                    # API on :8081 (runs Flyway migrations on boot)

cd frontend
npm install
npm run dev                        # Vite on :5173, proxies /api → :8081
```

Open http://localhost:5173. The Vite plugin runs `sbt frontend/fastLinkJS`
itself; for a faster loop keep `sbt "~frontend/fastLinkJS"` running in a
separate terminal.

### Tests

```bash
sbt sharedJVM/test    # domain: pricing golden tests, validation, imposition
sbt sharedJS/test     # the same suite on Scala.js (Node)
sbt backend/test      # HTTP routes + Postgres round-trip (needs Docker for testcontainers)
```

The pricing engine is locked by golden tests against the worked examples in
both spec documents (business-specification §5.13, catalog price list §11).

### Production bundle

```bash
cd frontend && npm run build       # emits frontend/dist
cp -r dist/* ../modules/backend/src/main/resources/public/
sbt backend/run                    # serves the SPA + API from :8081
```

## API

| Endpoint | Description |
|---|---|
| `GET /api/catalog` | Catalog + presets + compatibility rules + pricelist (the client's single source of truth) |
| `POST /api/orders` | Validate + re-price + persist an order (status `Placed`); `422` returns **all** accumulated errors |
| `GET /api/orders?limit=n` | New-orders queue summaries |
| `GET /api/orders/:id` | Full order incl. configuration and price breakdown |

## Stage-1 conventions

Two things the spec leaves undefined are fixed in `Imposition` (one-line
changes, locked by golden tests): cuts per sheet = `(cols+1)+(rows+1)`
guillotine cuts, and booklet body sheets = `ceil(qty × pages / (copiesPerSheet × 2))`.

Deliberately out of scope for stage 1: manufacturing workflow, customer
accounts, discount codes, visual editor, showcase gallery, delivery/payment
checkout steps, customer-specific pricing, dynamic queue-based rush pricing,
and catalog administration (the catalog is in-code sample data; only orders
live in Postgres).
