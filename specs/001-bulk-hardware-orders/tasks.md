---
description: "Executable implementation tasks for Bulk Hardware Order Management"
---

# Tasks: Bulk Hardware Order Management

**Input**: Design documents from `/specs/001-bulk-hardware-orders/`

**Prerequisites**: `plan.md`, `spec.md`, `research.md`, `data-model.md`, `contracts/`, and `quickstart.md`

**Organization**: Tasks are grouped by user story so each increment can be implemented and validated independently after the shared foundation is complete.

**Testing note**: The feature specification defines independent test criteria and acceptance scenarios, but does not request a TDD workflow; per-behaviour test creation is therefore not broken out as separate implementation tasks. The test harnesses named in `plan.md` (JUnit 5/Mockito/AssertJ, Testcontainers, Vitest, React Testing Library) are set up in Setup, and the measurable success criteria (SC-001, SC-003, SC-007) and the audit retention rule have explicit verification tasks because `plan.md` states them as goals and `research.md` #13 states retention enforcement as a decision.

## Alignment with the current design artifacts

This task list was regenerated against `plan.md` and the Phase 0/1 artifacts as of commit `aa52ec2`. The previous list predated four commits of design changes; tasks were renumbered (no implementation code exists yet). Changes made to close plan-to-tasks discrepancies:

| # | Discrepancy | Source of truth | Resolution |
| - | ----------- | --------------- | ---------- |
| 1 | USD/cent rounding rule and UTC millisecond timestamp precision were unspecified in tasks | `research.md` #13, `data-model.md` "Precision and Retention Conventions", `plan.md` Constraints | T011, T013, T017, T023, T024 now state `numeric(12,2)`/`numeric(5,2)`, `timestamptz(3)`, half-up rounding order, and ISO-8601 UTC millisecond serialization |
| 2 | Seven-year audit retention had no task at all | `research.md` #13 (scheduled retention job + integration tests), FR-007, SC-005, `quickstart.md` §8 | New T068, T069 |
| 3 | HTTP 409 bodies must carry `currentStatus` and `latestUpdatedAt`; tasks said only "conflict" | `contracts/openapi.yaml` `ConflictProblemDetail`, FR-016, `plan.md` Constraints, `quickstart.md` §7 | T018, T030, T046, T054, T061 |
| 4 | Line-item edit with missing/ambiguous/expired terms must return 422 and preserve prior line items and totals | FR-025, `research.md` #5, `contracts/openapi.yaml` `422` on `PUT /orders/{id}/line-items`, `quickstart.md` §3 | T029, T030, T033 |
| 5 | `OrderSummary` now requires `lineItems` and `grossTotal` | `contracts/openapi.yaml` `OrderSummary`, FR-008 | T037, T039 |
| 6 | Event delivery is at-least-once with consumer dedup, not exactly-once | `contracts/events.md`, `research.md` #7 | T028 and the User Story 1 checkpoint |
| 7 | SC-001 (<5s), SC-003 (≤3 steps), SC-007 (≥500/day) were named as plan goals but had no verification task | `plan.md` Performance Goals, `spec.md` Success Criteria, `analysis.md` G1–G3 | New T035, T043, T070 |
| 8 | An `Operator` JPA entity was placed in the `client/` package, which `plan.md` reserves for `EnterpriseClient`/`ContractDiscountTerms` | `plan.md` Source Code layout, `research.md` #1/#2 | T015 places the seeded operator identity in `identity/` |
| 9 | Frontend test harness (Vitest + RTL, `apps/order-portal/tests/`) was never set up, and the backend Testcontainers harness was parked inside User Story 4 | `plan.md` Testing, `quickstart.md` §9 | T009, T010 in Setup |
| 10 | Build/test script setup was duplicated inside the User Story 1 phase | `plan.md` Project Structure | Folded into T002, T003, T009, T010 |

**Not changed, deliberately**: `analysis.md` items C1 and C2 report a conflict with a constitution amendment permitting line-item edits while Backordered. That amendment (v1.1.0, commit `f75a8e5`) was reverted in commit `a879396`; the checked-in constitution is v1.0.0 and does not permit Backordered edits. FR-025 (edits in Intake or Processing only) therefore stands, and these tasks keep that scope. See the completion report for the two open items this leaves.

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Establish the monorepo modules, local dependencies, build tooling, and test harnesses required by the order API and portal.

- [ ] T001 Create the root Maven reactor aggregating `services/order-api` as the only active module in `pom.xml`
- [ ] T002 [P] Scaffold the Spring Boot 3.5 / Java 25 order service module with Web, Validation, Data JPA, Actuator, AMQP, Lombok, Flyway, and springdoc-openapi dependencies plus surefire/failsafe and an `integration` profile matching `quickstart.md` §9 in `services/order-api/pom.xml`
- [ ] T003 [P] Scaffold the React 19 / TypeScript / Vite portal with `dev`, `build`, and `test` scripts in `apps/order-portal/package.json`, `apps/order-portal/tsconfig.json`, and `apps/order-portal/vite.config.ts`
- [ ] T004 [P] Configure Tailwind CSS and the portal source/test entry points in `apps/order-portal/tailwind.config.ts`, `apps/order-portal/postcss.config.js`, and `apps/order-portal/src/main.tsx`
- [ ] T005 [P] Add the local PostgreSQL 17 and RabbitMQ management services with health checks in `deploy/docker-compose.yml`
- [ ] T006 [P] Add backend and frontend container build definitions in `services/order-api/Dockerfile` and `apps/order-portal/Dockerfile`
- [ ] T007 [P] Add initial Helm chart metadata and values for the order API in `deploy/helm/order-api/Chart.yaml` and `deploy/helm/order-api/values.yaml`
- [ ] T008 [P] Add initial Helm chart metadata and values for the order portal in `deploy/helm/order-portal/Chart.yaml` and `deploy/helm/order-portal/values.yaml`
- [ ] T009 [P] Set up the JUnit 5 / Mockito / AssertJ unit harness and the shared PostgreSQL + RabbitMQ Testcontainers base class in `services/order-api/src/test/java/com/compudelivery/orders/support/ContainerizedIntegrationTest.java`
- [ ] T010 [P] Set up the Vitest + React Testing Library harness and `npm run test` entry point in `apps/order-portal/vitest.config.ts` and `apps/order-portal/tests/setup.ts`

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Implement shared persistence, identity, monetary/timestamp conventions, API error handling, and infrastructure that every user story depends on.

**Checkpoint**: No user story work should begin until this phase is complete.

- [ ] T011 Create the Flyway schema migration for clients, operators, contract terms, catalog items, orders, line items, lifecycle transitions, net-total calculations, and cancellation records — using `numeric(12,2)` for monetary amounts, `numeric(5,2)` for discount percentages, `timestamptz(3)` for all audit timestamps, a `bigint version` column on `bulk_order`, and a unique `order_id` on the cancellation record — in `services/order-api/src/main/resources/db/migration/V1__create_order_schema.sql`
- [ ] T012 [P] Seed demo clients, demo operator identities, time-bounded contract discount terms (including a client with expired/missing terms for the FR-004 negative case), and the fixed catalog items matching the reference designs in `services/order-api/src/main/resources/db/migration/V2__seed_demo_data.sql`
- [ ] T013 [P] Configure PostgreSQL, RabbitMQ, Flyway, actuator, springdoc, and UTC-millisecond Jackson date handling in `services/order-api/src/main/resources/application.yml`
- [ ] T014 [P] Implement the seeded client, contract-terms, and catalog JPA entities and repositories in `services/order-api/src/main/java/com/compudelivery/orders/client/EnterpriseClient.java`, `services/order-api/src/main/java/com/compudelivery/orders/client/ContractDiscountTerms.java`, and `services/order-api/src/main/java/com/compudelivery/orders/catalog/HardwareCatalogItem.java`
- [ ] T015 [P] Implement the seeded demo operator identity entity and repository backing `X-Operator-Id` resolution and the identity switcher in `services/order-api/src/main/java/com/compudelivery/orders/identity/DemoOperator.java` and `services/order-api/src/main/java/com/compudelivery/orders/identity/DemoOperatorRepository.java`
- [ ] T016 Implement trusted caller identity resolution that reads the mutually exclusive `X-Client-Id` / `X-Operator-Id` headers, resolves them against seeded rows, and rejects a missing or unknown header for the endpoint's required role with HTTP 400, in `services/order-api/src/main/java/com/compudelivery/orders/identity/CallerIdentity.java`, `services/order-api/src/main/java/com/compudelivery/orders/identity/CallerIdentityFilter.java`, and `services/order-api/src/main/java/com/compudelivery/orders/identity/CallerIdentityArgumentResolver.java`
- [ ] T017 [P] Implement the shared USD money convention — `BigDecimal` scale-2 half-up rounding applied to line subtotals and Gross Total before the contract percentage, then to Net Total — in `services/order-api/src/main/java/com/compudelivery/orders/pricing/MoneyRounding.java`
- [ ] T018 [P] Implement RFC 7807 exception mapping for validation (400), generic not-found (404), invalid transition and optimistic-lock conflicts (409, using a `ConflictProblemDetail` body carrying `currentStatus` and `latestUpdatedAt`), and contract-term failures (422) in `services/order-api/src/main/java/com/compudelivery/orders/web/GlobalExceptionHandler.java` and `services/order-api/src/main/java/com/compudelivery/orders/web/ConflictProblemDetail.java`
- [ ] T019 Implement the shared order persistence entities, repositories, status enum, and insert-only history mappings — `@Version` optimistic lock, nullable `netTotalLockedAt`, and no update/delete paths on the history entities — in `services/order-api/src/main/java/com/compudelivery/orders/order/BulkOrder.java`, `services/order-api/src/main/java/com/compudelivery/orders/order/LineItem.java`, `services/order-api/src/main/java/com/compudelivery/orders/order/OrderStatus.java`, `services/order-api/src/main/java/com/compudelivery/orders/order/LifecycleTransition.java`, `services/order-api/src/main/java/com/compudelivery/orders/pricing/NetTotalCalculation.java`, and `services/order-api/src/main/java/com/compudelivery/orders/order/CancellationRecord.java`
- [ ] T020 [P] Configure JSON DTO conventions, scale-2 decimal serialization, ISO-8601 UTC timestamps with exactly millisecond precision, validation, and controller-wide OpenAPI metadata in `services/order-api/src/main/java/com/compudelivery/orders/web/ApiConfiguration.java`
- [ ] T021 Configure the durable `order.events` topic exchange, the `order.events.intaken` queue bound on routing key `order.intaken`, and publisher confirms in `services/order-api/src/main/java/com/compudelivery/orders/messaging/RabbitMqConfiguration.java`
- [ ] T022 [P] Implement the read-only catalog and demo-identity endpoints returning `HardwareCatalogItem` and `DemoIdentity` (`id`, `displayName`, `role`) payloads in `services/order-api/src/main/java/com/compudelivery/orders/catalog/CatalogController.java` and `services/order-api/src/main/java/com/compudelivery/orders/identity/DemoIdentityController.java`
- [ ] T023 [P] Add the shared frontend API types, identity context, header-attaching fetch client, USD-cent and UTC-millisecond formatting helpers, router shell, and global styles in `apps/order-portal/src/api/types.ts`, `apps/order-portal/src/api/client.ts`, `apps/order-portal/src/lib/format.ts`, `apps/order-portal/src/context/IdentityContext.tsx`, `apps/order-portal/src/App.tsx`, and `apps/order-portal/src/index.css`

---

## Phase 3: User Story 1 - Submit Bulk Order and Receive Contract-Priced Net Total (Priority: P1) - MVP

**Goal**: Let a client create a valid bulk order that is finalized immediately in Intake, priced from current contract terms in USD rounded to cents, editable while Intake or Processing, and announced by an `OrderIntaken` event.

**Independent Test**: With a seeded client and active contract terms, submit valid line items and verify HTTP 201, `INTAKE` status, Gross Total, Net Total, and the contract discount snapshot. Verify missing, ambiguous, or expired terms block creation with HTTP 422; invalid quantities/SKUs and empty orders are rejected; editing in `INTAKE` or `PROCESSING` recalculates totals and appends calculation history, while an edit under invalid contract terms returns 422 and leaves prior line items and totals unchanged.

### Implementation

- [ ] T024 [P] [US1] Implement catalog lookup, current contract-term selection (`effective_from <= now()` and `effective_until IS NULL OR >= now()`), zero-match and multi-match blocking, and cent-accurate contract pricing via `MoneyRounding` in `services/order-api/src/main/java/com/compudelivery/orders/pricing/PricingService.java` and `services/order-api/src/main/java/com/compudelivery/orders/client/ContractTermsRepository.java`
- [ ] T025 [P] [US1] Implement create-order request/response DTOs with per-line-item (`quantity >= 1`, known SKU) and non-empty-order (`minItems: 1`) validation in `services/order-api/src/main/java/com/compudelivery/orders/order/CreateOrderRequest.java`, `services/order-api/src/main/java/com/compudelivery/orders/order/OrderDetailResponse.java`, and `services/order-api/src/main/java/com/compudelivery/orders/order/LineItemInput.java`
- [ ] T026 [US1] Implement transactional order creation that treats a valid submission as immediate finalization into `INTAKE`, snapshots catalog unit list prices, persists Gross/Net Totals, and writes the initial `SYSTEM` lifecycle transition and `INTAKE`-trigger net-total calculation under client ownership and optimistic versioning in `services/order-api/src/main/java/com/compudelivery/orders/order/OrderCreationService.java`
- [ ] T027 [US1] Implement `POST /api/orders` with `X-Client-Id` enforcement, HTTP 201 `OrderDetail`, and 400/422 ProblemDetail responses in `services/order-api/src/main/java/com/compudelivery/orders/order/OrderController.java`
- [ ] T028 [US1] Implement the `AFTER_COMMIT` `OrderIntaken` application event, its payload mapper (unique `eventId`, UTC millisecond `occurredAt`, line items, totals, discount snapshot), and the at-least-once RabbitMQ publisher documented for consumer dedup by `eventId`/`orderId` in `services/order-api/src/main/java/com/compudelivery/orders/messaging/OrderIntaken.java`, `services/order-api/src/main/java/com/compudelivery/orders/messaging/OrderIntakenPublisher.java`, and `services/order-api/src/main/java/com/compudelivery/orders/messaging/OrderEventListener.java`
- [ ] T029 [US1] Implement line-item replacement that validates current contract terms *before* mutating anything and rolls back preserving prior line items and totals when terms are missing, ambiguous, or expired, then revalidates quantities/SKUs, reprices, appends a `LINE_ITEM_EDIT` calculation row, rejects orders past Processing or with `netTotalLockedAt` set, and scopes by client under optimistic locking, in `services/order-api/src/main/java/com/compudelivery/orders/order/LineItemEditService.java`
- [ ] T030 [US1] Implement `PUT /api/orders/{orderId}/line-items` returning 400 for invalid line items, the generic client-scoped 404, 409 `ConflictProblemDetail` with `currentStatus` and `latestUpdatedAt` for locked or concurrently modified orders, and 422 for invalid contract terms, in `services/order-api/src/main/java/com/compudelivery/orders/order/OrderController.java`
- [ ] T031 [P] [US1] Implement the order-detail/create page catalog quantity inputs and live line-item subtotal calculations in `apps/order-portal/src/pages/OrderDetailPage.tsx` and `apps/order-portal/src/components/CatalogTable.tsx`
- [ ] T032 [P] [US1] Implement the pricing summary in order — Gross Subtotal, Contract Discount, informational "Waived" Freight & Logistics, Final Net Total — formatted as USD to cents in `apps/order-portal/src/components/PricingSummary.tsx`
- [ ] T033 [US1] Connect order creation and editable-order save flows to the API, blocking submission at zero total quantity and surfacing 400, 409, and contract-terms 422 messages distinctly, in `apps/order-portal/src/pages/OrderDetailPage.tsx` and `apps/order-portal/src/api/orders.ts`
- [ ] T034 [P] [US1] Implement the shared permanent five-column lifecycle stepper (Intake, Processing, Backordered, Shipped, Delivered) in `apps/order-portal/src/components/LifecycleStepper.tsx`
- [ ] T035 [US1] Verify SC-001 by asserting that order submission returns a calculated Net Total in under 5 seconds against the Testcontainers-backed stack, with the measured environment and threshold recorded in the test, in `services/order-api/src/test/java/com/compudelivery/orders/integration/OrderCreationLatencyIT.java`
- [ ] T036 [US1] Validate the User Story 1 create, invalid-contract-terms, edit, edit-rejection, and event scenarios from `specs/001-bulk-hardware-orders/quickstart.md` §3 and §8

**Checkpoint**: A client can independently submit and edit a contract-priced order, and a successful intake publishes an after-commit `OrderIntaken` event that consumers deduplicate by `eventId`/`orderId`.

---

## Phase 4: User Story 2 - View Order History and Status (Priority: P2)

**Goal**: Let a client view only its own order summaries and detailed lifecycle/pricing history through the dashboard and detail pages.

**Independent Test**: Seed orders for two clients, then verify each client's list contains only its own orders with line items, Gross Total, Net Total, status and dates; detail responses include line items and lifecycle history; and an order belonging to another client returns the same generic 404 shape as an unknown order ID.

### Implementation

- [ ] T037 [US2] Implement client-scoped order summary and detail query services returning the contract's `OrderSummary` shape (`id`, `status`, `lineItems`, `grossTotal`, `netTotal`, `createdAt`, `updatedAt`) and detail with append-only lifecycle history, filtering by `client_id` in the repository `WHERE` clause rather than the serializer, in `services/order-api/src/main/java/com/compudelivery/orders/order/OrderQueryService.java`
- [ ] T038 [US2] Implement `GET /api/orders` and `GET /api/orders/{orderId}` with repository-level client scoping and the identical generic 404 for unknown and foreign orders in `services/order-api/src/main/java/com/compudelivery/orders/order/OrderController.java`
- [ ] T039 [P] [US2] Implement the dashboard active-order cards, history log with Gross/Net totals and per-entry status indicators, and the new-order action in `apps/order-portal/src/pages/DashboardPage.tsx` and `apps/order-portal/src/components/OrderHistoryList.tsx`
- [ ] T040 [US2] Connect dashboard and detail data loading to the shared identity-aware order API in `apps/order-portal/src/api/orders.ts`, `apps/order-portal/src/pages/DashboardPage.tsx`, and `apps/order-portal/src/pages/OrderDetailPage.tsx`
- [ ] T041 [P] [US2] Implement the portal header title, selected client badge with contract reference, and the client/operator demo identity switcher fed by `GET /api/demo-identities` in `apps/order-portal/src/components/IdentitySwitcher.tsx` and `apps/order-portal/src/components/PortalHeader.tsx`
- [ ] T042 [US2] Add dashboard/detail empty, loading, generic-not-found, and API-error states in `apps/order-portal/src/pages/DashboardPage.tsx` and `apps/order-portal/src/pages/OrderDetailPage.tsx`
- [ ] T043 [US2] Verify SC-003 with a Testing Library interaction test asserting that selecting an identity, opening the dashboard, and reaching an order's current status takes no more than three steps, in `apps/order-portal/tests/order-discovery.test.tsx`
- [ ] T044 [US2] Validate the User Story 2 client-scoped history and detail scenarios from `specs/001-bulk-hardware-orders/quickstart.md` §4

**Checkpoint**: A selected client can navigate from dashboard to detail within three steps and cannot retrieve another client's order data.

---

## Phase 5: User Story 3 - Cancel an Active Order (Priority: P2)

**Goal**: Let the owning client explicitly confirm cancellation of an active order while preserving cancellation history and conflict semantics.

**Independent Test**: Create an order, cancel it before delivery, verify `CANCELLED` and a cancellation timestamp/record, then verify duplicate, delivered, terminal, cross-client, and concurrent cancellation attempts are rejected correctly.

### Implementation

- [ ] T045 [US3] Implement client-scoped transactional cancellation permitted from `INTAKE`, `PROCESSING`, `BACKORDERED`, and `SHIPPED` and rejected from the terminal `FINAL_DELIVERY`/`CANCELLED` states, writing one append-only lifecycle transition, exactly one `CancellationRecord`, and the denormalized `cancelledAt`, under JPA optimistic locking, in `services/order-api/src/main/java/com/compudelivery/orders/order/OrderCancellationService.java`
- [ ] T046 [US3] Implement `POST /api/orders/{orderId}/cancel` returning the generic client-scoped 404 and a 409 `ConflictProblemDetail` carrying `currentStatus` and `latestUpdatedAt` for terminal-state and first-committed-wins rejections in `services/order-api/src/main/java/com/compudelivery/orders/order/OrderController.java`
- [ ] T047 [P] [US3] Add dashboard cancel controls requiring explicit confirmation, with status-aware disabled states, in `apps/order-portal/src/components/CancelOrderButton.tsx` and `apps/order-portal/src/pages/DashboardPage.tsx`
- [ ] T048 [US3] Connect cancellation responses, refresh behavior, and 409 conflict messaging that surfaces the returned current status and latest update time in `apps/order-portal/src/api/orders.ts` and `apps/order-portal/src/pages/DashboardPage.tsx`
- [ ] T049 [US3] Ensure cancelled orders render a distinct history status and no longer expose lifecycle advancement controls in `apps/order-portal/src/components/OrderHistoryList.tsx` and `apps/order-portal/src/components/LifecycleStepper.tsx`
- [ ] T050 [US3] Validate the User Story 3 cancellation scenarios from `specs/001-bulk-hardware-orders/quickstart.md` §5
- [ ] T051 [US3] Validate the first-committed-wins race from `specs/001-bulk-hardware-orders/quickstart.md` §7 — advance the order to `SHIPPED`, fire concurrent cancel and `FINAL_DELIVERY` requests, and confirm one HTTP 200 and one HTTP 409 carrying `currentStatus` and `latestUpdatedAt`

**Checkpoint**: An owning client can cancel an eligible order exactly once, while delivered/cancelled/foreign orders remain protected and conflict losers receive reconcilable state.

---

## Phase 6: User Story 4 - Progress an Order Through Fulfillment Stages (Priority: P3)

**Goal**: Let an identified operator advance orders through the exact lifecycle, including the Processing/Backordered exception, while recording every transition and locking pricing at Shipped.

**Independent Test**: Advance an order through `INTAKE -> PROCESSING -> SHIPPED -> FINAL_DELIVERY`, verify transition history and terminal rejection, then verify `PROCESSING -> BACKORDERED -> PROCESSING`; reject skips, invalid reversals, cancelled-order advancement, and non-operator requests.

### Implementation

- [ ] T052 [US4] Implement the centralized allowed-transition map (`INTAKE→PROCESSING`, `PROCESSING→BACKORDERED`, `BACKORDERED→PROCESSING`, `PROCESSING→SHIPPED`, `SHIPPED→FINAL_DELIVERY`, and client-only `{INTAKE, PROCESSING, BACKORDERED, SHIPPED}→CANCELLED`) with actor-type and terminal-state validation in `services/order-api/src/main/java/com/compudelivery/orders/order/OrderLifecycleService.java`
- [ ] T053 [US4] Implement transactional operator status advancement with append-only transition records, the Shipped-time `netTotalLockedAt` stamp that makes line items and totals read-only at the service layer, and optimistic-lock conflict handling in `services/order-api/src/main/java/com/compudelivery/orders/order/OrderStatusService.java`
- [ ] T054 [US4] Implement `POST /api/orders/{orderId}/status` with `X-Operator-Id` enforcement, 422 for a disallowed transition, and 409 `ConflictProblemDetail` with `currentStatus` and `latestUpdatedAt` for concurrent modification, in `services/order-api/src/main/java/com/compudelivery/orders/order/OrderController.java`
- [ ] T055 [P] [US4] Add operator identity selection and operator-only lifecycle controls to the order detail page in `apps/order-portal/src/components/OperatorStatusControls.tsx` and `apps/order-portal/src/pages/OrderDetailPage.tsx`
- [ ] T056 [US4] Connect status advancement, Backordered return handling, Shipped read-only behavior, and 409 conflict feedback to the portal in `apps/order-portal/src/api/orders.ts` and `apps/order-portal/src/pages/OrderDetailPage.tsx`
- [ ] T057 [US4] Ensure the five-column lifecycle stepper reflects completed, current, and upcoming states including Backordered for every order in `apps/order-portal/src/components/LifecycleStepper.tsx`
- [ ] T058 [US4] Make line items and pricing read-only in the portal once an order reaches Shipped in `apps/order-portal/src/pages/OrderDetailPage.tsx` and `apps/order-portal/src/components/PricingSummary.tsx`
- [ ] T059 [US4] Implement the end-to-end lifecycle integration test over PostgreSQL and RabbitMQ Testcontainers covering forward progression, the Backordered round trip, terminal rejection, and append-only transition history in `services/order-api/src/test/java/com/compudelivery/orders/integration/OrderLifecycleIT.java`
- [ ] T060 [US4] Validate the User Story 4 lifecycle, terminal-state, operator-restriction, and Backordered scenarios from `specs/001-bulk-hardware-orders/quickstart.md` §6
- [ ] T061 [US4] Validate the Shipped-time pricing lock by confirming a post-Shipped `PUT /api/orders/{orderId}/line-items` returns 409 `ConflictProblemDetail` per `specs/001-bulk-hardware-orders/contracts/openapi.yaml`

**Checkpoint**: Operators can drive the lifecycle without skips, client edits lock at Shipped, and all transitions remain auditable.

---

## Phase 7: Polish & Cross-Cutting Concerns

**Purpose**: Complete deployment artifacts, audit retention, documentation, accessibility, throughput verification, and end-to-end validation across all stories.

- [ ] T062 [P] Add Kubernetes Deployment and Service templates for the API in `deploy/helm/order-api/templates/deployment.yaml` and `deploy/helm/order-api/templates/service.yaml`
- [ ] T063 [P] Add Kubernetes Deployment and Service templates for the portal in `deploy/helm/order-portal/templates/deployment.yaml` and `deploy/helm/order-portal/templates/service.yaml`
- [ ] T064 [P] Add readiness/liveness probes, environment values, and PostgreSQL/RabbitMQ connection wiring in `deploy/helm/order-api/templates/configmap.yaml` and `deploy/helm/order-api/values.yaml`
- [ ] T065 [P] Add portal API base URL configuration and production static serving configuration in `apps/order-portal/.env.example` and `apps/order-portal/nginx.conf`
- [ ] T066 [P] Add accessibility, responsive-layout, and error-state polish across `apps/order-portal/src/index.css`, `apps/order-portal/src/components/`, and `apps/order-portal/src/pages/`
- [ ] T067 [P] Add springdoc annotations covering `ConflictProblemDetail` and `UtcMillisecondTimestamp` and verify the generated OpenAPI document against `specs/001-bulk-hardware-orders/contracts/openapi.yaml` in `services/order-api/src/main/java/com/compudelivery/orders/`
- [ ] T068 Implement the scheduled audit retention job that refuses deletion of lifecycle, cancellation, and net-total-calculation rows before the order's terminal transition timestamp plus seven years, with the retention window configurable, in `services/order-api/src/main/java/com/compudelivery/orders/audit/AuditRetentionService.java` and `services/order-api/src/main/resources/application.yml`
- [ ] T069 Implement the retention integration test covering both a protected record inside the seven-year window and an eligible record past it in `services/order-api/src/test/java/com/compudelivery/orders/integration/AuditRetentionIT.java`
- [ ] T070 Verify SC-007 with a repeatable throughput check sustaining at least 500 order submissions per day equivalent rate with unchanged Net Total accuracy and response time, recording the rate and acceptance threshold, in `services/order-api/src/test/java/com/compudelivery/orders/integration/OrderThroughputIT.java`
- [ ] T071 Run the complete backend unit/integration, frontend, Docker Compose, and Helm validation commands documented in `specs/001-bulk-hardware-orders/quickstart.md` §9 and §10
- [ ] T072 Update implementation and local-run instructions in `README.md` for `services/order-api/`, `apps/order-portal/`, and `deploy/`

---

## Dependencies & Execution Order

### Phase Dependencies

- **Phase 1: Setup** has no dependencies and establishes the active monorepo modules and both test harnesses.
- **Phase 2: Foundational** depends on Setup and blocks all user stories.
- **Phase 3: User Story 1** depends on Foundational and is the MVP increment.
- **Phase 4: User Story 2** depends on Foundational and the order records/API established by User Story 1; its query and portal work can proceed in parallel once the shared order contract exists.
- **Phase 5: User Story 3** depends on Foundational and the order lifecycle persistence from User Story 1; T051 additionally depends on the operator status endpoint from User Story 4 (T054), since the race is run from `SHIPPED`.
- **Phase 6: User Story 4** depends on Foundational and the shared order model; its lifecycle service must be complete before operator controls and pricing-lock behavior are finalized.
- **Phase 7: Polish** depends on the desired user stories being complete.

### User Story Dependencies

- **User Story 1 (P1)**: Can start after Phase 2; no dependency on another user story.
- **User Story 2 (P2)**: Uses orders created by User Story 1 for realistic validation, but its scoped query/detail implementation is independently testable with seeded data.
- **User Story 3 (P2)**: Uses the shared order lifecycle model from Phase 2 and can be tested with seeded orders; it does not require User Story 2 UI work. Only its §7 race validation (T051) needs User Story 4's status endpoint.
- **User Story 4 (P3)**: Uses the shared order model and history tables from Phase 2; it can be implemented independently of the client dashboard, though the portal displays its results.

### Story Completion Order

```text
Phase 1 -> Phase 2 -> US1 (MVP) -> US2 and US3 in parallel -> US4 -> Polish
```

## Parallel Opportunities

- **Setup**: T002-T010 can run in parallel after T001 establishes the root layout.
- **Foundation**: T012-T018 and T020-T023 can run in parallel after T011 defines the database shape; T019 depends on the schema but is independent of frontend shell work, and T021 depends on the module scaffold only.
- **US1**: T024, T025, T031, T032, and T034 can run in parallel; T026-T030 then integrate the backend service and controller; T033 integrates the portal API flow; T035-T036 verify.
- **US2**: T039 and T041 can run in parallel with T037-T038; T040, T042, and T043 follow the shared API/query contract.
- **US3**: T045-T046 can proceed in parallel with T047; T048-T049 follow both backend and UI control work; T051 waits on T054.
- **US4**: T052-T054 proceed in sequence while T055 and T057 proceed in parallel; T056 and T058 integrate the lifecycle contract into the portal.
- **Polish**: T062-T067 can run in parallel; T068-T070 can run in parallel with each other once the domain is complete; T071 follows all of them and T072 can be written alongside deployment work.

## Parallel Examples

### User Story 1

```text
Task T024: Implement contract-term lookup and cent-accurate PricingService
Task T025: Implement create-order DTO validation
Task T031: Implement catalog quantity-entry UI
Task T032: Implement pricing summary UI
Task T034: Implement the shared five-column lifecycle stepper
```

### User Story 2

```text
Task T037: Implement scoped order query services returning the OrderSummary shape
Task T039: Implement dashboard order cards and history list
Task T041: Implement header identity switcher UI
```

### User Story 3

```text
Task T045: Implement transactional cancellation service
Task T047: Implement explicit-confirmation cancel controls
```

### User Story 4

```text
Task T052: Implement lifecycle transition rules
Task T055: Implement operator status controls
Task T057: Implement five-column lifecycle rendering
```

## Implementation Strategy

### MVP First: User Story 1 Only

1. Complete Phase 1 Setup.
2. Complete Phase 2 Foundational prerequisites.
3. Complete Phase 3 User Story 1.
4. Run T035 and T036 and verify order intake, cent-accurate pricing, editing, validation failures, the contract-terms 422 rollback, sub-5-second response, and `OrderIntaken` publishing.
5. Stop for an MVP demo before adding history, cancellation, and operator lifecycle workflows.

### Incremental Delivery

1. Add User Story 2 for client-scoped dashboard history and detail views.
2. Add User Story 3 for safe client cancellation.
3. Add User Story 4 for operator lifecycle progression and Shipped-time pricing lock.
4. Complete Phase 7 deployment, audit retention, accessibility, throughput verification, documentation, and full quickstart validation.

## Completion Criteria

- All 72 tasks use the required `- [ ] [TaskID] [P?] [Story?] description with file path` format.
- Every user story has a goal, an independent test criterion, and a checkpoint.
- The MVP is independently demonstrable after T036.
- Every measurable success criterion in `spec.md` has a verification task: SC-001 (T035), SC-002 (T024-T029, T036), SC-003 (T043), SC-004 (T045-T050), SC-005 (T019, T026, T045, T053, T068, T069), SC-006 (T037-T038, T045), SC-007 (T070).
- Monetary amounts are USD rounded to cents and audit timestamps are UTC with millisecond precision throughout (T011, T013, T017, T020, T023, T024).
- No Warehouse or Invoice service/app code is created by these tasks.
