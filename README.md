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
