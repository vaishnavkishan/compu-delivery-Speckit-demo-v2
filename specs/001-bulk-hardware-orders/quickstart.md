# Quickstart: Bulk Hardware Order Management

Validates the feature end-to-end (User Stories 1–4) against the contracts in
`contracts/openapi.yaml` and `contracts/events.md`, and the data model in
`data-model.md`. Commands assume the project structure and files defined by this
plan; some paths (e.g. `docker-compose.yml`, `backend/`, `frontend/`) are created
during implementation, not by `/speckit-plan` itself.

## Prerequisites

- Java 25, Maven, Node.js (LTS matching Vite/React 19 requirements), Docker.
- Local dependencies: PostgreSQL 17 and RabbitMQ, started via `docker-compose up -d`
  from the repo root (see `research.md` #12).

## 1. Start dependencies and the backend

```bash
docker-compose up -d           # postgres:17, rabbitmq:3-management
cd backend
./mvnw spring-boot:run          # runs Flyway migrations on startup, seeds demo data
```

Expected: service listening on `http://localhost:8080`; Swagger UI available at
`http://localhost:8080/swagger-ui.html` (springdoc-openapi) matching
`contracts/openapi.yaml`.

## 2. Start the frontend

```bash
cd frontend
npm install
npm run dev
```

Expected: portal available at `http://localhost:5173`, header identity switcher
populated from `GET /api/demo-identities`.

## 3. Validate User Story 1 — submit a bulk order and receive contract-priced Net Total

```bash
curl -s -X POST http://localhost:8080/api/orders \
  -H "X-Client-Id: ACME-001" -H "Content-Type: application/json" \
  -d '{"lineItems":[{"sku":"SKU-1001","quantity":25}]}' | jq
```

Expected: HTTP 201, `status: "INTAKE"`, `netTotal` = `grossTotal` reduced by
ACME-001's seeded contract discount percentage (see FR-001–FR-003).

Negative case (FR-004): repeat against a seeded client with expired/missing terms
and confirm HTTP 422 with a clear reason, no order created.

Edit case (FR-025): `PUT /api/orders/{id}/line-items` while status is `INTAKE` or
`PROCESSING`; confirm `grossTotal`/`netTotal` change and a new `NetTotalCalculation`
row is implied by the updated `OrderDetail` response.

## 4. Validate User Story 2 — order history scoped per client

```bash
curl -s http://localhost:8080/api/orders -H "X-Client-Id: ACME-001" | jq
curl -s http://localhost:8080/api/orders -H "X-Client-Id: OTHER-CLIENT" | jq
```

Expected: each list contains only that client's own orders (FR-008, FR-009).
Fetching ACME-001's order ID with `X-Client-Id: OTHER-CLIENT` on
`GET /api/orders/{id}` returns HTTP 404 (FR-018), identical in shape to a
nonexistent order ID.

## 5. Validate User Story 3 — cancel an active order

```bash
curl -s -X POST http://localhost:8080/api/orders/{orderId}/cancel \
  -H "X-Client-Id: ACME-001" | jq
```

Expected: HTTP 200, `status: "CANCELLED"`. Repeating the same call returns HTTP 409
(FR-011). Attempting on another client's order returns HTTP 404 (FR-018).

## 6. Validate User Story 4 — operator advances lifecycle

```bash
curl -s -X POST http://localhost:8080/api/orders/{orderId}/status \
  -H "X-Operator-Id: OPS-1" -H "Content-Type: application/json" \
  -d '{"targetStatus":"PROCESSING"}' | jq
```

Repeat with `SHIPPED` immediately (skipping) to confirm HTTP 409 (FR-006). Then
advance in order (`PROCESSING`→`SHIPPED`→`FINAL_DELIVERY`) and confirm a further
status change attempt after `FINAL_DELIVERY` returns HTTP 409 (terminal state).
Advance a different order `PROCESSING`→`BACKORDERED`→`PROCESSING` and confirm both
transitions succeed (FR-006 exception case).

## 7. Validate FR-016 — first-committed-wins conflict

Fire a cancel request and a `targetStatus: FINAL_DELIVERY` advance request for the
same order concurrently (e.g. two parallel `curl` calls); confirm exactly one
succeeds (HTTP 200) and the other returns HTTP 409 with a message indicating the
order's state has changed.

## 8. Validate the OrderIntaken event

With the RabbitMQ management UI (`http://localhost:15672`, default guest/guest) or
`rabbitmqadmin`, confirm a message matching `contracts/events.md`'s `OrderIntaken`
schema was published to the `order.events` exchange with routing key
`order.intaken` for each order created in step 3.

## 9. Automated verification (run during implementation, not by this plan)

```bash
cd backend && ./mvnw test                 # JUnit5/Mockito/AssertJ unit tests
cd backend && ./mvnw verify -Pintegration # Testcontainers-backed integration tests
cd frontend && npm run test               # Vitest + RTL
```

## 10. Deployment validation (k3s / Rancher Desktop)

```bash
docker build -t order-service:local backend/
docker build -t order-portal:local frontend/
helm upgrade --install compu-delivery deploy/helm/ \
  --set backend.image=order-service:local \
  --set frontend.image=order-portal:local
kubectl get pods
```

Expected: backend, frontend, PostgreSQL, and RabbitMQ pods reach `Running`; the
portal is reachable per the chart's configured Service/Ingress, and steps 3–8 above
succeed against the k3s-hosted service.
