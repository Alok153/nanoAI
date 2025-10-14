# Coverage Risk Register

This register captures the open coverage gaps surfaced during the 005-improve-test-coverage initiative. Each item includes the enforcing test assets, mitigation owner, and status so release stewards can triage quickly.

## Summary
- Critical risks: 1 (escalated via automation)
- High risks: 2 (actively mitigated)
- Medium/Low risks: 0 (resolved or deferred out of scope)
- Latest report: see `app/build/coverage/summary.md` (generated 2025-10-12 via unit-only fallback run)

### Latest Coverage Snapshot (2025-10-12)

| Layer | Coverage | Threshold | Delta | Status |
| --- | ---: | ---: | ---: | --- |
| View Model | 39.58% | 75.00% | -35.42pp | BELOW_TARGET |
| UI | 1.90% | 65.00% | -63.10pp | BELOW_TARGET |
| Data | 18.91% | 70.00% | -51.09pp | BELOW_TARGET |

> **Note**: Instrumentation coverage was skipped locally using `-Pnanoai.skipInstrumentation=true` because managed virtual devices require hardware virtualization. CI runs must execute `./gradlew ciManagedDeviceDebugAndroidTest jacocoFullReport` to collect full unit + instrumentation coverage before sign-off.

## Escalated Risks
| Risk ID | Severity | Description | Mitigation Owner | Target Build | Linked Tests | Status |
| --- | --- | --- | --- | --- | --- | --- |
| RR-CRIT-041 | CRITICAL | Offline coverage dashboard fails to surface error banner when MockWebServer is unavailable. | Coverage Core (Priya N.) | r2025.42 | `app/src/androidTest/java/com/vjaykrsna/nanoai/coverage/ui/CoverageDashboardTest.kt` | IN_PROGRESS |
| RR-HIGH-027 | HIGH | Risk register coordinator escalates stale HIGH risks even after mitigation tags applied. | Quality Engineering (Miguel A.) | r2025.43 | `app/src/test/java/com/vjaykrsna/nanoai/coverage/RiskRegisterCoordinatorTest.kt` | OPEN |
| RR-HIGH-033 | HIGH | Hugging Face auth polling ignores `slow_down` hint, making retries inaccessible for screen-reader users. | AI Platform (Chin-Ling W.) | r2025.42 | `app/src/test/java/com/vjaykrsna/nanoai/feature/settings/domain/huggingface/HuggingFaceAuthCoordinatorTest.kt` | IN_PROGRESS |

## Mitigation Notes
- RR-CRIT-041 remains escalated until the dashboard renders the offline banner under 2 seconds with TalkBack descriptions validated. Instrumentation coverage now fails the build when the regression reproduces.
- RR-HIGH-027 requires synchronising the risk tag catalog with `TestSuiteCatalogEntry` metadata; once mitigations land, rerun `CoverageReportContractTest` to confirm sorting and tag coverage.
- RR-HIGH-033 is tracked through Hugging Face auth telemetry—ensure `slow_down` copy changes land before QA sign-off and rerun the coordinator tests on the Jupiter platform.

## Resolved Risks (Phase 005 Completion)

The following risks were successfully mitigated during the 005-improve-test-coverage feature implementation:

| Risk ID | Original Severity | Description | Resolution | Resolved Date |
| --- | --- | --- | --- | --- |
| RR-MED-001 | MEDIUM | Test environment contamination causing flaky first-launch scenarios | Introduced `TestEnvironmentRule` (T002) to reset DataStore/Room state before each test | 2025-10-13 |
| RR-MED-002 | MEDIUM | Managed device ABI instability on API 34 emulators | Stabilized with x86_64 ABI configuration in T001 | 2025-10-13 |
| RR-HIGH-034 | HIGH | Missing disclaimer flow test coverage | Added comprehensive tests in T013 and wired disclaimer UI state in T047-T048 | 2025-10-13 |
| RR-MED-003 | MEDIUM | Model catalog refresh lacks offline fallback tests | Implemented cached-fallback logic in T037 with comprehensive tests in T011, T014 | 2025-10-13 |
| RR-MED-004 | MEDIUM | Coverage reporting lacks structured JSON output | Added JSON schema contract (T003) and implemented in T050 | 2025-10-13 |
| RR-LOW-001 | LOW | JUnit4 legacy dependencies blocking modern test patterns | Completed JUnit4 → JUnit5 migration across all test suites | 2025-10-13 |

## TODO Items Deferred

The following items remain out of scope for the current phase and are tracked in `docs/todo-next.md`:

- **TODO-001**: Enable full instrumentation coverage on CI with hardware virtualization (currently falling back to unit-only locally).
- **TODO-002**: Implement automated risk escalation notifications when Critical risks remain open past target build.
- **TODO-003**: Add performance macrobenchmarks for coverage dashboard warm-load (<100ms target) to validate UI responsiveness under load.
- **TODO-004**: Expand schema validation in contract tests to cover all edge cases and error scenarios.
- **TODO-005**: Create automated coverage trend dashboards with historical data visualization for stakeholder review.
- **TODO-006**: Implement coverage badge generation for README and PR review visibility.
- **TODO-007**: Add telemetry for coverage trend monitoring (scaffolded in `CoverageTelemetryReporter` but not yet connected to analytics backend).

### Priority Deferred Items (Next Sprint)
- **P0**: Close coverage gaps to reach target thresholds (VM 75%, UI 65%, Data 70%) - see `docs/todo-next.md` Phase 1.
- **P1**: Address remaining escalated risks (RR-CRIT-041, RR-HIGH-027, RR-HIGH-033) before next production release.
- **P2**: Enable full CI instrumentation with managed devices to validate complete coverage metrics.

## How To Update
1. Run `./gradlew ciManagedDeviceDebugAndroidTest jacocoFullReport verifyCoverageThresholds` (or `./gradlew jacocoFullReport verifyCoverageThresholds -Pnanoai.skipInstrumentation=true` when virtualization is unavailable) to refresh coverage artefacts.
2. Inspect `app/build/coverage/summary.md` for risk deltas and update the table above.
3. Link any new automated tests or manual mitigations so stakeholders can validate the fix path.
4. Commit updates to this register alongside associated test or documentation changes.
