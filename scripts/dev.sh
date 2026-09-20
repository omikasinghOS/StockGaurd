#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
# Values remain in this shell and child containers; no .env file is written.
for key in POSTGRES_PASSWORD DB_PASSWORD JWT_SECRET DEMO_ADMIN_PASSWORD DEMO_MANAGER_PASSWORD DEMO_VIEWER_PASSWORD; do
  if [[ -z "${!key:-}" ]]; then
    read -rsp "Enter $key (JWT_SECRET: at least 32 characters; passwords: at least 12): " value
    echo
    export "$key=$value"
  fi
done
exec docker compose up --build "$@"
