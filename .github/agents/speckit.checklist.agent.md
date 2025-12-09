---
description: Generate a custom checklist for the current feature based on user requirements.
---

## Context
- Checklists are “unit tests for requirements,” not implementation tests. Keep focus on clarity, completeness, consistency, coverage, and measurability of requirements (including offline, performance, accessibility, security, privacy).

## Preconditions
1. Run `.specify/scripts/bash/check-prerequisites.sh --json`; parse `FEATURE_DIR`, `AVAILABLE_DOCS`.
2. Load `spec.md` and `plan.md`; optionally `tasks.md`, `data-model.md`, `contracts/` if present.

## Workflow
1. Derive up to 3 clarifying questions (scope, audience, rigor) only if answers change checklist content. Skip if unneeded.
2. Decide checklist type/name from `$ARGUMENTS` (e.g., ux, api, security); create `FEATURE_DIR/checklists/` if missing. Each run creates a new file with a descriptive filename.
3. Use `.specify/templates/checklist-template.md` structure: title/meta, categories, numbered `CHK###` items.
4. Craft items that test requirement quality, not behavior:
   - Completeness/coverage of stories, edge/error/offline paths, a11y, performance budgets, privacy/security.
   - Clarity/measurability of terms (fast, secure, robust) with targets.
   - Consistency across spec/plan/tasks and terminology.
   - Dependencies/assumptions documented.
   - Traceability to spec sections where possible; mark gaps explicitly.
5. Keep wording concise; avoid implementation verbs (“verify”, “click”).

## Output
- New checklist file path, item count, focus areas, and reminder that each invocation generates a new checklist.
