---
description: "Task list for standardising ViewModel state patterns"
---

# Tasks: Offline Multimodal nanoAI Assistant – ViewModel State Standardization

**Input**: specs/001-foundation/plan.md, specs/001-foundation/spec.md, specs/001-foundation/data-model.md, specs/001-foundation/quickstart.md, specs/001-foundation/research.md

**Prerequisites**: Ensure clean architecture constraints from AGENTS.md remain intact; reuse existing use cases, repositories, and data sources.

**Tests**: ViewModel changes must keep ≥75% coverage; update impacted unit tests and add new ones where coverage would regress.

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Align the team on a consistent ViewModel state strategy before code changes.

- [x] T001 Document target ViewModel state patterns (single StateFlow + event stream, reducers, testing strategy) in docs/development/VIEWMODEL_STATE.md with references to AGENTS.md and issues.md.
- [x] T002 Update docs/architecture/ARCHITECTURE.md to reflect the shared ViewModel state host between Composable → ViewModel → UseCase layers.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Provide shared state and event infrastructure required by all affected ViewModels.

- [x] T003 Create shared state contract (NanoAIViewState, reducer helpers) in app/src/main/java/com/vjaykrsna/nanoai/shared/state/NanoAIViewState.kt.
- [x] T004 Define one-shot event contract (NanoAIViewEvent, NanoAIEventChannel) in app/src/main/java/com/vjaykrsna/nanoai/shared/state/NanoAIViewEvent.kt.
- [x] T005 Implement ViewModelStateHost utility (state + event management, coroutine scoping) in app/src/main/java/com/vjaykrsna/nanoai/shared/state/ViewModelStateHost.kt.
- [x] T006 Add ViewModelStateHostTestHarness using Turbine/MainDispatcherRule in app/src/test/java/com/vjaykrsna/nanoai/shared/state/ViewModelStateHostTestHarness.kt.

**Checkpoint**: Shared state infrastructure is available; feature ViewModels can migrate safely.

---

## Phase 3: User Story 1 - Local-first Private Assistant (Priority: P1) 🎯 MVP

**Goal**: Chat experience emits a single coherent state object with predictable side effects.

**Independent Test**: Install app, download a local model, and complete several chats fully offline.

### Implementation for User Story 1

- [ ] T007 [US1] Introduce ChatUiState data class capturing thread, messages, personas, model picker, attachments, loading/error flags in app/src/main/java/com/vjaykrsna/nanoai/feature/chat/presentation/state/ChatUiState.kt.
- [ ] T008 [US1] Refactor ChatViewModel to extend ViewModelStateHost, emit ChatUiState via one StateFlow, migrate error/events onto NanoAIViewEvent in app/src/main/java/com/vjaykrsna/nanoai/feature/chat/presentation/ChatViewModel.kt.
- [ ] T009 [US1] Update ChatScreen and related composables to consume ChatUiState and event collectors in app/src/main/java/com/vjaykrsna/nanoai/feature/chat/ui/ChatScreen.kt.
- [ ] T010 [US1] Migrate HistoryViewModel to the shared state host with a HistoryUiState in app/src/main/java/com/vjaykrsna/nanoai/feature/chat/presentation/HistoryViewModel.kt and new app/src/main/java/com/vjaykrsna/nanoai/feature/chat/presentation/state/HistoryUiState.kt.
- [ ] T011 [US1] Refresh chat feature tests (ChatViewModelTest.kt, HistoryViewModelTest.kt) to use ViewModelStateHostTestHarness and validate new state/event contracts in app/src/test/java/com/vjaykrsna/nanoai/feature/chat/presentation/.

**Checkpoint**: Chat flows operate offline with consolidated state; tests cover new behavior.

---

## Phase 4: User Story 2 - Model Library Management (Priority: P1)

**Goal**: Model library surfaces unified state covering downloads, filters, and Hugging Face catalog.

**Independent Test**: From a clean install, manage models end-to-end without using chat.

### Implementation for User Story 2

- [x] T012 [US2] Create ModelLibraryUiState aggregating local/curated sections, filters, download queue, and Hugging Face catalog in app/src/main/java/com/vjaykrsna/nanoai/feature/library/presentation/state/ModelLibraryUiState.kt.
- [x] T013 [US2] Collapse ModelLibraryStateStore outputs into ModelLibraryUiState reducers and drop ad-hoc MutableStateFlows in app/src/main/java/com/vjaykrsna/nanoai/feature/library/presentation/ModelLibraryStateStore.kt.
- [x] T014 [US2] Refactor ModelLibraryViewModel to use ViewModelStateHost, emit ModelLibraryUiState, and normalize events in app/src/main/java/com/vjaykrsna/nanoai/feature/library/presentation/ModelLibraryViewModel.kt.
- [ ] T015 [US2] Align HuggingFaceLibraryViewModel with shared state/event contracts to avoid divergent patterns in app/src/main/java/com/vjaykrsna/nanoai/feature/library/presentation/HuggingFaceLibraryViewModel.kt.
- [x] T016 [US2] Update ModelLibraryScreen and supporting UI components to consume ModelLibraryUiState in app/src/main/java/com/vjaykrsna/nanoai/feature/library/ui/ModelLibraryScreen.kt.
- [x] T017 [US2] Add ModelLibraryViewModelTest (and relevant Hugging Face coverage) using the new harness in app/src/test/java/com/vjaykrsna/nanoai/feature/library/presentation/.

**Checkpoint**: Model library exposes a single reactive state and validated tests for downloads and filters.

---

## Phase 5: User Story 4 - Settings, Privacy & Backup (Priority: P2)

**Goal**: Settings expose consolidated state covering preferences, providers, backups, and auth flows.

**Independent Test**: Drive only through Settings and export/import; chat is incidental.

### Implementation for User Story 4

- [ ] T018 [US4] Define SettingsUiState (replacing SettingsUiUxState) covering providers, privacy, UI preferences, backup progress, and Hugging Face data in app/src/main/java/com/vjaykrsna/nanoai/feature/settings/presentation/state/SettingsUiState.kt.
- [ ] T019 [US4] Refactor SettingsViewModel to adopt ViewModelStateHost, emit SettingsUiState, and route controller side effects through NanoAIViewEvent in app/src/main/java/com/vjaykrsna/nanoai/feature/settings/presentation/SettingsViewModel.kt.
- [ ] T020 [US4] Update SettingsScreen wiring (SettingsScreen.kt, SettingsScreenState.kt, SettingsScreenContent.kt, sections) to consume SettingsUiState and streamlined events in app/src/main/java/com/vjaykrsna/nanoai/feature/settings/ui/.
- [ ] T021 [US4] Refresh SettingsViewModel tests (SettingsViewModelTest.kt, SettingsViewModelUiUxTest.kt, import/export suites) to cover the unified state in app/src/test/java/com/vjaykrsna/nanoai/feature/settings/presentation/.

**Checkpoint**: Settings flows expose a unified state model and pass updated tests.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Lock in consistency and quality gates across the codebase.

- [ ] T022 Add Detekt rule enforcing single-state exposure for ViewModels (update config/quality/detekt/detekt.yml and create ViewModelStateRule in config/quality/detekt/custom-rules/src/main/kotlin/).
- [ ] T023 Update docs/development/TESTING.md with ViewModelStateHostTestHarness usage and coverage expectations.
- [ ] T024 Extend docs/development/VIEWMODEL_STATE.md with an adoption tracker covering remaining ViewModels and follow-up actions.
- [ ] T025 Refresh docs/development/UI_COMPONENTS.md to show how composables consume the unified UiState + event streams.
- [ ] T026 Run ./gradlew spotlessCheck detekt testDebugUnitTest verifyCoverageThresholds to confirm formatting, static analysis, and coverage gates.

---

## Dependencies & Execution Order

- Phase 1 (Setup) must complete before implementing shared infrastructure.
- Phase 2 (Foundational) depends on Phase 1 and blocks all user stories.
- User Story phases (Phases 3–5) depend on Phase 2; tackle US1 first to validate the pattern, then US2, followed by US4.
- Phase 6 (Polish) can only begin after all targeted user stories reach their checkpoints.

## Parallel Opportunities

- **Setup**: T001 and T002 touch different docs and can proceed in parallel.
- **US1**: After T007 defines ChatUiState, T008 and T010 can proceed concurrently by separate owners; T009 waits on T008; T011 finalises tests.
- **US2**: T012 unlocks T013–T015; once T014 lands, T016 (UI) and T017 (tests) can run in parallel.
- **US4**: T018 enables T019; once ViewModel is refactored, T020 (UI) and T021 (tests) may run concurrently.
- **Polish**: T022–T025 are independent documentation/tooling tasks and can run in parallel before executing T026.

## Implementation Strategy

1. Deliver the MVP by completing Setup, Foundational work, and User Story 1; validate offline chat end-to-end.
2. Extend the pattern to the model library (User Story 2) to cover install/download flows.
3. Upgrade settings (User Story 4) once the pattern is proven, ensuring privacy and backup remain stable.
4. Finish with polish tasks to institutionalise the pattern and enforce it via tooling and documentation.
