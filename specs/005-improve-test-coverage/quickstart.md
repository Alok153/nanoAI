# Quickstart: Improve Test Coverage for nanoAI

Follow this checklist to validate the coverage initiative end-to-end.

## 1. Workspace Prep
- Checkout branch `005-improve-test-coverage`.
- Sync dependencies: `./gradlew spotlessApply` (optional) then `./gradlew help` to warm Gradle.
- Ensure Android emulator or device available for instrumentation tests.

## 2. Run Automated Suites
- **ViewModel + Repository**: `./gradlew testDebugUnitTest`
  - Expect deterministic coroutine-based tests using `runTest` and fakes.
- **Compose UI (instrumented)**: `./gradlew connectedDebugAndroidTest`
  - Confirms Material accessibility semantics and offline flows via MockWebServer.

## 3. Generate Coverage Reports
- Execute `./gradlew jacocoFullReport` (custom task) to merge unit + instrumentation coverage.
- Locate outputs:
  - HTML: `app/build/reports/jacoco/full/index.html`
  - XML: `app/build/reports/jacoco/full/jacocoFullReport.xml`

## 4. Validate Threshold Enforcement
- Run `./gradlew verifyCoverageThresholds`.
  - Command fails if any layer drops below ViewModel 75%, UI 65%, Data 70%.
  - Inspect console summary highlighting offending modules.

## 5. Review Risk Register
- Open `docs/coverage/risk-register.md` (will be generated) and confirm all High/Critical items have assigned owners & mitigation builds.

## 6. Ensure New Principles Compliance
- Verify all new test code includes KDoc comments and has undergone peer review.
- Confirm coverage reports are published in CI artifacts with release notes documenting changes.

## 7. Update Stakeholders
- Publish coverage deltas in team channel with markdown snippet produced by the tooling (`/build/coverage/summary.md`).
- Record improvements + outstanding gaps in `docs/todo-next.md`.

## Troubleshooting
- If instrumentation tests fail due to emulator offline, rerun with `ANDROID_SERIAL` or mark for rerun while logging issue in risk register.
- Flaky test triage: label suites with `@FlakyTest` temporarily, open tracking ticket, and ensure coverage summary flags the reduced confidence.
