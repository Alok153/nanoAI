# Tasks: Offline Multimodal nanoAI Assistant

**Input**: Design documents from `/specs/002-i-want-to/`
**Prerequisites**: `plan.md`, `research.md`, `data-model.md`, `contracts/`, `quickstart.md`

## Phase 3.1 Setup ✅ COMPLETED
- [x] T001 Configure dependency catalog: update `gradle/libs.versions.toml` and `app/build.gradle.kts` with MediaPipe v0.10.14, Retrofit + Kotlin Serialization, Room (runtime + ksp), WorkManager, DataStore, Coil, Hilt, Coroutines, kotlinx-datetime, and macrobenchmark libraries referenced in the plan.
- [x] T002 Enable Kotlin 1.9 Compose pipeline: adjust `app/build.gradle.kts` to apply Kotlin Serialization and Hilt plugins, set compile/target options (compileSdk 36, minSdk 31), enable Compose metrics, configure packaging options, Room schema export, and register baseline profile build variants.
- [x] T003 Harden CI gates: add or update `.github/workflows/android-ci.yml` to run ktlint, Detekt, Android Lint, `testDebugUnitTest`, `connectedDebugAndroidTest`, and macrobenchmark smoke jobs, wiring Gradle invocations documented in the plan.

## Phase 3.2 Tests First (TDD) ✅ COMPLETED
- [x] T004 [P] Generate contract test `app/src/test/java/com/vjaykrsna/nanoai/contracts/CreateCompletionContractTest.kt` asserting `POST /v1/completions` requests/responses conform to `contracts/llm-gateway.yaml`. **10 tests PASSING**.
- [x] T005 [P] Generate contract test `app/src/test/java/com/vjaykrsna/nanoai/contracts/ListModelsContractTest.kt` validating `GET /v1/models` schema from `contracts/llm-gateway.yaml`. **11 tests PASSING**.
- [x] T006 [P] Add JSON schema validation test `app/src/test/java/com/vjaykrsna/nanoai/contracts/ModelManifestSchemaTest.kt` to ensure `contracts/model-manifest.json` accepts valid manifests and rejects checksum/enum violations. **11 tests PASSING**.
- [x] T007 [P] Create Room DAO test `app/src/test/java/com/vjaykrsna/nanoai/core/data/db/ChatMessageDaoTest.kt` covering ChatThread-Message cascades, ordering index, and persona log joins. **11 tests created**.
- [x] T008 [P] Author domain unit test `app/src/test/java/com/vjaykrsna/nanoai/feature/chat/domain/SendPromptAndPersonaUseCaseTest.kt` verifying persona switch logging and local-versus-cloud routing logic with fakes. **12 tests created**.
- [x] T009 [P] Author domain unit test `app/src/test/java/com/vjaykrsna/nanoai/feature/library/domain/ModelDownloadsAndExportUseCaseTest.kt` asserting queue limits, checksum handling, and export bundle composition. **16 tests created**.
- [x] T010 [P] Build Compose instrumentation `app/src/androidTest/java/com/vjaykrsna/nanoai/feature/library/ModelLibraryFlowTest.kt` covering model download, pause/resume, queued downloads, and failure recovery from quickstart. **15 tests created**.
- [x] T011 [P] Build Compose instrumentation `app/src/androidTest/java/com/vjaykrsna/nanoai/feature/chat/PersonaOfflineFlowTest.kt` validating persona toggle prompts, logging overlay, and offline local-inference banner behavior. **16 tests created**.
- [x] T012 [P] Build Compose instrumentation `app/src/androidTest/java/com/vjaykrsna/nanoai/feature/settings/CloudFallbackAndExportTest.kt` verifying cloud badge/quota chips, export warning dialog, and consent timestamp updates. **18 tests created**.
- [x] T013 [P] Implement macrobenchmark & baseline profile instrumentation in `macrobenchmark/src/main/java/com/vjaykrsna/nanoai/ColdStartBenchmark.kt` and `.../BaselineProfileGenerator.kt` to assert cold start <1.5 s and scroll stability. **6 benchmark tests + baseline profile generator created**.
- **Total: 126+ tests created following TDD principles. Tests currently fail (expected) awaiting implementation of repositories, use cases, ViewModels, and UI.**

## Phase 3.3 Data Layer Implementation ✅ COMPLETED
- [x] T014 [P] Define `ChatThreadEntity` + `ChatThreadDao` in `app/src/main/java/com/vjaykrsna/nanoai/core/data/db/entities/ChatThreadEntity.kt` and `.../daos/ChatThreadDao.kt` with UUID storage, Instant converters, and archive toggles.
- [x] T015 [P] Define `MessageEntity` + `MessageDao` in `.../core/data/db/entities/MessageEntity.kt` and `.../daos/MessageDao.kt` capturing role enum (USER/ASSISTANT/SYSTEM), latency metrics, error codes, CASCADE delete, and composite index on (threadId, createdAt).
- [x] T016 [P] Define `PersonaProfileEntity` + `PersonaProfileDao` in `.../core/data/db/entities/PersonaProfileEntity.kt` and `.../daos/PersonaProfileDao.kt` storing system prompts, temperature/topP parameters, and model preferences.
- [x] T017 [P] Define `ModelPackageEntity` + `ModelPackageDao` in `.../feature/library/data/entities/ModelPackageEntity.kt` and `.../daos/ModelPackageDao.kt` mapping provider type enum (MEDIA_PIPE/TFLITE/MLC_LLM/ONNX_RUNTIME/CLOUD_API) and capability flags (TEXT_GEN/IMAGE_GEN/AUDIO_IN/AUDIO_OUT).
- [x] T018 [P] Define `DownloadTaskEntity` + `DownloadTaskDao` in `.../feature/library/data/entities/DownloadTaskEntity.kt` and `.../daos/DownloadTaskDao.kt` with progress tracking (0.0-1.0), status enum (QUEUED/DOWNLOADING/PAUSED/COMPLETED/FAILED/CANCELLED), and WorkManager task references.
- [x] T019 [P] Define `ApiProviderConfigEntity` + `ApiProviderConfigDao` in `.../core/data/db/entities/ApiProviderConfigEntity.kt` and `.../daos/ApiProviderConfigDao.kt` including API key fields (TODO: encrypt with Jetpack Security), quota reset timestamps, and provider status (OK/UNAUTHORIZED/RATE_LIMITED/ERROR/UNKNOWN).
- [x] T020 [P] Implement `PrivacyPreferenceDataStore` in `app/src/main/java/com/vjaykrsna/nanoai/core/data/preferences/PrivacyPreferenceStore.kt` using Preferences DataStore for consent timestamp tracking, telemetry opt-in (default false), and retention policy (INDEFINITE/MANUAL_PURGE_ONLY).
- [x] T021 [P] Define `PersonaSwitchLogEntity` + `PersonaSwitchLogDao` in `.../core/data/db/entities/PersonaSwitchLogEntity.kt` and `.../daos/PersonaSwitchLogDao.kt` capturing action types (CONTINUE_THREAD/START_NEW_THREAD) and timestamps with CASCADE delete.
- [x] T022 Compose `NanoAIDatabase` in `app/src/main/java/com/vjaykrsna/nanoai/core/data/db/NanoAIDatabase.kt` with TypeConverters (Instant ↔ Long, Set<String> ↔ String), version 1 schema, foreign key enforcement, and DAO accessors for all 7 entities (ChatThread, Message, PersonaProfile, PersonaSwitchLog, ModelPackage, DownloadTask, ApiProviderConfig).
- **Total: 32 files created (7 entities + 7 DAOs + 8 enums + PrivacyPreference + PrivacyPreferenceStore + TypeConverters + NanoAIDatabase). Main code compiles successfully. ✅**

## Phase 3.4 Core Logic & UI Implementation (post-tests) ⚠️ IN PROGRESS
- [x] T023 Implement runtime stack: create `MediaPipeLocalModelRuntime` (`app/src/main/java/com/vjaykrsna/nanoai/core/runtime/MediaPipeLocalModelRuntime.kt`), Retrofit `CloudGatewayService` + `CloudGatewayClient` (`.../core/network/`), and `InferenceOrchestrator` (`.../feature/chat/domain/InferenceOrchestrator.kt`) honoring MediaPipe v0.10.14 LoRA ranks and Gemini fallback. ✅ `./gradlew :app:compileDebugKotlin` passes with stack wired.
- [x] **T024 Implement data repositories & background workers** ✅
  - [x] T024.1 ConversationRepository interface & implementation (wraps ChatThreadDao + MessageDao)
  - [x] T024.2 PersonaRepository interface & implementation (wraps PersonaProfileDao)
  - [x] T024.3 PersonaSwitchLogRepository interface & implementation (wraps PersonaSwitchLogDao)
  - [x] T024.4 ModelCatalogRepository interface & implementation (wraps ModelPackageDao)
  - [x] T024.5 DownloadManager interface & implementation (wraps DownloadTaskDao + WorkManager, max 2 concurrent)
  - [x] T024.6 ApiProviderConfigRepository interface & implementation (wraps ApiProviderConfigDao)
  - [x] T024.7 ModelDownloadWorker implementation (WorkManager worker for background downloads with @HiltWorker)
  - [x] T024.8 Hilt DI modules (DatabaseModule, RepositoryModule, WorkerModule)
  - [x] T024.9 Create domain models (ChatThread, Message, PersonaProfile, ModelPackage, DownloadTask, ApiProviderConfig, PersonaSwitchLog with Entity↔Domain mappers)
  - [x] **T024.10 Verify repository compilation**: Added `kotlinx-datetime`, `androidx.hilt:hilt-work`, and `androidx.hilt:hilt-compiler` dependencies, synchronized DAO method calls, and reran `./gradlew :app:compileDebugKotlin` (now SUCCESSFUL). Full `clean build -x test` still surfaces ktlint + macrobenchmark issues slated for later cleanup. Room schema export is now configured via the Gradle plugin to emit JSON snapshots under `app/schemas`.
  - **Status**: 26+ files created (7 domain models, 6 repository interfaces, 6 implementations, 1 worker, 3 DI modules, 1 application class). Repository layer compiles and is ready for downstream use cases and ViewModels.
- [ ] T025 Implement domain use cases (`SendPromptUseCase`, `ManageModelDownloadsUseCase`, `PersonaSwitchUseCase`, `ExportBackupUseCase`) in `app/src/main/java/com/vjaykrsna/nanoai/feature/*/domain/` orchestrating repositories, offline constraints, and export warnings.
- [ ] T026 Implement ViewModels (`ChatViewModel`, `ModelLibraryViewModel`, `SettingsViewModel`, `SidebarViewModel`) in `app/src/main/java/com/vjaykrsna/nanoai/feature/*/presentation/` exposing immutable state flows, error channels, and analytics events.
- [ ] T027 Build Compose UI: deliver `ChatScreen`, `ModelLibraryScreen`, `SettingsScreen`, `ExportDialog`, `PrivacyDashboard`, and `NavigationScaffold` under `app/src/main/java/com/vjaykrsna/nanoai/feature/*/ui/` with status chips, persona sheet, accessibility semantics, and offline indicators.

## Phase 3.5 Polish & Validation
- [ ] T028 Address accessibility and performance polish: add TalkBack labels, dynamic type scaling, vibration hooks, and frame budget optimizations across composables and WorkManager constraints.
- [ ] T029 Update documentation: refresh `specs/002-i-want-to/quickstart.md`, `plan.md`, and `research.md` with completed architecture notes, runtime specifics, and testing guidance.
- [ ] T030 Execute Quickstart validation on Pixel 7 & Pixel 4a, recording outcomes and logs under `specs/002-i-want-to/validation/` and checking off the quickstart checklist.

## Dependencies
- Tests (T004–T013) must land before any implementation task (T014+).
- Data entities (T014–T021) must complete before database assembly (T022) and repositories (T024).
- Runtime stack (T023) precedes use cases (T025) and ViewModels (T026).
- Use cases (T025) block ViewModels (T026), which block UI implementation (T027) and polish items (T028).
- Documentation and validation (T029–T030) occur after functional work is merged.

## Parallel Execution Examples
- Contract validation burst: Task agent command: `taskctl run --parallel T004 T005 T006`
- Domain unit tests bundle: Task agent command: `taskctl run --parallel T008 T009`
- Room entity pass: Task agent command: `taskctl run --parallel T014 T015 T016 T017 T018 T019 T020 T021`
- Instrumentation trio: Task agent command: `taskctl run --parallel T010 T011 T012`
