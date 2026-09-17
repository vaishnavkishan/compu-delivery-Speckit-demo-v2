<!--
Sync Impact Report
- Version change: 1.0.0 → 2.0.0 (MAJOR: backward-incompatible redefinition of the Order
  Lifecycle and of the cancellation window)
- Modified principles:
  - I. Order Lifecycle Integrity — lifecycle is now enumerated as exactly four ordered
    states (Order Intake → Processing → Shipped → Final Delivery). Backordered / on-hold is
    explicitly excluded as a lifecycle state or branch. Cancellation branch now terminates
    at Shipped rather than at Final Delivery.
  - III. Client Data Ownership & Access Boundary — client-initiated Cancellation is now
    permitted only prior to Shipped (previously: prior to Final Delivery). Orders in
    Shipped are Active but no longer cancellable.
  - V. Bulk Order as First-Class Unit — title and intent unchanged; wording aligned to the
    enumerated lifecycle.
- Added sections:
  - Domain Language: added "Processing", "Shipped", and "Cancellation Window" terms.
- Removed sections: none. No principle or governance section was deleted.
- Deferred / TODO placeholders:
  - TODO(RATIFICATION_DATE): original adoption date still not supplied; carried forward
    from v1.0.0 pending confirmation by the project owner.
- Follow-up required outside this command (dependent artifacts read this file at runtime and
  are NOT modified here):
  - specs/001-bulk-hardware-orders/ (spec.md FR-005/FR-006/FR-022/FR-023, data-model.md
    status enum and transition diagram, plan.md, tasks.md, research.md, quickstart.md,
    checklists/requirements.md, analysis-report.md) and README.md still describe a
    Backordered stage, a 5-column lifecycle stepper, and cancellation up to Final Delivery.
    These now conflict with Principles I and III and must be reconciled via the normal
    spec workflow.
-->

# CompuDelivery Constitution

## Core Principles

### I. Order Lifecycle Integrity
Every bulk hardware order MUST progress through a single, well-defined Order Lifecycle
consisting of exactly four ordered states: **Order Intake → Processing → Shipped → Final
Delivery**. Cancellation is the only permitted branch out of this progression, and it is
available at any point prior to Shipped. An order MUST NOT skip a lifecycle state, MUST NOT
re-enter a prior state once it has advanced, and MUST NOT be silently or manually forced
into a state outside this defined progression. No on-hold, backordered, suspended, or
equivalent intermediate state exists in this lifecycle, and none MAY be introduced without
first amending this constitution. Rationale: enterprise clients rely on a predictable,
consistent lifecycle to plan procurement and delivery; undefined, optional, or reversible
states make order status ambiguous and therefore meaningless.

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
cancel only their own orders — never another client's orders or data. Client-initiated
Cancellation MUST only be permitted while an order is within the Cancellation Window, that
is, in Order Intake or Processing and not already cancelled. Once an order has reached
Shipped it MUST NOT be cancellable, because the hardware has left CompuDelivery's control.
Rationale: enterprise clients operate under confidentiality and competitive-separation
expectations, so cross-client visibility or control is a governance failure; and a
cancellation that cannot actually stop a shipment is a false promise to the client.

### IV. Traceability & Auditability
Every lifecycle state transition and every Net Total calculation MUST be attributable to a
specific point in time and a specific triggering actor or event (client action, contract
term, or system process). Pricing and lifecycle history MUST be reconstructable after the
fact and MUST NOT be overwritten in place. Rationale: disputes over price or delivery status
are inevitable at enterprise scale; without traceability, they cannot be resolved
authoritatively.

### V. Bulk Order as First-Class Unit
A Bulk Order (its Line Items, aggregate pricing, and lifecycle state) MUST be treated and
tracked as a single cohesive unit rather than as a collection of independently managed
line-level orders. Line Items MAY be itemized for pricing and reporting, but lifecycle
state, cancellation, and delivery confirmation apply at the order level. A Bulk Order MUST
hold exactly one lifecycle state at a time; partial-state orders (for example, some Line
Items Shipped while others are not) are not representable. Rationale: enterprise
procurement is negotiated and fulfilled at the order level; fragmenting lifecycle tracking
per line item creates status ambiguity the client did not ask for.

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
- **Order Lifecycle**: The defined, ordered sequence of exactly four states an order passes
  through — Order Intake, Processing, Shipped, Final Delivery — with Cancellation as the
  only permitted branch.
- **Order Intake**: The first lifecycle state, at which a Bulk Order is received and
  validated prior to entering active processing.
- **Processing**: The second lifecycle state, during which the order is prepared for
  dispatch. It is the last state within the Cancellation Window.
- **Shipped**: The third lifecycle state, at which the order has been dispatched and has
  left CompuDelivery's control. Entering Shipped permanently closes the Cancellation Window.
- **Final Delivery**: The terminal lifecycle state confirming the client has received the
  ordered hardware; no further state transitions are possible.
- **Cancellation**: A client-initiated termination of an order within the Cancellation
  Window; a terminal state distinct from Final Delivery.
- **Cancellation Window**: The span of the lifecycle during which Cancellation is a valid
  operation — Order Intake and Processing only. It closes on transition to Shipped and does
  not reopen.
- **Active Order**: An order in any non-terminal lifecycle state — Order Intake, Processing,
  or Shipped. Active is not the same as cancellable: a Shipped order is Active but outside
  the Cancellation Window.
- **Order History**: The read-only, chronological record of a client's own past and current
  orders, including lifecycle state and Net Total.

## Governance & Boundaries

**Domain boundary**: This constitution governs the intake, contract-based pricing,
lifecycle tracking, and client-facing history/cancellation of enterprise bulk hardware
orders. It does not govern warehouse or inventory management, carrier/logistics operations,
or payment settlement mechanics — those are separate domains and, if built, MUST be
governed by their own principles rather than assumed to inherit these. In particular,
hardware availability and stock replenishment are inventory concerns and MUST NOT be
modelled as order lifecycle states (see Principle I).

**Data governance**: Contract discount terms are confidential to the owning Enterprise
Client and MUST NOT be exposed to, or applied on behalf of, any other client. Order History
and order-level data are scoped strictly to the owning client (see Principle III).

**Usage rules**: Net Total MUST always be computed from the client's currently effective
Contract Discount at calculation time (see Principle II). Cancellation is only a valid
operation on orders inside the Cancellation Window (see Principle III); attempts to cancel
an order that is Shipped, at Final Delivery, or already cancelled are invalid operations
that MUST be rejected explicitly, not treated as silent no-ops.

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
explicitly with a documented justification rather than silently implemented. A MAJOR
amendment obliges a re-check of all existing spec artifacts in this domain against the
amended principles.

**Version**: 2.0.0 | **Ratified**: TODO(RATIFICATION_DATE): original adoption date not
supplied — confirm with project owner | **Last Amended**: 2026-09-17
