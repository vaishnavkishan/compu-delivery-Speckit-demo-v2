# Cross-Artifact Analysis Report: Bulk Hardware Order Management

**Command**: `/speckit-analyze` | **Date**: 2026-09-17 | **Branch**: `07-speckit-analyse`

**Artifacts analyzed**: `spec.md` (25 FR, 7 SC, 4 user stories), `plan.md`, `tasks.md` (62 tasks),
with `research.md`, `data-model.md`, `contracts/`, `quickstart.md` as supporting context, validated
against `.specify/memory/constitution.md` v1.0.0.

**Note**: `/speckit-analyze` is strictly read-only — it modified none of the artifacts below. This
file is a persisted copy of its output, captured for the walkthrough in `README.md`.

## Findings

| ID | Category | Severity | Location(s) | Summary | Recommendation |
|----|----------|----------|-------------|---------|----------------|
| D1 | Constitution | **CRITICAL** | `constitution.md:27`, `spec.md:129`, `plan.md:78`, `data-model.md:82`, `tasks.md:123` | Principle I states an order "MUST NOT re-enter a prior state once it has advanced." FR-006, the data-model transition diagram, and T046 all explicitly permit `BACKORDERED → PROCESSING` re-entry. The plan's gate table marks Principle I **PASS** with no deviation noted, but Governance requires every deviation to be "called out explicitly with a documented justification." | Either amend the constitution (MINOR bump) to admit on-hold re-entry, or record the deviation + justification in plan.md's Constitution Check and Complexity Tracking. Do not leave it as a silent PASS. |
| D2 | Constitution | HIGH | `constitution.md:26`, `constitution.md:78-85`, `spec.md:128` | The constitution's defined lifecycle and Domain Language are `intake → processing → final delivery`. FR-005 adds **Shipped** and **Backordered** as first-class stages; neither appears in the constitution's glossary. | Fold into the same constitution amendment as D1: add Shipped and Backordered to the Domain Language and Principle I's stage list. |
| E1 | Coverage | HIGH | `tasks.md:13`, `tasks.md:69`, `tasks.md:130`, `tasks.md:147`, `plan.md:40-42` | Self-contradicting test strategy. tasks.md says test creation "is not broken out as separate implementation tasks," yet T053 *does* create an integration test class, T031 adds "test scripts," and T061 runs "the complete backend unit/integration, frontend … validation commands." No task creates a single backend unit test or frontend test, so T061 will run empty suites and pass vacuously. | Add explicit test-authoring tasks per story, or rewrite the testing note and T061 to claim only what T053 actually builds. |
| E3 | Coverage | MEDIUM | `tasks.md:44`, `tasks.md:62`, `contracts/events.md:3` | **Unmapped tasks.** T017 and T024 implement the RabbitMQ `OrderIntaken` exchange, queue, and publisher. No FR or SC mentions events or messaging; events.md itself states there is no in-scope consumer. | Add an FR covering event publication for future consumers, or record it in plan.md as an explicitly accepted stack-driven addition beyond the spec. |
| E2 | Coverage | MEDIUM | `spec.md:164`, `spec.md:170`, `plan.md:55-57` | SC-001 (Net Total < 5s) and SC-007 (≥500 submissions/day without degradation) are carried into plan.md as Performance Goals but have **zero tasks**. SC-003 (< 3 clicks) likewise has no verification step. | Add a Phase 7 task to measure submission latency and throughput, or downgrade SC-001/SC-007 to non-verified demo-scale assumptions. |
| C1 | Underspecification | MEDIUM | `tasks.md:36-39`, `spec.md:150-158`, `data-model.md:8-18` | T009 creates an `operators` table, T010 seeds operators, T012 implements `client/Operator.java`. **Operator is not a Key Entity in spec.md and does not appear in data-model.md's entity list** — only as an `actor_type` enum value and in research.md #2. | Add an Operator entity to data-model.md and spec.md's Key Entities, or drop the table and treat operator identifiers as seeded reference values in the demo-identities endpoint only. |
| C2 | Underspecification | MEDIUM | `tasks.md:126`, `spec.md:112-118`, `plan.md:145` | T049 adds `OperatorStatusControls.tsx` and operator lifecycle controls to the order detail page. The spec's **User Interface Overview describes no operator-facing UI at all**, and plan.md's components list omits the component. FR-012 defines the capability but never where it surfaces. | Extend the UI Interface Overview to describe operator controls and their visibility rules; add the component to plan.md's structure. |
| C3 | Underspecification | MEDIUM | `spec.md:109` | The edge case "an order that would require a **negative or zero Net Total**" is posed as an open question and never resolved. FR-014 covers bad quantities/SKUs and FR-015 covers empty orders, but no FR addresses a discount ≥100%. | Add an FR (reject a non-positive Net Total with a clear message) or an explicit assumption that seeded discounts are bounded below 100%. |
| F1 | Inconsistency | MEDIUM | `spec.md:3`, `plan.md:3` | spec.md declares **Feature Branch `002-speckit-specify`** while the feature directory and plan.md both say `001-bulk-hardware-orders`. spec.md also still reads **Status: Draft** after two clarification sessions and a completed plan/tasks cycle. | Correct the branch header and advance Status past Draft. |
| A1 | Duplication | MEDIUM | `tasks.md:20-21`, `tasks.md:69` | T031 (US1) re-edits `services/order-api/pom.xml` and `apps/order-portal/package.json` — the exact files T002 and T003 scaffold in Phase 1. Overlapping file ownership across phases. | Merge T031's build-script content into T002/T003, or restate T031 as the specific additions it makes beyond scaffolding. |
| B1 | Ambiguity | LOW | `spec.md:127` | FR-004 blocks finalization when contract terms are "missing, **ambiguous**, or expired," but the spec never defines ambiguous. research.md #6 resolves it as ">1 matching row" — the operational definition lives only in a Phase 0 artifact. | Promote research #6's definition into FR-004 so the spec is self-contained. |
| B2 | Ambiguity | LOW | `tasks.md:145` | T059 ("accessibility, responsive-layout, and error-state polish") has no measurable criterion — no WCAG level, no breakpoints — and **no spec requirement backs accessibility at all**. | Name a target (e.g. WCAG 2.1 AA on the two pages) or drop the accessibility clause. |
| A2 | Duplication | LOW | `spec.md:138`, `spec.md:147` | FR-015 (API rejects zero line items) and FR-024 (UI blocks zero total quantity) overlap; FR-024 self-labels as "reinforces FR-015." | Intentional layering — no action needed. |
| F2 | Inconsistency | LOW | `plan.md:112`, `spec.md:31`, `spec.md:183` | plan.md lists `create-order-page.html` among in-scope Figma reference files. The 2026-08-17 clarification scoped the reference design to `enterprise-user-dashboard.html` + `enterprise-user-detail-page.html`; `create-order-page.html` is the superseded single-page design. | Remove `create-order-page.html` from plan.md:112's reference list. |
| F3 | Inconsistency | LOW | `spec.md:23`, `spec.md:145`, `spec.md:146` | Terminology drift on the 5th stepper column: the 2026-08-13 clarification says **"Delivered"**, FR-022/FR-023 say **"Final Delivery"**, the API and data model use `FINAL_DELIVERY`. | Pick one display label and one domain label, and state the mapping once in FR-022. |
| E4 | Coverage | LOW | `tasks.md:130` | T053 creates the single Testcontainers integration test class but is filed under US4 (P3), although it validates behavior from US1–US4. Anyone stopping at the MVP checkpoint gets no integration coverage. | Move T053 to Phase 2 (Foundational) or Phase 7 (Cross-Cutting). |

15 findings total — under the 50-row limit, nothing truncated.

## Coverage Summary

All 25 functional requirements map to at least one task.

| Requirement Key | Has Task? | Task IDs | Notes |
|---|---|---|---|
| FR-001 – FR-004 (intake, gross, net, blocked terms) | Yes | T020–T023 | Fully covered |
| FR-005, FR-006 (lifecycle set, no skips/reversals) | Yes | T015, T046, T047 | See **D1** — the Backordered reversal conflicts with Principle I |
| FR-007 (audit history) | Yes | T015, T022, T025, T040, T047 | Append-only tables |
| FR-008, FR-009 (own history, scoping) | Yes | T033, T034 | Repository-level `client_id` filter |
| FR-010, FR-011 (cancel, rejection reasons) | Yes | T040, T041 | |
| FR-012 (operator-only advancement) | Yes | T013, T047, T048 | UI surface underspecified — see **C2** |
| FR-013 (order-level, not line-level) | Weak | T015 | Covered only structurally; no task asserts the invariant |
| FR-014, FR-015 (invalid qty/SKU, empty order) | Yes | T021, T025 | Non-positive Net Total unhandled — see **C3** |
| FR-016 (first-committed-wins) | Yes | T014, T022, T025, T040, T047 | Optimistic locking → 409 |
| FR-017 (lock at Shipped) | Yes | T047, T052 | |
| FR-018 (generic not-found) | Yes | T026, T034, T041 | |
| FR-019 (demo identity switcher) | Yes | T018, T037 | Backing `Operator` entity undefined — see **C1** |
| FR-020, FR-021 (catalog entry, pricing summary) | Yes | T027, T028 | |
| FR-022, FR-023 (5-step stepper, status indicators) | Yes | T030, T035, T042, T044, T051 | Label drift — see **F3** |
| FR-024 (UI blocks empty order) | Yes | T029 | |
| FR-025 (edit while Intake/Processing) | Yes | T025, T026, T029 | |
| SC-001 (Net Total < 5s) | **No** | — | **Gap — E2** |
| SC-002 (100% contract-driven) | Yes | T020, T032 | |
| SC-003 (< 3 clicks to status) | Partial | T035, T036 | Built but never measured |
| SC-004 (100% invalid cancels rejected) | Yes | T040, T045 | |
| SC-005 (100% transitions audited) | Yes | T015, T047 | |
| SC-006 (zero cross-client access) | Yes | T033, T034, T039 | |
| SC-007 (≥500 orders/day) | **No** | — | **Gap — E2** |

## Constitution Alignment Issues

- **Principle I (Order Lifecycle Integrity)** — violated as written by FR-006's
  `BACKORDERED → PROCESSING` re-entry and by two stages absent from the constitution's lifecycle
  definition. The plan's gate table reports PASS without documenting either deviation, which is
  itself a Governance violation ("any deviation MUST be called out explicitly with a documented
  justification rather than silently implemented"). See **D1**, **D2**.
- **Principles II, III, IV, V** — no conflicts. The contract-terms lookup, repository-level
  `client_id` scoping, append-only history tables, and order-level status/cancellation fields all
  hold across spec, data model, and tasks.
- **Domain boundary** — holds. No task creates Warehouse or Invoice service/app code, matching the
  2026-08-17 out-of-scope clarification.

## Unmapped Tasks

- **T017, T024** — RabbitMQ topology and `OrderIntaken` publisher: no FR/SC in spec.md (see **E3**).
- **T055–T058, T062** — Helm charts, nginx/env config, README: deployment infrastructure with no
  spec requirement. Normal for Phase 7 polish; noted for completeness, not flagged as a defect.

## Metrics

| Metric | Value |
|---|---|
| Total requirements | 32 (25 FR + 7 SC) |
| Total tasks | 62 (T001–T062) |
| FR coverage | **25 / 25 = 100%** |
| SC coverage (buildable) | 5 / 7 = 71% (SC-001, SC-007 uncovered) |
| Overall coverage | 30 / 32 = **94%** |
| Ambiguity count | 2 |
| Duplication count | 2 |
| Unresolved placeholders | 1 (`TODO(RATIFICATION_DATE)` in constitution.md:129) |
| **Critical issues** | **1** |
| High / Medium / Low | 2 / 6 / 6 |

## Next Actions

**Resolve before `/speckit-implement`:**

1. **D1 + D2 (CRITICAL/HIGH)** — the Backordered re-entry and the two extra lifecycle stages
   conflict with Principle I as written. Run `/speckit-constitution` to amend Principle I and the
   Domain Language (MINOR → v1.1.0), *then* update plan.md's Constitution Check row to cite the
   amended principle. Do not implement T046 against a gate table claiming a PASS the constitution
   does not support.
2. **E1 (HIGH)** — decide the test strategy. Edit tasks.md to either add unit/component test tasks
   per story or narrow T031/T061 to what T053 actually provides. Shipping T061 as-is produces a
   green run that proves nothing.

**Recommended but non-blocking:**

3. **C1, C2, C3** — run `/speckit-specify` with refinement to add the Operator key entity, describe
   operator controls in the UI Interface Overview, and resolve the non-positive Net Total edge case.
   Then update data-model.md.
4. **E2, E3** — add a Phase 7 performance-verification task for SC-001/SC-007, and record the
   `OrderIntaken` event as an accepted stack-driven addition in plan.md.
5. **A1, F1, F2, F3, B1, B2, E4** — low-cost manual edits: fix the spec branch header and status,
   drop `create-order-page.html` from plan.md:112, settle the Delivered/Final Delivery label,
   promote research #6's "ambiguous" definition into FR-004, merge T031 into T002/T003, and move
   T053 to Phase 2 or 7.
