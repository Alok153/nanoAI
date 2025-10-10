# Tasks: Improve Test Coverage for nanoAI

**Input**: Design documents from `/specs/005-improve-test-coverage/`
**Prerequisites**: plan.md (required), research.md, data-model.md, contracts/

## Task List
- [X] T001 Configure JaCoCo merge & threshold tasks in `app/build.gradle.kts` (plugins, report variants, CI-friendly properties).
- [X] T002 Create coverage tooling scripts under `scripts/coverage/` (merge helper, markdown summary) and register them in Gradle.
- [X] T003 Update `.github/workflows/android-ci.yml` to run coverage suite, publish HTML/XML artifacts, and fail on threshold breaches.
- T004 [P] Add contract test `app/src/test/java/com/vjaykrsna/nanoai/coverage/CoverageReportContractTest.kt` validating `contracts/coverage-report.schema.json` against sample payloads.
- T005 [P] Add unit tests `app/src/test/java/com/vjaykrsna/nanoai/coverage/CoverageSummaryTest.kt` covering layer aggregation, threshold comparison, and trend deltas.
- T006 [P] Add unit tests `app/src/test/java/com/vjaykrsna/nanoai/coverage/RiskRegisterCoordinatorTest.kt` asserting catalog-risk relationships and severity rules.
- T007 [P] Add unit tests `app/src/test/java/com/vjaykrsna/nanoai/auth/HuggingFaceAuthCoordinatorTest.kt` validating OAuth state transitions and repository interactions.
- T008 [P] Add Compose instrumentation suite `app/src/androidTest/java/com/vjaykrsna/nanoai/coverage/ui/CoverageDashboardTest.kt` exercising quickstart scenarios and accessibility semantics.
- T009 [P] Add Gradle functional tests `app/src/test/java/com/vjaykrsna/nanoai/coverage/VerifyCoverageThresholdsTaskTest.kt` ensuring `verifyCoverageThresholds` fails below targets.
- T010 [P] Implement `CoverageSummary` model in `app/src/main/java/com/vjaykrsna/nanoai/coverage/model/CoverageSummary.kt` with derived trend calculations.
- T011 [P] Implement `CoverageTrendPoint` model in `app/src/main/java/com/vjaykrsna/nanoai/coverage/model/CoverageTrendPoint.kt` enforcing monotonic timestamps.
- T012 [P] Implement `TestSuiteCatalogEntry` model in `app/src/main/java/com/vjaykrsna/nanoai/coverage/model/TestSuiteCatalogEntry.kt` capturing ownership and risk tags.
- T013 [P] Implement `RiskRegisterItem` model in `app/src/main/java/com/vjaykrsna/nanoai/coverage/model/RiskRegisterItem.kt` with validation for severity and targets.
- T014 [P] Implement `TestLayer` enum in `app/src/main/java/com/vjaykrsna/nanoai/coverage/model/TestLayer.kt` shared across coverage artifacts.
- T015 [P] Implement `CoverageMetric` value object in `app/src/main/java/com/vjaykrsna/nanoai/coverage/model/CoverageMetric.kt` deriving status from coverage vs threshold.
- T016 [P] Implement `HuggingFaceAuthCoordinator` state machine in `app/src/main/java/com/vjaykrsna/nanoai/auth/HuggingFaceAuthCoordinator.kt` with repository hooks.
- T017 [P] Implement `RefreshModelCatalogUseCase` orchestration in `app/src/main/java/com/vjaykrsna/nanoai/modelcatalog/domain/RefreshModelCatalogUseCase.kt` using fake-friendly collaborators.
- T018 [P] Implement `ModelCatalogRepository` in `app/src/main/java/com/vjaykrsna/nanoai/modelcatalog/data/ModelCatalogRepository.kt` bridging local/remote sources.
- T019 Build coverage report generator `app/src/main/java/com/vjaykrsna/nanoai/coverage/domain/CoverageReportGenerator.kt` producing schema-compliant summaries.
- T020 Implement Compose UI `app/src/main/java/com/vjaykrsna/nanoai/coverage/ui/CoverageDashboardScreen.kt` rendering layer metrics, trends, and risk register chips.
- T021 Extend Gradle wiring in `app/build.gradle.kts` to expose `verifyCoverageThresholds` with markdown summary outputs consumed by scripts.
- T022 Wire DI bindings in `app/src/main/java/com/vjaykrsna/nanoai/coverage/di/CoverageModule.kt` for models, repository, and generator.
- T023 Integrate coverage publishing pipeline by updating `scripts/coverage/publish-summary.sh` and `docs/coverage/risk-register.md` generation flow.
- T024 [P] Update documentation (`specs/005-improve-test-coverage/quickstart.md`, `docs/coverage/risk-register.md`, `docs/coverage/summary.md`) with new commands and reporting expectations.
- T025 [P] Expand negative-path tests (`app/src/test/java/com/vjaykrsna/nanoai/coverage/CoverageFailureScenariosTest.kt`, `app/src/androidTest/java/com/vjaykrsna/nanoai/coverage/ui/CoverageDashboardAccessibilityTest.kt`).
- T026 [P] Execute end-to-end validation from quickstart (Gradle tasks + emulator run) and capture artefact checklist in `specs/005-improve-test-coverage/todo-next.md`.
- T027 [P] Add unit tests for critical ViewModel state transitions (e.g., HomeScreenViewModel, ChatViewModel) covering happy path, error, and loading states as referenced in docs/todo-next.md.
- T028 [P] Add Compose instrumentation tests for critical UI flows (conversation list, chat detail, message composition) validating user behavior, accessibility, and Material design compliance.
- T029 [P] Add unit and instrumentation tests for data access paths (Room DAOs, repositories, caching rules) confirming read/write integrity, error propagation, and offline resilience.

## Dependencies & Sequencing
- T001 → T002 → T003 establish tooling before tests.
- Tests (T004–T009) must run and fail before any implementations (T010–T023).
- Model tasks (T010–T018) feed into generator (T019) and UI (T020); keep [P] status but respect logical dependencies when assigning.
- Gradle wiring (T021) depends on tests from T009 and setup from T001.
- DI & publishing (T022–T023) depend on generator/UI completion (T019–T021).
- Polish tasks (T024–T026) run after functional integration.

## Parallel Execution Examples
- Run together: `specify task run T004`, `specify task run T005`, `specify task run T006` (different test files, shared setup complete).
- After tests pass, parallelize: `specify task run T010`, `specify task run T012`, `specify task run T015` (independent model files).

```sh
# Sample parallel kick-off
specify task run T004 & specify task run T005 & specify task run T006
```
