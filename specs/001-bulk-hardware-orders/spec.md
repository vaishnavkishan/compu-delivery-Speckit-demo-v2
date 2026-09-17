# Feature Specification: Bulk Hardware Order Management

**Feature Branch**: `002-speckit-specify`

**Created**: 2026-08-13

**Status**: Draft

**Input**: User description: "We need a system that accepts bulk hardware orders from enterprise clients, calculates their final net totals using pre-negotiated contract discounts, and tracks each order through its lifecycle from intake to final delivery. Clients should be able to view their order history or cancel active requests."

## Clarifications

### Session 2026-08-13

- Q: When a cancellation request and an operator's Final Delivery advancement hit the same order at nearly the same moment, which one should win? → A: First to commit wins — whichever request reaches persistent storage first is applied; the other is rejected with a clear conflict message. **(Still in force 2026-09-17, but the illustrative race changed: with the Cancellation Window now closing at Shipped, the contended advancement is Processing → Shipped, not → Final Delivery. See Session 2026-09-17.)**
- Q: Should the order lifecycle use exactly the four assumed stages (Intake → Processing → Shipped → Final Delivery), or a different set? → A: Add a Backordered/On-Hold stage, entered from Processing when hardware isn't immediately available and exited back to Processing once stock becomes available. **(Superseded 2026-09-17: constitution v2.0.0 Principle I enumerates exactly four lifecycle states and explicitly excludes any backordered/on-hold state; the lifecycle is now Intake → Processing → Shipped → Final Delivery. See Session 2026-09-17.)**
- Q: If a client's contract discount terms change after an order's Net Total was already calculated but before Final Delivery, should the order reprice? → A: No — Net Total is locked at intake and is not recalculated due to later contract renegotiation. **(Superseded 2026-08-17: the lock now takes effect on entering Shipped, not at intake, because Line Items stay editable through Processing — see FR-017 and FR-025.)**
- Q: If a client submits a bulk order listing the same hardware SKU twice as separate line items, what should the system do? → A: Allow as separate line items; each submitted line item is kept distinct and Gross Total sums all line items as submitted, with no auto-merging.
- Q: When a client tries to view or cancel an order ID that either doesn't exist or belongs to a different client, should the response be the same in both cases? → A: Yes — a single generic "not found" response for both, so a client cannot distinguish nonexistence from another client's ownership (see FR-018).
- Q: Since this demo won't implement authentication or authorization, how should the system know which enterprise client or operator is behind a given request? → A: Requests include a caller-supplied client/operator identifier that the system trusts without verifying credentials; per-client data scoping and operator-only restrictions still apply based on that identifier, but no login or credential verification is performed.
- Q: The reference UI design shows a second "Bulk Volume Tier" discount line (an extra 5% off at 50+ units) alongside the Contract Discount line — should this generic volume-based discount be part of the implemented pricing model? → A: No — drop the volume tier; the implemented UI and pricing model use a single Contract Discount line only, consistent with FR-003's rule that Net Total derives exclusively from the client's contract terms.
- Q: The reference UI design has no login screen or identifier input — it shows a static, already-resolved client badge. Since the system trusts a caller-supplied identifier with no login, how should the UI actually capture that identifier? → A: A demo client/operator switcher (dropdown/selector) lets the user pick among a small set of demo identities to view the portal scoped to that identifier, for testing and demonstration purposes.
- Q: The reference UI's lifecycle stepper shows only 4 steps (Intake → Processing → Shipped → Delivered), but the spec's lifecycle has a 5th stage, Backordered. How should Backordered appear in that stepper? → A: Expand the stepper to a permanent 5-column layout (Intake, Processing, Backordered, Shipped, Delivered) shown for every order, whether or not that order ever goes on hold. **(Superseded 2026-09-17: the stepper is a 4-step layout — Intake, Processing, Shipped, Final Delivery — following the removal of Backordered. See Session 2026-09-17.)**
- Q: The reference UI's catalog table lists a fixed set of hardware items (name, SKU, MSRP); the spec doesn't define where valid SKUs, names, and list prices come from. What is the catalog's scope? → A: A fixed, system-defined, non-editable reference catalog (matching the Figma's items) is the source of valid SKUs and list prices for Gross Total; catalog management (add/edit/remove items) is out of scope for this feature.
- Q: The reference UI shows a "Freight & Logistics" summary line that always reads "Waived," with no cost calculation. Should this remain purely informational, or does it imply real freight cost logic? → A: Freight/logistics stays informational only — the UI always shows "Waived" with no cost calculation, and freight is never added to Net Total, consistent with the constitution's domain boundary excluding carrier/logistics operations.

### Session 2026-08-17

- Q: The figma-designs folder now includes an Invoice Staff billing dashboard (tax invoices, sales-tax calculation, payment/remittance tracking) and a Warehouse Operator dashboard (per-unit serial asset tagging, carrier/waybill capture). Are these part of this feature's scope? → A: Out of scope for this feature (001-bulk-hardware-orders) — consistent with the constitution's domain boundary excluding payment settlement mechanics and warehouse/carrier operations. The Invoice Staff and Warehouse Operator portals (invoice-staff-dashboard.html, invoice-staff-detail-page.html, warehouse-ops-dashboard.html, warehouse-ops-detail-page.html) will be addressed by a separate, future feature and are not reference designs for this spec.
- Q: enterprise-user-dashboard.html and enterprise-user-detail-page.html show only a 4-step lifecycle stepper (Intake, Processing, Shipped, Delivered) with no Backordered step, contradicting FR-022's already-decided permanent 5-column stepper. Does this change the FR-022 decision? → A: No — FR-022's 5-column stepper (Intake, Processing, Backordered, Shipped, Delivered) remains the decision. The 4-step stepper in these two new reference files is a design gap to be corrected during implementation, not a reversal. **(Superseded 2026-09-17: Backordered has been removed from the lifecycle, so the reference files' 4-step stepper is now correct and is no longer a design gap. See Session 2026-09-17.)**
- Q: enterprise-user-dashboard.html and enterprise-user-detail-page.html split the original single-page portal into a dashboard page (active orders + history) and a separate order detail/create page (catalog + pricing panel). Should the spec adopt this two-page structure? → A: Yes — the UI Interface Overview now describes a dashboard page and a linked order detail/create page in place of the original single page.
- Q: enterprise-user-detail-page.html lets a client edit an already-submitted order's line items while it is in Intake or Processing status ("View / Edit Order"), which the spec previously did not describe and which appeared to conflict with FR-017's "locked at intake" wording. Should this editing capability be part of the spec? → A: Yes — clients MAY edit Line Items while an order is in Intake or Processing (new FR-025), with Gross Total and Net Total recalculated on each edit using currently effective contract terms; FR-017 is revised so the Net Total lock now takes effect once the order advances beyond Processing (upon entering Shipped) rather than at intake.

### Session 2026-09-17

- Q: Now that the constitution forbids any on-hold or backordered lifecycle state, what should happen to an order in Processing when the hardware isn't immediately available? → A: Drop Backordered entirely — the order stays in Processing until it can ship, and hardware availability is not represented in this feature at all (inventory and stock replenishment are a separate domain per the constitution's Governance & Boundaries).
- Q: When a client opens a Shipped order — now outside the cancellation window under Principle III — what should they see where the cancel button used to be? → A: The cancel control stays visible but disabled, with an inline explanation that the order has shipped and can no longer be cancelled; the API also rejects any direct cancellation attempt explicitly. The Cancellation Window is Intake and Processing only.
- Q: Where in the portal does an internal operator actually advance an order to the next lifecycle stage? → A: On the existing two pages — when an operator identity is active, each order card gains an "Advance to next stage" control in place of the client's cancel control; no separate operator page is introduced.
- Q: When an operator identity is selected, whose orders should the dashboard list? → A: All clients' orders — operators are internal staff and sit outside the per-client access boundary, with each order attributed to its owning client on screen. FR-009's restriction applies to client identities only; SC-006's zero-cross-client-access assertion likewise concerns client identities.
- Q: Should the dashboard's Order History Log list every order, or only orders that have finished (Delivered or Cancelled)? → A: Only finished orders — Active Orders holds Intake, Processing and Shipped; Order History holds Final Delivery and Cancelled. The two sections do not overlap and no order appears twice.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Submit Bulk Order and Receive Contract-Priced Net Total (Priority: P1)

An enterprise client submits a bulk hardware order consisting of one or more hardware line items (SKU and quantity). The system validates the order, applies the client's current pre-negotiated contract discount terms, and returns the calculated Net Total before the order is accepted into the fulfillment pipeline.

**Why this priority**: This is the core transaction the entire system exists to support. Without accurate, contract-driven pricing at intake, no other capability (tracking, history, cancellation) has anything meaningful to operate on.

**Independent Test**: Can be fully tested by submitting a bulk order for a client with known contract discount terms and verifying the returned Net Total matches Gross Total minus the contract discount, and that the order is recorded in Intake status.

**Acceptance Scenarios**:

1. **Given** an enterprise client with active, current contract discount terms, **When** the client submits a bulk order with valid hardware line items, **Then** the system calculates the Gross Total, applies the contract discount, returns the Net Total, and creates the order in Intake status.
2. **Given** an enterprise client whose contract discount terms are missing or expired, **When** the client submits a bulk order, **Then** the system blocks the order from being finalized and clearly indicates the reason instead of applying a default or estimated discount.
3. **Given** a submitted bulk order with multiple line items, **When** the Net Total is calculated, **Then** the calculation is based on the order as a whole and is traceable to the specific contract discount terms and point in time used.
4. **Given** an enterprise client's order in Intake or Processing status, **When** the client edits the order's line items (adds, removes, or changes a quantity), **Then** the system recalculates the Gross Total and Net Total using the client's current contract discount terms and records the recalculation.

---

### User Story 2 - View Order History and Status (Priority: P2)

An enterprise client views a list of their own bulk orders — past and current — including each order's lifecycle status and Net Total, without seeing any other client's orders.

**Why this priority**: Visibility into order status and history is explicitly requested and is the primary way clients build trust in the system after submitting an order.

**Independent Test**: Can be fully tested by creating orders for two different clients, then confirming that each client's order history view shows only that client's own orders with correct status and Net Total.

**Acceptance Scenarios**:

1. **Given** an enterprise client with multiple past and current orders, **When** the client views their order history, **Then** the system displays each order's identifier, line items, Net Total, current lifecycle status, and relevant dates.
2. **Given** an enterprise client, **When** they attempt to view order history, **Then** only orders belonging to that client are shown, regardless of how many other clients' orders exist in the system.
3. **Given** an order that has progressed through several lifecycle states, **When** the client views that order's detail, **Then** the current status is accurate and reflects the most recent state transition.
4. **Given** a client with one Shipped order and one Delivered order, **When** the client views the dashboard, **Then** the Shipped order appears under Active Orders only and the Delivered order appears under Order History only, with neither listed twice.

---

### User Story 3 - Cancel an Order Within the Cancellation Window (Priority: P2)

An enterprise client cancels one of their own orders while it is still within the Cancellation Window (i.e., in Intake or Processing, and not already cancelled). Once an order has shipped it is no longer cancellable, because the hardware has left CompuDelivery's control.

**Why this priority**: Cancellation is explicitly requested and directly affects committed hardware and fulfillment resources, making it a high-value client-facing control alongside order history.

**Independent Test**: Can be fully tested by submitting an order, cancelling it while in Intake or Processing, and verifying its status becomes Cancelled and it can no longer be advanced through the lifecycle; and by confirming a Shipped order cannot be cancelled.

**Acceptance Scenarios**:

1. **Given** an enterprise client's order that is in Intake or Processing, **When** the client requests cancellation, **Then** the system marks the order as Cancelled and records the time of cancellation.
2. **Given** an order that has reached Shipped or Final Delivery, **When** the client attempts to cancel it, **Then** the system rejects the cancellation and explains that the order has already shipped and can no longer be cancelled.
3. **Given** an order that has reached Shipped or Final Delivery, **When** the client views it in the portal, **Then** the cancel control is displayed in a disabled state with an inline explanation that the order has shipped and can no longer be cancelled.
4. **Given** an order that has already been cancelled, **When** the client attempts to cancel it again, **Then** the system rejects the duplicate cancellation request.
5. **Given** an order belonging to a different client, **When** a client attempts to cancel it, **Then** the system rejects the request.

---

### User Story 4 - Progress an Order Through Fulfillment Stages (Priority: P3)

An internal operator, identified via a caller-supplied operator identifier, advances a bulk order through its defined lifecycle stages (e.g., from Intake to Processing to Shipped to Final Delivery) as fulfillment work is actually completed, so that the order's status always reflects real-world progress.

**Why this priority**: Order tracking has no value unless something drives the lifecycle forward; this capability is what makes Order History (User Story 2) meaningful over time. It is lower priority than the client-facing stories because it is an enabling/back-office capability rather than a directly requested client interaction.

**Independent Test**: Can be fully tested by selecting an operator identity in the demo identity switcher, advancing a single order through each defined lifecycle stage in order via the order card's advance control, and verifying the status and transition history update correctly after each step, and that Final Delivery is treated as terminal.

**Acceptance Scenarios**:

1. **Given** an order in Intake status, **When** an operator advances it to the next defined lifecycle stage, **Then** the system updates the order's status and records the transition with a timestamp.
2. **Given** an order at Final Delivery, **When** any attempt is made to change its status further, **Then** the system rejects the change because Final Delivery is a terminal state.
3. **Given** an order that has been Cancelled, **When** any attempt is made to advance it through fulfillment, **Then** the system rejects the change because Cancelled is a terminal state.
4. **Given** a sequence of lifecycle stages, **When** an operator attempts to skip a stage or move an order backward to a prior stage, **Then** the system rejects the out-of-order transition.
5. **Given** an order in Processing for which hardware is not immediately available, **When** fulfillment waits for stock, **Then** the order remains in Processing with no on-hold or backordered status, and **When** any attempt is made to place the order in a status outside the four defined stages, **Then** the system rejects it.
6. **Given** an operator identity selected in the demo identity switcher, **When** the operator views a non-terminal order, **Then** the order card presents an advance control naming the next stage in place of the client's cancel control, and **When** the order is at a terminal state (Final Delivery or Cancelled), **Then** no advance control is offered.
7. **Given** a client identity selected in the demo identity switcher, **When** the client views their own order, **Then** no advance control is offered, because lifecycle advancement is operator-only (see FR-012).

---

### Edge Cases

- What happens when an enterprise client submits a bulk order with a line item quantity of zero, a negative quantity, or an unrecognized hardware SKU?
- A client's contract discount terms change (e.g., renegotiated) while an order is still in a non-terminal lifecycle state: if the order has advanced beyond Processing, its Net Total was already locked and is not recalculated; if the order is still in Intake or Processing, its Net Total is only recalculated the next time the client explicitly edits its Line Items, never automatically (see FR-017, FR-025).
- A cancellation request and an operator's Processing → Shipped advancement arrive for the same order at nearly the same moment: the request that commits first to persistent storage is applied; the other is rejected with a conflict message indicating the order's state has changed (see FR-016). This is the only race that can close the Cancellation Window mid-request.
- What happens when an enterprise client submits an order with no line items, or an order that would require a negative or zero Net Total?
- A client attempts to view or act on an order ID that does not exist or does not belong to them: the system returns the same generic "not found" response in both cases, without revealing whether the order exists under another client (see FR-018).

### User Interface Overview

The enterprise-client-facing portal is composed of two linked pages, per the reviewed reference design (see Clarifications, Sessions 2026-08-13 and 2026-08-17):

- **Header** (present on both pages): Portal title, a demo identity switcher for selecting the active client or operator identity, and a badge showing the client name and contract reference.
- **Dashboard page**: Active Orders & Lifecycle Tracking (one card per Active order, each showing a 4-step lifecycle indicator and a cancel control — enabled in Intake and Processing, disabled with an inline explanation once Shipped), the Order History Log (completed orders only — Final Delivery or Cancelled — with a status indicator per entry, never repeating an order shown in Active Orders), and an action to start a new order that navigates to the order detail page.
- **Operator mode** (same two pages, no separate operator page): when the identity switcher's active identity is an operator rather than a client, each non-terminal order card presents an "Advance to next stage" control, naming the next lifecycle stage, in place of the client-only cancel control; the order creation and line-item editing controls are not offered to operators. In operator mode the dashboard lists Active Orders and Order History across all enterprise clients, each labelled with its owning client.
- **Order detail / create page**: The Bulk Order Catalog (fixed Hardware Catalog Items with quantity entry) for a new or in-progress order, that order's own 4-step lifecycle indicator, and a pricing summary panel — Gross Subtotal, Contract Discount, Freight & Logistics (informational "Waived" only), and Final Net Total, updating live as catalog quantities change — with the order submission control.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST allow an enterprise client, identified via a caller-supplied client identifier (trusted as-is; not verified by login or credentials), to submit a bulk hardware order consisting of one or more line items, each specifying a hardware SKU and quantity.
- **FR-002**: System MUST calculate a Gross Total for each submitted order as the sum of its line items' list prices and quantities.
- **FR-003**: System MUST calculate the Net Total for each order by applying the submitting client's current, pre-negotiated contract discount terms to the Gross Total, and MUST NOT apply any manually overridden, estimated, or generic discount in place of the client's actual contract terms.
- **FR-004**: System MUST block an order from being finalized, and MUST clearly communicate the reason, when the submitting client's contract discount terms are missing, ambiguous, or expired at the time of submission.
- **FR-005**: System MUST assign every accepted order a single, well-defined lifecycle status drawn from an ordered set of exactly four stages (Intake, Processing, Shipped, Final Delivery), with Cancellation as the only allowed branch out of that progression, available only while the order is in Intake or Processing (the Cancellation Window). System MUST NOT provide any on-hold, backordered, or suspended status; an order awaiting hardware availability remains in Processing.
- **FR-006**: System MUST prevent an order from skipping a defined lifecycle stage, moving backward to a prior stage, or changing status after it has reached a terminal state (Final Delivery or Cancelled). There are no exceptions or permitted reversals.
- **FR-007**: System MUST record, for every lifecycle state transition and every Net Total calculation, the point in time it occurred and the actor or event that triggered it, and MUST preserve this history rather than overwriting it.
- **FR-008**: System MUST allow an enterprise client, identified via a caller-supplied client identifier (trusted as-is; not verified by login or credentials), to view a history of their own bulk orders, including each order's line items, Gross Total, Net Total, current lifecycle status, and relevant dates.
- **FR-009**: System MUST restrict an enterprise client's order history and order detail views to that client's own orders only, and MUST NOT expose another client's orders, pricing, or contract discount terms to any client identity. This restriction applies to client identities only; an operator identity (FR-012) is internal staff and sits outside the per-client access boundary (see FR-027).
- **FR-010**: System MUST allow an enterprise client, identified via a caller-supplied client identifier (trusted as-is; not verified by login or credentials), to cancel one of their own orders only while that order is within the Cancellation Window — that is, in Intake or Processing and not already cancelled. The Cancellation Window closes permanently on transition to Shipped and MUST NOT reopen.
- **FR-011**: System MUST reject a cancellation request for an order that has reached Shipped or Final Delivery, or that has already been cancelled, and MUST communicate the specific reason for rejection explicitly rather than treating it as a silent no-op. For an order that does not belong to the requesting client (see FR-018), the system MUST NOT reveal that reason and MUST instead return a generic not-found response.
- **FR-012**: System MUST allow only internal operators, identified via a caller-supplied operator identifier (trusted as-is; not verified by login or credentials, and distinct from enterprise clients), to advance an order from one lifecycle stage to the next.
- **FR-013**: System MUST treat Bulk Order lifecycle status, cancellation, and delivery confirmation as properties of the order as a whole, not of individual line items.
- **FR-014**: System MUST reject a submitted order containing a line item with a zero or negative quantity, or a hardware SKU the system does not recognize.
- **FR-015**: System MUST reject a submitted order that contains zero line items.
- **FR-016**: System MUST resolve concurrent conflicting requests on the same order (e.g., a cancellation request and an operator's Processing → Shipped advancement) using first-committed-wins semantics: whichever request commits to persistent storage first is applied, and the system MUST reject the other with a clear message indicating the order's state has changed.
- **FR-017**: System MUST lock an order's Net Total once the order advances beyond Processing (i.e., upon entering Shipped) and MUST NOT recalculate it after that point due to subsequent changes to the client's contract discount terms; for an order still in Intake or Processing, Net Total is only recalculated as a result of an explicit line item edit (see FR-025), never automatically due to a contract term change alone.
- **FR-018**: System MUST return an identical generic "not found" response, for both view and cancellation requests, whether the requested order ID does not exist or belongs to a different client, so that a client cannot distinguish nonexistence from another client's ownership.
- **FR-019**: System MUST provide a demo identity switcher that lets the user select the active client or operator identifier used for subsequent requests, in lieu of a login flow.
- **FR-020**: System MUST display the fixed Hardware Catalog Items (SKU, name, list price) and MUST let the client set a quantity per item to build Line Items for a new Bulk Order, recomputing each Line Item's subtotal and the pricing summary immediately as quantities change.
- **FR-021**: System MUST display the pricing summary, in order, as Gross Subtotal, Contract Discount, Freight & Logistics (always displayed as "Waived," informational only, never affecting Net Total), and Final Net Total.
- **FR-022**: System MUST display, for each of the client's Active Orders (those in Intake, Processing, or Shipped), a 4-step lifecycle indicator (Intake, Processing, Shipped, Final Delivery) reflecting completed, current, and upcoming steps, alongside a cancel control that requires explicit confirmation before submitting the cancellation. The control MUST be enabled only while the order is within the Cancellation Window (Intake or Processing); for a Shipped order it MUST remain visible but disabled, accompanied by an inline explanation that the order has shipped and can no longer be cancelled.
- **FR-023**: System MUST populate the Order History Log with completed orders only — those at Final Delivery or Cancelled — excluding any order still Active (Intake, Processing, or Shipped) so that no order appears in both Active Orders and Order History, and MUST display each entry with a visually distinct status indicator matching its lifecycle status (Final Delivery or Cancelled).
- **FR-024**: System MUST block submission of a new Bulk Order at the UI layer when total quantity across all catalog line items is zero, and MUST present a clear message instead of submitting an empty order (reinforces FR-015).
- **FR-025**: System MUST allow an enterprise client to edit the Line Items (add, remove, or change quantity) of their own Bulk Order only while that order is in Intake or Processing status, and MUST recalculate the Gross Total and Net Total (per FR-002 and FR-003, using currently effective contract discount terms) on each such edit, recording the recalculation per FR-007. Once an order advances beyond Processing, its Line Items and totals become read-only (see FR-017).
- **FR-026**: System MUST present lifecycle advancement in the UI on the existing dashboard and order detail pages rather than on a separate operator page: when the active identity selected via the demo identity switcher (FR-019) is an operator, each non-terminal order card MUST offer an advance control naming the next lifecycle stage, shown in place of the client-only cancel control (FR-022), and MUST offer no advance control for orders at a terminal state (Final Delivery or Cancelled). When the active identity is a client, no advance control MUST be offered (see FR-012).
- **FR-027**: System MUST, when the active identity selected via the demo identity switcher is an operator, list Active Orders and Order History across all enterprise clients rather than scoping them to a single client, and MUST label each listed order with its owning client so the operator can tell whose order they are advancing.

### Key Entities

- **Enterprise Client**: A business account, identified via a caller-supplied client identifier (trusted as-is, not verified by login or credentials), that submits bulk orders under its own pre-negotiated contract; owns its Orders and Order History and is the only party permitted to view or cancel them.
- **Contract Discount Terms**: The client-specific, pre-negotiated discount terms currently in effect for an Enterprise Client, used exclusively to compute Net Total; has a validity/expiration status.
- **Bulk Order**: A single order submission from one Enterprise Client containing one or more Line Items, an overall Gross Total, Net Total, and a single lifecycle status. Line Items and totals remain editable by the owning client while the order is in Intake or Processing status, and become locked once the order advances to Shipped (see FR-017, FR-025).
- **Line Item**: An individual hardware SKU and quantity within a Bulk Order, contributing to the order's Gross Total. The same SKU MAY appear across multiple Line Items within one order; each is kept distinct and is not auto-merged.
- **Hardware Catalog Item**: A fixed, system-defined reference entry (SKU, name, list/MSRP price) that a Line Item's SKU must match; the authoritative source of valid SKUs and list prices used in Gross Total. Catalog contents are non-editable within this feature's scope.
- **Lifecycle Transition Record**: A timestamped record of an order moving from one lifecycle status to another, including the triggering actor or event; preserved as history rather than overwritten.
- **Cancellation Record**: A timestamped record capturing that a client cancelled a specific order from within the Cancellation Window (Intake or Processing), including when the cancellation occurred.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: An enterprise client submitting a valid bulk order receives a calculated Net Total in under 5 seconds.
- **SC-002**: 100% of Net Total calculations reflect the client's currently effective contract discount terms at the moment of calculation, with zero instances of manual or default pricing overrides.
- **SC-003**: An enterprise client can locate the current status of any of their own orders, and no other client's orders, in under 3 clicks/steps from entering their client identifier.
- **SC-004**: 100% of cancellation attempts on orders outside the Cancellation Window (already Shipped, already delivered, or already cancelled) are rejected with an explicit reason, with zero successful erroneous cancellations.
- **SC-005**: 100% of order lifecycle transitions are recorded with a reconstructable timestamp and triggering actor, enabling full after-the-fact audit of any order's history.
- **SC-006**: Zero instances of one enterprise client identity viewing, cancelling, or otherwise accessing another client's order, pricing, or contract discount data. (Operator identities are internal and intentionally outside this boundary per FR-027; they are excluded from this criterion.)
- **SC-007**: The system supports at least 500 bulk order submissions per day across all enterprise clients without degradation in Net Total calculation accuracy or response time.

## Assumptions

- The defined lifecycle stages are exactly four — Intake, Processing, Shipped, and Final Delivery — with Cancellation as the only branch out of that progression. No on-hold or backordered stage exists; hardware availability and stock replenishment are inventory concerns outside this feature's domain (see Clarifications, Session 2026-09-17).
- Lifecycle progression (advancing an order from one stage to the next) is performed by internal operations staff (identified via a caller-supplied operator identifier) rather than being fully automated or client-triggered, consistent with typical enterprise fulfillment workflows.
- Each Enterprise Client is represented as a single account identifier that may be used by multiple individual users; all such users share the same order visibility scoped to that client's own data, since individual users are not separately identified in this demo.
- Pricing and order amounts are handled in a single currency; multi-currency support is out of scope for this feature.
- "Bulk" refers to any order containing one or more hardware line items submitted by an enterprise client under contract; no separate minimum-quantity threshold is enforced to qualify an order as "bulk."
- Split or partial deliveries are out of scope; an order reaches Final Delivery as a single terminal event for the order as a whole, consistent with treating the Bulk Order as a first-class unit.
- Enterprise clients are already onboarded with contract discount terms established through a process outside this feature's scope; this feature consumes those terms but does not define how contracts are negotiated or entered into the system.
- As a sample/demo application, authentication and authorization (login, credential verification, session management) are out of scope. Each request supplies a client or operator identifier that the system trusts without verification; per-client data scoping (FR-009, SC-006), order ownership checks (FR-010, FR-018), and operator-only restrictions (FR-012) are enforced based on that supplied identifier rather than a verified login session (see Clarifications, Session 2026-08-13).
- The client- and operator-facing portal's UI/UX (two-page layout — dashboard plus order detail/create — catalog table, pricing summary panel, lifecycle stepper, order history status indicators) follows the reviewed reference design, adapted per this session's clarifications: a single Contract Discount line only (no generic volume-tier discount), a demo identity switcher in place of login, a permanent 4-step lifecycle stepper (Intake, Processing, Shipped, Final Delivery), a fixed non-editable reference Hardware Catalog, and an informational-only "Waived" Freight & Logistics line (see Clarifications, Session 2026-08-13); and a dashboard + order detail/create page split rather than a single page (see Clarifications, Session 2026-08-17).
- Of the figma-designs reference files, only those covering the enterprise-client-facing order intake/tracking experience (the Enterprise User dashboard and detail pages) are in scope for this feature. The Invoice Staff and Warehouse Operator dashboards/detail pages are out of scope for this feature and belong to a separate, future feature (see Clarifications, Session 2026-08-17).
