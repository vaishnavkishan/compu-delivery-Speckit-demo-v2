# Implementation Plan: Bulk Hardware Order Management

**Branch**: `001-bulk-hardware-orders` | **Date**: 2026-09-17 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/001-bulk-hardware-orders/spec.md`

**Note**: This template is filled in by the `/speckit-plan` command; its definition describes the execution workflow.

**Revision note (2026-09-17)**: This plan and its Phase 0/1 artifacts were regenerated
against constitution **v2.0.0** and the spec's Session 2026-09-17 clarifications. The
previous revision assumed a five-stage lifecycle with a Backordered/On-Hold state and a
Cancellation Window closing at Final Delivery; both are now removed. See
[Constitution v2.0.0 reconciliation](#constitution-v200-reconciliation) for the full
list of design changes.

## Summary

Enterprise clients submit bulk hardware orders (SKU + quantity line items); the
system validates the order against a fixed catalog, computes Gross Total and, from
the client's currently effective pre-negotiated contract discount, a Net Total,
blocking finalization if terms are missing/ambiguous/expired. Accepted orders are
tracked through a lifecycle of exactly four ordered stages (Intake → Processing →
Shipped → Final Delivery), with Cancellation as the only branch and available only
while the order is in Intake or Processing (the Cancellation Window); there is no
on-hold or backordered stage, and an order awaiting hardware simply stays in
Processing. Every transition and every Net Total calculation is recorded as
append-only history. Client identities view and cancel only their own orders and may
edit line items while still Intake/Processing; operator identities sit outside the
per-client boundary, see all clients' orders, and alone advance lifecycle status.
Concurrent conflicting writes on one order resolve first-committed-wins via
optimistic locking. A Spring Boot 3.5/Java 25 REST API backed by PostgreSQL 17
implements the domain and publishes an `OrderIntaken` event to RabbitMQ on order
creation; a React 19/TypeScript/Vite/Tailwind portal (dashboard + order
detail/create pages, per the reviewed Figma reference design) serves both personas
from the same two pages, with no login — a demo identity switcher supplies the
trusted caller identity per request and determines whether a card shows the client's
cancel control or the operator's advance control.

## Technical Context

**Language/Version**: Java 25 (backend); TypeScript 5.x with React 19 (frontend)

**Primary Dependencies**: Spring Boot 3.5 (Web, Validation, Data JPA, Actuator,
AMQP/RabbitMQ), Maven, Lombok, Flyway, springdoc-openapi (backend); React 19, Vite,
Tailwind CSS, React Router (frontend)

**Storage**: PostgreSQL 17, optimistic locking via JPA `@Version` (see
`research.md` #3); Flyway-managed schema and seed data (demo clients, demo
operators, contract discount terms, fixed hardware catalog)

**Testing**: JUnit 5, Mockito, AssertJ, Testcontainers (backend unit +
Postgres/RabbitMQ-backed integration tests); Vitest, React Testing Library
(frontend)

**Target Platform**: Linux containers; local dev via `docker-compose`
(PostgreSQL + RabbitMQ), deployed to k3s (Rancher Desktop) via Helm; Docker
multi-stage builds for backend and frontend images

**Project Type**: web monorepo — a single repository intended to house
multiple backend API services (order, warehouse, invoice) and their
corresponding frontend apps as sibling modules. This feature implements only
the `order-api` service and `order-portal` app; `warehouse-api`,
`invoice-api`, and their portals are reserved directory slots for future
features and are not scaffolded here (see `research.md` #9).

**Performance Goals**: Net Total returned to the client within 5s of order
submission (SC-001); sustain ≥500 bulk order submissions/day across all clients
without degradation in calculation accuracy or response time (SC-007)

**Constraints**: No authentication/authorization — every request carries a
caller-supplied, trusted `X-Client-Id` or `X-Operator-Id` header (see
`research.md` #1); concurrent conflicting order mutations resolved
first-committed-wins via optimistic locking, loser rejected with HTTP 409
(FR-016); the Cancellation Window is Intake and Processing only and closes
permanently on entering Shipped (FR-010, FR-011); Net Total locked (read-only)
once an order reaches Shipped (FR-017); exactly four lifecycle stages with no
on-hold/backordered state and no permitted reversal (FR-005, FR-006); single
currency, no split/partial delivery (Assumptions)

**Scale/Scope**: Demo/reference scale — a small fixed set of seeded enterprise
clients, demo operators, and hardware catalog items (~10–20 SKUs matching the
Figma catalog); 4 user stories (submit & price, view history, cancel within the
window, operator lifecycle advancement) across a two-page portal that serves both
client and operator identities

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

Checked against constitution **v2.0.0** (2026-09-17).

| Principle | Gate | Design mechanism | Status |
|---|---|---|---|
| I. Order Lifecycle Integrity | Order MUST progress through exactly four ordered states (Intake → Processing → Shipped → Final Delivery); no skips, no re-entry, no silent forcing, and no on-hold/backordered/suspended state | `OrderStatus` enum holds exactly those four states plus terminal `CANCELLED` — no `BACKORDERED` member exists, so an out-of-lifecycle state is unrepresentable, not merely rejected; a centralized allowed-transitions map with no reverse edges is enforced by `OrderLifecycleService` before every status write (`data-model.md` State Transitions; `research.md` #4) | PASS |
| II. Contract-Driven Pricing | Net Total MUST derive exclusively from current contract terms; missing/ambiguous/expired terms block finalization, never a fallback discount | `ContractDiscountTerms` time-bounded lookup at intake and at each edit; zero or >1 matching row blocks the order (FR-004; `research.md` #6) | PASS |
| III. Client Data Ownership & Access Boundary | An enterprise client sees/cancels only its own orders; client-initiated Cancellation permitted only within the Cancellation Window (Intake or Processing), never once Shipped | Every **client-identity** repository query filters by `client_id` from the trusted header at the data-access layer, not just the response layer, so "not mine" and "doesn't exist" are indistinguishable (FR-018); the cancel path checks `status IN (INTAKE, PROCESSING)` before writing and rejects Shipped/Final Delivery/already-cancelled explicitly (`data-model.md` Cross-Entity Invariants; `research.md` #13). Operator identities are internal staff, are not enterprise clients, and are deliberately outside this boundary per FR-027 — the principle constrains what one *client* may reach, so an operator's cross-client listing is not a boundary breach; operator-scoped reads are served by separate repository methods that never accept a `client_id`, keeping the two scopes from being confused at the call site | PASS |
| IV. Traceability & Auditability | Every transition and every Net Total calculation attributable to a time + actor; history not overwritten | Append-only `LifecycleTransition` and `NetTotalCalculation` tables, insert-only (`data-model.md`) | PASS |
| V. Bulk Order as First-Class Unit | Lifecycle status, cancellation, delivery confirmation apply at order level, not line-item level; an order holds exactly one state at a time | `status`, `net_total_locked_at`, `cancelled_at` live on `BulkOrder` only; `LineItem` carries no independent lifecycle fields, making partial-state orders unrepresentable (`data-model.md`) | PASS |

No violations identified; **Complexity Tracking** is not needed for this plan.

Domain-boundary check (Governance & Boundaries): this feature does not implement
warehouse/inventory management, carrier/logistics operations, or payment
settlement — confirmed out of scope per spec Assumptions and the "Out of Scope
(Future)" list (Warehouse API/DB/UI, Invoicing API/DB/UI). The constitution's
stronger v2.0.0 statement that *hardware availability and stock replenishment are
inventory concerns and MUST NOT be modelled as order lifecycle states* is satisfied
by design: stock/availability appears nowhere in the data model, API, or UI, and an
order waiting on hardware holds `PROCESSING` (FR-005). PASS.

**Post-Phase 1 re-check**: The Phase 1 artifacts (`data-model.md`, `contracts/`)
were designed to satisfy the gate table above directly — each row's "Design
mechanism" column cites the concrete Phase 1 artifact enforcing it — rather than
being reconciled afterward. No new violations were introduced during design; all
five gates remain PASS. The one gate whose *mechanism* changed in this revision is
III: it now distinguishes client-scoped from operator-scoped reads (FR-027) and
narrows the cancellation precondition to the Cancellation Window.

### Constitution v2.0.0 reconciliation

Constitution v2.0.0 was a MAJOR amendment: it enumerated the lifecycle as exactly
four states, excluded any backordered/on-hold state, and moved the close of the
Cancellation Window from Final Delivery to Shipped. Its Sync Impact Report flagged
this plan and its artifacts as requiring reconciliation. Concretely, this re-run:

- Removed `BACKORDERED` from the `OrderStatus` enum, the transition map, the
  OpenAPI `OrderStatus` schema, and the quickstart's transition walkthrough, and
  removed the `PROCESSING ⇄ BACKORDERED` reversal so the transition map now has no
  reverse edge at all (`research.md` #4, `data-model.md`, `contracts/openapi.yaml`).
- Narrowed cancellation from "any non-terminal state" to the Cancellation Window
  (Intake or Processing), making a Shipped order Active-but-not-cancellable, and
  changed the illustrative FR-016 race from `→ FINAL_DELIVERY` to
  `PROCESSING → SHIPPED` (`research.md` #13, `data-model.md`, `quickstart.md` §7).
- Changed the lifecycle stepper from a permanent 5-column layout to 4 steps, which
  makes the existing Figma reference files correct rather than a design gap
  (FR-022; the prior revision's noted design gap is withdrawn).
- Added operator-scoped, cross-client order listing plus per-order client
  attribution (FR-027), and operator advance controls surfaced on the existing two
  pages instead of a separate operator page (FR-026).
- Added the Active Orders / Order History partition as a modelled rule — Active =
  `{INTAKE, PROCESSING, SHIPPED}`, History = `{FINAL_DELIVERY, CANCELLED}`, no
  overlap (FR-023; `data-model.md` Derived Views).

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
pom.xml                           # root Maven reactor POM (aggregates services/*)

services/                         # backend API services, one Maven module each
├── order-api/                    # Spring Boot 3.5 / Java 25 — THIS FEATURE
│   ├── pom.xml
│   ├── src/main/java/com/compudelivery/orders/
│   │   ├── order/                # BulkOrder, LineItem, OrderStatus, OrderLifecycleService, OrderController
│   │   ├── pricing/               # NetTotalCalculation, pricing service
│   │   ├── client/                # EnterpriseClient, ContractDiscountTerms
│   │   ├── catalog/               # HardwareCatalogItem
│   │   ├── identity/              # CallerIdentity filter + argument resolver (X-Client-Id / X-Operator-Id)
│   │   ├── messaging/             # RabbitMQ topology + OrderIntaken publisher
│   │   └── web/                   # ProblemDetail exception handling
│   ├── src/main/resources/
│   │   ├── application.yml
│   │   └── db/migration/          # Flyway migrations (schema + seed data)
│   └── src/test/java/com/compudelivery/orders/
│       ├── unit/                  # JUnit5/Mockito/AssertJ
│       └── integration/           # Testcontainers (PostgreSQL, RabbitMQ)
├── warehouse-api/                 # RESERVED — future feature, not scaffolded by this plan
└── invoice-api/                   # RESERVED — future feature, not scaffolded by this plan

apps/                              # frontend apps, one per persona/portal
├── order-portal/                  # React 19 / TypeScript / Vite / Tailwind — THIS FEATURE
│   ├── src/
│   │   ├── pages/                 # DashboardPage, OrderDetailPage (create/edit)
│   │   ├── components/            # LifecycleStepper (4-step), CatalogTable, PricingSummary,
│   │   │                          #   IdentitySwitcher, OrderCardActions (cancel | advance, by role)
│   │   ├── api/                   # fetch wrapper attaching X-Client-Id / X-Operator-Id
│   │   └── context/               # IdentityContext (selected demo identity + its role)
│   └── tests/                     # Vitest + React Testing Library
├── warehouse-portal/               # RESERVED — future feature, not scaffolded by this plan
└── invoice-portal/                 # RESERVED — future feature, not scaffolded by this plan

deploy/
├── docker-compose.yml             # local PostgreSQL 17 + RabbitMQ for order-api/order-portal dev
└── helm/
    ├── order-api/                 # Helm chart for this service's k3s deployment
    └── order-portal/              # Helm chart for this app's k3s deployment
```

**Structure Decision**: Web monorepo, matching the user-supplied stack and the
2026-08-19 direction that this repository will eventually house order,
warehouse, and invoice APIs and frontends as siblings. `services/` holds one
Maven module per backend API service and `apps/` holds one app per frontend
portal, each independently buildable/deployable; a root reactor `pom.xml`
aggregates the Maven modules under `services/` (`research.md` #9). This
feature implements only `services/order-api/` (package-by-feature internally)
and `apps/order-portal/` (two pages — dashboard, order detail/create — per
the reviewed Figma design and the 2026-08-17 clarifications).
`warehouse-api`/`invoice-api` and their portals are reserved directory names
only — no code, build files, or CI wiring is created for them here, since
Warehouse and Invoicing remain out of scope for this feature per the
constitution's domain boundary; a future feature scaffolds each when it is
actually built. Notably, the 2026-09-17 clarification that operators work from
the *existing* two pages (FR-026) means no operator-only page or app module is
added — role-conditional controls live in `OrderCardActions`, driven by
`IdentityContext`. `deploy/` holds the docker-compose file for this feature's
local inner-loop development and per-service/per-app Helm charts for k3s
(`research.md` #12), keeping deployment concerns out of `services/` and
`apps/`.

## Complexity Tracking

*No Constitution Check violations — this section is not applicable.*
