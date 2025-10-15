# Tasks: Improve Test Coverage for nanoAI - Phase 2

**Branch**: `005-improve-test-coverage`
**Current Status**: VM 66.76% (need 75%), UI 1.80% (need 65%), Data 8.51% (need 70%)
**Goal**: Close 864 untested classes to meet thresholds

## Format: `[ID] [P?] [Priority] Description`
- **[P]**: Parallel (different files)
- **Priority**: HIGH/MEDIUM/LOW
- 1-3 lines per task max

## Phase 2.1: Critical Infrastructure Fixes (BLOCKING)

**⚠️ CRITICAL**: Must complete before UI testing

- [ ] **T024** [HIGH] Fix AndroidTest compilation errors
  - Fix MockK dependency issues in androidTest
  - Resolve NoClassDefFoundError in ChatScreenTest
  - Validate: `./gradlew ciManagedDeviceDebugAndroidTest` compiles

- [ ] **T025** [P] [HIGH] Fix CoverageDashboardTest failures
  - Fix text assertion mismatches in offlineFallback and coverageLayers tests
  - Update expected strings and semantic tree checks

- [ ] **T026** [P] [HIGH] Fix ModelCatalogOfflineTest failure
  - Fix null assertion in offlineRefresh_preservesCachedCatalogAndSignalsFallbackSuccess
  - Review offline catalog caching logic

---

## Phase 2.2: Data Layer - Repository & Download Management (HIGH PRIORITY)

**Purpose**: Achieve Data layer 70% coverage by testing critical download and repository components (currently 0% coverage)

**Impact**: Prevents download failures, sync issues, database corruption - highest impact for app stability

### Download Manager Coverage (0% → 80%+)

- [ ] **T027** [P] [HIGH] Create DownloadManagerImpl unit tests - Basic operations
  - Path: `app/src/test/java/com/vjaykrsna/nanoai/feature/library/data/impl/DownloadManagerImplTest.kt` (NEW)
  - Test: startDownload, queueDownload, pauseDownload, resumeDownload, cancelDownload

- [ ] **T028** [P] [HIGH] DownloadManagerImpl unit tests - Progress & Status
  - Path: Same as T027 (add methods)
  - Test: observeProgress, getDownloadStatus, getActiveDownloads, getQueuedDownloads, getTaskById

- [ ] **T029** [P] [HIGH] DownloadManagerImpl unit tests - Error & Retry
  - Path: Same as T027 (add methods)
  - Test: retryDownload, resetTask, startDownload_handlesNetworkError, queueDownload_handlesDiskSpaceError

### Model Catalog Repository Coverage (0% → 85%+)

- [ ] **T030** [P] [HIGH] ModelCatalogRepositoryImpl unit tests - CRUD operations
  - Path: `app/src/test/java/com/vjaykrsna/nanoai/feature/library/data/impl/ModelCatalogRepositoryImplTest.kt` (expand)
  - Test: getAllModels, getModelById, getInstalledModels, getModelsByState, getModel

- [ ] **T031** [P] [HIGH] ModelCatalogRepositoryImpl unit tests - Catalog refresh
  - Path: Same as T030 (add methods)
  - Test: replaceCatalog, refreshCatalog, refreshOffline, refreshCatalog_handlesVersionMismatch

- [ ] **T032** [P] [HIGH] ModelCatalogRepositoryImpl unit tests - Session tracking
  - Path: Same as T030 (add methods)
  - Test: isModelActiveInSession, setActiveModel, clearActiveModel

### Catalog Source Coverage (0% → 80%+)

- [ ] **T033** [P] [MEDIUM] AssetModelCatalogSource unit tests
  - Path: `app/src/test/java/com/vjaykrsna/nanoai/feature/library/data/catalog/AssetModelCatalogSourceTest.kt` (NEW)
  - Test: fetchCatalog_parsesAssetsCorrectly, fetchCatalog_handlesMalformedJson, fetchCatalog_handlesCompanionObject

- [ ] **T034** [P] [MEDIUM] Catalog config unit tests
  - Path: `app/src/test/java/com/vjaykrsna/nanoai/feature/library/data/catalog/CatalogConfigTest.kt` (expand)
  - Test: ModelConfig_parsesAllFields, ModelConfig_validatesRequiredFields, ModelCatalogConfig_handlesVersioning, Companion_providesDefaults

### Repository Expansion (Partial → 85%+)

- [ ] **T035** [P] [MEDIUM] Expand ConversationRepositoryImpl tests
  - Path: `app/src/test/java/com/vjaykrsna/nanoai/core/data/repository/impl/ConversationRepositoryImplTest.kt` (expand)
  - Test: syncConversations_handlesOfflineQueue, archiveConversation, deleteConversation_cascadesMessages, getConversationsPaginated

- [ ] **T036** [P] [MEDIUM] Expand PersonaRepositoryImpl tests
  - Path: `app/src/test/java/com/vjaykrsna/nanoai/core/data/repository/impl/PersonaRepositoryImplTest.kt` (expand)
  - Test: switchPersona_updatesActivePersona, switchPersona_persistsPreference, getPersonaById_handlesNotFound, listPersonas_ordersCorrectly

- [ ] **T037** [P] [MEDIUM] Create UserProfileRepositoryImpl additional tests
  - Path: `app/src/test/java/com/vjaykrsna/nanoai/core/data/repository/impl/UserProfileRepositoryImplTest.kt` (expand)
  - Test: updateProfile_validatesFields, updateProfile_handlesConflicts, deleteProfile_clearsAllData

### DAO Instrumentation Tests (Partial → 90%+)

- [ ] **T038** [P] [MEDIUM] Create DownloadTaskDao instrumentation tests
  - Path: `app/src/androidTest/java/com/vjaykrsna/nanoai/feature/library/data/daos/DownloadTaskDaoTest.kt` (NEW)
  - Test: insert, update, delete, getTaskById, getAllTasks, getTasksByStatus

- [ ] **T039** [P] [MEDIUM] Create ModelMetadataDao instrumentation tests
  - Path: `app/src/androidTest/java/com/vjaykrsna/nanoai/feature/library/data/daos/ModelMetadataDaoTest.kt` (NEW)
  - Test: insertMetadata, updateMetadata, deleteMetadata, getMetadataByModelId, foreign key constraints

- [ ] **T040** [P] [MEDIUM] Expand ChatMessageDao tests for offline operations
  - Path: `app/src/test/java/com/vjaykrsna/nanoai/core/data/db/ChatMessageDaoTest.kt` (expand)
  - Test: getMessagesOffline_returnsQueuedMessages, markMessageSynced, getUnsentMessages_filtersCorrectly

**Checkpoint Phase 2.2**: Data layer coverage ≥70%, download and repository operations tested and protected

---

## Phase 2.3: ViewModel Layer - State Management & Error Handling (MEDIUM PRIORITY)

**Purpose**: Achieve ViewModel layer 75% coverage by testing uncovered ViewModels (currently 66.76%)

**Impact**: Prevents UI state bugs, race conditions, and improves error handling

### Settings ViewModel Expansion (Partial → 90%+)

- [ ] **T041** [P] [MEDIUM] Expand SettingsViewModel API provider tests
  - Path: `app/src/test/java/com/vjaykrsna/nanoai/feature/settings/presentation/SettingsViewModelTest.kt` (expand)
  - Test: addApiProvider_emitsProviderAddFailedOnError, updateApiProvider_emitsProviderUpdateFailedOnError, deleteApiProvider_emitsProviderDeleteFailedOnError, validateProvider_checksRequiredFields

- [ ] **T042** [P] [MEDIUM] SettingsViewModel HuggingFace integration tests
  - Path: Same as T041 (add methods)
  - Test: refreshHuggingFaceAccount_updatesAuthState, refreshHuggingFaceAccount_handlesAuthFailure, huggingFaceOAuth_completesSuccessfully

- [ ] **T043** [P] [MEDIUM] SettingsViewModel export/import tests
  - Path: Same as T041 (add methods)
  - Test: dismissExportWarnings_clearsState, exportData_triggersExportFlow, importData_handlesValidation

### App ViewModel Tests (0% → 85%+)

- [ ] **T044** [P] [HIGH] Create AppViewModel unit tests
  - Path: `app/src/test/java/com/vjaykrsna/nanoai/AppViewModelTest.kt` (NEW)
  - Test: onDisclaimerAccepted_persistsConsent, onDisclaimerAccepted_navigatesToMain, initialization_loadsConfiguration, initialization_handlesUnexpectedError, initialization_checksFirstLaunch

### Library ViewModel Expansion (Partial → 90%+)

- [ ] **T045** [P] [MEDIUM] Expand ModelLibraryViewModel download tests
  - Path: `app/src/test/java/com/vjaykrsna/nanoai/feature/library/presentation/ModelLibraryViewModelTest.kt` (expand)
  - Test: downloadModel_tracksProgressCorrectly, pauseDownload_updatesStateImmediately, resumeDownload_continuesFromLastState, cancelDownload_cleansUpResources, handleMultipleDownloads_coordinatesCorrectly

- [ ] **T046** [P] [MEDIUM] ModelLibraryViewModel catalog and filtering tests
  - Path: Same as T045 (add methods)
  - Test: refreshCatalog_updatesStateOnSuccess, refreshCatalog_showsOfflineFallbackOnFailure, filterByProvider_updatesVisibleModels, filterByCapability_combinesFilters, searchModels_filtersCorrectly

### Chat ViewModel Expansion (Partial → 90%+)

- [ ] **T047** [P] [MEDIUM] Expand ChatViewModel composition tests
  - Path: `app/src/test/java/com/vjaykrsna/nanoai/feature/chat/presentation/ChatViewModelTest.kt` (expand)
  - Test: sendMessage_validatesInputNotEmpty, sendMessage_handlesStreamingResponse, sendMessage_emitsErrorOnNetworkFailure, cancelMessage_stopsStreaming

- [ ] **T048** [P] [MEDIUM] ChatViewModel persona and thread tests
  - Path: Same as T047 (add methods)
  - Test: switchPersona_maintainsConversationContext, switchThread_loadsMessagesCorrectly, createNewThread_initializesEmptyState, deleteThread_cleansUpAndNavigates

### Shell ViewModel Expansion (Partial → 90%+)

- [ ] **T049** [P] [LOW] Expand ShellViewModel navigation tests
  - Path: `app/src/test/java/com/vjaykrsna/nanoai/feature/uiux/presentation/ShellViewModelTest.kt` (expand)
  - Test: navigateBack_handlesBackStack, toggleSidebar_updatesVisibility, toggleCompactMode_updatesLayout, handleDeepLink_navigatesCorrectly

**Checkpoint Phase 2.3**: ViewModel layer coverage ≥75%, state management tested, error scenarios validated

---

## Phase 2.4: UI Layer - Screens & Components (HIGH PRIORITY)

**Purpose**: Achieve UI layer 65% coverage by testing critical screens and components (currently 1.80%)

**Depends on**: T024 (instrumentation compilation fix)

**Impact**: Prevents UI crashes, validates accessibility, ensures Material Design compliance

### Sidebar UI Tests (0% → 80%+)

- [ ] **T050** [P] [HIGH] Create Sidebar thread list tests
  - Path: `app/src/androidTest/java/com/vjaykrsna/nanoai/ui/sidebar/SidebarContentTest.kt` (NEW)
  - Test: sidebarThreadList_displaysThreads, sidebarThreadList_emptyStateShowsMessage, sidebarThreadList_threadSelectionNavigates, sidebarThreadList_hasAccessibleLabels

- [ ] **T051** [P] [HIGH] Sidebar interaction tests
  - Path: Same as T050 (add methods)
  - Test: sidebarNewConversation_createsThread, sidebarThreadDelete_showsConfirmation, sidebarThreadSwipe_revealsActions, sidebarTalkBack_announcesCorrectly

- [ ] **T052** [P] [MEDIUM] Sidebar offline tests
  - Path: Same as T050 (add methods)
  - Test: sidebarOffline_showsIndicator, sidebarOffline_displaysCachedThreads, sidebarOffline_disablesSync

### Chat Screen Fixes & Expansion (Many failures → 95%+)

- [ ] **T053** [HIGH] Fix ChatScreenTest compilation and setup issues
  - Path: `app/src/androidTest/java/com/vjaykrsna/nanoai/feature/chat/ui/ChatScreenTest.kt` (fix existing)
  - Fix MockK errors and setup issues, ensure all 15 existing tests pass (depends on T024)

- [ ] **T054** [P] [MEDIUM] ChatScreen additional scenarios
  - Path: Same as T053 (add methods after fixes)
  - Test: chatScreen_multiModalMessage_rendersCorrectly, chatScreen_longMessage_scrollsAndTruncates, chatScreen_darkMode_rendersCorrectly

### Settings Screen Tests (0% → 80%+)

- [ ] **T055** [P] [HIGH] Create SettingsScreen provider management tests
  - Path: `app/src/androidTest/java/com/vjaykrsna/nanoai/feature/settings/ui/SettingsScreenTest.kt` (NEW)
  - Test: settingsScreen_displaysProviderList, settingsScreen_addProviderDialog_showsForm, settingsScreen_editProvider_updatesData, settingsScreen_deleteProvider_showsConfirmation

- [ ] **T056** [P] [HIGH] SettingsScreen HuggingFace auth tests
  - Path: Same as T055 (add methods)
  - Test: settingsScreen_huggingFaceLogin_triggersOAuth, settingsScreen_huggingFaceAccount_displaysInfo, settingsScreen_huggingFaceLogout_clearsCredentials

- [ ] **T057** [P] [MEDIUM] SettingsScreen accessibility tests
  - Path: Same as T055 (add methods)
  - Test: settingsScreen_allControls_haveSemantic, settingsScreen_talkBack_navigatesCorrectly, settingsScreen_dynamicType_adjustsCorrectly, settingsScreen_colorContrast_meetsStandards

### Library Screen Tests (0% → 80%+)

- [ ] **T058** [P] [HIGH] Create ModelLibraryScreen list tests
  - Path: `app/src/androidTest/java/com/vjaykrsna/nanoai/feature/library/ui/ModelLibraryScreenTest.kt` (NEW)
  - Test: libraryScreen_displaysModelList, libraryScreen_filterByProvider_updatesList, libraryScreen_searchModels_filtersCorrectly, libraryScreen_modelSections_renderCorrectly

- [ ] **T059** [P] [HIGH] ModelLibraryScreen download interaction tests
  - Path: Same as T058 (add methods)
  - Test: libraryScreen_downloadButton_startsDownload, libraryScreen_pauseButton_pausesDownload, libraryScreen_progressBar_updatesRealtime, libraryScreen_downloadStatus_showsCorrectState

- [ ] **T060** [P] [MEDIUM] ModelLibraryScreen offline tests
  - Path: Same as T058 (add methods)
  - Test: libraryScreen_offline_showsCatalogCached, libraryScreen_offline_disablesDownloads, libraryScreen_offline_showsBanner

### UI Component Tests (0% → 75%+)

- [ ] **T061** [P] [MEDIUM] Create CommandPalette tests
  - Path: `app/src/androidTest/java/com/vjaykrsna/nanoai/feature/uiux/ui/commandpalette/CommandPaletteTest.kt` (NEW)
  - Test: commandPalette_opens_onShortcut, commandPalette_search_filtersCommands, commandPalette_execution_triggersAction, commandPalette_keyboard_navigates

- [ ] **T062** [P] [MEDIUM] Create MessageComposer tests
  - Path: `app/src/androidTest/java/com/vjaykrsna/nanoai/feature/uiux/ui/components/composer/MessageComposerTest.kt` (NEW)
  - Test: messageComposer_input_updatesText, messageComposer_sendButton_enabledWhenNotEmpty, messageComposer_multiLine_expands, messageComposer_characterLimit_enforces

- [ ] **T063** [P] [MEDIUM] Create FeedbackSnackbar tests
  - Path: `app/src/androidTest/java/com/vjaykrsna/nanoai/feature/uiux/ui/components/feedback/FeedbackSnackbarTest.kt` (NEW)
  - Test: snackbar_success_showsCorrectStyle, snackbar_error_showsCorrectStyle, snackbar_autoDismiss_dismissesAfterDelay, snackbar_action_triggersCallback, snackbar_accessibility_announces

### Offline Flow Integration Tests (Skipped → 100%)

- [ ] **T064** [MEDIUM] Fix and expand ModelCatalogOfflineTest
  - Path: `app/src/androidTest/java/com/vjaykrsna/nanoai/feature/library/data/ModelCatalogOfflineTest.kt` (fix existing)
  - Fix offlineRefresh_preservesCachedCatalogAndSignalsFallbackSuccess assertion (depends on T026), add offline scenarios

- [ ] **T065** [MEDIUM] Implement PersonaOfflineFlow tests
  - Path: `app/src/androidTest/java/com/vjaykrsna/nanoai/feature/chat/PersonaOfflineFlowTest.kt` (un-skip existing)
  - Un-skip personaFlowsPending, add offline persona tests

- [ ] **T066** [MEDIUM] Implement ModelLibraryOfflineFlow tests
  - Path: `app/src/androidTest/java/com/vjaykrsna/nanoai/feature/library/ModelLibraryFlowTest.kt` (un-skip existing)
  - Un-skip modelLibraryFlowsPending, add offline library tests

**Checkpoint Phase 2.4**: UI layer coverage ≥65%, critical screens tested, accessibility validated, offline flows working

---

## Phase 2.5: Cross-Cutting Concerns & Validation (LOW PRIORITY)

**Purpose**: Polish, documentation, and final validation

- [ ] **T067** [P] [LOW] Expand HuggingFaceAuthCoordinator tests
  - Path: `app/src/test/java/com/vjaykrsna/nanoai/feature/settings/domain/huggingface/HuggingFaceAuthCoordinatorTest.kt` (expand)
  - Test: tokenRefresh_renews_beforeExpiry, oauthFlow_handlesUserCancellation, networkError_retriesWithBackoff

- [ ] **T068** [P] [LOW] Create performance benchmarks
  - Path: `macrobenchmark/src/main/java/com/vjaykrsna/nanoai/PerformanceBenchmarks.kt`
  - Benchmarks: Chat message send (<500ms), Model list scroll (0 jank), App cold start (<2s)

- [ ] **T069** [P] [LOW] Update comprehensive test documentation
  - Path: `docs/testing.md` (expand)
  - Add: New test patterns, MockK troubleshooting, offline testing best practices, maintenance guidelines

- [ ] **T070** [P] [LOW] Update risk register with Phase 2 progress
  - Path: `docs/coverage/risk-register.md`
  - Actions: Mark resolved risks, update mitigation status, document new risks

- [ ] **T071** [P] [LOW] Implement coverage telemetry
  - Path: `app/src/main/java/com/vjaykrsna/nanoai/telemetry/CoverageTelemetryReporter.kt` (expand)
  - Add: Coverage delta reporting, trend tracking, regression alerts

- [ ] **T072** [LOW] Run comprehensive quickstart validation
  - Path: Follow `specs/005-improve-test-coverage/quickstart.md`
  - Validate: Commands execute, thresholds pass, documentation accuracy, update with Phase 2 findings

- [ ] **T073** [LOW] Generate final coverage reports and summary
  - Commands: `./gradlew jacocoFullReport`, `./gradlew verifyCoverageThresholds`, `./gradlew coverageMarkdownSummary`
  - Publish: `app/build/coverage/summary.md`, validate all thresholds met

- [ ] **T074** [LOW] Final code review and documentation sync
  - Actions: Review Phase 2 test code, ensure KDoc comments, update AGENTS.md, validate constitution adherence, prepare stakeholder presentation

**Checkpoint Phase 2.5**: All coverage targets met, documentation complete, ready for stakeholder approval

---

## Critical Dependencies

**BLOCKING**: T024 must complete before any UI tests (T050-T066)

**PARALLEL**: Data (T027-T040) + ViewModel (T041-T049) can run simultaneously

---

## Success Metrics

- **Coverage Targets**: VM ≥75%, UI ≥65%, Data ≥70%
- **Quality**: Zero flaky tests, CI <20min, 100% accessibility on critical screens
- **Outcome**: >90% release confidence, >95% regression detection

---

## Risk Register

| Risk ID | Severity | Description | Mitigation | Status |
|---------|----------|-------------|------------|--------|
| R2-001 | CRITICAL | Instrumentation doesn't compile | T024 | Open |
| R2-002 | CRITICAL | UI layer 1.80% coverage | T050-T066 | Open |
| R2-003 | HIGH | Data layer 8.51% coverage | T027-T040 | Open |
| R2-004 | HIGH | DownloadManager 0% coverage | T027-T029 | Open |
| R2-005 | HIGH | ModelCatalog 0% coverage | T030-T032 | Open |
| R2-006 | HIGH | Sidebar UI 0% coverage | T050-T052 | Open |
| R2-007 | MEDIUM | ViewModel 66.76% coverage | T041-T049 | Open |
| R2-008 | MEDIUM | Settings errors untested | T041-T043 | Open |
| R2-009 | MEDIUM | Existing test failures | T025, T026 | Open |
| R2-010 | LOW | Offline flows skipped | T064-T066 | Open |

---

## Timeline (4 weeks, 3 developers)

**Week 1**: T024 (blocking) + Start Data/ViewModel layers
**Week 2**: Complete Data/ViewModel + Start UI (after T024)
**Week 3**: Complete UI layer
**Week 4**: Polish & validation (T067-T074)

---

## Best Practices

- **[P]**: Parallel tasks touch different files
- **Priority**: HIGH = prevents breakage, MEDIUM = improves stability, LOW = polish
- **TDD**: Write failing tests first
- **Isolation**: Use TestEnvironmentRule for instrumentation
- **Mocks**: Fakes for repos, MockK for services, MockWebServer for network
- **Accessibility**: Validate content descriptions and TalkBack
- **Coverage**: Run `./gradlew verifyCoverageThresholds` after checkpoints

---

## Quick Commands

```bash
# Unit tests
./gradlew testDebugUnitTest

# Instrumentation (after T024)
./gradlew ciManagedDeviceDebugAndroidTest

# Coverage
./gradlew jacocoFullReport
./gradlew verifyCoverageThresholds
cat app/build/coverage/summary.md
```

---

*Phase 2: Comprehensive Coverage • Generated 2025-10-15*
