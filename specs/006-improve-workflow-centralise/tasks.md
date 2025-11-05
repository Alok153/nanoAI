# Tasks: Improve Workflow – Centralise Build Logic

**Input**: Design documents from `/specs/006-improve-workflow-centralise/`
**Prerequisites**: plan.md (required), spec.md (required for user stories), research.md, data-model.md, contracts/

**Tests**: Not explicitly requested in the spec; story phases focus on implementation and validation steps.

**Organization**: Tasks are grouped by user story so each increment can be implemented and validated independently.

## Format: `[ID] [P?] [Story] Description`
- **[P]**: Can run in parallel (different files, no direct dependency)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Bootstrap composite build scaffolding required by all subsequent work

- [X] T001 [Setup] Update `settings.gradle.kts` to include `includeBuild("build-logic")` and share plugin repositories for the composite build
- [X] T002 [P] [Setup] Create `build-logic/settings.gradle.kts` with plugin management that imports the root `../gradle/libs.versions.toml`
- [X] T003 [P] [Setup] Create `build-logic/build.gradle.kts` applying the `kotlin-dsl` plugin and configuring repositories for convention plugins
- [X] T004 [P] [Setup] Create `build-logic/gradle.properties` setting JVM args and Kotlin code style defaults for the convention build

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Provide shared metadata and coverage inputs required before any story-specific work

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

- [X] T005 [Foundation] Create `config/testing/coverage/module-profiles.json` enumerating `ModuleProfile` entries for `:app` and `:macrobenchmark`
- [X] T006 [Foundation] Extend `scripts/coverage/merge-coverage.sh` to consume `module-profiles.json` when assembling multi-module coverage reports
- [X] T007 [Foundation] Create `config/testing/coverage/coverage-metadata.json` capturing default `CoverageMetric` thresholds for UI, ViewModel, and Data layers

**Checkpoint**: Shared metadata ready – user story implementation can now begin in parallel

---

## Phase 3: User Story 1 - Centralize Build Logic (Priority: P1) 🎯 MVP

**Goal**: Deliver convention plugins so every module consumes a single source of Gradle configuration truth

**Independent Test**: All modules build successfully while referencing only convention plugin IDs; no duplicated Android or dependency blocks remain in module `build.gradle.kts` files

Tests not requested for this story (per spec).

### Implementation for User Story 1

- [X] T008 [US1] Implement `build-logic/src/main/kotlin/com/vjaykrsna/nanoai/buildlogic/SharedConfiguration.kt` with shared compile/target SDK, Kotlin options, and quality task wiring helpers
- [X] T009 [P] [US1] Implement `build-logic/src/main/kotlin/com/vjaykrsna/nanoai/buildlogic/AndroidApplicationConventionPlugin.kt` registering the application plugin id
- [X] T010 [P] [US1] Implement `build-logic/src/main/kotlin/com/vjaykrsna/nanoai/buildlogic/AndroidFeatureConventionPlugin.kt` encapsulating feature module defaults
- [X] T011 [P] [US1] Implement `build-logic/src/main/kotlin/com/vjaykrsna/nanoai/buildlogic/AndroidLibraryComposeConventionPlugin.kt` enabling Compose-specific build features
- [X] T012 [P] [US1] Implement `build-logic/src/main/kotlin/com/vjaykrsna/nanoai/buildlogic/AndroidTestingConventionPlugin.kt` covering instrumentation-only modules such as `:macrobenchmark`
- [X] T013 [P] [US1] Implement `build-logic/src/main/kotlin/com/vjaykrsna/nanoai/buildlogic/KotlinLibraryConventionPlugin.kt` for pure Kotlin modules shared across features
- [X] T014 [US1] Update `build-logic/build.gradle.kts` to declare all convention plugins inside the `gradlePlugin {}` block with correct implementation classes
- [X] T015 [P] [US1] Create plugin descriptor files under `build-logic/src/main/resources/META-INF/gradle-plugins/` mapping plugin ids to implementation classes
- [X] T016 [US1] Update `gradle/libs.versions.toml` with shared dependency aliases, compiler flags, and Android SDK constants consumed by the convention plugins
- [X] T017 [US1] Refactor `app/build.gradle.kts` to apply `com.vjaykrsna.nanoai.android.application` and remove duplicated Android + dependency configuration now provided by conventions
- [X] T018 [US1] Refactor `macrobenchmark/build.gradle.kts` to apply `com.vjaykrsna.nanoai.android.testing` for instrumentation defaults and drop redundant blocks
- [X] T019 [US1] Create `config/quality/conventions.json` documenting each `BuildConventionPlugin` (id, appliesTo, qualityTasks, documentationRef) for the `/conventions` API response

**Checkpoint**: User Story 1 functional – all modules rely on convention plugins and build consistently

---

## Phase 4: User Story 2 - Enhance Static Analysis Tooling (Priority: P1)

**Goal**: Provide comprehensive Detekt + Spotless configuration with enforced architectural rules and centralized setup

**Independent Test**: Running `./gradlew detekt detektMain detektTest spotlessCheck` produces actionable reports across modules using the new configurations and custom rule set

Tests not requested for this story (per spec).

### Implementation for User Story 2

- [X] T020 [US2] Update `settings.gradle.kts` to include `:config:quality:detekt:custom-rules` and ensure the composite build exposes it
- [X] T021 [P] [US2] Create `config/quality/detekt/custom-rules/build.gradle.kts` configuring Kotlin/JVM compilation for the Detekt rule module
- [X] T022 [P] [US2] Implement `config/quality/detekt/custom-rules/src/main/kotlin/com/vjaykrsna/nanoai/detektrules/CleanArchitectureRuleSetProvider.kt`
- [X] T023 [P] [US2] Implement `config/quality/detekt/custom-rules/src/main/kotlin/com/vjaykrsna/nanoai/detektrules/CleanArchitectureRule.kt` enforcing layer boundaries
- [X] T024 [US2] Write `config/quality/detekt/detekt.yml` enabling type resolution, referencing the custom rule set, and documenting rule rationale
- [X] T025 [US2] Update `build.gradle.kts` to apply Detekt with type resolution defaults, wiring `detektPlugins(project(":config:quality:detekt:custom-rules"))`
- [X] T026 [P] [US2] Create `config/quality/spotless/spotless.kotlin.gradle` and `config/quality/spotless/spotless.misc.gradle` defining formatting standards
- [X] T027 [US2] Update `build.gradle.kts` and `build-logic/src/main/kotlin/com/vjaykrsna/nanoai/buildlogic/SharedConfiguration.kt` so convention plugins apply Spotless and Detekt tasks automatically
- [X] T028 [US2] Create `config/quality/detekt/rules.json` cataloguing each `StaticAnalysisRule` (ruleId, category, typeResolutionRequired, documentationRef)

**Checkpoint**: User Story 2 complete – static analysis tooling centralized with custom rule enforcement

---

## Phase 5: User Story 3 - Streamline Testing Strategy (Priority: P1)

**Goal**: Standardize coverage enforcement, Flow testing utilities, and screenshot tooling for predictable test execution

**Independent Test**: `./gradlew testDebugUnitTest verifyCoverageThresholds roboScreenshotDebug` runs with clear output, layer percentages respect thresholds, and Flow tests share the new helper utilities

Tests not requested for this story (per spec).

### Implementation for User Story 3

- [X] T029 [US3] Create `config/testing/coverage/coverage-thresholds.gradle.kts` to enforce `CoverageMetric` thresholds using `coverage-metadata.json`
- [X] T030 [P] [US3] Update `scripts/coverage/generate-summary.py` to produce per-layer summaries matching `CoverageMetric` definitions
- [X] T031 [P] [US3] Add `app/src/test/java/com/vjaykrsna.nanoai/shared/testing/FlowTestExt.kt` providing a Turbine-based Flow testing helper
- [X] T032 [P] [US3] Add `config/testing/tooling/roborazzi-config.gradle.kts` configuring Roborazzi screenshot baselines
- [X] T033 [US3] Update `build.gradle.kts` and relevant convention plugins to register `verifyCoverageThresholds`, `roboScreenshotDebug`, and Flow helper dependencies
- [X] T034 [P] [US3] Create `config/testing/tooling/test-commands.json` enumerating each `TestCommand` (taskName, scope, expectedDurationSeconds, reportsGenerated)

**Checkpoint**: User Story 3 complete – testing strategy automated with coverage and tooling clarity

---

## Phase 6: User Story 4 - Developer Documentation & Onboarding (Priority: P2)

**Goal**: Deliver clear onboarding and workflow documentation covering build commands, testing, and quality gates

**Independent Test**: A new developer follows the updated docs to set up the environment, run build + analysis commands, and understands quality gates without extra guidance

Tests not requested for this story (per spec).

### Implementation for User Story 4

- [X] T035 [US4] Create `docs/development/BUILD_WORKFLOW.md` outlining end-to-end build, analysis, and testing commands referencing convention plugins
- [X] T036 [P] [US4] Update `docs/development/TESTING.md` to incorporate Flow helpers, screenshot strategy, and coverage verification steps
- [X] T037 [P] [US4] Update `docs/development/QUALITY_GATES.md` with detailed gate descriptions, thresholds, and bypass policy references
- [X] T038 [P] [US4] Update `README.md` to surface links to the new workflow and testing documentation for onboarding

**Checkpoint**: User Story 4 complete – documentation enables self-service onboarding

---

## Phase 7: User Story 5 - Automated Quality Gate Enforcement (Priority: P2)

**Goal**: Enforce quality gates via local hooks, shared metadata, and build scan annotations for rapid feedback

**Independent Test**: Running `scripts/hooks/pre-commit.sh` blocks commits with clear remediation guidance when gates fail; build scans show annotated metadata

Tests not requested for this story (per spec).

### Implementation for User Story 5

- [X] T039 [US5] Create `scripts/hooks/pre-commit.sh` executing Spotless, Detekt, unit tests, and coverage verification with descriptive failure output
- [X] T040 [P] [US5] Create `scripts/hooks/install-hooks.sh` to symlink the pre-commit hook across developer environments (POSIX-compatible)
- [X] T041 [US5] Create `config/quality/quality-gates.json` documenting each `QualityGate` (gateId, task, threshold, severity, bypassPolicy)
- [X] T042 [US5] Update `docs/development/BUILD_WORKFLOW.md` with hook installation steps, bypass guidance, and troubleshooting tips
- [X] T043 [US5] Create `scripts/quality/annotate-build-scan.sh` posting branch, gate outcomes, and slow task tags to the `/build-scans/annotate` endpoint

**Checkpoint**: User Story 5 complete – automated gates enforced locally and in CI with actionable metadata

---

## Phase 8: Polish & Cross-Cutting Concerns

**Purpose**: Validate end-to-end workflow, capture metrics, and document outcomes across stories

- [ ] T044 [Polish] Run `./gradlew assembleDebug --scan`, capture before/after timings, and append results to `docs/development/BUILD_WORKFLOW.md`
- [ ] T045 [Polish] Execute `scripts/hooks/pre-commit.sh --dry-run` and record troubleshooting guidance in `docs/development/QUALITY_GATES.md`
- [ ] T046 [Polish] Follow `specs/006-improve-workflow-centralise/quickstart.md` steps end-to-end and log successes/failures in `docs/development/BUILD_WORKFLOW.md`
- [ ] T047 [Polish] Run `./gradlew detekt` to ensure static analysis passes across all modules
- [ ] T048 [Polish] Run `./gradlew testDebugUnitTest` to verify all unit tests pass
- [ ] T049 [Polish] Run `./gradlew connectedDebugAndroidTest` to verify instrumentation tests pass on device/emulator

---

## Dependencies & Execution Order

### Phase Dependencies
- **Setup (Phase 1)** → prerequisite for every other phase
- **Foundational (Phase 2)** → depends on Setup; blocks all user stories
- **User Story Phases (3–7)** → each depends on Foundational completion; P1 stories (US1–US3) should finish before P2 stories (US4–US5)
- **Polish (Phase 8)** → runs after desired user stories are complete

### User Story Dependencies
- **US1** → none beyond Phase 2; establishes convention plugins consumed by other stories
- **US2** → depends on US1 (conventions apply Detekt/Spotless) to avoid duplicate configuration
- **US3** → depends on US1 (convention hooks) and Phase 2 metadata
- **US4** → depends on US1–US3 for accurate documentation of tooling
- **US5** → depends on US1–US3 for gate execution details and on US4 for documentation structure

### Within-Story Sequencing
- Tasks modifying the same file run sequentially without `[P]`
- `[P]` tasks can start once their prerequisites (if any) are satisfied; they touch distinct files or directories
- Documentation updates in later stories assume earlier implementation tasks are merged

---

## Parallel Execution Examples

### User Story 1 (US1)
```bash
# After T008 completes, run these in parallel:
T009 – AndroidApplicationConventionPlugin.kt
T010 – AndroidFeatureConventionPlugin.kt
T011 – AndroidLibraryComposeConventionPlugin.kt
T012 – AndroidTestingConventionPlugin.kt
T013 – KotlinLibraryConventionPlugin.kt
T015 – Plugin descriptor files
```

### User Story 2 (US2)
```bash
# Once T020 finishes:
T021 – custom-rules/build.gradle.kts
T022 – CleanArchitectureRuleSetProvider.kt
T023 – CleanArchitectureRule.kt
T026 – Spotless config scripts
T028 – rules.json metadata
```

### User Story 3 (US3)
```bash
# After T029 initializes coverage thresholds:
T030 – scripts/coverage/generate-summary.py
T031 – FlowTestExt.kt helper
T032 – roborazzi-config.gradle.kts
T034 – test-commands.json
```

### User Story 4 (US4)
```bash
# With implementation details settled:
T036 – docs/development/TESTING.md
T037 – docs/development/QUALITY_GATES.md
T038 – README.md links
```

### User Story 5 (US5)
```bash
# After quality gate metadata exists:
T039 – scripts/hooks/pre-commit.sh
T040 – scripts/hooks/install-hooks.sh
T041 – quality-gates.json
T043 – scripts/quality/annotate-build-scan.sh
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)
1. Complete Phase 1 (Setup) and Phase 2 (Foundational)
2. Deliver Phase 3 (US1) to centralize build logic
3. Validate by building `:app` and `:macrobenchmark` with only convention plugins

### Incremental Delivery
1. Ship US1 (conventions) → enables consistent builds (MVP)
2. Add US2 (static analysis) → automated quality checks with custom rules
3. Add US3 (testing strategy) → coverage enforcement and tooling
4. Layer US4 (documentation) → onboarding clarity
5. Finish with US5 (quality gate automation) → local + CI enforcement

### Parallel Team Strategy
- Developer A: US1 → US3 (core tooling)
- Developer B: US2 (Detekt/Spotless) then assist US5 (hooks)
- Developer C: US4 (docs) and US5 (documentation updates)
- Sync at each checkpoint before moving to the next priority
