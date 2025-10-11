# Tasks: Improve Test Coverage for nanoAI

**Input**: Design documents from `specs/005-improve-test-coverage/`
**Prerequisites**: plan.md, research.md, data-model.md, contracts/, quickstart.md

## Phase 3.1: Setup
- [X] T001 Update `gradle/libs.versions.toml` to add `org.junit.jupiter` (api, params, engine) and `org.junit.platform` launcher aliases, align MockK/Truth dependencies, and drop the legacy `junit` alias so JVM tests compile against JUnit5 only.
- [X] T002 Configure `app/build.gradle.kts` to enable JUnit Platform (`tasks.withType<Test> { useJUnitPlatform() }`), wire `testImplementation` to the new Jupiter aliases, move Mockito/MockK exclusions, and register `jacocoFullReport` + `verifyCoverageThresholds` under `check` for CI usage.
- [X] T003 Replace `app/src/test/java/com/vjaykrsna/nanoai/testing/MainDispatcherRule.kt` with a JUnit5 `MainDispatcherExtension`, update affected tests in `app/src/test/java/**` to use `@ExtendWith` / `@RegisterExtension`, and delete remaining `@Rule` references.

## Phase 3.2: Tests First (TDD)
- [X] T004 [P] Refactor `app/src/test/java/com/vjaykrsna/nanoai/coverage/CoverageReportContractTest.kt` to JUnit5 and add a failing test that asserts risk register entries are sorted by severity order (CRITICAL → HIGH → MEDIUM → LOW) and reject mismatched catalog risk tags.
- [X] T005 [P] Create `app/src/test/java/com/vjaykrsna/nanoai/coverage/model/CoverageMetricTest.kt` with JUnit5 cases covering `EXCEEDS_TARGET` status and positive `deltaFromThreshold()` when coverage > threshold.
- [X] T006 [P] Convert `app/src/test/java/com/vjaykrsna/nanoai/coverage/CoverageSummaryTest.kt` to JUnit5 and add a failing test for a new `statusBreakdown()` helper returning counts per `CoverageMetric.Status`.
- [X] T007 [P] Add `app/src/test/java/com/vjaykrsna/nanoai/coverage/model/CoverageTrendPointTest.kt` verifying a new `deltaFromThreshold()` helper and that non-monotonic `recordedAt` values throw.
- [X] T008 [P] Add `app/src/test/java/com/vjaykrsna/nanoai/coverage/model/RiskRegisterItemTest.kt` with assertions for `isActionable(now)` semantics and CRITICAL risks without `targetBuild` throwing.
- [X] T009 [P] Add `app/src/test/java/com/vjaykrsna/nanoai/coverage/model/TestSuiteCatalogEntryTest.kt` covering `mitigatesRisk(riskId)` and rejecting blank owners under JUnit5.
- [X] T010 [P] Add `app/src/test/java/com/vjaykrsna/nanoai/coverage/model/TestLayerTest.kt` asserting `machineName` camelCase and a new kebab-case `analyticsKey`.
- [X] T011 [P] Migrate `app/src/test/java/com/vjaykrsna/nanoai/coverage/RiskRegisterCoordinatorTest.kt` to JUnit5 and add a failing case where `requiresAttention(now)` only escalates overdue High/Critical risks with unmitigated tags.
- [X] T012 [P] Migrate `app/src/test/java/com/vjaykrsna/nanoai/feature/settings/domain/huggingface/HuggingFaceAuthCoordinatorTest.kt` to JUnit5 and add a failing `slow_down` poll-backoff scenario asserting accessible error copy.
- [X] T013 [P] Create `app/src/test/java/com/vjaykrsna/nanoai/feature/library/domain/RefreshModelCatalogUseCaseTest.kt` covering success and failure Result semantics under JUnit5.
- [X] T014 [P] Create `app/src/test/java/com/vjaykrsna/nanoai/feature/library/data/impl/ModelCatalogRepositoryImplTest.kt` validating `replaceCatalog` metadata preservation and `deleteModelFiles` cleanup.
- [X] T015 [P] Expand `app/src/androidTest/java/com/vjaykrsna/nanoai/coverage/ui/CoverageDashboardTest.kt` with Espresso/JUnit5 extension assertions for offline fallback (error banner, TalkBack description) using MockWebServer.
- [X] T016 [P] Add `app/src/androidTest/java/com/vjaykrsna/nanoai/feature/library/data/ModelCatalogOfflineTest.kt` exercising Room in-memory DB + MockWebServer to cover the quickstart offline device farm scenario.

## Phase 3.3: Core Implementation (after tests fail)
- [X] T017 Update `app/src/main/java/com/vjaykrsna/nanoai/coverage/model/CoverageMetric.kt` to introduce `EXCEEDS_TARGET` branching, companion constants for bounds, and an `isExceedingTarget()` helper.
- [X] T018 Update `app/src/main/java/com/vjaykrsna/nanoai/coverage/model/CoverageSummary.kt` to expose `statusBreakdown()`, ensure `trendDeltaFor` defaults to `0.0`, and reuse the new metric helpers.
- [X] T019 Update `app/src/main/java/com/vjaykrsna/nanoai/coverage/model/CoverageTrendPoint.kt` to cache `deltaFromThreshold()` and validate non-decreasing `recordedAt` sequences.
- [X] T020 Update `app/src/main/java/com/vjaykrsna/nanoai/coverage/model/RiskRegisterItem.kt` with an `isActionable(now: Instant)` helper and stricter CRITICAL target validation.
- [X] T021 Update `app/src/main/java/com/vjaykrsna/nanoai/coverage/model/TestSuiteCatalogEntry.kt` to add `mitigatesRisk(riskId: String)` and guard blank tag sets.
- [X] T022 Update `app/src/main/java/com/vjaykrsna/nanoai/coverage/model/TestLayer.kt` to expose a new `analyticsKey` property derived from enum names.
- [X] T023 Update `app/src/main/java/com/vjaykrsna/nanoai/coverage/domain/RiskRegisterCoordinator.kt` to honour `targetBuild` deadlines, reuse `isActionable`, and remove the constant return from `shouldEscalate`.
- [X] T024 Update `app/src/main/java/com/vjaykrsna/nanoai/coverage/domain/CoverageReportGenerator.kt` to sort risk register output by severity and assert suite catalog tags cover referenced risks.
- [X] T025 Update `app/src/main/java/com/vjaykrsna/nanoai/feature/settings/domain/huggingface/HuggingFaceAuthCoordinator.kt` to expand `slow_down` handling, expose surfaced error copy, and ensure polling interval caps respect accessibility guidelines.
- [X] T026 Update `app/src/main/java/com/vjaykrsna/nanoai/feature/library/domain/RefreshModelCatalogUseCase.kt` to propagate failure causes, emit structured logs, and return `Result.failure` when repository writes fail.
- [X] T027 Update `app/src/main/java/com/vjaykrsna/nanoai/feature/library/data/impl/ModelCatalogRepositoryImpl.kt` to preserve integrity metadata during `replaceCatalog` and harden file cleanup for offline deletions.

## Phase 3.4: Integration
- [X] T028 Implement `app/src/main/java/com/vjaykrsna/nanoai/coverage/tasks/VerifyCoverageThresholdsTask.kt` (plus Gradle wiring) to invoke `CoverageThresholdVerifier`, emit summary markdown, and fail the build when tests detect regressions.
- [X] T029 Extend `scripts/coverage/merge-coverage.sh` to merge unit + instrumentation Jacoco exec files, honour the new thresholds, and copy artifacts to `app/build/reports/jacoco/full/`.
- [ ] T030 Enhance `scripts/coverage/generate-summary.py` to ingest the merged XML, compute the status breakdown, and refresh `app/build/coverage/summary.md` for stakeholder broadcasts.
- [ ] T031 Add or update `app/src/main/java/com/vjaykrsna/nanoai/telemetry/CoverageTelemetryReporter.kt` so coverage deltas and risk escalations are logged without PII and wired into existing telemetry dispatchers.

## Phase 3.5: Polish
- [ ] T032 [P] Refresh `specs/005-improve-test-coverage/quickstart.md` with JUnit5 commands, emulator prerequisites, and troubleshooting for the new offline tests.
- [ ] T033 [P] Generate `docs/coverage/risk-register.md` summarising escalated risks, mitigation owners, and links to the new tests.
- [ ] T034 [P] Add `macrobenchmark/src/main/java/com/vjaykrsna/nanoai/coverage/CoverageDashboardStartupBenchmark.kt` validating dashboard load <100ms and capture results in CI artifacts.
- [ ] T035 [P] Update `README.md` testing section with JUnit5 migration notes, coverage thresholds, and commands for `jacocoFullReport` + `verifyCoverageThresholds`.

## Dependencies
- Setup tasks T001 → T003 must finish before any test conversions (T004–T016).
- Each test task T004–T016 precedes its matching implementation task (T017–T027) to preserve TDD.
- Integration tasks T028–T031 depend on core implementation updates through T027.
- Polish tasks T032–T035 require prior sections so documentation reflects final behaviour.

## Parallel Example
```
# Run independent JUnit5 test migrations together once setup is complete
task-agent run T005
task-agent run T006
task-agent run T007

# Batch instrumentation scenarios after Gradle + unit migrations
task-agent run T015
task-agent run T016
```

## Notes
- Honour constitution gates: Material accessibility checks (T015), offline resilience (T016, T027), automation gates (T028–T030), AI integrity via Hugging Face auth (T012, T025), and documentation freshness (T032–T035).
- Coordinate with CI owners before enabling the new `verifyCoverageThresholds` gate to avoid blocking existing pipelines.
