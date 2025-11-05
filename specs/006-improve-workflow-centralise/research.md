# Research Findings: Improve Workflow – Centralise Build Logic

## Build Logic Conventions
- **Decision**: Introduce a `build-logic` composite build containing typed convention plugins for application, feature, library, and shared configurations.
- **Rationale**: Centralising plugin configuration keeps module `build.gradle.kts` files declarative while ensuring consistent compiler flags, lint/format tasks, and dependency alignment. Gradle recommends composite builds with precompiled script plugins over `buildSrc` for better configuration caching and parallelism.
- **Alternatives considered**: Maintaining duplicated blocks in each module (hard to keep in sync); reviving `buildSrc` (loses configuration cache benefits and obscures plugin boundaries).

## Static Analysis – Detekt & Spotless
- **Decision**: Enable Detekt type resolution across modules and build a custom rule set enforcing clean architecture and forbidden layer dependencies; keep Spotless configuration centralised alongside Detekt for consistency.
- **Rationale**: Type resolution unlocks rules that catch unsafe nullability, incorrect calls across layers, and deprecated APIs. Custom rules convert constitution requirements (e.g., no ViewModel → DataSource access) into automated gates. Housing Spotless settings next to Detekt clarifies ownership of quality tooling.
- **Alternatives considered**: Rely only on default Detekt rules (misses architecture violations); defer custom rules to manual reviews (non-scalable); keep Spotless config in each module (still fragmented).

## Test Tooling – Flow & UI Verification
- **Decision**: Standardise Flow testing on Turbine (already present) by documenting usage patterns and providing shared coroutine test utilities; adopt Roborazzi for JVM screenshot tests with Robolectric-backed Compose rendering.
- **Rationale**: Existing tests already use Turbine sporadically; formalising helpers ensures consistent cancellation, timeouts, and dispatchers. Roborazzi integrates with Robolectric/JUnit5, enabling deterministic screenshot diffs without emulator farms.
- **Alternatives considered**: Continue ad-hoc Flow assertions (risk flaky tests); use Paparazzi/Shot for screenshots (weaker Compose + Hilt support, higher maintenance).

## Target & Min SDK Verification
- **Decision**: Document compile/target SDK 36 and minSdk 31 (sourced from `app/build.gradle.kts`), and surface them in onboarding docs and convention plugin defaults.
- **Rationale**: Keeping SDK values inside conventions avoids drift and ensures new modules inherit correct levels automatically.
- **Alternatives considered**: Leave values in individual modules (centralisation goal unmet); rely on documentation only (prone to rot).

## Build Performance Baselines
- **Decision**: Capture baseline timings for `detekt`, `spotlessCheck`, and `verifyCoverageThresholds` via Gradle build scans before and after changes; store summary in `/docs/development/BUILD_WORKFLOW.md`.
- **Rationale**: Establishing benchmarks validates success metrics (detekt < 60s, coverage < 180s) and quantifies cache improvements. Build scans provide reproducible timing data and environment metadata.
- **Alternatives considered**: Rely on anecdotal timings (unreliable); add custom logging in Gradle scripts (duplicates scan features).

## Quality Gate Automation
- **Decision**: Ship pre-commit hooks (POSIX-compatible) calling Spotless, Detekt, unit tests, and coverage verification with guidance for Windows PowerShell equivalent; integrate hooks install script into onboarding.
- **Rationale**: Local enforcement shortens feedback loops and fulfils constitution automation mandate. Providing scripts plus docs reduces friction for developers adopting hooks.
- **Alternatives considered**: Depend solely on CI (delayed feedback); require manual hook setup without scripts (higher onboarding burden).

## Build Scan Enhancements
- **Decision**: Configure Gradle Enterprise build scans (or free scans) with custom tags/values capturing branch, CI job URL, quality gate outcomes, and slow task alerts.
- **Rationale**: Rich metadata accelerates triage of failed or slow builds and helps track performance regressions across commits.
- **Alternatives considered**: Default build scans (limited context); bespoke logging solution (reinvents build scan capabilities).

## Documentation & Onboarding
- **Decision**: Produce `BUILD_WORKFLOW.md` and refresh `TESTING.md`/`QUALITY_GATES.md` with centralised commands, hook installation, and troubleshooting; add robodocs snippet referencing constitution principles.
- **Rationale**: Aligns with constitution documentation mandate and reduces onboarding time to <2 hours per success criteria.
- **Alternatives considered**: Update existing docs piecemeal (risk inconsistent messaging); rely on AGENTS.md alone (too high-level for onboarding).
