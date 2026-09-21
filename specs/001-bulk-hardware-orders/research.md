# Phase 0 Research: Bulk Hardware Order Management

All technology choices were supplied directly by the user (see `plan.md` Technical
Context), so no NEEDS CLARIFICATION markers remain for stack selection. Research below
resolves the *implementation-pattern* decisions needed to apply that stack correctly
against the spec's requirements (FR-001–FR-030) and constitution gates.

## 1. Caller-supplied identity (no auth)

- **Decision**: A servlet `OncePerRequestFilter` reads `X-Client-Id`
  (enterprise-client-facing endpoints) and/or `X-Operator-Id`
  (operator-facing endpoints), resolving each against the seeded
  `enterprise_client` table or a small seeded `operator` table, and rejects
  the request with 400 if a required header is missing/unknown. The two
  headers are not always mutually exclusive: on the read endpoints
  (`GET /orders`, `GET /orders/{orderId}`), `X-Client-Id` is always required
  (it names the client whose data is being viewed) and `X-Operator-Id` is
  optional — present only when an operator has selected that client in the
  identity switcher's client/company selector (FR-026) and is viewing that
  client's per-client pages rather than their own. On the client-only
  mutation endpoints (`POST /orders`, `PUT /orders/{orderId}/line-items`,
  `POST /orders/{orderId}/cancel`), only `X-Client-Id` is accepted — supplying
  `X-Operator-Id` does not grant a client-only action. On the operator-only
  mutation endpoint (`POST /orders/{orderId}/status`), only `X-Operator-Id`
  is required; the order's owning client is already fixed by `orderId`, so no
  `X-Client-Id` is needed there. The resolved identity/identities are placed
  on the request as a typed `CallerIdentity` and injected into controller
  methods via a custom `HandlerMethodArgumentResolver`. No Spring Security
  filter chain, session, or credential check is added.
- **Rationale**: Matches the spec's explicit assumption that identity is trusted
  as-is (FR-001, FR-008, FR-010, FR-012) while still giving every layer below the
  controller a typed, non-forgeable-within-the-request identity to scope queries by,
  keeping FR-009/FR-018 enforcement mechanical rather than ad hoc. The dual-header
  read case directly implements FR-026's requirement that an operator views the
  same per-client pages "scoped to the client currently selected in the identity
  switcher" rather than a cross-client queue, without inventing a separate
  operator-only read endpoint.
- **Alternatives considered**: Full Spring Security with a permissive
  `PermitAll`/custom `AuthenticationProvider` — rejected as unnecessary ceremony for a
  demo with no login; a simple query parameter instead of a header — rejected because
  headers keep the identity out of URLs/logs by convention and match the "demo
  identity switcher" UI sending it on every request; a separate
  `GET /operators/{operatorId}/clients/{clientId}/orders` endpoint for the
  operator read case — rejected as a duplicate of the client read path that
  would have to be kept in lockstep with it for no behavioral difference.

## 2. Demo identity switcher backing data

- **Decision**: Seed a fixed set of demo `enterprise_client` rows (each with contract
  discount terms) and a fixed set of demo operator identifiers via Flyway migrations.
  Expose them read-only via `GET /api/demo-identities` for the frontend switcher to
  populate its dropdown. The seeded roster covers every contract-terms state FR-027
  requires, at minimum:
  - `ACME-001`, `GLOBEX-002` — valid, currently-effective terms with differing
    `discount_percentage` values, so both differing Net Totals and per-client
    isolation are observable side by side.
  - `NOTERMS-003` — no `contract_discount_terms` row at all (missing).
  - `EXPIRED-004` — one row whose `effective_until` is in the past (expired).
  - `AMBIGUOUS-005` — either zero rows matching "now" (a future-dated
    `effective_from` with no other coverage) or two overlapping rows for the
    same instant, demonstrating the ambiguous case distinctly from the
    missing/expired ones.
  - `OPS-1` — a seeded operator identifier, distinct from the client rows.
- **Rationale**: FR-019 requires the switcher to list selectable identities; since
  there is no signup/provisioning flow in scope, seed data is the only source.
  FR-027 additionally requires the roster to make every blocking condition in
  FR-004 demonstrable from the switcher alone, without a contract-management
  screen.
- **Alternatives considered**: Hardcoding the identity list in the frontend — rejected
  because it would drift from the backend's actual seeded clients/contract terms and
  duplicate data ownership; seeding only the two valid clients and leaving the
  missing/expired/ambiguous cases to be constructed ad hoc during testing —
  rejected because FR-027 requires them to ship as part of the fixed seed set.

## 3. Concurrency / first-committed-wins (FR-016)

- **Decision**: `bulk_order` carries a JPA `@Version` column. Every state-changing
  operation (cancel, advance status, edit line items) loads the order, applies the
  change, and saves within a single transaction. A concurrent conflicting write raises
  `OptimisticLockException`, which a `@ControllerAdvice` catches, re-reads the order's
  now-current row (a cheap follow-up `SELECT`, outside the failed transaction), and
  maps the failure to HTTP 409 with a Problem Details body stating the order's state
  has changed *and* carrying that freshly-read `currentStatus`, so the caller sees the
  actual outcome without a separate lookup (FR-016).
- **Rationale**: Optimistic locking is the standard Spring Data JPA / PostgreSQL
  mechanism for exactly this "whoever commits first wins, the other is rejected"
  semantics, with no extra locking service required; re-reading the row on conflict is
  the simplest way to satisfy FR-016's requirement that the rejection response include
  the order's current status, since the losing request's own in-memory copy is stale
  by definition.
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
  both pages.
- **Alternatives considered**: A heavier state library (Redux/Zustand) — rejected,
  the app's client state (selected identity, in-progress order draft, live pricing
  summary) fits comfortably in Context + component state; a full server-state library
  (React Query) is worth adopting during implementation for caching/refetch but is not
  a research-blocking decision.
- **Empty/loading/error states (FR-028, FR-029)**: Each order region (Active
  Orders, Order History) tracks its own `idle | loading | error | loaded` fetch
  state rather than sharing one page-level flag, since the two regions load
  independently and must be able to show a loading indicator, a retry-capable
  error message, or a distinct "No active orders" / "No past orders" empty
  message without one region's state leaking into the other's rendering.
  Retry re-issues the same fetch; no exponential backoff or automatic retry is
  needed at this demo scale.

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

## 13. Net Total rounding (FR-003)

- **Decision**: `unit_list_price` and `quantity` are exact by construction
  (catalog prices are stored to 2 decimal places, quantity is an integer), so
  each `line_subtotal` and the summed `gross_total` are already exact to 2
  decimal places with no intermediate rounding. The discount step —
  `net_total_raw = gross_total - (gross_total * discount_percentage / 100)` —
  is computed with `BigDecimal` at an unrounded/high intermediate scale (no
  `.setScale` until the final step), and only `net_total_raw` is rounded, once,
  to 2 decimal places using `RoundingMode.HALF_UP`, producing the persisted
  `net_total`. No other value in the pricing path is rounded.
- **Rationale**: Directly implements FR-003's "carry Gross Total and the
  discount calculation at full precision, then round only the final Net Total
  ... using round-half-up" rule; rounding `gross_total` or an intermediate
  discount amount before the final step would let compounding rounding error
  diverge from the spec's stated policy.
- **Alternatives considered**: Rounding at each intermediate step (gross
  total, discount amount, then net total) — rejected, explicitly contradicts
  FR-003; using `RoundingMode.HALF_EVEN` (banker's rounding, Java's
  `BigDecimal` default via `MathContext`) — rejected, FR-003 specifies
  half-up.

## 14. Order History pagination (FR-008)

- **Decision**: `GET /orders` accepts `page` (0-based, default `0`) and
  returns at most 25 orders per call, ordered by `created_at DESC, id DESC`
  (the `id` tiebreaker keeps ordering stable when multiple orders share a
  `created_at` value at the query's timestamp precision). The response is a
  paging envelope — `items`, `page`, `pageSize`, `totalCount`, `hasMore` —
  rather than a bare array, so the frontend's "reach older entries" control
  knows whether to render itself without a separate count call. A composite
  index on `bulk_order (client_id, created_at DESC, id DESC)` backs both the
  ownership filter (FR-009) and the ordering/paging in one index scan, keeping
  retrieval within SC-001's under-5-seconds target regardless of history
  length.
- **Rationale**: FR-008 requires newest-first ordering, a fixed page size of
  25, a way to reach older entries, and no skipped/duplicated entries across
  pages; offset-based paging with a stable `(created_at, id)` sort key is the
  simplest mechanism satisfying all four without introducing opaque cursor
  tokens the frontend has no other need for at this scale.
- **Alternatives considered**: Cursor/keyset pagination (`WHERE (created_at,
  id) < (:lastCreatedAt, :lastId)`) — a stronger guarantee against skips when
  rows are concurrently inserted mid-scroll, but rejected as more mechanism
  than a demo with ≤500 orders/day (SC-007) needs; returning the full history
  and paging client-side — rejected, defeats SC-001's retrieval-time target as
  history grows.

## 15. Line item and quantity ceilings (FR-014, FR-030)

- **Decision**: Bean Validation annotations enforce `quantity` between 1 and
  10,000 inclusive on each `LineItemInput` (`@Min(1) @Max(10000)`), and a
  custom validator on `CreateOrderRequest`/`ReplaceLineItemsRequest` rejects a
  `lineItems` list with more than 100 entries. Both checks run before the
  SKU-existence and contract-terms checks, so a request that trips a ceiling
  fails fast with a single clear message (per line-item ceiling or per-order
  ceiling) rather than a generic validation error, and — per FR-004 — no order
  or line item row is ever persisted for a rejected submission. The same
  validators run on `PUT /orders/{orderId}/line-items` so an edit cannot push
  an existing order past either limit either.
- **Rationale**: Bean Validation on the request DTO is the standard Spring
  mechanism for a fixed numeric ceiling and needs no hand-rolled check; the
  list-size ceiling needs one small custom validator since `@Size` alone
  wouldn't produce FR-030's specific error message distinguishing "too many
  line items" from other validation failures.
- **Alternatives considered**: Enforcing only at the database layer (a `CHECK`
  constraint) — rejected, produces an opaque constraint-violation error
  instead of FR-014/FR-030's required clear, specific message, and fires only
  on the write that would have exceeded it rather than during request
  validation.
