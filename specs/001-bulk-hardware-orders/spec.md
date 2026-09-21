# Feature Specification: Bulk Hardware Order Management

**Feature Branch**: `002-speckit-specify`

**Created**: 2026-08-13

**Status**: Draft

**Input**: User description: "We need a system that accepts bulk hardware orders from enterprise clients, calculates their final net totals using pre-negotiated contract discounts, and tracks each order through its lifecycle from intake to final delivery. Clients should be able to view their order history or cancel active requests."

## Clarifications

### Session 2026-08-13

- Q: When a cancellation request and an operator's Final Delivery advancement hit the same order at nearly the same moment, which one should win? → A: First to commit wins — whichever request reaches persistent storage first is applied; the other is rejected with a clear conflict message.
- Q: Should the order lifecycle use exactly the four assumed stages (Intake → Processing → Shipped → Final Delivery), or a different set? → A: Add a Backordered/On-Hold stage, entered from Processing when hardware isn't immediately available and exited back to Processing once stock becomes available.
- Q: If a client's contract discount terms change after an order's Net Total was already calculated but before Final Delivery, should the order reprice? → A: No — Net Total is locked at intake and is not recalculated due to later contract renegotiation.
- Q: If a client submits a bulk order listing the same hardware SKU twice as separate line items, what should the system do? → A: Allow as separate line items; each submitted line item is kept distinct and Gross Total sums all line items as submitted, with no auto-merging.
- Q: When a client tries to view or cancel an order ID that either doesn't exist or belongs to a different client, should the response be the same in both cases? → A: Yes — a single generic "not found" response for both, so a client cannot distinguish nonexistence from another client's ownership (see FR-018).
- Q: Since this demo won't implement authentication or authorization, how should the system know which enterprise client or operator is behind a given request? → A: Requests include a caller-supplied client/operator identifier that the system trusts without verifying credentials; per-client data scoping and operator-only restrictions still apply based on that identifier, but no login or credential verification is performed.
- Q: The reference UI design shows a second "Bulk Volume Tier" discount line (an extra 5% off at 50+ units) alongside the Contract Discount line — should this generic volume-based discount be part of the implemented pricing model? → A: No — drop the volume tier; the implemented UI and pricing model use a single Contract Discount line only, consistent with FR-003's rule that Net Total derives exclusively from the client's contract terms.
- Q: The reference UI design has no login screen or identifier input — it shows a static, already-resolved client badge. Since the system trusts a caller-supplied identifier with no login, how should the UI actually capture that identifier? → A: A demo client/operator switcher (dropdown/selector) lets the user pick among a small set of demo identities to view the portal scoped to that identifier, for testing and demonstration purposes.
- Q: The reference UI's lifecycle stepper shows only 4 steps (Intake → Processing → Shipped → Delivered), but the spec's lifecycle has a 5th stage, Backordered. How should Backordered appear in that stepper? → A: Expand the stepper to a permanent 5-column layout (Intake, Processing, Backordered, Shipped, Delivered) shown for every order, whether or not that order ever goes on hold.
- Q: The reference UI's catalog table lists a fixed set of hardware items (name, SKU, MSRP); the spec doesn't define where valid SKUs, names, and list prices come from. What is the catalog's scope? → A: A fixed, system-defined, non-editable reference catalog (matching the Figma's items) is the source of valid SKUs and list prices for Gross Total; catalog management (add/edit/remove items) is out of scope for this feature.
- Q: The reference UI shows a "Freight & Logistics" summary line that always reads "Waived," with no cost calculation. Should this remain purely informational, or does it imply real freight cost logic? → A: Freight/logistics stays informational only — the UI always shows "Waived" with no cost calculation, and freight is never added to Net Total, consistent with the constitution's domain boundary excluding carrier/logistics operations.

### Session 2026-08-17

- Q: The figma-designs folder now includes an Invoice Staff billing dashboard (tax invoices, sales-tax calculation, payment/remittance tracking) and a Warehouse Operator dashboard (per-unit serial asset tagging, carrier/waybill capture). Are these part of this feature's scope? → A: Out of scope for this feature (001-bulk-hardware-orders) — consistent with the constitution's domain boundary excluding payment settlement mechanics and warehouse/carrier operations. The Invoice Staff and Warehouse Operator portals (invoice-staff-dashboard.html, invoice-staff-detail-page.html, warehouse-ops-dashboard.html, warehouse-ops-detail-page.html) will be addressed by a separate, future feature and are not reference designs for this spec.
- Q: enterprise-user-dashboard.html and enterprise-user-detail-page.html show only a 4-step lifecycle stepper (Intake, Processing, Shipped, Delivered) with no Backordered step, contradicting FR-022's already-decided permanent 5-column stepper. Does this change the FR-022 decision? → A: No — FR-022's 5-column stepper (Intake, Processing, Backordered, Shipped, Delivered) remains the decision. The 4-step stepper in these two new reference files is a design gap to be corrected during implementation, not a reversal.
- Q: enterprise-user-dashboard.html and enterprise-user-detail-page.html split the original single-page portal into a dashboard page (active orders + history) and a separate order detail/create page (catalog + pricing panel). Should the spec adopt this two-page structure? → A: Yes — the UI Interface Overview now describes a dashboard page and a linked order detail/create page in place of the original single page.
- Q: enterprise-user-detail-page.html lets a client edit an already-submitted order's line items while it is in Intake or Processing status ("View / Edit Order"), which the spec previously did not describe and which appeared to conflict with FR-017's "locked at intake" wording. Should this editing capability be part of the spec? → A: Yes — clients MAY edit Line Items while an order is in Intake or Processing (new FR-025), with Gross Total and Net Total recalculated on each edit using currently effective contract terms; FR-017 is revised so the Net Total lock now takes effect once the order advances beyond Processing (upon entering Shipped) rather than at intake.

### Session 2026-09-21

- Q: If applying a client's contract discount would produce a Net Total of zero or a negative amount (e.g., a discount at or above 100%), what should the system do? → A: Treat the contract discount terms as invalid/ambiguous whenever the discount percentage is not strictly between 0% and 100% (exclusive of both bounds), and block the order from finalization via the existing FR-004 path rather than allowing a zero or negative Net Total.
- Q: How should monetary amounts (Gross Total, discount amount, Net Total) be rounded, and to what precision? → A: Carry Gross Total and the discount calculation at full precision, then round only the final Net Total to 2 decimal places using round-half-up.
- Q: Order History (FR-008) must show each order's "relevant dates" — which specific timestamps should that concretely include? → A: The timestamp of every lifecycle transition the order has undergone (Intake, Processing, Backordered, Shipped, Final Delivery, or Cancelled, as applicable), shown inline per order in the Order History Log.
- Q: When a request is rejected under first-committed-wins (FR-016) because another request already changed the order, what should the rejection response include? → A: The conflict message together with the order's now-current lifecycle status, so the caller can see what actually happened without a separate lookup.
- Q: What level of accessibility support should the client/operator portal (identity switcher, catalog quantity controls, lifecycle indicator, status indicators, cancel confirmation) be required to meet? → A: Best-effort general good practice (semantic HTML, labeled controls); no named accessibility standard (e.g., WCAG) is required for this feature.

### Session 2026-09-21 (continued)

- Q: When a bulk order submission is blocked because the client's contract discount terms are missing, expired, or ambiguous (FR-004), does the system create any persisted order record for it, or is the submission rejected with no order record at all? → A: No order record is created — the submission is rejected synchronously before any Bulk Order or Line Item rows are persisted, so it never appears in Order History or any lifecycle state.
- Q: When a client edits Line Items on an order in Intake or Processing (FR-025), but the client's contract discount terms are now missing, expired, or ambiguous at that moment, what should happen to the edit? → A: Reject the edit under the same FR-004 rule; the order's Line Items and its last valid Gross/Net Total remain unchanged.
- Q: When a client edits an order (FR-025) and adds a SKU that already exists as a separate Line Item on that order, should the system add it as a new distinct Line Item, or increase the quantity on the existing one? → A: Always add as a new distinct Line Item — the same no-auto-merge rule applies at edit time as at initial submission; a client wanting more of an existing line edits that line's quantity directly.
- Q: For how long must lifecycle transition and Net Total calculation history be retained, and to what timestamp precision must it be recorded? → A: Retained indefinitely (no deletion/archival policy in scope for this feature), with timestamps recorded to at least 1-second precision.
- Q: Beyond SC-001's 5-second target for initial order submission, should there be an explicit performance target for Line Item edit recalculation (FR-025) and Order History retrieval (FR-008)? → A: Yes — the same under-5-seconds target applies to edit recalculation and Order History retrieval.

### Session 2026-09-21 (continued 2)

- Q: Where in the product does an internal operator actually advance an order from one lifecycle stage to the next? → A: When an operator identity is selected in the demo identity switcher, the existing client pages reveal an operator-only "Advance to next stage" control on each Active Order (plus a Backordered toggle from Processing); there is no separate operator page.
- Q: What set of demo identities and contract discount states must the system ship with, given there is no login and no contract-management screen? → A: A fixed seeded dataset covering every relevant state — at least two clients with valid but differing contract discounts, one with missing terms, one with expired terms, one with ambiguous terms, plus at least one operator identity.
- Q: What should the portal show a client who has no orders at all, and what should it show while order data is still loading or fails to load? → A: Specify all three — a distinct empty state per region ("No active orders" / "No past orders", with a call to action to start an order), a loading indicator while data is being fetched, and a plain error message with a retry action when retrieval fails.
- Q: How should the Order History Log behave once a client has accumulated hundreds or thousands of orders — is there an ordering rule and a cap on how many are shown at once? → A: Order newest-first by order submission time and return/render it in bounded pages of 25 entries, with a control to reach older entries, so retrieval stays within SC-001's under-5-seconds target regardless of history length.
- Q: Is there an upper limit on a line item's quantity, or on the number of line items in a single bulk order? → A: Yes to both — reject any line item whose quantity exceeds 10,000 units and any order containing more than 100 line items, each with a clear message, handled the same way as the existing FR-014 rejections.

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

1. **Given** an enterprise client with multiple past and current orders, **When** the client views their order history, **Then** the system displays each order's identifier, line items, Net Total, current lifecycle status, and the timestamp of every lifecycle transition the order has undergone.
2. **Given** an enterprise client, **When** they attempt to view order history, **Then** only orders belonging to that client are shown, regardless of how many other clients' orders exist in the system.
3. **Given** an order that has progressed through several lifecycle states, **When** the client views that order's detail, **Then** the current status is accurate and reflects the most recent state transition.

---

### User Story 3 - Cancel an Active Order (Priority: P2)

An enterprise client cancels one of their own orders while it is still active (i.e., it has not yet reached Final Delivery and has not already been cancelled).

**Why this priority**: Cancellation is explicitly requested and directly affects committed hardware and fulfillment resources, making it a high-value client-facing control alongside order history.

**Independent Test**: Can be fully tested by submitting an order, cancelling it before delivery, and verifying its status becomes Cancelled and it can no longer be advanced through the lifecycle.

**Acceptance Scenarios**:

1. **Given** an enterprise client's order that is in an active (pre-delivery) lifecycle state, **When** the client requests cancellation, **Then** the system marks the order as Cancelled and records the time of cancellation.
2. **Given** an order that has already reached Final Delivery, **When** the client attempts to cancel it, **Then** the system rejects the cancellation and explains that delivered orders cannot be cancelled.
3. **Given** an order that has already been cancelled, **When** the client attempts to cancel it again, **Then** the system rejects the duplicate cancellation request.
4. **Given** an order belonging to a different client, **When** a client attempts to cancel it, **Then** the system rejects the request.

---

### User Story 4 - Progress an Order Through Fulfillment Stages (Priority: P3)

An internal operator, identified via a caller-supplied operator identifier, advances a bulk order through its defined lifecycle stages (e.g., from Intake to Processing to Shipped to Final Delivery) as fulfillment work is actually completed, so that the order's status always reflects real-world progress.

**Why this priority**: Order tracking has no value unless something drives the lifecycle forward; this capability is what makes Order History (User Story 2) meaningful over time. It is lower priority than the client-facing stories because it is an enabling/back-office capability rather than a directly requested client interaction.

**Independent Test**: Can be fully tested by advancing a single order through each defined lifecycle stage in order and verifying the status and transition history update correctly after each step, and that Final Delivery is treated as terminal.

**Acceptance Scenarios**:

1. **Given** an order in Intake status, **When** an operator advances it to the next defined lifecycle stage, **Then** the system updates the order's status and records the transition with a timestamp.
2. **Given** an order at Final Delivery, **When** any attempt is made to change its status further, **Then** the system rejects the change because Final Delivery is a terminal state.
3. **Given** an order that has been Cancelled, **When** any attempt is made to advance it through fulfillment, **Then** the system rejects the change because Cancelled is a terminal state.
4. **Given** a sequence of lifecycle stages, **When** an operator attempts to skip a stage or move an order backward to a prior stage, **Then** the system rejects the out-of-order transition.
5. **Given** an order in Processing for which hardware is not immediately available, **When** an operator marks it Backordered, **Then** the system updates its status accordingly, and **When** stock later becomes available and the operator moves it back to Processing, **Then** the system accepts that specific transition without treating it as an out-of-order move.

---

### Edge Cases

- What happens when an enterprise client submits a bulk order with a line item quantity of zero, a negative quantity, or an unrecognized hardware SKU?
- A client submits or edits an order with a line item quantity above 10,000 units, or with more than 100 line items: the system rejects it and states which limit was exceeded (see FR-014, FR-030, FR-025).
- A client's contract discount terms change (e.g., renegotiated) while an order is still active but not yet at Final Delivery: if the order has advanced beyond Processing, its Net Total was already locked and is not recalculated; if the order is still in Intake or Processing, its Net Total is only recalculated the next time the client explicitly edits its Line Items, never automatically (see FR-017, FR-025). If the client's contract terms have become missing, expired, or ambiguous by the time they attempt that edit, the edit itself is rejected (same rule as FR-004) and the order's existing Line Items and last valid totals are left unchanged (see FR-025).
- A cancellation request and an operator's Final Delivery advancement arrive for the same order at nearly the same moment: the request that commits first to persistent storage is applied; the other is rejected with a conflict message indicating the order's state has changed, along with the order's now-current lifecycle status (see FR-016).
- What happens when an enterprise client submits an order with no line items? The system rejects it (see FR-015).
- A client's contract discount percentage is not strictly between 0% and 100% (e.g., it is 0%, negative, exactly 100%, or greater), which would otherwise produce a Net Total that is zero or negative: the system treats these terms as ambiguous/invalid and blocks the order from finalization via the same path as missing or expired terms (see FR-004).
- A client with no orders at all (including every freshly seeded demo client) opens the dashboard: each region shows its own empty state rather than a blank area (see FR-028).
- A client's Order History contains more entries than one page holds: the Order History Log shows the newest 25 entries first and offers a control to reach older entries, with no entry skipped or repeated across pages (see FR-008).
- Order data cannot be retrieved (retrieval is still in flight, or it fails): the affected region shows a loading indicator, then a plain error message with a retry action on failure (see FR-029).
- A client attempts to view or act on an order ID that does not exist or does not belong to them: the system returns the same generic "not found" response in both cases, without revealing whether the order exists under another client (see FR-018).

### User Interface Overview

The enterprise-client-facing portal is composed of two linked pages, per the reviewed reference design (see Clarifications, Sessions 2026-08-13 and 2026-08-17):

- **Header** (present on both pages): Portal title, a demo identity switcher for selecting the active client or operator identity, and a badge showing the client name and contract reference.
- **Dashboard page**: Active Orders & Lifecycle Tracking (one card per Active order, each showing a 5-step lifecycle indicator and a cancel control), the Order History Log (past orders newest-first, paged 25 at a time with a control to reach older entries, each with a status indicator and full lifecycle transition timestamp history), and an action to start a new order that navigates to the order detail page.
- **Order detail / create page**: The Bulk Order Catalog (fixed Hardware Catalog Items with quantity entry) for a new or in-progress order, that order's own 5-step lifecycle indicator, and a pricing summary panel — Gross Subtotal, Contract Discount, Freight & Logistics (informational "Waived" only), and Final Net Total, updating live as catalog quantities change — with the order submission control.
- **Operator controls (same two pages, no separate operator page)**: While an operator identity is selected in the demo identity switcher, each Active Order additionally exposes operator-only lifecycle controls — an "Advance to next stage" action and, for an order in Processing, a Backordered on-hold toggle (and its return to Processing). These controls are hidden while a client identity is selected, and client-only controls (cancel, order creation, Line Item editing) are hidden while an operator identity is selected.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST allow an enterprise client, identified via a caller-supplied client identifier (trusted as-is; not verified by login or credentials), to submit a bulk hardware order consisting of one or more line items, each specifying a hardware SKU and quantity.
- **FR-002**: System MUST calculate a Gross Total for each submitted order as the sum of its line items' list prices and quantities.
- **FR-003**: System MUST calculate the Net Total for each order by applying the submitting client's current, pre-negotiated contract discount terms to the Gross Total, and MUST NOT apply any manually overridden, estimated, or generic discount in place of the client's actual contract terms. Gross Total and the discount amount MUST be carried at full precision through this calculation, with rounding applied only once, to the final Net Total, to 2 decimal places using round-half-up.
- **FR-004**: System MUST block an order from being finalized when the submitting client's contract discount terms are missing, ambiguous, or expired at the time of submission, and MUST clearly communicate which of those three conditions applies as the reason. A discount percentage that is not strictly between 0% and 100% (exclusive of both bounds) — including 0%, negative values, exactly 100%, or greater — MUST be treated as ambiguous/invalid terms under this rule, since applying it would produce a zero or negative Net Total. When a submission is blocked under this rule, the system MUST reject it synchronously without creating any Bulk Order or Line Item record — a blocked submission MUST NOT appear in the client's Order History or hold any lifecycle status.
- **FR-005**: System MUST assign every accepted order a single, well-defined lifecycle status drawn from an ordered set of stages (Intake, Processing, Shipped, Final Delivery), with Cancellation as an allowed branch at any point prior to Final Delivery, and with Backordered as an allowed on-hold branch from Processing (used when hardware isn't immediately available).
- **FR-006**: System MUST prevent an order from skipping a defined lifecycle stage, moving backward to a prior stage, or changing status after it has reached a terminal state (Final Delivery or Cancelled), except that an order in Backordered MAY move back to Processing once stock becomes available — this specific reversal is not considered a backward-stage violation.
- **FR-007**: System MUST record, for every lifecycle state transition and every Net Total calculation, the point in time it occurred (to at least 1-second precision) and the actor or event that triggered it, and MUST preserve this history indefinitely rather than overwriting or purging it.
- **FR-008**: System MUST allow an enterprise client, identified via a caller-supplied client identifier (trusted as-is; not verified by login or credentials), to view a history of their own bulk orders, including each order's line items, Gross Total, Net Total, current lifecycle status, and the timestamp of every lifecycle transition the order has undergone (Intake, Processing, Backordered, Shipped, Final Delivery, or Cancelled, as applicable), shown inline per order in the Order History Log. The history MUST be ordered newest-first by order submission time and MUST be returned and rendered in bounded pages of at most 25 entries, with a control that reaches older entries, so that retrieval meets SC-001's under-5-seconds target regardless of how long the client's history has grown. Paging MUST NOT skip or duplicate entries, and the newest-first ordering MUST be stable across pages.
- **FR-009**: System MUST restrict an enterprise client's order history and order detail views to that client's own orders only, and MUST NOT expose another client's orders, pricing, or contract discount terms.
- **FR-010**: System MUST allow an enterprise client, identified via a caller-supplied client identifier (trusted as-is; not verified by login or credentials), to cancel one of their own orders only while that order is Active (has not reached Final Delivery and has not already been cancelled). Cancellation eligibility (Active, i.e. Intake/Processing/Backordered/Shipped) is independent of, and broader than, Line Item editability (Intake/Processing only, see FR-025) — an order in Shipped or Backordered MAY still be cancelled even though its Line Items are already locked.
- **FR-011**: System MUST reject a cancellation request for an order that has already reached Final Delivery or already been cancelled, and MUST communicate the specific reason for rejection. For an order that does not belong to the requesting client (see FR-018), the system MUST NOT reveal that reason and MUST instead return a generic not-found response.
- **FR-012**: System MUST allow only internal operators, identified via a caller-supplied operator identifier (trusted as-is; not verified by login or credentials, and distinct from enterprise clients), to advance an order from one lifecycle stage to the next.
- **FR-013**: System MUST treat Bulk Order lifecycle status, cancellation, and delivery confirmation as properties of the order as a whole, not of individual line items.
- **FR-014**: System MUST reject a submitted order containing a line item with a zero or negative quantity, a line item quantity greater than 10,000 units, or a hardware SKU the system does not recognize, and MUST clearly state which condition caused the rejection.
- **FR-015**: System MUST reject a submitted order that contains zero line items.
- **FR-016**: System MUST resolve concurrent conflicting requests on the same order (e.g., a cancellation request and an operator's Final Delivery advancement) using first-committed-wins semantics: whichever request commits to persistent storage first is applied, and the system MUST reject the other with a clear message indicating the order's state has changed, together with the order's now-current lifecycle status so the caller can see the actual outcome without a separate lookup.
- **FR-017**: System MUST lock an order's Net Total once the order advances beyond Processing (i.e., upon entering Shipped) and MUST NOT recalculate it after that point due to subsequent changes to the client's contract discount terms; for an order still in Intake or Processing, Net Total is only recalculated as a result of an explicit line item edit (see FR-025), never automatically due to a contract term change alone.
- **FR-018**: System MUST return an identical generic "not found" response, for both view and cancellation requests, whether the requested order ID does not exist or belongs to a different client, so that a client cannot distinguish nonexistence from another client's ownership.
- **FR-019**: System MUST provide a demo identity switcher that lets the user select the active client or operator identifier used for subsequent requests, in lieu of a login flow.
- **FR-020**: System MUST display the fixed Hardware Catalog Items (SKU, name, list price) and MUST let the client set a quantity per item to build Line Items for a new Bulk Order, recomputing each Line Item's subtotal and the pricing summary immediately as quantities change.
- **FR-021**: System MUST display the pricing summary, in order, as Gross Subtotal, Contract Discount, Freight & Logistics (always displayed as "Waived," informational only, never affecting Net Total), and Final Net Total.
- **FR-022**: System MUST display, for each of the client's Active Orders, a 5-step lifecycle indicator (Intake, Processing, Backordered, Shipped, Final Delivery) reflecting completed, current, and upcoming steps, alongside a cancel control that requires explicit confirmation before submitting the cancellation.
- **FR-023**: System MUST display each Order History entry with a visually distinct status indicator matching the order's lifecycle status (Intake, Processing, Backordered, Shipped, Final Delivery, or Cancelled).
- **FR-024**: System MUST block submission of a new Bulk Order at the UI layer when total quantity across all catalog line items is zero, and MUST present a clear message instead of submitting an empty order (reinforces FR-015).
- **FR-025**: System MUST allow an enterprise client to edit the Line Items (add, remove, or change quantity) of their own Bulk Order only while that order is in Intake or Processing status, and MUST recalculate the Gross Total and Net Total (per FR-002 and FR-003, using currently effective contract discount terms) on each such edit, recording the recalculation per FR-007. Adding a Line Item for a SKU that already exists as a separate Line Item on the order MUST create a new, distinct Line Item rather than merging into the existing one, consistent with the no-auto-merge rule at initial submission (see Key Entities: Line Item). If the client's contract discount terms are missing, expired, or ambiguous at the moment of the edit, the system MUST reject the edit under the same rule as FR-004, leaving the order's existing Line Items and its last valid Gross Total and Net Total unchanged. Once an order advances beyond Processing, its Line Items and totals become read-only (see FR-017).
- **FR-026**: System MUST expose operator lifecycle controls only while an operator identity is selected in the demo identity switcher (FR-019), rendered on the same client-facing pages rather than on a separate operator page: an "Advance to next stage" control on each Active Order, and a Backordered on-hold toggle for an order in Processing (including its return to Processing). While a client identity is selected these controls MUST be hidden; while an operator identity is selected the client-only controls (cancel, new order creation, Line Item editing) MUST be hidden. An operator identity views the same per-client pages, scoped to the client currently selected in the identity switcher (the reference design already carries a separate client/company selector alongside the role selector), so an operator acts on that client's Active Orders rather than on a cross-client queue. All transitions triggered by these controls remain subject to FR-006, FR-012, and FR-016.
- **FR-027**: System MUST ship with a fixed, seeded set of demo identities selectable in the demo identity switcher (FR-019), covering at minimum: two enterprise clients with valid but differing contract discount terms (so differing Net Totals and per-client isolation are both observable), one client whose contract discount terms are missing, one whose terms are expired, one whose terms are ambiguous (zero or multiple matching term sets, or a discount percentage outside the strictly-0%-to-100% range), and one internal operator identity. Contract discount terms MUST NOT be creatable or editable from within this feature (see Assumptions); the seeded dataset is their only source.
- **FR-028**: System MUST present a distinct empty state in each order region when that region has nothing to show — "No active orders" in Active Orders & Lifecycle Tracking (with an action to start a new order) and "No past orders" in the Order History Log — rather than an unexplained blank region.
- **FR-029**: System MUST show a loading indicator in place of each order region while that region's data is being retrieved, and MUST replace it with a plain, non-technical error message plus a retry action if retrieval fails, rather than leaving the region blank or showing a stale view.
- **FR-030**: System MUST reject a submitted order containing more than 100 Line Items, with a clear message, handled the same way as the FR-014 rejections. The FR-014 per-line quantity ceiling and this per-order Line Item ceiling MUST also be enforced on Line Item edits (FR-025), so an edit cannot push an order past either limit.

### Key Entities

- **Enterprise Client**: A business account, identified via a caller-supplied client identifier (trusted as-is, not verified by login or credentials), that submits bulk orders under its own pre-negotiated contract; owns its Orders and Order History and is the only party permitted to view or cancel them.
- **Contract Discount Terms**: The client-specific, pre-negotiated discount terms currently in effect for an Enterprise Client, used exclusively to compute Net Total. Terms are time-bounded (an effective-from date, and an optional effective-until date for open-ended terms); "current" means the terms whose effective window includes the moment of calculation. If zero, or more than one, set of terms match that moment for a client, the terms are treated as ambiguous/invalid under FR-004.
- **Bulk Order**: A single order submission from one Enterprise Client containing one or more Line Items, an overall Gross Total, Net Total, and a single lifecycle status. Line Items and totals remain editable by the owning client while the order is in Intake or Processing status, and become locked once the order advances to Shipped (see FR-017, FR-025).
- **Line Item**: An individual hardware SKU and quantity within a Bulk Order, contributing to the order's Gross Total. The same SKU MAY appear across multiple Line Items within one order; each is kept distinct and is not auto-merged.
- **Hardware Catalog Item**: A fixed, system-defined reference entry (SKU, name, list/MSRP price) that a Line Item's SKU must match; the authoritative source of valid SKUs and list prices used in Gross Total. Catalog contents are non-editable within this feature's scope.
- **Lifecycle Transition Record**: A timestamped record of an order moving from one lifecycle status to another, including the triggering actor or event; preserved as history rather than overwritten.
- **Cancellation Record**: A timestamped record capturing that a client cancelled a specific Active Order, including when the cancellation occurred.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: An enterprise client submitting a valid bulk order receives a calculated Net Total in under 5 seconds; the same under-5-seconds target applies to a Line Item edit's recalculated Gross/Net Total (FR-025) and to retrieval of any single page of Order History (FR-008).
- **SC-002**: 100% of Net Total calculations — every calculation the system ever performs, not a sample — reflect the client's currently effective contract discount terms at the moment of calculation and the rounding policy in FR-003, with zero instances of manual or default pricing overrides.
- **SC-003**: Starting from selecting their identity in the demo identity switcher (FR-019), an enterprise client can reach a screen showing the current lifecycle status of any of their own orders — and never any other client's orders — in 3 or fewer discrete UI actions (e.g., a click, tap, or navigation), ending at the point that status is visible on screen.
- **SC-004**: 100% of cancellation attempts on orders that are not Active (already delivered or already cancelled) are rejected, with zero successful erroneous cancellations.
- **SC-005**: 100% of order lifecycle transitions are recorded with a reconstructable timestamp (at least 1-second precision) and triggering actor, retained indefinitely, enabling full after-the-fact audit of any order's history at any point in the future.
- **SC-006**: Zero instances of one enterprise client viewing, cancelling, or otherwise accessing another client's order, pricing, or contract discount data.
- **SC-007**: The system supports at least 500 bulk order submissions per day across all enterprise clients while continuing to meet SC-001's under-5-seconds response target and SC-002's 100%-accuracy requirement for every one of those submissions — "degradation" means either threshold being missed.

## Assumptions

- The defined lifecycle stages are Intake, Processing, Shipped, and Final Delivery, with Cancellation available as a branch at any point before Final Delivery and Backordered available as an on-hold branch from Processing (see Clarifications, Session 2026-08-13).
- Lifecycle progression (advancing an order from one stage to the next) is performed by internal operations staff (identified via a caller-supplied operator identifier) rather than being fully automated or client-triggered, consistent with typical enterprise fulfillment workflows.
- Each Enterprise Client is represented as a single account identifier that may be used by multiple individual users; all such users share the same order visibility scoped to that client's own data, since individual users are not separately identified in this demo.
- Pricing and order amounts are handled in a single currency; multi-currency support is out of scope for this feature.
- "Bulk" refers to any order containing one or more hardware line items submitted by an enterprise client under contract; no separate minimum-quantity threshold is enforced to qualify an order as "bulk."
- Split or partial deliveries are out of scope; an order reaches Final Delivery as a single terminal event for the order as a whole, consistent with treating the Bulk Order as a first-class unit.
- Enterprise clients are already onboarded with contract discount terms established through a process outside this feature's scope; this feature consumes those terms but does not define how contracts are negotiated or entered into the system, nor does it provide any screen for creating or editing them; within this demo those terms exist only as the fixed seeded dataset required by FR-027.
- As a sample/demo application, authentication and authorization (login, credential verification, session management) are out of scope. Each request supplies a client or operator identifier that the system trusts without verification; per-client data scoping (FR-009, SC-006), order ownership checks (FR-010, FR-018), and operator-only restrictions (FR-012) are enforced based on that supplied identifier rather than a verified login session (see Clarifications, Session 2026-08-13).
- The client- and operator-facing portal's UI/UX (two-page layout — dashboard plus order detail/create — catalog table, pricing summary panel, lifecycle stepper, order history status indicators) follows the reviewed reference design, adapted per this session's clarifications: a single Contract Discount line only (no generic volume-tier discount), a demo identity switcher in place of login, a permanent 5-step lifecycle stepper including Backordered, a fixed non-editable reference Hardware Catalog, and an informational-only "Waived" Freight & Logistics line (see Clarifications, Session 2026-08-13); and a dashboard + order detail/create page split rather than a single page (see Clarifications, Session 2026-08-17); and operator lifecycle controls surfaced on those same two pages under an operator identity, which are an addition beyond the reference design rather than something it depicts (see Clarifications, Session 2026-09-21 (continued 2)).
- Of the figma-designs reference files, only those covering the enterprise-client-facing order intake/tracking experience (the Enterprise User dashboard and detail pages) are in scope for this feature. The Invoice Staff and Warehouse Operator dashboards/detail pages are out of scope for this feature and belong to a separate, future feature (see Clarifications, Session 2026-08-17).
- The portal follows general good accessibility practice (semantic HTML, labeled controls) on a best-effort basis; conformance to a named accessibility standard (e.g., WCAG) is not a requirement for this feature (see Clarifications, Session 2026-09-21).
