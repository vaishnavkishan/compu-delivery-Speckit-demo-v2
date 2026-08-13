# Overview

This repo is a worked example of [GitHub Speckit](https://github.com/github/spec-kit) and Spec-Driven
Development (SDD) — a workflow where you write and refine a specification first, and let that
specification (not an ad-hoc prompt) drive planning, task breakdown, and implementation. It exists
to answer a simple question: what does it actually look like to build a real feature this way, end
to end, instead of just reading about the methodology?

If you're new to this repo, the best way to read it is top to bottom: this README is a
step-by-step tutorial, and each major step below corresponds to one branch, so you can check the git
history alongside the writeup to see exactly what each Speckit command generated or changed at
that stage. Every step also includes the raw slash-command prompt that was run, so you can see the
actual input, not just the resulting diff.

TLDR;
Run the github speckit commands in following order to generate code using Spec-driven development

```
constitution -> specify -> clarify -> plan -> checklist -> tasks -> analyse -> implement
```

## Prerequisites (branch name 00-speckit-initialize)

### 1. Install uv

Speckit's CLI (`specify`) runs via [uv](https://docs.astral.sh/uv/), a fast Python package/tool manager.

```bash
curl -LsSf https://astral.sh/uv/install.sh | sh
```

Verify the install:

```bash
uv --version
```

### 2. Install the Specify CLI

Official docs/reference: [github/spec-kit](https://github.com/github/spec-kit)

Install it as a persistent uv tool:

```bash
uv tool install specify-cli --from git+https://github.com/github/spec-kit.git
```

Or run it on demand without installing, via `uvx`:

```bash
uvx --from git+https://github.com/github/spec-kit.git specify --help
```

### 3. Initialize Speckit in this repo

Ran `specify init .` in the project root. It scaffolds Spec-Driven Development (SDD) into the current directory and generates:

- `.specify/` — constitution/memory, templates, scripts, and workflow config for the SDD process
- `.claude/skills/` — Speckit slash-command skills for Claude (specify, plan, tasks, implement, etc.)

## Steps

> The Speckit slash commands below can be run either in the VS Code chat (Claude Code, Copilot, etc.) or via the `specify` CLI directly.

### 1. Generate the project constitution (`/speckit-constitution`, branch name 01-speckit-constitution)

Ran the `/speckit-constitution` skill to derive
`.specify/memory/constitution.md` from requirements — domain language, boundaries, and standards only.

The raw prompt:

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

### 2. Generate the feature specification (`/speckit-specify`, branch name 02-speckit-specify)

With the constitution in place, ran the `/speckit-specify` skill to turn
requirements into `spec.md` — user stories,
functional requirements, and success criteria, produced from the domain description rather than
handwritten.

The raw prompt:

```
/speckit-specify refer below requirements.md and create specification document

Requirements:
We need a system that accepts bulk hardware orders from enterprise clients, calculates their final net totals using pre-negotiated contract discounts, and tracks each order through its lifecycle from intake to final delivery. Clients should be able to view their order history or cancel active requests.
```

### 3. Clarify the feature specification (`/speckit-clarify`, branch name 03-speckit-clarify)

Ran the `/speckit-clarify` skill against `spec.md` to surface underspecified areas the constitution
and initial spec didn't resolve — asked up to 5 targeted questions one at a time, then encoded each
accepted answer directly back into `spec.md` (a new `## Clarifications` section, plus updates to the
affected Edge Cases, Functional Requirements, Key Entities, and Assumptions).

The raw prompt (run with no argument):

```
/speckit-clarify
```

It can also be supplied specific information:

```
/speckit-clarify As this is a sample/demo application, we do not want to implement authorization and authentication currently
```

`/speckit-clarify` can also take a Figma design export (HTML/CSS) as an attachment instead of, or alongside, free-text guidance. This matters because a spec written before any UI exists is silent on interaction details a mockup makes concrete — e.g. this project's Figma export showed a second "volume discount" line, a 5-step order-status tracker, and a fixed hardware catalog that weren't in `spec.md` yet. Clarify treats the design as another source of ambiguity to resolve: it diffs what the mockup shows against the existing functional requirements and constitution, asks about each real conflict or gap one at a time, then encodes the accepted answers back into `spec.md` (a new `### User Interface Overview` subsection and UI-specific Functional Requirements) — so the spec, not the mockup, stays the single source of truth for implementation.

```
/speckit-clarify analyse the attached Figma design html and add the information about the UI/UX to be same as html when it is implemented
```
