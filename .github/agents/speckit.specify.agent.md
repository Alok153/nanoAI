---
description: Create or update the feature specification from a natural language feature description.
handoffs:
  - label: Build Technical Plan
    agent: speckit.plan
    prompt: Create a plan for the spec. I am building with...
  - label: Clarify Spec Requirements
    agent: speckit.clarify
    prompt: Clarify specification requirements
    send: true
---

## Context
- Product: nanoAI Android app (Kotlin 2.2, Jetpack Compose M3, Hilt, WorkManager, Room, DataStore, Retrofit + Kotlin Serialization, Coroutines, JUnit5, Detekt, Spotless).
- Architecture: Composable → ViewModel → UseCase → Repository → DataSource. No direct cross-layer calls; Kotlin-only.
- Quality gates: ≥75% VM/domain, ≥65% UI, ≥70% data coverage; offline-first; cold start <1.5s; jank <5%; secrets/PII encrypted.

## Inputs
- Feature description from `$ARGUMENTS` (do not ask the user to repeat).
- Optional flags provided by the user: `--short-name`, `--number`.

## Preconditions
1. Run once: `.specify/scripts/bash/create-new-feature.sh --json "$ARGUMENTS"` (include `--short-name/--number` if given).
2. Parse JSON: `BRANCH_NAME`, `SPEC_FILE`, `FEATURE_DIR`, `FEATURE_NUM`.
3. Load `.specify/templates/spec-template.md`.

## Workflow
1. Populate the template in order, removing unused sections:
   - User Scenarios: ordered P1..Pn with independent test and acceptance scenarios; include loading/empty/error/offline paths.
   - Edge Cases: boundaries, failures, offline, accessibility.
   - Functional Requirements: testable, scope-focused, Kotlin/Android agnostic; cover offline behavior, privacy, and performance budgets; avoid implementation details.
   - Key Entities: name, purpose, fields/constraints, relationships.
   - Success Criteria: measurable, tech-agnostic outcomes (time, %, counts).
   - Assumptions and Out-of-scope.
2. Use `[NEEDS CLARIFICATION: ...]` only when the answer changes scope/UX/security; cap at 3. Default to project standards (offline capable, encrypted secrets, Material 3, coroutines) when unspecified.
3. Keep terminology consistent (prefer "user"), align with constitution rules, and avoid leaking implementation specifics.
4. Create checklist `FEATURE_DIR/checklists/requirements.md` from the checklist template. Validate the spec against it:
   - Iterate fixes up to 2 passes; leave notes if items still fail.
5. Save `spec.md`; ensure no dangling placeholders and no implementation detail leakage.

## Outputs
- Branch checked out, `SPEC_FILE` written, checklist created.
- Ready for `/speckit.clarify` if clarifications remain, otherwise `/speckit.plan`.
