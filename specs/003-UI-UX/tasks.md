# Tasks: UI/UX — Polished Product-Grade Experience

**Input**: Design documents from `/specs/003-UI-UX/`
**Prerequisites**: `plan.md`, `research.md`, `data-model.md`, `contracts/`, `quickstart.md`

## Execution Flow (main)
```
1. Load plan.md for architecture, tech stack, and performance budgets.
2. Parse research.md for Compose, theming, accessibility, and offline decisions.
3. Extract entities from data-model.md (UserProfile, LayoutSnapshot, UIStateSnapshot).
4. Map contracts/ (openapi.yaml, ui_contracts.md) into test stubs and implementation targets.
5. Capture quickstart.md scenarios as instrumentation plans.
6. Emit dependency-ordered tasks (Setup → Tests → Core → Integration → Polish) with [P] for parallel-safe items.
```

## Format: `[ID] [P?] Description`
- **[P]**: Can run in parallel (independent files, no shared dependency blockers)
- Include fully-qualified paths in every task description

## Path Conventions
- Source: `app/src/main/java/com/vjaykrsna/nanoai/`
- Unit tests: `app/src/test/java/com/vjaykrsna/nanoai/`
- Instrumentation: `app/src/androidTest/java/com/vjaykrsna/nanoai/`
- Macrobenchmark: `macrobenchmark/src/main/java/com/vjaykrsna/nanoai/`

# Phase 3.1 Setup
- [X] T001 Align `gradle/libs.versions.toml` with Material3 dynamic color, navigation-compose, lifecycle-runtime-compose, DataStore, Room (runtime + ksp), Coil, WindowSizeClass, Compose testing, macrobenchmark, and serialization versions required by the UI/UX plan.
- [X] T002 Update `app/build.gradle.kts` to apply Compose compiler metrics, register BaselineProfile variants, enable Room schema export, wire DataStore/Room/Coil dependencies, and expose macrobenchmark instrumentation targets.
- [X] T003 Harden `.github/workflows/android-ci.yml` so CI runs ktlint, Detekt, unit tests, Compose instrumentation (contracts + quickstart scenarios), and the UI/UX macrobenchmark smoke job.
- [X] T004 Extend `config/detekt/detekt.yml` to enable Compose-specific rules, accessibility checks, and include `feature/uiux`, `ui/components`, and new DAO packages in analysis.

## Phase 3.2 Tests First (TDD) ⚠️ Complete before any implementation
- [X] T005 [P] Author failing `/user/profile` contract test at `app/src/test/java/com/vjaykrsna/nanoai/feature/uiux/contracts/UserProfileContractTest.kt` validating `contracts/openapi.yaml` schema with MockWebServer + Kotlin Serialization.
- [X] T006 [P] Add Compose contract test `app/src/androidTest/java/com/vjaykrsna/nanoai/feature/uiux/contracts/WelcomeScreenContractTest.kt` covering hero copy, CTA semantics, and skip control per `ui_contracts.md`.
- [X] T007 [P] Add Compose contract test `.../feature/uiux/contracts/HomeScreenContractTest.kt` asserting single-column layout, recent-actions ordering, and collapsed tools behavior.
- [X] T008 [P] Add Compose contract test `.../feature/uiux/contracts/SidebarContractTest.kt` verifying DrawerState accessibility, keyboard navigation, and deep-link slotting.
- [X] T009 [P] Add Compose contract test `.../feature/uiux/contracts/SettingsScreenContractTest.kt` validating grouped settings cards, inline help, and undo affordances.
- [X] T010 [P] Add Compose contract test `.../feature/uiux/contracts/ThemeToggleContractTest.kt` ensuring instant theme switch, semantics, and persistence intents.
- [X] T011 [P] Add Compose contract test `.../feature/uiux/contracts/OfflineBannerContractTest.kt` covering offline messaging, disabled actions, and retry hook.
- [X] T012 [P] Add Compose contract test `.../feature/uiux/contracts/OnboardingTooltipContractTest.kt` verifying dismiss + "Don't show again" semantics and HELP re-entry.
- [X] T013 [P] Create unit test `app/src/test/java/com/vjaykrsna/nanoai/feature/uiux/domain/UserProfileModelTest.kt` enforcing validation rules (displayName length, pinnedTools ≤10, savedLayouts ≤5, dismissed tips map).
- [X] T014 [P] Create unit test `.../LayoutSnapshotModelTest.kt` checking layout name length, pinned tools cap, and compact flag consistency.
- [X] T015 [P] Create unit test `.../UIStateSnapshotModelTest.kt` verifying sidebar toggle persistence, recentActions rotation (max 5), and expanded panel dedupe.
- [X] T016 [P] Create unit test `app/src/test/java/com/vjaykrsna/nanoai/feature/uiux/domain/ObserveUserProfileUseCaseTest.kt` faking DAO + DataStore flows to assert merged state fidelity and offline cache hydration.
- [X] T017 [P] Create unit test `.../UpdateThemePreferenceUseCaseTest.kt` ensuring DataStore writes, repository sync, and notification to observers.
- [X] T018 [P] Create unit test `.../RecordOnboardingProgressUseCaseTest.kt` validating dismissed tip storage and onboarding completion flag.
- [X] T019 [P] Create unit test `.../ToggleCompactModeUseCaseTest.kt` verifying compact mode persistence and layout snapshot updates.
- [X] T020 [P] Create unit test `app/src/test/java/com/vjaykrsna/nanoai/feature/uiux/presentation/WelcomeViewModelTest.kt` asserting onboarding branching, CTA analytics events, and skip gating.
- [X] T021 [P] Create unit test `.../HomeViewModelTest.kt` covering recommended action ranking, offline banner state, and tooltip surfacing.
- [X] T022 [P] Extend `app/src/test/java/com/vjaykrsna/nanoai/feature/settings/presentation/SettingsViewModelUiUxTest.kt` to fail on missing theme toggle persistence, density toggles, and undo interactions.
- [X] T023 [P] Instrument Quickstart Scenario 1 in `app/src/androidTest/java/com/vjaykrsna/nanoai/feature/uiux/scenario/FirstTimeWelcomeScenarioTest.kt` following welcome journey expectations.
- [X] T024 [P] Instrument Scenario 2 in `.../HomeNavigationScenarioTest.kt` validating expand-tools flow, latency <100 ms, and action execution.
- [X] T025 [P] Instrument Scenario 3 in `.../SidebarSettingsScenarioTest.kt` verifying sidebar navigation, grouped settings, and inline help copy.
- [X] T026 [P] Instrument Scenario 4 in `.../ThemeToggleScenarioTest.kt` ensuring theme persistence across process death and no layout jump.
- [X] T027 [P] Instrument Scenario 5 in `.../OfflineModeScenarioTest.kt` enforcing offline banner display, disabled CTAs, and queued retries.
- [X] T028 [P] Instrument Scenario 6 in `.../AccessibilityScenarioTest.kt` validating TalkBack ordering, dynamic type support, and focus traps.
- [X] T029 [P] Add macrobenchmark `macrobenchmark/src/main/java/com/vjaykrsna/nanoai/uiux/UiUxStartupBenchmark.kt` asserting cold start <1.5 s, FMP ≤300 ms, interaction latency ≤100 ms.

## Phase 3.3 Core Implementation (only after tests fail)
- [X] T030 [P] Implement `core/domain/model/uiux/UserProfile.kt` with validation, conversion helpers, and Flow mappers to Room/DataStore.
- [X] T031 [P] Implement `core/domain/model/uiux/LayoutSnapshot.kt` encapsulating saved layout metadata and mapping helpers.
- [X] T032 [P] Implement `core/domain/model/uiux/UIStateSnapshot.kt` modeling expanded panels, recent actions rotation, and mapping helpers.
- [X] T033 Create `core/data/db/entities/UserProfileEntity.kt` plus `toDomain`/`fromDomain` conversions with type converters for enums and maps.
- [X] T034 Create `core/data/db/entities/LayoutSnapshotEntity.kt` modeling saved layouts with indices for quick lookup.
- [X] T035 Create `core/data/db/entities/UIStateSnapshotEntity.kt` persisting session state with foreign keys to `UserProfileEntity`.
- [X] T036 Create `core/data/db/daos/UserProfileDao.kt` exposing Flow CRUD, pinned tools updates, and compact mode toggles.
- [X] T037 Create `core/data/db/daos/LayoutSnapshotDao.kt` for CRUD, ordering, and pinned tool synchronization.
- [X] T038 Create `core/data/db/daos/UIStateSnapshotDao.kt` for session restoration and sidebar collapse persistence.
- [X] T039 Update `core/data/db/NanoAIDatabase.kt` to register new entities/DAOs, add migrations, and export updated schema JSON.
- [X] T040 Implement `core/data/preferences/UiPreferencesStore.kt` storing theme preference, visual density, onboarding completion, dismissed tips, and pinned tool ordering via Preferences DataStore.
- [X] T041 Add `core/data/preferences/UiPreferences.kt` data class + mapper bridging DataStore snapshots to domain `UserProfile` overlays.
- [X] T042 Create `core/network/dto/UserProfileDto.kt` with serialization annotations and mapping to domain models.
- [X] T043 Define Retrofit `core/network/UserProfileService.kt` exposing `GET /user/profile` with suspend function.
- [X] T044 Implement `app/src/main/java/com/vjaykrsna/nanoai/feature/uiux/data/UserProfileRemoteDataSource.kt` handling API fetch, error wrapping, and DTO conversion.
- [X] T045 Implement `.../feature/uiux/data/UserProfileLocalDataSource.kt` coordinating DAO + DataStore updates and cache hydration.
- [X] T046 Define `core/data/repository/UserProfileRepository.kt` interface covering observe/update theme, onboarding, pinned tools, and layout snapshots.
- [X] T047 Implement `core/data/repository/impl/UserProfileRepositoryImpl.kt` merging remote/local sources, ensuring offline-first behavior, and exposing Flow APIs.
- [ ] T048 [P] Implement `app/src/main/java/com/vjaykrsna/nanoai/feature/uiux/domain/ObserveUserProfileUseCase.kt` wiring repository Flow into UI state mediator with error handling.
- [ ] T049 [P] Implement `.../UpdateThemePreferenceUseCase.kt` orchestrating DataStore writes and repository refresh triggers.
- [ ] T050 [P] Implement `.../RecordOnboardingProgressUseCase.kt` persisting dismissed tooltips and onboarding completion flag.
- [ ] T051 [P] Implement `.../ToggleCompactModeUseCase.kt` flipping density preference and syncing with layout snapshots.
- [ ] T052 [P] Implement `app/src/main/java/com/vjaykrsna/nanoai/feature/uiux/presentation/WelcomeViewModel.kt` coordinating onboarding, CTA navigation events, and analytics hooks.
- [ ] T053 [P] Implement `.../presentation/HomeViewModel.kt` combining recommended actions, offline banner state, and tooltip surfacing from repository + connectivity provider.
- [ ] T054 Extend `app/src/main/java/com/vjaykrsna/nanoai/feature/settings/presentation/SettingsViewModel.kt` to use UI preference use cases, expose theme/density toggles, and provide undo operations.
- [ ] T055 Extend `app/src/main/java/com/vjaykrsna/nanoai/feature/sidebar/presentation/SidebarViewModel.kt` with DrawerState, pinned tool ordering, and navigation intents for new sidebar contracts.
- [ ] T056 [P] Build `app/src/main/java/com/vjaykrsna/nanoai/feature/uiux/ui/WelcomeScreen.kt` Compose screen implementing hero layout, CTA buttons, skip inline link, and tooltip entry points.
- [ ] T057 [P] Build `.../feature/uiux/ui/HomeScreen.kt` Compose screen with single-column feed, recommended action cards, skeleton loaders, and collapsible tools rail.
- [ ] T058 [P] Build `app/src/main/java/com/vjaykrsna/nanoai/feature/sidebar/ui/SidebarDrawer.kt` Compose drawer handling breakpoint-specific behavior and accessibility semantics.
- [ ] T059 Update `app/src/main/java/com/vjaykrsna/nanoai/ui/navigation/Screen.kt` and `NavigationScaffold.kt` to register welcome/home routes, manage DrawerState, and inject ViewModels via Hilt.
- [ ] T060 [P] Create `app/src/main/java/com/vjaykrsna/nanoai/ui/components/ThemeToggle.kt` implementing manual/system toggle, animations, and semantics per contract.
- [ ] T061 [P] Create `.../ui/components/OfflineBanner.kt` exposing status messaging, retry callback, and queue indicator.
- [ ] T062 [P] Create `.../ui/components/OnboardingTooltip.kt` providing dismiss + "don't show again" behavior with Compose semantics.
- [ ] T063 [P] Create `.../ui/components/PrimaryActionCard.kt` for recommended actions with iconography, semantics, and haptic feedback hooks.
- [ ] T064 Update `app/src/main/java/com/vjaykrsna/nanoai/ui/theme/Color.kt`, `Theme.kt`, and `Type.kt` to define Material 3 tokens, spacing, elevation, and dynamic color fallbacks aligned with research.md budgets.
- [ ] T065 Update `app/src/main/java/com/vjaykrsna/nanoai/MainActivity.kt` to observe theme preference flows, gate welcome vs home navigation, and surface skeleton state while caches hydrate.

## Phase 3.4 Integration
- [ ] T066 Update `app/src/main/java/com/vjaykrsna/nanoai/core/di/DatabaseModule.kt` to provide new DAOs and migrate schema version for UserProfile/UI state tables.
- [ ] T067 Update `app/src/main/java/com/vjaykrsna/nanoai/core/di/RepositoryModule.kt` binding `UserProfileRepositoryImpl`, local, and remote data sources.
- [ ] T068 Update `app/src/main/java/com/vjaykrsna/nanoai/core/di/NetworkModule.kt` to supply `UserProfileService` Retrofit client with JSON serialization config.
- [ ] T069 Add or extend `app/src/main/java/com/vjaykrsna/nanoai/core/di/PreferencesModule.kt` providing singleton `UiPreferencesStore` and converters.
- [ ] T070 Implement `app/src/main/java/com/vjaykrsna/nanoai/feature/uiux/data/SyncUiStateWorker.kt` to queue layout snapshot sync and tooltip dismissal uploads via WorkManager.
- [ ] T071 Update `app/src/main/java/com/vjaykrsna/nanoai/core/di/WorkerModule.kt` to bind `SyncUiStateWorker` with Hilt and schedule periodic sync respecting offline constraints.

## Phase 3.5 Polish
- [ ] T072 [P] Add Compose visual regression `app/src/androidTest/java/com/vjaykrsna/nanoai/feature/uiux/visual/ThemeToggleVisualTest.kt` capturing light/dark snapshots for accessibility review.
- [ ] T073 Address accessibility findings: update `feature/uiux/ui/*` and `ui/components/*` with TalkBack labels, contentDescription, focus order, and large-text support per WCAG AA.
- [ ] T074 Tune performance budgets by updating `macrobenchmark/src/main/java/com/vjaykrsna/nanoai/uiux/UiUxStartupBenchmark.kt` thresholds and generating Baseline Profiles for welcome/home flows.
- [ ] T075 Refresh `specs/003-UI-UX/quickstart.md` with new instrumentation commands, offline validation notes, and expected screenshots.
- [ ] T076 Document UI/UX data flow and `/user/profile` endpoint in `docs/ARCHITECTURE.md` and `docs/API.md`, including caching and privacy notes.
- [ ] T077 Record validation outcomes in `specs/003-UI-UX/validation/uiux-validation.md`, referencing quickstart scenarios and benchmark results.
- [ ] T078 [P] Log manual QA results (accessibility, performance, theme checks) in `specs/003-UI-UX/logs/uiux-qa.md` with timestamps and device details.

## Dependencies
- T001–T004 must complete before any test task (T005–T029).
- Contract, model, use case, ViewModel, and scenario tests (T005–T029) must fail green before implementing corresponding code (T030+).
- Domain models (T030–T032) unblock Room entities/DAOs (T033–T039), which unblock repository/data layer tasks (T040–T047).
- Use cases (T048–T051) depend on repository completion (T046–T047) and gate ViewModel layer (T052–T055).
- ViewModels feed UI component tasks (T056–T065); navigation updates (T059) depend on ViewModels and components.
- DI + Worker wiring (T066–T071) depends on data layer and ViewModel availability.
- Polish tasks (T072–T078) run only after integration tasks succeed and instrumentation benchmarks exist.

## Parallel Execution Examples
```
# Run contract Compose tests together once setup is ready
task start T006
task start T007
task start T008
task wait T006 T007 T008

# Parallelize domain model creation after tests are red
task start T030
task start T031
task start T032
task wait T030 T031 T032
```

## Notes
- Keep [P] tasks limited to independent files to avoid merge conflicts; drop marker if scope changes.
- Ensure each test added in Phase 3.2 fails before implementing code that satisfies it (TDD discipline).
- Reference constitution performance, accessibility, and privacy gates while executing implementation and polish phases.
