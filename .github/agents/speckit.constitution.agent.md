---
description: Create or update the project constitution from interactive or provided principle inputs, ensuring all dependent templates stay in sync.
handoffs:
  - label: Build Specification
    agent: speckit.specify
    prompt: Implement the feature specification based on the updated constitution. I want to build...
---

## Context
- Constitution lives at `.specify/memory/constitution.md` and governs all gates (Kotlin-only, clean architecture, coverage/performance/privacy budgets, offline-first).

## Preconditions
1. Load `.specify/memory/constitution.md` template and identify placeholders `[TOKEN]`.
2. Gather inputs from `$ARGUMENTS` and repo context (README, docs) for project name, principles, governance dates, and version.

## Workflow
1. Determine version bump (PATCH wording, MINOR new/expanded principles/sections, MAJOR breaking removals). Set `LAST_AMENDED_DATE` to today; keep or set `RATIFICATION_DATE` if missing.
2. Replace all placeholders with concrete text; no leftover bracketed tokens unless explicitly deferred with `TODO(<FIELD>): reason`.
3. Ensure each principle is concise, testable, and aligned to project: Kotlin-only, layer boundaries, offline/performance budgets, security/privacy, documentation/testing expectations.
4. Update Governance section with amendment procedure and compliance expectations.
5. Propagate consistency: check `.specify/templates/plan-template.md`, `.specify/templates/spec-template.md`, `.specify/templates/tasks-template.md`, and `.specify/templates/agent-file-template.md` for required wording changes; adjust if constitution changes mandates.
6. Prepend a Sync Impact Report (HTML comment) summarizing version change, modified/added/removed sections, template updates (✅/⚠), and follow-up TODOs.

## Output
- Overwrite `.specify/memory/constitution.md` with updated content.
- Report new version, bump rationale, touched templates, outstanding TODOs, and suggested commit message (`docs: amend constitution to vX.Y.Z`).
