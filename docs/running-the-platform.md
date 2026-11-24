# Running the Platform (API Server, PostgreSQL & Frontend)

How to run the backend API server in its two storage modes and how to point the Scala.js frontends at it.
For one-command local development scripts covering all combinations, see [HOW_TO_RUN.md](../HOW_TO_RUN.md).
For the architecture behind it, see [backend-persistence-and-http-api.md](backend-persistence-and-http-api.md).

## 1. API server without a database (quickest)

```bash
./mill backend.app.run --storage=memory
```

This serves the in-memory sample data on port 8080. Data is lost when the server stops.

- `http://localhost:8080/docs` opens Swagger UI with all endpoints.
- `http://localhost:8080/healthz` is the health check (`/readyz` is the readiness check).
- Admin endpoints need a token. Call `POST /api/v1/admin/auth/login` with
  `{"email":"admin@printshop.cz","password":"staff1234"}`, then click **Authorize** in Swagger
  and paste the returned `session.token`.

### Demo logins

| Role | E-mail | Password |
|---|---|---|
| Admin (+ Manager, Operator) | `admin@printshop.cz` | `staff1234` |
| Operations Manager | `manager@printshop.cz` | `staff1234` |
| Prepress | `prepress@printshop.cz` | `staff1234` |
| Customer (B2B) | `jan@printshoppro.cz` | `demo1234` |

## 2. API server with PostgreSQL

Docker must be running.

```bash
docker compose up -d postgres
DATABASE_URL=postgresql://localhost:5432/printcraft ./mill backend.app.run --storage=postgres
```

- On first boot, the migration runner creates the tables (one schema per bounded context) and the
  seeder loads the sample catalog, pricing, customers, demo orders, demo artwork and the demo logins.
  Seeding never runs twice.
- Databases created before the Kyo rewrite (Flyway history, jsoniter JSON) are not compatible:
  recreate them with `docker compose down -v`.
- Data survives restarts. Uploaded artwork files are stored in `./data/blobs` (git-ignored).
- To start from a clean database: `docker compose down -v`.
- Passwordless sign-in codes are not e-mailed yet. They are printed in the server log as
  `[DEV OTP DELIVERY] … code=123456`.

## 3. Frontend talking to the server

The SPAs only switch to HTTP mode when the page defines `window.__API_BASE_URL__`.
Without it they keep running fully in the browser on the bundled sample runtime, exactly as before.

With the server from step 1 or 2 running, in a second terminal:

```bash
./mill ui.fastLinkJS
mkdir -p _dev_api
cp out/ui/fastLinkJS.dest/main.js _dev_api/
sed 's|<script src="main.js"></script>|<script>window.__API_BASE_URL__ = "http://localhost:8080";</script>\n  <script src="main.js"></script>|' \
  modules/ui/src/main/resources/index.html > _dev_api/index.html
cd _dev_api && python3 -m http.server 3000
```

Open `http://localhost:3000` for the storefront, or `http://localhost:3000/#/admin` for the backoffice.
Use a port other than 8080, because the API is on 8080. CORS allows any origin by default.

- **Standalone backoffice SPA:** same recipe with `./mill ui-admin.fastLinkJS`,
  `out/ui-admin/fastLinkJS.dest/main.js` and `modules/ui-admin/src/main/resources/index.html`.
- **Calculator widget:** pass the API URL as a mount option, or set `window.__API_BASE_URL__` on the page:

  ```js
  MPCalculator.mount("#calculator-root", { lang: "cs", apiBaseUrl: "http://localhost:8080" })
  ```

## 4. Server options

```bash
./mill backend.app.run --help
```

| Setting | Flag | Environment variable | Default |
|---|---|---|---|
| Storage mode | `--storage=memory\|postgres` | `STORAGE` | `memory` |
| Bind address / port | `--host`, `--port` | `HOST`, `PORT` | `0.0.0.0`, `8080` |
| Allowed CORS origin | `--cors=http://localhost:3000` (`*` or exactly one origin) | `CORS_ORIGINS` | `*` |
| Blob directory | `--blob-dir=/tmp/blobs` | `BLOB_DIR` | `./data/blobs` |
| Blob URL prefix | `--blob-public-base-url` | `BLOB_PUBLIC_BASE_URL` | `/api/v1/blobs` |
| Skip sample-data seeding | `--no-seed` | — | seeding on |
| Database | — | `DATABASE_URL` (`postgresql://host:port/db`), `DATABASE_USER`, `DATABASE_PASSWORD`, `DATABASE_POOL_SIZE` | user and password `printcraft` |

Flags take precedence over environment variables.

## 5. Tests

```bash
./mill backend.app.test                  # live HTTP end-to-end test, memory and PostgreSQL (needs Docker)
./mill 'backend.ordering.02-o-db.test'   # in-memory vs PostgreSQL behaviour checks for one context
./mill __.test                           # everything
```

The `02-o-db`, `platform` and `app` test suites start one throwaway PostgreSQL 17 container per test JVM
via kyo-pod (Docker or Podman must be running) and give every test its own fresh database. To use an
existing server instead (CI, or a local Docker VM that cannot forward published ports), point
`TEST_DATABASE_URL` at it, e.g. `TEST_DATABASE_URL=postgresql://postgres:secret@127.0.0.1:5432/postgres ./mill __.test`
(the role needs `CREATEDB`).
