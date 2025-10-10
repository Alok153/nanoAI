# Feature Specification: Fixes and Inconsistencies (Pre-Phase 4 Stabilization)

**Feature Branch**: `004-fixes-and-inconsistencies`  
**Created**: 2025-10-03  
**Status**: Draft  
**Input**: User description: "fixes and inconsistencies

There are many part of the codebase that I have documented that i want fixed before moving to phase 4 of the development as per todo-next.md, We have general-notes.md and inconsistencies.md (in specs/004-) where i have listed the problems
I want to tackle the issues that can be fixed before moving on to the phase 4 of the development (See <attachments> above for file contents.)"

## Execution Flow (main)
```
1. Parse user description from Input
   → If empty: ERROR "No feature description provided"
2. Extract key concepts from description
   → Identify: actors, actions, data, constraints
3. For each unclear aspect:
   → Mark with [NEEDS CLARIFICATION: specific question]
4. Fill User Scenarios & Testing section
   → If no clear user flow: ERROR "Cannot determine user scenarios"
5. Generate Functional Requirements
   → Each requirement must be testable
   → Mark ambiguous requirements
6. Identify Key Entities (if data involved)
7. Run Review Checklist
   → If any [NEEDS CLARIFICATION]: WARN "Spec has uncertainties"
   → If implementation details found: ERROR "Remove tech details"
8. Return: SUCCESS (spec ready for planning)
```

---

## ⚡ Quick Guidelines
- ✅ Focus on WHAT users need and WHY
- ❌ Avoid HOW to implement (no tech stack, APIs, code structure)
- 👥 Written for business stakeholders, not developers
- 🎯 Capture Material UX, performance, offline, privacy, AI integrity, and up-to-date documentation expectations aligned with the constitution.

### Section Requirements
- **Mandatory sections**: Must be completed for every feature
- **Optional sections**: Include only when relevant to the feature
- When a section doesn't apply, remove it entirely (don't leave as "N/A")

### For AI Generation
When creating this spec from a user prompt:
1. **Mark all ambiguities**: Use [NEEDS CLARIFICATION: specific question] for any assumption you'd need to make
2. **Don't guess**: If the prompt doesn't specify something (e.g., "login system" without auth method), mark it
3. **Think like a tester**: Every vague requirement should fail the "testable and unambiguous" checklist item
4. **Common underspecified areas**:
   - User types and permissions
   - Data retention/deletion policies  
   - Performance targets and scale
   - Error handling behaviors
   - Integration requirements
   - Security/compliance needs

---

   ## Clarifications

   ### Session 2025-10-03

   - Q: Which Detekt offenses should be treated as blocking for this stabilization pass? → A: A (Block these atleast: TooManyFunctions, LongMethod, CyclomaticComplexMethod)
   - Q: Which secrets storage approach is mandated for this pass? → A: A (Jetpack Security / EncryptedSharedPreferences required)
   - Q: In the event of low-memory during local inference, should the app fall back to cloud processing or decline the request with an error? → A: B (Decline the request with a helpful error message)


## User Scenarios & Testing *(mandatory)*

### Primary User Story
As a maintainer preparing for Phase 4 (multimodal and advanced features), I need the codebase cleaned of critical TODOs, security issues, major static-analysis violations, and incomplete tests so that new features can be built on a stable, well-tested foundation.

### Acceptance Scenarios
1. **Given** the repository at branch `002-disclaimer-and-fixes`, **When** the repo is built and static analysis is run, **Then** there are no critical Detekt offenses (TooManyFunctions, LongMethod, CyclomaticComplexMethod, LongParameterList) above the thresholds defined in `config/detekt/detekt.yml` and the CI lint gate passes.
2. **Given** a local model package is downloaded by `ModelDownloadWorker`, **When** the download completes, **Then** the worker verifies the manifest and checksum and refuses corrupted downloads with a clear error and retry path.
3. **Given** API keys or provider configuration are stored, **When** the app runs, **Then** secrets are persisted only via an encrypted mechanism (e.g., Jetpack Security / EncryptedSharedPreferences or documented equivalent) and no plaintext creds remain in source or test fixtures.
4. **Given** the test suite is run (`./gradlew test` and relevant compose/android tests), **When** tests complete, **Then** previously-marked TODO tests for critical flows (offline persona flow, disclaimer dialog, model library flow, cloud fallback) are implemented or explicitly annotated with a technical debt ticket and a reproducible mock so CI can run deterministically.
5. **Given** the codebase contains large composables and god-classes, **When** refactoring is applied, **Then** each refactor preserves behavior, has unit or UI tests covering the primary happy path, and reduces method/function size below Detekt thresholds.

### Edge Cases
- Device offline during model download: worker queues and retries; user-visible error messages and a persisted retry state.
- Low-memory device during local inference: the runtime should gracefully decline the request with a helpful error and telemetry (opt-in) to prevent OOM crashes.
- Missing encryption key (first-run or key rotation): app should surface recovery instructions and avoid crashing.
- Flaky UI tests: identify flaky tests, isolate them with mocks and stable test data, or mark as flaky with linked issues.

## Requirements *(mandatory)*

### Functional Requirements
- **FR-001 (Static analysis & style)**: The codebase MUST be brought to a state where CI's ktlint and Detekt checks pass for the rules marked as blocking in `config/detekt/detekt.yml`. Specifically: no remaining offenses that exceed the configured thresholds for TooManyFunctions, LongMethod, CyclomaticComplexMethod, or LongParameterList for production code. All naming and format violations affecting readability should be fixed.
- **FR-002 (Critical TODOs)**: Implement or provide a documented and tested alternative for critical TODOs identified in `docs/inconsistencies.md` before Phase 4. This includes at minimum: local inference runtime placeholder in `MediaPipeLocalModelRuntime.kt`, model download checksum verification in `ModelDownloadWorker.kt`, and secure storage of provider/API keys (see FR-003).
- **FR-003 (Secrets & encryption)**: All API keys, provider credentials, and similar secrets MUST be stored encrypted at rest (Jetpack Security / EncryptedSharedPreferences or keystore-backed solution). No plaintext secrets in source, test fixtures, or CI logs. Provide migration and key-rotation notes if applicable.
- **FR-004 (Download integrity)**: Model downloads MUST be validated using an authentic manifest and checksum (e.g., SHA256) stored in the package or retrieved from a signed catalog. The download worker must verify integrity before installation and expose retry/backoff behavior on failure.
- **FR-005 (Tests & coverage)**: Critical user flows listed in `docs/inconsistencies.md` and `docs/todo-next.md` (offline persona flow, disclaimer flow, model library flow, cloud fallback) MUST have deterministic tests (unit or instrumented) that run in CI. Any remaining manual/integration tests must be documented with a tracking ticket.
- **FR-006 (Refactoring & modularity)**: Large composables and classes flagged (e.g., `NavigationScaffold`, `HomeScreen`, `UserProfileRepository`) MUST be refactored into smaller units with preserved behavior and covered by tests. Public APIs should remain stable or have migration notes.
- **FR-007 (Error handling & consistency)**: Standardize error handling across domain layers using sealed result types or a Result wrapper. No unchecked exceptions should bubble to the UI in normal error scenarios.
- **FR-008 (Dead code & cleanup)**: Remove or document dead/unused code (e.g., legacy onboarding placeholders, unused `savedStateHandle` instances) and remove hardcoded URLs or secrets. Each removal must include a short rationale in the commit message.
- **FR-009 (Documentation & tracking)**: Update `docs/inconsistencies.md` and `docs/todo-next.md` to reflect completed fixes and add explicit tracking issues for deferred items. Add a changelog entry for this stabilization pass.
- **FR-010 (Performance guardrails)**: For any runtime changes (local inference), measure and document median P95 response times on representative hardware; local response should target <2s per prior spec, or the spec must include a justification and fallback strategy.

*Ambiguities (need clarification):*
- **FR-A1**: Resolved — All four Detekt offenses (TooManyFunctions, LongMethod, CyclomaticComplexMethod, LongParameterList) are blocking for this pass.
- **FR-A2**: Resolved — Jetpack Security (EncryptedSharedPreferences) is required for secrets storage for this pass.

## Key Entities *(include if feature involves data)*
- **RepoMaintenanceTask**: { id, title, description, priority (critical/high/medium/low), status, linked-issue }
- **CodeQualityMetric**: { ruleId, filePath, severity, occurrences, configuredThreshold }
- **ModelPackage**: { id, version, manifestUrl, checksum, signedMetadata }
- **DownloadManifest**: { modelId, version, checksum (sha256), sizeBytes, signature }

***

### Review & Acceptance Checklist

#### Content Quality
- [ ] No implementation details that belong in design docs (code-level notes are allowed for developer acceptance tests only)
- [ ] Focused on value: enabling Phase 4 by stabilizing foundation
- [ ] All mandatory sections completed

#### Requirement Completeness
- [ ] No [NEEDS CLARIFICATION] markers remain (or each has a tracking issue)
- [ ] Requirements are testable and have acceptance criteria (build/lint/tests)
- [ ] Success criteria are measurable (Detekt/ktlint pass, tests green, critical TODOs resolved)
- [ ] Scope is bounded to fixes/preparation for Phase 4; feature additions deferred

#### Constitution Alignment
- [ ] UX and accessibility expectations noted where UI changes occur
- [ ] Performance guardrails included for local runtimes
- [ ] Data handling and consent: secrets encrypted and no PII leaked in logs

***

## Execution Status

- [x] User description parsed
- [x] Key concepts extracted
- [x] Ambiguities marked
- [x] User scenarios defined
- [x] Requirements generated
- [x] Entities identified
- [ ] Review checklist passed

Execution notes / next steps
- Create tracked issues for each critical TODO referenced in `docs/inconsistencies.md` and assign owners.
- Add a temporary CI job to run the Detekt/ktlint gating and report the current counts per rule to make progress visible.
- Implement quick wins first: replace plaintext secrets with EncryptedSharedPreferences, add checksum verification to `ModelDownloadWorker`, and remove trivial dead-code (unused properties) with small, well-tested commits.

Done: this spec was created by running `.specify/scripts/bash/create-new-feature.sh --json` which created branch `004-fixes-and-inconsistencies`. The spec file path is `/home/vijay/Personal/myGithub/nanoAI/specs/004-fixes-and-inconsistencies/spec.md`.
