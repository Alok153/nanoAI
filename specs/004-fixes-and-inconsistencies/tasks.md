# Tasks: Fixes and Inconsistencies Stabilization Pass

**Input**: Design documents from `/specs/004-fixes-and-inconsistencies/`
**Prerequisites**: plan.md (required), research.md, data-model.md, contracts/

## Task List

- [X] **T001** Configure Jetpack Security and integrity dependencies in `app/build.gradle.kts` (add `androidx.security:security-crypto`, ensure WorkManager/MockWebServer test deps) so encryption and manifest tests can compile.
- [X] **T002** Update `.github/workflows/ci.yml` to run `./gradlew detekt ktlintCheck` on PRs with failure gates for `TooManyFunctions`, `LongMethod`, `CyclomaticComplexMethod`, `LongParameterList` (blocks merges when counts > 0).
- [X] **T003** Tighten `config/detekt/detekt.yml` thresholds to zero for blocking rules and drop matching entries from `config/detekt/baseline.xml`.
- [X] **T004 [P]** Promote placeholder `ModelManifestContractTest` into a failing contract test at `app/src/test/contract/com/vjaykrsna/nanoai/contracts/ModelManifestContractTest.kt` asserting manifest signature, SHA-256 length, and HTTPS URL.
- [X] **T005 [P]** Author failing contract test `ModelVerificationContractTest` in `app/src/test/contract/com/vjaykrsna/nanoai/contracts/ModelVerificationContractTest.kt` validating RETRY responses include `nextRetryAfterSeconds` and `ErrorEnvelope` mapping.
- [X] **T006 [P]** Author failing contract test `CredentialRotationContractTest` in `app/src/test/contract/com/vjaykrsna/nanoai/contracts/CredentialRotationContractTest.kt` covering unsupported provider/environment handling and returned `keyAlias`/`migrationRequired` fields.
- [X] **T007 [P]** Add failing unit test `NanoAIResultTest` under `app/src/test/java/com/vjaykrsna/nanoai/core/common/NanoAIResultTest.kt` verifying sealed result propagation from repositories to ViewModel reducers.
- [X] **T008 [P]** Add failing unit test `EncryptedSecretStoreMigrationTest` in `app/src/test/java/com/vjaykrsna/nanoai/security/EncryptedSecretStoreMigrationTest.kt` ensuring plaintext configs migrate to EncryptedSharedPreferences and legacy file is removed.
- [X] **T009 [P]** Add failing worker test `ModelDownloadWorkerIntegrityTest` at `app/src/test/java/com/vjaykrsna/nanoai/model/download/ModelDownloadWorkerIntegrityTest.kt` using MockWebServer to assert manifest checksum verification and retry behavior.
- [X] **T010 [P]** Add failing UI instrumentation test `OfflinePersonaFlowTest` in `app/src/androidTest/java/com/vjaykrsna/nanoai/persona/OfflinePersonaFlowTest.kt` reproducing quickstart Scenario 4 (offline banner, queued replay).
- [X] **T011 [P]** Add failing UI instrumentation test `DisclaimerDialogTest` in `app/src/androidTest/java/com/vjaykrsna/nanoai/disclaimer/DisclaimerDialogTest.kt` covering Scenario 5 accessibility semantics and blocking behavior.
- [X] **T012 [P]** Add failing UI instrumentation test `ModelDownloadScenarioTest` in `app/src/androidTest/java/com/vjaykrsna/nanoai/model/ModelDownloadScenarioTest.kt` checking that corrupt packages surface actionable errors (Scenario 3).
- [X] **T013 [P]** Add failing unit test `CloudFallbackViewModelTest` at `app/src/test/java/com/vjaykrsna/nanoai/inference/CloudFallbackViewModelTest.kt` for Scenario 6 verifying `RecoverableError` telemetry IDs and retry guidance.
- [X] **T014 [P]** Implement `RepoMaintenanceTaskEntity`, DAO, and mapper in `app/src/main/java/com/vjaykrsna/nanoai/core/maintenance/db/` satisfying T004–T006 dependencies (Room schema + data model fields).
- [X] **T015 [P]** Implement `CodeQualityMetricEntity` and DAO in `app/src/main/java/com/vjaykrsna/nanoai/core/maintenance/db/` including threshold tracking and linking to maintenance tasks.
- [X] **T016** Implement `ModelPackageEntity`, `DownloadManifestEntity`, and DAO/service layer in `app/src/main/java/com/vjaykrsna/nanoai/model/catalog/` with Room relationships and manifest caching (prereq T009).
- [X] **T017** Implement `NanoAIResult` sealed hierarchy and mappers in `app/src/main/java/com/vjaykrsna/nanoai/core/common/NanoAIResult.kt`, updating domain use cases to return typed results (prereq T007).
- [X] **T018** Implement `EncryptedSecretStore` in `app/src/main/java/com/vjaykrsna/nanoai/security/EncryptedSecretStore.kt` with MasterKey-backed EncryptedSharedPreferences API (prereq T008, T001).
- [X] **T019** Implement migration orchestrator `SecretMigrationInitializer` in `app/src/main/java/com/vjaykrsna/nanoai/security/SecretMigrationInitializer.kt` (triggered from Application/Hilt entry point) to move legacy secrets and log completion (prereq T018).
- [X] **T020** Extend `ModelDownloadWorker` in `app/src/main/java/com/vjaykrsna/nanoai/model/download/ModelDownloadWorker.kt` to fetch signed manifests, validate SHA-256/size, and emit structured `NanoAIResult` errors (prereq T009, T016, T017).
- [X] **T021** Add `ModelManifestRepository` & Retrofit API in `app/src/main/java/com/vjaykrsna/nanoai/model/catalog/ModelManifestRepository.kt` bridging contracts endpoints with Room cache (prereq T016, T004–T006).
- [X] **T022** Update Hilt modules (`app/src/main/java/com/vjaykrsna/nanoai/di/ModelModules.kt`) to provide new repositories, DAOs, EncryptedSecretStore, and migration initializer (prereq T014–T021).
- [X] **T023** Refactor `NavigationScaffold` in `app/src/main/java/com/vjaykrsna/nanoai/ui/scaffold/NavigationScaffold.kt` into smaller composables with Material 3 semantics and reduced cyclomatic complexity (prereq T010).
- [ ] **T024** Refactor `HomeScreen` in `app/src/main/java/com/vjaykrsna/nanoai/ui/home/HomeScreen.kt` splitting recent actions, offline banner, and latency indicator composables while keeping previews/test tags intact (prereq T010).
- [ ] **T025** Refactor `WelcomeScreen` in `app/src/main/java/com/vjaykrsna/nanoai/ui/onboarding/WelcomeScreen.kt` extracting hero/CTA/tooltips components and aligning parameter bundles (prereq T011).
- [ ] **T026** Refactor `SidebarContent` in `app/src/main/java/com/vjaykrsna/nanoai/ui/sidebar/SidebarContent.kt` to introduce `SidebarUiState` data class, reduce parameter lists, and add TalkBack semantics (prereq T011).
- [ ] **T027** Refactor `ThemeToggle` in `app/src/main/java/com/vjaykrsna/nanoai/ui/settings/ThemeToggle.kt` into reusable sub-composables with accessibility labels and animation guards (prereq T011).
- [ ] **T028** Add Room migration scripts and unit test in `app/src/test/java/com/vjaykrsna/nanoai/core/maintenance/db/MaintenanceMigrationsTest.kt` covering new entities and ensuring backward compatibility (prereq T014–T016).
- [ ] **T029** Wire telemetry/error logging via sealed results in `app/src/main/java/com/vjaykrsna/nanoai/telemetry/TelemetryReporter.kt`, ensuring `RecoverableError` publishes retry hints (prereq T017, T020).
- [ ] **T030 [P]** Update `docs/inconsistencies.md`, `docs/todo-next.md`, and `specs/004-fixes-and-inconsistencies/quickstart.md` to reflect resolved blockers, new migrations, and testing instructions (prereq T014–T029).
- [ ] **T031** Execute quickstart scenarios end-to-end, capture detekt/ktlint reports, instrumentation results, and attach artifacts to PR notes (prereq T030).

## Dependencies

- T002 depends on T001.
- T003 depends on T002.
- Tests (T004–T013) must be authored and failing before implementing dependent production tasks (T014–T021).
- T016 depends on T014–T015 for shared Room infrastructure.
- T017 depends on T007; T018 depends on T008 & T001; T019 depends on T018.
- T020 depends on T009, T016, T017; T021 depends on T016 and T004–T006.
- T022 depends on T014–T021.
- Compose refactors (T023–T027) depend on corresponding instrumentation tests (T010–T012).
- T028 depends on T014–T016; T029 depends on T017 & T020; T030 depends on implementation tasks (T014–T029); T031 is final validation.

## Parallel Execution Examples

- Run contract test authoring together once setup is complete:
  - `taskctl run --parallel T004 T005 T006`
- After tests are red, implement independent Room entities in parallel:
  - `taskctl run --parallel T014 T015`
- Close-out polish can batch docs with validation prep:
  - `taskctl run --parallel T030`

```
Reminder: ensure each test added in T004–T013 fails before implementing the matching production code. Mark tasks complete individually after verifying git status and test outcomes.
```
