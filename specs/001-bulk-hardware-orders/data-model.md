# Phase 1 Data Model: Bulk Hardware Order Management

Entities below map directly to the spec's Key Entities section, refined with the
concrete columns needed to satisfy the Functional Requirements and Constitution
Principles I–V. Storage is PostgreSQL 17, managed by Flyway migrations, in a single
schema owned by the order service.

Revised 2026-09-17 for constitution **v2.0.0**: the `status` enum drops
`BACKORDERED`, the transition map drops the `PROCESSING ⇄ BACKORDERED` reversal, and
cancellation is confined to the Cancellation Window (`INTAKE` or `PROCESSING`). Also
adds the `Operator` entity and the Derived Views section needed by FR-022, FR-023,
FR-026 and FR-027.

## Entity Overview

```
EnterpriseClient 1───* ContractDiscountTerms
EnterpriseClient 1───* BulkOrder
BulkOrder        1───* LineItem
BulkOrder        1───* LifecycleTransition
BulkOrder        1───* NetTotalCalculation
BulkOrder        0..1─1 CancellationRecord
HardwareCatalogItem 1───* LineItem  (by SKU, reference-only)
Operator         (standalone; referenced by LifecycleTransition.actor_id only)
```

## EnterpriseClient

Business account under contract; owns Orders and Order History.

| Field | Type | Notes |
|---|---|---|
| `client_id` | `varchar` (PK) | Caller-supplied identifier, e.g. `ACME-001`. Trusted as-is (no login). |
| `display_name` | `varchar` | Shown in header badge and demo identity switcher. |
| `created_at` | `timestamptz` | Seed/record time. |

Seeded via Flyway with a small fixed set of demo clients (FR-019).

## Operator

Internal staff identity permitted to advance an order's lifecycle status (FR-012).
An Operator is **not** an EnterpriseClient and owns no orders; it sits outside the
per-client access boundary and may read every client's orders (FR-027,
Constitution III).

| Field | Type | Notes |
|---|---|---|
| `operator_id` | `varchar` (PK) | Caller-supplied identifier, e.g. `OPS-1`. Trusted as-is (no login). |
| `display_name` | `varchar` | Shown in the demo identity switcher. |
| `created_at` | `timestamptz` | Seed/record time. |

Seeded via Flyway alongside the demo clients; both sets are exposed read-only
through `GET /api/demo-identities` with a `role` discriminator (`research.md` #2).
No foreign key ties `LifecycleTransition.actor_id` to this table, since that column
also holds client identifiers and the literal `SYSTEM`.

## ContractDiscountTerms

Time-bounded, client-specific discount used exclusively to compute Net Total.

| Field | Type | Notes |
|---|---|---|
| `id` | `uuid` (PK) | |
| `client_id` | `varchar` (FK → EnterpriseClient) | |
| `discount_percentage` | `numeric(5,2)` | e.g. `12.50` = 12.5%. |
| `effective_from` | `timestamptz` | Inclusive. |
| `effective_until` | `timestamptz`, nullable | Inclusive; null = open-ended. |
| `created_at` | `timestamptz` | |

**Validation rules**:
- "Current" terms for a client = the row where `effective_from <= now()` and
  (`effective_until IS NULL OR effective_until >= now()`).
- Zero matching rows → contract terms missing/expired → block order (FR-004).
- More than one matching row → ambiguous → block order (FR-004). Application-level
  invariant (not a DB constraint) enforced at order-submission time, since seed data
  is curated and this is a demo; documented as a known simplification.

## BulkOrder

The order as a whole; single source of lifecycle status, aggregate pricing.

| Field | Type | Notes |
|---|---|---|
| `id` | `uuid` (PK) | |
| `client_id` | `varchar` (FK → EnterpriseClient) | Owning client; every **client-identity** query is scoped by this (FR-009). Operator-identity queries are deliberately unscoped and instead project this column for on-screen attribution (FR-027). |
| `status` | `varchar`/enum | `INTAKE, PROCESSING, SHIPPED, FINAL_DELIVERY, CANCELLED` — exactly the four constitutional lifecycle states plus the terminal cancellation state. No `BACKORDERED`/on-hold member exists, so an out-of-lifecycle status is unrepresentable (Constitution I, FR-005). |
| `gross_total` | `numeric(12,2)` | Sum of line item subtotals as submitted (FR-002). |
| `applied_discount_percentage` | `numeric(5,2)` | Snapshot of the contract discount used for the current `net_total` (Constitution IV). |
| `net_total` | `numeric(12,2)` | `gross_total * (1 - applied_discount_percentage/100)`. |
| `net_total_locked_at` | `timestamptz`, nullable | Set when status transitions to `SHIPPED`; once set, line items and totals are read-only (FR-017). |
| `version` | `bigint` | JPA `@Version` — optimistic lock for first-committed-wins (FR-016). |
| `created_at` | `timestamptz` | Intake time. |
| `updated_at` | `timestamptz` | Last mutation time. |
| `cancelled_at` | `timestamptz`, nullable | Denormalized convenience; authoritative record is `CancellationRecord`. |

**Validation rules**:
- MUST have ≥1 LineItem to be created (FR-015).
- `status` transitions only via the allowed-transitions map (FR-006; see State
  Transitions below).
- Line items and `gross_total`/`net_total` are mutable only while `status IN
  (INTAKE, PROCESSING)` (FR-025); immutable once `net_total_locked_at` is set.
- An order awaiting hardware availability holds `PROCESSING`; no status represents
  stock state (FR-005, Constitution Governance & Boundaries).

**State Transitions** (FR-005, FR-006):

```
INTAKE ──▶ PROCESSING ──▶ SHIPPED ──▶ FINAL_DELIVERY  (terminal)
   │            │
   └────────────┴──▶ CANCELLED  (terminal, client-initiated only)

        └─ Cancellation Window ─┘   closes on entering SHIPPED, never reopens
```

- Forward progression (`INTAKE→PROCESSING→SHIPPED→FINAL_DELIVERY`) is
  operator-initiated (FR-012) and advances exactly one stage at a time.
- Cancellation is client-initiated (FR-010) and valid **only** from `INTAKE` or
  `PROCESSING` — the Cancellation Window. A `SHIPPED` order is Active but not
  cancellable; the attempt is rejected explicitly, never as a silent no-op
  (FR-011, Constitution III).
- There are no reverse edges and no self-edges: a backward move, a skipped stage,
  or any change from a terminal state is rejected (FR-006).
- `FINAL_DELIVERY` and `CANCELLED` are terminal — no further transitions accepted
  (FR-006, acceptance scenarios in User Story 4).
- Entering `SHIPPED` is the single point that both stamps `net_total_locked_at`
  (FR-017) and closes the Cancellation Window (FR-010), so one transition handler
  enforces both (`research.md` #5, #13).

## LineItem

An individual SKU + quantity within one order.

| Field | Type | Notes |
|---|---|---|
| `id` | `uuid` (PK) | |
| `order_id` | `uuid` (FK → BulkOrder) | |
| `sku` | `varchar` (FK → HardwareCatalogItem) | Must match a known catalog SKU (FR-014). |
| `quantity` | `integer` | Must be > 0 (FR-014). |
| `unit_list_price` | `numeric(12,2)` | Snapshot of catalog list price at time of add, so a later catalog price change never rewrites an existing line item's contribution to a past Gross Total. |
| `line_subtotal` | `numeric(12,2)` | `quantity * unit_list_price`. |

**Validation rules**:
- The same SKU MAY appear on multiple line items within one order; each is kept
  distinct and summed, never auto-merged (Key Entities, Line Item).
- Rejected at submission/edit time if `quantity <= 0` or `sku` not found in
  HardwareCatalogItem (FR-014).

## HardwareCatalogItem

Fixed, system-defined reference catalog; source of valid SKUs and list prices.

| Field | Type | Notes |
|---|---|---|
| `sku` | `varchar` (PK) | |
| `name` | `varchar` | |
| `list_price` | `numeric(12,2)` | |

Seeded via Flyway to match the Figma reference catalog; no create/update/delete API
in this feature's scope (Key Entities, Hardware Catalog Item).

## LifecycleTransition

Append-only history of every status change.

| Field | Type | Notes |
|---|---|---|
| `id` | `uuid` (PK) | |
| `order_id` | `uuid` (FK → BulkOrder) | |
| `from_status` | `varchar`/enum, nullable | Null for the initial `INTAKE` row. |
| `to_status` | `varchar`/enum | |
| `actor_type` | `varchar`/enum | `CLIENT`, `OPERATOR`, or `SYSTEM`. |
| `actor_id` | `varchar` | The caller-supplied client/operator identifier, or `SYSTEM` for the initial intake row. |
| `occurred_at` | `timestamptz` | |

**Validation rules**: rows are never updated or deleted after insert (FR-007,
Constitution IV).

## NetTotalCalculation

Append-only history of every Net Total calculation (initial intake and each
subsequent line-item-edit recalculation).

| Field | Type | Notes |
|---|---|---|
| `id` | `uuid` (PK) | |
| `order_id` | `uuid` (FK → BulkOrder) | |
| `gross_total` | `numeric(12,2)` | |
| `applied_discount_percentage` | `numeric(5,2)` | |
| `net_total` | `numeric(12,2)` | |
| `trigger` | `varchar`/enum | `INTAKE` or `LINE_ITEM_EDIT`. |
| `actor_id` | `varchar` | Client identifier that triggered the calculation. |
| `occurred_at` | `timestamptz` | |

**Validation rules**: rows are never updated or deleted after insert (FR-007,
FR-017, FR-025).

## CancellationRecord

Timestamped record that a client cancelled a specific order from within the
Cancellation Window.

| Field | Type | Notes |
|---|---|---|
| `id` | `uuid` (PK) | |
| `order_id` | `uuid` (FK → BulkOrder, unique) | At most one cancellation per order. |
| `cancelled_by` | `varchar` | Client identifier. |
| `cancelled_at` | `timestamptz` | |

Created exactly once, at the moment `BulkOrder.status` transitions to `CANCELLED`
from `INTAKE` or `PROCESSING`. The unique constraint on `order_id` is the last line
of defence; the service rejects a second cancellation attempt, and any attempt on a
`SHIPPED` or `FINAL_DELIVERY` order, before a second record could be created
(FR-011, User Story 3 acceptance scenarios 2 and 4).

## Derived Views (no stored state)

Three rules the API and UI both depend on are **derived from `status`**, never stored
as columns, so they cannot drift out of sync with the lifecycle (`research.md` #13):

| Derived value | Definition | Consumers |
|---|---|---|
| `cancellable` | `status IN (INTAKE, PROCESSING)` — the Cancellation Window | Cancel service precondition (FR-010/FR-011); `cancellable` boolean on the order payload driving the UI's enabled/disabled cancel control (FR-022) |
| `active` | `status IN (INTAKE, PROCESSING, SHIPPED)` | Dashboard's Active Orders section; its complement `status IN (FINAL_DELIVERY, CANCELLED)` is the Order History Log (FR-023) |
| `nextStatus` | the single outgoing edge from `status` in the transition map, or none for a terminal status | Operator's "Advance to <next stage>" control label and the server's advance validation (FR-006, FR-026) |

Because `active` is a total predicate over the enum, Active Orders and Order History
are exact complements: no order can be absent from both sections or present in both
(FR-023). `cancellable` is a strict subset of `active` — a `SHIPPED` order is Active
but outside the Cancellation Window, which is precisely the distinction constitution
v2.0.0 introduced.

## Cross-Entity Invariants (traced to gates)

- Every read of BulkOrder/LineItem/LifecycleTransition/NetTotalCalculation for a
  **client identity** is filtered by `client_id = <caller's identity>` at the
  repository/query level, never filtered only in the response serializer
  (Constitution III, FR-009).
- An order ID that doesn't exist, or exists but belongs to a different
  `client_id`, produces the identical generic 404 response (FR-018) — enforced by
  having the repository query include `client_id` in its `WHERE` clause so "not
  mine" and "doesn't exist" are indistinguishable at the data-access layer, not
  just at the response-formatting layer.
- Reads for an **operator identity** are served by separate repository methods that
  take no `client_id` parameter at all and return orders across every client, each
  carrying its owning `client_id` and client display name (FR-027). No repository
  method accepts a nullable `client_id` whose null would silently widen scope, so a
  client-scoped call site cannot become cross-client by omission (`research.md` #13).
  Operators are outside the per-client boundary by design and are excluded from
  SC-006.
- Writes are role-partitioned: `status` advancement is reachable only from an
  operator identity (FR-012, FR-026), while cancellation and line-item edits are
  reachable only from the owning client identity (FR-010, FR-025). No endpoint
  accepts either header interchangeably for a write.
- `CancellationRecord` exists if and only if `BulkOrder.status = CANCELLED`, and
  `BulkOrder.cancelled_at` equals that record's `cancelled_at`.
- `net_total_locked_at` is non-null if and only if the order has reached `SHIPPED`
  or `FINAL_DELIVERY`; while it is non-null, no `LineItem` row for that order and no
  `gross_total`/`net_total`/`applied_discount_percentage` value may change (FR-017,
  FR-025).
- `LifecycleTransition` rows for one order form an unbroken chain: the first row has
  `from_status = NULL, to_status = INTAKE`, and each subsequent row's `from_status`
  equals the previous row's `to_status`, matching an edge in the transition map. A
  gap or a non-map edge in this chain is evidence of a Principle I violation and is
  what the audit in SC-005 reconstructs.
