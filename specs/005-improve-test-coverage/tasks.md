# Tasks: Improve Test Coverage for nanoAI

**Input**: Design documents from `/specs/005-improve-test-coverage/`
**Prerequisites**: plan.md, research.md, data-model.md, contracts/, quickstart.md

## Phase 3.1: Setup
- [X] T001 Stabilize the managed Pixel 6 config in `app/build.gradle.kts` by setting `testedAbi = "x86_64"` on `pixel6Api34`, documenting the API 34 emulator ABI change, and wiring the property into `ciManagedDeviceDebugAndroidTest`.
- [X] T002 Add a deterministic instrumentation harness by introducing `app/src/androidTest/java/com/vjaykrsna/nanoai/testing/TestEnvironmentRule.kt` to reset DataStore/Room state and network toggles before each test and by applying the rule across Compose suites so first-launch disclaimers and offline flows start from a clean slate.

## Phase 3.2: Tests First (TDD)
- [X] T003 [P] Create `app/src/test/java/com/vjaykrsna/nanoai/contracts/CoverageReportSchemaTest.kt` that exercises `specs/005-improve-test-coverage/contracts/coverage-report.schema.json` with representative JSON payloads and asserts thresholds, trend entries, and risk register objects.
- [X] T004 [P] Extend `app/src/test/java/com/vjaykrsna/nanoai/coverage/CoverageSummaryTest.kt` to cover typed risk references, `statusBreakdown`, and trend delta rounding so new coverage summary invariants fail before implementation.
- [X] T005 [P] Add monotonic ordering and threshold alignment cases to `app/src/test/java/com/vjaykrsna/nanoai/coverage/model/CoverageTrendPointTest.kt`, including a failing test for mismatched thresholds.
- [X] T006 [P] Strengthen `app/src/test/java/com/vjaykrsna/nanoai/coverage/model/CoverageMetricTest.kt` with assertions for `deltaFromThreshold`, `meetsThreshold`, and enum transitions around boundary values.
- [X] T007 [P] Update `app/src/test/java/com/vjaykrsna/nanoai/coverage/model/TestSuiteCatalogEntryTest.kt` to expect case-insensitive risk tag matching and to reject negative coverage contributions.
- [X] T008 [P] Cover actionable deadlines, severity gates, and mitigation formatting in `app/src/test/java/com/vjaykrsna/nanoai/coverage/model/RiskRegisterItemTest.kt`.
- [X] T009 [P] Expand `app/src/test/java/com/vjaykrsna/nanoai/coverage/model/TestLayerTest.kt` with expectations for `machineName` camel casing (`ViewModel`, `UI`, `Data`) and analytics key normalization.
- [X] T010 [P] Add Jupiter tests in `app/src/test/java/com/vjaykrsna/nanoai/feature/settings/domain/huggingface/HuggingFaceAuthCoordinatorTest.kt` that fail until `slow_down` backoff and offline retry suppression are implemented.
- [X] T011 [P] Add a cached-fallback expectation to `app/src/test/java/com/vjaykrsna/nanoai/feature/library/domain/RefreshModelCatalogUseCaseTest.kt`, asserting a successful result when the remote source throws.
- [X] T012 [P] Harden `app/src/androidTest/java/com/vjaykrsna/nanoai/coverage/ui/CoverageDashboardTest.kt` with assertions for formatted percent strings, `coverage-layer-*` tags, and offline announcement semantics.
- [X] T013 [P] Enhance `app/src/androidTest/java/com/vjaykrsna/nanoai/disclaimer/DisclaimerDialogTest.kt` to verify TalkBack descriptions and to fail when the accept button enables before scrolling.
- [X] T014 [P] Extend `app/src/androidTest/java/com/vjaykrsna/nanoai/feature/library/data/ModelCatalogOfflineTest.kt` to assert successful results and audit MockWebServer calls during HTTP 503 fallbacks.
- [X] T015 [P] Strengthen `app/src/androidTest/java/com/vjaykrsna/nanoai/feature/uiux/CommandPaletteComposeTest.kt` with focus assertions, retry button state checks, and snackbar undo expectations to surface current regressions.
- [X] T016 [P] Expand `app/src/androidTest/java/com/vjaykrsna/nanoai/feature/uiux/HomeHubFlowTest.kt` with node visibility checks for quick actions, recent activity tags, and command palette events.
- [X] T017 [P] Update `app/src/androidTest/java/com/vjaykrsna/nanoai/feature/uiux/AdaptiveShellTest.kt` to fail when permanent drawers stay hidden on expanded layouts or when accessibility focus cannot reach `shell_content`.
- [X] T018 [P] Add queue population and semantics coverage to `app/src/androidTest/java/com/vjaykrsna/nanoai/feature/uiux/OfflineProgressTest.kt`, including retry intent verification.
- [X] T019 [P] Lock in `app/src/androidTest/java/com/vjaykrsna/nanoai/feature/uiux/contracts/HomeScreenContractTest.kt` expectations for column counts, mode cards, and recent feed accessibility.
- [X] T020 [P] Strengthen `app/src/androidTest/java/com/vjaykrsna/nanoai/feature/uiux/contracts/OfflineBannerContractTest.kt` with assertions for retry action semantics and disabled affordances.
- [X] T021 [P] Expand `app/src/androidTest/java/com/vjaykrsna/nanoai/feature/uiux/contracts/SettingsScreenContractTest.kt` to validate offline provider listings, FAB visibility, and TalkBack copy.
- [X] T022 [P] Tighten `app/src/androidTest/java/com/vjaykrsna/nanoai/feature/uiux/contracts/SidebarContractTest.kt` around drawer toggles, navigation destinations, and deep link slots.
- [X] T023 [P] Enhance `app/src/androidTest/java/com/vjaykrsna/nanoai/feature/uiux/contracts/ThemeToggleContractTest.kt` with persistence and recomposition checks.
- [X] T024 [P] Enrich `app/src/androidTest/java/com/vjaykrsna/nanoai/feature/uiux/scenario/HomeNavigationScenarioTest.kt` with assertions covering tools panel expansion and recent action execution.
- [X] T025 [P] Update `app/src/androidTest/java/com/vjaykrsna/nanoai/feature/uiux/scenario/OfflineModeScenarioTest.kt` to fail when offline banners or retry queues are missing.
- [X] T026 [P] Add navigation + undo verification to `app/src/androidTest/java/com/vjaykrsna/nanoai/feature/uiux/scenario/SidebarSettingsScenarioTest.kt`.
- [X] T027 [P] Extend `app/src/androidTest/java/com/vjaykrsna/nanoai/feature/uiux/scenario/ThemeToggleScenarioTest.kt` with process-death persistence assertions.
- [X] T028 [P] Strengthen `app/src/androidTest/java/com/vjaykrsna/nanoai/model/ModelDownloadScenarioTest.kt` to expect actionable error banners and retry affordances.
- [X] T029 [P] Enhance `app/src/androidTest/java/com/vjaykrsna/nanoai/persona/OfflinePersonaFlowTest.kt` to verify persona queue replay after network restoration.

## Phase 3.3: Core Implementation
- [X] T030 [P] Replace `riskItems: List<String>` with typed references in `app/src/main/java/com/vjaykrsna/nanoai/coverage/model/CoverageSummary.kt` and introduce `RiskRegisterItemRef` to satisfy the new tests.
- [X] T031 [P] Enforce threshold alignment and provide factory helpers in `app/src/main/java/com/vjaykrsna/nanoai/coverage/model/CoverageTrendPoint.kt` so tests around monotonic sequences pass.
- [X] T032 [P] Normalize risk tags and coverage contribution rounding in `app/src/main/java/com/vjaykrsna/nanoai/coverage/model/TestSuiteCatalogEntry.kt`.
- [X] T033 [P] Implement actionable deadline calculations and mitigation formatting in `app/src/main/java/com/vjaykrsna/nanoai/coverage/model/RiskRegisterItem.kt`.
- [X] T034 [P] Fix camel-case generation and analytics key helpers in `app/src/main/java/com/vjaykrsna/nanoai/coverage/model/TestLayer.kt`.
- [X] T035 [P] Add derived helpers (e.g., `statusColor`) and boundary-safe rounding in `app/src/main/java/com/vjaykrsna/nanoai/coverage/model/CoverageMetric.kt`.
- [X] T036 [P] Implement `slow_down` backoff handling, offline retry suppression, and accessibility surfaces in `app/src/main/java/com/vjaykrsna/nanoai/feature/settings/domain/huggingface/HuggingFaceAuthCoordinator.kt`.
- [X] T037 [P] Teach `app/src/main/java/com/vjaykrsna/nanoai/feature/library/domain/RefreshModelCatalogUseCase.kt` to return cached success on network failures and to log structured context for coverage reports.
- [X] T038 [P] Preserve cached catalog metadata and expose offline refresh hooks in `app/src/main/java/com/vjaykrsna/nanoai/feature/library/data/impl/ModelCatalogRepositoryImpl.kt`.
- [X] T039 [P] Update `app/src/main/java/com/vjaykrsna/nanoai/coverage/domain/CoverageReportGenerator.kt` to include risk register references, trend slicing, and threshold metadata required by the contract tests.
- [X] T040 [P] Align tags, percentage formatting, and offline banner semantics in `app/src/main/java/com/vjaykrsna/nanoai/coverage/ui/CoverageDashboardScreen.kt` and `CoverageDashboardBanner.kt`.
- [X] T041 [P] Ensure permanent drawers open on expanded layouts, add TalkBack hints, and adjust command palette focus handling in `app/src/main/java/com/vjaykrsna/nanoai/feature/uiux/ui/shell/NanoShellScaffold.kt`.
- [X] T042 [P] Refine quick action chips, mode cards, and recent activity semantics in `app/src/main/java/com/vjaykrsna/nanoai/feature/uiux/ui/HomeScreen.kt`.
- [X] T043 [P] Harden retry/clear affordances and semantics in `app/src/main/java/com/vjaykrsna/nanoai/feature/uiux/ui/progress/ProgressCenterPanel.kt`.
- [X] T044 [P] Improve focus management and disabled states in `app/src/main/java/com/vjaykrsna/nanoai/feature/uiux/ui/commandpalette/CommandPaletteSheet.kt`.
- [X] T045 [P] Update `app/src/main/java/com/vjaykrsna/nanoai/feature/uiux/state/ShellLayoutState.kt` to expose permanent drawer visibility and offline helpers that the UI now consumes.

## Phase 3.4: Integration
- [X] T046 [P] Expose disclaimer exposure state via `app/src/main/java/com/vjaykrsna/nanoai/core/data/preferences/PrivacyPreferenceStore.kt` (Flow + reset API) so UI can gate first run.
- [X] T047 [P] Surface disclaimer UI state in `app/src/main/java/com/vjaykrsna/nanoai/feature/uiux/presentation/AppViewModel.kt` and show the dialog in `app/src/main/java/com/vjaykrsna/nanoai/ui/navigation/NavigationScaffold.kt` with TalkBack-friendly tags.
- [X] T048 [P] Wire the disclaimer dialog entry point and hydration fallback in `app/src/main/java/com/vjaykrsna/nanoai/MainActivity.kt`.
- [X] T049 [P] Merge offline progress queues, undo flows, and palette visibility updates in `app/src/main/java/com/vjaykrsna/nanoai/feature/uiux/presentation/ShellViewModel.kt` to satisfy scenario tests.
- [X] T050 [P] Extend `app/src/main/java/com/vjaykrsna/nanoai/coverage/tasks/VerifyCoverageThresholdsTask.kt` (and its helpers) to emit risk summaries, trend arrays, and markdown matching the new schema/tests.  
	Note: added JSON output, generated per-layer trend points, and included risk reference lines in the markdown summary.

## Phase 3.5: Polish
- [X] T051 [P] Author `docs/todo-next.md` (or refresh it if recreated) with the new coverage backlog, referencing the failing flows resolved by this feature.
- [X] T052 [P] Update `docs/testing.md` with instructions for `jacocoFullReport`, `verifyCoverageThresholds`, and managed-device requirements introduced in T001–T002.
- [X] T053 [P] Refresh `specs/005-improve-test-coverage/quickstart.md` to document the new disclaimer rule, coverage commands, and emulator ABI requirement.
- [X] T054 [P] Revise `docs/coverage/risk-register.md` with the updated coverage snapshot, mitigated risks, and any TODO items deferred (leave explicit TODO comments where further automation is still pending).
- [X] T055 [P] Update `README.md` (project root) to highlight the new coverage workflows and links to the risk register.

## Dependency Notes
- T003–T029 depend on test harness setup from T001–T002.
- T030–T045 must only begin once the corresponding tests (T003–T029) are red to preserve TDD.
- T046–T050 depend on the core model and UI work from T030–T045.
- T051–T055 should run last after validation tasks confirm the suite is green.

## Parallel Execution Example
```bash
task start --id T003
task start --id T012
task start --id T015
```
