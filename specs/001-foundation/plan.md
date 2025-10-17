
# Implementation Plan: Offline Multimodal nanoAI Assistant

**Branch**: `001-foundation` | **Date**: 2025-09-30 | **Spec**: `/specs/001-foundation/spec.md`
**Input**: Feature specification from `/specs/001-foundation/spec.md`

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
Deliver an offline-first Android chat assistant with Compose UI, sidebar history, and persona toggles. The MVP runs small LLMs locally via MediaPipe Generative (LiteRT), integrates OpenAI/Gemini and custom endpoints for cloud fallback, and ships a model library with download/pause controls. Research establishes a runtime-agnostic abstraction so TensorFlow Lite, MLC LLM, and ONNX Runtime Mobile can be added iteratively.

## Technical Context
**Language/Version**: Kotlin 1.9.x (JDK 17 baseline)  
**Primary Dependencies**: Jetpack Compose Material 3, Hilt, WorkManager, Room, DataStore, Retrofit + Kotlin Serialization, OkHttp, MediaPipe Generative (LiteRT), Coil, Kotlin Coroutines  
**Storage**: Room (SQLite) for chats/models/personas; EncryptedSharedPreferences/DataStore for credentials and privacy preferences  
**Testing**: JUnit 5, Turbine/Coroutines test, Espresso & Compose UI tests, Macrobenchmark + Baseline Profile, Robolectric for unit UI coverage  
**Target Platform**: Android 13+ (compileSdk 36, minSdk 31); reference devices Pixel 7 / Pixel 4a  
**Project Type**: Mobile (single app module with feature packages; future split into `core`, `feature/chat`, `feature/library`)  
**Performance Goals**: Cold start <1.5s, local response <2s median, dropped frames <5%, queue flush under 500ms  
**Constraints**: Offline-first delivery, models ≤3 GB, default single active inference, no telemetry, personas decoupled from models, export archives unencrypted with warning  
**Cloud Fallback Note**: Gemini Android SDK is deprecated; we will call Gemini 2.x endpoints through the unified Firebase/Vertex AI path using Retrofit until an official replacement ships
**Scale/Scope**: Power-user audience; support 100+ threads, 10k messages total, model catalog ≤20 entries in v1  

## Constitution Check
*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- **Kotlin-First Clean Architecture**: MVVM + use-case layers enforced; all modules Kotlin. Repositories abstract runtimes (MediaPipe/Retrofit) with coroutine suspend APIs.
- **Delightful Material UX**: Compose Material 3, dynamic color support, TalkBack labels, 100ms tactile feedback requirement logged. Sidebar uses Navigation Drawer; offline/cloud status chips meet UX goals.
- **Resilient Performance & Offline Readiness**: WorkManager handles downloads, coroutine dispatchers prevent main-thread blocking, offline cache with Room ensures chat continuity. Performance budgets recorded in Technical Context.
- **Automated Quality Gates**: CI runs ktlint, Detekt, Android Lint, unit + instrumentation suites, macrobenchmarks on release branches. Constitution checklist added to PR template.
- **Privacy & Data Stewardship**: Credentials stored locally via EncryptedSharedPreferences; no telemetry; export warns about unencrypted bundle; consent flow logged in PrivacyPreference table.

## Project Structure

### Documentation (this feature)
```
specs/[###-feature]/
├── plan.md              # This file (/plan command output)
├── research.md          # Phase 0 output (/plan command)
├── data-model.md        # Phase 1 output (/plan command)
├── quickstart.md        # Phase 1 output (/plan command)
├── contracts/           # Phase 1 output (/plan command)
└── tasks.md             # Phase 2 output (/tasks command - NOT created by /plan)
```

### Source Code (repository root)
```
android/
└── app/
      ├── build.gradle.kts
      └── src/
            ├── main/
            │   ├── AndroidManifest.xml
            │   ├── java/com/vjaykrsna/nanoai/
            │   │   ├── core/
            │   │   │   ├── data/
            │   │   │   ├── domain/
            │   │   │   └── ui/
            │   │   ├── feature/chat/
            │   │   ├── feature/library/
            │   │   └── feature/settings/
            │   └── res/
            ├── androidTest/java/com/vjaykrsna/nanoai/
            └── test/java/com/vjaykrsna/nanoai/
```

**Structure Decision**: Single Android app module today. We layer packages under `com.vjaykrsna.nanoai` for `core` shared utilities and Compose UI, with feature packages (`feature.chat`, `feature.library`, `feature.settings`). Future splits into dedicated Gradle modules (`core`, `feature-chat`, `feature-library`) will pivot from this baseline without changing public APIs.

## Phase 0: Outline & Research
1. **Extract unknowns from Technical Context** above:
   - For each NEEDS CLARIFICATION → research task
   - For each dependency → best practices task
   - For each integration → patterns task

2. **Generate and dispatch research agents**:
   ```
   For each unknown in Technical Context:
     Task: "Research {unknown} for {feature context}"
   For each technology choice:
     Task: "Find best practices for {tech} in {domain}"
   ```

3. **Consolidate findings** in `research.md` using format:
   - Decision: [what was chosen]
   - Rationale: [why chosen]
   - Alternatives considered: [what else evaluated]

**Output**: research.md with all NEEDS CLARIFICATION resolved

## Phase 1: Design & Contracts
*Prerequisites: research.md complete*

1. **Extract entities from feature spec** → `data-model.md`:
   - Entity name, fields, relationships
   - Validation rules from requirements
   - State transitions if applicable

2. **Generate API contracts** from functional requirements:
   - For each user action → endpoint
   - Use standard REST/GraphQL patterns
   - Output OpenAPI/GraphQL schema to `/contracts/`

3. **Generate contract tests** from contracts:
   - One test file per endpoint
   - Assert request/response schemas
   - Tests must fail (no implementation yet)

4. **Extract test scenarios** from user stories:
   - Each story → integration test scenario
   - Quickstart test = story validation steps

5. **Update agent file incrementally** (O(1) operation):
   - Run `.specify/scripts/bash/update-agent-context.sh copilot`
     **IMPORTANT**: Execute it exactly as specified above. Do not add or remove any arguments.
   - If exists: Add only NEW tech from current plan
   - Preserve manual additions between markers
   - Update recent changes (keep last 3)
   - Keep under 150 lines for token efficiency
   - Output to repository root

**Output**: data-model.md, /contracts/*, failing tests, quickstart.md, agent-specific file

## Phase 2: Task Planning Approach
*This section describes what the /tasks command will do - DO NOT execute during /plan*

**Task Generation Strategy**:
- Load `.specify/templates/tasks-template.md` as base
- Generate tasks from Phase 1 design docs (contracts, data model, quickstart)
- Each contract → contract test task [P]
- Each entity → model creation task [P] 
- Each user story → integration test task
- Implementation tasks to make tests pass
- Feature-specific focus areas:
   - Runtime abstraction: `LocalModelRuntime`, `CloudGatewayClient`, and orchestration service wiring.
   - Offline UX: chat persistence, sync workers, model download manager with pause/resume logic.
   - Persona system: CRUD UI/viewmodels, default persona bootstrapping, privacy preference storage.
   - Export/import: background exporter, safety warnings, checksum validation, instrumentation tests.
   - Sidebar/navigation: Compose destinations, Navigation Drawer accessibility, badge counts.

**Ordering Strategy**:
- TDD order: Tests before implementation 
- Dependency order: Models before services before UI
- Mark [P] for parallel execution (independent files)

**Estimated Output**: 25-30 numbered, ordered tasks in tasks.md

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
- [x] Phase 2: Task planning complete (/plan command - describe approach only)
- [ ] Phase 3: Tasks generated (/tasks command)
- [ ] Phase 4: Implementation complete
- [ ] Phase 5: Validation passed

**Gate Status**:
- [x] Initial Constitution Check: PASS
- [x] Post-Design Constitution Check: PASS
- [x] All NEEDS CLARIFICATION resolved
- [ ] Complexity deviations documented

---
*Based on Constitution v1.0.0 - See `.specify/memory/constitution.md`*
