# Data Model: Workflow Centralisation

## BuildConventionPlugin
- **id** (String, required): Plugin identifier applied in module `plugins { }` block.
- **appliesTo** (Enum: `APPLICATION`, `FEATURE`, `LIBRARY`, `KOTLIN_LIBRARY`, `TESTING`): Module category target.
- **defaultDependencies** (List<String>): Version catalog aliases automatically added.
- **compilerFlags** (List<String>): Kotlin/Java compiler arguments enforced by the convention.
- **enabledFeatures** (List<String>): Android/Compose build features toggled on.
- **qualityTasks** (List<String>): Gradle tasks automatically wired (e.g., `spotlessCheck`, `detekt`).
- **documentationRef** (URI): Link to convention documentation section.

## ModuleProfile
- **name** (String): Gradle module path (e.g., `:app`, `:feature:chat`).
- **profileType** (Enum: `APPLICATION`, `FEATURE`, `CORE`, `MACROBENCHMARK`, `SHARED`).
- **conventionPlugin** (String): Expected convention plugin id.
- **additionalPlugins** (List<String>): Extra plugins allowed per module.
- **coverageLayer** (Enum: `UI`, `VIEW_MODEL`, `DATA`, `INFRA`): Used by coverage mapping.

## QualityGate
- **gateId** (String): Unique identifier (e.g., `detekt`, `spotless`, `coverage`).
- **description** (String): Summary of enforcement rule.
- **task** (String): Gradle task executed to validate the gate.
- **threshold** (Number | Object): Numeric threshold (e.g., coverage %) or structured limits for multi-layer gates.
- **severity** (Enum: `BLOCKING`, `WARNING`): Determines fail-fast behaviour.
- **documentationRef** (URI): Link to rationale in `QUALITY_GATES.md`.
- **bypassPolicy** (String): Required approvals to override.

## CoverageMetric
- **layer** (Enum: `UI`, `VIEW_MODEL`, `DATA`).
- **minimumPercent** (Number): Required minimum coverage.
- **sourceSets** (List<String>): Gradle source sets included in measurement.
- **reportPath** (String): Relative path to coverage XML/HTML output.

## StaticAnalysisRule
- **ruleId** (String): Detekt rule identifier.
- **category** (Enum: `STYLE`, `BUG`, `ARCHITECTURE`, `PERFORMANCE`).
- **typeResolutionRequired** (Boolean): Indicates the rule needs compiler type info.
- **configOptions** (Map<String, Any>): Custom rule parameters.
- **autoCorrect** (Boolean): Whether the rule offers auto-fix suggestions.

## TestCommand
- **taskName** (String): Gradle task (e.g., `testDebugUnitTest`).
- **scope** (Enum: `UNIT`, `INTEGRATION`, `PERFORMANCE`, `SCREENSHOT`).
- **expectedDurationSeconds** (Number): Target max execution time.
- **reportsGenerated** (List<String>): Paths to JUnit/cobertura/screenshot outputs.
- **prerequisites** (List<String>): Required services/devices (e.g., managed device group, emulator).
- **coverageDependencies** (List<String>): Other tasks producing coverage data.
