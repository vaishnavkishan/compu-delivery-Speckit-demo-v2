# Phase 1 Data Model: Bulk Hardware Order Management

Entities below map directly to the spec's Key Entities section, refined with the
concrete columns needed to satisfy the Functional Requirements and Constitution
Principles I–V. Storage is PostgreSQL 17, managed by Flyway migrations, in a single
schema owned by the order service.

## Entity Overview

```
EnterpriseClient 1───* ContractDiscountTerms
EnterpriseClient 1───* BulkOrder
BulkOrder        1───* LineItem
BulkOrder        1───* LifecycleTransition
BulkOrder        1───* NetTotalCalculation
BulkOrder        0..1─1 CancellationRecord
HardwareCatalogItem 1───* LineItem  (by SKU, reference-only)
```

## EnterpriseClient

Business account under contract; owns Orders and Order History.

| Field | Type | Notes |
|---|---|---|
| `client_id` | `varchar` (PK) | Caller-supplied identifier, e.g. `ACME-001`. Trusted as-is (no login). |
| `display_name` | `varchar` | Shown in header badge and demo identity switcher. |
| `created_at` | `timestamptz` | Seed/record time. |

Seeded via Flyway with a small fixed set of demo clients covering every
contract-terms state FR-027 requires: two with valid but differing discounts,
one with missing terms, one with expired terms, and one with ambiguous terms
(see `research.md` #2 for the concrete roster).

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
- A matching row whose `discount_percentage` is not strictly between 0 and 100
  (exclusive of both bounds) is also treated as ambiguous/invalid and blocks
  the order via the same FR-004 path, since applying it would yield a zero or
  negative Net Total.
- A blocked order (any of the above) is rejected synchronously and creates
  **no** `BulkOrder` or `LineItem` row — validation runs before any INSERT in
  the request's transaction (FR-004). The same rule applies to an in-flight
  edit under FR-025: a blocked edit leaves the order's existing `LineItem`
  rows and its last-valid `gross_total`/`net_total` untouched.

## BulkOrder

The order as a whole; single source of lifecycle status, aggregate pricing.

| Field | Type | Notes |
|---|---|---|
| `id` | `uuid` (PK) | |
| `client_id` | `varchar` (FK → EnterpriseClient) | Owning client; all queries scoped by this (FR-009). |
| `status` | `varchar`/enum | `INTAKE, PROCESSING, BACKORDERED, SHIPPED, FINAL_DELIVERY, CANCELLED`. |
| `gross_total` | `numeric(12,2)` | Sum of line item subtotals as submitted (FR-002); exact by construction, no rounding applied here. |
| `applied_discount_percentage` | `numeric(5,2)` | Snapshot of the contract discount used for the current `net_total` (Constitution IV). |
| `net_total` | `numeric(12,2)` | `round_half_up(gross_total - gross_total * applied_discount_percentage/100, 2)` — the discount arithmetic is carried at full precision and rounded exactly once, at this final step (FR-003; `research.md` #13). |
| `net_total_locked_at` | `timestamptz`, nullable | Set when status transitions to `SHIPPED`; once set, line items and totals are read-only (FR-017). |
| `version` | `bigint` | JPA `@Version` — optimistic lock for first-committed-wins (FR-016). |
| `created_at` | `timestamptz` | Intake time. |
| `updated_at` | `timestamptz` | Last mutation time. |
| `cancelled_at` | `timestamptz`, nullable | Denormalized convenience; authoritative record is `CancellationRecord`. |

**Validation rules**:
- MUST have ≥1 and ≤100 LineItems to be created or edited (FR-015, FR-030).
- `status` transitions only via the allowed-transitions map (FR-006; see State
  Transitions below).
- Line items and `gross_total`/`net_total` are mutable only while `status IN
  (INTAKE, PROCESSING)` (FR-025); immutable once `net_total_locked_at` is set.

**Indexes**: `(client_id, created_at DESC, id DESC)` backs both the
per-client ownership filter (FR-009) and the newest-first, stable-across-pages
Order History ordering (FR-008; `research.md` #14) in one scan.

**State Transitions** (FR-005, FR-006, edge case on Backordered):

```
INTAKE ──▶ PROCESSING ──▶ SHIPPED ──▶ FINAL_DELIVERY  (terminal)
              │  ▲
              ▼  │
          BACKORDERED

INTAKE, PROCESSING, BACKORDERED, SHIPPED ──▶ CANCELLED  (terminal, client-initiated only)
```

- Forward progression (`INTAKE→PROCESSING→SHIPPED→FINAL_DELIVERY`) and the
  `PROCESSING↔BACKORDERED` reversal are operator-initiated (FR-012).
- Cancellation is client-initiated (FR-010) and valid from any non-terminal state.
- `FINAL_DELIVERY` and `CANCELLED` are terminal — no further transitions accepted
  (FR-006, acceptance scenarios in User Story 4).

## LineItem

An individual SKU + quantity within one order.

| Field | Type | Notes |
|---|---|---|
| `id` | `uuid` (PK) | |
| `order_id` | `uuid` (FK → BulkOrder) | |
| `sku` | `varchar` (FK → HardwareCatalogItem) | Must match a known catalog SKU (FR-014). |
| `quantity` | `integer` | Must be between 1 and 10,000 inclusive (FR-014). |
| `unit_list_price` | `numeric(12,2)` | Snapshot of catalog list price at time of add, so a later catalog price change never rewrites an existing line item's contribution to a past Gross Total. |
| `line_subtotal` | `numeric(12,2)` | `quantity * unit_list_price`. |

**Validation rules**:
- The same SKU MAY appear on multiple line items within one order; each is kept
  distinct and summed, never auto-merged (Key Entities, Line Item). Editing an
  order (FR-025) and adding a SKU that already exists as a separate line item
  creates another distinct line item rather than merging into it.
- Rejected at submission/edit time if `quantity <= 0`, `quantity > 10000`, or
  `sku` not found in HardwareCatalogItem (FR-014), or if the edit/submission
  would leave the order with more than 100 line items in total (FR-030).

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

**Validation rules**: rows are never updated or deleted after insert and are
retained indefinitely — no archival or deletion job exists in this feature's
scope (FR-007, Constitution IV). `occurred_at` is recorded to at least
1-second precision (`timestamptz` naturally exceeds this).

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

Timestamped record that a client cancelled a specific Active Order.

| Field | Type | Notes |
|---|---|---|
| `id` | `uuid` (PK) | |
| `order_id` | `uuid` (FK → BulkOrder, unique) | At most one cancellation per order. |
| `cancelled_by` | `varchar` | Client identifier. |
| `cancelled_at` | `timestamptz` | |

Created exactly once, at the moment `BulkOrder.status` transitions to `CANCELLED`;
a second cancellation attempt is rejected before a second record could be created
(FR-011, User Story 3 acceptance scenario 3).

## Cross-Entity Invariants (traced to gates)

- Every read of BulkOrder/LineItem/LifecycleTransition/NetTotalCalculation for a
  client-facing endpoint is filtered by `client_id = <caller's identity>` at the
  repository/query level, never filtered only in the response serializer
  (Constitution III, FR-009).
- An order ID that doesn't exist, or exists but belongs to a different
  `client_id`, produces the identical generic 404 response (FR-018) — enforced by
  having the repository query include `client_id` in its `WHERE` clause so "not
  mine" and "doesn't exist" are indistinguishable at the data-access layer, not
  just at the response-formatting layer.
- A first-committed-wins rejection (`version` mismatch) on any mutation
  endpoint re-reads the order's now-current `status` and includes it in the
  409 response body, not just a generic conflict message (FR-016;
  `research.md` #3).
- An operator viewing `GET /orders`/`GET /orders/{orderId}` supplies both
  `X-Operator-Id` and `X-Client-Id`; the query is scoped by `client_id`
  exactly as it is for a client caller — the operator header only marks the
  actor type for `LifecycleTransition.actor_type` on any subsequent
  operator-only write, it does not widen the read's scope beyond that one
  client (FR-026; `research.md` #1).
