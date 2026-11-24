#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
cd "$ROOT_DIR"

# Defaults (override via flags or environment variables)
FRONTEND_PORT="${FRONTEND_PORT:-3000}"
API_PORT="${API_PORT:-8080}"
APP="${APP:-all}"                 # all | ui | admin | calculator
LINK_MODE="${LINK_MODE:-fast}"    # fast | full
BUILD_ONLY=false
NO_SEED=false
DATABASE_URL="${DATABASE_URL:-postgresql://localhost:5432/printcraft}"
DEV_DIR="$ROOT_DIR/_dev"

BACKEND_PID=""
FRONTEND_PID=""

usage() {
  cat <<'EOF'
PrintCraft Studio — Local Development Runner

Usage:
  ./scripts/dev.sh <command> [options]

Commands:
  frontend        Build & serve frontend only (static in-memory mode, no backend)
  frontend-api    Build & serve frontend + backend API (in-memory storage, no DB)
  fullstack       Start PostgreSQL + backend API + build & serve frontend
  backend         Run backend API server only (in-memory storage, no DB)
  backend-db      Start PostgreSQL + run backend API server
  db-up           Start PostgreSQL via Docker Compose and wait until ready
  db-down         Stop PostgreSQL container (keeps data volume)
  db-reset        Destroy PostgreSQL volume and start a fresh database
  build           Compile/link targets without starting servers
  test [args...]  Run Mill tests (defaults to __.test; pass Mill target(s) to scope)

Options:
  --app=<target>      Which frontend(s) to build/serve: all | ui | admin | calculator (default: all)
  --port=<port>       Frontend HTTP server port (default: 3000, env: FRONTEND_PORT)
  --api-port=<port>   Backend API server port (default: 8080, env: API_PORT)
  --prod, --full      Use Scala.js fullLinkJS instead of fastLinkJS
  --build-only        Build & assemble _dev/ without starting the frontend server
  --no-seed           Pass --no-seed to backend.app.run
  --db-url=<url>      PostgreSQL URL (default: postgresql://localhost:5432/printcraft)
  -h, --help          Show this help message

Examples:
  ./scripts/dev.sh frontend
  ./scripts/dev.sh frontend --app=ui --port=4000
  ./scripts/dev.sh frontend-api
  ./scripts/dev.sh fullstack
  ./scripts/dev.sh fullstack --app=admin --prod
  ./scripts/dev.sh backend-db --no-seed
  ./scripts/dev.sh test backend.sample.jvm.test
EOF
}

log() {
  printf '\033[1;34m[dev]\033[0m %s\n' "$*"
}

warn() {
  printf '\033[1;33m[dev]\033[0m %s\n' "$*" >&2
}

err() {
  printf '\033[1;31m[dev]\033[0m %s\n' "$*" >&2
  exit 1
}

require_cmd() {
  command -v "$1" >/dev/null 2>&1 || err "Required command '$1' not found in PATH."
}

ensure_java25() {
  local current_ver=""
  if command -v java >/dev/null 2>&1; then
    current_ver="$(java -version 2>&1 | head -n 1 || true)"
  fi
  if [[ "$current_ver" == *"\"25."* || "$current_ver" == *"\"25\""* ]]; then
    return 0
  fi

  local sdkman_dir="${SDKMAN_DIR:-$HOME/.sdkman}"
  local candidate=""
  if [[ -f "$ROOT_DIR/.sdkmanrc" ]]; then
    local rc_ver
    rc_ver="$(awk -F= '/^java=/{print $2}' "$ROOT_DIR/.sdkmanrc" | tr -d '[:space:]')"
    if [[ -n "$rc_ver" && -d "$sdkman_dir/candidates/java/$rc_ver" ]]; then
      candidate="$sdkman_dir/candidates/java/$rc_ver"
    fi
  fi
  if [[ -z "$candidate" ]]; then
    for d in "$sdkman_dir"/candidates/java/25*; do
      if [[ -d "$d" ]]; then
        candidate="$d"
        break
      fi
    done
  fi
  if [[ -z "$candidate" ]] && command -v /usr/libexec/java_home >/dev/null 2>&1; then
    candidate="$(/usr/libexec/java_home -v 25 2>/dev/null || true)"
  fi

  if [[ -n "$candidate" && -x "$candidate/bin/java" ]]; then
    export JAVA_HOME="$candidate"
    export PATH="$JAVA_HOME/bin:$PATH"
    log "Using JDK 25 from $JAVA_HOME"
    # Restart any Mill daemon that may have been started with an older JDK
    "$ROOT_DIR/mill" shutdown >/dev/null 2>&1 || true
  else
    warn "JDK 25 not detected ($current_ver). Mill/Kyo requires JDK 25 (see .sdkmanrc)."
  fi
}

ensure_java25

cleanup() {
  local code=$?
  if [[ -n "$FRONTEND_PID" ]] && kill -0 "$FRONTEND_PID" 2>/dev/null; then
    log "Stopping frontend server (pid $FRONTEND_PID)..."
    kill "$FRONTEND_PID" 2>/dev/null || true
    wait "$FRONTEND_PID" 2>/dev/null || true
  fi
  if [[ -n "$BACKEND_PID" ]] && kill -0 "$BACKEND_PID" 2>/dev/null; then
    log "Stopping backend server (pid $BACKEND_PID)..."
    # Kill process group if possible so Mill child JVM also stops
    kill -- -"$BACKEND_PID" 2>/dev/null || kill "$BACKEND_PID" 2>/dev/null || true
    wait "$BACKEND_PID" 2>/dev/null || true
  fi
  exit "$code"
}

trap cleanup EXIT INT TERM

ensure_postgres() {
  require_cmd docker
  log "Starting PostgreSQL container..."
  docker compose up -d postgres

  log "Waiting for PostgreSQL to accept connections..."
  local retries=30
  for ((i = 1; i <= retries; i++)); do
    if docker compose exec -T postgres pg_isready -U printcraft -d printcraft >/dev/null 2>&1; then
      log "PostgreSQL is ready."
      return 0
    fi
    sleep 1
  done
  err "PostgreSQL did not become ready within ${retries}s."
}

build_frontend() {
  local api_base_url="${1:-}"
  local link_task="fastLinkJS"
  if [[ "$LINK_MODE" == "full" ]]; then
    link_task="fullLinkJS"
  fi

  local targets=()
  case "$APP" in
    all)
      targets=("ui.$link_task" "+" "ui-admin.$link_task" "+" "ui-calculator.$link_task")
      ;;
    ui)
      targets=("ui.$link_task")
      ;;
    admin)
      targets=("ui-admin.$link_task")
      ;;
    calculator)
      targets=("ui-calculator.$link_task")
      ;;
    *)
      err "Unknown --app='$APP'. Expected: all | ui | admin | calculator"
      ;;
  esac

  log "Building Scala.js bundle(s): ${targets[*]}..."
  ./mill "${targets[@]}"

  rm -rf "$DEV_DIR"
  mkdir -p "$DEV_DIR"

  if [[ "$APP" == "all" || "$APP" == "ui" ]]; then
    assemble_app "modules/ui/src/main/resources" "out/ui/${link_task}.dest/main.js" "$DEV_DIR" "$api_base_url"
  fi
  if [[ "$APP" == "all" || "$APP" == "admin" ]]; then
    assemble_app "modules/ui-admin/src/main/resources" "out/ui-admin/${link_task}.dest/main.js" "$DEV_DIR/admin" "$api_base_url"
  fi
  if [[ "$APP" == "all" || "$APP" == "calculator" ]]; then
    assemble_app "modules/ui-calculator/src/main/resources" "out/ui-calculator/${link_task}.dest/main.js" "$DEV_DIR/calculator" "$api_base_url"
  fi

  log "Assembled frontend in $DEV_DIR"
}

assemble_app() {
  local res_dir="$1"
  local js_file="$2"
  local out_dir="$3"
  local api_base_url="$4"

  mkdir -p "$out_dir"
  cp -R "$res_dir"/. "$out_dir"/
  cp "$js_file" "$out_dir/main.js"

  if [[ -n "$api_base_url" && -f "$out_dir/index.html" ]]; then
    require_cmd python3
    python3 - "$out_dir/index.html" "$api_base_url" <<'PY'
import sys, json
html_path, api_url = sys.argv[1], sys.argv[2]
with open(html_path, "r", encoding="utf-8") as f:
    content = f.read()
needle = '<script src="main.js"></script>'
inject = f'<script>window.__API_BASE_URL__ = {json.dumps(api_url)};</script>\n  {needle}'
if needle in content:
    content = content.replace(needle, inject, 1)
else:
    content = content.replace("</body>", f"  <script>window.__API_BASE_URL__ = {json.dumps(api_url)};</script>\n</body>", 1)
with open(html_path, "w", encoding="utf-8") as f:
    f.write(content)
PY
  fi
}

backend_args() {
  local storage="$1"
  local args=("--storage=$storage" "--port=$API_PORT")
  if [[ "$NO_SEED" == "true" ]]; then
    args+=("--no-seed")
  fi
  printf '%s\n' "${args[@]}"
}

start_backend_bg() {
  local storage="$1"
  require_cmd curl

  local args=()
  while IFS= read -r line; do
    args+=("$line")
  done < <(backend_args "$storage")

  log "Starting backend server (storage=$storage, port=$API_PORT)..."
  if [[ "$storage" == "postgres" ]]; then
    DATABASE_URL="$DATABASE_URL" ./mill backend.app.run "${args[@]}" &
  else
    ./mill backend.app.run "${args[@]}" &
  fi
  BACKEND_PID=$!

  log "Waiting for backend health check at http://localhost:$API_PORT/healthz ..."
  local retries=120
  for ((i = 1; i <= retries; i++)); do
    if ! kill -0 "$BACKEND_PID" 2>/dev/null; then
      err "Backend process exited unexpectedly."
    fi
    if curl -sf "http://localhost:$API_PORT/healthz" >/dev/null 2>&1; then
      log "Backend is healthy."
      return 0
    fi
    sleep 1
  done
  err "Backend did not become healthy within ${retries}s."
}

print_urls() {
  local with_api="$1"
  echo ""
  log "Ready! Available URLs:"
  if [[ "$APP" == "all" || "$APP" == "ui" ]]; then
    echo "  Storefront:            http://localhost:$FRONTEND_PORT/"
    echo "  Backoffice (unified):  http://localhost:$FRONTEND_PORT/#/admin"
  fi
  if [[ "$APP" == "all" || "$APP" == "admin" ]]; then
    echo "  Backoffice (standalone): http://localhost:$FRONTEND_PORT/admin/"
  fi
  if [[ "$APP" == "all" || "$APP" == "calculator" ]]; then
    echo "  Price Calculator:      http://localhost:$FRONTEND_PORT/calculator/"
  fi
  if [[ "$with_api" == "true" ]]; then
    echo "  API Swagger UI:        http://localhost:$API_PORT/docs"
    echo "  API Health:            http://localhost:$API_PORT/healthz"
  fi
  echo ""
  echo "  Demo staff login:      admin@printshop.cz / staff1234"
  echo "  Demo B2B customer:     jan@printshoppro.cz / demo1234"
  echo "  Press Ctrl+C to stop."
  echo ""
}

serve_frontend_fg() {
  local with_api="$1"
  require_cmd python3
  if [[ "$BUILD_ONLY" == "true" ]]; then
    log "Build-only mode complete. Static files are in $DEV_DIR"
    return 0
  fi
  print_urls "$with_api"
  python3 -m http.server "$FRONTEND_PORT" --directory "$DEV_DIR" &
  FRONTEND_PID=$!
  wait "$FRONTEND_PID"
}

if [[ $# -lt 1 ]]; then
  usage
  exit 1
fi

COMMAND="$1"
shift

EXTRA_ARGS=()
while [[ $# -gt 0 ]]; do
  case "$1" in
    --app=*)
      APP="${1#*=}"
      ;;
    --port=*)
      FRONTEND_PORT="${1#*=}"
      ;;
    --api-port=*)
      API_PORT="${1#*=}"
      ;;
    --prod|--full)
      LINK_MODE="full"
      ;;
    --build-only)
      BUILD_ONLY=true
      ;;
    --no-seed)
      NO_SEED=true
      ;;
    --db-url=*)
      DATABASE_URL="${1#*=}"
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      EXTRA_ARGS+=("$1")
      ;;
  esac
  shift
done

case "$COMMAND" in
  -h|--help|help)
    usage
    ;;
  frontend|fe|static)
    build_frontend ""
    serve_frontend_fg "false"
    ;;
  frontend-api|fe-api|memory-stack)
    start_backend_bg "memory"
    build_frontend "http://localhost:$API_PORT"
    serve_frontend_fg "true"
    ;;
  fullstack|all|fe-be-db|postgres-stack)
    ensure_postgres
    start_backend_bg "postgres"
    build_frontend "http://localhost:$API_PORT"
    serve_frontend_fg "true"
    ;;
  backend|be|api)
    args=()
    while IFS= read -r line; do args+=("$line"); done < <(backend_args "memory")
    log "Running backend server (storage=memory, port=$API_PORT)..."
    exec ./mill backend.app.run "${args[@]}"
    ;;
  backend-db|be-db|api-db)
    ensure_postgres
    args=()
    while IFS= read -r line; do args+=("$line"); done < <(backend_args "postgres")
    log "Running backend server (storage=postgres, port=$API_PORT)..."
    exec env DATABASE_URL="$DATABASE_URL" ./mill backend.app.run "${args[@]}"
    ;;
  db-up)
    ensure_postgres
    ;;
  db-down)
    require_cmd docker
    log "Stopping PostgreSQL container..."
    docker compose stop postgres
    ;;
  db-reset)
    require_cmd docker
    log "Resetting PostgreSQL volume..."
    docker compose down -v
    ensure_postgres
    ;;
  build)
    if [[ ${#EXTRA_ARGS[@]} -gt 0 ]]; then
      ./mill "${EXTRA_ARGS[@]}"
    else
      build_frontend ""
      log "Compiling JVM backend..."
      ./mill backend.app.compile
    fi
    ;;
  test)
    if [[ ${#EXTRA_ARGS[@]} -gt 0 ]]; then
      ./mill "${EXTRA_ARGS[@]}"
    else
      ./mill __.test
    fi
    ;;
  *)
    warn "Unknown command: $COMMAND"
    echo ""
    usage
    exit 1
    ;;
esac
