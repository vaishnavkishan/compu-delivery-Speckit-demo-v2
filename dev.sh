#!/usr/bin/env bash
# Starts the full local stack for the order-api / order-portal feature:
# Postgres + RabbitMQ (docker-compose), the Spring Boot backend, then the
# Vite dev server in the foreground. Ctrl-C stops the frontend and backend;
# the docker-compose infra is left running (see deploy/docker-compose.yml).
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

echo "==> Starting PostgreSQL + RabbitMQ (docker-compose)"
(cd "$ROOT_DIR/deploy" && docker compose up -d)

echo "==> Starting order-api (Spring Boot) in the background"
(cd "$ROOT_DIR" && mvn -q -pl services/order-api -am spring-boot:run) &
BACKEND_PID=$!

cleanup() {
  echo "==> Stopping order-api"
  kill "$BACKEND_PID" 2>/dev/null || true
}
trap cleanup EXIT

echo "==> Waiting for order-api to become healthy on :8080"
until curl -sf http://localhost:8080/actuator/health >/dev/null 2>&1; do
  sleep 1
done
echo "==> order-api is up: http://localhost:8080"

echo "==> Starting order-portal (Vite dev server) on :5173"
cd "$ROOT_DIR/apps/order-portal"
npm install --silent
npm run dev
