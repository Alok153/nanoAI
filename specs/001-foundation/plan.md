
# Implementation Plan: Offline Multimodal nanoAI Assistant

**Branch**: `001-foundation` | **Date**: 2025-09-30 | **Spec**: `/specs/001-foundation/spec.md`
**Input**: Feature specification from `/specs/001-foundation/spec.md`

## Summary
Deliver a polished, offline-first Android multimodal AI assistant with comprehensive UI/UX, robust code quality, and thorough test coverage. The application provides offline chat with small LLMs via MediaPipe Generative (LiteRT), integrates OpenAI/Gemini and custom endpoints for cloud fallback, includes a model library with download/pause controls, supports multiple personas, and offers a first-launch disclaimer with data import/export capabilities. The implementation follows clean architecture with Kotlin-first principles, Material 3 design, automated quality gates, and meets coverage targets (ViewModel ≥75%, UI ≥65%, Data ≥70%).

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
app/
├── build.gradle.kts
├── proguard-rules.pro
├── schemas/                          # Room database schemas
└── src/
    ├── main/
    │   ├── AndroidManifest.xml
    │   ├── assets/                   # Model metadata, coverage dashboard
    │   ├── baseline-prof.txt         # Performance baseline profile
    │   ├── java/com/vjaykrsna/nanoai/
    │   │   ├── core/                 # Shared utilities and base classes
    │   │   │   ├── data/             # Database, preferences, network
    │   │   │   ├── domain/           # Use cases and business logic
    │   │   │   └── ui/               # Reusable Compose components
    │   │   ├── feature/              # Feature modules
    │   │   │   ├── chat/             # Chat interface and logic
    │   │   │   ├── library/          # Model management
    │   │   │   └── settings/         # Configuration and preferences
    │   │   └── security/             # Security utilities
    │   └── res/                      # Android resources
    ├── androidTest/java/com/vjaykrsna/nanoai/  # Instrumentation tests
    └── test/java/com/vjaykrsna/nanoai/         # JVM unit tests
```

**Structure Decision**: Single Android app module with clean architecture. Packages organized under `com.vjaykrsna.nanoai` with `core` for shared utilities and `feature` packages for domain-specific logic. Current structure supports the foundation implementation while enabling future modularization.

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

## Quality Gates & Standards
*See branches 002-005 for detailed requirements and implementation*

- **002-disclaimer-and-fixes**: CI gates, security, privacy compliance
- **003-UI-UX**: Material 3 design system, accessibility, responsive layouts
- **004-fixes-and-inconsistencies**: Code quality, error handling, runtime integrity
- **005-improve-test-coverage**: Coverage targets (ViewModel ≥75%, UI ≥65%, Data ≥70%)

**Aggregate CI Command**: `./gradlew ktlintFormat detekt lintDebug :app:testDebugUnitTest verifyCoverageThresholds`

## Implementation Status
*See `tasks.md` for complete task breakdown and current progress*

---
*Based on Constitution v1.0.0 - See `.specify/memory/constitution.md`*
