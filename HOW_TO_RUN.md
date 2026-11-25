# Local Development Guide

This guide covers the local development script [`scripts/dev.sh`](scripts/dev.sh) for building and running every supported combination of the PrintCraft Studio frontend, backend API, and PostgreSQL database.

## Prerequisites

- **JDK 25** (matching [`.sdkmanrc`](.sdkmanrc)) — `scripts/dev.sh` automatically detects JDK 25 from SDKMAN or `java_home` if your default shell uses another JDK
- **Mill 1.1.3** (via the bundled [`./mill`](mill) wrapper)
- **Python 3** (used to serve static frontend bundles and inject `window.__API_BASE_URL__`)
- **Docker** (or Podman with Compose) — only required for PostgreSQL modes (`fullstack`, `backend-db`, `db-*`) and DB test suites

Make sure the script is executable (already set in git):

```bash
chmod +x scripts/dev.sh
```

## Available Combinations

| Command | Frontend | Backend API | PostgreSQL | Use Case |
|---|---|---|---|---|
| `./scripts/dev.sh frontend` | Yes (static) | No | No | Fastest UI work; runs on bundled in-memory `SampleShowcaseRuntime` |
| `./scripts/dev.sh frontend-api` | Yes (HTTP) | Yes (`memory`) | No | Test HTTP client/server integration without Docker |
| `./scripts/dev.sh fullstack` | Yes (HTTP) | Yes (`postgres`) | Yes | Full production-like local stack with persistent DB |
| `./scripts/dev.sh backend` | No | Yes (`memory`) | No | API / Swagger UI development without DB |
| `./scripts/dev.sh backend-db` | No | Yes (`postgres`) | Yes | API / Swagger UI development with persistent DB |
| `./scripts/dev.sh db-up` | No | No | Yes | Start PostgreSQL container only (e.g. for running tests or IDE) |
| `./scripts/dev.sh db-down` | No | No | Stopped | Stop PostgreSQL container (preserves volume) |
| `./scripts/dev.sh db-reset` | No | No | Fresh | Wipe PostgreSQL volume and start a clean database |
| `./scripts/dev.sh build` | Optional | Optional | No | Build frontend bundles and compile backend without starting servers |
| `./scripts/dev.sh test` | — | — | — | Run `./mill __.test` or a specific Mill test target |

## Quick Start Examples

### 1. Frontend Alone (Static In-Memory Mode)

Builds all four Scala.js apps (`ui`, `ui-admin`, `ui-calculator`, `ui-editor`) with `fastLinkJS`, assembles them into `_dev/` (git-ignored), and serves them on port `3000`:

```bash
./scripts/dev.sh frontend
```

Open:
- Storefront: `http://localhost:3000/`
- Unified Staff Backoffice: `http://localhost:3000/#/admin`
- Standalone Staff Backoffice: `http://localhost:3000/admin/`
- Embeddable Price Calculator: `http://localhost:3000/calculator/`
- Design Studio (visual editor; the storefront's "Design online" opens it in an iframe): `http://localhost:3000/editor/`

To build and serve only a single app (faster incremental linking):

```bash
./scripts/dev.sh frontend --app=ui
./scripts/dev.sh frontend --app=admin --port=3001
./scripts/dev.sh frontend --app=calculator
```

To build without starting the HTTP server:

```bash
./scripts/dev.sh frontend --build-only
```

### 2. Frontend + Backend (In-Memory, No Database)

Starts the Kyo HTTP backend with `--storage=memory` on port `8080`, waits for `/healthz`, injects `window.__API_BASE_URL__ = "http://localhost:8080"` into the assembled `_dev/` HTML files, and serves the frontend on port `3000`:

```bash
./scripts/dev.sh frontend-api
```

Press `Ctrl+C` to stop both the frontend server and the backend server cleanly.

### 3. Frontend + Backend + PostgreSQL (Full Stack)

Starts PostgreSQL via `docker compose up -d postgres`, waits for `pg_isready`, starts the backend with `--storage=postgres` (running schema migrations and sample-data seeding on first boot), and serves the frontend connected to the API:

```bash
./scripts/dev.sh fullstack
```

Press `Ctrl+C` to stop the frontend and backend servers (PostgreSQL keeps running in Docker; run `./scripts/dev.sh db-down` to stop it or `./scripts/dev.sh db-reset` to wipe the volume).

### 4. Backend Alone

```bash
# In-memory storage (Swagger UI at http://localhost:8080/docs)
./scripts/dev.sh backend

# PostgreSQL storage
./scripts/dev.sh backend-db
```

## Options Reference

| Flag | Environment Variable | Default | Description |
|---|---|---|---|
| `--app=<target>` | `APP` | `all` | Frontend target(s): `all`, `ui` (includes the editor it embeds), `admin`, `calculator`, `editor` |
| `--port=<port>` | `FRONTEND_PORT` | `3000` | Port for the static frontend HTTP server |
| `--api-port=<port>` | `API_PORT` | `8080` | Port for the backend API server |
| `--prod`, `--full` | `LINK_MODE=full` | `fast` | Use Scala.js `fullLinkJS` instead of `fastLinkJS` |
| `--build-only` | — | `false` | Build & assemble `_dev/` without starting the frontend server |
| `--no-seed` | — | `false` | Skip automatic sample-data seeding on backend boot |
| `--db-url=<url>` | `DATABASE_URL` | `postgresql://localhost:5432/printcraft` | PostgreSQL connection URL |
| `-h`, `--help` | — | — | Print usage information |

## Demo Logins

| Role | Email | Password |
|---|---|---|
| Admin (+ Manager, Operator) | `admin@printshop.cz` | `staff1234` |
| Operations Manager | `manager@printshop.cz` | `staff1234` |
| Prepress | `prepress@printshop.cz` | `staff1234` |
| Customer (B2B) | `jan@printshoppro.cz` | `demo1234` |

## See Also

- [docs/running-the-platform.md](docs/running-the-platform.md) — Manual Mill commands, server flags, and test container details
- [docs/backend-persistence-and-http-api.md](docs/backend-persistence-and-http-api.md) — Persistence, migrations, and HTTP API architecture
- [docs/ui-guide.md](docs/ui-guide.md) — Frontend build and test details
