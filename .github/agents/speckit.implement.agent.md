---
description: Execute the implementation plan by processing and executing all tasks defined in tasks.md
---

## Context
- Kotlin-only, clean architecture, offline/performance/privacy budgets, coverage gates (≥75% VM/domain, ≥65% UI, ≥70% data).

## Preconditions
1. Run `.specify/scripts/bash/check-prerequisites.sh --json --require-tasks --include-tasks`; parse paths.
2. If `tasks.md` missing, stop and request `/speckit.tasks`.
3. Check checklists in `FEATURE_DIR/checklists/` if present; if any item unchecked, ask whether to proceed before continuing.
4. Required reads: `tasks.md`, `plan.md`; optional: `data-model.md`, `contracts/`, `research.md`, `quickstart.md`.

## Workflow
1. Verify ignore files for Android/Kotlin: `.gitignore` (build/, app/build/, .gradle/, .idea/, *.iml, local.properties, *.keystore), add only if missing.
2. Follow tasks **in order** respecting dependencies; `[P]` tasks may run in parallel when touching different files.
3. Prefer TDD where tests exist; add/keep unit tests for ViewModels/use cases/repos; add UI tests for critical flows and offline/a11y paths.
4. Implement using coroutines and Material 3; keep UI stateless with state in ViewModels/StateFlow; repositories handle data sources.
5. Ensure offline behavior, error/loading states, telemetry/logging, and performance budgets are honored per plan.
6. Update tasks as completed ([X]) only when work and tests for that task pass locally.

## Output
- Report completed tasks, failures, and remaining blockers. Suggest next quality gates (spotless, detekt, testDebugUnitTest, verifyCoverageThresholds).
