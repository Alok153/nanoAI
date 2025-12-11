# Tasks: Offline Multimodal nanoAI Assistant

**Input**: Design documents from /specs/001-foundation/
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/

## Phase 1: Setup (Shared Infrastructure)

- [x] T001 Install repo hooks to enforce spotless/detekt via [scripts/hooks/install-hooks.sh](scripts/hooks/install-hooks.sh)
- [x] T002 [P] Add `verifyCoverageThresholds` to local pre-commit guidance in [docs/development/BUILD_WORKFLOW.md](docs/development/BUILD_WORKFLOW.md)
- [x] T003 [P] Refresh quality gate configs for feature branch in [config/quality/quality-gates.json](config/quality/quality-gates.json) and [config/quality/conventions.json](config/quality/conventions.json)
- [x] T004 Ensure gradle plugin versions aligned with plan in [gradle/libs.versions.toml](gradle/libs.versions.toml) (no downgrades)

## Phase 2: Foundational (Blocking Prerequisites)

- [x] T005 Create feature-owned domain module skeletons for audio, image, library, settings under [app/src/main/java/com/vjaykrsna/nanoai/feature/](app/src/main/java/com/vjaykrsna/nanoai/feature/) with `domain` and `data` packages respecting UseCase → Repository → DataSource
- [x] T006 Add shared sealed result/error types for feature domains in [app/src/main/java/com/vjaykrsna/nanoai/core/model/NanoAIResult.kt](app/src/main/java/com/vjaykrsna/nanoai/core/model/NanoAIResult.kt) to be reused by feature modules (depends on T005)
- [x] T007 [P] Wire Hilt bindings for new feature repositories in [app/src/main/java/com/vjaykrsna/nanoai/feature/di/FeatureRepositoryModule.kt](app/src/main/java/com/vjaykrsna/nanoai/feature/di/FeatureRepositoryModule.kt) (depends on T005)
- [x] T008 [P] Add Room schema updates for ModelPackage/DownloadTask/PersonaProfile to match data-model.md in [app/src/main/java/com/vjaykrsna/nanoai/core/data/db](app/src/main/java/com/vjaykrsna/nanoai/core/data/db) with migrations and tests (depends on T005)
- [x] T009 Establish MediaPipe runtime abstraction interface in [app/src/main/java/com/vjaykrsna/nanoai/core/runtime/LocalRuntimeGateway.kt](app/src/main/java/com/vjaykrsna/nanoai/core/runtime/LocalRuntimeGateway.kt) with IO dispatcher usage and tests
- [x] T010 Add offline/online connectivity notifier shared flow in [app/src/main/java/com/vjaykrsna/nanoai/core/device/ConnectivityObserver.kt](app/src/main/java/com/vjaykrsna/nanoai/core/device/ConnectivityObserver.kt) with Turbine tests

## Phase 3: User Story 1 - Local-first Private Assistant (Priority: P1) 🎯

### Tests
- [ ] T011 [P] [US1] Add failing offline chat ViewModel tests covering local model selection and banner in [app/src/test/java/com/vjaykrsna/nanoai/feature/chat/ChatViewModelTest.kt](app/src/test/java/com/vjaykrsna/nanoai/feature/chat/ChatViewModelTest.kt) (depends on T005, T009, T010)
- [ ] T012 [P] [US1] Add repository/use case unit tests for local inference in [app/src/test/java/com/vjaykrsna/nanoai/feature/chat/domain/LocalInferenceUseCaseTest.kt](app/src/test/java/com/vjaykrsna/nanoai/feature/chat/domain/LocalInferenceUseCaseTest.kt) (depends on T005, T009)
- [ ] T013 [US1] Add Compose UI test for offline chat flow and accessibility labels in [app/src/androidTest/java/com/vjaykrsna/nanoai/feature/chat/ChatScreenTest.kt](app/src/androidTest/java/com/vjaykrsna/nanoai/feature/chat/ChatScreenTest.kt) (depends on T011)

### Implementation
- [ ] T014 [P] [US1] Implement `LocalInferenceUseCase` and repository using MediaPipe runtime in [app/src/main/java/com/vjaykrsna/nanoai/feature/chat/domain](app/src/main/java/com/vjaykrsna/nanoai/feature/chat/domain) (depends on T009)
- [ ] T015 [P] [US1] Add persona-aware chat state models and mappers in [app/src/main/java/com/vjaykrsna/nanoai/feature/chat/model](app/src/main/java/com/vjaykrsna/nanoai/feature/chat/model) (depends on T005)
- [ ] T016 [US1] Update `ChatViewModel` to route through use case and surface offline indicator in [app/src/main/java/com/vjaykrsna/nanoai/feature/chat/presentation/ChatViewModel.kt](app/src/main/java/com/vjaykrsna/nanoai/feature/chat/presentation/ChatViewModel.kt) (depends on T014, T015, T010)
- [ ] T017 [US1] Add chat screen UI for offline banner and error envelopes in [app/src/main/java/com/vjaykrsna/nanoai/feature/chat/presentation/ChatScreen.kt](app/src/main/java/com/vjaykrsna/nanoai/feature/chat/presentation/ChatScreen.kt) (depends on T016)

## Phase 4: User Story 2 - Model Library Management (Priority: P1)

### Tests
- [ ] T018 [P] [US2] Add download queue repository tests for pause/resume/concurrency=1 in [app/src/test/java/com/vjaykrsna/nanoai/feature/library/data/ModelDownloadRepositoryTest.kt](app/src/test/java/com/vjaykrsna/nanoai/feature/library/data/ModelDownloadRepositoryTest.kt) (depends on T008)
- [ ] T019 [P] [US2] Add ViewModel tests for progress center and deletion safety in [app/src/test/java/com/vjaykrsna/nanoai/feature/library/ModelLibraryViewModelTest.kt](app/src/test/java/com/vjaykrsna/nanoai/feature/library/ModelLibraryViewModelTest.kt) (depends on T018)
- [ ] T020 [US2] Add Compose UI test for download progress/pause/resume in [app/src/androidTest/java/com/vjaykrsna/nanoai/feature/library/ModelLibraryScreenTest.kt](app/src/androidTest/java/com/vjaykrsna/nanoai/feature/library/ModelLibraryScreenTest.kt) (depends on T019)

### Implementation
- [ ] T021 [P] [US2] Implement model library domain use cases (queue download, pause, resume, delete) in [app/src/main/java/com/vjaykrsna/nanoai/feature/library/domain](app/src/main/java/com/vjaykrsna/nanoai/feature/library/domain) (depends on T005, T008)
- [ ] T022 [P] [US2] Implement repository with checksum validation and concurrency limit in [app/src/main/java/com/vjaykrsna/nanoai/feature/library/data/ModelDownloadRepository.kt](app/src/main/java/com/vjaykrsna/nanoai/feature/library/data/ModelDownloadRepository.kt) (depends on T021)
- [ ] T023 [US2] Update Model Library ViewModel and UI for progress center, delete safety, and connectivity handling in [app/src/main/java/com/vjaykrsna/nanoai/feature/library/presentation/ModelLibraryViewModel.kt](app/src/main/java/com/vjaykrsna/nanoai/feature/library/presentation/ModelLibraryViewModel.kt) and [app/src/main/java/com/vjaykrsna/nanoai/feature/library/presentation/ModelLibraryScreen.kt](app/src/main/java/com/vjaykrsna/nanoai/feature/library/presentation/ModelLibraryScreen.kt) (depends on T022, T010)

## Phase 5: User Story 3 - Personas & Context Control (Priority: P2)

### Tests
- [ ] T024 [P] [US3] Add persona repository/use case tests for switching and logging in [app/src/test/java/com/vjaykrsna/nanoai/feature/settings/domain/PersonaUseCaseTest.kt](app/src/test/java/com/vjaykrsna/nanoai/feature/settings/domain/PersonaUseCaseTest.kt) (depends on T005, T008)
- [ ] T025 [US3] Add ViewModel tests for persona switch flow and thread continuation in [app/src/test/java/com/vjaykrsna/nanoai/feature/chat/PersonaSwitcherViewModelTest.kt](app/src/test/java/com/vjaykrsna/nanoai/feature/chat/PersonaSwitcherViewModelTest.kt) (depends on T024)

### Implementation
- [ ] T026 [P] [US3] Implement persona domain use cases and repository in [app/src/main/java/com/vjaykrsna/nanoai/feature/settings/domain](app/src/main/java/com/vjaykrsna/nanoai/feature/settings/domain) and [app/src/main/java/com/vjaykrsna/nanoai/feature/settings/data](app/src/main/java/com/vjaykrsna/nanoai/feature/settings/data) (depends on T005, T008)
- [ ] T027 [US3] Wire persona switcher UI and state handling in [app/src/main/java/com/vjaykrsna/nanoai/feature/chat/presentation/PersonaSwitcher.kt](app/src/main/java/com/vjaykrsna/nanoai/feature/chat/presentation/PersonaSwitcher.kt) and [app/src/main/java/com/vjaykrsna/nanoai/feature/chat/presentation/ChatViewModel.kt](app/src/main/java/com/vjaykrsna/nanoai/feature/chat/presentation/ChatViewModel.kt) (depends on T026, T025)

## Phase 6: User Story 4 - Settings, Privacy & Backup (Priority: P2)

### Tests
- [ ] T028 [P] [US4] Add export/import unit tests for deterministic persona/provider restore in [app/src/test/java/com/vjaykrsna/nanoai/feature/settings/data/BackupRepositoryTest.kt](app/src/test/java/com/vjaykrsna/nanoai/feature/settings/data/BackupRepositoryTest.kt) (depends on T008)
- [ ] T029 [US4] Add UI test for disclaimer, export warning, and import success in [app/src/androidTest/java/com/vjaykrsna/nanoai/feature/settings/SettingsScreenTest.kt](app/src/androidTest/java/com/vjaykrsna/nanoai/feature/settings/SettingsScreenTest.kt) (depends on T028)

### Implementation
- [ ] T030 [P] [US4] Implement backup/export/import repository with encryption warnings in [app/src/main/java/com/vjaykrsna/nanoai/feature/settings/data/BackupRepository.kt](app/src/main/java/com/vjaykrsna/nanoai/feature/settings/data/BackupRepository.kt) (depends on T028)
- [ ] T031 [US4] Update Settings ViewModel and screen for disclaimer logging, privacy dashboard, and backup actions in [app/src/main/java/com/vjaykrsna/nanoai/feature/settings/presentation/SettingsViewModel.kt](app/src/main/java/com/vjaykrsna/nanoai/feature/settings/presentation/SettingsViewModel.kt) and [app/src/main/java/com/vjaykrsna/nanoai/feature/settings/presentation/SettingsScreen.kt](app/src/main/java/com/vjaykrsna/nanoai/feature/settings/presentation/SettingsScreen.kt) (depends on T030)

## Phase 7: User Story 5 - Robust Shell & Navigation (Priority: P3)

### Tests
- [ ] T032 [P] [US5] Add navigation shell ViewModel tests for connectivity banners and context preservation in [app/src/test/java/com/vjaykrsna/nanoai/feature/uiux/ShellViewModelTest.kt](app/src/test/java/com/vjaykrsna/nanoai/feature/uiux/ShellViewModelTest.kt) (depends on T010)
- [ ] T033 [US5] Add Compose UI test for Home/Chat/Library/Settings navigation with offline/online toggles in [app/src/androidTest/java/com/vjaykrsna/nanoai/feature/uiux/ShellNavigationTest.kt](app/src/androidTest/java/com/vjaykrsna/nanoai/feature/uiux/ShellNavigationTest.kt) (depends on T032)

### Implementation
- [ ] T034 [P] [US5] Implement connectivity-aware shell state and progress center integration in [app/src/main/java/com/vjaykrsna/nanoai/feature/uiux/ShellViewModel.kt](app/src/main/java/com/vjaykrsna/nanoai/feature/uiux/ShellViewModel.kt) (depends on T010)
- [ ] T035 [US5] Update shell UI for sidebar navigation, banners, and command palette hooks in [app/src/main/java/com/vjaykrsna/nanoai/feature/uiux/ShellScaffold.kt](app/src/main/java/com/vjaykrsna/nanoai/feature/uiux/ShellScaffold.kt) (depends on T034)

## Phase 8: Polish & Cross-Cutting

- [ ] T036 [P] Resolve at least 10 TODO/FIXME items focusing on image/audio/runtime placeholders in [app/src/main/java/com/vjaykrsna/nanoai](app/src/main/java/com/vjaykrsna/nanoai) with linked commits
- [ ] T037 [P] Align architecture docs with feature-owned domain migration in [docs/architecture/ARCHITECTURE.md](docs/architecture/ARCHITECTURE.md) and [docs/architecture/project-structure.md](docs/architecture/project-structure.md) (depends on T005)
- [ ] T038 [P] Add Roborazzi or screenshot baselines for critical screens in [app/src/androidTest/java/com/vjaykrsna/nanoai](app/src/androidTest/java/com/vjaykrsna/nanoai) to guard UI regressions
- [ ] T039 Add macrobenchmark for cold start and navigation in [macrobenchmark/src/main/java/com/vjaykrsna/nanoai/macrobenchmark/StartupBenchmark.kt](macrobenchmark/src/main/java/com/vjaykrsna/nanoai/macrobenchmark/StartupBenchmark.kt)
- [ ] T040 [P] Update quickstart validation steps to reflect offline-first flows in [specs/001-foundation/quickstart.md](specs/001-foundation/quickstart.md) and add acceptance traceability
- [ ] T041 Run full gate: `./gradlew spotlessCheck detekt testDebugUnitTest verifyCoverageThresholds` and capture results in [app/build/reports](app/build/reports)

## Dependencies & Execution Order
- Phase 1 precedes all work; Phase 2 blocks stories.
- Story phases can proceed after Phase 2; within each story, tests (T011/T012/T013, etc.) precede implementation.
- Tasks touching the same files declare dependencies in descriptions; prefer [P] tasks in distinct paths for parallelism.

## Parallel Opportunities
- [P] tasks across phases may run concurrently when file paths do not overlap (see [P] markers).

## Suggested MVP Scope
- Complete Phase 1–2 and User Stories 1–2 (P1) to deliver offline chat plus model library management.

## Totals
- Overall tasks: 41
- Per story: US1 (7), US2 (6), US3 (4), US4 (4), US5 (4)
- Parallel-capable tasks: 17
