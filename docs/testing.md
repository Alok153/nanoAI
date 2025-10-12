# Testing & Coverage Guide

This guide explains how the nanoAI test suites are organised, how they embody the product design philosophy, and how contributors can confidently measure coverage. New teammates should be able to get productive by following the practices documented here.

## Testing Philosophy
- **Kotlin-first clean architecture**: Every test reinforces separation between UI (Jetpack Compose), domain (ViewModels and coordinators), and data (Room, Retrofit). Tests focus on contract boundaries so layers remain swappable.
- **Accessibility and offline resilience**: Instrumentation scenarios validate Material 3 semantics, TalkBack descriptions, and offline fallbacks so we never trade usability for velocity.
- **Deterministic asynchronous code**: Coroutine-heavy components run under `kotlinx.coroutines.test` with explicit dispatchers (see `app/src/test/java/com/vjaykrsna/nanoai/testing/MainDispatcherExtension.kt`) to prevent flakiness.
- **Coverage as a quality gate**: We enforce ≥75% ViewModel, 65% UI, and 70% data-layer coverage through Gradle tasks wired into `check`. Risk register metadata is part of the report so leadership can approve releases with evidence.
- **Test-driven delivery**: Feature work lands with failing tests first (see `specs/005-improve-test-coverage/tasks.md`) to guard against regressions and remind us to prove behaviour before wiring implementation.

## Test Suite Map
| Suite | Location | Purpose | Key Libraries |
| --- | --- | --- | --- |
| JVM unit tests | `app/src/test/java` | ViewModels, repositories, coordinators, contract tests | JUnit 5, Truth, MockK, kotlinx.coroutines.test, Robolectric (when Android deps needed) |
| Instrumentation UI | `app/src/androidTest/java` | Compose UI flows, accessibility, offline fallbacks | AndroidX Test Runner, Espresso, Compose UI Test, MockWebServer |
| Contracts & schemas | `app/src/test/contract` resources + `config/schemas` | Schema conformance (e.g. coverage JSON) | networknt JSON Schema validator, Jackson |
| Macrobenchmarks | `macrobenchmark/src/main/java` | App startup and coverage dashboard warm-load performance | AndroidX Macrobenchmark |
| Coverage tooling | `scripts/coverage` | Report merge & markdown summary | Python 3, Bash, JaCoCo |

### Common Patterns
- **Coroutine helpers**: Use `MainDispatcherExtension` with `@ExtendWith` or instantiate per-test to override `Dispatchers.Main`.
- **Fixture builders**: Domain- and data-layer packages expose factory helpers under `app/src/test/java/com/vjaykrsna/nanoai/**/fixtures` (search for `Fixture.kt`) to keep test setup terse.
- **Compose assertions**: Prefer semantics matchers (`onNodeWithContentDescription`, `assertHasClickAction`) over screenshot testing for determinism.
- **Room DAO checks**: Run against in-memory databases; leverage `androidx.room:room-testing` and `runTest` to cover suspend DAO calls.
- **Network fallbacks**: Use `MockWebServer` to script success/latency/failure for repositories and instrumentation flows.

## Running the Test Suites
1. **Unit & contract tests**
   ```bash
   ./gradlew testDebugUnitTest
   ```
   Generates `app/build/reports/tests/testDebugUnitTest/index.html` and `*.exec` coverage data.
- Tip: Use the Jupiter selector to narrow execution, e.g. `./gradlew testDebugUnitTest --tests "com.vjaykrsna.nanoai.coverage.*"` when validating coverage helpers.
2. **Instrumentation & Compose UI tests** (requires emulator or device)
   ```bash
   ./gradlew connectedDebugAndroidTest
   ```
   Produces reports under `app/build/reports/androidTests/connected/` and `.ec` coverage files.
- To run specific tests, use `-Pandroid.testInstrumentationRunnerArguments.class="*TestClass*"` (e.g., `-Pandroid.testInstrumentationRunnerArguments.class="*OfflineProgressTest*"`), as `--tests` is not supported for instrumentation tasks.
- When triaging flakes, append `-Pandroid.testInstrumentationRunnerArguments.notAnnotation=flaky` and explicitly toggle radios (`adb shell svc wifi disable|enable`, `adb shell svc data disable|enable`) to rehearse offline fallbacks.
3. **Macrobenchmarks** (optional, CI-only by default)
   ```bash
   ./gradlew :macrobenchmark:connectedCheck
   ```

### Recommended Development Loop
- Run targeted JVM tests via `--tests` (e.g. `--tests "com.vjaykrsna.nanoai.coverage.*"`).
- For instrumentation tests, use Android runner arguments like `-Pandroid.testInstrumentationRunnerArguments.class="*TestClass*"` to filter execution.
- Before submitting changes run `./gradlew check` to execute formatting, lint, tests, merged coverage, and threshold verification.

## Coverage Workflow
1. **Merge coverage**
   ```bash
   ./gradlew jacocoFullReport
   ```
   Produces the merged XML + HTML report under `app/build/reports/jacoco/full/`. The task automatically runs both unit and instrumentation suites, so ensure an emulator or device is available first.
2. **Verify thresholds**
   ```bash
   ./gradlew verifyCoverageThresholds
   ```
   Executes `CoverageThresholdVerifier` via `VerifyCoverageThresholdsTask`, writing a human-readable gate summary to `app/build/coverage/thresholds.md`. The task fails if any layer falls below the 75/65/70 targets and is wired into `check`.
3. **Publish coverage summaries**
   ```bash
   ./gradlew coverageMarkdownSummary
   ```
   Runs `scripts/coverage/generate-summary.py` to emit `app/build/coverage/summary.md` and `app/build/coverage/summary.json`. A legacy copy of the markdown is kept at `app/build/reports/jacoco/full/summary.md` for existing CI uploads.
4. **Bundle CI artefacts**
   ```bash
   ./gradlew coverageMergeArtifacts
   ```
   Wraps `scripts/coverage/merge-coverage.sh`, re-running the full suite, copying HTML/XML outputs, summarised markdown/JSON, and all `.exec`/`.ec` inputs into `app/build/reports/jacoco/full/`. Use this before publishing artefacts in CI.

### Coverage Artefacts
- **Contract schema**: `specs/005-improve-test-coverage/contracts/coverage-report.schema.json` documents the JSON payload consumed by dashboards.
- **Layer classification**: `config/coverage/layer-map.json` maps class name patterns to `TestLayer`s for threshold enforcement. Update this map when new modules land.
- **Report generator**: `app/src/main/java/com/vjaykrsna/nanoai/coverage/domain/CoverageReportGenerator.kt` enforces severity ordering and ensures every risk is mitigated by a catalog entry.
- **Risk register alignment**: Tests in `app/src/test/java/com/vjaykrsna/nanoai/coverage/` validate sorting, tag enforcement, and risk escalation logic.
- **Telemetry**: `app/src/main/java/com/vjaykrsna/nanoai/telemetry/CoverageTelemetryReporter.kt` forwards layer deltas and actionable risks to the shared `TelemetryReporter` without PII.

## Integration with Design Principles
- **Automated quality gates**: `verifyCoverageThresholds` plugs into CI to block regressions automatically.
- **AI integrity**: Hugging Face auth flows (`app/src/test/java/com/vjaykrsna/nanoai/feature/settings/domain/huggingface`) are covered to guarantee slower polling and accessible failure copy—see `HuggingFaceAuthCoordinatorTest`.
- **Offline readiness**: Repository tests under `app/src/test/java/com/vjaykrsna/nanoai/feature/library/data` pair MockWebServer with Room to mimic flaky networks.
- **Material UX**: `app/src/androidTest/java/com/vjaykrsna/nanoai/coverage/ui/CoverageDashboardTest.kt` asserts TalkBack output and fallback banners.
- **Performance guardrails**: `macrobenchmark/src/main/java/com/vjaykrsna/nanoai/coverage/CoverageDashboardStartupBenchmark.kt` enforces the <100ms warm-load target and publishes results for CI artefacts.

## Adding or Updating Tests
1. **Start with a failing test** aligned to the task plan (see `specs/005-improve-test-coverage/tasks.md`).
2. **Select the right suite**:
   - Pure Kotlin or ViewModel logic → JVM tests.
   - Composable behaviour or end-to-end flows → instrumentation.
   - Performance regressions → macrobenchmark.
3. **Inject dependencies** via Hilt test components or fakes—avoid real network calls.
4. **Use deterministic clocks and dispatchers** (`kotlinx.datetime.Clock`, `MainDispatcherExtension`).
5. **Document scenarios** with concise KDoc and link relevant risk IDs where applicable so coverage dashboards stay meaningful.
6. **Run coverage tasks** before pushing to catch threshold regressions locally.

## Troubleshooting
- **Missing emulator**: `connectedDebugAndroidTest` will fail quickly—set `ANDROID_SERIAL` or launch an emulator via Android Studio / `emulator` CLI.
- **Instrumentation test filtering**: Use `-Pandroid.testInstrumentationRunnerArguments.class="*TestClass*"` since `--tests` is not supported for `connectedAndroidTest` tasks (they are `DeviceProviderInstrumentTestTask`, not standard `Test` tasks).
- **Offline instrumentation flakes**: After simulating offline states, clear the app cache with `adb shell pm clear com.vjaykrsna.nanoai` so MockWebServer fixtures rehydrate cleanly before reruns.
- **Coverage gaps reported**: Inspect `app/build/coverage/summary.md` for the failing layer, then open the HTML report to locate uncovered classes.
- **Risk register digest**: `docs/coverage/risk-register.md` summarises escalated items, mitigation owners, and linked JUnit5 suites.
- **Flaky tests**: Temporarily annotate with `@Tag("flaky")`, open an incident in the risk register, and prioritise stabilisation before release.

For quick onboarding, pair this guide with `specs/005-improve-test-coverage/quickstart.md` to rehearse the full workflow end to end.
