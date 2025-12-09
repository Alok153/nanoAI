---
description: Perform a non-destructive cross-artifact consistency and quality analysis across spec.md, plan.md, and tasks.md after task generation.
---

## Context
- Read-only analysis; constitution rules are authoritative (Kotlin-only, clean layering, offline/performance/privacy, coverage targets).

## Preconditions
1. Run `.specify/scripts/bash/check-prerequisites.sh --json --require-tasks --include-tasks`; parse `FEATURE_DIR`, `AVAILABLE_DOCS`.
2. Resolve paths: `SPEC=spec.md`, `PLAN=plan.md`, `TASKS=tasks.md` under `FEATURE_DIR`; load `.specify/memory/constitution.md`.

## Workflow
1. Load minimal necessary sections:
   - spec: overview/stories, functional/non-functional requirements, edge cases.
   - plan: technical context, architecture, constraints, phases.
   - tasks: IDs, phases, [P] markers, file paths.
2. Build internal mappings: requirements ↔ user stories ↔ tasks; note terminology and entities.
3. Detect issues (limit to ~50 findings):
   - Duplication/ambiguity/underspecification.
   - Constitution violations (always CRITICAL).
   - Coverage gaps (requirements without tasks; tasks without requirements/stories; missing non-functional coverage for performance/offline/security/a11y).
   - Inconsistencies (terminology, data model, ordering/dependency contradictions).
4. Assign severity: CRITICAL (constitution/missing core coverage), HIGH, MEDIUM, LOW.

## Output
- Markdown table of findings: ID, Category, Severity, Location(s), Summary, Recommendation.
- Coverage summary table: requirement key → has task? → task IDs/notes.
- Sections for Constitution issues and unmapped tasks.
- Metrics: counts of requirements, tasks, coverage %, ambiguity/duplication counts, critical issues.
- Suggest next actions (e.g., rerun `/speckit.tasks` or update spec/plan). Do **not** modify files.
