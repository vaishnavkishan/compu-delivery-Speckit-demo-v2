# Implementation Plan: Bulk Hardware Order Management

**Branch**: `001-bulk-hardware-orders` | **Date**: 2026-08-18 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/001-bulk-hardware-orders/spec.md`

**Note**: This template is filled in by the `/speckit-plan` command; its definition describes the execution workflow.

## Summary

Enterprise clients submit bulk hardware orders (SKU + quantity line items); the
system validates the order against a fixed catalog, computes Gross Total and, from
the client's currently effective pre-negotiated contract discount, a Net Total,
blocking finalization if terms are missing/ambiguous/expired. Accepted orders are
tracked through a defined lifecycle (Intake → Processing ⇄ Backordered → Shipped →
Final Delivery, with Cancellation as a branch before Final Delivery), with every
transition and every Net Total calculation recorded as append-only history. Clients
view only their own order history/detail and may cancel or edit (while still
Intake/Processing) their own orders; operators alone advance lifecycle status.
Concurrent conflicting writes on one order resolve first-committed-wins via
optimistic locking. A Spring Boot 3.5/Java 25 REST API backed by PostgreSQL 17
implements the domain and publishes an `OrderIntaken` event to RabbitMQ on order
creation; a React 19/TypeScript/Vite/Tailwind portal (dashboard + order
detail/create pages, per the reviewed Figma reference design) provides the
enterprise-client- and operator-facing UI, with no login — a demo identity
switcher supplies the trusted caller identity per request.

## Technical Context

**Language/Version**: Java 25 (backend); TypeScript 5.x with React 19 (frontend)

**Primary Dependencies**: Spring Boot 3.5 (Web, Validation, Data JPA, Actuator,
AMQP/RabbitMQ), Maven, Lombok, Flyway, springdoc-openapi (backend); React 19, Vite,
Tailwind CSS, React Router (frontend)

**Storage**: PostgreSQL 17, optimistic locking via JPA `@Version` (see
`research.md` #3); Flyway-managed schema and seed data (demo clients, contract
discount terms, fixed hardware catalog)

**Testing**: JUnit 5, Mockito, AssertJ, Testcontainers (backend unit +
Postgres/RabbitMQ-backed integration tests); Vitest, React Testing Library
(frontend)

**Target Platform**: Linux containers; local dev via `docker-compose`
(PostgreSQL + RabbitMQ), deployed to k3s (Rancher Desktop) via Helm; Docker
multi-stage builds for backend and frontend images

**Project Type**: web (frontend + backend)

**Performance Goals**: Net Total returned to the client within 5s of order
submission (SC-001); sustain ≥500 bulk order submissions/day across all clients
without degradation in calculation accuracy or response time (SC-007)

**Constraints**: No authentication/authorization — every request carries a
caller-supplied, trusted `X-Client-Id` or `X-Operator-Id` header (see
`research.md` #1); concurrent conflicting order mutations resolved
first-committed-wins via optimistic locking, loser rejected with HTTP 409
(FR-016); Net Total locked (read-only) once an order reaches Shipped (FR-017);
single currency, no split/partial delivery (Assumptions)

**Scale/Scope**: Demo/reference scale — a small fixed set of seeded enterprise
clients and hardware catalog items (~10–20 SKUs matching the Figma catalog); 4
user stories (submit & price, view history, cancel, operator lifecycle
advancement) across a two-page enterprise-client portal plus operator status
transitions

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Gate | Design mechanism | Status |
|---|---|---|---|
| I. Order Lifecycle Integrity | Order MUST progress through a single defined lifecycle; no skips, no re-entry, no silent forcing | `OrderStatus` enum + centralized allowed-transitions map enforced by `OrderLifecycleService` before every status write (`data-model.md` State Transitions; `research.md` #4) | PASS |
| II. Contract-Driven Pricing | Net Total MUST derive exclusively from current contract terms; missing/ambiguous/expired terms block finalization, never a fallback discount | `ContractDiscountTerms` time-bounded lookup at intake and at each edit; zero or >1 matching row blocks the order (FR-004; `research.md` #6) | PASS |
| III. Client Data Ownership & Access Boundary | Client sees/cancels only its own orders; cancellation only while Active | Every client-facing repository query filters by `client_id` from the trusted header at the data-access layer, not just the response layer; cancel endpoint checks Active status before writing (`data-model.md` Cross-Entity Invariants) | PASS |
| IV. Traceability & Auditability | Every transition and every Net Total calculation attributable to a time + actor; history not overwritten | Append-only `LifecycleTransition` and `NetTotalCalculation` tables, insert-only (`data-model.md`) | PASS |
| V. Bulk Order as First-Class Unit | Lifecycle status, cancellation, delivery confirmation apply at order level, not line-item level | `status`, `net_total_locked_at`, `cancelled_at` live on `BulkOrder` only; `LineItem` carries no independent lifecycle fields (`data-model.md`) | PASS |

No violations identified; **Complexity Tracking** is not needed for this plan.

Domain-boundary check (Governance & Boundaries): this feature does not implement
warehouse/inventory management, carrier/logistics operations, or payment
settlement — confirmed out of scope per spec Assumptions and user input's
"Out of Scope (Future)" list (Warehouse API/DB/UI, Invoicing API/DB/UI). PASS.

**Post-Phase 1 re-check**: The Phase 1 artifacts (`data-model.md`, `contracts/`)
were designed to satisfy the gate table above directly — each row's "Design
mechanism" column cites the concrete Phase 1 artifact enforcing it — rather than
being reconciled afterward. No new violations were introduced during design; all
five gates remain PASS.

## Project Structure

### Documentation (this feature)

```text
specs/001-bulk-hardware-orders/
├── plan.md               # This file (/speckit-plan command output)
├── research.md           # Phase 0 output (/speckit-plan command)
├── data-model.md         # Phase 1 output (/speckit-plan command)
├── quickstart.md         # Phase 1 output (/speckit-plan command)
├── contracts/            # Phase 1 output (/speckit-plan command)
│   ├── openapi.yaml
│   └── events.md
├── checklists/
│   └── requirements.md
├── figma-designs/         # reference UI (enterprise-user-dashboard.html, enterprise-user-detail-page.html, create-order-page.html)
└── tasks.md               # Phase 2 output (/speckit-tasks command - NOT created by /speckit-plan)
```

### Source Code (repository root)

```text
backend/                          # Spring Boot 3.5 / Java 25, single Maven module
├── src/main/java/com/compudelivery/orders/
│   ├── order/                    # BulkOrder, LineItem, OrderStatus, OrderLifecycleService, OrderController
│   ├── pricing/                  # NetTotalCalculation, pricing service
│   ├── client/                   # EnterpriseClient, ContractDiscountTerms
│   ├── catalog/                  # HardwareCatalogItem
│   ├── identity/                 # CallerIdentity filter + argument resolver (X-Client-Id / X-Operator-Id)
│   ├── messaging/                # RabbitMQ topology + OrderIntaken publisher
│   └── web/                      # ProblemDetail exception handling
├── src/main/resources/
│   ├── application.yml
│   └── db/migration/             # Flyway migrations (schema + seed data)
└── src/test/java/com/compudelivery/orders/
    ├── unit/                     # JUnit5/Mockito/AssertJ
    └── integration/              # Testcontainers (PostgreSQL, RabbitMQ)

frontend/                         # React 19 / TypeScript / Vite / Tailwind
├── src/
│   ├── pages/                    # DashboardPage, OrderDetailPage (create/edit)
│   ├── components/               # LifecycleStepper, CatalogTable, PricingSummary, IdentitySwitcher
│   ├── api/                      # fetch wrapper attaching X-Client-Id / X-Operator-Id
│   └── context/                  # IdentityContext (selected demo identity)
└── tests/                        # Vitest + React Testing Library

deploy/
├── docker-compose.yml            # local PostgreSQL 17 + RabbitMQ for backend/frontend dev
└── helm/                         # Helm chart(s) for backend, frontend, and their k3s deployment
```

**Structure Decision**: Web application (Option 2: frontend + backend), matching
the user-supplied stack — a single-module Maven Spring Boot service in `backend/`
(package-by-feature; see `research.md` #9) and a Vite/React SPA in `frontend/`
(two pages — dashboard, order detail/create — per the reviewed Figma design and
the 2026-08-17 clarifications). `deploy/` holds the docker-compose file for local
inner-loop development and the Helm chart for k3s/Rancher Desktop deployment
(`research.md` #12), keeping deployment concerns out of both application
directories. No CLI, mobile, or additional service directories are introduced —
Warehouse and Invoicing remain out of scope per the constitution's domain
boundary and are not scaffolded here.

## Complexity Tracking

*No Constitution Check violations — this section is not applicable.*
