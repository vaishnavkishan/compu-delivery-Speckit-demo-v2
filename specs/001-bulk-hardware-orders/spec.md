# Feature Specification: Bulk Hardware Order Management

**Feature Branch**: `002-speckit-specify`

**Created**: 2026-08-13

**Status**: Draft

**Input**: User description: "We need a system that accepts bulk hardware orders from enterprise clients, calculates their final net totals using pre-negotiated contract discounts, and tracks each order through its lifecycle from intake to final delivery. Clients should be able to view their order history or cancel active requests."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Submit Bulk Order and Receive Contract-Priced Net Total (Priority: P1)

An enterprise client submits a bulk hardware order consisting of one or more hardware line items (SKU and quantity). The system validates the order, applies the client's current pre-negotiated contract discount terms, and returns the calculated Net Total before the order is accepted into the fulfillment pipeline.

**Why this priority**: This is the core transaction the entire system exists to support. Without accurate, contract-driven pricing at intake, no other capability (tracking, history, cancellation) has anything meaningful to operate on.

**Independent Test**: Can be fully tested by submitting a bulk order for a client with known contract discount terms and verifying the returned Net Total matches Gross Total minus the contract discount, and that the order is recorded in Intake status.

**Acceptance Scenarios**:

1. **Given** an enterprise client with active, current contract discount terms, **When** the client submits a bulk order with valid hardware line items, **Then** the system calculates the Gross Total, applies the contract discount, returns the Net Total, and creates the order in Intake status.
2. **Given** an enterprise client whose contract discount terms are missing or expired, **When** the client submits a bulk order, **Then** the system blocks the order from being finalized and clearly indicates the reason instead of applying a default or estimated discount.
3. **Given** a submitted bulk order with multiple line items, **When** the Net Total is calculated, **Then** the calculation is based on the order as a whole and is traceable to the specific contract discount terms and point in time used.

---

### User Story 2 - View Order History and Status (Priority: P2)

An enterprise client views a list of their own bulk orders — past and current — including each order's lifecycle status and Net Total, without seeing any other client's orders.

**Why this priority**: Visibility into order status and history is explicitly requested and is the primary way clients build trust in the system after submitting an order.

**Independent Test**: Can be fully tested by creating orders for two different clients, then confirming that each client's order history view shows only that client's own orders with correct status and Net Total.

**Acceptance Scenarios**:

1. **Given** an enterprise client with multiple past and current orders, **When** the client views their order history, **Then** the system displays each order's identifier, line items, Net Total, current lifecycle status, and relevant dates.
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

An authorized internal operator advances a bulk order through its defined lifecycle stages (e.g., from Intake to Processing to Shipped to Final Delivery) as fulfillment work is actually completed, so that the order's status always reflects real-world progress.

**Why this priority**: Order tracking has no value unless something drives the lifecycle forward; this capability is what makes Order History (User Story 2) meaningful over time. It is lower priority than the client-facing stories because it is an enabling/back-office capability rather than a directly requested client interaction.

**Independent Test**: Can be fully tested by advancing a single order through each defined lifecycle stage in order and verifying the status and transition history update correctly after each step, and that Final Delivery is treated as terminal.

**Acceptance Scenarios**:

1. **Given** an order in Intake status, **When** an authorized operator advances it to the next defined lifecycle stage, **Then** the system updates the order's status and records the transition with a timestamp.
2. **Given** an order at Final Delivery, **When** any attempt is made to change its status further, **Then** the system rejects the change because Final Delivery is a terminal state.
3. **Given** an order that has been Cancelled, **When** any attempt is made to advance it through fulfillment, **Then** the system rejects the change because Cancelled is a terminal state.
4. **Given** a sequence of lifecycle stages, **When** an operator attempts to skip a stage or move an order backward to a prior stage, **Then** the system rejects the out-of-order transition.

---

### Edge Cases

- What happens when an enterprise client submits a bulk order with a line item quantity of zero, a negative quantity, or an unrecognized hardware SKU?
- What happens when a client's contract discount terms change (e.g., renegotiated) while an order is still active but not yet finalized in the lifecycle?
- How does the system handle a cancellation request submitted at the exact moment an operator is advancing the same order to Final Delivery?
- What happens when an enterprise client submits an order with no line items, or an order that would require a negative or zero Net Total?
- How does the system respond if a client attempts to view or act on an order ID that does not exist or does not belong to them?

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST allow an authenticated enterprise client to submit a bulk hardware order consisting of one or more line items, each specifying a hardware SKU and quantity.
- **FR-002**: System MUST calculate a Gross Total for each submitted order as the sum of its line items' list prices and quantities.
- **FR-003**: System MUST calculate the Net Total for each order by applying the submitting client's current, pre-negotiated contract discount terms to the Gross Total, and MUST NOT apply any manually overridden, estimated, or generic discount in place of the client's actual contract terms.
- **FR-004**: System MUST block an order from being finalized, and MUST clearly communicate the reason, when the submitting client's contract discount terms are missing, ambiguous, or expired at the time of submission.
- **FR-005**: System MUST assign every accepted order a single, well-defined lifecycle status drawn from an ordered set of stages (Intake, Processing, Shipped, Final Delivery), with Cancellation as an allowed branch at any point prior to Final Delivery.
- **FR-006**: System MUST prevent an order from skipping a defined lifecycle stage, moving backward to a prior stage, or changing status after it has reached a terminal state (Final Delivery or Cancelled).
- **FR-007**: System MUST record, for every lifecycle state transition and every Net Total calculation, the point in time it occurred and the actor or event that triggered it, and MUST preserve this history rather than overwriting it.
- **FR-008**: System MUST allow an authenticated enterprise client to view a history of their own bulk orders, including each order's line items, Gross Total, Net Total, current lifecycle status, and relevant dates.
- **FR-009**: System MUST restrict an enterprise client's order history and order detail views to that client's own orders only, and MUST NOT expose another client's orders, pricing, or contract discount terms.
- **FR-010**: System MUST allow an authenticated enterprise client to cancel one of their own orders only while that order is Active (has not reached Final Delivery and has not already been cancelled).
- **FR-011**: System MUST reject a cancellation request for an order that has already reached Final Delivery, already been cancelled, or does not belong to the requesting client, and MUST communicate the specific reason for rejection.
- **FR-012**: System MUST allow only authorized internal operators (not enterprise clients) to advance an order from one lifecycle stage to the next.
- **FR-013**: System MUST treat Bulk Order lifecycle status, cancellation, and delivery confirmation as properties of the order as a whole, not of individual line items.
- **FR-014**: System MUST reject a submitted order containing a line item with a zero or negative quantity, or a hardware SKU the system does not recognize.
- **FR-015**: System MUST reject a submitted order that contains zero line items.

### Key Entities

- **Enterprise Client**: A business account authorized to submit bulk orders under its own pre-negotiated contract; owns its Orders and Order History and is the only party permitted to view or cancel them.
- **Contract Discount Terms**: The client-specific, pre-negotiated discount terms currently in effect for an Enterprise Client, used exclusively to compute Net Total; has a validity/expiration status.
- **Bulk Order**: A single order submission from one Enterprise Client containing one or more Line Items, an overall Gross Total, Net Total, and a single lifecycle status.
- **Line Item**: An individual hardware SKU and quantity within a Bulk Order, contributing to the order's Gross Total.
- **Lifecycle Transition Record**: A timestamped record of an order moving from one lifecycle status to another, including the triggering actor or event; preserved as history rather than overwritten.
- **Cancellation Record**: A timestamped record capturing that a client cancelled a specific Active Order, including when the cancellation occurred.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: An enterprise client submitting a valid bulk order receives a calculated Net Total in under 5 seconds.
- **SC-002**: 100% of Net Total calculations reflect the client's currently effective contract discount terms at the moment of calculation, with zero instances of manual or default pricing overrides.
- **SC-003**: An enterprise client can locate the current status of any of their own orders, and no other client's orders, in under 3 clicks/steps from login.
- **SC-004**: 100% of cancellation attempts on orders that are not Active (already delivered or already cancelled) are rejected, with zero successful erroneous cancellations.
- **SC-005**: 100% of order lifecycle transitions are recorded with a reconstructable timestamp and triggering actor, enabling full after-the-fact audit of any order's history.
- **SC-006**: Zero instances of one enterprise client viewing, cancelling, or otherwise accessing another client's order, pricing, or contract discount data.
- **SC-007**: The system supports at least 500 bulk order submissions per day across all enterprise clients without degradation in Net Total calculation accuracy or response time.

## Assumptions

- The defined lifecycle stages are Intake, Processing, Shipped, and Final Delivery, with Cancellation available as a branch at any point before Final Delivery; this reflects standard B2B hardware fulfillment practice and is not explicitly specified in the source requirements.
- Lifecycle progression (advancing an order from one stage to the next) is performed by authorized internal operations staff rather than being fully automated or client-triggered, consistent with typical enterprise fulfillment workflows.
- Each Enterprise Client is represented as a single account that may have multiple authorized individual users; all such users share the same order visibility and permissions scoped to that client's own data.
- Pricing and order amounts are handled in a single currency; multi-currency support is out of scope for this feature.
- "Bulk" refers to any order containing one or more hardware line items submitted by an enterprise client under contract; no separate minimum-quantity threshold is enforced to qualify an order as "bulk."
- Split or partial deliveries are out of scope; an order reaches Final Delivery as a single terminal event for the order as a whole, consistent with treating the Bulk Order as a first-class unit.
- Enterprise clients are already onboarded with contract discount terms established through a process outside this feature's scope; this feature consumes those terms but does not define how contracts are negotiated or entered into the system.
