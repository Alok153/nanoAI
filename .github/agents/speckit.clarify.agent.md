---
description: Identify underspecified areas in the current feature spec by asking up to 5 targeted clarification questions and encoding answers back into the spec.
handoffs:
  - label: Build Technical Plan
    agent: speckit.plan
    prompt: Create a plan for the spec. I am building with...
---

## Context
- Same product constraints as speckit.specify: Kotlin-only, clean architecture layers, offline-first, privacy-first, and coverage/performance budgets.

## Inputs
- `$ARGUMENTS` for any user hints or exclusions.

## Preconditions
1. Run `.specify/scripts/bash/check-prerequisites.sh --json --paths-only` once; parse `FEATURE_DIR`, `FEATURE_SPEC`, `IMPL_PLAN`, `TASKS`.
2. Load `spec.md` from `FEATURE_SPEC`.

## Workflow
1. Scan the spec for gaps across: functional scope, roles, data/entities, UX flows (loading/empty/error/offline), non-functional (performance, accessibility, security/privacy, observability), integrations, and edge cases.
2. Build up to 5 high-impact questions (short-answer or 2–5 options). Ask **one at a time**; prefer defaults that align with project standards.
3. When an answer is accepted:
   - Ensure `## Clarifications` exists (add `### Session YYYY-MM-DD`).
   - Append `- Q: <question> → A: <answer>` under today’s session.
   - Update the relevant spec sections (requirements, user stories, entities, success criteria, edge cases) with the clarified detail; remove contradictions/placeholders.
   - Save after each integration.
4. Stop when all critical gaps are closed, the user says stop, or 5 questions are used.

## Validation & Output
- No more than 5 questions; no unresolved contradictions.
- Spec keeps template order and remains implementation-free.
- Report: questions answered, sections touched, outstanding/deferred gaps, and next suggested command (usually `/speckit.plan`).
