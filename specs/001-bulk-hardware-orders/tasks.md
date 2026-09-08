---
description: "Executable implementation tasks for Bulk Hardware Order Management"
---

# Tasks: Bulk Hardware Order Management

**Input**: Design documents from `/specs/001-bulk-hardware-orders/`

**Prerequisites**: `plan.md`, `spec.md`, `research.md`, `data-model.md`, `contracts/`, and `quickstart.md`

**Organization**: Tasks are grouped by user story so each increment can be implemented and validated independently after the shared foundation is complete.

**Testing note**: The feature specification defines independent test criteria and acceptance scenarios, but does not request a TDD workflow; test creation is therefore not broken out as separate implementation tasks. Existing unit, integration, frontend, and quickstart verification commands are included in cross-cutting validation.

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Establish the monorepo modules, local dependencies, and build tooling required by the order API and portal.

- [ ] T001 Create the root Maven reactor with `services/order-api` as the only active module in `pom.xml`
- [ ] T002 [P] Scaffold the Spring Boot 3.5 / Java 25 order service module and dependency configuration in `services/order-api/pom.xml`
- [ ] T003 [P] Scaffold the React 19 / TypeScript / Vite portal in `apps/order-portal/package.json`, `apps/order-portal/tsconfig.json`, and `apps/order-portal/vite.config.ts`
- [ ] T004 [P] Configure Tailwind CSS and the portal source/test entry points in `apps/order-portal/tailwind.config.ts`, `apps/order-portal/postcss.config.js`, and `apps/order-portal/src/main.tsx`
- [ ] T005 [P] Add the local PostgreSQL 17 and RabbitMQ management services with health checks in `deploy/docker-compose.yml`
- [ ] T006 [P] Add backend and frontend container build definitions in `services/order-api/Dockerfile` and `apps/order-portal/Dockerfile`
- [ ] T007 [P] Add initial Helm chart metadata and values for the order API in `deploy/helm/order-api/Chart.yaml` and `deploy/helm/order-api/values.yaml`
- [ ] T008 [P] Add initial Helm chart metadata and values for the order portal in `deploy/helm/order-portal/Chart.yaml` and `deploy/helm/order-portal/values.yaml`

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Implement shared persistence, identity, API error handling, and infrastructure that every user story depends on.

**Checkpoint**: No user story work should begin until this phase is complete.

- [ ] T009 Create Flyway schema migrations for clients, operators, contract terms, catalog items, orders, line items, lifecycle transitions, net-total calculations, and cancellation records in `services/order-api/src/main/resources/db/migration/V1__create_order_schema.sql`
- [ ] T010 [P] Seed demo clients, operators, contract discount terms, and fixed catalog items matching the reference designs in `services/order-api/src/main/resources/db/migration/V2__seed_demo_data.sql`
- [ ] T011 [P] Configure PostgreSQL, RabbitMQ, Flyway, actuator, and springdoc settings in `services/order-api/src/main/resources/application.yml`
- [ ] T012 [P] Implement shared JPA base mappings and repositories for seeded clients, operators, contract terms, and catalog items in `services/order-api/src/main/java/com/compudelivery/orders/client/EnterpriseClient.java`, `services/order-api/src/main/java/com/compudelivery/orders/client/Operator.java`, `services/order-api/src/main/java/com/compudelivery/orders/client/ContractDiscountTerms.java`, and `services/order-api/src/main/java/com/compudelivery/orders/catalog/HardwareCatalogItem.java`
- [ ] T013 Implement trusted caller identity resolution for `X-Client-Id` and `X-Operator-Id` in `services/order-api/src/main/java/com/compudelivery/orders/identity/CallerIdentity.java`, `services/order-api/src/main/java/com/compudelivery/orders/identity/CallerIdentityFilter.java`, and `services/order-api/src/main/java/com/compudelivery/orders/identity/CallerIdentityArgumentResolver.java`
- [ ] T014 [P] Implement consistent RFC 7807 exception mapping for validation, not-found, invalid-transition, contract-term, and optimistic-lock conflicts in `services/order-api/src/main/java/com/compudelivery/orders/web/GlobalExceptionHandler.java`
- [ ] T015 Implement shared order persistence entities, repositories, status enum, and append-only history mappings in `services/order-api/src/main/java/com/compudelivery/orders/order/BulkOrder.java`, `services/order-api/src/main/java/com/compudelivery/orders/order/LineItem.java`, `services/order-api/src/main/java/com/compudelivery/orders/order/OrderStatus.java`, `services/order-api/src/main/java/com/compudelivery/orders/order/LifecycleTransition.java`, `services/order-api/src/main/java/com/compudelivery/orders/pricing/NetTotalCalculation.java`, and `services/order-api/src/main/java/com/compudelivery/orders/order/CancellationRecord.java`
- [ ] T016 [P] Configure JSON DTO conventions, decimal serialization, validation, and controller-wide OpenAPI metadata in `services/order-api/src/main/java/com/compudelivery/orders/web/ApiConfiguration.java`
- [ ] T017 Configure RabbitMQ exchange, durable queue, routing key, and publisher-confirm settings in `services/order-api/src/main/java/com/compudelivery/orders/messaging/RabbitMqConfiguration.java`
- [ ] T018 [P] Implement read-only catalog and demo-identity endpoints in `services/order-api/src/main/java/com/compudelivery/orders/catalog/CatalogController.java` and `services/order-api/src/main/java/com/compudelivery/orders/identity/DemoIdentityController.java`
- [ ] T019 [P] Add shared frontend API types, identity context, header-attaching fetch client, router shell, and global styles in `apps/order-portal/src/api/types.ts`, `apps/order-portal/src/api/client.ts`, `apps/order-portal/src/context/IdentityContext.tsx`, `apps/order-portal/src/App.tsx`, and `apps/order-portal/src/index.css`

---

## Phase 3: User Story 1 - Submit Bulk Order and Receive Contract-Priced Net Total (Priority: P1) - MVP

**Goal**: Let a client create a valid bulk order, calculate contract-based pricing, edit it while editable, and receive an auditable `OrderIntaken` event.

**Independent Test**: With a seeded client and active contract terms, submit valid line items and verify HTTP 201, `INTAKE` status, Gross Total, Net Total, and the contract discount snapshot. Verify missing, ambiguous, or expired terms block creation; invalid quantities/SKUs and empty orders are rejected; editing in `INTAKE` or `PROCESSING` recalculates totals and appends calculation history.

### Implementation

- [ ] T020 [P] [US1] Implement catalog lookup, current contract-term selection, ambiguity detection, and contract-pricing calculations in `services/order-api/src/main/java/com/compudelivery/orders/pricing/PricingService.java` and `services/order-api/src/main/java/com/compudelivery/orders/client/ContractTermsRepository.java`
- [ ] T021 [P] [US1] Implement create-order request/response DTOs with line-item and non-empty-order validation in `services/order-api/src/main/java/com/compudelivery/orders/order/CreateOrderRequest.java`, `services/order-api/src/main/java/com/compudelivery/orders/order/OrderDetailResponse.java`, and `services/order-api/src/main/java/com/compudelivery/orders/order/LineItemInput.java`
- [ ] T022 [US1] Implement transactional order creation with catalog price snapshots, Gross/Net Total persistence, initial lifecycle and calculation history, optimistic versioning, and client ownership in `services/order-api/src/main/java/com/compudelivery/orders/order/OrderCreationService.java`
- [ ] T023 [US1] Implement create-order REST endpoint with `X-Client-Id` identity enforcement and ProblemDetail responses in `services/order-api/src/main/java/com/compudelivery/orders/order/OrderController.java`
- [ ] T024 [US1] Implement the after-commit `OrderIntaken` application event, payload mapper, and RabbitMQ publisher in `services/order-api/src/main/java/com/compudelivery/orders/messaging/OrderIntaken.java`, `services/order-api/src/main/java/com/compudelivery/orders/messaging/OrderIntakenPublisher.java`, and `services/order-api/src/main/java/com/compudelivery/orders/messaging/OrderEventListener.java`
- [ ] T025 [US1] Implement editable line-item replacement with quantity/SKU validation, current contract repricing, append-only calculation history, client scoping, and optimistic locking in `services/order-api/src/main/java/com/compudelivery/orders/order/LineItemEditService.java`
- [ ] T026 [US1] Implement `PUT /api/orders/{orderId}/line-items` with generic client-scoped not-found behavior and locked-order conflict handling in `services/order-api/src/main/java/com/compudelivery/orders/order/OrderController.java`
- [ ] T027 [P] [US1] Implement the order-detail/create page catalog quantity inputs and live line-item subtotal calculations in `apps/order-portal/src/pages/OrderDetailPage.tsx` and `apps/order-portal/src/components/CatalogTable.tsx`
- [ ] T028 [P] [US1] Implement the order pricing summary with Gross Subtotal, Contract Discount, informational Waived Freight & Logistics, and Final Net Total in `apps/order-portal/src/components/PricingSummary.tsx`
- [ ] T029 [US1] Connect order creation and editable-order save flows to the API, including empty-order blocking and clear validation errors, in `apps/order-portal/src/pages/OrderDetailPage.tsx` and `apps/order-portal/src/api/orders.ts`
- [ ] T030 [P] [US1] Implement the shared five-column lifecycle stepper in `apps/order-portal/src/components/LifecycleStepper.tsx`
- [ ] T031 [US1] Add order API, pricing, messaging, and portal build/test scripts in `services/order-api/pom.xml` and `apps/order-portal/package.json`
- [ ] T032 [US1] Validate the User Story 1 create, invalid-pricing, edit, and event scenarios from `specs/001-bulk-hardware-orders/quickstart.md`

**Checkpoint**: A client can independently submit and edit a contract-priced order, and a successful intake publishes exactly one after-commit event.

---

## Phase 4: User Story 2 - View Order History and Status (Priority: P2)

**Goal**: Let a client view only its own order summaries and detailed lifecycle/pricing history through the dashboard and detail pages.

**Independent Test**: Seed orders for two clients, then verify each client's list contains only its own orders, detail responses include line items and current status, and an order belonging to another client returns the same generic 404 shape as an unknown order ID.

### Implementation

- [ ] T033 [US2] Implement client-scoped order summary and detail query services with line items and append-only lifecycle history in `services/order-api/src/main/java/com/compudelivery/orders/order/OrderQueryService.java`
- [ ] T034 [US2] Implement `GET /api/orders` and `GET /api/orders/{orderId}` with repository-level client scoping in `services/order-api/src/main/java/com/compudelivery/orders/order/OrderController.java`
- [ ] T035 [P] [US2] Implement the dashboard page active-order cards, history log, status indicators, and new-order navigation in `apps/order-portal/src/pages/DashboardPage.tsx` and `apps/order-portal/src/components/OrderHistoryList.tsx`
- [ ] T036 [US2] Connect dashboard and detail data loading to the shared identity-aware order API in `apps/order-portal/src/api/orders.ts`, `apps/order-portal/src/pages/DashboardPage.tsx`, and `apps/order-portal/src/pages/OrderDetailPage.tsx`
- [ ] T037 [P] [US2] Implement header portal title, selected client badge, and demo identity switcher UI in `apps/order-portal/src/components/IdentitySwitcher.tsx` and `apps/order-portal/src/components/PortalHeader.tsx`
- [ ] T038 [US2] Add dashboard/detail empty, loading, generic-not-found, and API-error states in `apps/order-portal/src/pages/DashboardPage.tsx` and `apps/order-portal/src/pages/OrderDetailPage.tsx`
- [ ] T039 [US2] Validate the User Story 2 client-scoped history and detail scenarios from `specs/001-bulk-hardware-orders/quickstart.md`

**Checkpoint**: A selected client can navigate from dashboard to detail and cannot retrieve another client's order data.

---

## Phase 5: User Story 3 - Cancel an Active Order (Priority: P2)

**Goal**: Let the owning client explicitly confirm cancellation of an active order while preserving cancellation history and conflict semantics.

**Independent Test**: Create an order, cancel it before delivery, verify `CANCELLED` and a cancellation timestamp/record, then verify duplicate, delivered, terminal, cross-client, and concurrent cancellation attempts are rejected correctly.

### Implementation

- [ ] T040 [US3] Implement client-scoped transactional cancellation with allowed-state checks, append-only lifecycle transition, one-time cancellation record, and JPA optimistic locking in `services/order-api/src/main/java/com/compudelivery/orders/order/OrderCancellationService.java`
- [ ] T041 [US3] Implement `POST /api/orders/{orderId}/cancel` with generic client-scoped 404 and specific terminal/conflict 409 responses in `services/order-api/src/main/java/com/compudelivery/orders/order/OrderController.java`
- [ ] T042 [P] [US3] Add dashboard cancel controls with explicit confirmation and status-aware disabled states in `apps/order-portal/src/components/CancelOrderButton.tsx` and `apps/order-portal/src/pages/DashboardPage.tsx`
- [ ] T043 [US3] Connect cancellation responses, refresh behavior, and conflict/error messaging to the portal in `apps/order-portal/src/api/orders.ts` and `apps/order-portal/src/pages/DashboardPage.tsx`
- [ ] T044 [US3] Ensure cancelled orders render a distinct history status and no longer expose lifecycle advancement controls in `apps/order-portal/src/components/OrderHistoryList.tsx` and `apps/order-portal/src/components/LifecycleStepper.tsx`
- [ ] T045 [US3] Validate the User Story 3 cancellation and first-committed-wins scenarios from `specs/001-bulk-hardware-orders/quickstart.md`

**Checkpoint**: An owning client can cancel an eligible order exactly once, while delivered/cancelled/foreign orders remain protected.

---

## Phase 6: User Story 4 - Progress an Order Through Fulfillment Stages (Priority: P3)

**Goal**: Let an identified operator advance orders through the exact lifecycle, including the Processing/Backordered exception, while recording every transition and locking pricing at Shipped.

**Independent Test**: Advance an order through `INTAKE -> PROCESSING -> SHIPPED -> FINAL_DELIVERY`, verify transition history and terminal rejection, then verify `PROCESSING -> BACKORDERED -> PROCESSING`; reject skips, invalid reversals, cancelled-order advancement, and non-operator requests.

### Implementation

- [ ] T046 [US4] Implement the centralized allowed-transition map and actor/status validation in `services/order-api/src/main/java/com/compudelivery/orders/order/OrderLifecycleService.java`
- [ ] T047 [US4] Implement transactional operator status advancement, append-only transition records, Shipped-time Net Total lock, and optimistic-lock conflict handling in `services/order-api/src/main/java/com/compudelivery/orders/order/OrderStatusService.java`
- [ ] T048 [US4] Implement `POST /api/orders/{orderId}/status` with `X-Operator-Id` enforcement and transition ProblemDetail responses in `services/order-api/src/main/java/com/compudelivery/orders/order/OrderController.java`
- [ ] T049 [P] [US4] Add operator identity selection and operator-only lifecycle controls to the order detail page in `apps/order-portal/src/components/OperatorStatusControls.tsx` and `apps/order-portal/src/pages/OrderDetailPage.tsx`
- [ ] T050 [US4] Connect status advancement, Backordered return handling, Shipped read-only behavior, and conflict feedback to the portal in `apps/order-portal/src/api/orders.ts` and `apps/order-portal/src/pages/OrderDetailPage.tsx`
- [ ] T051 [US4] Ensure the five-column lifecycle stepper reflects completed, current, and upcoming states including Backordered in `apps/order-portal/src/components/LifecycleStepper.tsx`
- [ ] T052 [US4] Make line items and pricing read-only after Shipped in `apps/order-portal/src/pages/OrderDetailPage.tsx` and `apps/order-portal/src/components/PricingSummary.tsx`
- [ ] T053 [US4] Configure backend integration verification for PostgreSQL and RabbitMQ Testcontainers in `services/order-api/src/test/java/com/compudelivery/orders/integration/OrderIntegrationTest.java`
- [ ] T054 [US4] Validate the User Story 4 lifecycle, terminal-state, operator restriction, pricing-lock, and concurrency scenarios from `specs/001-bulk-hardware-orders/quickstart.md`

**Checkpoint**: Operators can drive the lifecycle without skips, client edits lock at Shipped, and all transitions remain auditable.

---

## Phase 7: Polish & Cross-Cutting Concerns

**Purpose**: Complete deployment artifacts, documentation, accessibility, performance, and end-to-end verification across all stories.

- [ ] T055 [P] Add Kubernetes Deployment and Service templates for the API in `deploy/helm/order-api/templates/deployment.yaml` and `deploy/helm/order-api/templates/service.yaml`
- [ ] T056 [P] Add Kubernetes Deployment and Service templates for the portal in `deploy/helm/order-portal/templates/deployment.yaml` and `deploy/helm/order-portal/templates/service.yaml`
- [ ] T057 [P] Add readiness/liveness probes, environment values, and PostgreSQL/RabbitMQ connection wiring in `deploy/helm/order-api/templates/configmap.yaml` and `deploy/helm/order-api/values.yaml`
- [ ] T058 [P] Add portal API base URL configuration and production static serving configuration in `apps/order-portal/.env.example` and `apps/order-portal/nginx.conf`
- [ ] T059 [P] Add API and portal accessibility, responsive-layout, and error-state polish across `apps/order-portal/src/index.css`, `apps/order-portal/src/components/`, and `apps/order-portal/src/pages/`
- [ ] T060 [P] Add generated API documentation annotations and verify the running OpenAPI output against `specs/001-bulk-hardware-orders/contracts/openapi.yaml` in `services/order-api/src/main/java/com/compudelivery/orders/`
- [ ] T061 Run the complete backend unit/integration, frontend, Docker Compose, quickstart, and Helm validation commands documented in `specs/001-bulk-hardware-orders/quickstart.md`
- [ ] T062 Update implementation and local-run instructions in `README.md` for `services/order-api/`, `apps/order-portal/`, and `deploy/`

---

## Dependencies & Execution Order

### Phase Dependencies

- **Phase 1: Setup** has no dependencies and establishes the active monorepo modules.
- **Phase 2: Foundational** depends on Setup and blocks all user stories.
- **Phase 3: User Story 1** depends on Foundational and is the MVP increment.
- **Phase 4: User Story 2** depends on Foundational and the order records/API established by User Story 1; its query and portal work can proceed in parallel once the shared order contract exists.
- **Phase 5: User Story 3** depends on Foundational and the order lifecycle persistence from User Story 1; dashboard cancellation UI can proceed alongside backend cancellation work.
- **Phase 6: User Story 4** depends on Foundational and the shared order model; its lifecycle service must be complete before operator controls and pricing-lock behavior are finalized.
- **Phase 7: Polish** depends on the desired user stories being complete.

### User Story Dependencies

- **User Story 1 (P1)**: Can start after Phase 2; no dependency on another user story.
- **User Story 2 (P2)**: Uses orders created by User Story 1 for realistic validation, but its scoped query/detail implementation is independently testable with seeded data.
- **User Story 3 (P2)**: Uses the shared order lifecycle model from Phase 2 and can be tested with seeded orders; it does not require User Story 2 UI work.
- **User Story 4 (P3)**: Uses the shared order model and history tables from Phase 2; it can be implemented independently of the client dashboard, though the portal displays its results.

### Story Completion Order

```text
Phase 1 -> Phase 2 -> US1 (MVP) -> US2 and US3 in parallel -> US4 -> Polish
```

## Parallel Opportunities

- **Setup**: T002-T008 can run in parallel after T001 establishes the root layout.
- **Foundation**: T010-T014, T016-T019 can run in parallel after T009 defines the database shape; T015 depends on the schema model but is independent of frontend shell work.
- **US1**: T020, T021, T027, T028, and T030 can run in parallel; T022-T026 then integrate the backend service and controller; T029 integrates the portal API flow.
- **US2**: T035 and T037 can run in parallel with T033-T034; T036 and T038 follow the shared API/query contract.
- **US3**: T040-T041 can proceed in parallel with T042; T043-T044 follow both backend and UI control work.
- **US4**: T046-T048 can proceed in sequence while T049 and T051 proceed in parallel; T050 and T052 integrate the lifecycle contract into the portal.
- **Polish**: T055-T060 can run in parallel before T061; T062 can be completed alongside deployment work.

## Parallel Examples

### User Story 1

```text
Task T020: Implement contract-term lookup and PricingService
Task T021: Implement create-order DTO validation
Task T027: Implement catalog quantity-entry UI
Task T028: Implement pricing summary UI
Task T030: Implement the shared five-column lifecycle stepper
```

### User Story 2

```text
Task T033: Implement scoped order query services
Task T035: Implement dashboard order cards and history list
Task T037: Implement header identity switcher UI
```

### User Story 3

```text
Task T040: Implement transactional cancellation service
Task T042: Implement explicit-confirmation cancel controls
```

### User Story 4

```text
Task T046: Implement lifecycle transition rules
Task T049: Implement operator status controls
Task T051: Implement five-column lifecycle rendering
```

## Implementation Strategy

### MVP First: User Story 1 Only

1. Complete Phase 1 Setup.
2. Complete Phase 2 Foundational prerequisites.
3. Complete Phase 3 User Story 1.
4. Run T032 and verify order intake, pricing, editing, validation failures, and `OrderIntaken` publishing.
5. Stop for an MVP demo before adding history, cancellation, and operator lifecycle workflows.

### Incremental Delivery

1. Add User Story 2 for client-scoped dashboard history and detail views.
2. Add User Story 3 for safe client cancellation.
3. Add User Story 4 for operator lifecycle progression and Shipped-time pricing lock.
4. Complete Phase 7 deployment, accessibility, documentation, and full quickstart validation.

## Completion Criteria

- All 62 tasks use the required `- [ ] [TaskID] [P?] [Story?] description with file path` format.
- Every user story has a goal, an independent test criterion, and a checkpoint.
- The MVP is independently demonstrable after T032.
- No Warehouse or Invoice service/app code is created by these tasks.
