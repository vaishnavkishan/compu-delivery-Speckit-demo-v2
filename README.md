# Overview

This repo is a worked example of [GitHub Speckit](https://github.com/github/spec-kit) and
Spec-Driven Development (SDD): write and refine a spec first, then let it drive planning, task
breakdown, and implementation.

Read top to bottom — each step below corresponds to one branch, so you can follow the git history
alongside the writeup. Every step includes the raw slash-command prompt used.

TLDR;

```
constitution -> specify -> clarify -> plan -> checklist (optional) -> tasks -> analyse (optional) -> implement
```

## Prerequisites (branch name 00-speckit-initialize)

### 1. Install uv

Speckit's CLI (`specify`) runs via [uv](https://docs.astral.sh/uv/).

```bash
curl -LsSf https://astral.sh/uv/install.sh | sh
uv --version
```

### 2. Install the Specify CLI

Docs: [github/spec-kit](https://github.com/github/spec-kit)

```bash
uv tool install specify-cli --from git+https://github.com/github/spec-kit.git
```

Or run on demand without installing:

```bash
uvx --from git+https://github.com/github/spec-kit.git specify --help
```

### 3. Initialize Speckit in this repo

Ran `specify init .`. Generates:

- `.specify/` — constitution/memory, templates, scripts, workflow config
- `.claude/skills/` — Speckit slash-command skills for Claude

## Steps

> Commands below can be run in VS Code chat (Claude Code, Copilot, etc.) or via the `specify` CLI.

### 1. Generate the project constitution (`/speckit-constitution`, branch 01-speckit-constitution)

Derives `.specify/memory/constitution.md` from requirements — domain language, boundaries, and
standards only.

```
/speckit-constitution add constitution based on below requirements. Extract high-level domain rules and functional conventions—do not transcribe feature requirements directly.

Requirements:
We need a system that accepts bulk hardware orders from enterprise clients, calculates their final net totals using pre-negotiated contract discounts, and tracks each order through its lifecycle from intake to final delivery. Clients should be able to view their order history or cancel active requests.

**Extract & Add:**

* **Domain Language:** Glossary, domain-specific terminology, and abbreviations.
* **Governance & Boundaries:** Domain boundaries, data governance policies, and usage rules.
* **Standards:** Core standards and functional principles.

**Constraints:**

* Strictly exclude raw requirements, user stories, technical architecture, and implementation details.
```

### 2. Generate the feature specification (`/speckit-specify`, branch 02-speckit-specify)

Turns requirements into `spec.md` — user stories, functional requirements, success criteria.

```
/speckit-specify refer below requirements and create specification document

Requirements:
We need a system that accepts bulk hardware orders from enterprise clients, calculates their final net totals using pre-negotiated contract discounts, and tracks each order through its lifecycle from intake to final delivery. Clients should be able to view their order history or cancel active requests.
```

### 3. Clarify the feature specification (`/speckit-clarify`, branch 03-speckit-clarify)

Surfaces underspecified areas in `spec.md` — up to 5 targeted questions, then encodes accepted
answers back into `spec.md` (`## Clarifications`, plus Edge Cases/FRs/Key Entities/Assumptions).

#### No argument:

```
/speckit-clarify
```

#### Specific requirement:

```
/speckit-clarify As this is a sample/demo application, we do not want to implement authorization and authentication currently
```

#### Using Figma designs:

Can take a Figma HTML/CSS export as input — diffs it against the current spec/constitution and
asks about each real conflict or gap.

```
/speckit-clarify analyse the Figma design html specs/001-bulk-hardware-orders/figma-designs/create-order-page.html and add the information about the UI/UX to be same as html when it is implemented
```

#### Re-clarifying after designs change:

Re-running against an updated `figma-designs/` folder catches new drift — e.g. it caught new
undefined personas (Invoice Staff, Warehouse Operator), a reverted lifecycle stepper, a
single-page-to-two-page layout split, and newly-editable post-submission line items.

```
/speckit-clarify verify that the spec is matching the designs from html figma-designs folder
```

### 4. Generate the implementation plan (`/speckit-plan`, branch 04-speckit-plan)

Fills in Technical Context, runs a Constitution Check gate table, then Phase 0 research and
Phase 1 design:

- `plan.md` — Technical Context, Constitution Check (all 5 gates PASS), project structure
- `research.md` — Phase 0 decisions (trusted-header identity, optimistic locking, lifecycle
  enforcement, contract-terms lookup, etc.)
- `data-model.md` — Phase 1 entities, relationships, validation rules, state transitions
- `contracts/openapi.yaml` and `contracts/events.md` — REST API and RabbitMQ event contracts
- `quickstart.md` — Phase 1 end-to-end validation guide

#### With tech stack:

Tech stack and architecture decisions are too important to leave to an AI agent's guess, so that
info needs to be passed explicitly in the plan command. Speckit has no built-in command that
suggests a stack — instead, the current spec is used as context in a normal AI chat to define the
architecture and stack manually, then supplied to `/speckit-plan` directly:

```
/speckit-plan Stack:
- Backend: Java 25, Spring Boot 3.5 (Web, Validation, Data JPA, Actuator, AMQP/RabbitMQ), Maven, Lombok, Flyway, springdoc-openapi
- DB & Broker: PostgreSQL 17 (@Version optimistic locking), RabbitMQ
- Frontend: React 19, TypeScript, Vite, Tailwind
- Testing: JUnit 5, Mockito, AssertJ, Testcontainers | Vitest, RTL
- Infra: Docker (multi-stage), k3s / Rancher Desktop, Helm -> K8s

In Scope:
- Order API & DB: Order domain (intake, pricing, status lifecycle, cancellation, contract terms, history). Publishes `OrderIntaken` event to RabbitMQ.
- Message Queue: RabbitMQ broker.
- Portal UI: Enterprise Client dashboard + order create/detail views (role-gated shell).

Out of Scope (Future):
- Warehouse API / DB & Operator UI
- Invoicing API / DB & Staff UI
```

#### Revising the plan for a monorepo layout:

`/speckit-plan` can be re-run against an existing plan with new guidance instead of only from
scratch. Here, a follow-up prompt said the repo should be a monorepo housing order, warehouse,
and invoice APIs/frontends side by side. Re-running updated the artifacts in place:

- `plan.md` — `services/*` (one Maven module per API — `order-api` implemented,
  `warehouse-api`/`invoice-api` reserved) and `apps/*` (one app per portal — `order-portal`
  implemented, others reserved) under a root reactor `pom.xml`, with per-service Helm charts
- `research.md` — module-layout and deployment-path decisions revised in place, old decision
  kept visible alongside the new one
- `quickstart.md` — commands/paths updated to `services/order-api/` and `apps/order-portal/`

Scope is unchanged — only the order feature is implemented; Warehouse/Invoicing stay out of
scope as reserved directories.

```
/speckit-plan this is going to be a mono repo setup and will include order, warehouse and invoice apis and frontend
```

### 5. Generate a requirements checklist (`/speckit-checklist`, branch 05-speckit-checklist)

Creates a requirements-quality checklist for the feature. The checklist acts as "unit tests for
English": it evaluates whether requirements are complete, clear, consistent, measurable, and
ready for implementation. It does not test application behavior or implementation details.

Reviewer can open the generated checklist file in an editor. Add missing edge cases, modify vague criteria, or delete irrelevant checks.

The generated checklist is appended to
`specs/001-bulk-hardware-orders/checklists/requirements.md` and covers:

- Requirement completeness and clarity
- Pricing, lifecycle, concurrency, and data-ownership consistency
- Acceptance criteria and scenario coverage
- Edge cases, non-functional requirements, dependencies, and assumptions
- Ambiguities and conflicts that still need resolution

```
/speckit-checklist
```

### 6. Generate the implementation tasks (`/speckit-tasks`, branch 06-speckit-tasks)

Turns the specification and design artifacts into an actionable, dependency-ordered task list
organized by user story. Each task includes a sequential ID, an optional parallelization marker,
the applicable story label, and a concrete file path.

The generated task list is written to
`specs/001-bulk-hardware-orders/tasks.md` and includes:

- Shared setup and foundational infrastructure
- One independently testable phase for each prioritized user story
- Backend, frontend, persistence, messaging, and deployment tasks mapped to the plan
- Dependencies, parallel execution opportunities, and implementation strategy
- An MVP scope focused on User Story 1 (submit and price a bulk order)

```
/speckit-tasks
```

### 7. Analyse the artifacts for consistency (`/speckit-analyze`, branch 07-speckit-analyse, commit `35f686a`)

A read-only consistency check across `spec.md`, `plan.md`, and `tasks.md`, validated against the
constitution. Run it after `/speckit-tasks` and before `/speckit-implement` — it reports, it never
edits. Passes cover duplication, ambiguity, underspecification, constitution alignment, coverage
gaps, and inconsistency; constitution violations are always CRITICAL.

This run found 15 issues across 62 tasks and 32 requirements — including one CRITICAL
(`BACKORDERED → PROCESSING` in FR-006 contradicts the constitution's "MUST NOT re-enter a prior
state," yet the plan's gate table still said PASS) and a test strategy that runs suites no task
creates.

The command writes no files; this run's output is kept at
[`specs/001-bulk-hardware-orders/analysis-report.md`](specs/001-bulk-hardware-orders/analysis-report.md).

```
/speckit-analyze
```

#### Re-running the constitution to resolve a CRITICAL finding (commit `ef5bf10`):

`/speckit-analyze` only reports a violation — you decide whether the spec or the constitution is
the wrong side. Here the backordered stage was wrong, so the constitution is amended first and the
spec, plan, and tasks follow.

Bumped 1.0.0 → 2.0.0 (MAJOR — both are backward-incompatible redefinitions):

- **Principle I** — lifecycle is now exactly four states (Intake → Processing → Shipped → Final
  Delivery), with no on-hold/backordered state allowed.
- **Principle III** — cancellation window narrowed to "prior to Shipped".

Scope guard: the command edits only `.specify/memory/constitution.md`, and reports conflicting
downstream artifacts as a `Next Actions` list instead of rewriting them.

```
/speckit-constitution There is no need to implement backordered stage. The complete order lifecycle is Intake, Processing, Shipped, Final Delivery with Cancellation as an allowed branch at any point prior to Shipped
```

#### Re-clarifying the spec against the amended constitution (commit `3c72979`):

The spec still described the backordered stage, so it is re-clarified next. 5 questions:
lifecycle cut to four states, cancellation window closed at Shipped, an operator surface for
lifecycle advancement (new FR-026, FR-027), and Order History limited to completed orders.

```
/speckit-clarify
```

#### Re-planning the design artifacts against the amended constitution (commit `edea0cd`):

The plan and its design artifacts were still the pre-amendment ones, so `/speckit-plan` is re-run.
It regenerates all five — `plan.md`, `research.md`, `data-model.md`, `contracts/`, `quickstart.md` —
drops Backordered from the lifecycle, derives the cancellation window from status, adds the operator
surface (FR-026, FR-027), and re-checks all five gates (PASS).

`tasks.md` and `checklists/requirements.md` stay stale — each belongs to the command that owns it.

```
/speckit-plan
```

#### Re-generating the tasks against the amended constitution (commit `78ee06b`):

`tasks.md` was the last stale artifact, so `/speckit-tasks` is re-run. It regenerates all 69 tasks
(was 62): Backordered and the five-column stepper are gone, and new tasks cover the cancellation
window closing at Shipped, the Active/History partition, role-conditional controls, and operator
cross-client scope (FR-023, FR-026, FR-027).

```
/speckit-tasks
```
