# Tasks: Improve Test Coverage for nanoAI

**Input**: Design documents from `/specs/005-improve-test-coverage/`
**Prerequisites**: `plan.md`, `research.md`, `data-model.md`, `contracts/`, `quickstart.md`

## Execution Flow (main)
```
1. Load plan.md for coverage thresholds, target layers, and JaCoCo rollout strategy.
2. Parse research.md to lock JaCoCo aggregation, Compose/UI testing decisions, and offline simulation tactics.
3. Extract entities (CoverageSummary, TrendPoint, CatalogEntry, RiskRegisterItem, TestLayer, CoverageMetric) from data-model.md to drive model and repository tasks.
4. Map contracts/coverage-report.schema.json into contract tests and schema validation checkpoints.
5. Translate quickstart.md scenarios into ViewModel, Compose UI, DAO, and coverage dashboard integration tests.
6. Emit dependency-ordered tasks (Setup → Tests → Core Models/Services → Integration → Polish) with [P] markers for parallel-safe work.
```

## Format: `[ID] [P?] Description`
- **[P]**: Can run in parallel (independent files, no blocking dependencies)
- Include fully-qualified paths in every task description

## Path Conventions
- Source: `app/src/main/java/com/vjaykrsna/nanoai/`
- Unit tests: `app/src/test/java/com/vjaykrsna/nanoai/`
- Instrumentation: `app/src/androidTest/java/com/vjaykrsna/nanoai/`
- Scripts: `scripts/coverage/`
- Documentation artifacts: `docs/coverage/`

# Phase 3.1 Setup
- [ ] T001 Align `gradle/libs.versions.toml` with JaCoCo 0.8.11, kotlinx-coroutines-test, Turbine, MockK, networknt JSON schema, Compose UI test artifacts, and remove duplicate `androidx.security.crypto` entry.
- [ ] T002 Update `app/build.gradle.kts` to apply the `jacoco` plugin, enable unit + instrumentation report merging, and register `jacocoFullReport` (XML + HTML) plus `verifyCoverageThresholds` tasks using layer targets (VM 75, UI 65, Data 70).
- [ ] T003 Update root `build.gradle.kts` to add shared `jacocoMerge`/`jacocoCoverageData` wiring, ensure `verifyCoverageThresholds` runs after `jacocoFullReport`, and expose coverage tasks in the help group.
- [ ] T004 Create `scripts/coverage/generate-summary.kt` (Kotlin CLI) with Gradle entry + fixtures to transform `app/build/reports/jacoco/full/jacocoFullReport.xml` into `build/coverage/summary.md`, documenting usage in `scripts/coverage/README.md`.
- [ ] T005 Harden `.github/workflows/android-ci.yml` to run `testDebugUnitTest`, `connectedDebugAndroidTest`, `jacocoFullReport`, `verifyCoverageThresholds`, and upload HTML/XML/markdown artifacts for stakeholders.

## Phase 3.2 Tests First (TDD) ⚠️ Complete before implementation
- [ ] T006 [P] Add failing contract test `app/src/test/java/com/vjaykrsna/nanoai/coverage/contracts/CoverageReportContractTest.kt` validating generated JSON against `specs/005-improve-test-coverage/contracts/coverage-report.schema.json` with networknt JSON schema.
- [ ] T007 [P] Create unit test `app/src/test/java/com/vjaykrsna/nanoai/coverage/data/CoverageReportParserTest.kt` covering XML merge, trend calculation, and error paths for missing layers.
- [ ] T008 [P] Create unit test `app/src/test/java/com/vjaykrsna/nanoai/coverage/domain/CoverageThresholdVerifierTest.kt` asserting pass/fail messaging per TestLayer and regression on threshold downgrades.
- [ ] T009 [P] Create use case test `app/src/test/java/com/vjaykrsna/nanoai/coverage/domain/CoverageSummaryUseCaseTest.kt` verifying repository integration, risk linking, and markdown snippet generation triggers.
- [ ] T010 [P] Create renderer test `app/src/test/java/com/vjaykrsna/nanoai/coverage/reporting/CoverageSummaryMarkdownRendererTest.kt` ensuring summary formatting, badges, and quickstart-friendly callouts.
- [ ] T011 [P] Create risk register writer test `app/src/test/java/com/vjaykrsna/nanoai/coverage/reporting/RiskRegisterWriterTest.kt` covering severity sorting, mitigation placeholders, and file idempotency.
- [ ] T012 [P] Add repository unit test `app/src/test/java/com/vjaykrsna/nanoai/core/data/repository/ConversationRepositoryImplTest.kt` for archive/delete flows, persona updates, and Flow emission accuracy.
- [ ] T013 [P] Add ViewModel unit test `app/src/test/java/com/vjaykrsna/nanoai/feature/chat/presentation/ChatViewModelTest.kt` covering send success/failure, persona switching, and offline deferral events.
- [ ] T014 [P] Add DAO regression test `app/src/test/java/com/vjaykrsna/nanoai/core/data/db/ChatThreadDaoCoverageTest.kt` verifying archive/unarchive toggles, persona timestamp updates, and active-thread counts.
- [ ] T015 [P] Add DAO regression test `app/src/test/java/com/vjaykrsna/nanoai/core/data/db/MessageDaoCoverageTest.kt` covering error filtering, average latency queries, and cascade deletions.
- [ ] T016 [P] Add Compose instrumentation test `app/src/androidTest/java/com/vjaykrsna/nanoai/feature/chat/ui/ChatScreenContractTest.kt` ensuring persona dropdown semantics, message list accessibility, and error snackbar behavior.
- [ ] T017 [P] Add Compose instrumentation test `app/src/androidTest/java/com/vjaykrsna/nanoai/feature/chat/ui/MessageInputAccessibilityTest.kt` verifying focus order, talkback labels, and send gating during loading.
- [ ] T018 [P] Add Compose instrumentation test `app/src/androidTest/java/com/vjaykrsna/nanoai/coverage/ui/CoverageDashboardOfflineScenarioTest.kt` simulating coverage summary fetch, offline fallback messaging, and quick action prompts.

## Phase 3.3 Core Implementation (after tests are red)
- [ ] T019 [P] Implement `TestLayer` enum in `app/src/main/java/com/vjaykrsna/nanoai/coverage/model/TestLayer.kt` reflecting schema values and helper parsing.
- [ ] T020 [P] Implement `CoverageMetric` data class in `.../coverage/model/CoverageMetric.kt` with derived status helpers and validation.
- [ ] T021 [P] Implement `RiskRegisterItem` data class in `.../coverage/model/RiskRegisterItem.kt` enforcing severity/target build invariants.
- [ ] T022 [P] Implement `TestSuiteCatalogEntry` data class in `.../coverage/model/TestSuiteCatalogEntry.kt` with risk tag validation utilities.
- [ ] T023 [P] Implement `CoverageTrendPoint` data class in `.../coverage/model/CoverageTrendPoint.kt` ensuring monotonic timestamps and layer alignment.
- [ ] T024 [P] Implement `CoverageSummary` aggregate in `.../coverage/model/CoverageSummary.kt` linking metrics, thresholds, trends, and risk references.
- [ ] T025 Implement `CoverageReportParser` in `app/src/main/java/com/vjaykrsna/nanoai/coverage/data/CoverageReportParser.kt` to load JaCoCo XML, validate against contract JSON, and emit domain models.
- [ ] T026 Implement `CoverageThresholdVerifier` in `app/src/main/java/com/vjaykrsna/nanoai/coverage/domain/CoverageThresholdVerifier.kt` producing layer-specific pass/fail diagnostics.
- [ ] T027 Implement `CoverageSummaryMarkdownRenderer` in `app/src/main/java/com/vjaykrsna/nanoai/coverage/reporting/CoverageSummaryMarkdownRenderer.kt` with table, badge, and trend sections.
- [ ] T028 Implement `RiskRegisterWriter` in `app/src/main/java/com/vjaykrsna/nanoai/coverage/reporting/RiskRegisterWriter.kt` writing `docs/coverage/risk-register.md` with severity grouping and owner slots.
- [ ] T029 Implement `CoverageRepositoryImpl` in `app/src/main/java/com/vjaykrsna/nanoai/coverage/data/CoverageRepositoryImpl.kt` merging parser output, cached risk register, and catalog fixtures.
- [ ] T030 Implement `CoverageSummaryUseCase` in `app/src/main/java/com/vjaykrsna/nanoai/coverage/domain/CoverageSummaryUseCase.kt` orchestrating repository fetch, threshold verification, markdown rendering, and risk updates.
- [ ] T031 Implement `CoverageDashboardViewModel` in `app/src/main/java/com/vjaykrsna/nanoai/coverage/presentation/CoverageDashboardViewModel.kt` exposing layered metrics, trend chips, and offline status.
- [ ] T032 Build Compose UI `CoverageDashboardScreen` in `app/src/main/java/com/vjaykrsna/nanoai/coverage/ui/CoverageDashboardScreen.kt` rendering metrics grid, risk list, and retry workflow.
- [ ] T033 Update `app/src/main/java/com/vjaykrsna/nanoai/feature/chat/presentation/ChatViewModel.kt` to surface `ChatUiState`, injectable dispatcher, and offline deferral hooks required by new tests.
- [ ] T034 Add shared fixtures `app/src/test/java/com/vjaykrsna/nanoai/testing/coverage/CoverageFixtures.kt` supplying JaCoCo XML, schema-compliant JSON, and risk catalog builders.

## Phase 3.4 Integration
- [ ] T035 Register DI bindings in `app/src/main/java/com/vjaykrsna/nanoai/core/di/CoverageModule.kt` wiring parser, repository, threshold verifier, renderer, and ViewModel.
- [ ] T036 Update navigation (`app/src/main/java/com/vjaykrsna/nanoai/ui/navigation/Screen.kt`, `NavigationScaffold.kt`) to add coverage dashboard destination, drawer entry, and routing analytics.
- [ ] T037 Update sidebar (`app/src/main/java/com/vjaykrsna/nanoai/ui/sidebar/SidebarContent.kt`, `SidebarViewModel.kt`) to surface coverage status badge and open dashboard actions.
- [ ] T038 Extend Gradle/CI wiring (`build.gradle.kts`, `scripts/coverage/README.md`) to copy `build/coverage/summary.md` and `docs/coverage/risk-register.md` into `docs/coverage/` for publishing.

## Phase 3.5 Polish
- [ ] T039 [P] Add failure-path unit tests `app/src/test/java/com/vjaykrsna/nanoai/coverage/domain/CoverageSummaryUseCaseFailureTest.kt` covering schema violations, missing artifacts, and IO exceptions.
- [ ] T040 [P] Add Compose instrumentation `app/src/androidTest/java/com/vjaykrsna/nanoai/coverage/ui/CoverageAccessibilityTest.kt` validating TalkBack order, large text scaling, and semantics for summary cards.
- [ ] T041 [P] Refresh documentation (`specs/005-improve-test-coverage/quickstart.md`, `docs/ARCHITECTURE.md`, `docs/coverage/risk-register.md`) with coverage workflow, threshold gates, and stakeholder handoff steps.
- [ ] T042 [P] Capture execution evidence in `specs/005-improve-test-coverage/validation/coverage-validation.md` (create directory if missing) summarizing test runs, coverage %, and uploaded artifacts.

## Dependencies
- T001–T005 must finish before any test authoring (T006–T018) to guarantee tooling and CI readiness.
- Contract and domain tests (T006–T011) should land before repository/UI tests (T012–T018) to drive implementation through red → green.
- Entity model tasks (T019–T024) rely conceptually on T006–T011 and should precede parser/repository work (T025–T029); `TestLayer` (T019) must complete before other model tasks despite the [P] marker.
- Use case/UI tasks (T030–T033) depend on repository/model completion (T019–T029) and unblock fixtures (T034) plus integration wiring (T035–T038).
- Polish tasks (T039–T042) execute only after integration passes and coverage artifacts are generated.

## Parallel Execution Examples
```
# After completing setup tasks, run contract + parser tests together
task start T006
task start T007
task start T008
task wait T006 T007 T008

# Once tests are red, parallelize entity model implementations
task start T019
task start T020
task start T021
task start T022
task start T023
task start T024
task wait T019 T020 T021 T022 T023 T024
```

## Notes
- Ensure every test introduced in Phase 3.2 fails before implementing the corresponding production code (TDD discipline).
- Maintain constitution goals: Material accessibility, offline resilience, privacy (no PII in coverage artifacts), and automated quality gates.
- Document any deviations or threshold exceptions directly in `tasks.md` annotations or follow-up notes for review.
