# Quickstart: Fixes and Inconsistencies Stabilization Pass

**Feature**: 004-fixes-and-inconsistencies  
**Date**: 2025-10-03  

## Overview
Follow this guide to validate that the stabilization work meets constitutional requirements: static analysis gates, secure secrets storage, model download integrity, deterministic tests for critical flows, and regression coverage for the new maintenance migrations/telemetry reporting work.

## Prerequisites
- Android Studio Iguana+ with Android SDK 36 installed.
- Connected device via adb.
- Clean checkout of branch `004-fixes-and-inconsistencies` with Gradle wrapper configured.
- Test credentials for cloud fallback stored as environment variables (will be migrated to encrypted storage during Scenario 2).

## Scenario 1: Static Analysis Gates
a. Run Spotless + Detekt gates.
    1. `./gradlew spotlessCheck`
    2. `./gradlew detekt`
    3. Verify no blocking rules remain (TooManyFunctions, LongMethod, CyclomaticComplexMethod, LongParameterList).
    4. Capture HTML reports under `build/reports/{spotless,detekt}`.

**Expected**: Both commands exit 0. Reports show 0 occurrences for blocking Detekt rules.

## Scenario 2: Secrets Migration & Encryption
1. Launch the app on a device/emulator with a build from this branch.
2. Inject legacy plaintext keys by toggling the developer setting `Load legacy provider config` (available only in debug builds) or via `adb shell setprop dev.nanoai.plaintext_config 1`.
3. Trigger the migration flow (`Settings → Providers → Migrate credentials`).
4. Inspect logs for `EncryptedSecretStore` migration status.
5. Use `adb shell run-as com.vjaykrsna.nanoai ls files/` to confirm plaintext store removed.
6. Verify new encrypted entries exist in `/data/data/com.vjaykrsna.nanoai/shared_prefs/encrypted_provider_prefs.xml` (binary) and that MasterKey alias set.

**Expected**: Migration reports success, plaintext file deleted, encrypted store present, app restarts without crashes.

## Scenario 3: Model Download Integrity & Telemetry
1. Start `MockModelCatalogServer` (see instrumentation test utility) or run `./gradlew app:connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.vjaykrsna.nanoai.model.ModelDownloadScenarioTest`.
2. Observe logs: `ModelDownloadWorker` fetches manifest, validates SHA-256 before install, and emits telemetry for successful downloads via `TelemetryReporter`.
3. Repeat test with corrupted package (set flag `catalog.corrupt=true`).
4. Confirm worker returns `Result.retry()` up to 3 times, records each retry in telemetry with `RecoverableError` IDs, then surfaces `INTEGRITY_FAILURE` error to UI and quick actions to retry.

**Expected**: Happy path installs model and logs `download.success`; corrupted path produces decline message, telemetry with retry hints, and no corrupted file saved.

## Scenario 4: Offline Persona Flow
1. Disable network (airplane mode) before launching the app.
2. Run `./gradlew app:connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.vjaykrsna.nanoai.persona.OfflinePersonaFlowTest`.
3. Verify Compose banner displays "Offline mode active" and queued actions stored.
4. Re-enable network; test asserts queued actions replay and success banner shown.

**Expected**: Offline banner accessible via TalkBack, state persists across process death.

## Scenario 5: Disclaimer Dialog Regression
1. Run `./gradlew app:connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.vjaykrsna.nanoai.disclaimer.DisclaimerDialogTest`.
2. Test covers first-launch acceptance, persistence, and TalkBack semantics.

**Expected**: Dialog text matches spec, cannot proceed without acknowledgement, semantics describe purpose.

## Scenario 6: Cloud Fallback Error Handling
1. Run `./gradlew app:testDebugUnitTest --tests "com.vjaykrsna.nanoai.inference.CloudFallbackViewModelTest"`.
2. Scenario covers sealed `NanoAIResult` handling when local runtime declines due to low memory.
3. Ensure `RecoverableError` path logs telemetry ID and surfaces actionable retry instructions.

**Expected**: Tests pass, error envelope displayed in UI snapshot test, and telemetry stream includes `cloud_fallback.recoverable` entries.

## Scenario 7: Maintenance Schema Migrations
1. Run `./gradlew app:testDebugUnitTest --tests "com.vjaykrsna.nanoai.core.maintenance.db.MaintenanceMigrationsTest"`.
2. Confirm the test verifies the new `RepoMaintenanceTaskEntity`, `CodeQualityMetricEntity`, and `ModelPackageEntity` schema migrations.
3. Inspect the generated schema under `app/schemas/` to ensure the migration file for the latest version matches expectations.

**Expected**: Migration test passes and the schema reflects newly added tables/columns without data loss.

## Automation & Tooling
- **Unit tests**: `./gradlew testDebugUnitTest`
- **Instrumentation suite**: `./gradlew connectedAndroidTest`
- **Macrobenchmark smoke**: `./gradlew macrobenchmark:connectedAndroidTest`
- **Static analysis baseline regeneration (if needed)**: `./gradlew detektBaseline` (use only after approvals).

## Performance Validation
- Use Macrobenchmark scenario `InferenceWarmupBenchmark` to confirm local inference ≤ 2s median.
- Monitor `FrameMetricsAggregator` (Jetpack Macrobenchmark) to ensure frame drop rate < 5% after refactors.
- Confirm WorkManager job metrics (via `adb shell dumpsys jobscheduler`) show ≤3 retries for model downloads.

## Completion Criteria
- All scenarios above pass without manual intervention.
- Static analysis gates green in CI, no blocking Detekt rule regressions.
- Secrets stored only in encrypted form; migration notes documented.
- Model download integrity enforced with clear error surface for corrupt packages.
- Offline, disclaimer, and cloud fallback tests deterministic and passing.
- Documentation (`docs/inconsistencies.md`, `docs/todo-next.md`) updated to mark resolved items.

## Artifacts to Capture
- Detekt & Spotless reports (attach to PR).
- Screenshot/video of disclaimer dialog and offline banner after refactor.
- Logs demonstrating corrupted download rejection.
- Test reports for unit, instrumentation, and macrobenchmark runs.
