# Quality Gates

NanoAI enforces automated gates across formatting, static analysis, coverage, and visual
regression. Passing every gate is required before merging to `main`; bypasses demand explicit
approval from the Eng Lead + QE counterpart and must be documented in the PR description.

## Summary Matrix

| Gate | Command | Threshold / Expectation | Reports |
| --- | --- | --- | --- |
| Formatting | `./gradlew spotlessCheck` | No formatting violations | N/A (fails fast) |
| Static analysis | `./gradlew detekt detektMain detektTest` | No detekt findings; clean-architecture rule enforced | `build/reports/detekt/*.html` |
| Coverage | `./gradlew :app:verifyCoverageThresholds` | ≥75 % ViewModel, ≥65 % UI, ≥70 % Data (from `coverage-metadata.json`) | `app/build/coverage/thresholds.{md,json}` |
| Screenshot regression | `./gradlew :app:roboScreenshotDebug` (verify mode in CI) | No unexpected diffs; baselines under `app/src/test/screenshots` | Test run output + Roborazzi artefacts |
| Instrumentation health | `./gradlew ciManagedDeviceDebugAndroidTest` | All tests pass on managed ATD Pixel 6 | `app/build/reports/androidTests/managedDebug/index.html` |

## Gate Details

### 1. Formatting (Spotless)
- Applies to `.kt` + `.gradle.kts` via `config/quality/spotless/spotless.*.gradle`
- Auto-fix before pushing: `./gradlew spotlessApply`
- CI gate is strict; no bypass policy (run `spotlessApply`).

### 2. Static Analysis (Detekt)
- Type-resolution enabled across modules; configuration lives in `config/quality/detekt/detekt.yml`
- Custom rule set `nanoai-clean-architecture` blocks feature→data and core→feature dependencies
- Fix violations or add rationale + ticket link before requesting bypass

### 3. Coverage Verification
- `verifyCoverageThresholds` parses JaCoCo XML and metadata from `config/testing/coverage/coverage-metadata.json`
- Thresholds may only be lowered through a staffed RFC; increasing thresholds is encouraged when
  risk justifies it
- Failing gate requires either test additions or documented risk acceptance signed by Product + QE

### 4. Roborazzi Screenshot Diffs
- `roboScreenshotDebug` records baselines locally. CI runs in verify mode to catch unexpected UI
  regressions
- Baselines are version-controlled (`app/src/test/screenshots`). Intentional changes require updated
  baselines in the same PR
- Temporary bypass permitted only for hotfixes with follow-up issue within 24 h

### 5. Instrumentation Reliability
- Managed-device suite (`ciManagedDeviceDebugAndroidTest`) ensures Compose UI, accessibility and
  offline scenarios stay healthy
- Flaky tests must be quarantined with `@Ignore` + tracking ticket; long-term skips are not allowed

## Recommended Order Before Pushing
1. `./gradlew spotlessApply detekt`
2. `./gradlew testDebugUnitTest`
3. `./gradlew :app:verifyCoverageThresholds`
4. `./gradlew :app:roboScreenshotDebug` (when UI changes)
5. `./gradlew check`

## Bypass Process
1. Document the failing gate, justification, and mitigation ETA in the PR description.
2. Obtain written approval from Eng Lead **and** QE owner.
3. Add `quality-gate-bypass` label to the PR and link to tracking issue.
4. Remove bypass ASAP—carry debt < 2 working days.

All bypasses are audited during release readiness reviews.
