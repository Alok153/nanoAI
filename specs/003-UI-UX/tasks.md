# Tasks: UI/UX — Polished Product-Grade Experience

**Input**: Design documents from `/specs/003-UI-UX/`
**Prerequisites**: plan.md (required), research.md, data-model.md, contracts/, quickstart.md

## Phase 3.1: Setup
- [X] T001 Update Compose/navigation dependencies and enable Compose metrics exports in `app/build.gradle.kts` to support the unified shell instrumentation.  
  _Depends on_: —
- [X] T002 Create placeholder shell entry points (`ShellStateRepository`, `ShellViewModel`, `NanoShellScaffold`) under `app/src/main/java/com/vjaykrsna/nanoai/feature/uiux/` with `TODO()` bodies so contract tests can compile.  
  _Depends on_: T001

## Phase 3.2: Tests First (TDD)
- [X] T003 [P] Author failing unit tests in `app/src/test/java/com/vjaykrsna/nanoai/feature/uiux/presentation/ShellViewModelTest.kt` covering all intents from `contracts/shell-interactions.md`.  
  _Depends on_: T002
- [X] T004 [P] Add failing Compose instrumentation tests in `app/src/androidTest/java/com/vjaykrsna/nanoai/feature/uiux/CommandPaletteComposeTest.kt` per `contracts/command-palette-ui-tests.md`.  
  _Depends on_: T002
- [X] T005 [P] Add failing offline/progress instrumentation tests in `app/src/androidTest/java/com/vjaykrsna/nanoai/feature/uiux/OfflineProgressTest.kt` per `contracts/offline-progress.md`.  
  _Depends on_: T002
- [X] T006 [P] Write Home Hub flow Compose test in `app/src/androidTest/java/com/vjaykrsna/nanoai/feature/uiux/HomeHubFlowTest.kt` validating acceptance scenarios from spec (grid launch within two taps, recent activity).  
  _Depends on_: T002
- [X] T007 [P] Write adaptive layout Compose test in `app/src/androidTest/java/com/vjaykrsna/nanoai/feature/uiux/AdaptiveShellTest.kt` asserting `WindowSizeClass` driven drawer behavior and accessibility focus.  
  _Depends on_: T002
- [X] T008 Update macrobenchmark scenario in `macrobenchmark/src/main/java/com/vjaykrsna/nanoai/macrobenchmark/NavigationBenchmarks.kt` to capture Home Hub launch + mode switch latency budgets (<100 ms interactions).  
  _Depends on_: T003–T007

## Phase 3.3: Core Implementation (only after tests fail)
- [X] T009 [P] Implement `ShellLayoutState` data class and helpers in `app/src/main/java/com/vjaykrsna/nanoai/feature/uiux/state/ShellLayoutState.kt` matching data-model.md.  
  _Depends on_: T003
- [X] T010 [P] Implement `ModeCard` model in `app/src/main/java/com/vjaykrsna/nanoai/feature/uiux/state/ModeCard.kt` with localized labels and primary actions per research.  
  _Depends on_: T006
- [X] T011 [P] Implement `CommandPaletteState` in `app/src/main/java/com/vjaykrsna/nanoai/feature/uiux/state/CommandPaletteState.kt` including selection handling and recent commands.  
  _Depends on_: T004
- [X] T012 [P] Implement `ProgressJob` domain model in `app/src/main/java/com/vjaykrsna/nanoai/feature/uiux/state/ProgressJob.kt` representing WorkManager/inference jobs.  
  _Depends on_: T005
- [X] T013 [P] Implement `ConnectivityBannerState` in `app/src/main/java/com/vjaykrsna/nanoai/feature/uiux/state/ConnectivityBannerState.kt` with CTA metadata.  
  _Depends on_: T005
- [X] T014 [P] Implement `UiPreferenceSnapshot` extensions in `app/src/main/java/com/vjaykrsna/nanoai/feature/uiux/state/UiPreferenceSnapshot.kt` capturing theme, density, font scale, onboarding, dismissed tooltips.  
  _Depends on_: T005
- [X] T015 [P] Implement `RecentActivityItem` model in `app/src/main/java/com/vjaykrsna/nanoai/feature/uiux/state/RecentActivityItem.kt` for history cards.  
  _Depends on_: T006
- [X] T016 Extend `UIStateSnapshotEntity`, `UIStateSnapshot` domain model, and add migration in `app/src/main/java/com/vjaykrsna/nanoai/core/data/db/entities/UIStateSnapshotEntity.kt` & `.../NanoAIDatabaseMigrations.kt` for drawer/palette persistence.  
  _Depends on_: T009, T015
- [X] T017 Expand `UiPreferencesStore` in `app/src/main/java/com/vjaykrsna/nanoai/core/data/preferences/UiPreferencesStore.kt` with keys for command palette recents, connectivity banner dismissal, and density defaults.  
  _Depends on_: T014
- [X] T018 Build `ShellStateRepository` in `app/src/main/java/com/vjaykrsna/nanoai/feature/uiux/data/ShellStateRepository.kt` combining preferences, progress, connectivity, and activity flows into `ShellLayoutState`.  
  _Depends on_: T009–T017
- [X] T019 Implement `CommandPaletteActionProvider` in `app/src/main/java/com/vjaykrsna/nanoai/feature/uiux/domain/CommandPaletteActionProvider.kt` aggregating navigation + quick actions.  
  _Depends on_: T011, T015, T018
- [X] T020 Implement `ProgressCenterCoordinator` in `app/src/main/java/com/vjaykrsna/nanoai/feature/uiux/domain/ProgressCenterCoordinator.kt` syncing WorkManager progress with `ProgressJob`.  
  _Depends on_: T012, T018
- [X] T021 Implement `ShellViewModel` in `app/src/main/java/com/vjaykrsna/nanoai/feature/uiux/presentation/ShellViewModel.kt` to satisfy all contract intents with proper state reducers.  
  _Depends on_: T003–T020
- [ ] T022 Build `NanoShellScaffold` composable in `app/src/main/java/com/vjaykrsna/nanoai/feature/uiux/ui/shell/NanoShellScaffold.kt` hosting drawers, command palette overlay, and offline banner surfaces.  
  _Depends on_: T004, T021
- [ ] T023 Implement `CommandPaletteSheet` UI in `app/src/main/java/com/vjaykrsna/nanoai/feature/uiux/ui/commandpalette/CommandPaletteSheet.kt` with keyboard navigation + semantics.  
  _Depends on_: T004, T019, T022
- [ ] T024 Implement `ProgressCenterPanel` UI in `app/src/main/java/com/vjaykrsna/nanoai/feature/uiux/ui/progress/ProgressCenterPanel.kt` rendering job queue + retry logic.  
  _Depends on_: T005, T012, T020
- [ ] T025 Implement `ConnectivityBanner` composable in `app/src/main/java/com/vjaykrsna/nanoai/feature/uiux/ui/components/ConnectivityBanner.kt` wired to dismissal cooldown.  
  _Depends on_: T013, T017, T022
- [ ] T026 Rebuild `HomeScreen` in `app/src/main/java/com/vjaykrsna/nanoai/feature/uiux/ui/HomeScreen.kt` as responsive grid with quick actions and recent activity list.  
  _Depends on_: T006, T010, T015, T022
- [ ] T027 Implement right-sidebar contextual panels (model selector, settings shortcuts, progress) in `app/src/main/java/com/vjaykrsna/nanoai/feature/uiux/ui/sidebar/RightSidebarPanels.kt`.  
  _Depends on_: T012, T024, T026
- [ ] T028 Refactor `feature/sidebar` components in `app/src/main/java/com/vjaykrsna/nanoai/ui/sidebar/` to consume new navigation data and highlight active routes.  
  _Depends on_: T022, T026, T027
- [ ] T029 Update `MainActivity.kt` and `app/src/main/java/com/vjaykrsna/nanoai/ui/navigation/NavigationScaffold.kt` to mount `NanoShellScaffold`, supply `WindowSizeClass`, and remove legacy per-screen scaffolds.  
  _Depends on_: T022, T026–T028
- [ ] T030 Refactor Chat UI (`app/src/main/java/com/vjaykrsna/nanoai/feature/chat/ui/ChatScreen.kt`) to use shell slots (header/composer) instead of its own `Scaffold`.  
  _Depends on_: T029
- [ ] T031 Refactor Model Library/History UI (`app/src/main/java/com/vjaykrsna/nanoai/feature/library/ui/ModelLibraryScreen.kt` and related) to rely on shell scaffolding and right drawer hooks.  
  _Depends on_: T029
- [ ] T032 Refactor Settings UI (`app/src/main/java/com/vjaykrsna/nanoai/feature/settings/ui/SettingsScreen.kt`) to match tabbed sections and shell layout, including persistent Save/Undo controls.  
  _Depends on_: T029

## Phase 3.4: Integration
- [ ] T033 Wire new repositories/providers in Hilt (`app/src/main/java/com/vjaykrsna/nanoai/core/di/PreferencesModule.kt`, `.../RepositoryModule.kt`) for `ShellStateRepository`, `CommandPaletteActionProvider`, and `ProgressCenterCoordinator`.  
  _Depends on_: T018–T021
- [ ] T034 Update navigation routing + deep links in `app/src/main/java/com/vjaykrsna/nanoai/ui/navigation/Screen.kt` and command palette registry so actions invoke the correct destinations.  
  _Depends on_: T019, T029–T032
- [ ] T035 Connect analytics/telemetry events for command palette usage, drawer toggles, and queued jobs in `app/src/main/java/com/vjaykrsna/nanoai/telemetry/` respecting consent flags.  
  _Depends on_: T017, T021, T024, T032
- [ ] T036 Refresh macrobenchmark assertions in `macrobenchmark/src/main/java/com/vjaykrsna/nanoai/macrobenchmark/NavigationBenchmarks.kt` and add new scenario for command palette open latency.  
  _Depends on_: T022–T035

## Phase 3.5: Polish
- [ ] T037 [P] Extend unit/UI tests for error + undo states (`ShellViewModelTest`, `CommandPaletteComposeTest`) to cover retries, disabled actions, and snackbar flows.  
  _Depends on_: T021–T025, T030–T032
- [ ] T038 [P] Perform accessibility pass (TalkBack order, semantics, contrast) across new composables in `feature/uiux/ui/` and `ui/sidebar/`.  
  _Depends on_: T022–T032
- [ ] T039 [P] Profile performance (JankStats, Compose metrics) and tune animations/transitions, updating Baseline Profile data and documenting outcomes.  
  _Depends on_: T036, T037–T038
- [ ] T040 Update documentation (`specs/003-UI-UX/quickstart.md`, `docs/ARCHITECTURE.md`, in-app help strings) to describe new shell, command palette, and offline flows.  
  _Depends on_: T022–T039
- [ ] T041 Execute manual QA checklist from quickstart on Pixel 7 + large-screen emulator, logging findings in `specs/003-UI-UX/logs/uiux-qa.md`.  
  _Depends on_: T040

## Parallel Execution Examples
- After completing T008, run data model implementations in parallel:
  - `task run T009`
  - `task run T010`
  - `task run T011`
  - `task run T012`
  - `task run T013`
  - `task run T014`
  - `task run T015`
- After finishing T029, refactor feature surfaces in parallel:
  - `task run T030`
  - `task run T031`
  - `task run T032`
- During polish, accessibility and performance work can proceed together:
  - `task run T038`
  - `task run T039`

```diff
Legend:
- [P] Tasks that can run in parallel.
- All other tasks must respect listed dependencies.
```
