# Phase 0 Research: Bulk Hardware Order Management

All technology choices were supplied directly by the user (see `plan.md` Technical
Context), so no NEEDS CLARIFICATION markers remain for stack selection. Research below
resolves the *implementation-pattern* decisions needed to apply that stack correctly
against the spec's requirements (FR-001–FR-027) and the constitution **v2.0.0** gates.

Revised 2026-09-17 for constitution v2.0.0 (four-state lifecycle, no
backordered/on-hold state, Cancellation Window closing at Shipped) and the spec's
Session 2026-09-17 clarifications (operator cross-client scope, operator controls on
the existing pages, Active/History partition). Decisions #4 and #13 carry the
substantive changes; superseded wording is marked rather than deleted so the
reasoning trail stays intact.

## 1. Caller-supplied identity (no auth)

- **Decision**: A servlet `OncePerRequestFilter` reads one of two mutually exclusive
  request headers — `X-Client-Id` (enterprise-client-facing endpoints) or
  `X-Operator-Id` (operator-facing endpoints) — resolves it against the seeded
  `enterprise_client` table or a small seeded `operator` table, and rejects the request
  with 400 if the header is missing/unknown for the endpoint's required role. The
  resolved identity is placed on the request as a typed `CallerIdentity` and injected
  into controller methods via a custom `HandlerMethodArgumentResolver`. No Spring
  Security filter chain, session, or credential check is added.
- **Rationale**: Matches the spec's explicit assumption that identity is trusted
  as-is (FR-001, FR-008, FR-010, FR-012) while still giving every layer below the
  controller a typed, non-forgeable-within-the-request identity to scope queries by,
  keeping FR-009/FR-018 enforcement mechanical rather than ad hoc.
- **Alternatives considered**: Full Spring Security with a permissive
  `PermitAll`/custom `AuthenticationProvider` — rejected as unnecessary ceremony for a
  demo with no login; a simple query parameter instead of a header — rejected because
  headers keep the identity out of URLs/logs by convention and match the "demo
  identity switcher" UI sending it on every request.

## 2. Demo identity switcher backing data

- **Decision**: Seed a fixed set of demo `enterprise_client` rows (each with contract
  discount terms) and a fixed set of demo operator identifiers via Flyway migrations.
  Expose them read-only via `GET /api/demo-identities` for the frontend switcher to
  populate its dropdown.
- **Rationale**: FR-019 requires the switcher to list selectable identities; since
  there is no signup/provisioning flow in scope, seed data is the only source.
- **Alternatives considered**: Hardcoding the identity list in the frontend — rejected
  because it would drift from the backend's actual seeded clients/contract terms and
  duplicate data ownership.

## 3. Concurrency / first-committed-wins (FR-016)

- **Decision**: `bulk_order` carries a JPA `@Version` column. Every state-changing
  operation (cancel, advance status, edit line items) loads the order, applies the
  change, and saves within a single transaction. A concurrent conflicting write raises
  `OptimisticLockException`, which a `@ControllerAdvice` maps to HTTP 409 with a
  Problem Details body stating the order's state has changed.
- **Rationale**: Optimistic locking is the standard Spring Data JPA / PostgreSQL
  mechanism for exactly this "whoever commits first wins, the other is rejected"
  semantics, with no extra locking service required.
- **Alternatives considered**: Pessimistic row locks (`SELECT ... FOR UPDATE`) —
  rejected as unnecessary contention/latency for a low-throughput demo entity and a
  worse fit for the "reject the loser with a message" requirement than an exception
  mapped to 409; application-level compare-and-swap on a status column — rejected as
  a hand-rolled duplicate of what `@Version` already provides correctly.

## 4. Order lifecycle transition validation (FR-005, FR-006)

- **Decision** (revised 2026-09-17): A single `OrderStatus` enum with exactly the
  four lifecycle states plus the terminal cancellation state — `INTAKE, PROCESSING,
  SHIPPED, FINAL_DELIVERY, CANCELLED` — plus a static allowed-transitions map
  enforced in a domain service (`OrderLifecycleService`) before any status write:
  `INTAKE→PROCESSING`, `PROCESSING→SHIPPED`, `SHIPPED→FINAL_DELIVERY`, and
  `{INTAKE, PROCESSING}→CANCELLED` (client-initiated only). The map contains no
  reverse edge and no self-edge; any transition not in it is rejected with 409 and a
  clear message. `FINAL_DELIVERY` and `CANCELLED` have no outgoing edges.
- **Rationale**: Constitution v2.0.0 Principle I enumerates the lifecycle as exactly
  four ordered states and forbids introducing any on-hold/backordered/suspended
  state without amending the constitution first. Omitting such a state from the enum
  (rather than defining it and refusing to use it) makes it *unrepresentable* — no
  persisted row, API payload, or UI step can express it — which is a stronger
  guarantee than validation alone. With no reversal to carve out, the transition map
  becomes a plain linear chain plus one branch, so FR-006 (no skips, no backward
  moves, no post-terminal changes) holds with no exceptions to document. Centralizing
  the map keeps Principle I enforceable in one place.
- **Hardware availability**: an order waiting on stock stays in `PROCESSING`
  (FR-005). Availability, stock levels, and replenishment are modelled nowhere in
  this service — the constitution's Governance & Boundaries section assigns them to
  the inventory domain and explicitly forbids modelling them as lifecycle states.
- **Superseded**: the pre-2026-09-17 decision used a six-member enum including
  `BACKORDERED`, with `PROCESSING→BACKORDERED` and `BACKORDERED→PROCESSING` edges
  and cancellation valid from any non-terminal state including `SHIPPED`. All three
  are withdrawn: the state is gone, the reversal is gone, and cancellation is
  confined to the Cancellation Window (see #13).
- **Alternatives considered**: A full state-machine library (e.g. Spring
  Statemachine) — rejected as disproportionate for 5 states and 4 edges. Keeping
  `BACKORDERED` in the enum but making it unreachable — rejected: it would still
  surface in the OpenAPI `OrderStatus` schema and in persisted history, inviting
  clients and future consumers to handle a state the constitution says does not
  exist.

## 5. Net Total lock point and recalculation on edit (FR-017, FR-025)

- **Decision**: `bulk_order.net_total_locked_at` is null until the order transitions
  to `SHIPPED`, at which point it is stamped and further line-item edits/pricing
  recalculation are rejected at the service layer (not just the UI). While
  `INTAKE`/`PROCESSING`, `PUT /api/orders/{id}/line-items` replaces the full line-item
  set and recomputes Gross Total and Net Total from the client's *currently effective*
  contract discount terms, writing a new row to `net_total_calculation` (never
  updating a prior row) per FR-007/Constitution Principle IV.
- **Rationale**: Directly implements the FR-017 lock semantics and keeps
  recalculation append-only and traceable.
- **Note (2026-09-17)**: this lock point is unchanged by constitution v2.0.0, and it
  now coincides exactly with the close of the Cancellation Window — entering
  `SHIPPED` both stamps `net_total_locked_at` and ends cancellability, so a single
  transition handler enforces both (see #13).
- **Alternatives considered**: Locking at intake (original spec wording) — superseded
  by the 2026-08-17 clarification; recalculating net total automatically whenever
  contract terms change — explicitly rejected by the spec (Edge Cases, FR-017).

## 6. Contract discount terms modeling

- **Decision**: `contract_discount_terms` is a separate table keyed by client with
  `discount_percentage`, `effective_from`, `effective_until` (nullable = open-ended).
  "Current" terms = the row for that client where `effective_from <= now()` and
  (`effective_until` is null or `effective_until >= now()`). If zero or more-than-one
  row qualifies, terms are treated as missing/ambiguous and the order is blocked
  (FR-004).
  Each accepted order snapshots the discount percentage actually applied (on the order
  row and again on each `net_total_calculation` row) so later contract renegotiation
  never retroactively changes a locked order's history.
- **Rationale**: A time-bounded terms table is the simplest model that supports
  "expired", "not yet effective", and "ambiguous/overlapping" without a status enum
  that could drift from the dates.
- **Alternatives considered**: A single mutable `discount_percentage` column on
  `enterprise_client` with no history — rejected because it can't represent
  "expired" (FR-004) or preserve what was actually applied at calculation time
  (Constitution Principle IV).

## 7. OrderIntaken event publishing (RabbitMQ)

- **Decision**: A topic exchange `order.events` with routing key
  `order.intaken`. The order service publishes after the intake transaction commits,
  using `TransactionalEventListener(phase = AFTER_COMMIT)` on a Spring
  `ApplicationEvent` raised inside the transactional order-creation service method,
  so a message is never published for an order that failed to persist (and a
  persisted order never silently fails to publish due to an unrelated later
  exception in the same request). The exchange and a durable queue bound to it are
  declared by the order service itself (`AmqpAdmin`/`@Bean` declarations), since no
  downstream consumer service exists yet in this feature's scope.
- **Rationale**: AFTER_COMMIT avoids the classic dual-write problem (DB commit
  succeeds, message fails, or vice versa) with a two-line Spring idiom appropriate
  for this feature's scope, rather than building a transactional outbox table.
- **Alternatives considered**: Transactional outbox pattern with a relay
  poller/CDC — rejected as disproportionate infrastructure for a feature whose only
  in-scope consumer is "none yet" (Warehouse/Invoicing are future features); publishing
  synchronously inside the same transaction via a `ChannelAwareMessageListener`/JTA
  XA transaction spanning Postgres and RabbitMQ — rejected, unnecessary operational
  complexity for a demo and not natively supported by the chosen non-XA
  `ConnectionFactory` setup.

## 8. API documentation & error format

- **Decision**: springdoc-openapi generates the OpenAPI document from the controllers
  directly (annotated with `@Operation`/`@Schema` where the generated description
  needs disambiguation); `contracts/openapi.yaml` in this plan is the hand-authored
  source of truth for Phase 1 design review and diverges-from-implementation checks,
  not a duplicate maintained by hand forever. Errors use Spring Boot 3's built-in
  `ProblemDetail` (RFC 7807) for all 4xx/409 responses.
- **Rationale**: Keeps one documentation source (code-generated) for the running
  service while still giving Phase 1 a reviewable contract artifact; `ProblemDetail`
  is already on the Spring Web classpath, no extra dependency needed.
- **Alternatives considered**: Hand-maintained OpenAPI as the sole source with
  contract tests enforcing drift — deferred; may be revisited if a consumer team
  external to this repo needs a frozen contract.

## 9. Backend module layout

- **Decision** (revised 2026-08-19): The repository is a monorepo. A root Maven
  reactor `pom.xml` aggregates one Maven module per backend API service under
  `services/`; this feature adds and implements `services/order-api/` only.
  `services/warehouse-api/` and `services/invoice-api/` are reserved names for
  future features and are not created (no `pom.xml`, no source, no CI wiring) by
  this plan. Within `order-api`, internal structure stays package-by-feature
  (`order`, `catalog`, `client`, `pricing`, `identity`, `messaging`) rather than
  package-by-layer, since the domain is small and cohesive.
- **Rationale**: The user has directed that this is a monorepo intended to house
  order, warehouse, and invoice APIs and frontends as sibling services/apps, so the
  directory shape (`services/*`, one module per service) should reflect that target
  topology now, even though only `order-api` is in scope for this feature. This
  differs from the original decision (superseded below), which avoided any
  multi-module shape to prevent anticipating out-of-scope work; the monorepo
  direction makes the *placement convention* itself part of what's being decided,
  while the constitution's domain boundary still governs what gets *implemented* —
  no Warehouse/Invoicing code, schema, or tests are added by this feature.
- **Alternatives considered**: Hexagonal/ports-and-adapters module split
  (`domain`/`application`/`infrastructure` as separate Maven modules within
  `order-api`) — rejected as more ceremony than a single-team, single-bounded-context
  service needs; can be introduced later without changing the public API if
  warranted. Separate repositories per service (original decision, pre-2026-08-19) —
  superseded by the user's explicit monorepo direction; would have avoided any
  coupling between services' build/release cadence but conflicts with the stated
  goal of one repo housing all three APIs and their frontends.

## 10. Frontend structure and identity propagation

- **Decision**: React 19 + TypeScript + Vite + Tailwind SPA in `apps/order-portal/`
  (see #9 for the monorepo `apps/` convention), using React Router for the two
  pages (dashboard, order detail/create) and React Context to hold the currently
  selected demo identity (client or operator), attached to every API call as the
  appropriate header via a thin `fetch` wrapper.
- **Rationale**: Matches the two-page structure decided in Clarifications
  (2026-08-17) and keeps identity-switching a single source of truth consumed by
  both pages. The selected identity's *role* also drives which per-order control a
  card renders (FR-026), so the same Context serves both personas without a separate
  operator page — see #14.
- **Alternatives considered**: A heavier state library (Redux/Zustand) — rejected,
  the app's client state (selected identity, in-progress order draft, live pricing
  summary) fits comfortably in Context + component state; a full server-state library
  (React Query) is worth adopting during implementation for caching/refetch but is not
  a research-blocking decision.

## 11. Testcontainers strategy

- **Decision**: Backend integration tests spin up PostgreSQL 17 and RabbitMQ via
  Testcontainers, run Flyway migrations against the ephemeral Postgres container, and
  assert on published messages using a test consumer bound to the real exchange.
  Unit tests (pricing calculation, lifecycle transition validation) use plain
  JUnit5/Mockito/AssertJ with no containers.
- **Rationale**: Matches the specified testing stack exactly and keeps fast unit
  tests separate from slower container-backed integration tests.
- **Alternatives considered**: An embedded/in-memory broker or H2 for tests —
  rejected; the project's stated testing stack already includes Testcontainers
  specifically to avoid the prod/test parity gap that in-memory substitutes cause.

## 12. Local/dev environment and deployment path

- **Decision** (revised 2026-08-19): `deploy/docker-compose.yml` at repo root for
  local dependency startup (PostgreSQL 17, RabbitMQ with management plugin) used
  during `order-api`/`order-portal` dev. Multi-stage `Dockerfile`s live alongside
  each service/app (`services/order-api/Dockerfile`: Maven build stage → minimal
  JRE runtime stage; `apps/order-portal/Dockerfile`: Vite build stage →
  static-file serving stage), so each backend service and frontend app owns its
  own image build independent of its siblings. Helm charts live under
  `deploy/helm/`, one subchart per service/app (`deploy/helm/order-api/`,
  `deploy/helm/order-portal/`), each packaging that component's
  Deployment/Service; Postgres/RabbitMQ are declared as chart dependencies or
  separate manifests. Only the `order-api`/`order-portal` charts and
  `Dockerfile`s are created by this feature — `warehouse-api`/`invoice-api` and
  their portals get their own Dockerfile and Helm subchart when those future
  features scaffold those services.
- **Rationale**: Per-service/per-app Dockerfiles and Helm subcharts keep each
  monorepo component independently buildable and deployable, matching the
  monorepo direction (see #9) without coupling one service's release to
  another's; docker-compose gives fast inner-loop iteration while Helm/k3s gives
  a realistic deployment target without requiring a cloud environment for this
  demo.
- **Alternatives considered**: Skipping docker-compose and requiring k3s for all
  local dev — rejected as too slow an inner loop for day-to-day iteration. A
  single repo-root Dockerfile/Helm chart building all services together
  (original decision, pre-2026-08-19) — superseded because it would force
  warehouse/invoice services to be built and deployed as one unit with order-api
  once they exist, defeating the point of a services-oriented monorepo.

## 13. Cancellation Window enforcement and operator access scope

Added 2026-09-17 to cover constitution v2.0.0 Principle III and spec clarifications
FR-010/FR-011 (window closes at Shipped), FR-027 (operator cross-client scope) and
FR-023 (Active/History partition).

- **Decision — window enforcement**: cancellability is derived, never stored. A
  domain predicate `BulkOrder.isWithinCancellationWindow()` returns
  `status == INTAKE || status == PROCESSING`, and the cancel service calls it before
  any write. Three distinct rejections are produced so the reason is explicit rather
  than a silent no-op (constitution Usage rules): `SHIPPED` → 409 "order has shipped
  and can no longer be cancelled"; `FINAL_DELIVERY` → 409 "order has been
  delivered"; already `CANCELLED` → 409 "order is already cancelled". The same
  predicate feeds the API response (`cancellable` boolean on the order payload) so
  the UI's disabled-with-explanation control (FR-022) and the server's rejection
  cannot disagree.
- **Rationale**: Deriving the window from `status` keeps it impossible for a stored
  "cancellable" flag to drift out of sync with the lifecycle, and the shared
  predicate means the UI never has to re-implement the rule. Returning the boolean
  rather than having the frontend hardcode the state set also means a future
  lifecycle amendment changes one place.
- **Alternatives considered**: A `cancellation_window_closed_at` timestamp column —
  rejected as redundant with `net_total_locked_at` (both are stamped on entering
  `SHIPPED`) and as a second source of truth for something `status` already
  determines; letting the UI decide cancellability from `status` alone — rejected
  because it duplicates a constitutional rule in the frontend.

- **Decision — operator scope**: client-scoped and operator-scoped reads use
  *separate repository methods*. Client reads go through methods that take a
  `clientId` parameter and always include `client_id` in the `WHERE` clause
  (`findByIdAndClientId`, `findAllByClientId`); operator reads go through distinct
  methods that take no `clientId` at all (`findByIdForOperator`, `findAllForOperator`)
  and return each order with its owning `clientId` and client display name for
  on-screen attribution (FR-027). No method takes a nullable `clientId` whose null
  silently widens the scope.
- **Rationale**: FR-009's boundary applies to client identities; operators are
  internal staff outside it. Expressing that as two separate method signatures makes
  the scope visible at every call site and makes "forgot to pass the client id" a
  compile-time impossibility rather than a data-leak bug — the failure mode that
  Constitution Principle III and SC-006 exist to prevent. A nullable-parameter
  design would make the most serious possible defect in this feature a one-line
  omission.
- **Alternatives considered**: One repository method with a nullable `clientId`
  (null = operator/all) — rejected per the above; a Spring Security-style row filter
  or Hibernate `@Filter` keyed on the request identity — rejected as indirection
  that hides the scope decision from the reading of the service code, in a codebase
  with no security framework to hang it on.

- **Decision — Active/History partition**: the split is a single derived rule applied
  in one place. `OrderStatus.isActive()` is true for `{INTAKE, PROCESSING, SHIPPED}`
  and false for `{FINAL_DELIVERY, CANCELLED}`; the list endpoint returns every
  in-scope order once with its `status`, and the dashboard partitions on
  `isActive()`. Because the predicate is total over the enum and the two sections are
  its complement, no order can be missing from both sections or appear in both
  (FR-023).
- **Rationale**: Partitioning client-side from one authoritative predicate guarantees
  the no-overlap/no-omission property structurally, rather than relying on two server
  queries whose filters must be kept mutually exclusive by hand. It also keeps a
  single round trip per dashboard load.
- **Alternatives considered**: Two endpoints (`/orders?scope=active`,
  `?scope=history`) — rejected: two independently-maintained filters are exactly how
  an order comes to appear twice or vanish, and it doubles the dashboard's requests
  for no benefit at this scale. Adding a stored `is_active` column — rejected as
  derivable state.

## 14. Role-conditional UI controls (FR-026, FR-022)

Added 2026-09-17.

- **Decision**: No operator-only page or route is added. `IdentityContext` exposes the
  selected identity's `role` (`CLIENT` | `OPERATOR`), and a single
  `OrderCardActions` component renders, for one order: the cancel control when the
  role is `CLIENT` (enabled iff the order payload's `cancellable` is true, otherwise
  disabled with the inline shipped/delivered/cancelled explanation), or the "Advance
  to <next stage>" control when the role is `OPERATOR` and the order is non-terminal,
  or nothing when the order is terminal. The next-stage label comes from a shared
  frontend mirror of the transition map, so the button always names the stage the
  server would actually accept. Order creation and line-item editing controls are
  hidden for operator identities.
- **Rationale**: FR-026 explicitly places advancement on the existing two pages;
  concentrating the role/state branching in one component keeps both pages' cards
  consistent and gives the Vitest suite a single unit covering all
  role × status combinations.
- **Alternatives considered**: A separate operator route/page — rejected, contradicts
  FR-026; rendering both controls and disabling the inapplicable one — rejected, the
  spec calls for the advance control to appear *in place of* the cancel control, and
  showing clients a disabled operator action misrepresents what they may do.
