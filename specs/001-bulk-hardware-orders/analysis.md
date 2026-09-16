## Specification Analysis Report

Analysis performed against the current `spec.md`, `plan.md`, `tasks.md`, and
`.specify/memory/constitution.md`.

| ID | Category | Severity | Location(s) | Summary | Recommendation |
|---|---|---|---|---|---|
| C1 | Constitution Alignment | CRITICAL | [constitution.md](../../.specify/memory/constitution.md#L67), [spec.md](spec.md#L148), [tasks.md](tasks.md#L64) | The constitution now permits the owning client to edit line items while an order is Backordered, but FR-025, the Bulk Order entity definition, and T025 limit edits to Intake/Processing. | Align the spec and tasks with the amended constitution, or explicitly narrow the constitution rule. The constitution is authoritative for this conflict. |
| C2 | Constitution Alignment | HIGH | [plan.md](plan.md#L78), [constitution.md](../../.specify/memory/constitution.md#L25) | The plan's lifecycle gate still describes a no-re-entry lifecycle and reports PASS without documenting the constitution's explicit Backordered -> Processing exception. | Update the Constitution Check and lifecycle design references to include the permitted exception. |
| A1 | Ambiguity | HIGH | [spec.md](spec.md#L148), [plan.md](plan.md#L79) | The edit workflow does not define what happens when contract terms are missing, ambiguous, or expired during an edit. | Specify whether the edit is rejected, leaves the prior totals unchanged, or places the order into a blocked state. |
| G1 | Coverage Gap | HIGH | [spec.md](spec.md#L164), [tasks.md](tasks.md#L147) | SC-001 requires a Net Total response under five seconds, but tasks only provide generic quickstart validation. | Add an explicit timed API performance test with a defined environment and measurement method. |
| G2 | Coverage Gap | MEDIUM | [spec.md](spec.md#L170), [tasks.md](tasks.md#L147) | SC-007 requires at least 500 submissions per day without degradation, but no load or throughput test is specified. | Add a repeatable throughput validation task with an explicit submission rate and acceptance threshold. |
| G3 | Coverage Gap | MEDIUM | [spec.md](spec.md#L166), [tasks.md](tasks.md#L90) | SC-003 requires order discovery in under three steps, but no task explicitly asserts the interaction count. | Add a portal interaction test covering identity selection, dashboard access, and order detail navigation. |
| D1 | Duplication | LOW | [spec.md](spec.md#L128), [spec.md](spec.md#L148) | FR-025 repeats pricing behavior already established by FR-002 and FR-003, creating multiple maintenance points. | Keep FR-025 focused on edit permissions and reference FR-002/FR-003 for calculation rules. |

### Coverage Summary

| Requirement Key | Has Task? | Task IDs | Notes |
|---|---:|---|---|
| FR-001 to FR-004 | Yes | T020-T024 | Creation, validation, and contract pricing covered. |
| FR-005 to FR-007 | Yes | T015, T046-T048 | Lifecycle and audit history covered; plan gate needs amendment alignment. |
| FR-008 to FR-009 | Yes | T033-T039 | Client-scoped history and detail covered. |
| FR-010 to FR-011 | Yes | T040-T045 | Cancellation and generic not-found behavior covered. |
| FR-012 to FR-016 | Yes | T013, T040-T054 | Operator restrictions and concurrency covered. |
| FR-017 to FR-018 | Yes | T026, T047, T052, T054 | Pricing lock and generic not-found behavior covered. |
| FR-019 to FR-024 | Yes | T018-T019, T027-T030, T035-T044, T049-T052 | UI, identity switching, catalog, lifecycle display, and cancellation covered. |
| FR-025 | Partial | T025-T026, T029 | Editing is covered only for Intake/Processing; it does not cover the constitution's Backordered edit rule. |
| SC-001 | Partial | T032, T061 | No explicit timing test. |
| SC-002 | Yes | T020-T025, T032 | Pricing correctness and calculation history covered. |
| SC-003 | Partial | T035-T039, T061 | No explicit three-step interaction assertion. |
| SC-004 | Yes | T040-T045 | Terminal cancellation behavior covered. |
| SC-005 | Yes | T015, T022, T040, T047, T054 | Append-only audit history covered. |
| SC-006 | Yes | T033-T041 | Client scoping and ownership checks covered. |
| SC-007 | Partial | T061 | No explicit throughput/load test. |

### Constitution Alignment Issues

- **Critical:** The amended constitution permits Backordered line-item edits, while the
  specification and task list still prohibit or omit them.
- **High:** The plan's Constitution Check is stale and does not reflect the explicit
  Backordered -> Processing exception or the new Backordered editability rule.

### Unmapped Tasks

No implementation task is wholly orphaned. Setup, deployment, documentation, and validation
are cross-cutting tasks rather than direct mappings to one functional requirement.

### Metrics

- Total Requirements: 32, including 25 functional requirements and 7 buildable success criteria
- Total Tasks: 62
- Coverage: 100% of requirements have at least one associated task; 87.5% have complete
   rather than partial validation coverage, with FR-025 and three success criteria needing
   follow-up
- Ambiguity Count: 1
- Duplication Count: 1
- Critical Issues Count: 1

### Next Actions

Resolve C1 before `/speckit-implement`:

1. Run `/speckit-specify` or manually update FR-025, the Bulk Order entity definition, and
   the relevant edge cases to allow edits while Backordered.
2. Update T025, T029, T050, T052, and the user-story validation criteria to cover
   Backordered edits and repricing.
3. Run `/speckit-plan` or manually update the Constitution Check to document the amended
   lifecycle and editability rules.
4. Add explicit timed, throughput, and three-step UX validation tasks for G1-G3.

Would you like me to suggest concrete remediation edits for the top 3 issues?
