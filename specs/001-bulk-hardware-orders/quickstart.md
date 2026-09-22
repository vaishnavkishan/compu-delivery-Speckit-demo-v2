# Quickstart: Bulk Hardware Order Management

Validates the feature end-to-end (User Stories 1–4) against the contracts in
`contracts/openapi.yaml` and `contracts/events.md`, and the data model in
`data-model.md`. Commands assume the monorepo project structure defined by this
plan; some paths (e.g. `deploy/docker-compose.yml`, `services/order-api/`,
`apps/order-portal/`) are created during implementation, not by `/speckit-plan`
itself. `services/warehouse-api/`, `services/invoice-api/`, and their portals are
reserved for future features and are not part of this quickstart.

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
`contracts/openapi.yaml`.

## 2. Start the frontend

```bash
cd apps/order-portal
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

Ceiling case (FR-014, FR-030): repeat with a line item quantity of `10001` and
confirm HTTP 400 naming the quantity ceiling; repeat with 101 distinct line
items and confirm HTTP 400 naming the line-item-count ceiling; confirm neither
request creates an order.

## 4. Validate User Story 2 — order history scoped per client, paged, and operator-viewable

```bash
curl -s http://localhost:8080/api/orders -H "X-Client-Id: ACME-001" | jq
curl -s http://localhost:8080/api/orders -H "X-Client-Id: OTHER-CLIENT" | jq
```

Expected: each response is an `OrderHistoryPage` (`items`, `page`, `pageSize`,
`totalCount`, `hasMore`) containing only that client's own orders,
newest-first (FR-008, FR-009). Fetching ACME-001's order ID with
`X-Client-Id: OTHER-CLIENT` on `GET /api/orders/{id}` returns HTTP 404
(FR-018), identical in shape to a nonexistent order ID.

Pagination case (FR-008): create 26+ orders for one client, then confirm
`GET /api/orders?page=0` returns 25 items with `hasMore: true`, and
`GET /api/orders?page=1` returns the remainder with no entry repeated or
skipped across the two pages.

Operator-view case (FR-026): as the seeded operator, call
`curl -s http://localhost:8080/api/orders -H "X-Operator-Id: OPS-1" -H "X-Client-Id: ACME-001"`
and confirm it returns the same page ACME-001 itself would see — an operator
views a specific client's history via the identity switcher's client
selector, never a cross-client queue.

Seeded-roster case (FR-027, FR-004): submitting an order as `NOTERMS-003`
(missing terms), `EXPIRED-004` (expired terms), or `AMBIGUOUS-005` (ambiguous
terms) each returns HTTP 422 naming the specific condition, per step 3's
negative case.

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
succeeds (HTTP 200) and the other returns HTTP 409 whose body includes both a
message indicating the order's state has changed and a `currentStatus` field
matching the order's actual now-current status.

## 8. Validate the OrderIntaken event

With the RabbitMQ management UI (`http://localhost:15672`, default guest/guest) or
`rabbitmqadmin`, confirm a message matching `contracts/events.md`'s `OrderIntaken`
schema was published to the `order.events` exchange with routing key
`order.intaken` for each order created in step 3.

## 9. Validate portal empty/loading/error states (FR-028, FR-029)

Manual UI check against `apps/order-portal` (started in step 2):

- Select a freshly seeded demo client with no orders yet: Active Orders shows
  "No active orders" with a call to action to start an order, and Order
  History shows "No past orders" — not a blank region.
- Throttle or block the network in devtools while loading the dashboard:
  each region shows a loading indicator, then — on failure — a plain error
  message with a retry action; retrying re-issues the fetch.

## 10. Validate SC-003 — order status reachable within 3 UI actions

Manual UI check against `apps/order-portal` (started in step 2), performed from a
fresh page load with no identity yet selected. Count every discrete UI action
(click, tap, or navigation) taken:

1. **Action 1**: Click the header's demo identity switcher and select an
   enterprise client identity (FR-019).
2. If the Dashboard's Active Orders section is now showing that client's own
   orders with each order's `LifecycleStepper` status visible, **stop here** —
   status was reached in 1 action.
3. If instead the lifecycle status is only shown on a per-order detail screen,
   continue: **Action 2** — click an order card/row (from Active Orders or
   Order History) to open `OrderDetailPage`, and (if needed) **Action 3** —
   any further click required to reveal the status indicator on that screen.

Expected: the action count from step 1 through a status being visible on
screen is **3 or fewer**, satisfying SC-003. Repeat with a second seeded
identity and confirm only that identity's own orders and statuses ever
appear — never another client's.

## 11. Automated verification (run during implementation, not by this plan)

```bash
cd services/order-api && ./mvnw test                 # JUnit5/Mockito/AssertJ unit tests
cd services/order-api && ./mvnw verify -Pintegration  # Testcontainers-backed integration tests
cd apps/order-portal && npm run test                  # Vitest + RTL
```

## 12. Deployment validation (k3s / Rancher Desktop)

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
