---
description: "Executable implementation tasks for Bulk Hardware Order Management"
---

# Tasks: Bulk Hardware Order Management

**Input**: Design documents from `/specs/001-bulk-hardware-orders/`

**Prerequisites**: `plan.md`, `spec.md`, `research.md`, `data-model.md`, `contracts/`, and `quickstart.md`

**Organization**: Tasks are grouped by user story so each increment can be implemented and validated independently once the shared foundation is complete.

**Testing note**: The feature specification defines independent test criteria and acceptance scenarios but does not request a TDD workflow, so failing-test-first tasks are not broken out per story. Automated unit, integration, and frontend suites (the stack named in `plan.md`) are implemented in the Polish phase, and each story ends with an explicit `quickstart.md` validation task.

**Revision note (2026-09-17)**: Regenerated against constitution **v2.0.0** and the spec's Session 2026-09-17 clarifications. The previous revision's `BACKORDERED` state, `PROCESSING ⇄ BACKORDERED` reversal, five-column stepper, and cancel-until-Final-Delivery tasks are removed; tasks for the Cancellation Window closing at Shipped (FR-010/FR-011/FR-022), the Active/History partition (FR-023), role-conditional controls on the existing two pages (FR-026), and operator cross-client scope (FR-027) are added.

## Path Conventions

Web monorepo per `plan.md`: backend at `services/order-api/` (Spring Boot 3.5 / Java 25, package root `com.compudelivery.orders`), frontend at `apps/order-portal/` (React 19 / TypeScript / Vite / Tailwind), deployment artifacts at `deploy/`. `services/warehouse-api/`, `services/invoice-api/`, and their portals are reserved names only and are **not** scaffolded by any task below.

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Establish the monorepo modules, local dependencies, and build/deployment tooling required by the order API and portal.

- [ ] T001 Create the root Maven reactor with `services/order-api` as the only active module in `pom.xml`
- [ ] T002 [P] Scaffold the Spring Boot 3.5 / Java 25 order service module with Web, Validation, Data JPA, Actuator, AMQP, Lombok, Flyway, springdoc-openapi, and Testcontainers dependencies in `services/order-api/pom.xml`
- [ ] T003 [P] Scaffold the React 19 / TypeScript / Vite portal with React Router and Vitest in `apps/order-portal/package.json`, `apps/order-portal/tsconfig.json`, and `apps/order-portal/vite.config.ts`
- [ ] T004 [P] Configure Tailwind CSS and the portal source/test entry points in `apps/order-portal/tailwind.config.ts`, `apps/order-portal/postcss.config.js`, and `apps/order-portal/src/main.tsx`
- [ ] T005 [P] Add local PostgreSQL 17 and RabbitMQ (management plugin) services with health checks in `deploy/docker-compose.yml`
- [ ] T006 [P] Add multi-stage container build definitions in `services/order-api/Dockerfile` and `apps/order-portal/Dockerfile`
- [ ] T007 [P] Add Helm chart metadata and values for the order API in `deploy/helm/order-api/Chart.yaml` and `deploy/helm/order-api/values.yaml`
- [ ] T008 [P] Add Helm chart metadata and values for the order portal in `deploy/helm/order-portal/Chart.yaml` and `deploy/helm/order-portal/values.yaml`

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Implement shared persistence, the constitutional lifecycle model, trusted-identity resolution, API error format, messaging topology, and the frontend shell that every user story depends on.

**⚠️ CRITICAL**: No user story work can begin until this phase is complete.

- [ ] T009 Create the Flyway schema migration for clients, operators, contract discount terms, catalog items, orders (with `version`, `net_total_locked_at`, `cancelled_at`), line items, lifecycle transitions, net-total calculations, and cancellation records (unique `order_id`) in `services/order-api/src/main/resources/db/migration/V1__create_order_schema.sql`, constraining `status` to `INTAKE, PROCESSING, SHIPPED, FINAL_DELIVERY, CANCELLED` only (FR-005, Constitution I)
- [ ] T010 [P] Seed demo clients, demo operators, contract discount terms (including one client with expired terms and one with none, for FR-004), and the fixed Figma-matching hardware catalog in `services/order-api/src/main/resources/db/migration/V2__seed_demo_data.sql`
- [ ] T011 [P] Configure PostgreSQL, RabbitMQ, Flyway, Actuator, and springdoc settings in `services/order-api/src/main/resources/application.yml`
- [ ] T012 [P] Implement the seeded reference entities and repositories in `services/order-api/src/main/java/com/compudelivery/orders/client/EnterpriseClient.java`, `services/order-api/src/main/java/com/compudelivery/orders/client/Operator.java`, `services/order-api/src/main/java/com/compudelivery/orders/client/ContractDiscountTerms.java`, and `services/order-api/src/main/java/com/compudelivery/orders/catalog/HardwareCatalogItem.java`
- [ ] T013 Implement the `OrderStatus` enum with exactly five members (no on-hold/backordered member), the centralized allowed-transition map with no reverse or self edges, and the derived predicates `isActive()`, `isWithinCancellationWindow()`, and `nextStatus()` in `services/order-api/src/main/java/com/compudelivery/orders/order/OrderStatus.java` (FR-005, FR-006, FR-010, FR-023, FR-026)
- [ ] T014 Implement the order persistence entities and append-only history mappings — `BulkOrder` with JPA `@Version`, `LineItem`, `LifecycleTransition`, `NetTotalCalculation`, `CancellationRecord` — in `services/order-api/src/main/java/com/compudelivery/orders/order/BulkOrder.java`, `services/order-api/src/main/java/com/compudelivery/orders/order/LineItem.java`, `services/order-api/src/main/java/com/compudelivery/orders/order/LifecycleTransition.java`, `services/order-api/src/main/java/com/compudelivery/orders/order/CancellationRecord.java`, and `services/order-api/src/main/java/com/compudelivery/orders/pricing/NetTotalCalculation.java` (FR-007, FR-016, Constitution IV, V)
- [ ] T015 Implement trusted caller identity resolution for mutually exclusive `X-Client-Id` / `X-Operator-Id` headers, rejecting missing, unknown, or both-supplied identities with HTTP 400, in `services/order-api/src/main/java/com/compudelivery/orders/identity/CallerIdentity.java`, `services/order-api/src/main/java/com/compudelivery/orders/identity/CallerIdentityFilter.java`, and `services/order-api/src/main/java/com/compudelivery/orders/identity/CallerIdentityArgumentResolver.java`
- [ ] T016 [P] Implement RFC 7807 `ProblemDetail` exception mapping for validation (400), generic client-scoped not-found (404), invalid transition / locked order / already-cancelled / `OptimisticLockException` (409), and contract-term failures (422) in `services/order-api/src/main/java/com/compudelivery/orders/web/GlobalExceptionHandler.java`
- [ ] T017 [P] Implement the shared `Order`/`OrderSummary`/`OrderDetail` response DTOs and mapper that project the derived `active`, `cancellable`, `nextStatus`, and `netTotalLocked` flags plus decimal serialization conventions in `services/order-api/src/main/java/com/compudelivery/orders/order/OrderResponseMapper.java` and `services/order-api/src/main/java/com/compudelivery/orders/web/ApiConfiguration.java`
- [ ] T018 Configure the durable `order.events` topic exchange, the `order.events.intaken` queue bound on routing key `order.intaken`, and publisher confirms in `services/order-api/src/main/java/com/compudelivery/orders/messaging/RabbitMqConfiguration.java`
- [ ] T019 [P] Implement the read-only catalog and demo-identity endpoints (the latter returning a `CLIENT`/`OPERATOR` role discriminator) in `services/order-api/src/main/java/com/compudelivery/orders/catalog/CatalogController.java` and `services/order-api/src/main/java/com/compudelivery/orders/identity/DemoIdentityController.java`
- [ ] T020 [P] Add the shared frontend API types, identity-header-attaching fetch client, `IdentityContext` (selected identity plus its role), router shell, and global styles in `apps/order-portal/src/api/types.ts`, `apps/order-portal/src/api/client.ts`, `apps/order-portal/src/context/IdentityContext.tsx`, `apps/order-portal/src/App.tsx`, and `apps/order-portal/src/index.css`
- [ ] T021 [P] Add the frontend mirror of the four-stage transition map with next-stage labels and `active`/`cancellable` helpers in `apps/order-portal/src/lib/lifecycle.ts`

**Checkpoint**: Foundation ready — the lifecycle is unrepresentable outside its four stages, identity and error handling are uniform, and user story implementation can begin.

---

## Phase 3: User Story 1 - Submit Bulk Order and Receive Contract-Priced Net Total (Priority: P1) 🎯 MVP

**Goal**: Let a client submit a bulk order, receive a Net Total derived exclusively from currently effective contract terms, edit line items while the order is Intake or Processing, and have each intake publish an auditable `OrderIntaken` event.

**Independent Test**: Submit an order for a seeded client with active terms and verify HTTP 201, `INTAKE` status, Gross Total, the discount snapshot, and Net Total; verify missing/ambiguous/expired terms return 422 with no order created; verify zero/negative quantities, unknown SKUs, and empty orders are rejected; verify a line-item edit in `INTAKE`/`PROCESSING` reprices from current terms and appends calculation history.

### Implementation for User Story 1

- [ ] T022 [P] [US1] Implement the currently-effective contract terms lookup with zero-row (missing/expired) and multi-row (ambiguous) detection in `services/order-api/src/main/java/com/compudelivery/orders/client/ContractTermsRepository.java` and `services/order-api/src/main/java/com/compudelivery/orders/client/ContractTermsService.java` (FR-004, Constitution II)
- [ ] T023 [P] [US1] Implement Gross Total and Net Total calculation from catalog list-price snapshots with the applied discount percentage recorded in `services/order-api/src/main/java/com/compudelivery/orders/pricing/PricingService.java` (FR-002, FR-003)
- [ ] T024 [P] [US1] Implement the create-order and replace-line-items request DTOs with non-empty-order and per-line quantity validation in `services/order-api/src/main/java/com/compudelivery/orders/order/CreateOrderRequest.java`, `services/order-api/src/main/java/com/compudelivery/orders/order/ReplaceLineItemsRequest.java`, and `services/order-api/src/main/java/com/compudelivery/orders/order/LineItemInput.java` (FR-014, FR-015)
- [ ] T025 [US1] Implement transactional order creation — catalog SKU resolution and price snapshotting, `INTAKE` status, the initial `LifecycleTransition` (`null → INTAKE`, actor `SYSTEM`) and `NetTotalCalculation` (`trigger = INTAKE`) rows — in `services/order-api/src/main/java/com/compudelivery/orders/order/OrderCreationService.java` (FR-001–FR-004, FR-007)
- [ ] T026 [US1] Implement `POST /api/orders` accepting `X-Client-Id` exclusively, returning `OrderDetail` on 201 and ProblemDetail on 400/422, in `services/order-api/src/main/java/com/compudelivery/orders/order/OrderController.java`
- [ ] T027 [US1] Implement the `OrderIntaken` payload, the Spring application event raised inside the intake transaction, and the `AFTER_COMMIT` RabbitMQ publisher in `services/order-api/src/main/java/com/compudelivery/orders/messaging/OrderIntaken.java`, `services/order-api/src/main/java/com/compudelivery/orders/messaging/OrderIntakenEvent.java`, and `services/order-api/src/main/java/com/compudelivery/orders/messaging/OrderIntakenPublisher.java`
- [ ] T028 [US1] Implement client-scoped line-item replacement that rejects edits once `net_total_locked_at` is set (409), reprices from currently effective terms, and appends a `LINE_ITEM_EDIT` calculation row under optimistic locking in `services/order-api/src/main/java/com/compudelivery/orders/order/LineItemEditService.java` (FR-017, FR-025)
- [ ] T029 [US1] Implement `PUT /api/orders/{orderId}/line-items` with generic client-scoped 404, locked-order 409, and contract-term 422 responses in `services/order-api/src/main/java/com/compudelivery/orders/order/OrderController.java`
- [ ] T030 [P] [US1] Implement the fixed catalog table with per-item quantity entry and live line subtotals in `apps/order-portal/src/components/CatalogTable.tsx` (FR-020)
- [ ] T031 [P] [US1] Implement the pricing summary panel in order — Gross Subtotal, Contract Discount, Freight & Logistics (always "Waived", informational only), Final Net Total — updating live as quantities change in `apps/order-portal/src/components/PricingSummary.tsx` (FR-021)
- [ ] T032 [P] [US1] Implement the 4-step lifecycle stepper (Intake, Processing, Shipped, Final Delivery) with completed/current/upcoming states in `apps/order-portal/src/components/LifecycleStepper.tsx` (FR-022)
- [ ] T033 [US1] Wire the order detail/create page submit and edit flows to the API, blocking submission when total quantity is zero and surfacing contract-term and validation failures clearly, in `apps/order-portal/src/pages/OrderDetailPage.tsx` and `apps/order-portal/src/api/orders.ts` (FR-024)
- [ ] T034 [US1] Validate the User Story 1 submit, 422 contract-term, line-item edit, and `OrderIntaken` scenarios from `specs/001-bulk-hardware-orders/quickstart.md` (steps 3 and 9)

**Checkpoint**: A client can independently submit and edit a contract-priced order; every calculation is recorded append-only and each intake publishes exactly one post-commit event.

---

## Phase 4: User Story 2 - View Order History and Status (Priority: P2)

**Goal**: Let a client see only their own orders, partitioned into Active Orders (Intake, Processing, Shipped) and an Order History Log (Final Delivery, Cancelled) with no order appearing twice.

**Independent Test**: Seed orders for two clients, then verify each client's list contains only its own orders with correct status and Net Total, that a Shipped order appears under Active Orders only while a Delivered order appears under Order History only, and that another client's order ID returns a 404 identical to an unknown ID.

### Implementation for User Story 2

- [ ] T035 [US2] Implement client-scoped repository methods that always include `client_id` in the `WHERE` clause (`findAllByClientId`, `findByIdAndClientId`) with no nullable-scope parameter in `services/order-api/src/main/java/com/compudelivery/orders/order/BulkOrderRepository.java` (FR-009, FR-018, Constitution III)
- [ ] T036 [US2] Implement the client-scoped order summary and detail query service returning line items, totals, derived flags, and append-only lifecycle transitions in `services/order-api/src/main/java/com/compudelivery/orders/order/OrderQueryService.java` (FR-008)
- [ ] T037 [US2] Implement `GET /api/orders` and `GET /api/orders/{orderId}` for a client identity, returning the generic not-found response for both nonexistent and other-clients' orders, in `services/order-api/src/main/java/com/compudelivery/orders/order/OrderController.java` (FR-009, FR-018)
- [ ] T038 [P] [US2] Implement the portal header with title, client name / contract reference badge, and the demo identity switcher populated from `GET /api/demo-identities` in `apps/order-portal/src/components/PortalHeader.tsx` and `apps/order-portal/src/components/IdentitySwitcher.tsx` (FR-019)
- [ ] T039 [P] [US2] Implement the dashboard page partitioning orders on the derived `active` flag into Active Orders cards (each with the 4-step stepper) and the Order History Log with per-entry status indicators, plus the start-new-order navigation, in `apps/order-portal/src/pages/DashboardPage.tsx` and `apps/order-portal/src/components/OrderHistoryList.tsx` (FR-022, FR-023)
- [ ] T040 [US2] Wire dashboard and detail data loading to the identity-aware order API with loading, empty, generic-not-found, and error states in `apps/order-portal/src/api/orders.ts`, `apps/order-portal/src/pages/DashboardPage.tsx`, and `apps/order-portal/src/pages/OrderDetailPage.tsx`
- [ ] T041 [US2] Validate the User Story 2 client-scoped history, detail, 404-parity, and Active/History partition scenarios from `specs/001-bulk-hardware-orders/quickstart.md` (step 4)

**Checkpoint**: A selected client can navigate dashboard → detail, sees each of their orders in exactly one section, and cannot reach another client's data.

---

## Phase 5: User Story 3 - Cancel an Order Within the Cancellation Window (Priority: P2)

**Goal**: Let the owning client cancel an order while it is Intake or Processing, and reject — explicitly and by stated reason — any cancellation once the order has Shipped, been delivered, or already been cancelled.

**Independent Test**: Cancel an Intake/Processing order and verify `CANCELLED`, a cancellation timestamp and record, `active: false`, and `cancellable: false`; verify a Shipped order reports `active: true, cancellable: false` and its cancellation is rejected with 409 stating it has shipped; verify duplicate, delivered, and cross-client attempts are rejected, the last as a generic 404.

### Implementation for User Story 3

- [ ] T042 [US3] Implement client-scoped transactional cancellation gated on `isWithinCancellationWindow()`, producing three distinct 409 reasons (shipped / delivered / already cancelled), one `CancellationRecord`, the `→ CANCELLED` transition row, and `cancelled_at`, under optimistic locking, in `services/order-api/src/main/java/com/compudelivery/orders/order/OrderCancellationService.java` (FR-010, FR-011, FR-016, Constitution III)
- [ ] T043 [US3] Implement `POST /api/orders/{orderId}/cancel` accepting `X-Client-Id` exclusively, with generic 404 and reason-bearing 409 ProblemDetail responses, in `services/order-api/src/main/java/com/compudelivery/orders/order/OrderController.java`
- [ ] T044 [P] [US3] Implement the cancel control — enabled only when the order payload's `cancellable` is true and requiring explicit confirmation, otherwise visible but disabled with an inline explanation that the order has shipped and can no longer be cancelled — in `apps/order-portal/src/components/CancelOrderButton.tsx` (FR-022)
- [ ] T045 [US3] Wire the cancellation flow, post-cancel refresh, and 409 conflict/state-changed messaging into the dashboard and detail pages in `apps/order-portal/src/api/orders.ts`, `apps/order-portal/src/pages/DashboardPage.tsx`, and `apps/order-portal/src/pages/OrderDetailPage.tsx`
- [ ] T046 [P] [US3] Render cancelled orders with a visually distinct Order History status indicator and a terminated stepper state in `apps/order-portal/src/components/OrderHistoryList.tsx` and `apps/order-portal/src/components/LifecycleStepper.tsx` (FR-023)
- [ ] T047 [US3] Validate the User Story 3 cancellation, window-closed, duplicate, cross-client, and first-committed-wins race scenarios from `specs/001-bulk-hardware-orders/quickstart.md` (steps 5 and 7)

**Checkpoint**: An owning client can cancel exactly once inside the window; shipped, delivered, cancelled, and foreign orders are each rejected with the correct, explicit response.

---

## Phase 6: User Story 4 - Progress an Order Through Fulfillment Stages (Priority: P3)

**Goal**: Let an operator identity advance an order one stage at a time along Intake → Processing → Shipped → Final Delivery from the existing two pages, see every client's orders attributed to its owner, and have entry to Shipped lock the Net Total and close the Cancellation Window.

**Independent Test**: With an operator identity selected, advance one order through each stage via the order card's advance control, verifying the recorded transition, `nextStatus` naming the following stage and becoming null at Final Delivery, and `netTotalLocked: true` / `cancellable: false` on entering Shipped; verify skips, backward moves, terminal changes, and client-identity attempts are rejected, an out-of-lifecycle target status returns 400, and the operator dashboard lists all clients' orders.

### Implementation for User Story 4

- [ ] T048 [US4] Implement advance validation against the foundational transition map, requiring `targetStatus` to equal the order's current `nextStatus` and rejecting skips, backward moves, and any change from `FINAL_DELIVERY`/`CANCELLED` with 409, in `services/order-api/src/main/java/com/compudelivery/orders/order/OrderLifecycleService.java` (FR-005, FR-006)
- [ ] T049 [US4] Implement transactional operator status advancement that appends the `OPERATOR`-actor transition row and, on entering `SHIPPED`, stamps `net_total_locked_at` and closes the Cancellation Window in one handler, under optimistic locking, in `services/order-api/src/main/java/com/compudelivery/orders/order/OrderStatusService.java` (FR-007, FR-010, FR-016, FR-017)
- [ ] T050 [US4] Implement `POST /api/orders/{orderId}/status` accepting `X-Operator-Id` exclusively, with the `AdvanceStatusRequest` body rejecting unknown enum values at deserialization (400) and invalid transitions as 409, in `services/order-api/src/main/java/com/compudelivery/orders/order/OrderController.java` (FR-012, FR-026)
- [ ] T051 [US4] Implement operator-scoped repository methods that take no `clientId` parameter (`findAllForOperator`, `findByIdForOperator`) and project each order's owning `clientId` and client display name in `services/order-api/src/main/java/com/compudelivery/orders/order/BulkOrderRepository.java` and `services/order-api/src/main/java/com/compudelivery/orders/order/OrderQueryService.java` (FR-027, Constitution III)
- [ ] T052 [US4] Extend `GET /api/orders` and `GET /api/orders/{orderId}` so an operator identity resolves orders across all clients with owner attribution, while both endpoints still reject neither-header and both-headers requests with 400, in `services/order-api/src/main/java/com/compudelivery/orders/order/OrderController.java` (FR-027)
- [ ] T053 [P] [US4] Implement the role-conditional per-order control — client role renders the cancel control, operator role renders "Advance to &lt;next stage&gt;" for non-terminal orders, and terminal orders render neither — in `apps/order-portal/src/components/OrderCardActions.tsx` (FR-022, FR-026)
- [ ] T054 [US4] Render operator mode on the existing pages: label each listed order with its owning client and hide the order-creation and line-item-editing controls for operator identities, in `apps/order-portal/src/pages/DashboardPage.tsx` and `apps/order-portal/src/pages/OrderDetailPage.tsx` (FR-026, FR-027)
- [ ] T055 [US4] Make line items and pricing read-only once the order payload reports `netTotalLocked` in `apps/order-portal/src/pages/OrderDetailPage.tsx`, `apps/order-portal/src/components/CatalogTable.tsx`, and `apps/order-portal/src/components/PricingSummary.tsx` (FR-017, FR-025)
- [ ] T056 [US4] Wire the advance action, post-advance refresh, and 409 state-changed feedback into the portal in `apps/order-portal/src/api/orders.ts` and `apps/order-portal/src/pages/DashboardPage.tsx`
- [ ] T057 [US4] Validate the User Story 4 lifecycle, invalid-transition, out-of-lifecycle 400, client-rejection, operator cross-client scope, and role-conditional UI scenarios from `specs/001-bulk-hardware-orders/quickstart.md` (steps 6, 6a, and 8)

**Checkpoint**: Operators drive the lifecycle one stage at a time with no skips or reversals, Shipped simultaneously locks pricing and closes cancellation, and every transition remains auditable.

---

## Phase 7: Polish & Cross-Cutting Concerns

**Purpose**: Automated test suites, deployment artifacts, documentation, accessibility, and end-to-end verification across all stories.

- [ ] T058 [P] Add unit tests for the transition map, terminal states, and the `isActive`/`isWithinCancellationWindow`/`nextStatus` predicates — including the absence of any backordered member — in `services/order-api/src/test/java/com/compudelivery/orders/unit/OrderLifecycleServiceTest.java`
- [ ] T059 [P] Add unit tests for Gross/Net Total calculation and for missing, ambiguous, and expired contract terms in `services/order-api/src/test/java/com/compudelivery/orders/unit/PricingServiceTest.java` and `services/order-api/src/test/java/com/compudelivery/orders/unit/ContractTermsServiceTest.java`
- [ ] T060 [P] Add Testcontainers integration tests (PostgreSQL 17 + RabbitMQ) covering intake with published `OrderIntaken`, client scoping and 404 parity, the cancellation window, operator advancement and the Shipped lock, operator cross-client reads, and first-committed-wins 409, in `services/order-api/src/test/java/com/compudelivery/orders/integration/OrderApiIntegrationTest.java`
- [ ] T061 [P] Add Vitest/React Testing Library tests for all role × status combinations of `OrderCardActions` and for the dashboard's Active/History partition in `apps/order-portal/tests/OrderCardActions.test.tsx` and `apps/order-portal/tests/DashboardPage.test.tsx`
- [ ] T062 [P] Add Kubernetes Deployment and Service templates for the API in `deploy/helm/order-api/templates/deployment.yaml` and `deploy/helm/order-api/templates/service.yaml`
- [ ] T063 [P] Add Kubernetes Deployment and Service templates for the portal in `deploy/helm/order-portal/templates/deployment.yaml` and `deploy/helm/order-portal/templates/service.yaml`
- [ ] T064 [P] Add readiness/liveness probes and PostgreSQL/RabbitMQ connection wiring in `deploy/helm/order-api/templates/configmap.yaml` and `deploy/helm/order-api/values.yaml`
- [ ] T065 [P] Add the portal API base URL configuration and production static-serving configuration in `apps/order-portal/.env.example` and `apps/order-portal/nginx.conf`
- [ ] T066 [P] Apply accessibility, responsive-layout, and error-state polish across `apps/order-portal/src/index.css`, `apps/order-portal/src/components/`, and `apps/order-portal/src/pages/`
- [ ] T067 [P] Add springdoc `@Operation`/`@Schema` annotations and verify the generated OpenAPI document matches `specs/001-bulk-hardware-orders/contracts/openapi.yaml` — in particular a five-member `OrderStatus` with no backordered value — across `services/order-api/src/main/java/com/compudelivery/orders/`
- [ ] T068 Run the full backend unit/integration, frontend, Docker Compose, quickstart, and Helm/k3s validation sequence in `specs/001-bulk-hardware-orders/quickstart.md` (steps 10 and 11)
- [ ] T069 Update local-run and implementation instructions for `services/order-api/`, `apps/order-portal/`, and `deploy/` in `README.md`

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — can start immediately
- **Foundational (Phase 2)**: Depends on Setup — BLOCKS all user stories
- **User Stories (Phases 3–6)**: All depend on Foundational completion; then proceed in parallel (if staffed) or sequentially in priority order P1 → P2 → P2 → P3
- **Polish (Phase 7)**: Depends on all desired user stories being complete

### User Story Dependencies

- **User Story 1 (P1)**: Depends only on Foundational. No dependency on another story.
- **User Story 2 (P2)**: Depends only on Foundational. Reads orders that US1 creates, but is independently testable against Flyway-seeded or API-created orders.
- **User Story 3 (P2)**: Depends only on Foundational. Cancels an order it can create via US1's endpoint; the cancellation path itself shares no code with US1.
- **User Story 4 (P3)**: Depends only on Foundational. Extends the US2 list/detail endpoints with an operator branch (T051, T052) — sequence T051/T052 after T035–T037 if both stories are in flight, since they touch `BulkOrderRepository.java`, `OrderQueryService.java`, and `OrderController.java`.

### Within Each User Story

- Repositories and DTOs before services; services before controllers; backend endpoints before the frontend calls that consume them
- Shared components (`LifecycleStepper`, `OrderCardActions`) before the pages that compose them
- The story's `quickstart.md` validation task last

### Story Completion Order

1. User Story 1 (P1) — MVP
2. User Story 2 (P2)
3. User Story 3 (P2)
4. User Story 4 (P3)

---

## Parallel Opportunities

- Setup: T002–T008 all run in parallel after T001
- Foundational: T010–T012 and T016, T017, T019–T021 run in parallel; T009 precedes T010/T012, T013 precedes T014/T017
- US1: T022, T023, T024 in parallel; frontend T030, T031, T032 in parallel with each other and with the backend tasks
- US2: T038 and T039 in parallel with the backend chain T035 → T036 → T037
- US3: T044 and T046 in parallel with the backend chain T042 → T043
- US4: T053 in parallel with the backend chain T048 → T049 → T050 and with T051 → T052
- Polish: T058–T067 all run in parallel; T068 and T069 come last
- Across stories: once Phase 2 is complete, US1–US4 can be staffed in parallel, observing the `OrderController.java` / `OrderQueryService.java` / `BulkOrderRepository.java` contention noted above

---

## Parallel Examples

### User Story 1

```bash
Task: "Implement currently-effective contract terms lookup in services/order-api/src/main/java/com/compudelivery/orders/client/ContractTermsService.java"
Task: "Implement Gross/Net Total calculation in services/order-api/src/main/java/com/compudelivery/orders/pricing/PricingService.java"
Task: "Implement create-order request DTOs in services/order-api/src/main/java/com/compudelivery/orders/order/CreateOrderRequest.java"
Task: "Implement catalog table with quantity entry in apps/order-portal/src/components/CatalogTable.tsx"
Task: "Implement pricing summary panel in apps/order-portal/src/components/PricingSummary.tsx"
Task: "Implement the 4-step lifecycle stepper in apps/order-portal/src/components/LifecycleStepper.tsx"
```

### User Story 2

```bash
Task: "Implement portal header and demo identity switcher in apps/order-portal/src/components/IdentitySwitcher.tsx"
Task: "Implement dashboard Active/History partition in apps/order-portal/src/pages/DashboardPage.tsx"
```

### User Story 3

```bash
Task: "Implement the enabled/disabled cancel control in apps/order-portal/src/components/CancelOrderButton.tsx"
Task: "Render cancelled-order history indicator in apps/order-portal/src/components/OrderHistoryList.tsx"
```

### User Story 4

```bash
Task: "Implement role-conditional OrderCardActions in apps/order-portal/src/components/OrderCardActions.tsx"
Task: "Implement operator-scoped repository methods in services/order-api/src/main/java/com/compudelivery/orders/order/BulkOrderRepository.java"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup
2. Complete Phase 2: Foundational (CRITICAL — blocks all stories)
3. Complete Phase 3: User Story 1
4. **STOP and VALIDATE**: run `quickstart.md` step 3 (submit, 422, edit) and step 9 (event)
5. Demo the contract-priced intake path

### Incremental Delivery

1. Setup + Foundational → foundation ready
2. Add User Story 1 → validate → demo (MVP: submit and price an order)
3. Add User Story 2 → validate → demo (own-orders dashboard with Active/History partition)
4. Add User Story 3 → validate → demo (cancel inside the window, explicit rejection outside it)
5. Add User Story 4 → validate → demo (operator advancement, cross-client view, Shipped lock)
6. Phase 7 → automated suites, k3s deployment, full quickstart pass

### Parallel Team Strategy

1. Team completes Setup + Foundational together
2. Then: Developer A on US1, Developer B on US2, Developer C on US3, Developer D on US4 — with US4's T051/T052 sequenced after US2's T035–T037 to avoid conflicting edits to the shared repository, query service, and controller files

---

## Completion Criteria

- All 69 tasks checked off
- `quickstart.md` steps 3–9 pass against the locally running stack, and steps 10–11 pass in k3s
- The generated OpenAPI document matches `contracts/openapi.yaml`, with `OrderStatus` holding exactly `INTAKE, PROCESSING, SHIPPED, FINAL_DELIVERY, CANCELLED`
- No persisted row, API payload, or UI element can express a lifecycle state outside the constitutional four plus `CANCELLED`
- Every lifecycle transition and Net Total calculation is retrievable as append-only history (SC-005)
- No client identity can read or act on another client's order (SC-006); operator identities are intentionally cross-client per FR-027

---

## Notes

- `[P]` marks tasks touching different files with no dependency on incomplete work
- `[Story]` labels map tasks to spec user stories for traceability; Setup, Foundational, and Polish tasks carry no story label
- Commit after each task or logical group; stop at any checkpoint to validate a story independently
- Reserved modules `services/warehouse-api/`, `services/invoice-api/`, `apps/warehouse-portal/`, and `apps/invoice-portal/` are intentionally not scaffolded by any task here
