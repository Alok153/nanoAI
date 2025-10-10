# Phase 0 Research: Improve Test Coverage for nanoAI

## JaCoCo Integration Strategy
- **Decision**: Use Gradle JaCoCo plugin with unified `jacocoTestReport` task aggregating unit (`testDebugUnitTest`) and instrumentation (`connectedDebugAndroidTest`) coverage, exporting XML + HTML outputs.
- **Rationale**: Supports Kotlin/Compose projects, integrates cleanly with AGP 8.x, and feeds CI dashboards + GitHub PR annotations. XML enables future tooling, HTML serves stakeholders.
- **Alternatives Considered**:
  - **Kover**: Simpler Kotlin DSL but lacks instrumentation merge parity today.
  - **Firebase Test Lab metrics**: Powerful but adds cost/latency and requires infra changes.
  - **c8/nyc**: JavaScript-centric; misaligned with Kotlin bytecode.

## Compose UI Testing Patterns
- **Decision**: Adopt semantic node assertions with `SemanticsMatcher`, leverage `performClick`/`performTextInput` for user flows, and introduce accessibility checks (content descriptions, talkback labels) via `assertContentDescriptionEquals`.
- **Rationale**: Deterministic UI verification aligned with Material UX goals and accessible experiences; reduces flakiness vs screenshot comparisons.
- **Alternatives Considered**:
  - **Screenshot testing**: High maintenance with frequent golden updates.
  - **Manual QA only**: Does not satisfy automated quality gates.

## ViewModel & Repository Test Harness
- **Decision**: Standardize on `runTest` with `UnconfinedTestDispatcher`, use fake repositories/dispatchers via Hilt test bindings, and share fixture builders through a new `core/testing` module.
- **Rationale**: Ensures deterministic coroutine execution, isolates dependencies, and promotes reuse so coverage scales efficiently across ~8 ViewModels.
- **Alternatives Considered**:
  - **LiveDataTestUtil**: Legacy approach not aligned with Flow-first codebase.
  - **Real repositories in tests**: Slower, harder to debug.

## Offline & Failure Simulation
- **Decision**: Add Room in-memory database for DAO tests, pair with Retrofit MockWebServer scenarios for error handling, and exercise offline toggles via repository flags.
- **Rationale**: Covers Resilient Performance & Offline Readiness with fast feedback while mimicking production pathways.
- **Alternatives Considered**:
  - **Device-only instrumentation**: Accurate but slower; keep for smoke tests, not full suite.
  - **Pure mocks**: Misses SQL schema regressions.

## Coverage Reporting & Stakeholder Visibility
- **Decision**: Publish HTML reports under `app/build/reports/jacoco/` and upload XML summary as CI artifact; add summary parser generating markdown snippet for PRs and changelog.
- **Rationale**: Balances human-readable dashboards with automation hooks; aligns with requirement to monitor trend data and new CI/CD principles for automated publishing and rollback capabilities.
- **Alternatives Considered**:
  - **SonarQube integration**: Feature-rich but overkill and adds infra complexity.
  - **Badge-only reporting**: Lacks layer granularity demanded by stakeholders.
