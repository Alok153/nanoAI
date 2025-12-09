---
description: Execute the implementation planning workflow using the plan template to generate design artifacts.
handoffs:
  - label: Create Tasks
    agent: speckit.tasks
    prompt: Break the plan into tasks
    send: true
  - label: Create Checklist
    agent: speckit.checklist
    prompt: Create a checklist for the following domain...
---

## Context
- Kotlin-only Android app; layering: Composable → ViewModel → UseCase → Repository → DataSource.
- Budgets: cold start <1.5s, jank <5%, offline-first, secrets encrypted.
- Coverage gates: ≥75% VM/domain, ≥65% UI, ≥70% data.

## Preconditions
1. Run `.specify/scripts/bash/setup-plan.sh --json`; parse `FEATURE_SPEC`, `IMPL_PLAN`, `FEATURE_DIR`, `BRANCH`, `HAS_GIT`.
2. Load `.specify/templates/plan-template.md`, current `spec.md`, and `.specify/memory/constitution.md`.

## Workflow
1. Fill Technical Context (language Kotlin, Compose, Hilt, WorkManager/Room/DataStore/Retrofit, testing JUnit5/Turbine). Mark unknowns as `NEEDS CLARIFICATION`.
2. Constitution Check: assert Kotlin-only, clean layering, offline/performance/privacy requirements, and coverage targets. Note any violations with rationale.
3. Phase 0 – Research:
   - Turn each `NEEDS CLARIFICATION` into a research item in `research.md` (decision, rationale, alternatives).
   - Resolve blockers before design continues.
4. Phase 1 – Design & Contracts:
   - Derive entities/relationships into `data-model.md` from `spec.md`.
   - Generate API/contracts in `contracts/` for user actions; include error/offline behaviors.
   - Draft `quickstart.md` for primary happy path + failure/offline checks.
   - Run `.specify/scripts/bash/update-agent-context.sh copilot` to refresh agent context.
5. Re-check Constitution gates after design; document any justified exceptions.
6. Keep plan free of implementation detail; record assumptions and risks.

## Output
- Updated `plan.md`, plus `research.md`, `data-model.md`, `contracts/`, `quickstart.md` when applicable.
- Report branch, generated artifacts, and next step (`/speckit.tasks`).
