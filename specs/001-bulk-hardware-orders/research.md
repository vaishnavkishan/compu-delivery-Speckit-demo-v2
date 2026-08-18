# Phase 0 Research: Bulk Hardware Order Management

All technology choices were supplied directly by the user (see `plan.md` Technical
Context), so no NEEDS CLARIFICATION markers remain for stack selection. Research below
resolves the *implementation-pattern* decisions needed to apply that stack correctly
against the spec's requirements (FR-001–FR-025) and constitution gates.

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

- **Decision**: A single `OrderStatus` enum (`INTAKE, PROCESSING, BACKORDERED,
  SHIPPED, FINAL_DELIVERY, CANCELLED`) plus a static allowed-transitions map enforced
  in a domain service (`OrderLifecycleService`) before any status write:
  `INTAKE→PROCESSING`, `PROCESSING→BACKORDERED`, `BACKORDERED→PROCESSING`,
  `PROCESSING→SHIPPED`, `SHIPPED→FINAL_DELIVERY`, and `{INTAKE, PROCESSING,
  BACKORDERED, SHIPPED}→CANCELLED` (client-initiated only). Any transition not in the
  map is rejected with 409/422 and a clear message.
- **Rationale**: Centralizing the transition table is the simplest way to guarantee
  FR-006 (no skips, no backward moves, no post-terminal changes) except the one
  explicitly allowed reversal (Backordered→Processing), and keeps Constitution
  Principle I enforceable in one place.
- **Alternatives considered**: A full state-machine library (e.g. Spring
  Statemachine) — rejected as disproportionate for 6 states and a handful of edges.

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

- **Decision**: Single Spring Boot Maven module `backend/` (not a multi-module
  build). Internal package-by-feature structure (`order`, `catalog`, `client`,
  `pricing`, `identity`, `messaging`) rather than package-by-layer, since the domain
  is small and cohesive.
- **Rationale**: The in-scope domain is one bounded context (Order). Multi-module
  Maven or multiple services would anticipate the out-of-scope Warehouse/Invoicing
  services prematurely; those will be separate repos/services when built per the
  constitution's domain-boundary note.
- **Alternatives considered**: Hexagonal/ports-and-adapters module split
  (`domain`/`application`/`infrastructure` as separate Maven modules) — rejected as
  more ceremony than a single-team, single-bounded-context demo service needs; can be
  introduced later without changing the public API if warranted.

## 10. Frontend structure and identity propagation

- **Decision**: React 19 + TypeScript + Vite + Tailwind SPA in `frontend/`, using
  React Router for the two pages (dashboard, order detail/create) and React Context
  to hold the currently selected demo identity (client or operator), attached to
  every API call as the appropriate header via a thin `fetch` wrapper.
- **Rationale**: Matches the two-page structure decided in Clarifications
  (2026-08-17) and keeps identity-switching a single source of truth consumed by
  both pages.
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

- **Decision**: `docker-compose.yml` at repo root for local dependency startup
  (PostgreSQL 17, RabbitMQ with management plugin) used during backend/frontend dev.
  Multi-stage `Dockerfile`s for `backend/` (Maven build stage → minimal JRE runtime
  stage) and `frontend/` (Vite build stage → static-file serving stage). A Helm chart
  under `deploy/helm/` packages the backend Deployment/Service, frontend
  Deployment/Service, and references to Postgres/RabbitMQ (as chart dependencies or
  separate manifests) for deployment to k3s via Rancher Desktop.
- **Rationale**: Directly follows the specified infra stack; docker-compose gives fast
  inner-loop iteration while Helm/k3s gives a realistic deployment target without
  requiring a cloud environment for this demo.
- **Alternatives considered**: Skipping docker-compose and requiring k3s for all
  local dev — rejected as too slow an inner loop for day-to-day backend/frontend
  iteration.
