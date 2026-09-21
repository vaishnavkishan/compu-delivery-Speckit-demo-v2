---

description: "Cross-artifact consistency analysis for Bulk Hardware Order Management (/speckit-analyze)"

---

# Specification Analysis Report: Bulk Hardware Order Management

**Feature**: `001-bulk-hardware-orders` | **Date**: 2026-09-21

**Analyzed artifacts**: `spec.md`, `plan.md`, `tasks.md` (cross-referenced against `data-model.md`, `quickstart.md`, and `.specify/memory/constitution.md`)

**Operating constraint**: This report is read-only output from `/speckit-analyze`. No files referenced within it were modified as part of producing it.

## Findings

| ID | Category | Severity | Location(s) | Summary | Recommendation |
|----|----------|----------|-------------|---------|----------------|
| G1 | Coverage Gap | HIGH | [spec.md:170](./spec.md#L170) (FR-017), [data-model.md:77](./data-model.md#L77), [tasks.md:173](./tasks.md#L173) (T063) | `data-model.md` specifies `net_total_locked_at` is "Set when status transitions to `SHIPPED`," and `plan.md`'s Constitution Check table cites this exact field as the Principle II enforcement mechanism — but no task in `tasks.md` assigns writing that field. T063 (`OrderLifecycleAdvancementService.advance`) only describes appending a `LifecycleTransition`; T042 (`OrderEditService.replaceLineItems`) only checks status, not the lock field. If `net_total_locked_at` is never set, FR-017's lock and Constitution Principle II's designed enforcement silently don't happen. | Add explicit language to T063 (or a new task) to set `net_total_locked_at = now()` when the transition target is `SHIPPED`, and to T042 to consult/enforce it. |
| G2 | Coverage Gap | MEDIUM | [spec.md:205](./spec.md#L205) (SC-007), [tasks.md:182-193](./tasks.md#L182-L193) (Phase 7) | SC-007 requires sustaining ≥500 bulk order submissions/day without missing SC-001's latency target or SC-002's accuracy target. No task (Phase 7 or elsewhere) performs load/throughput testing; `quickstart.md` step 10 runs only functional unit/integration/Vitest suites. | Add a load-test task (e.g., Testcontainers or a k6/Gatling script) exercising ≥500 submissions in a bounded window against `POST /api/orders`, asserting SC-001 latency and 0 pricing errors. |
| G3 | Coverage Gap | MEDIUM | [spec.md:199](./spec.md#L199) (SC-001), [tasks.md:91-96](./tasks.md#L91-L96) (T032-T036) | SC-001's under-5-seconds target applies to order submission, line-item-edit recalculation, and Order History page retrieval. Existing integration tests (T034, T035, T047) assert correctness only, not response-time bounds. | Add a timing assertion (or a lightweight perf test) to the relevant integration tests, or a dedicated task, so SC-001 is actually verified rather than assumed. |
| G4 | Coverage Gap | LOW | [spec.md:201](./spec.md#L201) (SC-003), [quickstart.md:131-141](./quickstart.md#L131-L141) | SC-003 requires reaching order-status visibility in ≤3 discrete UI actions from identity selection. No task or quickstart step counts/verifies this UI-action budget. | Add a manual UI-flow check to `quickstart.md` (or a Vitest/RTL interaction test) that walks the identity-select → status-visible path and asserts the action count. |
| U1 | Underspecification | MEDIUM | [spec.md:168](./spec.md#L168) (FR-015), [tasks.md:100](./tasks.md#L100) (T038), [tasks.md:93](./tasks.md#L93) (T034) | FR-015 requires rejecting a submitted order with zero line items. T034 tests for the 400, but T038 — the task that defines the request validators — only names the per-quantity ceiling (`@Min/@Max`) and the max-100-entries validator; no min-1-line-item validator is named. | Add an explicit `@NotEmpty`/min-size validator on `CreateOrderRequest.lineItems`/`ReplaceLineItemsRequest.lineItems` to T038's description so the empty-order rejection isn't left to implicit inference. |
| F1 | Inconsistency | LOW | [constitution.md:24-31](../../.specify/memory/constitution.md#L24-L31) (Principle I), [spec.md:158](./spec.md#L158) (FR-005) | Constitution Principle I's illustrative lifecycle is "intake → processing → final delivery." Spec/plan/data-model/tasks now normatively define a 5-stage lifecycle (Intake, Processing, Backordered, Shipped, Final Delivery) added across later clarification sessions. This is an expansion, not a violation of Principle I's actual MUST (no skip/no backward re-entry), so it's not CRITICAL — but the constitution's own example text has drifted from the artifacts it governs. | Optionally amend the constitution's Principle I example (PATCH-level wording clarification) to list the full 5-stage lifecycle, so the founding document and derived artifacts stay literally in sync. |

## Coverage Summary Table

FR-001–FR-030; all have ≥1 associated task (see G1/U1 for two with an incomplete implementation-task description rather than zero coverage):

| Requirement Key | Has Task? | Task IDs | Notes |
|-----------------|-----------|----------|-------|
| FR-001 Submit order | Yes | T039, T041 | |
| FR-002 Gross Total | Yes | T037, T039 | |
| FR-003 Net Total + rounding | Yes | T032, T037 | |
| FR-004 Block invalid terms | Yes | T033, T034, T039 | |
| FR-005 Lifecycle stages | Yes | T012, T023 | |
| FR-006 No skip/backward/terminal | Yes | T023, T062 | |
| FR-007 Record + retain history | Yes | T018, T019 | |
| FR-008 Order History paging | Yes | T047, T050, T055 | |
| FR-009 Client scoping | Yes | T016, T047, T051 | |
| FR-010 Cancel Active order | Yes | T057, T059 | |
| FR-011 Reject invalid cancel | Yes | T057, T059 | |
| FR-012 Operator-only advance | Yes | T021, T022, T063 | |
| FR-013 Order-level lifecycle | Yes (structural) | data-model.md design | No dedicated task; inherent to schema (no line-item status field) |
| FR-014 Reject bad line items | Yes | T034, T038 | |
| FR-015 Reject empty order | Partial | T034 (test only) | See U1 |
| FR-016 First-committed-wins | Yes | T024, T058, T059, T063 | |
| FR-017 Net Total lock at Shipped | Partial | T042 (checks status) | See G1 — lock-setting step unassigned |
| FR-018 Generic 404 | Yes | T048, T051, T057 | |
| FR-019 Identity switcher | Yes | T026, T031 | |
| FR-020 Catalog + quantity entry | Yes | T044 | |
| FR-021 Pricing summary order | Yes | T045 | |
| FR-022 5-step indicator + cancel confirm | Yes | T053, T061 | |
| FR-023 History status indicator | Yes | T055 | |
| FR-024 Block zero-total UI submit | Yes | T046 | |
| FR-025 Edit line items | Yes | T035, T042, T043, T046 | |
| FR-026 Operator control visibility | Yes | T065, T066 | |
| FR-027 Seeded identities | Yes | T010 | |
| FR-028 Empty states | Yes | T054, T055 | |
| FR-029 Loading/error states | Yes | T054, T055 | |
| FR-030 Max 100 line items | Yes | T034, T038 | |

## Constitution Alignment Issues

None at CRITICAL/MUST-violation level. F1 above is a documentation-drift note, not a violation. (Aside, out of scope for this feature's spec/plan/tasks: `constitution.md` line 14 still carries an unresolved `TODO(RATIFICATION_DATE)` placeholder.)

## Unmapped Tasks

T001–T007 (Setup), T008–T031 (Foundational), T067–T074 (Polish) carry no `[Story]` label by design (per `tasks.md`'s own Notes section) and aren't defects.

## Metrics

- Total Functional Requirements: 30 (FR-001–FR-030)
- Total Success Criteria (buildable subset reviewed): 6 (SC-001, SC-003, SC-004, SC-005, SC-006, SC-007)
- Total Tasks: 74 (T001–T074)
- FR Coverage: 30/30 have ≥1 task (100%); 2 flagged as incompletely specified (G1, U1)
- SC Coverage: 3/6 buildable criteria lack verification tasks (SC-001, SC-003, SC-007)
- Ambiguity Count: 0 (spec is heavily clarified — all vague terms resolved via clarification sessions)
- Duplication Count: 0
- Critical Issues Count: 0

## Next Actions

No CRITICAL issues — implementation may proceed toward `/speckit-implement`, but G1 (Net Total lock) is worth fixing first since it's a functional-correctness risk tied to a Constitution gate, not just a documentation nit.

- Recommended: manually edit `tasks.md` T063 to explicitly set `net_total_locked_at` on the `SHIPPED` transition (G1), and T038 to name the min-1-line-item validator (U1).
- Recommended: add a load-test task for SC-007 and timing assertions for SC-001 to `tasks.md` Phase 7, and a UI-action-count check to `quickstart.md` for SC-003.
- Optional: amend `constitution.md` Principle I's lifecycle example (PATCH version bump) to reflect the 5-stage lifecycle — run `/speckit-constitution` if this is wanted as a formal amendment.
