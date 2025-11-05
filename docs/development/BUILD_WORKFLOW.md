# Build & Workflow Guide

## Convention Plugin Overview

NanoAI modules use convention plugins from the `build-logic` composite build:

- `com.vjaykrsna.nanoai.android.application` – application defaults, coverage wiring, Roborazzi tasks
- `com.vjaykrsna.nanoai.android.feature` – feature module Compose configuration
- `com.vjaykrsna.nanoai.android.library.compose` – shared Compose library setup
- `com.vjaykrsna.nanoai.android.testing` – instrumentation-only modules such as `:macrobenchmark`
- `com.vjaykrsna.nanoai.kotlin.library` – pure Kotlin libraries shared across layers

Each plugin applies Spotless + Detekt, provides baseline dependencies, and registers verification
hooks so `./gradlew check` runs formatting, static analysis, and coverage gates automatically.

## Everyday Build Commands

| Goal | Command | Notes |
| --- | --- | --- |
| Build debug APK | `./gradlew assembleDebug` | Uses centralized Android SDK + compiler flags |
| Run static analysis | `./gradlew detekt detektMain detektTest` | Includes custom clean-architecture rule set |
| Format sources | `./gradlew spotlessApply` | Kotlin + Gradle scripts configured from `config/quality/spotless` |
| Verify coverage thresholds | `./gradlew :app:verifyCoverageThresholds` | Enforces metadata from `config/testing/coverage/coverage-metadata.json` |
| Run unit tests | `./gradlew testDebugUnitTest` | Flow helpers live under `com.vjaykrsna.nanoai.shared.testing` |
| Record screenshots | `./gradlew :app:roboScreenshotDebug` | Outputs to `app/src/test/screenshots` |
| Merge coverage artefacts | `./gradlew :app:coverageMergeArtifacts` | Invokes `scripts/coverage/merge-coverage.sh` |

## Quality Gates

All convention plugins wire `spotlessCheck`, `detekt`, `verifyCoverageThresholds`, and (for the
application module) `roboScreenshotDebug` into the `check` lifecycle. Running `./gradlew check`
provides a full signal that can be consumed locally or in CI. Coverage thresholds are defined in
`config/testing/coverage/coverage-metadata.json` and exposed through `coverage-thresholds.gradle.kts`.

### Local Hook Installation

1. Run `scripts/hooks/install-hooks.sh` to symlink the repository-managed `pre-commit` hook.
2. Validate without executing tasks by running `scripts/hooks/pre-commit.sh --dry-run`.
3. For full verification, execute `scripts/hooks/pre-commit.sh`; the hook mirrors the CI `check`
   footprint (formatting, Detekt, unit tests, coverage verification).
4. Use `scripts/hooks/install-hooks.sh --force` if another team has already created a hook and you
   need to replace it with the shared automation.

If your filesystem blocks symlinks (Windows without developer mode), copy `scripts/hooks/pre-commit.sh`
into `.git/hooks/pre-commit` manually and mark it executable.

### Bypass & Troubleshooting

- Follow the bypass checklist from `docs/development/QUALITY_GATES.md` whenever you need to skip a
  gate. Document approvals in the pull request template.
- When the pre-commit script fails, inspect the suggestion emitted in the terminal. Reports live in
  `build/reports/detekt`, `app/build/reports/tests`, and `app/build/coverage/`.
- Re-run `scripts/hooks/pre-commit.sh --verbose` to surface full Gradle logs when investigating
  stubborn failures.

## Coverage Workflow

1. Execute unit tests: `./gradlew testDebugUnitTest`
2. Generate merged JaCoCo report + coverage summary: `./gradlew :app:jacocoFullReport :app:coverageMarkdownSummary`
3. Enforce thresholds: `./gradlew :app:verifyCoverageThresholds`
4. (Optional) Merge and archive artefacts for CI: `./gradlew :app:coverageMergeArtifacts`

Both the verification task and markdown summary load layer thresholds from
`config/testing/coverage/coverage-metadata.json`, ensuring the Gradle verification step and CI
reporting stay in sync.

## Screenshot & UI Regression Flow

- Roborazzi tasks live in `config/testing/tooling/roborazzi-config.gradle.kts`.
- Recording new baselines: `./gradlew :app:roboScreenshotDebug`
- Baselines are stored under `app/src/test/screenshots` and should be committed when UX changes.
- Tests consume the same output directory when running in verify mode, enabling CI diffs.

## Flow Testing Helpers

`app/src/test/java/com/vjaykrsna/nanoai/shared/testing/FlowTestExt.kt` exposes two helpers:

- `flow.testFlow { ... }` for simple Turbine assertions inside `runTest`
- `testScope.testFlow(flow) { ... }` to launch and drain flows while the TestScope advances its
  virtual clock

Convention plugins automatically add Turbine + coroutines test dependencies so helpers are available
in both unit and instrumentation source sets.

## Recommended Development Loop

1. Format + static analysis: `./gradlew spotlessApply detekt`
2. Targeted test command from [`config/testing/tooling/test-commands.json`](./test-commands.json)
3. Coverage enforcement: `./gradlew :app:verifyCoverageThresholds`
4. Optional screenshot updates: `./gradlew :app:roboScreenshotDebug`

Gradle Build Scans (`./gradlew tasks --scan`) remain available for deeper diagnostics and
performance metrics.
