# Tasks: Improve Test Coverage for nanoAI

**Input**: Design documents from `/specs/005-improve-test-coverage/`
**Prerequisites**: plan.md (required), research.md, data-model.md, contracts/ 

## Task List

- [x] **T001** Update `.github/workflows/ci.yml` to run `./gradlew ciManagedDeviceDebugAndroidTest jacocoFullReport verifyCoverageThresholds` on a virtualization-enabled runner and upload merged coverage XML/HTML plus `app/build/coverage/summary.md` as CI artifacts. (Already configured)
- [x] **T002 [P]** Extend `config/coverage/layer-map.json` with patterns for new `feature/chat`, `feature/library`, `feature/settings`, and `coverage/ui` test packages so JaCoCo layer classification tracks upcoming suites (ViewModel/UI/Data). (Already configured)
- [x] **T003 [P]** Create reusable JVM test fixtures in `app/src/test/java/com/vjaykrsna/nanoai/testing/` (MainDispatcherRule, fake repositories/use cases, domain builders) to support high-traffic ViewModel coverage.
- [x] **T004 [P]** Add Compose instrumentation harness in `app/src/androidTest/java/com/vjaykrsna/nanoai/testing/ComposeTestHarness.kt` exposing accessibility matchers, `TestEnvironmentRule` wiring, and managed-device helpers shared by new UI tests.
- [x] **T005 [P]** Author failing unit tests `app/src/test/java/com/vjaykrsna/nanoai/feature/chat/presentation/ChatViewModelTest.kt` covering thread selection, send success/error, persona switch (START_NEW_THREAD), and archive/delete flows.
- [x] **T006 [P]** Author failing unit tests `app/src/test/java/com/vjaykrsna/nanoai/feature/library/presentation/ModelLibraryViewModelTest.kt` covering refreshCatalog offline fallback, filter toggles, download monitoring, and error propagation.
- [x] **T007 [P]** Author failing unit tests `app/src/test/java/com/vjaykrsna/nanoai/feature/settings/presentation/SettingsViewModelTest.kt` asserting Hugging Face slow-down announcements, privacy retention updates, and undo flows reach the UI state.
- [x] **T008 [P]** Add Compose instrumentation suite `app/src/androidTest/java/com/vjaykrsna/nanoai/feature/chat/ui/ChatScreenTest.kt` validating TalkBack semantics, send button enablement, offline banner copy, and loading indicators.
- [x] **T009 [P]** Add Compose suite `app/src/androidTest/java/com/vjaykrsna/nanoai/feature/library/ui/ModelLibraryScreenTest.kt` covering filter chips (provider, capability), download status semantics (Available, Downloading, Installed, Failed), and sections (Needs Attention, Installed, Available).
- [x] **T010 [P]** Add Compose suite `app/src/androidTest/java/com/vjaykrsna/nanoai/feature/settings/ui/SettingsScreenTest.kt` validating HuggingFace auth dialogs (OAuth flow, API key input), export/import snackbar messaging, privacy toggle accessibility (telemetry, retention policy).
- [x] **T011 [P]** Add JUnit5 suite `app/src/test/java/com/vjaykrsna/nanoai/core/data/repository/impl/ConversationRepositoryImplTest.kt` covering Room DAO CRUD, cascade delete, reactive flows (Turbine).
- [x] **T012 [P]** Create Room + filesystem instrumentation tests `app/src/androidTest/java/com/vjaykrsna/nanoai/feature/library/data/ModelCatalogRepositoryImplTest.kt` covering replaceCatalog state preservation, offline fallback recording, and deleteModelFiles cleanup.
- [x] **T013 [P]** Add DataStore unit tests `app/src/test/java/com/vjaykrsna/nanoai/core/data/preferences/PrivacyPreferenceStoreTest.kt` for telemetry opt-in, retention policy, and consent acknowledgement flows.
- [x] **T014 [P]** Extend `app/src/test/java/com/vjaykrsna/nanoai/coverage/RiskRegisterCoordinatorTest.kt` with a failing scenario reproducing RR-HIGH-027 (release-style target build `r2025.43`, uppercase risk id) to keep mitigated risks from triggering `requiresAttention`.
- [x] **T015** Update `app/src/main/java/com/vjaykrsna/nanoai/coverage/domain/RiskRegisterCoordinator.kt` to normalise release-style build identifiers and mitigation tags so T014 passes and RR-HIGH-027 is resolved.
- [x] **T016** Refactor `app/src/main/java/com/vjaykrsna/nanoai/feature/chat/presentation/ChatViewModel.kt` to inject coroutine dispatchers/test hooks, emit `ChatError.ThreadCreationFailed` when no thread is active, and expose hot flows in support of T005.
- [x] **T017** Refine `app/src/main/java/com/vjaykrsna/nanoai/feature/library/presentation/ModelLibraryViewModel.kt` to differentiate initial loading vs refresh states, surface offline fallback errors, and dispose download observers per T006 coverage.
- [x] **T018** Update `app/src/main/java/com/vjaykrsna/nanoai/feature/settings/presentation/SettingsViewModel.kt` to propagate Hugging Face device-auth announcements/countdowns into UI state and clear transient status once consumed (satisfying T007).
- [ ] **T019** Adjust data repositories (`app/src/main/java/com/vjaykrsna/nanoai/feature/library/data/impl/ModelCatalogRepositoryImpl.kt`, `app/src/main/java/com/vjaykrsna/nanoai/core/data/repository/impl/ConversationRepositoryImpl.kt`) to record refresh metadata, expose deterministic flows, and match instrumentation coverage expectations from T011–T012. *(In progress — flow/refresh wiring updated, but Hilt/KSP now fails on `ModelCatalogRepositoryImpl` constructor defaults; revert or provide explicit provider before marking complete.)*
- [ ] **T020** Introduce coverage dashboard presenter (`app/src/main/java/com/vjaykrsna/nanoai/coverage/presentation/CoverageDashboardViewModel.kt`) plus DI wiring (`app/src/main/java/com/vjaykrsna/nanoai/di/CoverageModule.kt`) to fetch reports, merge trend data, and surface offline banners using `CoverageDashboardBanner.offline`, closing RR-CRIT-041. *(In progress — presenter + repository added, but detekt violations and asset/DI integration need follow-up before completion.)*
- [ ] **T021** Update coverage publishing pipeline (`scripts/coverage/merge-coverage.sh`, `.github/workflows/ci.yml`) to post merged coverage summaries/JSON to CI artifacts and PR comments, ensuring stakeholders see threshold status.
- [ ] **T022** Run `./gradlew ciManagedDeviceDebugAndroidTest jacocoFullReport verifyCoverageThresholds` and attach HTML/XML plus `app/build/coverage/summary.md` to the PR validating thresholds (≥75/65/70) are met.
- [ ] **T023** Refresh documentation (`docs/todo-next.md`, `docs/coverage/risk-register.md`, `specs/005-improve-test-coverage/quickstart.md`) with new coverage percentages, resolved risks (RR-CRIT-041, RR-HIGH-027, RR-HIGH-033), and updated workflow guidance.

## Dependencies

- T002 depends on T001 for updated CI naming when exporting artifacts.
- T005 depends on T003; T006 depends on T003; T007 depends on T003.
- T008 depends on T004; T009 depends on T004; T010 depends on T004.
- T011 depends on T004; T012 depends on T004; T013 depends on T003.
- T014 depends on T002 (layer mapping) and T003 (test fixtures).
- T015 depends on T014.
- T016 depends on T005; T017 depends on T006; T018 depends on T007.
- T019 depends on T011 and T012.
- T020 depends on T015 and existing instrumentation harness (T004).
- T021 depends on T001 and T020.
- T022 depends on T015–T021.
- T023 depends on T022.

## Parallel Execution Examples

- After completing T003, run multiple ViewModel test authoring in parallel:
  - `taskctl run --parallel T005 T006 T007`
- Once T004 is finished, execute UI/data instrumentation tasks together:
  - `taskctl run --parallel T008 T009 T010`
- When repository instrumentation scaffolding (T011, T012) is ready, iterate on data fixes while coverage pipeline work proceeds:
  - `taskctl run --parallel T019 T021`
