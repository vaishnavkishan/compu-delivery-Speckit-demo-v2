<!--
Sync Impact Report
- Version change: 1.0.0 → 1.1.0 (backorder lifecycle and editability rules)
- Modified principles:
  - I. Order Lifecycle Integrity — explicitly permits the Backordered → Processing
    availability transition
  - IV. Traceability & Auditability — explicitly covers repricing caused by permitted
    backorder edits
  - V. Bulk Order as First-Class Unit — permits order line-item edits while Backordered
- Added sections: none
- Removed sections: none
- Deferred / TODO placeholders:
  - TODO(RATIFICATION_DATE): original adoption date not provided by user input; using
    today's date as Last Amended only. Ratification date to be confirmed and backfilled.
- Templates requiring follow-up: none (constitution-only command; dependent templates read
  this file at runtime and are not modified here).
-->

# CompuDelivery Constitution

## Core Principles

### I. Order Lifecycle Integrity
Every bulk hardware order MUST progress through a single, well-defined Order Lifecycle
(Intake → Processing → Shipped → Final Delivery, with Backordered as an on-hold branch
from Processing and Cancellation as an allowed branch before Final Delivery). An order
MUST NOT skip lifecycle states or be silently or manually forced into an undefined state.
Backordered MUST be allowed to transition back to Processing when stock becomes available;
this is the sole permitted re-entry exception. Final Delivery and Cancellation are terminal
states. Rationale: enterprise clients rely on a predictable, consistent lifecycle to plan
procurement and delivery; explicit exception handling keeps temporary stock holds from
creating undefined status behavior.

### II. Contract-Driven Pricing
Net Total for any order MUST be derived exclusively from the enterprise client's current
pre-negotiated contract discount terms applied to the Gross Total; pricing MUST NOT be
manually overridden, estimated, or substituted with generic/list pricing. If contract
discount terms are missing, ambiguous, or expired for a client, the system MUST treat the
order as blocked from finalization rather than falling back to an assumed discount.
Rationale: bulk hardware orders exist specifically to honor negotiated contract economics;
any deviation breaks the commercial agreement with the client.

### III. Client Data Ownership & Access Boundary
An enterprise client MUST be able to view only their own Order History and MUST be able to
cancel only their own Active Orders — never another client's orders or data. Cancellation
MUST only be permitted while an order is Active (i.e., has not reached Final Delivery and
has not already been cancelled). Rationale: enterprise clients operate under confidentiality
and competitive-separation expectations; cross-client visibility or control is a governance
failure, not a feature gap.

### IV. Traceability & Auditability
Every lifecycle state transition and every Net Total calculation MUST be attributable to a
specific point in time and a specific triggering actor or event (client action, contract
term, or system process). Pricing and lifecycle history MUST be reconstructable after the
fact and MUST NOT be overwritten in place. A permitted line-item edit while an order is
Backordered MUST create a new Gross Total and Net Total calculation record, including the
actor, effective contract terms, and timestamp. Rationale: disputes over price or delivery
status are inevitable at enterprise scale; without traceability, they cannot be resolved
authoritatively.

### V. Bulk Order as First-Class Unit
A Bulk Order (its Line Items, aggregate pricing, and lifecycle state) MUST be treated and
tracked as a single cohesive unit rather than as a collection of independently managed
line-level orders. Line Items MAY be itemized for pricing and reporting, but lifecycle
status, cancellation, and delivery confirmation apply at the order level. The owning client
MAY add, remove, or change line items while the order is in Intake, Processing, or
Backordered; those edits MUST revalidate the order and recalculate its totals using the
currently effective contract terms. Once the order reaches Shipped, its line items and
totals MUST be read-only. Rationale: enterprise procurement is negotiated and fulfilled at
the order level; controlled edits during an unresolved stock hold preserve order
cohesion without permitting changes after shipment.

## Domain Language

- **Enterprise Client**: A business account operating under a pre-negotiated contract that
  governs its pricing (contract discounts) and order terms.
- **Bulk Order**: A single order submission from one enterprise client containing one or
  more hardware Line Items.
- **Line Item**: An individual hardware SKU and quantity within a Bulk Order.
- **Contract Discount**: A pre-negotiated, client-specific discount term defined in the
  client's contract, applied to eligible Line Items or the order as a whole.
- **Gross Total**: The sum of Line Item list prices before any Contract Discount is applied.
- **Net Total**: The final payable amount after Contract Discounts are applied to the Gross
  Total; the authoritative amount owed by the client.
- **Order Intake**: The stage at which a Bulk Order is received and validated, prior to
  entering active processing.
- **Order Lifecycle**: The defined, ordered sequence of states an order passes through from
  Order Intake to Final Delivery, including the Backordered on-hold branch from Processing
  and its permitted return to Processing when stock becomes available.
- **Active Order**: An order in any lifecycle state prior to Final Delivery or Cancellation;
  the set of states eligible for client-initiated Cancellation.
- **Final Delivery**: The terminal lifecycle state confirming the client has received the
  ordered hardware; no further state transitions are possible.
- **Cancellation**: A client-initiated termination of an Active Order; a terminal state
  distinct from Final Delivery.
- **Order History**: The read-only, chronological record of a client's own past and current
  orders, including lifecycle state and Net Total.

## Governance & Boundaries

**Domain boundary**: This constitution governs the intake, contract-based pricing,
lifecycle tracking, and client-facing history/cancellation of enterprise bulk hardware
orders. It does not govern warehouse or inventory management, carrier/logistics operations,
or payment settlement mechanics — those are separate domains and, if built, MUST be
governed by their own principles rather than assumed to inherit these.

**Data governance**: Contract discount terms are confidential to the owning Enterprise
Client and MUST NOT be exposed to, or applied on behalf of, any other client. Order History
and order-level data are scoped strictly to the owning client (see Principle III).

**Usage rules**: Net Total MUST always be computed from the client's currently effective
Contract Discount at calculation time (see Principle II). The owning client MAY edit line
items while an order is in Intake, Processing, or Backordered; each edit MUST recalculate
Gross Total and Net Total and append an audit record. After Shipped, line items and totals
MUST NOT be changed. Cancellation is only a valid operation on Active Orders (see Principle
III); attempts to cancel an order already at Final Delivery or already cancelled are invalid
operations, not silent no-ops.

## Governance

This constitution supersedes ad-hoc practice for any work touching order intake, contract
pricing, order lifecycle, or client-facing order history/cancellation. Any specification,
plan, or implementation that conflicts with a Core Principle MUST either be revised to
comply or MUST amend this constitution first, with the conflict and rationale documented.

**Amendment procedure**: Amendments are proposed via an update to this file. Each amendment
MUST update the Sync Impact Report at the top of this file, state which principles or
sections changed, and set the version per the versioning policy below.

**Versioning policy**: This constitution follows semantic versioning:
- MAJOR: Backward-incompatible removal or redefinition of a principle or governance rule.
- MINOR: A new principle, glossary term, or governance rule is added, or existing guidance
  is materially expanded.
- PATCH: Wording clarifications, typo fixes, or non-semantic refinements.

**Compliance review**: Specs, plans, and task breakdowns for this domain MUST be checked
against these principles before implementation begins; any deviation MUST be called out
explicitly with a documented justification rather than silently implemented.

**Version**: 1.1.0 | **Ratified**: TODO(RATIFICATION_DATE): original adoption date not
supplied — confirm with project owner | **Last Amended**: 2026-09-08
