---
description: Generate an actionable, dependency-ordered tasks.md for the feature based on available design artifacts.
handoffs:
  - label: Analyze For Consistency
    agent: speckit.analyze
    prompt: Run a project analysis for consistency
    send: true
  - label: Implement Project
    agent: speckit.implement
    prompt: Start the implementation in phases
    send: true
---

## Context
- Enforce Kotlin-only, clean layering, offline/performance/privacy gates, and coverage targets.

## Preconditions
1. Run `.specify/scripts/bash/check-prerequisites.sh --json`; parse `FEATURE_DIR`, `AVAILABLE_DOCS`.
2. Required reads: `plan.md` and `spec.md`. Optional: `data-model.md`, `contracts/`, `research.md`, `quickstart.md` if present.
3. Load `.specify/templates/tasks-template.md`.

## Workflow
1. Extract user stories with priorities (P1..Pn) from `spec.md`; align with entities/contracts from supporting docs.
2. Use strict task format: `- [ ] T### [P] [USx] Description with exact file path`. `[P]` only if parallelizable; `[USx]` only in story phases.
3. Phases:
   - Phase 1 Setup: repo/gradle/scaffolding, lint/format, CI hooks.
   - Phase 2 Foundational: shared infra (DI modules, Room schema, networking, offline storage, logging/telemetry) needed before any story.
   - Phase 3+ One phase per user story in priority order: tests (if requested/implicit for coverage) → models/entities → domain/use cases → data/repositories → UI/compose/screens → wiring. Include offline/error/loading handling and accessibility.
   - Final Polish & cross-cutting (perf, accessibility, telemetry, docs).
4. Mark dependencies when tasks touch same file; prefer parallel `[P]` otherwise. Map every requirement/story to at least one task; avoid orphan tasks.
5. Include Android/Kotlin essentials: Detekt/Spotless, unit tests, instrumentation where applicable, performance budgets, and privacy/secret handling.

## Output
- Write `tasks.md` under `FEATURE_DIR` using the template structure and checklist format.
- Report totals (overall and per story), parallel opportunities, dependency order, and suggested MVP scope (usually P1).
