# Implementation Plan: Improve Workflow – Centralise Build Logic

**Branch**: `006-improve-workflow-centralise` | **Date**: 2025-11-05 | **Spec**: [/specs/006-improve-workflow-centralise/spec.md](/specs/006-improve-workflow-centralise/spec.md)
**Input**: Feature specification from `/specs/006-improve-workflow-centralise/spec.md`

**Note**: This template is filled in by the `/speckit.plan` command. See `.specify/templates/commands/plan.md` for the execution workflow.

## Summary

Centralise Gradle build configuration via convention plugins and a shared version catalog, enhance static analysis with type-aware Detekt plus custom rules, and streamline the testing toolchain (coverage enforcement, Flow/Compose testing helpers, screenshot baselines) so developer workflows stay consistent, automated, and fast.

## Technical Context

<!--
  ACTION REQUIRED: Replace the content in this section with the technical details
  for the project. The structure here is presented in advisory capacity to guide
  the iteration process.
-->

**Language/Version**: Kotlin 2.2.0 (JDK 17), Kotlin DSL for Gradle
**Primary Dependencies**: Android Gradle Plugin 8.13.0, Hilt, Jetpack Compose Material 3, WorkManager, Room, Detekt, Spotless, Gradle Build Scans, Turbine, planned Roborazzi integration
**Storage**: Room (SQLite) and DataStore; encryption handled via EncryptedSecretStore (no changes expected)
**Testing**: JUnit5 + Kotlin Coroutines Test, Robolectric, Compose UI test APIs; Turbine already adopted for Flow assertions, standardise helpers and introduce Roborazzi for screenshot coverage
**Target Platform**: Android mobile app (minSdk 31, compile/target SDK 36 propagated through conventions)
**Project Type**: Modular Android application with single-activity architecture and multiple feature/core modules
**Performance Goals**: `./gradlew detekt` < 60s, `./gradlew verifyCoverageThresholds` < 180s, build cache hit rate +40%, no regression to cold start <1.5s; capture baselines via Gradle build scans before rollout
**Constraints**: Enforce clean architecture layers, offline readiness, automated quality gates (spotless, detekt, coverage), Kotlin-only code additions
**Scale/Scope**: 1 application module (`:app`), 6 feature modules, 9 core submodules, macrobenchmark module, CI pipelines (GitHub Actions) – impact spans all modules and developer toolchain

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- **Kotlin-First Clean Architecture**: Plan reinforces Kotlin-only tooling and clearer module boundaries. ✅
- **Automated Quality Gates**: Expands Detekt/Spotless coverage and adds pre-commit integration—aligned but must ensure CI hooks stay mandatory. ✅
- **Documentation & Knowledge Sharing**: Requires updated docs (quickstart, testing, quality gates) and onboarding guidance—must be delivered in Phase 1 artifacts. ✅
- **Streamlined and Clean Codebase**: Convention plugins reduce duplication without overengineering; ensure complexity justified if custom Gradle tasks exceed needs. ✅
- **Resilient Performance & Offline Readiness**: Build improvements must not compromise runtime/offline behavior; watch for heavier lint tasks impacting CI time. ✅

No constitution violations identified; proceed with Phase 0 research.

*Re-check after Phase 1 design*: Proposed conventions, tooling modules, and documentation updates remain compliant; no additional violations detected.

## Project Structure

### Documentation (this feature)

```
specs/[###-feature]/
├── plan.md              # This file (/speckit.plan command output)
├── research.md          # Phase 0 output (/speckit.plan command)
├── data-model.md        # Phase 1 output (/speckit.plan command)
├── quickstart.md        # Phase 1 output (/speckit.plan command)
├── contracts/           # Phase 1 output (/speckit.plan command)
└── tasks.md             # Phase 2 output (/speckit.tasks command - NOT created by /speckit.plan)
```

### Source Code (repository root)
<!--
  ACTION REQUIRED: Replace the placeholder tree below with the concrete layout
  for this feature. Delete unused options and expand the chosen structure with
  real paths (e.g., apps/admin, packages/something). The delivered plan must
  not include Option labels.
-->

```
build-logic/
├── build.gradle.kts
├── settings.gradle.kts
├── convention/
│   ├── android-application.gradle.kts
│   ├── android-library-compose.gradle.kts
│   ├── android-feature.gradle.kts
│   ├── kotlin-library.gradle.kts
│   └── shared-config.gradle.kts
└── src/main/kotlin/
  └── com/vjaykrsna/nanoai/buildlogic/
    ├── AndroidApplicationConventionPlugin.kt
    ├── AndroidFeatureConventionPlugin.kt
    ├── AndroidLibraryComposeConventionPlugin.kt
    ├── KotlinLibraryConventionPlugin.kt
    └── SharedConfiguration.kt

config/quality/
├── detekt/
│   ├── detekt.yml
│   └── custom-rules/
│       ├── build.gradle.kts
│       └── src/main/kotlin/
│           └── com/vjaykrsna/nanoai/detektrules/
│               ├── CleanArchitectureRuleSetProvider.kt
│               └── CleanArchitectureRule.kt
├── spotless/
│   ├── spotless.kotlin.gradle
│   ├── spotless.misc.gradle
│   └── templates/
└── hooks/
  ├── pre-commit.sh
  └── README.md

config/testing/
├── coverage/
│   ├── layer-map.json
│   └── coverage-thresholds.gradle.kts
├── tooling/
│   ├── turbine-setup.md
│   └── roborazzi-config.gradle.kts
└── scripts/
  └── run-tests.sh

docs/development/
├── TESTING.md (update)
├── BUILD_WORKFLOW.md (new)
└── QUALITY_GATES.md (new or update)

scripts/
└── hooks/install-hooks.sh
```

**Structure Decision**: Extend the existing Android project by adding a `build-logic` composite build for convention plugins, centralised quality tooling under `config/quality`, and updated documentation/scripts aligned with current directories.

## Complexity Tracking

*Fill ONLY if Constitution Check has violations that must be justified*

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| [e.g., 4th project] | [current need] | [why 3 projects insufficient] |
| [e.g., Repository pattern] | [specific problem] | [why direct DB access insufficient] |
