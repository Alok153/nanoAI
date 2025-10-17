
# Implementation Plan: Fixes and Inconsistencies Stabilization Pass

**Branch**: `004-fixes-and-inconsistencies` | **Date**: 2025-10-03 | **Spec**: `/home/vijay/Personal/myGithub/nanoAI/specs/004-fixes-and-inconsistencies/spec.md`
**Input**: Feature specification from `/home/vijay/Personal/myGithub/nanoAI/specs/004-fixes-and-inconsistencies/spec.md`

## Execution Flow (/plan command scope)
```
1. Load feature spec from Input path
   → If not found: ERROR "No feature spec at {path}"
2. Fill Technical Context (scan for NEEDS CLARIFICATION)
   → Detect Project Type from file system structure or context (web=frontend+backend, mobile=app+api)
   → Set Structure Decision based on project type
3. Fill the Constitution Check section based on the content of the constitution document.
4. Evaluate Constitution Check section below
   → If violations exist: Document in Complexity Tracking
   → If no justification possible: ERROR "Simplify approach first"
   → Update Progress Tracking: Initial Constitution Check
5. Execute Phase 0 → research.md
   → If NEEDS CLARIFICATION remain: ERROR "Resolve unknowns"
6. Execute Phase 1 → contracts, data-model.md, quickstart.md, agent-specific template file (e.g., `.github/copilot-instructions.md` for GitHub Copilot).
7. Re-evaluate Constitution Check section
   → If new violations: Refactor design, return to Phase 1
   → Update Progress Tracking: Post-Design Constitution Check
8. Plan Phase 2 → Describe task generation approach (DO NOT create tasks.md)
9. STOP - Ready for /tasks command
```

**IMPORTANT**: The /plan command STOPS at step 7. Phases 2-4 are executed by other commands:
- Phase 2: /tasks command creates tasks.md
- Phase 3-4: Implementation execution (manual or via tools)

## Summary
Stabilize the Android codebase before Phase 4 by eliminating critical static-analysis violations, securing secrets, validating model downloads, completing high-priority tests, and refactoring oversized components without regressing UX or performance. The approach prioritizes Detekt blockers, WorkManager-based download integrity, Jetpack Security–backed credential storage, and deterministic test coverage for offline, disclaimer, model library, and cloud fallback flows.

## Technical Context
**Language/Version**: Kotlin 1.9.x (JDK 17 baseline)  
**Primary Dependencies**: Jetpack Compose Material 3, Hilt, WorkManager, Room (KSP), DataStore, Retrofit + Kotlin Serialization, OkHttp, MediaPipe Tasks GenAI LiteRT, Coil, Kotlin Coroutines  
**Storage**: Room (SQLite) for structured data, DataStore Preferences/Proto for lightweight state, EncryptedSharedPreferences (Jetpack Security) for secrets  
**Testing**: JUnit4, Kotlin test, Robolectric, MockK, Turbine, Compose UI Test JUnit4, Espresso, Macrobenchmark harness  
**Target Platform**: Android (minSdk 31, targetSdk 36) running on Pixel-class hardware and large-screen emulators  
**Project Type**: Mobile (Android app module + macrobenchmark module)  
**Performance Goals**: Cold start ≤ 1.5s, Compose interactions ≤ 100ms feedback/≤ 500ms content updates, local inference ≤ 2s P95, WorkManager download retries capped at 3 with exponential backoff  
**Constraints**: Offline-first behavior, encrypted secrets at rest, no Detekt blockers (TooManyFunctions, LongMethod, CyclomaticComplexMethod, LongParameterList), base APK < 100MB, graceful low-memory handling (decline with helpful error)  
**Scale/Scope**: Single Android application with ~50 Compose screens, 1 macrobenchmark module, and 1k+ Detekt findings targeted for reduction to 0 blockers

## Constitution Check
*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- **Kotlin-First Clean Architecture**: Refactors split god classes (`NavigationScaffold`, `UserProfileRepository`, `LayoutSnapshotDao`) into focused composables/use cases, maintain Kotlin-only layers, and capture DI adjustments (Hilt modules) to enforce single responsibility.
- **Polished Material UX**: Compose decompositions retain Material 3 components, add accessibility semantics, and validate latency (<100ms touch feedback, <500ms content updates) through Compose testing plus macrobenchmark smoke checks.
- **Resilient Performance & Offline Readiness**: Model downloads stay in WorkManager with exponential backoff, cached manifests, and offline queue resiliency; low-memory inference paths return structured decline responses while logging telemetry.
- **Automated Quality Gates**: Expand Detekt + ktlint CI gates to cover targeted rules, generate new deterministic unit/UI tests for offline persona, disclaimer dialog, model library, and cloud fallback flows, and document gating in quickstart.
- **Privacy & Data Stewardship**: Migrate secrets to EncryptedSharedPreferences, scrub plaintext fixtures, document migration/rotation handling, and verify TLS/cert pinning assumptions for Retrofit service clients.
- **AI Inference Integrity**: Add SHA-256 manifest verification and signed metadata checks to `ModelDownloadWorker`, validate MediaPipe runtime wiring, and ensure low-memory fallbacks decline requests with actionable messaging.
- **Up-to-Date Documentation and Best Practices**: Reference the latest Jetpack Security, WorkManager download, and MediaPipe integrity guidelines (pulled via Context7 MCP) to guide migration steps and note citations in research.md.

**Initial Review Result**: PASS — No deviations required prior to Phase 0; all principles have actionable coverage.
**Post-Design Review Result**: PASS — Phase 1 artifacts enforce the same guarantees with no additional exceptions.

## Project Structure

### Documentation (this feature)
```
specs/004-fixes-and-inconsistencies/
├── plan.md              # This file (/plan command output)
├── research.md          # Phase 0 output (/plan command)
├── data-model.md        # Phase 1 output (/plan command)
├── quickstart.md        # Phase 1 output (/plan command)
├── contracts/           # Phase 1 output (/plan command)
└── tasks.md             # Phase 2 output (/tasks command)
└── inconsistencies.md
├── todo-next.md
└── general-notes.md
```

### Source Code (repository root)
```
android/
└── app/
      ├── build.gradle.kts
      ├── src/
      │   ├── main/
      │   │   ├── AndroidManifest.xml
      │   │   ├── java/com/vjaykrsna/nanoai/
      │   │   └── res/
      │   ├── androidTest/java/com/vjaykrsna/nanoai/
      │   └── test/java/com/vjaykrsna/nanoai/
      └── schemas/

macrobenchmark/
└── src/
      ├── main/java/com/vjaykrsna/nanoai/macrobenchmark/
      └── androidTest/java/com/vjaykrsna/nanoai/macrobenchmark/

config/detekt/
├── detekt.yml
└── baseline.xml
```

**Structure Decision**: Mobile-first Android application with supporting macrobenchmark module; stabilization work touches `app/` source (UI, domain, data), WorkManager workers, encryption helpers, and accompanying docs/config.

## Phase 0: Outline & Research
1. **Extract unknowns from Technical Context** above:
   - Migration path for existing plaintext secrets to EncryptedSharedPreferences without blocking app start and with key rotation guidance.
   - Integrity validation workflow for `ModelDownloadWorker` (manifest schema, checksum storage, signature verification, retry policy).
   - Standardized error envelope/sealed Result usage across domain layers to replace ad-hoc exceptions.
   - Compose decomposition strategy for oversized composables while preserving Material 3 semantics, accessibility, and preview coverage.
   - Deterministic testing patterns for offline persona, disclaimer dialog, model library, and cloud fallback flows using Compose + Robolectric/Instrumentation.

2. **Generate and dispatch research agents**:
   ```
   Research Jetpack Security migration + rotation strategy for encrypting existing DataStore/Room secrets.
   Research WorkManager model download integrity (manifest, checksum, signature, retry) with MediaPipe task packages.
   Research Kotlin sealed error/result patterns for multi-layer error handling in Android clean architecture.
   Research Material 3 Compose decomposition and accessibility patterns for large screens and drawers.
   Research deterministic Compose UI and WorkManager testing approaches for offline/inference flows.
   ```

3. **Consolidate findings** in `research.md` using format:
   - Decision: [what was chosen]
   - Rationale: [why chosen]
   - Alternatives considered: [what else evaluated]
   - References: [Context7 MCP doc URLs or official resources]

**Output**: `research.md` capturing final recommendations; no unresolved `NEEDS CLARIFICATION` remain before Phase 1.

## Phase 1: Design & Contracts
*Prerequisites: `research.md` complete*

1. **Extract entities from feature spec** → `data-model.md`:
   - Document `RepoMaintenanceTask`, `CodeQualityMetric`, `ModelPackage`, `DownloadManifest`, `SecretCredential`, and `ErrorEnvelope` structures with validation rules and relationships.
   - Capture state transitions for maintenance tasks (e.g., identified → in-progress → verified) and download lifecycle (queued → downloading → verified → installed).

2. **Generate operational contracts** from functional requirements:
   - Define `contracts/openapi.yaml` with endpoints for catalog/manifest retrieval, checksum verification, and credential management (e.g., `GET /catalog/models/{id}/manifest`, `POST /catalog/models/{id}/verify`, `POST /credentials/provider/{id}`).
   - Include error models aligned with the sealed Result strategy.

3. **Generate contract tests** from contracts:
   - Add placeholder failing tests (e.g., `contracts/tests/ModelManifestContractTest.kt`, `contracts/tests/CredentialEncryptionContractTest.kt`) that assert schema expectations using MockWebServer + Truth.
   - Highlight TODO markers to ensure they are implemented before merging to main.

4. **Extract test scenarios** from user stories:
   - Populate `quickstart.md` with deterministic walkthroughs for: Detekt/ktlint gate validation, encrypted secrets migration, model download verification (success + corrupt case), and offline/cloud fallback tests.
   - Include automation commands for unit + instrumentation suites and macrobenchmark smoke.

5. **Update agent file incrementally** (O(1) operation):
   - Run `.specify/scripts/bash/update-agent-context.sh copilot`.
   - Append only new context (e.g., Jetpack Security migration, WorkManager checksum pattern) inside `.github/copilot-instructions.md` between manual markers.
   - Maintain ≤150 lines by pruning obsolete entries if required.

**Output**: `data-model.md`, `contracts/openapi.yaml`, `contracts/contract-tests.md` (or similar placeholders), `contracts/tests/*.kt` placeholders, `quickstart.md`, and updated `.github/copilot-instructions.md`.

## Phase 2: Task Planning Approach
*This section describes what the /tasks command will do - DO NOT execute during /plan*

**Task Generation Strategy**:
- Load `.specify/templates/tasks-template.md` as base.
- Derive tasks from Phase 1 artifacts:
   - Entities → refactor/data migration tasks (RepoMaintenanceTask backlog, ModelPackage integrity updates, SecretCredential storage).
   - Contracts → API/worker integration tasks (manifest retrieval, checksum validation, credential update flows).
   - Quickstart scenarios → unit/UI/integration test tasks (Detekt gate, encrypted secret migration, model download happy/sad path, offline/cloud fallback, documentation updates).
   - Documentation + tooling tasks → update docs, remove resolved TODOs, add changelog entry, adjust CI pipelines.

**Ordering Strategy**:
- TDD-first: create failing contract + unit/UI tests before implementation.
- Address secrets & download integrity before refactoring large composables to unblock security gates.
- Mark [P] for parallelizable refactors (e.g., splitting multiple DAOs) once dependencies are isolated.

**Estimated Output**: 28–32 ordered tasks in `tasks.md` with dependency annotations and [P] flags for safe parallel work.

**IMPORTANT**: This phase is executed by the /tasks command, NOT by /plan

## Phase 3+: Future Implementation
*These phases are beyond the scope of the /plan command*

**Phase 3**: Task execution (/tasks command creates tasks.md)  
**Phase 4**: Implementation (execute tasks.md following constitutional principles)  
**Phase 5**: Validation (run tests, execute quickstart.md, performance validation)

## Complexity Tracking
*Fill ONLY if Constitution Check has violations that must be justified*

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| [e.g., 4th project] | [current need] | [why 3 projects insufficient] |
| [e.g., Repository pattern] | [specific problem] | [why direct DB access insufficient] |


## Progress Tracking
*This checklist is updated during execution flow*

**Phase Status**:
- [x] Phase 0: Research complete (/plan command)
- [x] Phase 1: Design complete (/plan command)
- [ ] Phase 2: Task planning complete (/plan command - describe approach only)
- [ ] Phase 3: Tasks generated (/tasks command)
- [ ] Phase 4: Implementation complete
- [ ] Phase 5: Validation passed

**Gate Status**:
- [x] Initial Constitution Check: PASS
- [x] Post-Design Constitution Check: PASS
- [x] All NEEDS CLARIFICATION resolved
- [x] Complexity deviations documented (None required)

---
*Based on Constitution v1.2.0 - See `.specify/memory/constitution.md`*
