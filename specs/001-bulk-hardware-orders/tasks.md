---

description: "Task list for Bulk Hardware Order Management"
---

# Tasks: Bulk Hardware Order Management

**Input**: Design documents from `/specs/001-bulk-hardware-orders/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/openapi.yaml, contracts/events.md, quickstart.md

**Tests**: Included. `research.md` #11 and the "Testing" row of `plan.md`'s Technical Context specify JUnit5/Mockito/AssertJ + Testcontainers (backend) and Vitest + React Testing Library (frontend) as part of this feature's stack, and `quickstart.md` step 10 exercises them; test tasks below implement that stack per user story.

**Organization**: Tasks are grouped by user story (from spec.md, priorities P1/P2/P2/P3) to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (US1, US2, US3, US4)
- Every task lists its exact file path(s)

## Path Conventions

Per `plan.md`'s Project Structure (web monorepo):
- Backend: `services/order-api/src/main/java/com/compudelivery/orders/...`, tests in `services/order-api/src/test/java/com/compudelivery/orders/{unit,integration}/...`
- Frontend: `apps/order-portal/src/...`, tests in `apps/order-portal/tests/...`
- Deploy: `deploy/docker-compose.yml`, `deploy/helm/order-api/`, `deploy/helm/order-portal/`, `deploy/loadtest/` (k6 scripts)
- `services/warehouse-api/`, `services/invoice-api/`, `apps/warehouse-portal/`, `apps/invoice-portal/` are reserved directory slots for future features and are **not** created by any task below.

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Repository/module scaffolding so the backend and frontend projects build and run.

- [X] T001 Create root Maven reactor `pom.xml` at repository root, aggregating modules under `services/`
- [X] T002 [P] Scaffold `services/order-api/pom.xml` (Spring Boot 3.5 parent; Web, Validation, Data JPA, Actuator, AMQP/RabbitMQ, Lombok, Flyway, springdoc-openapi dependencies; Java 25 target)
- [X] T003 [P] Configure `services/order-api/src/main/resources/application.yml` (PostgreSQL datasource, RabbitMQ connection, Flyway enabled, springdoc-openapi path)
- [X] T004 [P] Scaffold `apps/order-portal/` as a Vite + React 19 + TypeScript project (`package.json`, `vite.config.ts`, `tsconfig.json`) with Tailwind CSS configured (`tailwind.config.js`, `postcss.config.js`) and React Router installed
- [X] T005 [P] Create `deploy/docker-compose.yml` declaring `postgres:17` and `rabbitmq:3-management` services for local dev
- [X] T006 [P] Configure backend build-time formatting/linting (Spotless or Checkstyle plugin) in `services/order-api/pom.xml`
- [X] T007 [P] Configure frontend linting/formatting (ESLint + Prettier configs) in `apps/order-portal/.eslintrc.cjs` and `apps/order-portal/.prettierrc`

**Checkpoint**: `./mvnw -q compile` succeeds from repo root; `npm install && npm run dev` starts the (still empty) portal.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Schema, seed data, domain entities, identity handling, lifecycle rules, error handling, and messaging topology that every user story depends on.

**⚠️ CRITICAL**: No user story work can begin until this phase is complete.

- [X] T008 Flyway migration for reference tables (`enterprise_client`, `operator`, `contract_discount_terms`, `hardware_catalog_item`) in `services/order-api/src/main/resources/db/migration/V1__reference_schema.sql`
- [X] T009 Flyway migration for order tables (`bulk_order` incl. `version` column, `line_item`, `lifecycle_transition`, `net_total_calculation`, `cancellation_record`, with FKs and the composite index `bulk_order(client_id, created_at DESC, id DESC)`) in `services/order-api/src/main/resources/db/migration/V2__order_schema.sql`
- [X] T010 Flyway seed migration for demo identities per FR-027 (`ACME-001`, `GLOBEX-002` valid differing discounts; `NOTERMS-003` missing terms; `EXPIRED-004` expired terms; `AMBIGUOUS-005` ambiguous terms; operator `OPS-1`) in `services/order-api/src/main/resources/db/migration/V3__seed_identities.sql`
- [X] T011 Flyway seed migration for the fixed Hardware Catalog (~10–20 SKUs matching `figma-designs/create-order-page.html`) in `services/order-api/src/main/resources/db/migration/V4__seed_catalog.sql`
- [X] T012 [P] Create `OrderStatus` enum (`INTAKE, PROCESSING, BACKORDERED, SHIPPED, FINAL_DELIVERY, CANCELLED`) in `services/order-api/src/main/java/com/compudelivery/orders/order/OrderStatus.java`
- [X] T013 [P] Create `EnterpriseClient` JPA entity and repository in `services/order-api/src/main/java/com/compudelivery/orders/client/EnterpriseClient.java` and `EnterpriseClientRepository.java`
- [X] T014 [P] Create `ContractDiscountTerms` JPA entity and repository with a "current terms for client at instant" query in `services/order-api/src/main/java/com/compudelivery/orders/client/ContractDiscountTerms.java` and `ContractDiscountTermsRepository.java`
- [X] T015 [P] Create `HardwareCatalogItem` JPA entity and repository in `services/order-api/src/main/java/com/compudelivery/orders/catalog/HardwareCatalogItem.java` and `HardwareCatalogItemRepository.java`
- [X] T016 [P] Create `BulkOrder` JPA entity (with `@Version`) and repository with a client-scoped, newest-first paged query in `services/order-api/src/main/java/com/compudelivery/orders/order/BulkOrder.java` and `BulkOrderRepository.java`
- [X] T017 [P] Create `LineItem` JPA entity and repository in `services/order-api/src/main/java/com/compudelivery/orders/order/LineItem.java` and `LineItemRepository.java`
- [X] T018 [P] Create `LifecycleTransition` append-only JPA entity and repository in `services/order-api/src/main/java/com/compudelivery/orders/order/LifecycleTransition.java` and `LifecycleTransitionRepository.java`
- [X] T019 [P] Create `NetTotalCalculation` append-only JPA entity and repository in `services/order-api/src/main/java/com/compudelivery/orders/pricing/NetTotalCalculation.java` and `NetTotalCalculationRepository.java`
- [X] T020 [P] Create `CancellationRecord` JPA entity and repository in `services/order-api/src/main/java/com/compudelivery/orders/order/CancellationRecord.java` and `CancellationRecordRepository.java`
- [X] T021 Implement `CallerIdentityFilter` (`OncePerRequestFilter` reading `X-Client-Id`/`X-Operator-Id`, resolving against seeded tables, 400 on missing/unknown) in `services/order-api/src/main/java/com/compudelivery/orders/identity/CallerIdentityFilter.java` (depends on T013)
- [X] T022 Implement `CallerIdentity` type and its `HandlerMethodArgumentResolver` for controller injection in `services/order-api/src/main/java/com/compudelivery/orders/identity/CallerIdentity.java` and `CallerIdentityArgumentResolver.java` (depends on T021)
- [X] T023 Implement `OrderLifecycleService` with the static allowed-transitions map (FR-005, FR-006) in `services/order-api/src/main/java/com/compudelivery/orders/order/OrderLifecycleService.java` (depends on T012, T018)
- [X] T024 Implement global exception handling (`@ControllerAdvice` producing `ProblemDetail` for 4xx and `ConflictProblemDetail` carrying `currentStatus` for `OptimisticLockException` → 409) in `services/order-api/src/main/java/com/compudelivery/orders/web/GlobalExceptionHandler.java`
- [X] T025 Declare RabbitMQ topology (`order.events` durable topic exchange; `order.events.intaken` durable queue bound with routing key `order.intaken`) in `services/order-api/src/main/java/com/compudelivery/orders/messaging/RabbitTopologyConfig.java`
- [X] T026 [P] Implement `GET /api/demo-identities` in `services/order-api/src/main/java/com/compudelivery/orders/identity/DemoIdentityController.java` (depends on T013)
- [X] T027 [P] Implement `GET /api/catalog` in `services/order-api/src/main/java/com/compudelivery/orders/catalog/CatalogController.java` (depends on T015)
- [X] T028 [P] Create `IdentityContext` (selected demo client/operator identity, role) in `apps/order-portal/src/context/IdentityContext.tsx`
- [X] T029 [P] Create the API fetch wrapper attaching `X-Client-Id`/`X-Operator-Id` headers in `apps/order-portal/src/api/client.ts`
- [X] T030 Set up React Router with `DashboardPage`/`OrderDetailPage` routes and the shared header (title, identity switcher slot, client badge) in `apps/order-portal/src/App.tsx` (depends on T028, T029)
- [X] T031 [P] Build `IdentitySwitcher` component fetching `GET /api/demo-identities` in `apps/order-portal/src/components/IdentitySwitcher.tsx` (depends on T029)

**Checkpoint**: Backend boots with seeded data and answers `GET /api/demo-identities`/`GET /api/catalog`; frontend shell renders the header with a working identity switcher. User story implementation can now begin.

---

## Phase 3: User Story 1 - Submit Bulk Order and Receive Contract-Priced Net Total (Priority: P1) 🎯 MVP

**Goal**: An enterprise client submits a bulk order; the system validates it, applies the client's current contract discount, returns Net Total, and creates the order in Intake — or blocks it with a clear reason. Clients may also edit Line Items while Intake/Processing, with recalculation.

**Independent Test**: Submit a bulk order for a client with known contract terms and verify Net Total = Gross Total minus the contract discount and the order is recorded in Intake; submit for a client with missing/expired/ambiguous terms and verify a blocked submission with no order created.

### Tests for User Story 1

- [X] T032 [P] [US1] Unit test for `PricingService` rounding (full-precision discount arithmetic, round-half-up only on the final Net Total; FR-003, `research.md` #13) in `services/order-api/src/test/java/com/compudelivery/orders/unit/PricingServiceTest.java`
- [X] T033 [P] [US1] Unit test for the "current contract terms" lookup covering valid/missing/expired/ambiguous-overlap/percentage-out-of-(0,100)-range cases (FR-004) in `services/order-api/src/test/java/com/compudelivery/orders/unit/ContractDiscountTermsLookupTest.java`
- [X] T034 [P] [US1] Integration test (Testcontainers) for `POST /api/orders`: happy path 201 with correct Net Total, 422 for each blocked-terms condition with no order persisted, 400 for quantity-ceiling/line-item-ceiling/empty-order/unknown-SKU (FR-001–FR-004, FR-014, FR-015, FR-030) in `services/order-api/src/test/java/com/compudelivery/orders/integration/CreateOrderIntegrationTest.java`
- [X] T035 [P] [US1] Integration test (Testcontainers) for `PUT /api/orders/{orderId}/line-items`: recalculation while Intake/Processing, no-auto-merge on duplicate SKU, rejection when terms become invalid (existing Line Items/totals unchanged), rejection past Processing (FR-025, FR-017) in `services/order-api/src/test/java/com/compudelivery/orders/integration/ReplaceLineItemsIntegrationTest.java`
- [X] T036 [P] [US1] Integration test (Testcontainers) confirming an `OrderIntaken` message is published to `order.events`/`order.intaken` only after the creating transaction commits, matching `contracts/events.md`'s schema (`research.md` #7) in `services/order-api/src/test/java/com/compudelivery/orders/integration/OrderIntakenEventIntegrationTest.java`

### Implementation for User Story 1

- [X] T037 [US1] Implement `PricingService` (BigDecimal full precision through the discount step, `RoundingMode.HALF_UP` applied once to the final Net Total) in `services/order-api/src/main/java/com/compudelivery/orders/pricing/PricingService.java`
- [X] T038 [US1] Implement line-item validation (`@Min(1) @Max(10000)` on `LineItemInput.quantity`; `@NotEmpty` min-1-line-item validation rejecting FR-015's empty-order case; custom max-100-entries `MaxLineItemsValidator` — all on `CreateOrderRequest.lineItems`/`ReplaceLineItemsRequest.lineItems`) in `services/order-api/src/main/java/com/compudelivery/orders/order/LineItemInput.java`, `CreateOrderRequest.java`, `ReplaceLineItemsRequest.java`, `MaxLineItemsValidator.java`
- [X] T039 [US1] Implement `OrderIntakeService.createOrder` (validate SKUs/ceilings, look up current contract terms, block per FR-004 before any INSERT, persist `BulkOrder` + `LineItem`s, write the initial `LifecycleTransition` and `NetTotalCalculation` rows) in `services/order-api/src/main/java/com/compudelivery/orders/order/OrderIntakeService.java` (depends on T037, T038)
- [X] T040 [US1] Implement the `OrderIntaken` domain event and its `@TransactionalEventListener(phase = AFTER_COMMIT)` publisher in `services/order-api/src/main/java/com/compudelivery/orders/messaging/OrderIntakenPublisher.java` (depends on T039)
- [X] T041 [US1] Implement `POST /api/orders` in `services/order-api/src/main/java/com/compudelivery/orders/order/OrderController.java` (depends on T039)
- [X] T042 [US1] Implement `OrderEditService.replaceLineItems` (Intake/Processing only, recompute Gross/Net Total from currently effective terms, append a new `NetTotalCalculation` row, reject under FR-004/ceilings/lock with existing data untouched, optimistic-lock 409) in `services/order-api/src/main/java/com/compudelivery/orders/order/OrderEditService.java` (depends on T037, T038)
- [X] T043 [US1] Implement `PUT /api/orders/{orderId}/line-items` in `OrderController.java` (depends on T042)
- [X] T044 [P] [US1] Build `CatalogTable` component (fixed Hardware Catalog Items, per-item quantity entry, live per-line subtotal) in `apps/order-portal/src/components/CatalogTable.tsx`
- [X] T045 [P] [US1] Build `PricingSummary` component (Gross Subtotal, Contract Discount, Freight & Logistics "Waived", Final Net Total, recomputing live as quantities change) in `apps/order-portal/src/components/PricingSummary.tsx`
- [X] T046 [US1] Build `OrderDetailPage` (catalog + pricing summary + submit control blocking zero-total submission per FR-024, plus editing an existing Intake/Processing order's Line Items) in `apps/order-portal/src/pages/OrderDetailPage.tsx` (depends on T044, T045)

**Checkpoint**: User Story 1 is independently functional — a client can submit a priced order and edit it while Intake/Processing.

---

## Phase 4: User Story 2 - View Order History and Status (Priority: P2)

**Goal**: An enterprise client (or an operator viewing that client) sees only that client's own orders — Active Orders with lifecycle status, and a paged, newest-first Order History Log with full transition timestamps.

**Independent Test**: Create orders for two different clients; confirm each client's view shows only its own orders with correct status/Net Total, and that history pages newest-first without skips/duplicates.

### Tests for User Story 2

- [X] T047 [P] [US2] Integration test (Testcontainers) for `GET /api/orders`: 25-per-page newest-first paging with no skip/duplicate across pages, and per-client isolation (FR-008, FR-009) in `services/order-api/src/test/java/com/compudelivery/orders/integration/ListOrdersIntegrationTest.java`
- [X] T048 [P] [US2] Integration test (Testcontainers) for `GET /api/orders/{orderId}`: correct detail incl. transition history, identical generic 404 for a nonexistent order and one owned by a different client (FR-018) in `services/order-api/src/test/java/com/compudelivery/orders/integration/GetOrderIntegrationTest.java`
- [X] T049 [P] [US2] Integration test (Testcontainers) for an operator supplying `X-Operator-Id` + `X-Client-Id` on `GET /api/orders`/`GET /api/orders/{orderId}`, confirming the response matches exactly what that client would see and never widens beyond it (FR-026) in `services/order-api/src/test/java/com/compudelivery/orders/integration/OperatorScopedReadIntegrationTest.java`

### Implementation for User Story 2

- [X] T050 [US2] Implement `OrderHistoryService.listOrders` (client-scoped, `created_at DESC, id DESC`, 25-entry `OrderHistoryPage` envelope with `page`/`pageSize`/`totalCount`/`hasMore`) in `services/order-api/src/main/java/com/compudelivery/orders/order/OrderHistoryService.java` (depends on T016)
- [X] T051 [US2] Implement `OrderQueryService.getOrder` (client-scoped lookup incl. transitions, uniform 404 for not-found-or-not-mine) in `services/order-api/src/main/java/com/compudelivery/orders/order/OrderQueryService.java` (depends on T016, T018)
- [X] T052 [US2] Implement `GET /api/orders` and `GET /api/orders/{orderId}` in `OrderController.java` (depends on T050, T051)
- [X] T053 [P] [US2] Build `LifecycleStepper` component (permanent 5-column Intake/Processing/Backordered/Shipped/Delivered indicator) in `apps/order-portal/src/components/LifecycleStepper.tsx`
- [X] T054 [P] [US2] Build `ActiveOrdersSection` (one card per Active order with `LifecycleStepper`; "No active orders" empty state with a start-order CTA; independent loading/error-with-retry states per FR-028/FR-029) in `apps/order-portal/src/components/ActiveOrdersSection.tsx` (depends on T053)
- [X] T055 [P] [US2] Build `OrderHistoryLog` (status indicator per entry, full lifecycle transition timestamps, "reach older entries" pagination control, "No past orders" empty state, independent loading/error-with-retry states) in `apps/order-portal/src/components/OrderHistoryLog.tsx`
- [X] T056 [US2] Build `DashboardPage` assembling `ActiveOrdersSection` + `OrderHistoryLog` (each tracking its own `idle|loading|error|loaded` fetch state) plus the "start a new order" action navigating to `OrderDetailPage` in `apps/order-portal/src/pages/DashboardPage.tsx` (depends on T054, T055)

**Checkpoint**: User Stories 1 and 2 both work independently — orders can be submitted and then tracked/viewed per-client.

---

## Phase 5: User Story 3 - Cancel an Active Order (Priority: P2)

**Goal**: An enterprise client cancels one of their own Active orders; delivered/already-cancelled/other-client orders are rejected appropriately.

**Independent Test**: Submit an order, cancel it before delivery, verify status becomes Cancelled and it can no longer advance; verify re-cancellation, post-delivery cancellation, and cross-client cancellation are all rejected.

### Tests for User Story 3

- [X] T057 [P] [US3] Integration test (Testcontainers) for `POST /api/orders/{orderId}/cancel`: success from any Active state, 409 on double-cancel, 409 after Final Delivery, 404 for another client's order (FR-010, FR-011, FR-018) in `services/order-api/src/test/java/com/compudelivery/orders/integration/CancelOrderIntegrationTest.java`
- [X] T058 [P] [US3] Integration test (Testcontainers) for FR-016 first-committed-wins: concurrent cancel vs. operator advance-to-Final-Delivery on the same order — exactly one 200, the other 409 with the order's now-current status in `services/order-api/src/test/java/com/compudelivery/orders/integration/ConcurrentConflictIntegrationTest.java`

### Implementation for User Story 3

- [X] T059 [US3] Implement `OrderCancellationService.cancel` (Active-state check, create the `CancellationRecord`, append a `LifecycleTransition` to `CANCELLED`, optimistic-lock 409 carrying `currentStatus`) in `services/order-api/src/main/java/com/compudelivery/orders/order/OrderCancellationService.java` (depends on T023)
- [X] T060 [US3] Implement `POST /api/orders/{orderId}/cancel` in `OrderController.java` (depends on T059)
- [X] T061 [US3] Add a cancel control with an explicit confirmation step to `ActiveOrdersSection` in `apps/order-portal/src/components/ActiveOrdersSection.tsx` (depends on T054)

**Checkpoint**: User Stories 1–3 all work independently.

---

## Phase 6: User Story 4 - Progress an Order Through Fulfillment Stages (Priority: P3)

**Goal**: An operator advances an order through Intake→Processing→Shipped→Final Delivery (and Processing↔Backordered), with skips/backward moves/post-terminal changes rejected.

**Independent Test**: Advance a single order through each defined stage in order, verifying status/history update correctly at each step and Final Delivery is terminal; attempt a skip, a backward move, and a post-terminal change and verify each is rejected; verify Processing↔Backordered works both ways.

### Tests for User Story 4

- [X] T062 [P] [US4] Integration test (Testcontainers) for `POST /api/orders/{orderId}/status`: full forward progression, rejected skip, rejected change after a terminal state, and the Processing↔Backordered reversal succeeding without being treated as backward (FR-006, FR-012) in `services/order-api/src/test/java/com/compudelivery/orders/integration/AdvanceStatusIntegrationTest.java`

### Implementation for User Story 4

- [X] T063 [US4] Implement `OrderLifecycleAdvancementService.advance` (operator-only; delegates to `OrderLifecycleService`'s transition map; appends a `LifecycleTransition`; when the transition target is `SHIPPED`, explicitly set `BulkOrder.net_total_locked_at = now()` per `data-model.md`'s field spec and FR-017/Constitution Principle II; optimistic-lock 409 carrying `currentStatus`) in `services/order-api/src/main/java/com/compudelivery/orders/order/OrderLifecycleAdvancementService.java` (depends on T023)
- [X] T064 [US4] Implement `POST /api/orders/{orderId}/status` in `OrderController.java` (depends on T063)
- [X] T065 [P] [US4] Add operator-only "Advance to next stage" control and a Backordered on-hold toggle (with return to Processing) to `ActiveOrdersSection`, hidden while a client identity is selected, in `apps/order-portal/src/components/ActiveOrdersSection.tsx` (depends on T054)
- [X] T066 [US4] Wire role-based control visibility (client-only controls — cancel, new order, Line Item editing — hidden under an operator identity; operator controls hidden under a client identity) across `DashboardPage`/`OrderDetailPage` via `IdentityContext` in `apps/order-portal/src/context/IdentityContext.tsx` (depends on T028, T056, T046)

**Checkpoint**: All four user stories are independently functional.

---

## Phase 7: Polish & Cross-Cutting Concerns

**Purpose**: Deployment artifacts, remaining frontend test coverage, end-to-end/contract validation, and performance validation against SC-001/SC-007.

- [X] T067 [P] Write `services/order-api/Dockerfile` (Maven build stage → minimal JRE runtime stage)
- [X] T068 [P] Write `apps/order-portal/Dockerfile` (Vite build stage → static-file serving stage)
- [X] T069 [P] Create the `deploy/helm/order-api/` Helm chart (Deployment/Service for the backend image)
- [X] T070 [P] Create the `deploy/helm/order-portal/` Helm chart (Deployment/Service for the frontend image)
- [X] T071 [P] Add Vitest + React Testing Library tests for live pricing recomputation (`CatalogTable`/`PricingSummary`) and empty/loading/error rendering (`ActiveOrdersSection`/`OrderHistoryLog`) in `apps/order-portal/tests/`
- [X] T072 Diff the running service's springdoc-openapi-generated document against `contracts/openapi.yaml` and reconcile any drift
- [X] T073 Run `quickstart.md` steps 1–12 end-to-end against a local `docker-compose` + `mvnw spring-boot:run` + `npm run dev` stack
- [X] T074 Accessibility pass (semantic HTML, labeled form controls) over the identity switcher, catalog quantity controls, lifecycle indicator, status indicators, and cancel confirmation, per the best-effort general-practice bar (no named standard required)
- [X] T075 [P] Write a k6 load test simulating 500 `POST /api/orders` bulk order submissions per day (paced across a representative window, spread across multiple seeded client identities) against a locally running stack, asserting every submission stays under SC-001's 5-second response target and returns a Net Total matching SC-002's 100%-accuracy rounding policy (FR-003), in `deploy/loadtest/bulk-order-submission.js` (SC-007)
- [X] T076 [P] Add elapsed-time assertions to `CreateOrderIntegrationTest.java` (order submission), `ReplaceLineItemsIntegrationTest.java` (Line Item edit recalculation), and `ListOrdersIntegrationTest.java` (a single Order History page fetch), each asserting the request completes in under 5 seconds per SC-001, in `services/order-api/src/test/java/com/compudelivery/orders/integration/`

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — start immediately.
- **Foundational (Phase 2)**: Depends on Setup — BLOCKS all user stories.
- **User Story 1 (Phase 3, P1)**: Depends only on Foundational.
- **User Story 2 (Phase 4, P2)**: Depends only on Foundational; reads orders User Story 1 creates but has its own independently testable read paths.
- **User Story 3 (Phase 5, P2)**: Depends only on Foundational; acts on orders that already exist (from US1) but its cancel logic is independently testable against any Active order.
- **User Story 4 (Phase 6, P3)**: Depends only on Foundational; advances orders that already exist (from US1) but its transition logic is independently testable.
- **Polish (Phase 7)**: Depends on all four user stories being complete.

### User Story Dependencies

- **US1 (P1)**: No dependency on other stories — the MVP.
- **US2 (P2)**: No dependency on other stories' code; needs orders to exist to observe (created via US1 or directly in tests).
- **US3 (P2)**: No dependency on other stories' code; needs an order to exist to cancel.
- **US4 (P3)**: No dependency on other stories' code; needs an order to exist to advance.

### Within Each User Story

- Tests are written before their corresponding implementation task and must fail first.
- Entities/services (Foundational) before story-specific services.
- Services before the controller endpoints that call them.
- Backend endpoint before the frontend component that calls it, where one exists.

### Parallel Opportunities

- All Setup tasks marked [P] (T002–T007) run in parallel once T001 exists.
- Within Foundational, the eight entity/repository tasks (T012–T020) run in parallel; the four frontend foundational tasks (T028, T029, T031, plus T026/T027 on the backend) run in parallel with each other and with the entity tasks.
- All test tasks within a story phase (marked [P]) run in parallel with each other.
- Once Foundational is complete, US1, US2, US3, and US4 can be staffed and built in parallel by different developers — each only reads/writes its own service and controller methods, converging on the shared `OrderController.java` file (a known point of care: land each story's endpoint method as an independent, non-overlapping addition to that file).

---

## Parallel Example: User Story 1

```bash
# Launch all tests for User Story 1 together:
Task: "Unit test for PricingService rounding in services/order-api/src/test/java/.../unit/PricingServiceTest.java"
Task: "Unit test for contract terms lookup in services/order-api/src/test/java/.../unit/ContractDiscountTermsLookupTest.java"
Task: "Integration test for POST /api/orders in services/order-api/src/test/java/.../integration/CreateOrderIntegrationTest.java"
Task: "Integration test for PUT /api/orders/{orderId}/line-items in services/order-api/src/test/java/.../integration/ReplaceLineItemsIntegrationTest.java"
Task: "Integration test for OrderIntaken event publishing in services/order-api/src/test/java/.../integration/OrderIntakenEventIntegrationTest.java"

# Launch the two independent frontend components for User Story 1 together:
Task: "Build CatalogTable component in apps/order-portal/src/components/CatalogTable.tsx"
Task: "Build PricingSummary component in apps/order-portal/src/components/PricingSummary.tsx"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup
2. Complete Phase 2: Foundational (CRITICAL — blocks all stories)
3. Complete Phase 3: User Story 1
4. **STOP and VALIDATE**: submit orders for `ACME-001` (valid), `NOTERMS-003`/`EXPIRED-004`/`AMBIGUOUS-005` (each blocked), confirm Net Total and the 422 reasons
5. Demo if ready

### Incremental Delivery

1. Setup + Foundational → foundation ready
2. Add User Story 1 → validate independently → MVP demo
3. Add User Story 2 → validate independently → demo (history/status visibility)
4. Add User Story 3 → validate independently → demo (cancellation)
5. Add User Story 4 → validate independently → demo (full lifecycle progression)
6. Polish (Phase 7) → deployment artifacts, remaining frontend coverage, full quickstart pass

### Parallel Team Strategy

1. Team completes Setup + Foundational together.
2. Once Foundational is done: Developer A takes US1, Developer B takes US2, Developer C takes US3, Developer D takes US4.
3. Stories complete and integrate independently against the shared `OrderController.java`/`ActiveOrdersSection.tsx` files (coordinate on non-overlapping method/JSX additions).

---

## Notes

- [P] tasks touch different files with no unmet dependency.
- [Story] labels map every user-story-phase task to US1–US4 for traceability back to spec.md.
- Commit after each task or logical group.
- Stop at any checkpoint to validate a story independently before moving to the next.
- No task in Setup/Foundational/Polish carries a [Story] label, per the format rules.
