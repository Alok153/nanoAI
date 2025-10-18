# Tasks: Improve Test Coverage for nanoAI - Phase 2

**Branch**: `005-improve-test-coverage`
**Current Status**: VM 65.63% (need 75%), UI 1.49% (need 65%), Data 10.21% (need 70%)
**Goal**: Achieve all coverage targets (VM ≥75%, UI ≥65%, Data ≥70%) and fix critical test suite issues

## Format: `[ID] [P?] [Priority] Description`
- **[P]**: Parallel (different files)
- **Priority**: HIGH/MEDIUM/LOW
- 1-3 lines per task max

## Phase 2.1: Foundation Fixes (BLOCKING)

**⚠️ CRITICAL**: Must complete before coverage expansion

**Purpose**: Fix critical blockers preventing test execution and establish reliable test infrastructure

- [ ] **T000** [HIGH] Fix AndroidTest compilation errors
  - Fix MockK dependency issues in androidTest
  - Resolve NoClassDefFoundError in instrumentation tests
  - Validate: `./gradlew ciManagedDeviceDebugAndroidTest` compiles

- [ ] **T001** [P] [HIGH] Refactor large test classes
  - Split `ModelCatalogRepositoryImplTest`, `SettingsScreenTest`, `ModelLibraryScreenTest` 
  - Reduce complexity violations (currently 29 Detekt issues)
  - Target: <300 lines per test class

- [ ] **T002** [P] [MEDIUM] Implement shared test fixtures
  - Create `DomainFixtures` for consistent test data
  - Add custom test rules and utilities
  - Establish test infrastructure in `shared/` directory

**Checkpoint Phase 2.1**: All tests compile and run reliably, test infrastructure established

---

## Phase 2.2: Data Layer Coverage (HIGH PRIORITY)

**Purpose**: Achieve Data layer 70% coverage by testing critical data components

**Impact**: Prevents data corruption, sync issues, database failures - highest impact for app stability

### Repository Tests (10.21% → 70%+)

- [ ] **T003** [P] [HIGH] Expand ConversationRepositoryImpl tests
  - Path: `app/src/test/java/com/vjaykrsna/nanoai/core/data/repository/impl/ConversationRepositoryImplTest.kt` (expand)
  - Test: syncConversations_handlesOfflineQueue, archiveConversation, deleteConversation_cascadesMessages, getConversationsPaginated

- [ ] **T004** [P] [HIGH] Expand PersonaRepositoryImpl tests
  - Path: `app/src/test/java/com/vjaykrsna/nanoai/core/data/repository/impl/PersonaRepositoryImplTest.kt` (expand)
  - Test: switchPersona_updatesActivePersona, switchPersona_persistsPreference, getPersonaById_handlesNotFound, listPersonas_ordersCorrectly

- [ ] **T005** [P] [HIGH] Create UserProfileRepositoryImpl additional tests
  - Path: `app/src/test/java/com/vjaykrsna/nanoai/core/data/repository/impl/UserProfileRepositoryImplTest.kt` (expand)
  - Test: updateProfile_validatesFields, updateProfile_handlesConflicts, deleteProfile_clearsAllData

### DAO Instrumentation Tests (0% → 90%+)

- [ ] **T006** [P] [HIGH] Create DownloadTaskDao instrumentation tests
  - Path: `app/src/androidTest/java/com/vjaykrsna/nanoai/feature/library/data/daos/DownloadTaskDaoTest.kt` (NEW)
  - Test: insert, update, delete, getTaskById, getAllTasks, getTasksByStatus

- [ ] **T007** [P] [HIGH] Create ModelMetadataDao instrumentation tests
  - Path: `app/src/androidTest/java/com/vjaykrsna/nanoai/feature/library/data/daos/ModelMetadataDaoTest.kt` (NEW)
  - Test: insertMetadata, updateMetadata, deleteMetadata, getMetadataByModelId, foreign key constraints

- [ ] **T008** [P] [MEDIUM] Expand ChatMessageDao tests for offline operations
  - Path: `app/src/test/java/com/vjaykrsna/nanoai/core/data/db/ChatMessageDaoTest.kt` (expand)
  - Test: getMessagesOffline_returnsQueuedMessages, markMessageSynced, getUnsentMessages_filtersCorrectly

**Checkpoint Phase 2.2**: Data layer coverage ≥70%, critical data operations tested and protected

---

## Phase 2.3: UI Layer Coverage (HIGH PRIORITY)

**Purpose**: Achieve UI layer 65% coverage through component and screen testing

**Impact**: Prevents UI crashes, validates accessibility, ensures Material Design compliance

### Screen Tests (1.49% → 65%+)

- [ ] **T009** [P] [HIGH] Create ModelLibraryScreen list tests
  - Path: `app/src/androidTest/java/com/vjaykrsna/nanoai/feature/library/ui/ModelLibraryScreenTest.kt` (NEW)
  - Test: libraryScreen_displaysModelList, libraryScreen_filterByProvider_updatesList, libraryScreen_searchModels_filtersCorrectly, libraryScreen_modelSections_renderCorrectly

- [ ] **T010** [P] [HIGH] ModelLibraryScreen download interaction tests
  - Path: Same as T009 (add methods)
  - Test: libraryScreen_downloadButton_startsDownload, libraryScreen_pauseButton_pausesDownload, libraryScreen_progressBar_updatesRealtime, libraryScreen_downloadStatus_showsCorrectState

- [ ] **T011** [P] [MEDIUM] ModelLibraryScreen offline tests
  - Path: Same as T009 (add methods)
  - Test: libraryScreen_offline_showsCatalogCached, libraryScreen_offline_disablesDownloads, libraryScreen_offline_showsBanner

### Component Tests (0% → 75%+)

- [ ] **T012** [P] [MEDIUM] Create CommandPalette tests
  - Path: `app/src/androidTest/java/com/vjaykrsna/nanoai/feature/uiux/ui/commandpalette/CommandPaletteTest.kt` (NEW)
  - Test: commandPalette_opens_onShortcut, commandPalette_search_filtersCommands, commandPalette_execution_triggersAction, commandPalette_keyboard_navigates

- [ ] **T013** [P] [MEDIUM] Create MessageComposer tests
  - Path: `app/src/androidTest/java/com/vjaykrsna/nanoai/feature/uiux/ui/components/composer/MessageComposerTest.kt` (NEW)
  - Test: messageComposer_input_updatesText, messageComposer_sendButton_enabledWhenNotEmpty, messageComposer_multiLine_expands, messageComposer_characterLimit_enforces

- [ ] **T014** [P] [MEDIUM] Create FeedbackSnackbar tests
  - Path: `app/src/androidTest/java/com/vjaykrsna/nanoai/feature/uiux/ui/components/feedback/FeedbackSnackbarTest.kt` (NEW)
  - Test: snackbar_success_showsCorrectStyle, snackbar_error_showsCorrectStyle, snackbar_autoDismiss_dismissesAfterDelay, snackbar_action_triggersCallback, snackbar_accessibility_announces

### Accessibility & Integration Tests

- [ ] **T015** [MEDIUM] Implement accessibility compliance tests
  - Path: `app/src/androidTest/java/com/vjaykrsna/nanoai/accessibility/` (NEW)
  - Test: TalkBack navigation, focus management, content descriptions, color contrast

**Checkpoint Phase 2.3**: UI layer coverage ≥65%, accessibility validated, Material Design compliance ensured

---

## Phase 2.4: ViewModel Completion & Integration (MEDIUM PRIORITY)

**Purpose**: Complete ViewModel coverage and add integration testing

**Impact**: Ensures robust state management and cross-layer integration

### ViewModel Edge Cases (65.63% → 75%+)

- [ ] **T016** [P] [MEDIUM] Expand ShellViewModel navigation tests
  - Path: `app/src/test/java/com/vjaykrsna/nanoai/feature/uiux/presentation/ShellViewModelTest.kt` (expand)
  - Test: navigateBack_handlesBackStack, toggleSidebar_updatesVisibility, toggleCompactMode_updatesLayout, handleDeepLink_navigatesCorrectly

- [ ] **T017** [P] [MEDIUM] Add ViewModel error state and configuration tests
  - Path: Various ViewModel test files (expand)
  - Test: processDeath_recovery, configurationChange_statePreservation, errorState_recovery, asyncCancellation_handling

### Integration Tests (0% → 50%+)

- [ ] **T018** [MEDIUM] Create cross-layer integration tests
  - Path: `app/src/androidTest/java/com/vjaykrsna/nanoai/integration/` (NEW)
  - Test: chatFlow_endToEnd, modelDownloadFlow_complete, offlineFallback_integration

**Checkpoint Phase 2.4**: All coverage targets met (VM ≥75%, UI ≥65%, Data ≥70%), integration tested

---

## Phase 2.5: Optimization & Documentation (LOW PRIORITY)

**Purpose**: Performance optimization, documentation, and final validation

- [ ] **T019** [P] [LOW] Create performance benchmarks
  - Path: `macrobenchmark/src/main/java/com/vjaykrsna/nanoai/PerformanceBenchmarks.kt`
  - Benchmarks: Chat message send (<500ms), Model list scroll (0 jank), App cold start (<2s)

- [ ] **T020** [P] [LOW] Update comprehensive test documentation
  - Path: `docs/testing.md` (expand)
  - Add: New test patterns, MockK troubleshooting, offline testing best practices, maintenance guidelines

- [ ] **T021** [LOW] Run comprehensive quickstart validation
  - Path: Follow `specs/005-improve-test-coverage/quickstart.md`
  - Validate: Commands execute, thresholds pass, documentation accuracy, update with Phase 2 findings

- [ ] **T022** [LOW] Generate final coverage reports and summary
  - Commands: `./gradlew jacocoFullReport`, `./gradlew verifyCoverageThresholds`, `./gradlew coverageMarkdownSummary`
  - Publish: `app/build/coverage/summary.md`, validate all thresholds met

**Checkpoint Phase 2.5**: All targets met, documentation complete, ready for stakeholder approval

---

## Critical Dependencies

**BLOCKING**: T000 must complete before any instrumentation tests (T006-T015)

**PARALLEL**: Data (T003-T008) + UI (T009-T015) can run simultaneously after foundation fixes

---

## Success Metrics

| Metric | Baseline | Target | Timeline |
| --- | --- | --- | --- |
| Data Coverage | 10.21% | ≥70% | 4 weeks |
| UI Coverage | 1.49% | ≥65% | 6 weeks |
| ViewModel Coverage | 65.63% | ≥75% | 8 weeks |
| Test Execution Time | ~10 min | <8 min | 8 weeks |
| Detekt Violations | 29 | <10 | 2 weeks |
| Test Reliability | ~90% | ≥95% | 8 weeks |

---

## Risk Register

| Risk ID | Severity | Description | Mitigation | Status |
|---------|----------|-------------|------------|--------|
| R2-001 | CRITICAL | Instrumentation doesn't compile | T000 | Open |
| R2-002 | CRITICAL | UI layer 1.49% coverage | T009-T015 | Open |
| R2-003 | HIGH | Data layer 10.21% coverage | T003-T008 | Open |
| R2-004 | HIGH | Large test classes (29 violations) | T001 | Open |
| R2-005 | MEDIUM | ViewModel 65.63% coverage | T016-T017 | Open |
| R2-006 | MEDIUM | Test organization issues | T002 | Open |
| R2-007 | LOW | Integration testing gaps | T018 | Open |

---

## Timeline (8 weeks, 3 developers)

**Week 1-2**: Foundation fixes (T000-T002) - All tests compile reliably
**Week 3-4**: Data layer coverage (T003-T008) - Data ≥70%
**Week 5-6**: UI layer coverage (T009-T015) - UI ≥65%
**Week 7-8**: ViewModel completion & integration (T016-T022) - All targets met

---

## Best Practices

- **[P]**: Parallel tasks touch different files
- **Priority**: HIGH = prevents breakage, MEDIUM = improves stability, LOW = polish
- **TDD**: Write failing tests first

---

## Quick Commands

```bash
# Unit tests
./gradlew testDebugUnitTest

# Instrumentation (after T000)
./gradlew ciManagedDeviceDebugAndroidTest

# Coverage
./gradlew jacocoFullReport
./gradlew verifyCoverageThresholds
cat app/build/coverage/summary.md

# Code quality
./gradlew detekt
./gradlew spotlessCheck
```

---

*Phase 2: Comprehensive Coverage • Updated 2025-10-17*
