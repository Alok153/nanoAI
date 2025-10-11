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
| Macrobenchmarks | `macrobenchmark/src/main/java` | App startup and dashboard performance | AndroidX Macrobenchmark |
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
2. **Instrumentation & Compose UI tests** (requires emulator or device)
   ```bash
   ./gradlew connectedDebugAndroidTest
   ```
   Produces reports under `app/build/reports/androidTests/connected/` and `.ec` coverage files.
3. **Macrobenchmarks** (optional, CI-only by default)
   ```bash
   ./gradlew :macrobenchmark:connectedCheck
   ```

### Recommended Development Loop
- Run targeted JVM tests via `--tests` (e.g. `--tests "com.vjaykrsna.nanoai.coverage.*"`).
- Before submitting changes run `./gradlew check` to execute formatting, lint, tests, merged coverage, and threshold verification.

## Coverage Workflow
1. **Merge coverage**
   ```bash
   ./gradlew jacocoFullReport
   ```
   Outputs XML + HTML under `app/build/reports/jacoco/full/` and depends on both unit & instrumentation tests.
2. **Verify thresholds**
   ```bash
   ./gradlew verifyCoverageThresholds
   ```
   Fails if any layer drops below mandated ratios. This task already runs as part of `check`.
3. **Publish markdown summary**
   ```bash
   ./gradlew coverageMarkdownSummary
   ```
   Calls `scripts/coverage/generate-summary.py` to emit `app/build/reports/jacoco/full/summary.md` for PR or Slack updates.
4. **CI merge helper**
   ```bash
   ./gradlew coverageMergeArtifacts
   ```
   Invokes `scripts/coverage/merge-coverage.sh` to collect `.exec`/`.ec` files in CI environments before the report runs.

### Coverage Artefacts
- **Contract schema**: `specs/005-improve-test-coverage/contracts/coverage-report.schema.json` documents the JSON report shape expected by stakeholders.
- **Report generator**: `app/src/main/java/com/vjaykrsna/nanoai/coverage/domain/CoverageReportGenerator.kt` enforces severity ordering and ensures every risk is mitigated by a catalog entry.
- **Risk register alignment**: Tests in `app/src/test/java/com/vjaykrsna/nanoai/coverage/` validate sorting, tag enforcement, and risk escalation logic.

## Integration with Design Principles
- **Automated quality gates**: `verifyCoverageThresholds` plugs into CI to block regressions automatically.
- **AI integrity**: Hugging Face auth flows (`app/src/test/java/com/vjaykrsna/nanoai/feature/settings/domain/huggingface`) are covered to guarantee slower polling and accessible failure copy—see `HuggingFaceAuthCoordinatorTest`.
- **Offline readiness**: Repository tests under `app/src/test/java/com/vjaykrsna/nanoai/feature/library/data` pair MockWebServer with Room to mimic flaky networks.
- **Material UX**: `app/src/androidTest/java/com/vjaykrsna/nanoai/coverage/ui/CoverageDashboardTest.kt` asserts TalkBack output and fallback banners.

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
- **Coverage gaps reported**: Inspect `summary.md` for the failing layer, then open the HTML report to locate uncovered classes.
- **Flaky tests**: Temporarily annotate with `@Tag("flaky")`, open an incident in the risk register, and prioritise stabilisation before release.

For quick onboarding, pair this guide with `specs/005-improve-test-coverage/quickstart.md` to rehearse the full workflow end to end.
