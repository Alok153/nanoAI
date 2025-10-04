# Codebase Inconsistencies

This document catalogs inconsistencies identified across the codebase, including static analysis issues from Detekt, code style violations, architectural mismatches, potential bugs, unimplemented features (from TODOs), and gaps relative to specs. Issues are categorized for prioritization. Fixes should address high-priority items first to improve maintainability, reliability, and completeness.

## Recent Resolutions (2025-10-04)
- **UI Complexity Refactors**: `HomeScreen`, `WelcomeScreen`, `SidebarContent`, and `ThemeToggle` have been decomposed into focused composables with shared parameter bundles, bringing them under Detekt `LongMethod`/`LongParameterList` thresholds and improving TalkBack semantics.
- **Telemetry Cohesion**: `TelemetryReporter` now centralizes event emission and is wired into `ModelDownloadWorker` and `ModelManifestRepository`, closing the gap for consistent `RecoverableError` reporting noted under Error Handling.
- **Maintenance Migrations**: Room migrations for maintenance entities include automated coverage via `MaintenanceMigrationsTest`, preventing future regressions called out in Testing Gaps.
- **Quickstart Guidance**: Documentation refreshed to align Scenarios 3 and 7 with the new telemetry expectations and migration validation commands.

## 1. Static Analysis (Detekt) Issues
Detekt reported 1062 weighted issues. Key categories and examples:

### Complexity & Structure
- **TooManyFunctions** (Threshold: 11): Several classes/interfaces exceed function limits, indicating potential god classes.
  - `UserProfileRepository` (15 functions): Interface bloated with UI/UX ops; split into `UserProfileRepository` and `UiStateRepository`.
  - `UserProfileRepositoryImpl` (15 functions): Implementation mirrors interface; refactor to use cases.
  - `UIStateSnapshotDao` (14 functions): DAO over-responsible; separate concerns for snapshots vs. profiles.
  - `UserProfileDao` (14 functions): Similar bloat; consolidate queries.
  - `LayoutSnapshotDao` (17 functions): Highest offender; extract sub-interfaces.
  - `TypeConverters` (14 functions): Utility class too broad; split by type family (e.g., `DateTimeConverters`, `EnumConverters`).
  - `DatabaseModule` (11 functions): DI object at threshold; group related providers.
  - `SettingsViewModel` (12 functions): VM handles too many concerns; delegate to use cases.
  - `UserProfileLocalDataSource` (20 functions): Data source violates single responsibility; split read/write operations.

- **LongMethod** (Threshold: 60 lines):
  - `ThemeToggle` (137 lines): Composable too complex; extract sub-composables (e.g., `ToggleSwitch`, `LabelRow`).
  - `SidebarContent` (96 lines): Sidebar logic overgrown; break into `SearchBar`, `ThreadList`, `InferenceToggle`.
  - `NavigationScaffold` (228 lines): Main scaffold violates cohesion; extract `TopBar`, `BottomNav`, `DrawerContent`.
  - `WelcomeScreen` (79 lines): Onboarding screen; split into `HeroSection`, `CtaSection`, `TooltipSection`.
  - `HomeScreen` (116 lines): Home feed; decompose into `RecentActionsList`, `OfflineBanner`, `LatencyIndicator`.
  - `SidebarDrawer` (61 lines): Minor overrun; extract `NavItems`, `PinnedToolsList`.

- **CyclomaticComplexMethod** (Threshold: 15):
  - `NavigationScaffold` (complexity: 16): Nested conditionals for navigation; simplify with state machine or composable routing.
  - `instantiateViewModel` in `WelcomeViewModelTest.kt` (complexity: 25): Test setup overly branched; use parameterized tests or fixtures.

- **LongParameterList** (Threshold: 6/7):
  - `SidebarContent` (15 parameters): Excessive; use data class `SidebarUiState` for grouping.
  - `WelcomeScreen` (8 parameters): Bundle callbacks into `WelcomeActions`.
  - `HomeScreen` (8 parameters): Similar; use `HomeActions` data class.
  - `SettingsViewModel` constructor (7 parameters): At threshold; consider facade or sub-modules.
  - `WelcomeViewModel` constructor (7 parameters): Group use cases into `UiUxUseCases`.

### Naming & Conventions
- **FunctionNaming**: CamelCase violations in composables (e.g., `ThemeToggle`, `OnboardingTooltip`, `PrimaryActionCard`, `OfflineBanner` should be lowercase start).
- **ParameterNaming**: Lambda params use past tense (e.g., `onTooltipDismissed` → `onTooltipDismiss` in `WelcomeScreen`).

### Style & Formatting
- **Indentation**: Widespread issues in UI/UX domain files (e.g., `RecordOnboardingProgressUseCase.kt`, `ToggleCompactModeUseCase.kt`, `ObserveUserProfileUseCase.kt`); inconsistent spacing breaks readability.
- **MaximumLineLength** (120 chars): Exceeded in tests (e.g., `WelcomeViewModelTest.kt` lines 38,48,72, etc.) and impl files (e.g., `UserProfileLocalDataSource.kt` lines 261,310).
- **ParameterListWrapping/ArgumentListWrapping**: Long arg lists in tests (e.g., `ExportServiceImplTest.kt` line 160) need line breaks.

### Compose-Specific
- **CompositionLocalAllowlist**: Custom locals in `Theme.kt` (lines 104-105); prefer existing Material locals or justify.
- **ComposableParamOrder**: `NavigationScaffold` (line 84) params out of order; reorder: modifier first, then non-defaults, defaults last.

### Unused/Dead Code
- `savedStateHandle` in `WelcomeViewModel.kt` (line 36): Unused private property; remove or use for state restoration.

## 2. Architectural Inconsistencies
- **Layering Violations**: UI models (e.g., `UserProfile`) mix domain logic (validation in init blocks) with data; move sanitization to use cases or repositories.
- **Dependency Injection**: `DatabaseModule` provides too many DAOs; consider sub-modules (`UiUxDatabaseModule`, `ChatDatabaseModule`).
- **State Management**: Multiple flows in VMs (e.g., `ChatViewModel` combines threads/messages); use `StateFlow` for derived state to reduce boilerplate.
- **Error Handling**: Inconsistent; some use `Result<T>`, others throw exceptions (e.g., `InferenceOrchestrator`). Standardize on sealed classes for domain errors.
- **Testing Gaps**: Many contract tests (e.g., `HomeScreenContractTest.kt`) but incomplete coverage for edge cases like offline mode or persona switches.

## 3. Unimplemented Features & TODOs (Gaps vs. Specs)
From codebase search, several placeholders indicate incomplete implementations, violating specs (e.g., 001-foundation FR-004 for offline inference, FR-030 for MediaPipe integration):

- **Local Inference Runtime** (`MediaPipeLocalModelRuntime.kt`):
  - Line 52: "// TODO: Replace synthesis with actual MediaPipe LiteRT inference pipeline." – Core offline generation is mocked; no real model execution, breaking offline-first (spec FR-004).
  - Gap: Specs require local response <2s; current synthesis bypasses hardware acceleration.

- **Security & Encryption** (`ApiProviderConfigEntity.kt`):
  - Lines 19,35: "// TODO: Encrypt with EncryptedSharedPreferences or Jetpack Security" – API keys stored plaintext, risking exposure; contradicts privacy stewardship (constitution).

- **Download Validation** (`ModelDownloadWorker.kt`):
  - Line 66: "// TODO: Get actual download URL from model package or remote catalog" – Hardcoded URL; no dynamic catalog integration.
  - Line 129: "// TODO: Verify checksum" – No integrity check post-download; risks corrupted models (spec FR-006).

- **Testing Completeness** (Multiple androidTest files):
  - Pending tests marked "TODO(003-UI-UX#T079)": e.g., `CloudFallbackAndExportTest.kt`, `FirstLaunchDisclaimerDialogTest.kt`, `ModelLibraryFlowTest.kt`, `ExportDialogAccessibilityTest.kt`, `PersonaOfflineFlowTest.kt`, `AccessibilityScenarioTest.kt`. Indicates incomplete E2E/UI validation for key flows (specs 003-UI-UX FR-007, 002-disclaimer-and-fixes FR-004).

- **Disclaimer UI** (`FirstLaunchDisclaimer.kt`):
  - Implicit gap: Text mentions "incomplete" responsibility; no full modal implementation or persistence tie-in.

These TODOs highlight MVP placeholders; full implementation needed for production readiness.

## 4. Feature Completeness Gaps (vs. Specs)
- **Multimodal Support** (001-foundation): Text-only; `Message` has `audioUri`/`imageUri` fields but no handling in UI/use cases (deferred per research.md).
- **Export/Import** (002-disclaimer-and-fixes): Basic; lacks schema validation, error recovery for corrupted files.
- **Offline Sync** (001-foundation FR-005): Queued actions stubbed; no conflict resolution or retry logic.
- **Accessibility** (003-UI-UX FR-005): Semantics partial; dynamic content (e.g., loading) lacks live regions.
- **Performance Monitoring** (003-UI-UX FR-007): No RUM integration; baselines planned but unimplemented.

## 5. Interaction & Integration Issues
- **Chat-Library Flow**: `ChatViewModel` calls `SendPromptAndPersonaUseCase` but no model availability check; potential crash if no local/cloud ready (gap in orchestration).
- **UI-State Sync**: `ObserveUserProfileUseCase` loads from Room/DataStore but no real-time cloud sync (spec 001-foundation FR-005 incomplete).
- **Navigation**: `NavigationScaffold` handles routes but no deep linking for threads/personas (003-UI-UX FR-012 partial).

## Prioritization
- **Critical**: Implement core TODOs (MediaPipe, encryption, checksums) to enable offline/privacy.
- **High**: Fix Detekt blockers (complexity, naming) to pass CI; complete pending tests.
- **Medium**: Refactor long methods/params; enhance error handling consistency.
- **Low**: Style fixes; full accessibility audit.

Last Updated: 2025-10-03
