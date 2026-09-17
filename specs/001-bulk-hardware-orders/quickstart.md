# Quickstart: Bulk Hardware Order Management

Validates the feature end-to-end (User Stories 1–4) against the contracts in
`contracts/openapi.yaml` and `contracts/events.md`, and the data model in
`data-model.md`. Commands assume the monorepo project structure defined by this
plan; some paths (e.g. `deploy/docker-compose.yml`, `services/order-api/`,
`apps/order-portal/`) are created during implementation, not by `/speckit-plan`
itself. `services/warehouse-api/`, `services/invoice-api/`, and their portals are
reserved for future features and are not part of this quickstart.

Revised 2026-09-17 for constitution v2.0.0 — the lifecycle walkthrough is now a
four-stage chain with no Backordered step, cancellation is validated as closing at
Shipped, and steps 6a/7/8 cover the operator cross-client scope, the Active/History
partition, and the role-conditional controls.

## Prerequisites

- Java 25, Maven, Node.js (LTS matching Vite/React 19 requirements), Docker.
- Local dependencies: PostgreSQL 17 and RabbitMQ, started via
  `docker-compose up -d` from `deploy/` (see `research.md` #12).

## 1. Start dependencies and the backend

```bash
cd deploy && docker-compose up -d   # postgres:17, rabbitmq:3-management
cd ../services/order-api
./mvnw spring-boot:run              # runs Flyway migrations on startup, seeds demo data
```

Expected: service listening on `http://localhost:8080`; Swagger UI available at
`http://localhost:8080/swagger-ui.html` (springdoc-openapi) matching
`contracts/openapi.yaml` — in particular an `OrderStatus` enum of exactly
`INTAKE, PROCESSING, SHIPPED, FINAL_DELIVERY, CANCELLED` with no backordered/on-hold
member (constitution v2.0.0 Principle I).

## 2. Start the frontend

```bash
cd apps/order-portal
npm install
npm run dev
```

Expected: portal available at `http://localhost:5173`, header identity switcher
populated from `GET /api/demo-identities` with both client and operator entries.

## 3. Validate User Story 1 — submit a bulk order and receive contract-priced Net Total

```bash
curl -s -X POST http://localhost:8080/api/orders \
  -H "X-Client-Id: ACME-001" -H "Content-Type: application/json" \
  -d '{"lineItems":[{"sku":"SKU-1001","quantity":25}]}' | jq
```

Expected: HTTP 201, `status: "INTAKE"`, `netTotal` = `grossTotal` reduced by
ACME-001's seeded contract discount percentage (see FR-001–FR-003), plus the derived
flags `active: true`, `cancellable: true`, `nextStatus: "PROCESSING"`.

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

Partition check (FR-023): every returned order carries `active`, and the set of
orders with `active: true` (status `INTAKE`/`PROCESSING`/`SHIPPED`) and the set with
`active: false` (status `FINAL_DELIVERY`/`CANCELLED`) are disjoint and together
cover the whole list — no order is missing and none appears in both.

## 5. Validate User Story 3 — cancel within the Cancellation Window

```bash
curl -s -X POST http://localhost:8080/api/orders/{orderId}/cancel \
  -H "X-Client-Id: ACME-001" | jq
```

Expected: HTTP 200, `status: "CANCELLED"`, `cancellable: false`, `active: false`.
Repeating the same call returns HTTP 409 stating the order is already cancelled
(FR-011). Attempting on another client's order returns HTTP 404 (FR-018).

Window-closed case (FR-010, FR-011 — the v2.0.0 behaviour change): take a second
order, advance it to `SHIPPED` via step 6, then attempt cancellation.

```bash
curl -s -o /dev/null -w '%{http_code}\n' -X POST \
  http://localhost:8080/api/orders/{shippedOrderId}/cancel -H "X-Client-Id: ACME-001"
```

Expected: HTTP 409 whose `detail` explicitly says the order has shipped and can no
longer be cancelled — not a silent no-op, and not a success. The same order's
payload reports `active: true, cancellable: false`, confirming a Shipped order is
Active but outside the Cancellation Window.

## 6. Validate User Story 4 — operator advances the lifecycle

```bash
curl -s -X POST http://localhost:8080/api/orders/{orderId}/status \
  -H "X-Operator-Id: OPS-1" -H "Content-Type: application/json" \
  -d '{"targetStatus":"PROCESSING"}' | jq
```

Then, in order: `PROCESSING`→`SHIPPED`→`FINAL_DELIVERY`, confirming each response's
`nextStatus` names the following stage and becomes `null` at `FINAL_DELIVERY`.
Confirm on entering `SHIPPED` that `netTotalLocked: true` and `cancellable: false`
(FR-017, FR-010 — one transition closes both).

Invalid-transition cases, each expecting HTTP 409 (FR-006):

- Skip a stage: request `SHIPPED` on an order still in `INTAKE`.
- Move backward: request `PROCESSING` on an order in `SHIPPED`.
- Change a terminal order: request any status on an order at `FINAL_DELIVERY` or
  `CANCELLED`.

Out-of-lifecycle status, expecting HTTP **400** rather than 409: post
`{"targetStatus":"BACKORDERED"}`. It fails deserialization as an unknown enum value
before any transition check runs — the state is unrepresentable, not merely
disallowed (constitution v2.0.0 Principle I).

Client-identity rejection (FR-012): repeat the first call with `X-Client-Id` instead
of `X-Operator-Id` and confirm it is rejected rather than advancing the order.

Availability case (FR-005): an order whose hardware is unavailable simply remains in
`PROCESSING` — there is no endpoint, field, or status representing stock state.

## 6a. Validate operator cross-client scope (FR-027)

```bash
curl -s http://localhost:8080/api/orders -H "X-Operator-Id: OPS-1" \
  | jq '[.[] | {id, clientId, clientDisplayName, status, active}]'
```

Expected: orders from **every** seeded client, each labelled with its owning
`clientId`/`clientDisplayName`, so the union of the per-client lists from step 4 is
covered. `GET /api/orders/{id}` with `X-Operator-Id` resolves any client's order
(HTTP 200) rather than returning the client-scoped 404 — operators sit outside the
per-client boundary by design and are excluded from SC-006.

Also confirm `GET /api/orders` with neither header, and with both headers, each
returns HTTP 400.

## 7. Validate FR-016 — first-committed-wins conflict

Fire a client cancel request and an operator `targetStatus: SHIPPED` advance request
for the same `PROCESSING` order concurrently (e.g. two parallel `curl` calls):

```bash
curl -s -o /dev/null -w 'cancel:%{http_code}\n' -X POST \
  http://localhost:8080/api/orders/{orderId}/cancel -H "X-Client-Id: ACME-001" &
curl -s -o /dev/null -w 'advance:%{http_code}\n' -X POST \
  http://localhost:8080/api/orders/{orderId}/status \
  -H "X-Operator-Id: OPS-1" -H "Content-Type: application/json" \
  -d '{"targetStatus":"SHIPPED"}' &
wait
```

Expected: exactly one returns HTTP 200 and the other HTTP 409 with a message
indicating the order's state has changed. This is the only race that can close the
Cancellation Window mid-request (spec Edge Cases).

## 8. Validate the portal's role-conditional UI (FR-022, FR-023, FR-026)

With a **client** identity selected in the switcher:

- Each Active Orders card shows a 4-step stepper (Intake, Processing, Shipped, Final
  Delivery) — four columns, no Backordered step.
- An `INTAKE`/`PROCESSING` card's cancel control is enabled and requires explicit
  confirmation; a `SHIPPED` card's cancel control is visible but **disabled**, with
  an inline explanation that the order has shipped (FR-022).
- The Order History Log lists only `FINAL_DELIVERY` and `CANCELLED` orders, and no
  order appears in both sections (FR-023).
- No advance control appears anywhere (FR-012).

Switch to an **operator** identity:

- The dashboard lists all clients' orders, each attributed to its owning client.
- Each non-terminal card shows "Advance to <next stage>" in place of the cancel
  control, naming the stage the API would accept; terminal orders show neither
  control (FR-026).
- Order creation and line-item editing controls are not offered.

## 9. Validate the OrderIntaken event

With the RabbitMQ management UI (`http://localhost:15672`, default guest/guest) or
`rabbitmqadmin`, confirm a message matching `contracts/events.md`'s `OrderIntaken`
schema was published to the `order.events` exchange with routing key
`order.intaken` for each order created in step 3. No event is published for
lifecycle advancement or cancellation in this feature's scope.

## 10. Automated verification (run during implementation, not by this plan)

```bash
cd services/order-api && ./mvnw test                 # JUnit5/Mockito/AssertJ unit tests
cd services/order-api && ./mvnw verify -Pintegration  # Testcontainers-backed integration tests
cd apps/order-portal && npm run test                  # Vitest + RTL
```

## 11. Deployment validation (k3s / Rancher Desktop)

```bash
docker build -t order-api:local services/order-api/
docker build -t order-portal:local apps/order-portal/
helm upgrade --install order-api deploy/helm/order-api/ \
  --set image=order-api:local
helm upgrade --install order-portal deploy/helm/order-portal/ \
  --set image=order-portal:local
kubectl get pods
```

Expected: backend, frontend, PostgreSQL, and RabbitMQ pods reach `Running`; the
portal is reachable per the chart's configured Service/Ingress, and steps 3–9 above
succeed against the k3s-hosted service.
